(ns fuelsurcharges.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [fuelsurcharges.ajax :as ajax]
   [fuelsurcharges.events]
   [reitit.core :as reitit]
   [oz.core :as oz]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string]
   [goog.string :as gstring]
   [re-com.core :as rc :refer [h-box v-box box gap line]]
   [goog.string.format]
   [mount.core :as mount])
  (:import goog.History))

(defn line-plot [data]
  {:title    "Price of automotive gas oil, 1000L"
   :data     {:values data}
   :encoding {:x {:field "price-date" :type "temporal"}
              :y {:field "price" :type "quantitative"}}
   :mark     "line"
   :width    800})

(def uniqkey (atom 0))
(defn gen-key []
  (let [res (swap! uniqkey inc)]
    res))

(def points
  "0,120 20,60 40,80 60,20")
(gen-key)

(defn svg
  [[minx miny user-width user-height :as dims] width height contents]
  [:svg {:xmlns   "http://www.w3.org/2000/svg" :version "1.1"
         :viewBox (apply gstring/format "%f %f %f %f" (map double dims))
         :width   (gstring/format "%dpx" width)
         :height  (gstring/format "%dpx" height)}
   contents])

(defn css
  [m]
  (string/join (for [[k v] m]
                 (gstring/format "%s:%s;" (name k) (str v)))))

(defn set-style
  [attrs]
  (cond-> attrs
    (map? (:style attrs))
    (update-in [:style] css)))

(defn elem
  ([tagname attrs]
   [tagname (set-style attrs)])
  ([tagname attrs contents]
   [tagname (set-style attrs) contents]))

(defn ^:private points-str
  [sep points]
  (string/join sep (for [[x y] points] (str (double x) \, (double y)))))

(defn polyline
  ([points] (polyline points {}))
  ([points attrs]
   (elem :polyline (assoc attrs :points (points-str " " points)))))

(defn price-points-str [prices width height]
  (let [xs        (map #(int (* % (/ width (count prices)))) (range))
        max-price (apply max prices)
        min-price (apply min prices)
        points    (->> prices
                       (map #(int (* (- max-price %) (/ height (- max-price min-price)))))
                       (map vector xs)
                       (map #(string/join "," %))
                       (string/join " "))]
    points))

(defn home []
  (let [loading?   @(rf/subscribe [:markets/loading?])
        market-ids @(rf/subscribe [:markets/list])]
    [v-box
     :children
     [[v-box
       :children
       [[h-box
         :height "3rem"
         :justify :center
         :align :center
         :children
         [[:h1.text-2xl.font-bold "FuelSurcharges.com"]]]]]
      [line]
      [gap :size "1rem"]
      [h-box
       :justify :center
       :children
       [(if loading?
          [:h3 "Loading markets"]
          [v-box
           :justify :center
           :align :center
           :width "1000px"
           :children
           [[:h2.text-xl.font-bold "Top global fuel prices"]
            [v-box
             :children
             [[:table
               [:thead
                [:tr
                 [:th.text-left.p-2 "Name"]
                 [:th.text-right.p-2 "Price"]
                 [:th.text-right.p-2 "Change"]
                 [:th.text-right.p-2 "Price Graph (Year)"]]]
               [:tbody
                (let [
                      prices @(rf/subscribe [:markets/prices-list-by-id 1])
                      width  300
                      height 100
                      points (price-points-str (take-last 52 (map :price prices)) width height)]
                  [:tr
                   [:td
                    [:p.w-56.text-left.p-2
                     "Automotive Diesel, EU"]]
                   [:td.text-right.p-2 (->> prices last :price (gstring/format "€%d"))]
                   [:td.text-right.p-2 "0.11%"]
                   [:td.p-2
                    [:div.w-40.justify-end
                     [:svg.inline-block {:viewBox [0 0 width height]}
                      [:polyline {:points points :stroke "grey" :fill "none" :stroke-width 2}]]]]])]]]]]])]]]]))
;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'home] (.getElementById js/document "app")))

(defn init! []
  (mount/start)
  (ajax/load-interceptors!)
  (rf/dispatch [:app/initialize])
  (mount-components))
