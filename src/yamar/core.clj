(ns yamar.core
  (:require
   [clojure.pprint :as pp]
   [net.cgrand.enlive-html :as en]
   [yamar.scrape :as scrape]
   [yamar.render :as render]
   [yamar.util :as u])
  (:gen-class))

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

(defn- mk-activity
  [act-node]
  (let [act-id (scrape/activity-id act-node)
        [act-date year] (scrape/act-date act-node)]
    {:activity-id act-id
     :activity-url (act-url act-id)
     :thumbnail-url (scrape/thumbnail-url act-node)
     :num-photos (scrape/num-photos act-node)
     :elapse (scrape/elapse act-node)
     :distance (scrape/distance act-node)
     :altitude (scrape/altitude act-node)
     :heading (scrape/heading act-node)
     :act-date act-date
     :year year}))

(defn- progress
  [& args]
  (println (apply str args)))

(defn db!
  [edn-file]
  (progress "Reading archive from DB: " edn-file)
  (let [db (u/read-edn! edn-file)
        num-acts (count (:activities db))]
    (if (empty? db)
      (progress "No DB found")
      (progress "Found " num-acts " activities"))
    db))

(defn- append-activities
  [to-list id-set from-list]
  (let [new-act-list (remove #(id-set (:activity-id %)) from-list)]
    [(into to-list new-act-list)
     (into id-set (map :activities new-act-list))
     (< (count new-act-list) (count from-list))]))

(defn- done?
  [current-page-no max-page-no found-dup?]
  (let [done-with-all-pages (>= current-page-no max-page-no)
        too-many-pages (>= current-page-no limit-page-no)]
    (when found-dup?
      (progress "Done with new activities"))
    (when done-with-all-pages
      (progress "Done with all pages"))
    (when too-many-pages
      (progress "Aborting; too many pages"))
    (or found-dup? done-with-all-pages too-many-pages)))

(defn scrape!
  [user-id db]
  (loop [page-no 1
         act-list (get db :activities [])
         id-set (set (map :activity-id act-list))]
    (progress "Fetching page #" page-no "...")
    (let [page (-> user-id
                   (index-url page-no)
                   (fetch!))
          max-page-no (scrape/max-page-no page)
          page-act-list (map mk-activity (scrape/act-node-list page))
          [act-list id-set found-dup?]
          (append-activities act-list id-set page-act-list )]
      (if (done? page-no max-page-no found-dup?)
        {:user-id user-id
         :user-name (scrape/user-name page)
         :activities act-list}
        (do
         (progress "More pages to go(max will be #" max-page-no ")")
         (recur (inc page-no) act-list id-set))))))

(defn go! [user-id dest]
  (let [edn-file (u/resolve-path dest (str user-id ".edn"))
        html-file (u/resolve-path dest (str user-id ".html"))
        db (db! edn-file)
        ar (scrape! user-id db)
        ]
    (progress "Saving edn: " edn-file)
    (u/write-edn! edn-file ar)
    (progress "Saving html " html-file)
    (spit html-file
          (render/render ar)))
  )

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

 (map scrape/activity-id (scrape/act-node-list page))
 (map scrape/thumbnail-url (scrape/act-node-list page))
 (map scrape/num-photos (scrape/act-node-list page))
 (map scrape/elapse (scrape/act-node-list page))
 (map scrape/distance (scrape/act-node-list page))
 (map scrape/altitude (scrape/act-node-list page))
 (map scrape/heading (scrape/act-node-list page))
 (map scrape/act-date (scrape/act-node-list page))
 (map mk-activity (scrape/act-node-list page))

 (scrape/max-page-no page)
 (scrape/user-name page)

 (go! 1764261 "docs/")

 (def ar (scrape! 1764261 {}))

 (pp/pprint ar)
 (spit "index.html" (render/render (:activities ar)))

 (let [ar (u/read-edn! "docs/1764261.edn")]
   (->> ar
        (render/render)
        (spit "index.html")))

 (act-url 23886620)
 (def page (let [url (act-url 23886620)]
             (fetch! url)))
 (scrape/details page)

 )

