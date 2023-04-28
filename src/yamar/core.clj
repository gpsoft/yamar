(ns yamar.core
  (:require
   [clojure.pprint :as pp]
   [net.cgrand.enlive-html :as en]
   [yamar.scrape :as scrape]
   [yamar.render :as render]))

(def ^:private yamap-url-base "https://yamap.com")
(def ^:private limit-page-no 10)

(defn- index-url
  ([user-id]
   (index-url user-id 1))
  ([user-id page-no]
   (str yamap-url-base "/users/" user-id "?page=" page-no)))

(defn- act-url
  [act-id]
  (str yamap-url-base "/activities/" act-id))

(defn- fetch!
  [url]
  #_(let [html-raw (slurp url)
          html-nodes (en/html-snippet html-raw)]
      [html-raw html-nodes])
  (en/html-resource (java.net.URL. url)))

(defn- activity
  [act]
  (let [act-id (scrape/activity-id act)
        [act-date year] (scrape/act-date act)]
    {:activity-id act-id
     :activity-url (act-url act-id)
     :thumbnail-url (scrape/thumbnail-url act)
     :num-photos (scrape/num-photos act)
     :elapse (scrape/elapse act)
     :distance (scrape/distance act)
     :altitude (scrape/altitude act)
     :heading (scrape/heading act)
     :act-date act-date
     :year year}))

(defn- progress
  [s]
  (println s))

(defn go! [user-id]
  (loop [page-no 1
         act-list []]
    (progress (str "Fetching page #" page-no "..."))
    (let [page (-> user-id
                   (index-url page-no)
                   (fetch!))
          max-page-no (min (scrape/max-page-no page) limit-page-no)
          act-list (into act-list (map activity (scrape/activity-list page)))]
      (if (>= page-no max-page-no)
        act-list
        (do
         (progress (str "More pages to go(max will be #" max-page-no ")"))
         (recur (inc page-no) act-list))))))

(defn- resolve-args
  [args]
  (if (nil? args) nil
    [(first args) (nth args 1 "./")]))

(defn- show-usage
  []
  (println "usage: yamar USERID [DESTINATION]"))

(defn -main [& args]
  (if-let [[user-id dest] (resolve-args args)]
    (let [act-list (go! user-id)]
      (spit (str dest user-id ".html")
            (render/render act-list)))
    (show-usage)))

(comment

 (index-url 1764261)

 (def page (let [url (index-url 1764261)]
             (fetch! url)))

 (map scrape/activity-id (scrape/activity-list page))
 (map scrape/thumbnail-url (scrape/activity-list page))
 (map scrape/num-photos (scrape/activity-list page))
 (map scrape/elapse (scrape/activity-list page))
 (map scrape/distance (scrape/activity-list page))
 (map scrape/altitude (scrape/activity-list page))
 (map scrape/heading (scrape/activity-list page))
 (map scrape/act-date (scrape/activity-list page))
 (map activity (scrape/activity-list page))

 (scrape/max-page-no page)

 (def act-list (go! 1764261))

 (pp/pprint act-list)
 (spit "index.html" (render/render act-list))


 )
