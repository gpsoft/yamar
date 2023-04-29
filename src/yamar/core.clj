(ns yamar.core
  (:require
   [clojure.pprint :as pp]
   [net.cgrand.enlive-html :as en]
   [yamar.scrape :as scrape]
   [yamar.render :as render]
   [yamar.util :as u]))

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

(defn scrape! [user-id]
  (loop [page-no 1
         act-list []]
    (progress (str "Fetching page #" page-no "..."))
    (let [page (-> user-id
                   (index-url page-no)
                   (fetch!))
          max-page-no (scrape/max-page-no page)
          act-list (into act-list (map activity (scrape/activity-list page)))]
      (if (>= page-no (min max-page-no limit-page-no))
        (do
         (when (> max-page-no limit-page-no)
           (progress "Aborting; too much pages"))
         {:user-id user-id
          :user-name (scrape/user-name page)
          :activities act-list})
        (do
         (progress (str "More pages to go(max will be #" max-page-no ")"))
         (recur (inc page-no) act-list))))))

(defn go! [user-id dest]
  (let [ar (scrape! user-id)
        edn-file (u/resolve-path dest (str user-id ".edn"))
        html-file (u/resolve-path dest (str user-id ".html"))]
    (progress (str "Saving edn: " edn-file))
    (u/write-edn! edn-file ar)
    (progress (str "Saving html " html-file))
    (spit html-file
          (render/render ar))))

(defn- resolve-args
  [args]
  (if (nil? args) nil
    [(first args) (nth args 1 "./")]))

(defn- show-usage
  []
  (println "usage: yamar USERID [DESTINATION]"))

(defn -main [& args]
  (if-let [[user-id dest] (resolve-args args)]
    (go! user-id dest)
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
 (scrape/user-name page)

 (def ar (scrape! 1764261))

 (pp/pprint ar)
 (spit "index.html" (render/render (:activities ar)))

 (let [ar (u/read-edn! "docs/1764261.edn")]
   (->> ar
        (render/render)
        (spit "index.html")))

 )
