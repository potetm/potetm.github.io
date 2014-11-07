(ns reactive-design.core
  (:require-macros [blog.reactive-design.event :refer [defsubscriber]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [<!] :as a]
            [datascript :as d]
            [blog.reactive-design.event :as event]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def conn (d/create-conn))

(defn bind
  ([conn q]
   (bind conn q (atom nil)))
  ([conn q state]
   (let [k (uuid/make-random)]
     (reset! state (d/q q @conn))
     (d/listen!
       conn k
       (fn [tx-report]
         (let [novelty (d/q q (:tx-data tx-report))]
           (when (not-empty novelty) ;; Only update if query results actually changed
             (reset! state (d/q q (:db-after tx-report)))))))
     (set! (.-__key state) k)
     state)))

(defn incr [db attr]
  (let [[e v] (first
                (d/q '[:find ?e ?v
                       :in $ ?a
                       :where
                       [_ ?a ?v]]
                     db
                     attr))]
    [[:db/add e attr (inc (or v 0))]]))

(defsubscriber test 5 [:button-clicked]
  (go-loop []
    (when-some [v (<! test)]
      (d/transact! conn [[:db.fn/call incr :count]])
      (recur))))

(def query
  '[:find ?v
    :in $
    :where
    [_ :count ?v]])

(defn hello-reagent []
  (let [state (bind conn query (atom [[0]]))]
    [:div (str "Hello, Reagent! " (ffirst @state))
     [:button {:on-click #(event/publish!
                           (event/event :button-clicked {} {}))}]]))

(reagent/render-component
  (fn []
    [hello-reagent])
  (js/document.getElementById "test-01"))
