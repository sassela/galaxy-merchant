(ns galaxy-merchant.core
  (:gen-class)
  (:require [clojure.set :as set]
            [clojure.spec :as s]
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
        :return (s/+ keyword?)
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

(s/def ::value (s/or :int? (s/and int? pos?)
                     :double? (s/double-in :infinite? false :Nan? false :min 1)
                     :ratio (s/and ratio? pos?)))           ;;TODO tidy up. (s/and pos?) does weird things

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

;;parse input, update map
;;if undefined, 3a get
;;if no key, add
;;no dupe vals either

(defn parse-instruction
  [input]
  ;;TODO provide fn map?
  (let [res (condp re-matches (-> input str/trim str/lower-case)
              NUMERAL_VALUE_REG #(prn {:a "intergal"})
              METAL_VALUE_REG #(prn {:metals :b})
              CONVERT_NUMERAL_REG #(prn {:q "nums"})
              CONVERT_METAL_REG #(prn {:q "metal"})
              #(prn {:a "I have no idea what you're talking about"}))]
    (prn "input " input " res: " res)
    res))

(s/fdef parse-instruction
        :args (s/cat :input ::user-input)
        :ret fn?)

(defn mapfirst
  "Map for thread first"
  [coll fn]
  (map fn coll))

(s/def ::unit (s/and keyword? #(s/valid? ::nblank-str (name %))))
(s/def ::units (s/+ ::unit))
(s/def ::numeral-value ::roman-numeral)
(s/def ::metal (->> metals (map keyword) set))
(s/def ::metals (s/coll-of ::metal :distinct true :min-count 1))

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

(s/def ::metal-vals (s/map-of ::metal ::value :min-count 1))
(s/def ::unit-vals (s/map-of keyword? ::roman-numeral :min-count 1))
(s/def ::conversion-values (s/keys :req-un [::metal-vals ::unit-vals]))

(defn set-unit->numeral-value
  [db {:keys [unit numeral-value] :as unit-vals}]
  (assoc-in db [:unit-vals unit] numeral-value))

(s/fdef set-unit->numeral-value
        :args (s/cat :conversion-values ::conversion-values
                     :input (s/keys :req-un [::unit ::numeral-value]))
        :ret ::conversion-values
        :fn #(assoc-in %
                       [:ret :unit-vals (-> % :args :input ::unit)]
                       (-> % :args :input ::numeral-value)))

(defn units->value
  [db units]
  (if (empty? units)
    1
    (->> units (map #(get-in db [:unit-vals %] "")) str/join numeral->value)))

;;FIXME fn spec gen if time
;(s/and
;  (s/cat :conversion-values ::conversion-values
;    :units (s/coll-of keyword?))
;  #(set/subset? (-> % :units set) (-> % :conversion-values :unit-vals :keys)))

(s/fdef units->value
  :args (s/cat :conversion-values ::conversion-values
          :units (s/coll-of keyword?))
  :ret ::value)

(defn set-wares->value
  [db {:keys [metal units value] :as values}]
  (if (set/subset? (set units) (->> db :unit-vals keys set))
    (let [divisor (units->value db units)]
      (assoc-in db [:metal-vals metal] (/ value divisor)))
    (do
      (prn "ERROR: conversion units do not exist. Please set them first.")
      db)))

;;FIXME :fn spec gen if time
;(defn wares-convertable?
;  [args]
;  (set/subset? (->> args :wares :units set) (->> args :conversion-values :unit-vals keys set)))
;
;(defn wares-converted?
;  [fn-vals]
;  (contains? (->> fn-vals :ret :metal-vals keys set) (->> fn-vals :args :wares :metal)))

;(s/def ::wares->value-args
;  (s/with-gen
;    (s/cat :conversion-values ::conversion-values
;      :wares (s/keys :req-un [::units ::metal ::value]))
;    (gen/fmap (s/gen ::unit-vals))))

(s/fdef set-wares->value
  :args (s/cat :conversion-values ::conversion-values
          :wares (s/keys :req-un [::metal ::value]
                   :opt-un [::units]))
  :ret ::conversion-values)

(s/fdef wares->value
  :args (s/cat :conversion-values ::conversion-values
          :wares (s/keys :req-un [::metal ::value]
                   :opt-un [::units]))
  :ret (s/nilable ::value))