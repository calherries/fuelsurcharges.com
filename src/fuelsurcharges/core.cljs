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
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string]
   [mount.core :as mount])
  (:import goog.History))

(defn home []
  [:div.content>div.columns.is-centered>div.column.is-two-thirds
   [:h3 "Loading messages"]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'home] (.getElementById js/document "app")))

(defn init! []
  (mount/start)
  (ajax/load-interceptors!)
  (mount-components))
