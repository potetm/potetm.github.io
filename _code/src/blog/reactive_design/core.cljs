(ns reactive-design.core
  (:require-macros [blog.reactive-design.event :refer [defsubscriber]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [<!] :as a]
            [datascript :as d]
            [blog.reactive-design.event :as event]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defsubscriber test 5 [:button-clicked]
  (go-loop []
    (when-some [v (<! test)]
      (println v)
      (recur))))

(defn hello-reagent []
  [:div "Hello, Reagent!"
   [:button {:on-click #(event/publish!
                         (event/event :button-clicked {} {}))}]])

(reagent/render-component
  hello-reagent
  (js/document.getElementById "test-01"))
