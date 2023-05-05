(ns yamar.util
  (:require
   [clojure.pprint :as pp]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [lambdaisland.uri :as uri])
  (:import
   [java.awt Desktop]
   [java.net URI]
   [java.io File]))

(defn tap! [v] (pp/pprint v) v)

(defn to-int
  [s]
  (Integer. (re-find #"\d+" s)))

(defn pad00
  [s]
  (->> s
       to-int
       (format "%02d")))

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

(defn mkdir
  [path-str]
  (-> path-str
      (File.)
      (.mkdirs)))

(defn read-edn!
  [fpath-str not-found]
  (if (.exists (io/file fpath-str))
    (-> fpath-str
        (slurp)
        (edn/read-string))
    not-found))

(defn write-edn!
  [fpath-str data]
  (let [text (with-out-str (pp/pprint data))]
    (spit fpath-str text)))

(defn read-resource!
  [path-str]
  (-> (io/resource path-str)
      slurp))

(defn wget
  [url-str fpath-str]
  (try
   (with-open [in (io/input-stream url-str)
               out (io/output-stream fpath-str)]
     (io/copy in out)
     url-str)
   (catch Exception e nil)))

(defn open-browser!
  [url-str]
  (let [desktop (Desktop/getDesktop)
        uri (new URI url-str)]
    (.browse desktop uri)))

(defn split-url
  [url-str]
  (let [u (uri/uri url-str)
        q-map (uri/query-map u)
        f (:fragment u)]
    [(uri/uri-str (assoc u :query nil :fragment nil)) q-map f]))

(defn join-url
  ([base q-map] (join-url base q-map nil))
  ([base q-map f]
   (let [q (uri/map->query-string q-map)
         u (uri/uri base)]
     (-> u
         (uri/assoc-query* q-map)
         (assoc :fragment f)
         uri/uri-str))))

(comment
 
 (split-url "https://yamap.com:8080/hoge-fuga?t=yes&s=no#footer")
 (join-url "https://yamap.com:8080/hoge-fuga" {:t "yes" :s "no"})
 (join-url "https://yamap.com:8080/hoge-fuga" {:t "yes" :s "no"} "footer")

 )
