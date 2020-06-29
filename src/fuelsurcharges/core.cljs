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
    [:div.v-box
     [:div.h-box.h-16.justify-center.items-center
      [:h1.text-4xl.font-bold {:style {:color "#0086FF"}} "FuelSurcharges.com"]]
     [line]
     [:div.h-box.justify-center
      (if loading?
        [:h3 "Loading markets"]
        [:div.v-box.justify-center.items-center.max-w-screen-lg
         [:div.mt-5>h2.text-2xl.font-bold "Top global fuel prices"]
         [:div.v-box.mt-4
          [:table
           [:thead
            [:tr
             [:th.text-left.p-2 "Name"]
             [:th.text-right.p-2 "Price (dollars per gallon)"]
             [:th.text-right.p-2 "Change"]
             [:th.text-right.p-2 "Price Graph (Year)"]]]
           [:tbody
            (let [market @(rf/subscribe [:markets/market-by-id 3])
                  prices @(rf/subscribe [:markets/prices-by-id 3])
                  width  300
                  height 100
                  points (price-points-str (take-last 52 (map :price prices)) width height)]
              [:tr
               [:td
                [:p.w-56.text-left.p-2
                 (:market-name market)]]
               [:td.text-right.p-2 (->> prices last :price (str "$"))]
               [:td.text-right.p-2 "0.11%"]
               [:td.p-2
                [:div.w-40.justify-end
                 [:svg.inline-block {:viewBox [0 0 width height]}
                  [:polyline {:points points :stroke "grey" :fill "none" :stroke-width 2}]]]]])]]]])]]))
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
