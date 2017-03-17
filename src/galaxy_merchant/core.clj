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
(def ^:const SYMBOLS {:M 1000, :V 5, :I 1, :L 50, :X 10, :C 100, :D 500})

(def nblank? (complement str/blank?))

(s/def ::numeral-ks
  (->> SYMBOLS keys set))

(s/def ::numeral-strs
  (->> SYMBOLS keys (map name) set))

(s/def ::numeral-symbol
  (s/with-gen nblank? #(gen/fmap str/join (s/gen (s/+ ::numeral-strs)))))

(s/def ::roman-numeral (s/and ::numeral-symbol #(re-matches NUMERAL_REGEX %)))

(s/fdef keywords
        :args ::numeral-symbol
        :return (s/coll-of ::numeral-ks))

(s/fdef numeral->value
        :args ::roman-numeral
        :ret nat-int?)