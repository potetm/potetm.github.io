(ns blog.reactive-design.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [blog.reactive-design.domain :as domain]
            [blog.reactive-design.subscriber :as subs]
            [cljs.core.async :refer [<! chan] :as a]
            [datascript :as d]
            [phi.core :as phi
             :refer [conn component]
             :refer-macros [add-subscriber]]))

(enable-console-print!)

(phi/init-datascript-conn! (d/create-conn domain/schema))
(d/transact conn domain/initial-state)

(def todo-list
  (component
    (reify
      phi/IPhi
      (query [_ db]
        (domain/get-todo-items db))
      (render [_ items]
        [:ul
         (map (partial vector :li) items)]))))

(def todo-user
  (component
    (reify
      phi/IPhi
      (query [_ db]
        (domain/get-username db))
      (render [_ name]
        [:div name]))))

(def todo-app
  (component
    (reify
      phi/IPhi
      (query [_this db]
        (let [input (domain/get-input-val db)
              items (domain/get-todo-items db)]
          (when (or input (seq items))
            {:input-val  input
             :item-count (count items)})))
      (render [_this {:keys [input-val item-count]}]
        [:div
         [:h3 (todo-user)]
         [:h3 "TODO"]
         [:div (todo-list)]
         [:div.other-list (todo-list)]
         [:form {:on-submit (fn [e]
                              (.preventDefault e)
                              (phi/publish!
                                (phi/event :todo/add-item @conn {})))}
          [:input
           {:value     input-val
            :on-change (fn [e]
                         (phi/publish!
                           (phi/event :todo/input-val @conn {:value (-> e .-target .-value)})))}]
          [:button (str "Add #" (inc item-count))]]]))))

(phi/mount-app (todo-app) (js/document.getElementById "test-01"))
