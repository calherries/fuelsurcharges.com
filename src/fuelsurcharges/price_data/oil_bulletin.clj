(ns fuelsurcharges.price-data.oil-bulletin
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as t]
            [oz.core :as oz]
            [fuelsurcharges.db.core :as db]
            [dk.ative.docjure.spreadsheet :as ss]))

(defn download-oil-bulletin []
  (clojure.java.io/copy
    (:body (http-client/get "http://ec.europa.eu/energy/observatory/reports/Oil_Bulletin_Prices_History.xlsx" {:as :stream}))
    (java.io.File. "downloads/Oil_Bulletin_Prices_History.xlsx")))

(defn inst->local-date [inst]
  (t/local-date inst (t/zone-id "UTC")))

(defn price-data []
  (->> (ss/load-workbook "downloads/Oil_Bulletin_Prices_History.xlsx")
       (ss/select-sheet  "Prices with taxes, EU")
       (ss/select-columns {:B :date, :D :price})
       (drop 5) ;; drop the first 5 rows
       (filter (comp inst? :date))
       (map #(update % :date (comp t/format inst->local-date)))
       (sort-by :date)))

(comment (take 5 price-data))

;; convert rows into form needed by database
(defn market-prices-insert [prices]
  (for [{:keys [date price]} prices]
    [1 (t/local-date date) price "EUR"]))

(comment (db/get-markets))
(comment (db/create-market! {:market-name "Automotive Gas Oil with Taxes (EU), European Commission Oil Bulletin"
                             :source-name "The European Commission's Weekly Oil Bulletin"}))
(comment (db/delete-market-prices! {:market-id 1}))
(comment (db/insert-market-prices! {:market-prices (market-prices-insert (price-data))}))
(comment (db/get-market-prices))
;; OZ
(comment (oz/start-server!))

(comment (def line-plot
           {:title    "Price of automotive gas oil, 1000L"
            :mark     "line"
            :data     {:values ( price-data)}
            :encoding {:x {:field "date" :type "temporal"}
                       :y {:field "price" :type "quantitative"}}
            :width    800}))

;; Render the plot
(comment (oz/view! line-plot))
