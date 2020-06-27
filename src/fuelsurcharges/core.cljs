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


(defn home []
  (let [loading?   @(rf/subscribe [:markets/loading?])
        market-ids @(rf/subscribe [:markets/list])]
    [:div.page.w-screen
     [:div.header.m-auto.max-w-screen-xl.p-3
      [:h1.text-2xl.font-bold.text-center "FuelSurcharges.com"]]
     [:div.content.m-auto.max-w-screen-xl
      (if loading?
        [:h3 "Loadin markets"]
        [:div
         [:h3 "loaded!"]
         [:p (str market-ids)]
         [:div
          (let [prices @(rf/subscribe [:markets/prices-list-by-id 1])
                height 500
                width  1000
                xs     (map #(* % (/ width (count prices))) (range))
                ys     (->> prices
                            (map :price)
                            (map #(float (/ % height)))
                            (map vector xs))])
          [:svg {:viewBox [0 0 500 100]}
           [:polyline {:points points :stroke "black" :fill "none"}]]
          (for [id market-ids]
            (let [prices @(rf/subscribe [:markets/prices-list-by-id id])]
              [(oz/vega-lite (line-plot prices))]))]])]]))

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
