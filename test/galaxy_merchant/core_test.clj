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

(->> (test/check `c/parse-unit->numeral-value) test/summarize-results)
(deftest parse-unit->numeral-value-test
  (testing "Given a valid string, returns a map representing the unit->numeral-value query"
    (are [input actual] (= actual (c/parse-unit->numeral-value input))
                        "glob is I" {:units [:glob], :numeral-value "I"}
                        "prok  is V" {:units [:prok], :numeral-value "V"}
                        "pish is X " {:units [:pish], :numeral-value "X"}
                        " tegj is L" {:units [:tegj], :numeral-value "L"})))

(->> (test/check `c/parse-wares->value) test/summarize-results)
(deftest parse-wares->value-test
  (testing "Given a valid string, returns a map representing the wares->value query"
    (are [input actual] (= actual (c/parse-wares->value input))
                        "glob glob Silver is 34 Credits" {:units [:glob :glob]
                                                          :metal [:silver]
                                                          :value 34}
                        "glob prok Gold is 57800 Credits " {:units [:glob :prok]
                                                            :metal [:gold]
                                                            :value 57800}
                        "pish pish Iron is 3910 Credits" {:units [:pish :pish]
                                                          :metal [:iron]
                                                          :value 3910}
                        "higgledeypop silver is 01 Credits" {:units [:higgledeypop]
                                                             :metal [:lead]
                                                             :value 1})))
