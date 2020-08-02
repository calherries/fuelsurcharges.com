(ns fuelsurcharges.test-util
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]
            [expound.alpha :as expound]))

(defn check
  "Passes sym to stest/check with a :max-size of 3 (generated sequences will have no
  more than 3 elements, returning true if the test passes or the explained error if not"
  ([sym]
   (check 25))
  ([sym num-tests]
   (let [check-result (stest/check sym {:clojure.spec.test.check/opts {:num-tests num-tests
                                                                       :max-size  3}})
         result       (-> check-result
                          first ;; stest/check accepts a variable number of syms, this doesn't
                          :clojure.spec.test.check/ret
                          :result)]
     (when-not (true? result)
       (expound/explain-results check-result))
     result)))
