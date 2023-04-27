(ns yamar.core
  (:require
   [clojure.pprint :as pp]
   [net.cgrand.enlive-html :as en]
   [yamar.scrape :as scrape]))

(def ^:private yamap-url-base "https://yamap.com")

(defn- index-url
  ([user-id]
   (index-url user-id 1))
  ([user-id page-no]
   (str yamap-url-base "/users/" user-id "?page=" page-no)))

(defn- act-url
  [act-id]
  (str yamap-url-base "/activities/" act-id))

(defn- fetch
  [url]
  #_(let [html-raw (slurp url)
          html-nodes (en/html-snippet html-raw)]
      [html-raw html-nodes])
  (en/html-resource (java.net.URL. url)))

(defn- activity
  [act]
  (let [act-id (scrape/activity-id act)]
    {:activity-id act-id
     :activity-url (act-url act-id)
     :thumbnail-url (scrape/thumbnail-url act)
     :num-photos (scrape/num-photos act)
     :elapse (scrape/elapse act)
     :distance (scrape/distance act)
     :altitude (scrape/altitude act)
     :heading (scrape/heading act)
     :date (scrape/act-date act)}))

(defn go! [user-id]
  (let [url (index-url user-id)
        page (fetch url)
        act-nodes (scrape/activity-list page)
        acts (map activity act-nodes)]
    (pp/pprint acts)))

(defn -main [& args]
  (prn args)
  (go! 1764261))

(comment

 (index-url 1764261)

 (def page (let [url (index-url 1764261)]
              (fetch url)))

 (map scrape/activity-id (scrape/activity-list page))
 (map scrape/thumbnail-url (scrape/activity-list page))
 (map scrape/num-photos (scrape/activity-list page))
 (map scrape/elapse (scrape/activity-list page))
 (map scrape/distance (scrape/activity-list page))
 (map scrape/altitude (scrape/activity-list page))
 (map scrape/heading (scrape/activity-list page))
 (map scrape/act-date (scrape/activity-list page))
 (map activity (scrape/activity-list page))

 )
