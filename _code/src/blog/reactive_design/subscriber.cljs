(ns blog.reactive-design.subscriber
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [blog.reactive-design.domain :as domain]
            [cljs.core.async :refer [<! chan] :as a]
            [datascript :as d]
            [phi.core :as phi
             :refer [conn]
             :refer-macros [add-subscriber]]))

(add-subscriber update-input-val 5 [:todo/input-val]
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
      (recur))))
