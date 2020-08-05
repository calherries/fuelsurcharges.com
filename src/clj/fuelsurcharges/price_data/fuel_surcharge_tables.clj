(ns fuelsurcharges.price-data.fuel-surcharge-tables
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as t]
            [fuelsurcharges.db.core :as db]
            [fuelsurcharges.fuel-surcharge :as fuel-surcharge]
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
       (filter (comp (partial not-any? nil?) vals))
       (map #(update % :fuel-surcharge-table-id int))
       (map vals)))

(comment (fuel-surcharge-table "FedEx Freight"))

(comment (db/get-markets))
(comment (fuel-surcharge/get-fuel-surcharges))
(comment (fuel-surcharge/delete-fuel-surcharge! 12))
(comment (fuel-surcharge/insert-fuel-surcharge! {:market-id    3
                                                 :name         "Freight"
                                                 :source-url   "https://www.fedex.com/en-us/shipping/fuel-surcharge.html"
                                                 :company-name "FedEx"}))

(comment (db/insert-market-prices! {:market-prices
                                    [[1 (LocalDate/now) 1.091 "EUR"]]}))

(comment (db/create-fuel-surcharge-table!
           {:fuel-surcharge-id        10
            :update-interval-unit     "week"
            :update-interval          1
            :delay-period-unit        "day"
            :delay-periods            10
            :price-is-rounded-to-cent true
            :surcharge-type           "percentage_of_line_haul"}))

(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "UPS Ground")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "UPS Domestic Air")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "UPS International Air - Export")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "UPS International Air - Import")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "YRC TL")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "YRC LTL")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "YRC Jet")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "FedEx Ground")))
(comment (fuel-surcharge/insert-fuel-surcharge-table-rows!
           (fuel-surcharge-table  "FedEx Freight")))
(comment (fuel-surcharge/delete-fuel-surcharge-table-rows! 6))
