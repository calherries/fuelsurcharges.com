(ns fuelsurcharges.db.models
  (:require
   [fuelsurcharges.db.core :refer [datasource] :as db]
   [java-time :as t]
   [mount.core :refer [defstate]]
   [gungnir.database :as gd]
   [gungnir.query :as q]
   [honeysql.core :as sql]
   [gungnir.model :refer [register!]]))

(def market-model
  [:map
   {:has-many {:market-price :market/market-prices}}
   [:market/id {:primary-key true} int?]
   [:market/market-name string?]
   [:market/source-name string?]
   [:market/created-at {:auto true} [:fn t/local-date-time?]]])

(def market-price-model
  [:map
   {:belongs-to {:market :market-price/market-id}}
   [:market-price/id {:primary-key true} int?]
   [:market-price/market-id int?]
   [:market-price/price double?]
   [:market-price/currency string?]
   [:market-price/price-date [:fn t/local-date?]]
   [:market-price/created-at {:auto true} [:fn t/local-date-time?]]])

(defstate register-models
  :start
  (register!
    {:market       market-model
     :market-price market-price-model}))

(defn select-all [table]
  (-> (q/select :*)
      (q/from table)
      (gd/query!)))

(comment (select-all :market))