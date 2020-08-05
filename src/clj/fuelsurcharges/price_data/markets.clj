(ns fuelsurcharges.price-data.markets
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as t]
            [fuelsurcharges.market :as market]
            [dk.ative.docjure.spreadsheet :as ss]
            [missionary.core :as m]))

(defn download-oil-bulletin []
  (clojure.java.io/copy
    (:body (http-client/get "http://ec.europa.eu/energy/observatory/reports/Oil_Bulletin_Prices_History.xlsx" {:as :stream}))
    (java.io.File. "downloads/Oil_Bulletin_Prices_History.xlsx")))

(defn download-eia-prices []
  (clojure.java.io/copy
    (:body (http-client/get "https://www.eia.gov/petroleum/gasdiesel/xls/psw18vwall.xls" {:as :stream}))
    (java.io.File. "downloads/EIA_On_Highway_Diesel.xlsx")))

(defn download-eia-gas-prices []
  (clojure.java.io/copy
    (:body (http-client/get "https://www.eia.gov/petroleum/gasdiesel/xls/pswrgvwall.xls" {:as :stream}))
    (java.io.File. "downloads/EIA_Regular_Gas.xlsx")))

(defn download-eia-spot-prices []
  (clojure.java.io/copy
    (:body (http-client/get "https://www.eia.gov/dnav/pet/xls/PET_PRI_SPT_S1_W.xls" {:as :stream}))
    (java.io.File. "downloads/EIA_Gulf_Jet_Fuel.xlsx")))

(comment (download-eia-prices))
(comment (download-eia-gas-prices))
(comment (download-eia-spot-prices))

(defn inst->local-date [inst]
  (t/local-date inst (t/zone-id "Europe/Amsterdam")))

(defn price-data []
  (download-oil-bulletin)
  (->> (ss/load-workbook "downloads/Oil_Bulletin_Prices_History.xlsx")
       (ss/select-sheet  "Prices with taxes, EU")
       (ss/select-columns {:B :date, :D :price})
       (drop 5) ;; drop the first 5 rows
       (filter (comp inst? :date))
       (map #(update % :date (comp t/format inst->local-date)))
       (sort-by :date)))

(defn eia-price-data []
  (download-eia-prices)
  (->> (ss/load-workbook "downloads/EIA_On_Highway_Diesel.xlsx")
       (ss/select-sheet  "Data 1")
       (ss/select-columns {:A :date, :B :price})
       (drop 3) ;; drop the first 5 rows
       (filter (comp inst? :date))
       (map #(update % :date (comp t/format inst->local-date)))
       (sort-by :date)))

(defn eia-gas-price-data []
  (download-eia-gas-prices)
  (->> (ss/load-workbook "downloads/EIA_Regular_Gas.xlsx")
       (ss/select-sheet  "Data 3")
       (ss/select-columns {:A :date, :B :price})
       (drop 3) ;; drop the first 5 rows
       (filter (comp inst? :date))
       (map #(update % :date (comp t/format inst->local-date)))
       (filter :price) ;; drop null price rows
       (sort-by :date)))

(defn eia-spot-price-data []
  (download-eia-spot-prices)
  (->> (ss/load-workbook "downloads/EIA_Gulf_Jet_Fuel.xlsx")
       (ss/select-sheet  "Data 6")
       (ss/select-columns {:A :date, :B :price})
       (filter (comp inst? :date))
       (map #(update % :date (comp t/format inst->local-date)))
       (drop 3) ;; drop the first 5 rows
       (filter :price) ;; drop null price rows
       (sort-by :date)))

(def markets [{:id 3 :data-fn eia-price-data}
              {:id 4 :data-fn eia-spot-price-data}
              {:id 5 :data-fn eia-gas-price-data}])

(def markets-with-data
  (let [data (map hash-map
                  (repeat :data)
                  (m/? (apply m/join vector
                              (doall (map #(m/sp ((:data-fn %))) markets)))))]
    (map merge markets data)))

;; convert rows into form needed by database
(defn market-prices-insert [id prices currency]
  (for [{:keys [date price]} prices]
    {:market-price/id         id
     :market-price/price-date (t/local-date date)
     :market-price/price      price
     :markte-price/currency   currency}))

(defn update-market-price! [{:keys [id data]}]
  (market/delete-market-prices! id)
  (market/insert-market-prices! (market-prices-insert id data "USD")))

(comment (update-market-price! {:id   3
                                :data [{:date "2020-07-13" :price 2.195}]}))

(defn update-market-prices!
  "Updates all markets with the latest prices"
  []
  (doall (map update-market-price! markets-with-data)))

(comment (update-market-prices!))

(comment (market/get-markets))
(comment (->> (market/get-market-prices)
              (filter (comp #{3} :market-price/market-id))))
(comment (market/insert-market! {:market/market-name "Automotive Gas Oil with Taxes (EU), European Commission Oil Bulletin"
                                 :market/source-name "The European Commission's Weekly Oil Bulletin"}))
(comment (market/insert-market! {:market/market-name "U.S. On Highway Diesel Fuel"
                                 :market/source-name "EIA"}))
(comment (market/insert-market! {:market/market-name "U.S. Gulf Coast Kerosene-Type Jet Fuel"
                                 :market/source-name "EIA"}))
(comment (market/insert-market! {:market/market-name "U.S. Regular Gasoline"
                                 :market/source-name "EIA"}))
(comment (market/insert-market! {:market/market-name "U.S. Test Gasoline"
                                 :market/source-name "EIA"}))
(comment (market/delete-market! 6))
(comment (market/delete-market-prices! 3))
(comment (market/delete-market-prices! 4))
(comment (market/delete-market-prices! 5))
(comment (market/insert-market-prices! (market-prices-insert 3 eia-price-data "USD")))
(comment (market/insert-market-prices! (market-prices-insert 4 eia-spot-price-data "USD")))
(comment (market/insert-market-prices! (market-prices-insert 5 eia-gas-price-data "USD")))
