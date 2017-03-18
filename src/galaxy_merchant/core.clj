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
(def ^:const NUMERAL_REG #"^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")
(def ^:const SYMBOL_VALUES {:M 1000, :V 5, :I 1, :L 50, :X 10, :C 100, :D 500})

(s/def ::numeral-ks
  (->> SYMBOL_VALUES keys set))

(s/def ::numeral-chars
  (->> SYMBOL_VALUES keys (map name) set))

(s/def ::roman-numeral
  (s/with-gen
    #(re-matches NUMERAL_REG %)
    #(gen/fmap str/join (gen/vector (s/gen ::numeral-chars) 1 10))))

(defn string->keywords
  "Given a string, returns a collection of keywords corresponding to characters"
  [s]
  (->> s str/upper-case (re-seq #"[\S]") (map keyword)))

(s/def ::nblank-str (s/and string? (complement str/blank?)))

(s/fdef string->keywords
        :args (s/cat :input ::nblank-str)
        :return (s/coll-of keyword?)
        :fn #(= (count (-> % :args :input)) (count (:ret %))))

(defn numeral->value
  "Given a single roman numeral, returns its value"
  [numeral]
  (letfn [(running-val [prev val value]
            (if (and prev (< prev val)) (+ (- value (* 2 prev)) val) (+ value val)))]
    (loop [[head & tail] (string->keywords numeral) value 0 prev nil]
    (if (nil? head)
      value
      (let [val (head SYMBOL_VALUES)]
        (recur tail (running-val prev val value) val))))))

(s/fdef numeral->value
        :args (s/cat :input ::roman-numeral)
        :ret int?)

;; user input
(def NUMERAL_VALUE_REG #"^\S+\sis\s\S+")
(def METAL_VALUE_REG #"^\S+\s(silver|gold|iron)\sis\s\d+\scredit(s)?")
(def CONVERT_NUMERAL_REG #"^(how much)\sis\s((a|b|c)\s)+(\s)?\?")
(def CONVERT_METAL_REG #"^(how many credits is)\s((a|b|c)\s)+(silver|gold|iron)(\s)?\?")

(def numeral-gen
  #(gen/fmap (fn [[s1 s2]] (str s1 " is " s2))
             (gen/tuple (s/gen ::nblank-str) (s/gen ::nblank-str))))

(def metal-gen
  #(gen/fmap (fn [[s1 s2 s3]] (str s1 " " s2 " is " s3 " credits"))
             (gen/tuple (s/gen ::nblank-str) (s/gen #{"silver" "gold" "iron"}) (s/gen pos-int?))))

(def convert-numeral-gen
  #(gen/fmap (fn [s1] (str "how much is " s1 "?"))
             (s/gen ::nblank-str)))

(def convert-metal-gen
  #(gen/fmap (fn [[s1 s2]] (str "how many credits is " s1 " " s2 "?"))
             (gen/tuple (s/gen ::nblank-str) (s/gen #{"silver" "gold" "iron"}))))

(s/def ::user-input
  (s/with-gen
    string?
    #(let [gens [(numeral-gen) (metal-gen) (convert-numeral-gen) (convert-metal-gen) (s/gen ::nblank-str)]]
       (gen/fmap (fn [v] (nth v (rand-int (count gens))))
                 (apply gen/tuple gens)))))(s/fdef parse-input

(s/fdef parse-instruction
        :args (s/cat :input ::user-input)
        :ret fn?)