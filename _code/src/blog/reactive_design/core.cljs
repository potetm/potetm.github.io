(ns blog.reactive-design.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [blog.reactive-design.domain :as domain]
            [blog.reactive-design.subscriber :as subs]
            [cljs.core.async :refer [<! chan] :as a]
            [datascript :as d]
            [fluxme.core :as fluxme
             :refer [conn component]
             :refer-macros [add-subscriber]]))

(enable-console-print!)

(fluxme/init-fluxme!
  (d/create-conn domain/schema)
  (chan 1024))
(d/transact conn domain/initial-state)
(subs/init-subscribers)

(def todo-list
  (component
    (reify
      fluxme/IFlux
      (query [_ db]
        (domain/get-todo-items db))
      (render [_ items]
        [:ul
         (map (partial vector :li) items)]))))

(def todo-app
  (component
    (reify
      fluxme/IFlux
      (query [_this db]
        (let [input (domain/get-input-val db)
              items (domain/get-todo-items db)]
          (when (or input (seq items))
            {:input-val  input
             :item-count (count items)})))
      (render [_this {:keys [input-val item-count]}]
        [:div
         [:h3 "TODO"]
         (todo-list)
         [:input
          {:value     input-val
           :on-change (fn [e]
                        (fluxme/publish!
                          (fluxme/event :todo/input-val @conn {:value (-> e .-target .-value)})))}]
         [:button
          {:on-click (fn [_]
                       (fluxme/publish!
                         (fluxme/event :todo/add-item @conn {})))}
          (str "Add #" (inc item-count))]])
      fluxme/ISubscribe
      (subscribers [_]
        [(add-subscriber update-input-val 5 [:todo/input-val]
           (go-loop []
             (when-some [{:keys [subjects]} (<! update-input-val)]
               (d/transact! conn (domain/get-update-input-facts (:value subjects)))
               (recur))))
         (add-subscriber add-item 5 [:todo/add-item]
           (go-loop []
             (when-some [{:keys [db]} (<! add-item)]
               (d/transact! conn
                            (concat (domain/get-add-item-facts db)
                                    (domain/reset-input-facts)))
               (recur))))]))))

(fluxme/mount-app (todo-app) (js/document.getElementById "test-01"))
