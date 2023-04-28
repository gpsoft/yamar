(ns yamar.scrape
  (:require
   [clojure.string :as str]
   [net.cgrand.enlive-html :as en]))

(defn- sel1
  [nodes selector]
  (first (en/select nodes selector)))

(defn- val1
  [node attr]
  (first (en/attr-values node attr)))

(defn- to-int
  [s]
  (Integer. (re-find #"\d+" s)))

(defn- to-year
  [date-str]
  (to-int (re-find #"^\d+" date-str)))

(defn- nth-str
  [s ix]
  (str/trim (nth (str/split s #"\n") ix)))

(defn user-name
  [page]
  (let [h1 (sel1 page [:.UsersId__FaceInfo__Name])]
    (en/text h1)))

(defn activity-list
  [page]
  (en/select page [:ul.UserActivityList__List :li :article]))

(defn activity-id
  [act]
  (let [a (sel1 act [:a.ActivityItem__Thumbnail])
        href (val1 a :href)
        match (re-find #"/(\d+)$" href)]
    (when match (to-int (second match)))))

(defn thumbnail-url
  [act]
  (let [f (sel1 act [:a.ActivityItem__Thumbnail :figure.ActivityItem__Thumbnail__Img])
        src (val1 f :data-src)]
    ;; data-src属性値がURLなんだけど、
    ;; これはJSで作ってるので、取れない。
    src))

(defn- counter-text
  [act ix]
  (let [spans (en/select act [:span.CounterCapsule__Count])
        txt (en/text (nth spans ix))]
    txt))

(defn- record-text
  [act ix]
  (let [spans (en/select act [:span.ActivityCounters__Count__Record])
        txt (en/text (nth spans ix))]
    txt))

(defn num-photos
  [act]
  (let [txt (counter-text act 0)]
    (to-int txt)))

(defn elapse
  [act]
  (let [txt (record-text act 0)]
    txt))

(defn distance
  [act]
  (let [txt (record-text act 1)]
    (nth-str txt 1)))

(defn altitude
  [act]
  (let [txt (record-text act 2)]
    (nth-str txt 1)))

(defn heading
  [act]
  (let [h (sel1 act [:h3.ActivityItem__Heading])]
    (en/text h)))

(defn act-date
  [act]
  (let [s (sel1 act [:span.ActivityItem__Date])
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
