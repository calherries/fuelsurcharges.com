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
   [mount.core :as mount])
  (:import goog.History))

(defn line-plot [data]
  {:title    "Price of automotive gas oil, 1000L"
   :data     {:values data}
   :encoding {:x {:field "price-date" :type "ordinal"}
              :y {:field "price" :type "quantitative"}}
   :mark     "line"
   :width    800})

(comment (let [loading?   @(rf/subscribe [:markets/loading?])
               market-ids @(rf/subscribe [:markets/list])
               ]
           (for [id market-ids]
             (let [prices @(rf/subscribe [:markets/prices-list-by-id id])]))))

(def ex (oz/vega-lite (line-plot @(rf/subscribe [:markets/prices-list-by-id 1]))))

(defn home []
  (let [loading?   @(rf/subscribe [:markets/loading?])
        market-ids @(rf/subscribe [:markets/list])
        ]
    [:div.content>div.columns.is-centered>div.column.is-two-thirds
     (if loading?
       [:h3 "Loadin markets"]
       [:div
        [:h3 "loaded!"]
        [:p (str market-ids)]
        [:div
         (for [id market-ids]
           (let [prices @(rf/subscribe [:markets/prices-list-by-id id])]
             [(oz/vega-lite (line-plot prices))]))]])]))

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
