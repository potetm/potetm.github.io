(ns blog.frp
  (:require [jayq.core :refer [$] :as j]
            [yolk.bacon :as b]
            [yolk.jquery :as bjb]))

(-> (b/merge-all
      (b/interval 250 1)
      (b/interval 1000 2)
      (b/interval 1500 3))
    (b/map #(str "<div class=\"proc-" % "\">Process " % "</div>"))
    (b/sliding-window 10)
    (b/map (comp into-array reverse))
    (b/on-value
      #(j/html ($ :div#example-multi-process) %)))

(defn offset-x [e]
  (or (.-offsetX e)
      (- (.-pageX e)
         (int (:left (j/offset ($ (.-target e))))))))

(defn offset-y [e]
  (or (.-offsetY e)
      (- (.-pageY e)
         (int (:top (j/offset ($ (.-target e))))))))

(defn offset-stream [$elem]
  (-> (bjb/mousemoveE $elem)
      (b/map #(vector (offset-x %) (offset-y %)))))

(let [$elem ($ :div#example-mouse-element)]
  (-> (offset-stream $elem)
      (b/map (fn [[x y]] (str x ", " y)))
      (b/on-value
        #(j/html $elem %))))

(defn page-position [$elem [x y]]
  (let [offset (j/offset $elem)]
    [(+ x (int (:left offset)))
     (+ y (int (:top offset)))]))

(let [$elem ($ :div#example-mouse-page)]
  (-> (offset-stream $elem)
      (b/map (partial page-position $elem))
      (b/map (fn [[x y]] (str x ", " y)))
      (b/on-value
        #(j/html $elem %))))

(let [$elem ($ :div#example-mouse-keyboard)
      mouse-stream (-> (offset-stream $elem)
                       (b/map (partial page-position $elem))
                       (b/map (fn [[x y]] (str x ", " y))))
      keyboard-stream (-> (bjb/keyupE ($ js/window))
                          (b/map #(.-keyCode %))
                          (b/to-property ""))]
  (-> (b/combine-as-array mouse-stream keyboard-stream)
      (b/on-value
        (fn [[pos-string keycode]]
          (j/html ($ :span#emk-mouse $elem) pos-string)
          (j/html ($ :span#emk-keyboard $elem) keycode)))))

(defn fake-search [kind]
  (fn [query]
    (b/later (rand-int 100) [kind query])))

(def web-1 (fake-search :web-1))
(def web-2 (fake-search :web-2))
(def image-1 (fake-search :image-1))
(def image-2 (fake-search :image-2))
(def video-1 (fake-search :video-1))
(def video-2 (fake-search :video-2))

(defn fastest [query & replicas]
  (let [bus (b/bus)
        timeout (b/later 80 "null")]
    (doseq [replica replicas]
      (b/plug bus (replica query)))

    (-> (b/merge timeout bus)
        (b/take 1))))

(defn google [query]
  (b/combine-as-array
    (fastest query web-1 web-2)
    (fastest query image-1 image-2)
    (fastest query video-1 video-2)))

(-> ($ :button#search)
    bjb/clickE
    (b/do-action #(j/prevent %))
    (b/flat-map-latest #(google "clojure"))
    (b/on-value
      #(j/text ($ :div#example-search-output) %)))
