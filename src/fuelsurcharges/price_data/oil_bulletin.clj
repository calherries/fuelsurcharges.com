(ns fuelsurcharges.price-data.oil-bulletin
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as t]
            [oz.core :as oz]
            [fuelsurcharges.db.core :as db]
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
    [id (t/local-date date) price currency]))

(defn update-market-price! [{:keys [id data]}]
  (db/delete-market-prices! {:market-id id})
  (db/insert-market-prices! {:market-prices (market-prices-insert id data "USD")}))

(comment (update-market-price! {:id   3
                                :data [{:date "2020-07-13" :price 2.195}]}))

(defn update-market-prices!
  "Updates all markets with the latest prices"
  []
  (doall (map update-market-price! markets-with-data)))

(comment (update-market-prices!))

(comment (db/get-markets))
(comment (->> (db/get-market-prices)
              (filter (comp #{3} :market-id))))
(comment (db/create-market! {:market-name "Automotive Gas Oil with Taxes (EU), European Commission Oil Bulletin"
                             :source-name "The European Commission's Weekly Oil Bulletin"}))
(comment (db/create-market! {:market-name "U.S. On Highway Diesel Fuel"
                             :source-name "EIA"}))
(comment (db/create-market! {:market-name "U.S. Gulf Coast Kerosene-Type Jet Fuel"
                             :source-name "EIA"}))
(comment (db/create-market! {:market-name "U.S. Regular Gasoline"
                             :source-name "EIA"}))
(comment (db/delete-market! {:id 3}))
(comment (db/delete-market-prices! {:market-id 3}))
(comment (db/delete-market-prices! {:market-id 4}))
(comment (db/delete-market-prices! {:market-id 5}))
(comment (db/insert-market-prices! {:market-prices (market-prices-insert 3 eia-price-data "USD")}))
(comment (db/insert-market-prices! {:market-prices (market-prices-insert 4 eia-spot-price-data "USD")}))
(comment (db/insert-market-prices! {:market-prices (market-prices-insert 5 eia-gas-price-data "USD")}))
(comment (map (fn [[k v]] [k (count v)]) (group-by :market-id (db/get-market-prices))))
;; OZ
(comment (oz/start-server!))

(comment (def line-plot
           {:title    "Price of automotive gas oil"
            :mark     "line"
            :data     {:values eia-price-data}
            :encoding {:x {:field "date" :type "temporal"}
                       :y {:field "price" :type "quantitative"}}
            :width    800}))

;; Render the plot
(comment (oz/view! line-plot))
