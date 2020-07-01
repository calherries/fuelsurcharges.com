(ns fuelsurcharges.markets
  (:require [fuelsurcharges.db.core :as db]))

(defn market-prices-list []
  {:market-prices (->> (db/get-last-year-market-prices)
                       (map #(select-keys % [:market-id :price-date :price])))})

(defn markets-list []
  (let [prices (:market-prices (market-prices-list))]
    (->> (db/get-markets)
         (map #(select-keys % [:id :market-name :source-name :price]))
         (map #(assoc % :prices (filter (comp #{(:id %)} :market-id) prices))))))

(comment (map #(-> % :prices last) (markets-list)))

(comment (db/get-markets))
(comment (db/create-market! {:market-name "Automotive Gas Oil with Taxes (EU), European Commission Oil Bulletin"
                             :source-name "The European Commission's Weekly Oil Bulletin"}))
(comment (db/get-market-prices))
(comment (db/insert-market-prices! {:market-prices
                                    [[1 (LocalDate/now) 1.091 "EUR"]]}))
