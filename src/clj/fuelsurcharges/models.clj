(ns fuelsurcharges.models
  (:require
   [fuelsurcharges.db.core :refer [datasource] :as db]
   [java-time :as t]
   [mount.core :refer [defstate]]
   [gungnir.database :as gd]
   [gungnir.query :as q]
   [honeysql.core :as sql]
   [clojure.java.jdbc :as jdbc]
   [malli.provider :as mp]
   [malli.core :as m]
   [gungnir.model :refer [register!]]))

(def market
  [:map
   {:has-many {:market-price :market/market-prices}}
   [:market/id {:primary-key true} int?]
   [:market/market-name string?]
   [:market/source-name string?]
   [:market/created-at {:auto true} [:fn t/local-date-time?]]])

(def market-price
  [:map
   {:belongs-to {:market :market-price/market-id}}
   [:market-price/id {:primary-key true} int?]
   [:market-price/market-id int?]
   [:market-price/price double?]
   [:market-price/currency string?]
   [:market-price/price-date [:fn t/local-date?]]
   [:market-price/created-at {:auto true} [:fn t/local-date-time?]]])

(def fuel-surcharge
  [:map
   {:has-many {:fuel-surcharge-table :fuel-surcharge/fuel-surcharge-tables}}
   [:fuel-surcharge/id {:primary-key true} int?]
   [:fuel-surcharge/market-id int?]
   [:fuel-surcharge/name string?]
   [:fuel-surcharge/source-url string?]
   [:fuel-surcharge/company-name string?]
   [:fuel-surcharge/created-at {:auto true} [:fn t/local-date-time?]]])

(def fuel-surcharge-table
  [:map
   {:belongs-to {:fuel-surcharge :fuel-surcharge-table/fuel-surcharge-id}
    :has-many   {:fuel-surcharge-table-row :fuel-surcharge-table/fuel-surcharge-table-rows}}
   [:fuel-surcharge-table/id {:primary-key true :auto true} int?]
   [:fuel-surcharge-table/surcharge-type string?]
   [:fuel-surcharge-table/delay-periods int?]
   [:fuel-surcharge-table/fuel-surcharge-id int?]
   [:fuel-surcharge-table/update-interval int?]
   [:fuel-surcharge-table/update-interval-unit string?]
   [:fuel-surcharge-table/valid-at [:fn t/local-date?]]
   [:fuel-surcharge-table/delay-period-unit string?]
   [:fuel-surcharge-table/price-is-rounded-to-cent boolean?]
   [:fuel-surcharge-table/created-at {:auto true} [:fn t/local-date-time?]]])

(def fuel-surcharge-table-row
  [:map
   {:belongs-to {:fuel-surcharge-table :fuel-surcharge-table-row/fuel-surcharge-table-id}}
   [:fuel-surcharge-table-row/id {:primary-key true :auto true}int?]
   [:fuel-surcharge-table-row/fuel-surcharge-table-id int?]
   [:fuel-surcharge-table-row/price double?]
   [:fuel-surcharge-table-row/surcharge-amount double?]
   [:fuel-surcharge-table-row/created-at {:auto true} [:fn t/local-date-time?]]])

(defstate register-models
  :start
  (register!
    {:market                   market
     :market-price             market-price
     :fuel-surcharge           fuel-surcharge
     :fuel-surcharge-table     fuel-surcharge-table
     :fuel-surcharge-table-row fuel-surcharge-table-row}))

(defn select-all [table]
  (-> (q/select :*)
      (q/from table)
      (sql/format)
      db/query!))

(defn strip-keys
  [value schema]
  (malli.core/decode schema value malli.transform/strip-extra-keys-transformer))

(defn map->nsmap
  [n m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (name n) (name k))
                              k)]
                 (assoc acc new-kw v)))
             {} m))

(defn select-all-namespaced [table]
  (->> (select-all table)
       (map (partial map->nsmap table))))

;; Utilities for generating malli for a given table
(comment (def model-name :market-price))
;; sample a row
(comment (first (select-all-namespaced model-name)))
;; given three sample rows, infer the schema
(comment (mp/provide (take 3 (select-all-namespaced model-name))))
;; check the schema against one row
(comment (m/explain (var-get (resolve (symbol model-name))) (first (select-all-namespaced model-name))))
;; check the schema against all rows
(comment (m/explain [:sequential (var-get (resolve (symbol model-name)))]
                    (select-all-namespaced model-name)))
