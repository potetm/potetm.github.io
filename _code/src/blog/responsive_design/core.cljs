(ns blog.responsive-design.core
  (:require [blog.responsive-design.template :as t]
            [clojure.string :as str]
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
    (b/filter obs (comp not nil? keys))))

(defn update-model! [model events event-key update-key next-fn]
  (let [events (filter-keywords events event-key)]
    (-> (b/map events model)
        ->clj
        (b/map
          (fn [{:keys [items highlighted selected] :as m}]
            (clj->js (assoc m update-key (next-fn m)))))
        (->> (bjb/add-source model)))))

(defn init-events! [model events]
  (update-model! model events :next :highlighted
                 (fn [{:keys [items highlighted]}]
                   (mod (inc highlighted) (count items))))

  (update-model! model events :prev :highlighted
                 (fn [{:keys [items highlighted]}]
                   (mod (dec highlighted) (count items))))

  (update-model! model events :select :selected
                 (fn [{:keys [highlighted]}]
                   highlighted))

  (let [events (b/filter events number?)]
    (-> events
        b/skip-duplicates
        (b/map (b/combine-as-array model events))
        ->clj
        (b/map
          (fn [[m e]] (clj->js (assoc m :highlighted e))))
        (->> (bjb/add-source model)))))

(defn menu [items events render]
  (let [model (init-model items)]
    (init-events! model events)

    (-> model
        b/log
        ->clj
        (b/on-value
          (fn [{:keys [items highlighted selected]}]
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

(defn hoverstream [$ul]
  (-> $ul
      bjb/mouseoverE
      (b/map #($ (.-target %)))
      (b/filter #(j/is % "li"))
      (b/map #(.index %))))

(defn hover-events [$ul]
  (-> $ul
      bjb/clickE
      (b/map (constantly :select))
      b/log
      (b/merge-all (hoverstream $ul) (key-events $ul))))

(let [$elem ($ :ul#ul-highlight-select-list)]
  (menu
    ["Gravity's Rainbow"
     "Swann's Way"
     "Absalom, Absalom"
     "Moby Dick"]
    (hover-events $elem)
    (fn [items highlighted selected]
      (j/html $elem "")
      (doseq [[item i] (map vector items (iterate inc 0))
              :let [$item ($ (t/li item (= i highlighted) (= i selected)))]]
        (j/append $elem $item)))))