(ns fuelsurcharges.market
  (:require   [gungnir.query :as q]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [gungnir.model]
              [malli.core :as m]
              [honeysql.core :as sql]
              [fuelsurcharges.db.core :as db]
              [gungnir.record :refer [model table]]
              [fuelsurcharges.db.models :refer [register-models] :as models]
              [java-time :as t]))

(comment (q/all! :market))
(comment (st/instrument))

(defn select-all [table]
  (-> (q/select :*)
      (q/from table)
      (q/query!)))

(defn get-markets []
  (select-all :market))

(defn get-market-prices []
  (select-all :market-price))

(def market-prices-list-schema
  [:sequential
   [:map
    [:market-price/market-id int?]
    [:market-price/price-date any?]
    [:market-price/price double?]]])

(defn-spec get-last-year-market-prices (m/validator market-prices-list-schema)
  []
  (-> (q/select
        :market-price/market-id
        :market-price/price-date
        :market-price/price)
      (q/from :market-price)
      (q/where [:> :market-price/price-date (sql/raw ["now() - interval '1 year' - interval '2 week'"])])
      (q/query!)))

(def markets-list-schema
  [:sequential
   [:map
    [:market/id int?]
    [:market/market-name any?]
    [:market/source-name any?]
    [:market/prices any?]]])

(defn-spec markets-list (m/validator markets-list-schema)
  []
  (let [prices (get-last-year-market-prices)]
    (->> (get-markets)
         (map #(select-keys % [:market/id :market/market-name :market/source-name :market/price]))
         (map #(assoc % :market/prices (filter (comp #{(:market/id %)} :market-price/market-id) prices))))))

(comment (first (market-prices-list)))
(comment (m/explain markets-list-schema (markets-list)))
(comment (m/explain market-prices-list-schema (get-last-year-market-prices)))
