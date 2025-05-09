(ns yamar.core
  (:require
   [clojure.pprint :as pp]
   [clojure.tools.cli :as cli]
   [clojure.string :as str]
   [net.cgrand.enlive-html :as en]
   [yamar.scrape :as scrape]
   [yamar.render :as render]
   [yamar.util :as u])
  (:gen-class))

(def ^:private yamap-url-base "https://yamap.com")
(def ^:private limit-page-no 10)
(def ^:private fpath-index "/home/maru/Desktop/a.html")
(def ^:private fpath-details "/home/maru/Desktop/b.html")

(def ^:private cli-options
  [["-d" "--destination DIR" "Destination directory"
    :default "docs/"]
   ["-D" "--details" "Archive in details"
    :default false]
   ["-h" "--help" "Show usage"
    :default false]])

(defn- progress
  [& args]
  (println (apply str args)))

(defn- index-url
  ([user-id]
   (index-url user-id 1))
  ([user-id page-no]
   (str yamap-url-base "/users/" user-id "?page=" page-no)))

(defn- act-url
  [act-id]
  (str yamap-url-base "/activities/" act-id))

(defn- thumbnail-url
  [cover-url]
  (let [url-str (if (vector? cover-url) (first cover-url) cover-url)
        st (:st (second cover-url))
        e (:e (second cover-url))]
    (u/join-url url-str {:st st
                         :e e
                         :t "crop"
                         :w 300
                         :h 300})))

(defn- fetch!
  [fpath]
  (let [html-raw (slurp fpath)
        html-nodes (en/html-snippet html-raw)]
      html-nodes))

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

(defn db!
  [edn-file]
  (progress "Reading archive from DB: " edn-file)
  (let [db (u/read-edn! edn-file {})
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
      (progress "Done with all index pages"))
    (when too-many-pages
      (progress "Aborting; too many index pages"))
    (or found-dup? done-with-all-pages too-many-pages)))

(defn scrape!
  [fpath user-id db]
  (loop [page-no 1
         act-list (get db :activities [])
         id-set (set (map :activity-id act-list))]
    (progress "Fetching index page #" page-no "...")
    (let [page (fetch! fpath-index)
          #_(-> user-id
                (index-url page-no)
                (fetch!))
          ; max-page-no (scrape/max-page-no page)
          page-act-list (map mk-activity (scrape/act-node-list page))
          [act-list id-set found-dup?]
          (append-activities act-list id-set page-act-list )]
      (if true #_(done? page-no max-page-no found-dup?)
        {:user-id user-id
         :user-name (scrape/user-name page)
         :mypage-url (index-url user-id)
         :activities act-list}
        (do
         #_(progress "More index pages to go(max will be #" max-page-no ")")
         (recur (inc page-no) act-list id-set))))))

(defn scrape-details!
  [page act-id act]
  (if (or (not= (:activity-id act) act-id)
           (:has-details? act))
    act
    (let [details (scrape/details page)]
      (assoc act
             :has-details? (some? details)
             :details details))))

(defn save-cover!
  [dir act]
  (if (or (not (:has-details? act))
          (:thumbnail-url act))
    act
    (let [cover-url (get-in act [:details :cover-url])
          act-id (:activity-id act)
          thumb-file (u/resolve-path dir (str act-id ".jpg"))
          _ (progress "Fetching cover image for " act-id)
          thumb-url (u/wget (u/tap! (thumbnail-url cover-url))
                            thumb-file)
          ; _ (Thread/sleep 5000)
          ]
      (if thumb-url
        (assoc act
               :thumbnail-url thumb-url)
        (do
         (progress "** Failed!")
         act)))))

(defn go-details! [db]
  (let [act-list (:activities db)
        page (fetch! fpath-details)
        act-id (scrape/act-id page)]
    (if (empty? act-list)
      db
      (assoc db
             :activities (mapv #(scrape-details! page act-id %) act-list)))))

(defn go-covers! [cover-dir db]
  (let [act-list (:activities db)]
    (if (empty? act-list)
      db
      (assoc db
             :activities (mapv #(save-cover! cover-dir %) act-list)))))

(defn go! [user-id dest details?]
  (let [edn-file (u/resolve-path dest (str user-id ".edn"))
        html-file (u/resolve-path dest (str user-id ".html"))
        cover-dir (u/resolve-path dest (str user-id))
        db (db! edn-file)
        db (if details?
             (go-details! db)
             (scrape! fpath-index user-id db))
        db (if details?
             (do
              (u/mkdir cover-dir)
              (go-covers! cover-dir db))
             db)]
    (progress "Saving edn: " edn-file)
    (u/mkdir dest)
    (u/write-edn! edn-file db)
    (progress "Saving html " html-file)
    (spit html-file
          (render/render db)))
  )

(defn- show-usage!
  [options-summary]
  (println)
  (println "Usage: yamar [OPTIONS] USER-ID")
  (println)
  (println "Options:")
  (println options-summary))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)
        user-id (first arguments)
        destination (:destination options)
        err-user-id? (when-not user-id
                       (println "Missing argument: USER-ID")
                       true)
        err-options? (when errors
                       (println (str/join \newline errors))
                       true)]
    (if (or err-user-id?
            err-options?
            (:help options))
      (show-usage! summary)
      (go! user-id destination (:details options)))))

(comment

 ;; latest version
 (def page (let [fpath fpath-index]
             (fetch! fpath)))
 (scrape/user-name page)
 (map scrape/activity-id (scrape/act-node-list page))
 (map scrape/thumbnail-url (scrape/act-node-list page))   ; should be nil
 (map scrape/num-photos (scrape/act-node-list page))
 (map scrape/elapse (scrape/act-node-list page))
 (map scrape/distance (scrape/act-node-list page))
 (map scrape/altitude (scrape/act-node-list page))
 (map scrape/heading (scrape/act-node-list page))
 (map scrape/act-date (scrape/act-node-list page))
 (map mk-activity (scrape/act-node-list page))

 (go! 1764261 "docs/" false)

 (def page (let [fpath fpath-details]
             (fetch! fpath)))

 (scrape/act-id page)
 (pp/pprint (scrape/details page))

 (go! 1764261 "docs/" true)


 ;; older version
 (index-url 1764261)
 (scrape/max-page-no page)
 (def ar (scrape! fpath-index 1764261 {}))
 (pp/pprint ar)
 (spit "index.html" (render/render (:activities ar)))
 (let [ar (u/read-edn! "docs/1764261.edn" {})]
   (->> ar
        (render/render)
        (spit "index.html")))
 (act-url 23213302)

 (cli/parse-opts
  ["--help"
   "-d" "html/"
   "23886620"] cli-options)

 )

