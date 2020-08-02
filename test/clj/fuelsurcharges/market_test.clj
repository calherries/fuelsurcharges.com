(ns fuelsurcharges.market-test
  (:require   [clojure.spec.test.alpha :as stest]
              [fuelsurcharges.test-util :refer [check]]
              [clojure.test :refer :all]
              [fuelsurcharges.market :as market]))

(deftest market-test
  (testing "Check function specs"
    (is (true? (check `market/markets-list 1)))
    (is (true? (check `market/get-last-year-market-prices 1)))))
