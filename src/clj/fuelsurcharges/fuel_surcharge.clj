(ns fuelsurcharges.fuel-surcharge
  (:require   [gungnir.query :as q]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [gungnir.model :as gm]
              [gungnir.query :as query]
              [gungnir.changeset :as changeset]
              [malli.core :as m]
              [malli.provider :as mp]
              [malli.generator :as mg]
              [honeysql.core :as sql]
              [honeysql.helpers :as h]
              [malli.util :as mu]
              [fuelsurcharges.db.core :as db]
              [fuelsurcharges.models :refer [register-models] :as models]
              [java-time :as t]))

(comment (st/instrument))

(defn load-walk [record relation]
  (clojure.walk/postwalk #(if (= relation %)
                            (q/load! record %)
                            %)
                         record))

(comment (-> (q/find! :fuel-surcharge 3)
             (load-walk :fuel-surcharge/fuel-surcharge-tables)
             (load-walk :fuel-surcharge-table/fuel-surcharge-table-rows)))

;; Read

(defn-spec get-fuel-surcharges (m/validator [:sequential models/fuel-surcharge])
  []
  (q/all! :fuel-surcharge))

(def fuel-surcharge-table-rows-schema
  [:sequential
   [:map
    [:fuel-surcharge-table-row/price double?]
    [:fuel-surcharge-table-row/surcharge-amount double?]]])

(defn-spec get-current-fuel-surcharge-table-rows (m/validator fuel-surcharge-table-rows-schema)
  [id int?]
  (-> (h/select :fuel-surcharge-table-row.*)
      (h/from :fuel-surcharge)
      (h/join :fuel-surcharge-table [:= :fuel-surcharge/id :fuel-surcharge-table/fuel-surcharge-id])
      (h/merge-join :fuel-surcharge-table-row [:= :fuel-surcharge-table/id :fuel-surcharge-table-row/fuel-surcharge-table-id])
      (h/where [:= :fuel-surcharge/id id])
      q/all!
      (models/strip-keys fuel-surcharge-table-rows-schema)))

(def fuel-surcharge-history-schema
  [:sequential
   [:map
    [:fuel-surcharge/id int?]
    [:fuel-surcharge/market-id int?]
    [:market-price/price-date [:fn t/local-date?]]
    [:market-price/price double?]
    [:fuel-surcharge-table-row/price double?]
    [:fuel-surcharge-table-row/surcharge-amount double?]]])

(defn-spec get-last-year-fuel-surcharges (m/validator fuel-surcharge-history-schema)
  []
  (-> (q/select :fuel-surcharge/id
                :fuel-surcharge/market-id
                :market-price/price-date
                :market-price/price
                [(sql/call :max :fuel-surcharge-table-row/price) :fuel-surcharge-table-row/price]
                [(sql/call :max :fuel-surcharge-table-row/surcharge-amount) :fuel-surcharge-table-row/surcharge-amount])
      (q/from :market-price)
      (q/merge-join :fuel-surcharge [:= :market-price/market-id :fuel-surcharge/market-id])
      (q/merge-join :fuel-surcharge-table [:= :fuel-surcharge/id :fuel-surcharge-table/fuel-surcharge-id])
      (q/merge-join :fuel-surcharge-table-row [:= :fuel-surcharge-table/id :fuel-surcharge-table-row/fuel-surcharge-table-id])
      (q/merge-where [:> :market-price/price :fuel-surcharge-table-row/price])
      (q/merge-where [:> :market-price/price-date (sql/raw ["now() - interval '1 year' - interval '2 week'"])])
      (q/group :fuel-surcharge/id
               :fuel-surcharge/market-id
               :market-price/price-date
               :market-price/price)
      (q/order-by :market-price/price-date)
      db/honey->sql
      db/execute!))

(comment (take 2 (get-last-year-fuel-surcharges)))
(comment (db/execute! ["select * from fuel_surcharge"]))

(def get-fuel-surcharges-history-schema
  [:sequential
   (mu/assoc models/fuel-surcharge :fuel-surcharge/history fuel-surcharge-history-schema)])

(defn-spec get-fuel-surcharges-history (m/validator get-fuel-surcharges-history-schema)
  []
  (let [history (get-last-year-fuel-surcharges)]
    (->> (get-fuel-surcharges)
         (map #(assoc % :fuel-surcharge/history (filter (comp #{(:fuel-surcharge/id %)} :fuel-surcharge/id) history)))
         (#(models/strip-keys % get-fuel-surcharges-history-schema)))))

(comment (mp/provide (get-current-fuel-surcharge-table-rows 3)))
(comment (mp/provide (take 2 (get-fuel-surcharges-history))))
(comment (mp/provide (take 2 (get-last-year-fuel-surcharges))))
(comment (m/explain fuel-surcharge-table-rows-schema (get-current-fuel-surcharge-table-rows 3)))
(comment (m/explain market-prices-list-schema (get-last-year-market-prices)))
(comment (mp/provide (take 2 (get-last-year-fuel-surcharges))))
(comment (m/explain fuel-surcharge-history-schema (take 2 (get-last-year-fuel-surcharges))))

;; side-effecting create functions

(defn insert-fuel-surcharge! [m]
  (db/insert! :fuel-surcharge m))

(comment (insert-fuel-surcharge!
           {:fuel-surcharge/market-id    "heyj"
            :fuel-surcharge/name         "Test"
            :fuel-surcharge/source-url   "Source-url"
            :fuel-surcharge/company-name "My company"}))

(defn insert-fuel-surcharge-table! [m]
  (db/insert! :fuel-surcharge-table m))

(defn delete-fuel-surcharge! [id]
  (db/delete! :fuel-surcharge id))

(defn delete-fuel-surcharge-table! [id]
  (db/delete! :fuel-surcharge-table id))

(defn delete-fuel-surcharge-table-rows! [table-id]
  (db/delete! :fuel-surcharge-table-row :fuel-surcharge-table-row/fuel-surcharge-table-id table-id))

(defn insert-fuel-surcharge-table-row! [m]
  (db/insert! :fuel-surcharge-table-row m))

(defn insert-fuel-surcharge-table-rows! [rows]
  (db/insert-many! :fuel-surcharge-table-row rows))

(comment (db/execute! (delete! :fuel-surcharge 11)))
(comment (def x (-> (query/find-by! :fuel-surcharge-table/id 9)
                    (changeset/cast :fuel-surcharge-table)
                    (dissoc :fuel-surcharge-table/id :fuel-surcharge-table/created-at))))
(comment (db/insert! :fuel-surcharge-table x))
(comment (query/all! :fuel-surcharge-table))
(comment (gm/find d :fuel-surcharge-table))
(comment (delete-fuel-surcharge-table! 10))
(comment (def x (-> (query/find-by! :fuel-surcharge-table-row/fuel-surcharge-table-id 9)
                    (changeset/cast :fuel-surcharge-table-row)
                    (dissoc :fuel-surcharge-table-row/id :fuel-surcharge-table-row/created-at))))
(comment (delete-fuel-surcharge-table-rows! 10))
