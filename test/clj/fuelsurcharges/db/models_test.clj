(ns fuelsurcharges.db.models-test
  (:require
   [clojure.spec.test.alpha :as stest]
   [fuelsurcharges.test-util :refer [check]]
   [clojure.test :refer :all]
   [gungnir.model :as gmodel]
   [malli.core :as m]
   [fuelsurcharges.db.models :refer [register-models] :as models]))

(defn validate-model [model-name]
  (let [model (model-name @gmodel/models)]
    (m/validate [:sequential model] (models/select-all-namespaced model-name))))

(comment (every? identity (map validate-model (keys @gmodel/models))))
(comment (validate-model (first (keys @gmodel/models))))

(deftest models-test
  (testing "Validate all registered model definitions"
    (is (every? identity (map validate-model (keys @gmodel/models))))))
