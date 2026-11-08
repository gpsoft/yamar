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
  (let [h1 (sel1 page [:h1.css-1u3x0bx])]
    (en/text h1)))

(defn act-node-list
  [page]
  (en/select page [:ul.css-4anh0l :li :article]))

(defn activity-id
  [act-node]
  (let [a (sel1 act-node [:a.css-ezibo1])
        href (attr1 a :href)
        match (re-find #"/(\d+)$" href)]
    (when match
      (u/to-int (second match)))))

(defn thumbnail-url
  [act-node]
  (let [f (sel1 act-node [:a.css-192jaxu :img])]
    ;; src属性値をJSで書き換えるようになってる。
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
  (let [txt (en/text (sel1 act-node [:div.css-vz5jps :span.css-in3yi3]))]
    (u/to-int txt)))

(defn elapse
  [act-node]
  (let [txt (en/text (nth (en/select act-node [:div.css-sh0fqq :span.css-in3yi3]) 0))]
    txt))

(defn distance
  [act-node]
  (let [txt (en/text (nth (en/select act-node [#_:span.css-19gv6gn :span.css-in3yi3]) 3))]
    (str/replace txt #"[\s\nkm]" "")))

(defn altitude
  [act-node]
  (let [txt (en/text (nth (en/select act-node [#_:span.css-19gv6gn :span.css-in3yi3]) 4))]
    (str/replace txt #"[\s\nm]" "")))

(defn heading
  [act-node]
  (let [h (sel1 act-node [:a.css-vzt7ok])]
    (en/text h)))

(defn act-date
  [act-node]
  (let [date-str (en/text (sel1 act-node [:span.css-8e72v8]))
        date-str (str/replace date-str #"[\s\n]" "")]
    [date-str (to-year date-str)]))

(defn max-page-no
  [page]
  (let [lis (en/select page [:div.UsersId__Pagination :li.number])]
    (->> lis
        (map en/text)
        (map u/to-int)
        (cons 0)  ; need at least one element for max
        (apply max))))

(defn cover-url
  [page]
  (-> page
      (sel1 [[:meta (en/attr= :property "og:image")]])
      (attr1 :content)
      #_(u/split-url)
      #_first))

(defn- mk-time
  ([m] (mk-time "00" m))
  ([h m]
   (str (u/pad00 h) ":" (u/pad00 m))))

(defn format-duration [s]
  (let [[_ hours minutes] (re-matches #"(?:(\d+)時間)?(\d+)分" s)
        hours (or hours "0")]
    (format "%02d:%02d"
            (Integer/parseInt hours)
            (Integer/parseInt minutes))))

(defn rest-time
  [page]
  (-> page
      (sel1 [:div.css-ktw0g7 :div.css-l0ue95 :time.css-qnn6ou])
      (en/text)
      format-duration))

(defn- passed-point
  [pp-node]
  (let [nm (sel1 pp-node [:a.css-1v9130b])
        times (en/select pp-node [:time.css-j04ele])]
    [(en/text nm) (mapv en/text times)]))

(defn passed-point-list
  [page]
  (-> page
      (en/select [:div.css-1yuhvjn :div.css-kjdmu4 :div.css-uqzade])
      (->> (map passed-point))))

(defn- sta-end-time
  [pp-list]
  (let [[_ [sta]] (first pp-list)
        [_ [end]] (last pp-list)]
    (when (and sta end)
      [sta end])))

(defn description
  [page]
  (-> page
      (sel1 [:p.css-61ww83])
      (en/text)))

(defn- photo
  [photo-node]
  (let [path (sel1 photo-node [:.Yn1glG_photoLink])
        caption (sel1 photo-node [:.Yn1glG_photoCaption])]
    {:url-path (attr1 path :href)
     :caption (en/text caption)}))

(defn photo-list
  [page]
  (-> page
      (en/select [:.Yn1glG_photoFigure])
      (->> (map photo))))

(defn act-id
  [page]
  (let [url (attr1 (sel1 page [:a.css-cvl8o3]) :href)]
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
