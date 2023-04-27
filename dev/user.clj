(ns user
  (:require
   clojure.pprint
   [yamar.core :as core]))

(defn dev
  []
  (core/go! 1764261)
  (in-ns 'yamar.core))

