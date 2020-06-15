(ns fuelsurcharges.markets
  (:require [fuelsurcharges.db.core :as db]))

(defn markets-list []
  {:markets (vec (db/get-markets))})

(comment (markets-list))

(defn market-prices-list []
  {:market-prices (->> (db/get-market-prices)
                       (map #(select-keys % [:id :market-id :price-date :price])))})

(comment (take-last 5 (:market-prices (market-prices-list))))
