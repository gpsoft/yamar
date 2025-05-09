(ns yamar.scrape
  (:require
   [clojure.string :as str]
   [net.cgrand.enlive-html :as en]
   [yamar.util :as u]))

(defn- sel1
  [nodes selector]
  (first (en/select nodes selector)))

(defn- attr1
  [node attr]
  (first (en/attr-values node attr)))

(defn- to-year
  [date-str]
  (u/to-int (re-find #"^\d+" date-str)))

(defn- nth-str
  [txt ix]
  (str/trim (nth (str/split txt #"\n") ix "")))

(defn- second-text
  [node]
  (nth-str (en/text node) 1))

(defn user-name
  [page]
  (let [h1 (sel1 page [:.css-jctfiw])]
    (en/text h1)))

(defn act-node-list
  [page]
  (en/select page [:ul.css-qksbms :li :article]))

(defn activity-id
  [act-node]
  (let [a (sel1 act-node [:a.css-192jaxu])
        href (attr1 a :href)
        match (re-find #"/(\d+)$" href)]
    (when match
      (u/to-int (second match)))))

(defn thumbnail-url
  [act-node]
  (let [f (sel1 act-node [:a.css-192jaxu :img.css-kjzr73])
        src (attr1 f :src)]
    ;; src属性値をJSで改変する仕組みなので、
    ;; ここではnilにしとく。
    nil))

(defn- counter-text
  [act-node ix]
  (let [spans (en/select act-node [:span.CounterCapsule__Count])
        txt (en/text (nth spans ix))]
    txt))

(defn- record-text
  [act-node ix]
  (let [spans (en/select act-node [:span.ActivityCounters__Count__Record])
        txt (en/text (nth spans ix))]
    txt))

(defn num-photos
  [act-node]
  (let [txt (nth (:content (sel1 act-node [:div.css-1daphfi :span.css-1xxe9l9])) 1)]
    (u/to-int txt)))

(defn elapse
  [act-node]
  (let [txt (en/text (nth (en/select act-node [:div.css-n261p7 :span.css-1xxe9l9]) 0))]
    txt))

(defn distance
  [act-node]
  (let [txt (en/text (nth (en/select act-node [:div.css-n261p7 :span.css-1xxe9l9]) 1))]
    (str/replace txt #" km" "")))

(defn altitude
  [act-node]
  (let [txt (en/text (nth (en/select act-node [:div.css-n261p7 :span.css-1xxe9l9]) 2))]
    (str/replace txt #" m" "")))

(defn heading
  [act-node]
  (let [h (sel1 act-node [:h3 :a.css-1pla16])]
    (en/text h)))

(defn act-date
  [act-node]
  (let [date-str (en/text (sel1 act-node [:span.css-125iqyy]))]
    [(str/replace date-str #" " "") (to-year date-str)]))

(defn max-page-no
  [page]
  (let [lis (en/select page [:div.UsersId__Pagination :li.number])]
    (->> lis
        (map en/text)
        (map u/to-int)
        (cons 0)  ; need at least one element for max
        (apply max))))

(defn- cover-url
  [page]
  (-> page
      (sel1 [:div.ActivityDetailTabLayout__Image])
      (attr1 :data-src)
      (u/split-url)
      #_first))

(defn- mk-time
  ([m] (mk-time "00" m))
  ([h m]
   (str (u/pad00 h) ":" (u/pad00 m))))

(defn- rest-time
  [page]
  (-> page
      (en/select [:.CourseTimeItem__Total__RestTime :.CourseTimeItem__Total__Number])
      (->> (map en/text)
           (apply mk-time))))

(defn- passed-point
  [pp-node]
  (let [nm (sel1 pp-node [:.CourseTimeItem__PassedPoint__Name])
        times (en/select pp-node [:.CourseTimeItem__PassedPoint__Time])]
    [(second-text nm) (mapv second-text times)]))

(defn- passed-point-list
  [page]
  (-> page
      (en/select [:.CourseTimeItem__PassedPoint])
      (->> (map passed-point))))

(defn- sta-end-time
  [pp-list]
  (let [[_ [sta]] (first pp-list)
        [_ [end]] (last pp-list)]
    (when (and sta end)
      [sta end])))

(defn- description
  [page]
  (-> page
      (en/select [:.ActivitiesId__Description :.ActivitiesId__Description__Body :> :*])
      (->> (map en/text)
           (str/join ""))))

(defn- photo
  [photo-node]
  (let [path (sel1 photo-node [:.ActivitiesId__Photo__Link])
        caption (sel1 photo-node [:.ActivitiesId__Photo__Caption])]
    {:url-path (attr1 path :href)
     :caption (en/text caption)}))

(defn- photo-list
  [page]
  (-> page
      (en/select [:.ActivitiesId__Photo])
      (->> (map photo))))

(defn act-id
  [page]
  (let [url (attr1 (sel1 page [:a.ActivityDetailTabLayout__TabItem]) :href)]
    (u/to-int (last (str/split url #"/")))))

(defn details
  [page]
  (when page
    (let [photos (photo-list page)
          pp-list (passed-point-list page)]
      {:cover-url (cover-url page)
       ;; :cover-url can be either string
       ;; or tuple [url-string q-map]
       :rest-time (rest-time page)
       :passed-points pp-list
       :sta-end-time (sta-end-time pp-list)
       :description (description page)
       :photos photos})))

(comment

 ; https://cdn.yamap.co.jp/public/image2.yamap.co.jp/production/a8e2e488-745c-49d8-9460-68d7f62d96d5?t=resize&w=480

 (require '[yamar.core :as core])
 (-> core/page
     (description)
     #_(en/select [:.ActivitiesId__Photo])
     #_(description))

 (let [edn-file "docs/1764261.edn"
       ar (u/read-edn! edn-file {})
       act-list (:activities ar)]
   (when-not (empty? act-list)
     (->> act-list
          (mapv #(let [pp-list (get-in % [:details :passed-points])]
                   (assoc-in % [:details :sta-end-time] (sta-end-time pp-list))))
          (assoc ar :activities)
          (u/write-edn! edn-file))
     nil))

 (let [edn-file "docs/1764261.edn"
       ar (u/read-edn! edn-file {})
       user-id (:user-id ar)]
   (-> ar
       (assoc :mypage-url (str "https://yamap.com/users/" user-id))
       (->> (u/write-edn! edn-file))))

 )
