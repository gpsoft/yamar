(ns yamar.render
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]))

(def ^:private index "index.html")

(defn- num-items
  [num-items]
  [:span.num-items (str num-items "件")])

(defn- label-and-value
  [label value]
  [:div.label-and-value
   [:div.lav-label label]
   [:div.lav-value value]])

(defn- render-act
  [act]
  (let [{:keys [act-date activity-url heading elapse distance altitude]} act]
    (html [:article
           [:div.main-line
            [:div.act-date act-date]
            [:a.link {:href activity-url}
             heading]]
           [:div.sub-line1
            [:div.elapse
             (label-and-value "所要時間" elapse)]
            [:div.distance
             (label-and-value "距離" (str distance "km"))]
            [:div.altitude
             (label-and-value "標高" (str altitude "m"))]]
           ])))

(defn- render-year
  [year year-acts]
  (let [act-list (get year-acts year)]
    (str
     (html [:h2.year
            [:span (str year "年")]
            (num-items (count act-list))])
     (apply str (map render-act act-list)))))

(defn render
  [ar]
  (let [act-list (:activities ar)
        user-name (:user-name ar)
        year-acts (group-by :year act-list)
        years (sort #(compare %2 %1) (keys year-acts) )
        html-str (-> (io/resource index)
                           slurp)]
    (str
     html-str
     (html [:h1
            [:span (str user-name "さんの活動日記")]
            (num-items (count act-list))])
     (apply str (map #(render-year % year-acts) years))
     "</div></body></html>")))
