(ns fuelsurcharges.fuel-surcharges
  (:require [fuelsurcharges.db.core :as db]))

(defn get-fuel-surcharges []
  {:fuel-surcharges (->> (db/get-fuel-surcharges))})

(defn get-fuel-surcharges-history []
  (let [history (db/get-last-year-fuel-surcharges)]
    (->> (db/get-fuel-surcharges)
         ;; (map #(dissoc % :created-at))
         (map #(assoc % :history (filter (comp #{(:id %)} :fuel-surcharge-id) history))))))

(comment (get-fuel-surcharges-history))
(comment (db/get-current-fuel-surcharge-table-rows {:id 3}))
(comment (db/get-fuel-surcharges))
(comment (db/get-last-year-fuel-surcharges))
