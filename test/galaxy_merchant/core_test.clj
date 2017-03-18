(ns galaxy-merchant.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test :as test]
            [galaxy-merchant.core :as c]))

(->> (test/check `c/string->keywords) test/summarize-results)
(deftest string->keywords-test
  (testing "Given a string, returns a collection of keywords corresponding to characters"
    (are [input actual] (= actual (c/string->keywords input))
                        "X" '(:X)
                        " DCCI " '(:D :C :C :I)
                        "mccix" '(:M :C :C :I :X)
                        "5" '(:5))))

(->> (test/check `c/numeral->value) test/summarize-results)
(deftest numeral->value-test
  (testing "Given a single roman numeral, returns its value"
    (are [input actual] (= actual (c/numeral->value input))
                        "X" 10
                        " DCCI " 701
                        "MMV" 2005
                        "mccix" 1209)))