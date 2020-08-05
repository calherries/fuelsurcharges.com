(ns fuelsurcharges.market
  (:require   [gungnir.query :as q]
              [gungnir.database :as gd]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [gungnir.model :as gm]
              [malli.core :as m]
              [honeysql.core :as sql]
              [malli.util :as mu]
              [gungnir.record :refer [model table]]
              [fuelsurcharges.models :refer [register-models] :as models]
              [fuelsurcharges.db.core :as db]
              [java-time :as t]))

(comment (gm/find :market))
(comment (gm/find :market-price))
(comment (q/all! :market))
(comment (st/instrument))
(comment (models/strip-keys [:map
                             [:include-key int?]]
                            {:include-key 1
                             :strip-key   2}))

;; Read

(defn get-markets []
  (q/all! :market))

(defn get-market-prices []
  (q/all! :market-price))

(def market-prices-list-schema
  [:sequential
   (-> models/market-price
       (mu/select-keys [:market-price/market-id
                        :market-price/price-date
                        :market-price/price]))])

(defn-spec get-last-year-market-prices (m/validator market-prices-list-schema)
  []
  (-> (q/where [:> :market-price/price-date (sql/raw ["now() - interval '1 year' - interval '2 week'"])])
      (q/all! :market-price)
      (models/strip-keys market-prices-list-schema)))

(def markets-list-schema
  [:sequential
   (-> models/market
       (mu/select-keys [:market/id
                        :market/market-name
                        :market/source-name])
       (mu/assoc :market/market-prices [:sequential models/market-price]))])

(defn-spec markets-list (m/validator markets-list-schema)
  []
  (->>    (q/all! :market)
          (map #(-> %
                    (update :market/market-prices swap! q/merge-where [:> :market-price/price-date (sql/raw ["now() - interval '1 year' - interval '2 week'"])])
                    (q/load! :market/market-prices)))
          (#(models/strip-keys % markets-list-schema))))

(comment (get-markets))
(comment (m/explain markets-list-schema (markets-list)))
(comment (m/explain market-prices-list-schema (get-last-year-market-prices)))

;; Create
(defn insert-market! [m]
  (db/insert! :market m))

(defn insert-market-prices! [m]
  (db/insert-many! :market-price m))

;; Delete
(defn delete-market! [id]
  (db/delete! :market id))

(defn delete-market-prices! [id]
  (db/delete! :market-price :market-price/market-id id))
