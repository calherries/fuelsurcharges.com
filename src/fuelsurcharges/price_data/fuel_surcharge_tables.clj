(ns fuelsurcharges.price-data.fuel-surcharge-tables
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as t]
            [oz.core :as oz]
            [fuelsurcharges.db.core :as db]
            [dk.ative.docjure.spreadsheet :as ss]))

(defn inst->local-date [inst]
  (t/local-date inst (t/zone-id "Europe/Amsterdam")))

(defn fuel-surcharge-table [sheet-name]
  (->> (ss/load-workbook "downloads/fuel_surcharge_tables.xlsx")
       (ss/select-sheet  sheet-name)
       (ss/select-columns {:A :fuel-surcharge-table-id
                           :B :price
                           :C :surcharge-amount})
       (drop 1)
       (map #(update % :fuel-surcharge-table-id int))
       (map vals)))

(comment (fuel-surcharge-table "UPS Ground"))

(comment (db/get-fuel-surcharge-tables))
(comment (db/get-fuel-surcharge-table-rows))
(comment (db/create-fuel-surcharge! {:market-id    3
                                     :name         "Ground"
                                     :source-url   "https://www.ups.com/us/en/shipping/surcharges/fuel-surcharges.page"
                                     :company-name "UPS"}))

(comment (db/insert-market-prices! {:market-prices
                                    [[1 (LocalDate/now) 1.091 "EUR"]]}))

(comment (db/create-fuel-surcharge-table!
           {:fuel-surcharge-id        1
            :update-interval-unit     "week"
            :update-interval          1
            :delay-period-unit        "week"
            :delay-periods            2
            :price-is-rounded-to-cent true
            :surcharge-type           "percentage_of_line_haul"}))

(comment (db/insert-fuel-surcharge-table-rows!
           {:fuel-surcharge-table (fuel-surcharge-table "UPS Ground")}))
