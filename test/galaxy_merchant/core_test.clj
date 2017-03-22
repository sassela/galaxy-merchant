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
                          "glob is I" {:unit :glob, :numeral-value "I"}
                          "prok  is V" {:unit :prok, :numeral-value "V"}
                          "pish is X " {:unit :pish, :numeral-value "X"}
                          " tegj is L" {:unit :tegj, :numeral-value "L"})))

(->> (test/check `c/parse-wares->value) test/summarize-results)
(deftest parse-wares->value-test
  (testing "Given a valid string, returns a map representing the wares->value query"
    (are [input expected] (= expected (c/parse-wares->value input))
                          "glob glob silver is 34 credits" {:units [:glob :glob]
                                                            :metal :silver
                                                            :value 34}
                          "glob prok gold is 57800 credits " {:units [:glob :prok]
                                                              :metal :gold
                                                              :value 57800}
                          "pish pish iron is 3910 credits" {:units [:pish :pish]
                                                            :metal :iron
                                                            :value 3910}
                          "higgledeypop silver is 01 credits" {:units [:higgledeypop]
                                                               :metal :silver
                                                               :value 1})))

; these generated tests take a while to run 1000, limiting to 100
(-> `c/set-unit->numeral-value
  (test/check {:clojure.spec.test.check/opts {:num-tests 100}})
  test/summarize-results)
(deftest set-unit->numeral-value-test
  (testing "Updates a given map with the unit->numeral conversion"
    (are [input expected] (= expected (c/set-unit->numeral-value {} input))
                          {:unit :glob, :numeral-value "I"} {:unit-vals {:glob "I"}}
                          {:unit :prok, :numeral-value "V"} {:unit-vals {:prok "V"}}
                          {:unit :pish, :numeral-value "X"} {:unit-vals {:pish "X"}}
                          {:unit :tegj, :numeral-value "L"} {:unit-vals {:tegj "L"}})))

(-> `c/set-wares->value
  (test/check {:clojure.spec.test.check/opts {:num-tests 100}})
  test/summarize-results)

(deftest set-wares->value-test
  (testing "Updates a given map with the metal->value conversion"
    (let [db {:unit-vals {:glob "I"
                          :prok "V"
                          :pish "X"
                          :tegj "L"}}]
      (are [input expected] (= expected (c/set-wares->value db input))
                            {:units [:glob :glob]
                             :metal :silver
                             :value 34}
                            (merge db {:metal-vals {:silver 17}})

                            {:units [:glob :prok]
                             :metal :gold
                             :value 57800}
                            (merge db {:metal-vals {:gold 14450}})

                            {:units [:pish :pish]
                             :metal :iron
                             :value 3910}
                            (merge db {:metal-vals {:iron 391/2}})

                            {:units [:non :existent]
                             :metal :iron
                             :value 10}
                            db))))

(-> `c/wares->value
  (test/check {:clojure.spec.test.check/opts {:num-tests 100}})
  test/summarize-results)
(deftest wares->value-test
  (testing "Updates a given map with the metal unit->value conversion"
    (let [db {:unit-vals  {:glob "I"
                           :prok "V"
                           :pish "X"
                           :tegj "L"}
              :metal-vals {:iron   391/2
                           :gold   14450
                           :silver 17}}]
      (are [input expected] (= expected (c/wares->value db input))
                            {:units [:pish :tegj :glob :glob]} 42

                            {:units [:glob :prok], :metal :silver} 68

                            {:units [:glob :prok], :metal :gold} 57800

                            {:units [:glob :prok], :metal :iron} 782

                            {:units [:non :existent]} nil))))