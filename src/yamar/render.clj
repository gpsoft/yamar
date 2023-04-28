(ns yamar.render
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]))

(def ^:private index
  (-> (io/resource "index.html")
      slurp))

(defn- render-act
  [act]
  (let [{:keys [act-date activity-url heading elapse distance altitude]} act]
    (html [:article
           [:div.main-line
            [:div.act-date act-date]
            [:a.link {:href activity-url}
             heading]]
           [:div.sub-line1
            [:div.elapse (str "所要時間: " elapse)]
            [:div.distance (str "距離: " distance "km")]
            [:div.altitude (str "標高: " altitude "m")]]
           ])))

(defn- render-year
  [year year-acts]
  (let [act-list (get year-acts year)]
    (str
     (html [:h2.year (str year "年")])
     (apply str (map render-act act-list)))))

(defn render
  [ar]
  (let [act-list (:activities ar)
        user-name (:user-name ar)
        year-acts (group-by :year act-list)
        years (sort #(compare %2 %1) (keys year-acts) )]
    (str
     index
     (html [:h1
            [:span (str user-name "さんの活動日記")]
            [:span.num-acts (str (count act-list) "件")]])
     (apply str (map #(render-year % year-acts) years))
     "</body></html>")))
