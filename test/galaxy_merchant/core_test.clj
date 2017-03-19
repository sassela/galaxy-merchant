(ns galaxy-merchant.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test :as test]
            [galaxy-merchant.core :as c]))

(->> (test/check `c/string->keywords) test/summarize-results)
(deftest string->keywords-test
  (testing "Given a string, returns a collection of keywords corresponding to characters"
    (are [input expected] (= expected (c/string->keywords input))
                          "X" '(:X)
                          " DCCI " '(:D :C :C :I)
                          "mccix" '(:M :C :C :I :X)
                          "5" '(:5))))

(->> (test/check `c/numeral->value) test/summarize-results)
(deftest numeral->value-test
  (testing "Given a single roman numeral, returns its value"
    (are [input expected] (= expected (c/numeral->value input))
                          "X" 10
                          " DCCI " 701
                          "MMV" 2005
                          "mccix" 1209)))

(->> (test/check `c/parse-unit->numeral-value) test/summarize-results)
(deftest parse-unit->numeral-value-test
  (testing "Given a valid string, returns a map representing the unit->numeral-value query"
    (are [input expected] (= expected (c/parse-unit->numeral-value input))
                          "glob is I" {:units [:glob], :numeral-value "I"}
                          "prok  is V" {:units [:prok], :numeral-value "V"}
                          "pish is X " {:units [:pish], :numeral-value "X"}
                          " tegj is L" {:units [:tegj], :numeral-value "L"})))

(->> (test/check `c/parse-wares->value) test/summarize-results)
(deftest parse-wares->value-test
  (testing "Given a valid string, returns a map representing the wares->value query"
    (are [input expected] (= expected (c/parse-wares->value input))
                          "glob glob silver is 34 credits" {:units  [:glob :glob]
                                                            :metals [:silver]
                                                            :value  34}
                          "glob prok gold is 57800 credits " {:units  [:glob :prok]
                                                              :metals [:gold]
                                                              :value  57800}
                          "pish pish iron is 3910 credits" {:units  [:pish :pish]
                                                            :metals [:iron]
                                                            :value  3910}
                          "higgledeypop silver is 01 credits" {:units  [:higgledeypop]
                                                               :metals [:silver]
                                                               :value  1})))

(->> (test/check `c/set-unit->numeral-value) test/summarize-results)
(deftest set-unit->numeral-value-test
  (testing "Updates a given map with the unit->value conversion"
    (are [input expected] (= expected (c/set-unit->numeral-value {} input))
                          {:units [:glob], :numeral-value "I"} {:glob 1}
                          {:units [:prok], :numeral-value "V"} {:prok 5}
                          {:units [:pish], :numeral-value "X"} {:pish 10}
                          {:units [:tegj], :numeral-value "L"} {:tegj 50})))

(->> (test/check `c/set-wares->value) test/summarize-results)
(deftest parse-wares->value-test
  (testing "Updates a given map with the metal unit->value conversion"
    (let [db {::c/unit-vals {:glob 1
                            :prok 5
                            :pish 10
                            :tegj 50}}]
      (are [input expected] (= expected (c/set-wares->value db input))
                            {:units  [:glob :glob]
                             :metals [:silver]
                             :value  34}
                            (merge db {::c/metal-vals {:silver 17}})

                            {:units  [:glob :prok]
                             :metals [:gold]
                             :value  57800}
                            (merge db {::c/metal-vals {:gold 14450}})

                            {:units  [:pish :pish]
                             :metals [:iron]
                             :value  3910}
                            (merge db {::c/metal-vals {:iron 195.5}})))))
