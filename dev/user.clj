(ns user
  (:require
   clojure.pprint
   [yamar.core :as core]))

(defn dev
  []
  #_(core/go! 1764261)
  (in-ns 'yamar.core))

(comment

 ;; 広島の山DBを作る。
 (require
  '[clojure.data.json :as json]
  '[clojure.string :as str]
  '[yamar.util :as u])
 (u/wget "https://api.yamap.com/prefectures/34/mountains?page=68&per=10" "mt68.json")
 (->>
  (for [no (range 1 69)]
    ;; ↑mt69.jsonは、手作り。
    (let [fname (str "docs/mt" (u/pad00 (str no)) ".json")
          j (slurp fname)
          d (json/read-str j :key-fn keyword)
          hl (fn [d]
               (->> d
                    (map :body)
                    (str/join \newline)))
          mt (fn [d]
               (-> d
                   (dissoc
                    ; :altitude
                    ; :coord
                    ; :description
                    ; :id
                    ; :name
                    ; :name_hira
                    :primary_image
                    :mountain_highlights
                    :attention_info
                    :created_at
                    :images
                    :max_difficulty_level
                    :max_fitness_level
                    :min_difficulty_level
                    :min_fitness_level
                    :prefecture_image_url
                    :prefectures
                    :regions
                    :short_description
                    :tags
                    :updated_at
                    :wikipedia_url
                    )
                   (assoc
                    :highlights (hl (:mountain_highlights d))
                    :image-url (get-in d [:primary_image :base_url]))))]
      (mapv mt (:mountains d))))
  (apply concat)
  (sort-by :altitude)
  (reverse)
  (into [])
  (u/write-edn! "docs/mt_hirosima.edn"))

 ;; 山DBからhtmlへ。
 (require
  '[clojure.string :as str]
  '[hiccup.core :refer [html]]
  '[yamar.util :as u])
 (let [mt-list (u/read-edn! "docs/mt_hirosima.edn" [])
       mt (fn [{:keys [id name name_hira altitude description highlights image-url]
                [lat lng] :coord}]
            [:div.mountain {:data-id id}
             [:div.side-pane
              [:div.altitude (str altitude "m")]]
             [:div.main-pane
              [:div.name-line
               [:a.link-yamap {:href (str "https://yamap.com/mountains/" id)}
                [:div.name name]
                [:div.name-hira name_hira]]
               [:a.link-gmap {:href (str "https://www.google.co.jp/maps/?q=" lng "," lat)
                    :target "_blank"}
                ""]
               [:a.link-gearth {:href (str "https://earth.google.com/web/@" lng "," lat, ",18a,200000d,1y,-30h,70t,0r")
                    :target "_blank"}
                ""]]
              [:div.highlights highlights]
              [:div.description description]]])]
   (->>
    (html
     [:div.container
      [:h1 "広島の山"]
      [:div.mt-list
       (map mt mt-list)]])
    (spit "docs/_hiroshima.html")))

 )
