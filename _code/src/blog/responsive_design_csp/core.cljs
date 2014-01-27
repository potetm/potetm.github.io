(ns blog.responsive-design-csp.core
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

(defn calculate-next [list key idx]
  (let [cnt (count list)]
    (match [idx key]
           ["none" :next] 0
           ["none" :previous] (dec cnt)
           [_ :next] (mod (inc idx) cnt)
           [_ :previous] (mod (dec idx) cnt))))

(defn next-val [list key idx]
  (if (or (number? key) (= :clear key))
    key
    (calculate-next list key idx)))

(defn highlighter-filter [v]
  (or (not (nil? (#{:next :previous :clear} v))) (number? v)))

(defn highlighter
  ([in list]
   (highlighter in list (b/constant true)))
  ([in list control?]
   (let [cur (bjb/model "none")
         prev-cur (b/sliding-window cur 2 2)
         out (b/bus)]
     (-> prev-cur
         (b/on-value
           (fn [[prev cur]]
             (when (number? prev)
               (-unhighlight! list prev))
             (when (not= cur :clear)
               (-highlight! list cur))
             (b/push out cur))))

     (-> in
         (b/filter highlighter-filter)
         (b/map (b/combine-with in cur (partial next-val list)))
         (->> (bjb/add-source cur)))

     (-> (b/filter in (comp not highlighter-filter))
         (b/merge out)))))

(defn keystream [$elem]
  (let [mousein (b/map (bjb/mouseenterE $elem) true)
        mouseout (b/map (bjb/mouseleaveE $elem) false)
        has-focus? (bjb/model false)]
    (-> (b/merge mousein mouseout)
        (->> (bjb/add-source has-focus?)))

    (-> ($ js/document)
        bjb/keydownE
        (b/filter has-focus?)
        (b/do-action j/prevent)
        (b/map keycode)
        (b/filter (comp not nil? KEYS))
        (b/map key->keyword))))

(defn create-example [events render actor]
  (when render
    (render))
  (-> (actor events)
      (b/on-value #(when render (render)))))

(defn set-char! [s i c]
  (str (.substring s 0 i) c (.substring s (inc i))))

(extend-type array
  IHighlightable
  (-highlight! [list n]
    (aset list n (set-char! (aget list n) 0 ">")))
  (-unhighlight! [list n]
    (aset list n (set-char! (aget list n) 0 " "))))

(let [$div ($ :div#array-highlight)
      $pre ($ :pre#array-highlight-list $div)
      list (array "   Alan Kay"
                  "   J.C.R. Licklider"
                  "   John McCarthy")
      events (keystream $div)
      render #(j/text $pre (.join list "\n"))
      action #(highlighter % list)]
  (create-example events render action))

(defprotocol ISelectable
  (-select! [list n])
  (-unselect! [list n]))

(defn selector [in list]
  (let [highlighted (b/to-property (b/filter in number?))
        selected (bjb/model nil)
        prev-cur (b/sliding-window selected 2 2)
        out (b/bus)]
    (-> prev-cur
        (b/on-value
          (fn [[prev cur]]
            (when (number? prev)
              (-unselect! list prev))
            (-select! list cur)
            (b/push out cur))))
    (-> in
        (b/filter (partial = :select))
        (b/map highlighted)
        (->> (bjb/add-source selected)))

    (-> (b/filter in (partial not= :select))
        (b/merge out))))

(extend-type array
  ISelectable
  (-select! [list n]
    (aset list n (set-char! (aget list n) 1 "*")))
  (-unselect! [list n]
    (aset list n (set-char! (aget list n) 1 " "))))

(let [$div ($ :div#array-highlight-select)
      $pre ($ :pre#array-highlight-select-list $div)
      list (array "   Smalltalk"
                  "   Lisp"
                  "   Prolog"
                  "   ML")
      events (keystream $div)
      render #(j/text $pre (.join list "\n"))
      action #(selector (highlighter % list) list)]
  (create-example events render action))

(defn mouseleave [$ul]
  (-> (bjb/mouseleaveE $ul)
      (b/map (constantly :clear))))

(defn mouseover [$ul]
  (-> $ul
      bjb/mouseoverE
      (b/map #($ (.-target %)))
      (b/filter #(j/is % "li"))
      (b/map #(.index %))))

(defn hover-events [$ul]
  (-> $ul
      bjb/clickE
      (b/map (constantly :select))
      (b/merge-all (mouseleave $ul) (mouseover $ul) (keystream $ul))))

(defn do-to-li [list n update-fn]
  (update-fn ($ (str "li:nth-child(" (inc n) ")") ($ list))))

(extend-type js/HTMLUListElement
  ICounted
  (-count [list]
    (.-length ($ :li ($ list))))

  IHighlightable
  (-highlight! [list n]
    (do-to-li list n #(j/add-class % "highlighted")))
  (-unhighlight! [list n]
    (do-to-li list n #(j/remove-class % "highlighted")))

  ISelectable
  (-select! [list n]
    (do-to-li list n #(j/add-class % "selected")))
  (-unselect! [list n]
    (do-to-li list n #(j/remove-class % "selected"))))

(let [$ul ($ :ul#ul-highlight-select-list)
      ul (aget $ul 0)
      events (hover-events $ul)
      action #(selector (highlighter % ul) ul)]
  (create-example events nil action))
