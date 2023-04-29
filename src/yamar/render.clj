(ns yamar.render
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [yamar.util :as u]))

(def ^:private index "index.html")
(def ^:private script "script.js")

(defn- num-items-v
  [num-items]
  [:span.num-items (str num-items "件")])

(defn- label-and-value-v
  [label value]
  [:div.label-and-value
   [:div.lav-label label]
   [:div.lav-value value]])

(defn- render-act
  [act]
  (let [{:keys [act-date activity-url heading elapse distance altitude]} act]
    [:article
     [:div.main-line
      [:div.act-date act-date]
      [:a.link {:href activity-url}
       heading]]
     [:div.sub-line1
      [:div.elapse
       (label-and-value-v "所要時間" elapse)]
      [:div.distance
       (label-and-value-v "距離" (str distance "km"))]
      [:div.altitude
       (label-and-value-v "標高" (str altitude "m"))]]]))

(defn- year-v
  [year year-acts]
  (let [act-list (u/rev-sort :act-date (get year-acts year))]
    [:div.year-wrap
     [:h2.year
      [:span (str year "年")]
      (num-items-v (count act-list))]
     (map render-act act-list)]))

(defn render
  [ar]
  (let [act-list (:activities ar)
        user-name (:user-name ar)
        year-acts (group-by :year act-list)
        years (u/rev-sort (keys year-acts))
        html-str (u/read-resource! index)
        script-str (u/read-resource! script)]
    (str
     html-str
     (html [:div.container
            [:h1
             [:span (str user-name "さんの活動日記")]
             (num-items-v (count act-list))]
            (map #(year-v % year-acts) years)])
     "<script>"
     script-str
     "</script></body></html>")))
