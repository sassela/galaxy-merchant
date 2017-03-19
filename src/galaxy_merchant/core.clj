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
    (s/and string? #(re-matches NUMERAL_REG %))
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

(s/def ::value (s/and (s/or :int int? :double double?) pos?)) ;;TODO allow ratio?

(s/fdef numeral->value
        :args (s/cat :input ::roman-numeral)
        :ret (s/and int? pos?))

;; user input
(def NUMERAL_VALUE_REG #"^\S+\sis\s\S+")
(def METAL_VALUE_REG #"^\S+\s(silver|gold|iron)\sis\s\d+\scredit(s)?")
(def CONVERT_NUMERAL_REG #"^(how much)\sis\s((a|b|c)\s)+(\s)?\?")
(def CONVERT_METAL_REG #"^(how many credits is)\s((a|b|c)\s)+(silver|gold|iron)(\s)?\?")
(def metals #{"silver" "gold" "iron"})

(def numeral-gen
  #(gen/fmap (fn [[s1 s2]] (str s1 " is " s2))
             (gen/tuple (s/gen ::nblank-str) (s/gen ::roman-numeral))))

(def wares-gen
  #(gen/fmap (fn [[s1 s2 s3]] (str s1 " " s2 " is " s3 " credits"))
             (gen/tuple (s/gen ::nblank-str) (s/gen metals) (s/gen ::value))))

(def convert-numeral-gen
  #(gen/fmap (fn [s1] (str "how much is " s1 "?"))
             (s/gen ::nblank-str)))

(def convert-metal-gen
  #(gen/fmap (fn [[s1 s2]] (str "how many credits is " s1 " " s2 "?"))
             (gen/tuple (s/gen ::nblank-str) (s/gen metals))))

(s/def ::user-input
  (s/with-gen
    string?
    #(let [gens [(numeral-gen)
                 (wares-gen)
                 (convert-numeral-gen)
                 (convert-metal-gen) 
                 (s/gen ::nblank-str)]]
       (gen/fmap (fn [coll] (nth coll (rand-int (count gens))))
                 (apply gen/tuple gens)))))



(s/fdef parse-instruction
        :args (s/cat :input ::user-input)
        :ret fn?)

(defn mapfirst
  "Map for thread first"
  [coll fn]
  (map fn coll))

(s/def ::units (s/coll-of keyword?))
(s/def ::numeral-value ::roman-numeral)
(s/def ::metals (s/coll-of (->> metals (map keyword) set)))

(defn parse-unit->numeral-value
  [input]
  (let [[unit numeral-value] (-> input (str/split #" is ") (mapfirst str/trim))]
    {:units (-> unit keyword vector) :numeral-value numeral-value}))

(s/fdef parse-unit->numeral-value
        :args (s/cat :input (s/with-gen ::nblank-str numeral-gen))
        :ret (s/keys :req-un [::units ::numeral-value]))

(defn parse-wares->value
  [input]
  (let [[wares numeral-value] (-> input (str/split #"( is | credits)") (mapfirst str/trim))
        [metal & units] (reverse (str/split wares #"\s+"))]
    {:units (mapv keyword (reverse units))                   ;;FIXME double reverse
     :metals (-> metal keyword vector)
     :value (read-string numeral-value)}))

(s/fdef parse-wares->value
        :args (s/cat :input (s/with-gen ::nblank-str wares-gen))
        :ret (s/keys :req-un [::units ::metals ::value]))

;; conversion

(s/def ::metal-vals (s/and map? (fn [m] (every? #(s/valid? ::value %) (vals m)))))
(s/def ::unit-vals (s/and map? (fn [m] (every? #(s/valid? ::roman-numeral %) (vals m)))))

(s/def ::conversion-values
  (s/keys :opt [::metal-vals ::unit-vals]))

(s/fdef set-unit->numeral-value
        :args (s/cat :units (s/keys :req-un [::units ::numeral-value])
                     :conversion-values ::conversion-values)
        :ret ::conversion-values)

(s/fdef set-wares->value
        :args (s/cat :wares (s/keys :req-un [::metals ::value]
                                    :opt-un [::units])
                     :conversion-values ::conversion-values)
        :ret ::conversion-values)
