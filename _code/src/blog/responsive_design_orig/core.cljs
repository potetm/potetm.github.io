(ns blog.responsive-design-orig.core
  (:require-macros
    [cljs.core.match.macros :refer [match]])
  (:require
    [cljs.core.match]
    [clojure.string :as str]
    [jayq.core :refer [$] :as j]
    [yolk.bacon :as b]
    [yolk.jquery :as bjb]))

(def ENTER 13)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def TAB 9)
(def ESC 27)

(def KEYS #{UP_ARROW DOWN_ARROW ENTER TAB ESC})

(defn keycode [e]
  (.-keyCode e))

(defn key->keyword [code]
  (condp = code
    UP_ARROW :previous
    DOWN_ARROW :next
    ENTER :select
    TAB :select
    ESC :exit))

(defprotocol IHighlightable
  (-highlight! [list n])
  (-unhighlight! [list n]))

(defprotocol ISelectable
  (-select! [list n])
  (-unselect! [list n]))

(defn calculate-next [list key idx]
  (let [cnt (count list)]
    (match [idx key]
           ["none" :next] 0
           ["none" :previous] (dec cnt)
           [_ :next] (mod (inc idx) cnt)
           [_ :previous] (mod (dec idx) cnt))))

(defn highlighter-filter [v]
  (or (not (nil? (#{:next :previous :clear} v))) (number? v)))

(defn highlighter
  ([in list]
   (highlighter in list (b/constant true)))
  ([in list control?]
   (let [cur (bjb/model :none)
         prev-cur (b/sliding-window cur 2 2)
         out (b/bus)]
     (-> prev-cur
         (b/on-value
           (fn [[prev cur]]
             (when (number? prev)
               (-unhighlight! list prev))
             (-highlight! list cur)
             (b/push out cur))))

     (-> in
         (b/filter highlighter-filter)
         (b/zip-with cur (partial calculate-next list))
         (->> (bjb/add-source cur)))

     (-> (b/filter in (comp not highlighter-filter))
         (b/merge out)))))

(defn keystream [has-focus?]
  (-> ($ js/document)
      bjb/keydownE
      (b/filter has-focus?)
      (b/do-action j/prevent)
      (b/map keycode)
      (b/filter (comp not nil? KEYS))
      (b/map key->keyword)))

(defn create-example [selector render action]
  (let [$elem ($ selector)
        mousein (b/map (bjb/mouseenterE $elem) true)
        mouseout (b/map (bjb/mouseleaveE $elem) false)
        has-focus? (bjb/model false)]
    (-> (b/merge mousein mouseout)
        (->> (bjb/add-source has-focus?)))
    (render)
    (-> (action (keystream has-focus?))
        (b/on-value render))))

(defn set-char! [s i c]
  (str (.substring s 0 i) c (.substring s (inc i))))

(extend-type array
  IHighlightable
  (-highlight! [list n]
    (aset list n (set-char! (aget list n) 0 ">")))
  (-unhighlight! [list n]
    (aset list n (set-char! (aget list n) 0 " ")))

  ISelectable
  (-select! [list n]
    (aset list n (set-char! (aget list n) 1 "*")))
  (-unselect! [list n]
    (aset list n (set-char! (aget list n) 1 " "))))

(let [$elem ($ :pre#test-ui)
      ui (array "   Smalltalk"
                "   Lisp"
                "   Prolog"
                "   ML")
      render #(j/text $elem (.join ui "\n"))]
  (create-example :pre#test-ui render #(highlighter % ui)))

#_(defn selector [in list data]
  (let [out (chan)]
    (go (loop [highlighted ::none selected ::none]
          (let [e (<! in)]
            (if (= e :select)
              (do
                (when (number? selected)
                  (-unselect! list selected))
                (if (number? highlighted)
                  (do
                    (-select! list highlighted)
                    (>! out [:select (nth data highlighted)]))
                  (>! out [:select highlighted]))
                (recur highlighted highlighted))
              (do
                (>! out e)
                (if (or (= e ::none) (number? e))
                  (recur e selected)
                  (recur highlighted selected)))))))
    out))
