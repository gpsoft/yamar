(ns yamar.render
  (:require
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
  [act user-id]
  (let [{:keys [activity-id has-details? act-date activity-url heading elapse distance altitude details]} act
        {:keys [description rest-time sta-end-time]} details]
    [:article
     [:div.side-pane
      [:div.act-date-time
       [:div.act-date act-date]
       (when (and has-details? sta-end-time)
         (let [[sta end] sta-end-time]
           [:div.act-time (str sta "〜" end)]))]
      (when has-details?
        [:div.thumbnail {:style (str "background-image:url('" user-id "/" activity-id ".jpg')")}])]
     [:div.main-pane
      [:div.main-line
       [:a.act-link {:href activity-url}
        heading]]
      [:div.sub-line1
       [:div.elapse
        (label-and-value-v "所要時間" elapse)]
       (when has-details?
         [:div.rest-time
          (label-and-value-v "休憩時間" rest-time)])
       [:div.distance
        (label-and-value-v "距離" (str distance "km"))]
       [:div.altitude
        (label-and-value-v "標高" (str altitude "m"))]]
      (when has-details? 
        [:div.desc-line
         [:div.description
          description]
         [:div.description-float
          description]])]]))

(defn- year-v
  [year year-acts user-id]
  (let [act-list (u/rev-sort :act-date (get year-acts year))]
    [:div.year-wrap
     [:h2.year
      [:span (str year "年")]
      (num-items-v (count act-list))]
     [:div.article-list
      (map #(render-act % user-id) act-list)]]))

(defn render
  [ar]
  (let [act-list (:activities ar)
        user-id (:user-id ar)
        user-name (:user-name ar)
        mypage-url (:mypage-url ar)
        year-acts (group-by :year act-list)
        years (u/rev-sort (keys year-acts))
        html-str (u/read-resource! index)
        script-str (u/read-resource! script)]
    (str
     html-str
     (html [:div.container
            [:div.heading
             [:h1
              [:span
               [:a.mypage-link {:href mypage-url}
                user-name]
               "さんの活動日記"]
              (num-items-v (count act-list))]
             [:div.filter-wrap
              [:div.filter-on
               "フィルターモード"]
              [:div.filter-off ""]
              [:input {:type "text"
                       :class "filter-input"
                       :placeholder "検索キーワード"}]]]
            (map #(year-v % year-acts user-id) years)])
     "<script type=\"text/javascript\">"
     script-str
     "</script></body></html>")))
