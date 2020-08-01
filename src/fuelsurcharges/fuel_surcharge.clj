(ns fuelsurcharges.fuel-surcharge
  (:require   [gungnir.query :as q]
              [gungnir.database :as gd]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [gungnir.model :as model]
              [malli.core :as m]
              [malli.provider :as mp]
              [honeysql.core :as sql]
              [malli.util :as mu]
              [fuelsurcharges.db.core :as db]
              [gungnir.record :refer [model table]]
              [fuelsurcharges.db.models :refer [register-models] :as models]
              [java-time :as t]))

(def fuel-surcharge-schema)

(comment (mp/provide (db/get-current-fuel-surcharge-table-rows {:id 3})))

;; -- :name get-current-fuel-surcharge-table-rows :? :*
;; -- :doc gets the current fuel-surcharge-table rows given the fuel surcharge id
;; select
;; r.price
;; , r.surcharge_amount
;; from fuel_surcharge f
;; join fuel_surcharge_table t on f.id = t.fuel_surcharge_id
;; join fuel_surcharge_table_row r on t.id = r.fuel_surcharge_table_id
;; where f.id = :id
