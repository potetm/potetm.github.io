(ns blog.responsive-design.core
  (:require [clojure.string :as str]
            [jayq.core :refer [$] :as j]
            [yolk.bacon :as b]
            [yolk.jquery :as bjb]))

(def ENTER 13)
(def TAB 9)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def ESC 27)

(defn ->clj [obs]
  (b/map obs #(js->clj % :keywordize-keys true)))

(def KEYS #{ENTER TAB UP_ARROW DOWN_ARROW ESC})
(def keycode->action
  {ENTER      :select
   TAB        :select
   UP_ARROW   :prev
   DOWN_ARROW :next
   ESC        :exit})

(defn key-events [$elem]
  (let [mousein (b/map (bjb/mouseenterE $elem) true)
        mouseout (b/map (bjb/mouseleaveE $elem) false)
        has-focus? (bjb/model false)]
    (-> (b/merge mousein mouseout)
        (->> (bjb/add-source has-focus?)))

    (-> ($ js/document)
        bjb/keydownE
        (b/filter has-focus?)
        (b/do-action j/prevent)
        (b/map #(.-keyCode %))
        (b/filter (comp not nil? KEYS))
        (b/map #(keycode->action %)))))

(defn init-model [items]
  (bjb/combine-model
    {:items       items
     :highlighted -1
     :selected    -1}))

(defn filter-keywords [obs & keys]
  (let [keys (set keys)]
    (b/filter obs #(not (nil? (keys %))))))

(defn init-events! [model events]
  (let [next (filter-keywords events :next)]
    (-> (b/map next model)
        ->clj
        (b/map
          (fn [{:keys [items highlighted] :as m}]
            (let [next (if (or (< highlighted 0)
                               (= (count items) (inc highlighted)))
                         0
                         (inc highlighted))]
              (clj->js (assoc m :highlighted next)))))
        (->> (bjb/add-source model)))))

(defn menu [items events render]
  (let [model (init-model items)]
    (init-events! model events)

    (-> model
        b/log
        ->clj
        (b/on-value
          (fn [{:keys [items highlighted selected] :as test}]
            (render items highlighted selected))))))

(let [$elem ($ :pre#array-highlight-list)]
  (menu
    ["so" "it" "goes"]
    (key-events $elem)
    (fn [items highlighted selected]
      (->> (map
             (fn [i item]
               (str
                 (if (= i highlighted) ">" " ")
                 (if (= i selected) "*" " ")
                 "  "
                 item))
             (iterate inc 0)
             items)
           (str/join "\n")
           (j/text $elem)))))