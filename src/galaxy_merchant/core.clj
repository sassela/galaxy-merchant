(ns galaxy-merchant.core
  (:gen-class)
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.string :as str]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;; roman numerals
(def ^:const NUMERAL_REGEX #"^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")
(def ^:const SYMBOL_VALUES {:M 1000, :V 5, :I 1, :L 50, :X 10, :C 100, :D 500})

(s/def ::numeral-ks
  (->> SYMBOL_VALUES keys set))

(s/def ::numeral-chars
  (->> SYMBOL_VALUES keys (map name) set))

(s/def ::roman-numeral
  (s/with-gen #(re-matches NUMERAL_REGEX %) #(gen/fmap str/join (gen/vector (s/gen ::numeral-chars) 1 10))))


(s/fdef string->keywords
        :args (s/cat :input (s/and string? (complement str/blank?)))
        :return (s/coll-of keyword?)
        :fn #(= (count (-> % :args :input)) (count (:ret %))))


(s/fdef numeral->value
        :args (s/cat :input ::roman-numeral)
        :ret int?)