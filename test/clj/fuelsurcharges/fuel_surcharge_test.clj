(ns fuelsurcharges.fuel-surcharge-test
  (:require   [clojure.spec.test.alpha :as stest]
              [fuelsurcharges.test-util :refer [check]]
              [clojure.test :refer :all]
              [orchestra.spec.test :as st]
              [fuelsurcharges.fuel-surcharge :refer :all]))

(deftest fuel-surcharge-test
  (testing "Check function specs"
    (is (true? (check `get-fuel-surcharges 1)))
    (is (true? (check `get-fuel-surcharges-history 1)))
    (is (true? (check `get-last-year-fuel-surcharges 1)))))

;; TODO: make this into a util
(comment (doall (map #(do (println %)
                          (check % 1)) (stest/enumerate-namespace 'fuelsurcharges.fuel-surcharge))))
