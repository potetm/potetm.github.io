(ns blog.reactive-design.domain
  (:require [datascript :as d]))

(def schema
  {:todo/items {:db/cardinality :db.cardinality/many
                :db/valueType   :db.type/ref}})

(def initial-state
  [[:db/add 1 :todo/input ""]
   [:db/add 2 :user/username "tpote"]])

(defn get-username [db]
  (ffirst
    (d/q
      '[:find ?v
        :where
        [?u :user/username ?v]]
      db
      2)))

(defn get-todo-items [db]
  (map
    first
    (sort-by
      second
      (d/q
        '[:find ?n ?tx
          :in $ ?t
          :where
          [?t :todo/items ?i]
          [?i :item/name ?n ?tx]]
        db
        1))))

(defn get-input-val [db]
  (ffirst
    (d/q
      '[:find ?v
        :in $
        :where
        [?t :todo/input ?v]]
      db
      1)))

(defn get-update-input-facts [v]
  [[:db/add 1 :todo/input v]])

(defn reset-input-facts []
  [[:db/add 1 :todo/input ""]])

(defn get-add-item-facts [db]
  [[:db/add 1 :todo/items -1]
   [:db/add -1 :item/name (get-input-val db)]])
