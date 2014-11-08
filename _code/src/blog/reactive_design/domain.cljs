(ns blog.reactive-design.domain
  (:require [datascript :as d]))

(def schema
  {:todo/items {:db/cardinality :db.cardinality/many}})

(def initial-state
  [[:db/add 1 :todo/input ""]])

(defn get-todo-items [db]
  (map
    first
    (sort-by
      second
      (d/q
        '[:find ?i ?tx
          :in $ ?t
          :where
          [?t :todo/items ?i ?tx]]
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
  [[:db/add 1 :todo/items (get-input-val db)]])
