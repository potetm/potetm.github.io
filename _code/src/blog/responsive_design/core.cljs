(ns blog.responsive-design.core
  (:require [blog.responsive-design.template :as t]
            [jayq.core :refer [$] :as j]
            [yolk.bacon :as b]
            [yolk.jquery :as bjb]))

(defn ->clj [obs]
  (b/map obs #(js->clj % :keywordize-keys true)))

(defn init-model [$elem highlight unhighlight]
  (j/a))

(defn init-elem [items]
  (let [$elem ($ (t/ul))]
    (doseq [item items]
      (j/append $elem (t/li item)))

    $elem))

(defn menu [items highlight unhighlight]
  (let [$elem (init-elem items)
        next nil
        prev nil
        highlight nil]

    {:$elem $elem
     :in    {:next      next
             :prev      prev
             :highlight highlight}}))

(let [menu (menu ["a" "s" "d" "f"] nil nil)]
  (j/html ($ :div#test) (:$elem menu)))