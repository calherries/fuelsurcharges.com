(ns fuelsurcharges.markets
  (:require [fuelsurcharges.db.core :as db]))

(defn markets-list []
  {:markets (vec (db/get-markets))})

(comment (markets-list))

(defn market-prices-list []
  {:market-prices (->> (db/get-last-year-market-prices)
                       (map #(select-keys % [:market-id :price-date :price])))})

(defn markets-list []
  (let [prices (:market-prices (market-prices-list))]
    (->> (db/get-markets)
         (map #(select-keys % [:id :market-name :source-name :price]))
         (map #(assoc % :prices (filter (comp #{(:id %)} :market-id) prices))))))

(comment (map #(-> % :prices last) (markets-list)))
