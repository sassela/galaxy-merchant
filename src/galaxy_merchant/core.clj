(ns galaxy-merchant.core
  (:gen-class)
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.string :as str]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def ^:const NUMERAL_REGEX #"^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")

(def nblank? (complement str/blank?))

(s/def ::numeral-symbol
  (s/with-gen nblank? #(gen/fmap str/join (s/gen (s/+ #{"M" "V" "I" "L" "X" "C" "D"})))))

(s/def ::roman-numeral (s/and ::numeral-symbol #(re-matches NUMERAL_REGEX %)))
