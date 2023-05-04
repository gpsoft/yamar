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

(defn- to-int
  [s]
  (Integer. (re-find #"\d+" s)))

(defn- to-year
  [date-str]
  (to-int (re-find #"^\d+" date-str)))

(defn- nth-str
  [txt ix]
  (str/trim (nth (str/split txt #"\n") ix "")))

(defn- second-text
  [node]
  (nth-str (en/text node) 1))

(defn user-name
  [page]
  (let [h1 (sel1 page [:.UsersId__FaceInfo__Name])]
    (en/text h1)))

(defn act-node-list
  [page]
  (en/select page [:ul.UserActivityList__List :li :article]))

(defn activity-id
  [act-node]
  (let [a (sel1 act-node [:a.ActivityItem__Thumbnail])
        href (attr1 a :href)
        match (re-find #"/(\d+)$" href)]
    (when match
      (to-int (second match)))))

(defn thumbnail-url
  [act-node]
  (let [f (sel1 act-node [:a.ActivityItem__Thumbnail :figure.ActivityItem__Thumbnail__Img])
        src (attr1 f :data-src)]
    ;; data-src属性値がURLなんだけど、
    ;; これはJSで作ってるので、取れない。
    src))

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
  (let [txt (counter-text act-node 0)]
    (to-int txt)))

(defn elapse
  [act-node]
  (let [txt (record-text act-node 0)]
    txt))

(defn distance
  [act-node]
  (let [txt (record-text act-node 1)]
    (nth-str txt 1)))

(defn altitude
  [act-node]
  (let [txt (record-text act-node 2)]
    (nth-str txt 1)))

(defn heading
  [act-node]
  (let [h (sel1 act-node [:h3.ActivityItem__Heading])]
    (en/text h)))

(defn act-date
  [act-node]
  (let [s (sel1 act-node [:span.ActivityItem__Date])
        txt (en/text s)
        date-str (nth-str txt 1)]
    [date-str (to-year date-str)]))

(defn max-page-no
  [page]
  (let [lis (en/select page [:div.UsersId__Pagination :li.number])]
    (->> lis
        (map en/text)
        (map to-int)
        (apply max))))

(defn- cover-url
  [page]
  (-> page
      (sel1 [:div.ActivityDetailTabLayout__Image :img])
      (attr1 :src)
      (u/split-url)
      first))

(defn- rest-time
  [page]
  (-> page
      (sel1 [:.CourseTimeItem__Total__RestTime :.CourseTimeItem__Total__Number])
      (en/text)))

(defn- passed-point
  [pt-node]
  (let [nm (sel1 pt-node [:.CourseTimeItem__PassedPoint__Name])
        times (en/select pt-node [:.CourseTimeItem__PassedPoint__Time])]
    [(second-text nm) (mapv second-text times)]))

(defn- passed-point-list
  [page]
  (-> page
      (en/select [:.CourseTimeItem__PassedPoint])
      (->> (map passed-point))))

(defn- description
  [page]
  (-> page
      (sel1 [:.ActivitiesId__Description :.ActivitiesId__Description__Body :span])
      (en/text)))

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
(defn details
  [page]
  (let [photos (photo-list page)]
    {:cover-url (cover-url page)
     :rest-time (rest-time page)
     :passed-points (passed-point-list page)
     :description (description page)
     :photos photos})
  )

(comment
 
 ; https://cdn.yamap.co.jp/public/image2.yamap.co.jp/production/a8e2e488-745c-49d8-9460-68d7f62d96d5?t=resize&w=480

 (require '[yamar.core :as core])
 (-> core/page
     (photo-list)
     #_(en/select [:.ActivitiesId__Photo])
     #_(description))
 
 )
