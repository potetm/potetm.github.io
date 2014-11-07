(ns blog.responsive-design-frp.core
  (:require [blog.responsive-design-frp.template :as t]
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

(defn ->js [obs]
  (b/map obs clj->js))

(def KEYS #{ENTER TAB UP_ARROW DOWN_ARROW ESC})
(def keycode->action
  {ENTER      :select
   TAB        :select
   UP_ARROW   :prev
   DOWN_ARROW :next
   ESC        :exit})

(defn key-events [$elem]
  (-> ($ js/document)
      bjb/keydownE
      (b/filter #(j/is $elem ":hover"))
      (b/do-action j/prevent)
      (b/map #(.-keyCode %))
      (b/filter (comp not nil? KEYS))
      (b/map (comp vector keycode->action))))

(defn init-model [items]
  (let [model (bjb/combine-model
                {:items       items
                 :highlighted "none"
                 :selected    "none"})]
    (-> model
        ->clj
        (b/filter (comp empty? :items))
        (b/skip-duplicates =)
        (b/map #(assoc % :highlighted "none" :selected "none"))
        ->js
        (->> (bjb/add-source model)))
    model))

(defn update-model! [model events event-key update-key next-fn]
  (-> (b/filter events (comp not nil? #{event-key} first))
      (b/map (b/combine-as-array model events))
      ->clj
      (b/map (fn [[m e :as v]] (assoc m update-key (next-fn v))))
      ->js
      (->> (bjb/add-source model))))

(defn init-events! [model events]
  (update-model! model events :next :highlighted
                 (fn [[{:keys [items highlighted]} _e]]
                   (if (= "none" highlighted)
                     0
                     (mod (inc highlighted) (count items)))))

  (update-model! model events :prev :highlighted
                 (fn [[{:keys [items highlighted]} _e]]
                   (if (= "none" highlighted)
                     (count items)
                     (mod (dec highlighted) (count items)))))

  (update-model! model events :clear-highlight :highlighted
                 (constantly "none"))

  (update-model! model events :highlight :highlighted
                 (fn [[{:keys [highlighted]} [_k index]]]
                   (if (or (number? index) (= "none" index))
                     index
                     highlighted)))

  (update-model! model events :select :selected
                 (fn [[{:keys [highlighted]} _e]]
                   highlighted))

  (update-model! model events :append :items
                 (fn [[{:keys [items]} [_k item]]]
                   (if (empty? item)
                     []
                     (conj items item)))))

(defn menu [items events render]
  (let [model (init-model items)]
    (init-events! model events)
    (-> model
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [{:keys [items highlighted selected]}]
            (render items highlighted selected))))
    model))

(let [$div ($ :div#array-highlight)
      $pre ($ :pre#array-highlight-list $div)]
  (menu
    ["so" "it" "goes"]
    (key-events $div)
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
           (j/text $pre)))))

(defn leavestream [$ul]
  (-> $ul
      bjb/mouseleaveE
      (b/map (constantly [:clear-highlight]))))

(defn overstream [$ul]
  (-> $ul
      bjb/mouseoverE
      (b/map #($ (.-target %)))
      (b/filter #(j/is % "li"))
      (b/map #(vector :highlight (.index %)))))

(defn hover-events [$ul]
  (-> $ul
      bjb/clickE
      (b/map (constantly [:select]))
      (b/merge-all (leavestream $ul) (overstream $ul) (key-events $ul))))

(let [$elem ($ :ul#ul-highlight-select-list)]
  (menu
    ["So"
     "say"
     "we"
     "all"]
    (hover-events $elem)
    (fn [items highlighted selected]
      (j/html $elem "")
      (doseq [[item i] (map vector items (iterate inc 0))
              :let [$item ($ (t/li item (= i highlighted) (= i selected)))]]
        (j/append $elem $item)))))

(defn menu-incremental [items events]
  (let [model (init-model items)
        items (bjb/lens model "items")
        highlight (-> (bjb/lens model "highlighted") (b/sliding-window 2))
        select (-> (bjb/lens model "selected") (b/sliding-window 2))]
    (init-events! model events)

    {:items       items
     :highlight   (b/map highlight second)
     :unhighlight (b/map highlight first)
     :select      (b/map select second)
     :unselect    (b/map select first)}))

(defn bind-li-changes! [$elem stream update-fn]
  (-> stream
      (b/filter (partial not= "none"))
      (b/filter (comp not nil?))
      (b/map #($ (str "li:nth-child(" (inc %) ")") $elem))
      (b/on-value update-fn)))

(let [$elem ($ :ul#incremental-list)]
  (let [updates (menu-incremental
                  ["You're"
                   "a"
                   "Hufflepuff"
                   "Harry"]
                  (hover-events $elem))]
    (-> (:items updates)
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [items]
            (j/html $elem "")
            (doseq [item items]
              (j/append $elem ($ (t/li item false false)))))))

    (bind-li-changes! $elem (:highlight updates) #(j/add-class % "highlighted"))
    (bind-li-changes! $elem (:unhighlight updates) #(j/remove-class % "highlighted"))
    (bind-li-changes! $elem (:select updates) #(j/add-class % "selected"))
    (bind-li-changes! $elem (:unselect updates) #(j/remove-class % "selected"))))

(defn item-events [& items]
  (-> (b/repeatedly 1000 (clj->js items))
      (b/map (partial vector :append))))

(let [$elem ($ :ul#incremental-changes-list)]
  (let [events (hover-events $elem)
        item-events (item-events "Time" "to" "toss" "the" "dice" [])
        updates (menu-incremental [] (b/merge events item-events))]
    (-> (:items updates)
        (b/map (b/combine-as-array (:items updates) (:highlight updates) (:select updates)))
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [[items highlighted selected]]
            (j/html $elem "")
            (doseq [[item i] (map vector items (iterate inc 0))
                    :let [$item ($ (t/li item (= i highlighted) (= i selected)))]]
              (j/append $elem $item)))))

    (bind-li-changes! $elem (:highlight updates) #(j/add-class % "highlighted"))
    (bind-li-changes! $elem (:unhighlight updates) #(j/remove-class % "highlighted"))
    (bind-li-changes! $elem (:select updates) #(j/add-class % "selected"))
    (bind-li-changes! $elem (:unselect updates) #(j/remove-class % "selected"))))
