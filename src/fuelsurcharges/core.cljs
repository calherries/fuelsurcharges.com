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
   [cljs-time.format :as tf]
   [mount.core :as mount])
  (:import goog.History))

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

(defn unparse-date [date]
  (tf/unparse (tf/formatter "MM/dd") date))

(defn format-pct [n]
  (gstring/format "%.2f%%" (* 100 n)))

(defn home []
  (let [markets-loading? @(rf/subscribe [:markets/loading?])
        fsc-loading?     @(rf/subscribe [:fsc/loading?])]
    [:div.v-box
     [:div.h-box.h-16.justify-center.items-center {:style {:background-color "#024"}}
      [:h1.text-2xl.font-bold {:style {:color "#FFF"}} "FuelSurcharges.com"]]
     [line]
     [:div.h-box.justify-center
      (if markets-loading?
        [:h3 "Loading markets"]
        [:div.v-box.justify-center.items-center.w-auto
         [:div.mt-10>h2.text-2xl.font-bold "Top U.S. Fuel Prices"]
         [:div.v-box.mt-5
          [:table.m-2
           [:thead.border-b
            [:tr
             [:th.text-left]
             [:th.text-center {:col-span "3"} "Dollars Per Gallon"]]
            [:tr
             [:th.text-left "Price"]
             [:th.text-center.p-2 "Last Week"]
             [:th.text-center.p-2 "This Week"]
             [:th.text-center.p-2 "Change"]]]
           [:tbody
            (doall
              (for [market @(rf/subscribe [:markets/markets])]
                (let [prices            (:prices market)
                      width             300
                      height            100
                      points            (price-points-str (take-last 52 (map :price prices)) width height)
                      current-price     (->> prices last)
                      previous-price    (->> prices (take-last 2) first)
                      change            (- (:price current-price) (:price previous-price))
                      percentage-change (/ change (:price previous-price))
                      ]
                  ^{:key (:id market)}
                  [:tr.border-b
                   [:td
                    [:v-box
                     [:p.w-auto.text-left
                      (:market-name market)]
                     [:p.text-xs.text-gray-500 (str "SOURCE: " (:source-name market))]]]
                   [:td.text-center.p-2
                    [:v-box.justify-center
                     [:h-box
                      [:p.inline-block (str (:price previous-price))]]
                     [:p.text-xs.text-gray-500 (unparse-date (:price-date previous-price))]]]
                   [:td.text-center.p-2
                    [:v-box
                     [:h-box
                      [:p.inline-block (str (:price current-price))]]
                     [:p.text-xs.text-gray-500 (unparse-date (:price-date current-price))]]]
                   [:td.text-center.p-2
                    [:v-box
                     [:h-box
                      (if (pos? change)
                        [:div.change-direction--positive.inline-block.mr-1]
                        [:div.change-direction--negative.inline-block])
                      [:p.inline-block (gstring/format "%.3f" change)]]
                     [:p.text-xs.text-gray-500 (str (when (pos? change) "+") (gstring/format "%.1f%" (* 100 percentage-change)))]]]
                   [:td.w-0.md:p-2.invisible.md:visible
                    [:div.w-0.md:w-40.md:p-2
                     [:svg.inline-block {:viewBox [0 0 width height]}
                      [:polyline {:points points :stroke "#024" :fill "none" :stroke-width 3}]]]]])))]]]])]
     [:div.h-box.justify-center
      (if fsc-loading?
        [:h3 "Loading Fuel Surcharges"]
        [:div.v-box.justify-center.items-center.w-auto
         [:div.mt-10>h2.text-2xl.font-bold "Top U.S. Fuel Surcharges"]
         [:div.v-box.mt-5
          [:table.m-2
           [:thead.border-b
            [:tr
             [:th.text-left]
             [:th.text-center {:col-span "3"} "% of Line Haul Price"]]
            [:tr
             [:th.text-left "Fuel Surcharge"]
             [:th.text-center.p-2 "Last Week"]
             [:th.text-center.p-2 "This Week"]
             [:th.text-center.p-2 "Change"]]]
           [:tbody
            (doall
              (for [fsc @(rf/subscribe [:fsc/list])]
                (let [history           (:history fsc)
                      width             300
                      height            100
                      points            (price-points-str (take-last 52 (map :surcharge-amount history)) width height)
                      current           (->> history last)
                      previous          (->> history (take-last 2) first)
                      change            (- (:surcharge-amount current) (:surcharge-amount previous))
                      percentage-change (/ change (:surcharge-amount previous))
                      ]
                  ^{:key (:id fsc)}
                  [:tr.border-b
                   [:td
                    [:v-box
                     [:p.w-auto.text-left
                      (str (:company-name fsc) " " (:name fsc))]
                     [:a.text-xs.text-gray-500.block {:href (:source-url fsc)} (str "SOURCE: " (:company-name fsc) ".COM")]]]
                   [:td.text-center.p-2
                    [:v-box.justify-center
                     [:h-box
                      [:p.inline-block (format-pct (:surcharge-amount previous))]]
                     [:p.text-xs.text-gray-500 (unparse-date (:price-date previous))]]]
                   [:td.text-center.p-2
                    [:v-box
                     [:h-box
                      [:p.inline-block (format-pct (:surcharge-amount current))]]
                     [:p.text-xs.text-gray-500 (unparse-date (:price-date current))]]]
                   [:td.text-center.p-2
                    [:v-box
                     [:h-box
                      (cond
                        (pos? change) [:div.change-direction--positive.inline-block.mr-1]
                        (neg? change) [:div.change-direction--negative.inline-block.mr-1])
                      [:p.inline-block (format-pct change)]]]]
                   [:td.w-0.md:p-2.invisible.md:visible
                    [:div.w-0.md:w-40.md:p-2
                     [:svg.inline-block {:viewBox [0 0 width height]}
                      [:polyline {:points points :stroke "#024" :fill "none" :stroke-width 3}]]]]])))]]]])]
     ]))
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
