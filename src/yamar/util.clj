(ns yamar.util
  (:require
   [clojure.pprint :as pp]
   [clojure.java.io :as io]
   [clojure.edn :as edn])
  (:import
   [java.awt Desktop]
   [java.net URI]))

(defn tap! [v] (pp/pprint v) v)

(defn rev-sort
  ([coll]
   (sort #(compare %2 %1) coll))
  ([keyfn coll]
   (sort-by keyfn #(compare %2 %1) coll)))

(defn resolve-path
  [base-str part-str]
  (let [d (io/file base-str)]
    (-> d
        (.toPath)
        (.resolve part-str)
        (.toString))))

(defn read-edn!
  [fpath-str]
  (if (.exists (io/file fpath-str))
    (-> fpath-str
        (slurp)
        (edn/read-string))
    {}))

(defn write-edn!
  [fpath-str data]
  (let [text (with-out-str (pp/pprint data))]
    (spit fpath-str text)))

(defn read-resource!
  [path-str]
  (-> (io/resource path-str)
      slurp))

(defn open-browser!
  [url-str]
  (let [desktop (Desktop/getDesktop)
        uri (new URI url-str)]
    (.browse desktop uri)))
