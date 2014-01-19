---
layout: post_page
title: "Responsive Reactive"
---
```clojure
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
```
```clojure
(defn update-model! [model events event-key update-key next-fn]
  (-> (b/filter events (comp not nil? #{event-key}))
      (b/map model)
      ->clj
      (b/map #(assoc % update-key (next-fn %)))
      ->js
      (->> (bjb/add-source model))))

(defn bind-highlight-number! [model events]
  (let [events (b/filter events #(or (number? %) (= "none" %)))]
    (-> events
        (b/map (b/combine-as-array model events))
        ->clj
        (b/map (fn [[m e]] (assoc m :highlighted e)))
        ->js
        (->> (bjb/add-source model)))))

(defn bind-item-update! [model events]
  (let [lens (bjb/lens model "items")]
    (-> events
        (b/filter vector?)
        (b/filter #(= (first %) :append))
        (b/map second)
        (b/on-value
          (fn [item]
            (if (empty? item)
              (bjb/set-value lens [])
              (bjb/modify lens #(conj (vec %) item))))))))

(defn init-events! [model events]
  (update-model! model events :next :highlighted
                 (fn [{:keys [items highlighted]}]
                   (if (= "none" highlighted)
                     0
                     (mod (inc highlighted) (count items)))))

  (update-model! model events :prev :highlighted
                 (fn [{:keys [items highlighted]}]
                   (if (= "none" highlighted)
                     (count items)
                     (mod (dec highlighted) (count items)))))

  (update-model! model events :select :selected
                 (fn [{:keys [highlighted]}]
                   highlighted))

  (update-model! model events :clear-highlight :highlighted (constantly "none"))

  (bind-highlight-number! model events)
  (bind-item-updates! model events))
```
```clojure
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
```
```clojure
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
```
<div id="array-highlight" class="example-select">
  <pre id="array-highlight-list">
  </pre>
</div>
```clojure
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
```
<div id="ul-highlight-select" class="example-select">
   <ul id="ul-highlight-select-list">
   </ul>
</div>
```clojure
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
```
```clojure
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
```
<div id="incremental" class="example-select">
   <ul id="incremental-list">
   </ul>
</div>
```clojure
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
```
<div id="incremental-changes" class="example-select">
   <ul id="incremental-changes-list">
   </ul>
</div>
<script type="text/javascript" src="/js/jquery.min.js"></script>
<script type="text/javascript" src="/js/bacon.js"></script>
<script type="text/javascript" src="/js/bacon-model.js"></script>
<script type="text/javascript" src="/js/bacon-jquery.js"></script>
<script type="text/javascript" src="/js/responsive-design-frp.js"></script>
