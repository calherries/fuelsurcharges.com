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
   [reitit.core :as rt]
   [reitit.frontend :as rtf]
   [reitit.coercion.spec :as rts]
   [oz.core :as oz]
   [reitit.frontend.easy :as rtfe]
   [clojure.string :as string]
   [goog.string :as gstring]
   [re-com.core :as rc :refer [h-box v-box box gap line]]
   [goog.string.format]
   [schema.core :as s]
   [cljs-time.core :as t]
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

(defn scroll-to-top! []
  (js/scrollTo 0 0))

(defn subscribe [msg]
  (let [email (r/atom "")]
    (fn []
      [:div.h-box.justify-center.p-10.text-center {:style {:background-color "#EEE"}}
       [:div.v-box
        [:div.m-5
         [:h1.text-2xl.font-bold msg]
         [:p "Get weekly fuel price updates, right to your inbox."]]
        [:input.m-5.text-gray-500.font-bold.p-1
         {:type        "email"
          :value       @email
          :placeholder "Enter your email..."
          :on-change   #(reset! email (-> % .-target .-value))}]
        [:div.h-box.justify-center
         [:button.m-5.text-white.font-bold.p-2.w-auto.rounded
          {:style    {:background-color "#024"}
           :on-click #(do
                        (rf/dispatch [:user/subscribe @email])
                        (reset! email ""))}
          "Subscribe now"]]]])))

(defn home-page []
  (let [markets-loading? @(rf/subscribe [:markets/loading?])
        fsc-loading?     @(rf/subscribe [:fsc/loading?])]
    [:div.v-box
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
                   [:td {:style {:width "20rem"}}
                    [:v-box
                     [:a.w-auto.text-left.underline {:href (rtfe/href :market {:id (:id market)})}
                      (:market-name market)]
                     [:a.text-xs.text-gray-500.block {:href "https://www.eia.gov/petroleum/gasdiesel/"} "SOURCE: EIA.GOV"]]]
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
                (let [history  (:history fsc)
                      width    300
                      height   100
                      points   (price-points-str (take-last 52 (map :surcharge-amount history)) width height)
                      current  (->> history last)
                      previous (->> history (take-last 2) first)
                      change   (- (:surcharge-amount current) (:surcharge-amount previous))]
                  ^{:key (:id fsc)}
                  [:tr.border-b
                   [:td {:style {:width "20rem"}}
                    [:v-box
                     [:a.w-auto.text-left.underline {:href (rtfe/href :fsc {:id (:id fsc)})}
                      (str (:company-name fsc) " " (:name fsc))]
                     [:a.text-xs.text-gray-500.block {:href (:source-url fsc)} (str "SOURCE: " (string/upper-case (:company-name fsc)) ".COM")]]]
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
                      (if (zero? change)
                        [:p.text-gray-700 "-"]
                        [:p.inline-block (format-pct change)])]]]
                   [:td.w-0.md:p-2.invisible.md:visible
                    [:div.w-0.md:w-40.md:p-2
                     [:svg.inline-block {:viewBox [0 0 width height]}
                      [:polyline {:points points :stroke "#024" :fill "none" :stroke-width 3}]]]]])))]]]])]]))

(defn market-page []
  (let [market @(rf/subscribe [:markets/selected])]
    [:div.v-box
     [:div.h-box.justify-center
      [:div.v-box.justify-center.w-auto.items-center
       [:div.h-box.my-5.underline.text-gray-700
        [:a {:href (rtfe/href :home)} "← BACK"]]
       [:div>h2.text-2xl.font-bold (:market-name market)]
       [:div.v-box.mt-5
        [:table.m-2
         [:thead.border-b
          [:tr
           [:th.text-left]
           [:th.text-center {:col-span "3"} "Dollars Per Gallon"]]
          [:tr
           [:th.text-left "Source"]
           [:th.text-center.p-2 "Last Week"]
           [:th.text-center.p-2 "This Week"]
           [:th.text-center.p-2 "Change"]]]
         [:tbody
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
             [:td.text-center
              [:v-box.justify-center
               [:a.text-xs.text-gray-700 {:href "https://www.eia.gov/petroleum/gasdiesel/"} "EIA.GOV"]]]
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
                [:polyline {:points points :stroke "#024" :fill "none" :stroke-width 3}]]]]])]]]
       [:div.mt-10>h2.text-2xl.font-bold "History"]
       [:div.v-box.mt-5
        [:table.m-2.table-fixed {:style {:width "50rem"}}
         [:thead.border-b
          [:tr
           [:th.text-center "Start Date"]
           [:th.text-center "End Date"]
           [:th.text-center.p-2 "Fuel Price"]]]
         [:tbody
          (for [price (reverse (:prices market))]
            ^{:key (gen-key)}
            [:tr.border-b.text-center
             [:td {:style {:width "25%"}}
              (tf/unparse (tf/formatter "MMM d, yyyy") (:price-date price))]
             [:td {:style {:width "25%"}}
              (tf/unparse (tf/formatter "MMM d, yyyy") (t/plus (:price-date price) (t/days 7)))]
             [:td.text-center.p-2 {:style {:width "25%"}}
              [:v-box.justify-center
               [:h-box
                [:p.inline-block (gstring/format "%.3f" (:price price))]]]]])]]]]]]))

(defn indices [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn display-subset [fsc-table-data current-surcharge]
  (let [current-index (first (indices #(= (:surcharge-amount %) (:surcharge-amount current-surcharge))
                                      fsc-table-data))]
    (keep-indexed #(when
                       (and
                         (> %1 (- current-index 10))
                         (< %1 (+ current-index 10)))
                     %2)
                  fsc-table-data)))

(display-subset @(rf/subscribe [:fsc/selected-fsc-table]) {:surcharge-amount 0.065})

(defn fsc-table [fsc-table-data current-surcharge]
  [:table.m-5.text-center
   [:thead.border-b
    [:tr.w-12
     [:th.p-2.w-full "Fuel Price"]
     [:th.p-2.w-full "Fuel Surcharge"]]]
   [:tbody
    (for [{:keys [price surcharge-amount]} (display-subset fsc-table-data current-surcharge)]
      (let [row-style (if (= surcharge-amount (:surcharge-amount current-surcharge))
                        {:class "font-bold"
                         :style {:color "#3BB143"}}
                        {})]
        ^{:key (gen-key)}
        [:tr.border-b.text-center
         [:td.p-1 row-style
          (gstring/format "%.3f" price)]
         [:td.p-1 row-style
          (format-pct surcharge-amount)]]))]])


(defn fsc-page []
  (let [fsc               @(rf/subscribe [:fsc/selected-fsc])
        fsc-table-data    @(rf/subscribe [:fsc/selected-fsc-table])
        current-surcharge (-> fsc :history last)]
    [:div.v-box
     [:div.h-box.justify-center
      [:div.v-box.justify-center.w-auto.items-center
       [:div.h-box.my-5.underline.text-gray-700
        [:a {:href (rtfe/href :home)} "← BACK"]]
       [:div>h2.text-2xl.font-bold (str (:company-name fsc) " " (:name fsc) " Fuel Surcharge")]
       [:p.mt-5
        "The current fuel price is "
        [:span.font-bold {:style {:color "#3BB143"}}
         (gstring/format "%.3f" (:price current-surcharge))]
        " gallons per litre."]
       [:p
        "The fuel surcharge is "
        [:span.font-bold {:style {:color "#3BB143"}}
         (format-pct (:surcharge-amount current-surcharge))]
        "."]
       [:div.mt-10>h2.text-2xl.font-bold "Rate Table"]
       [fsc-table fsc-table-data current-surcharge]
       [:div.mt-10>h2.text-2xl.font-bold "History"]
       [:div.v-box.mt-5
        [:table.m-2.table-fixed {:style {:width "50rem"}}
         [:thead.border-b
          [:tr
           [:th.text-center "Start Date"]
           [:th.text-center "End Date"]
           [:th.text-center.p-2 "Fuel Price"]
           [:th.text-center.p-2 "Fuel Surcharge"]]]
         [:tbody
          (for [price (reverse (:history fsc))]
            ^{:key (gen-key)}
            [:tr.border-b.text-center
             [:td {:style {:width "25%"}}
              (tf/unparse (tf/formatter "MMM d, yyyy") (:price-date price))]
             [:td {:style {:width "25%"}}
              (tf/unparse (tf/formatter "MMM d, yyyy") (t/plus (:price-date price) (t/days 7)))]
             [:td.text-center.p-2 {:style {:width "25%"}}
              [:v-box.justify-center
               [:h-box
                [:p.inline-block (:price price)]]]]
             [:td.text-center.p-2 {:style {:width "25%"}}
              [:v-box
               [:h-box
                [:p.inline-block (format-pct (:surcharge-amount price))]]]]])]]]]]]))

(defn header []
  [:header.h-box.h-16.justify-center.items-center {:style {:background-color "#024"}}
   [:a {:href (rtfe/href :home)}
    [:h1.text-2xl.font-bold {:style {:color "#FFF"}} "FuelSurcharges.com"]]])

(defn footer []
  [:footer.mt-10.h-box.h-16.justify-center.items-center {:style {:background-color "#EEE"}}
   [:a {:href (rtfe/href :home)}
    [:p {:style {:color "#888"}} "© 2020 FuelSurcharges.com"]]])

(defn base-page [page]
  (fn []
    [:div.v-box
     [header]
     [subscribe "Track your fuel surcharges"]
     [page]
     [footer]]))

;; -------------------------
;; Routing

(def routes
  ["/"
   [""
    {:name :home
     :view (base-page home-page)}]
   ["market/:id"
    {:name        :market
     :view        (base-page market-page)
     :parameters  {:path {:id int?}}
     :controllers [{:parameters {:path [:id]}}]}]
   ["fsc/:id"
    {:name        :fsc
     :view        (base-page fsc-page)
     :parameters  {:path {:id int?}}
     :controllers [{:parameters {:path [:id]}
                    :start      (fn [{{id :id} :path}]
                                  (rf/dispatch [:fsc/load-fsc-page-data id]))}]}]])

(def router
  (rtf/router
    routes
    {:data {:coercion rts/coercion}}))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:app/navigated new-match])))

(defn init-routes! []
  (js/console.log "initialising routes")
  (rtfe/start!
    router
    on-navigate
    {:use-fragment true}))

(defn current-page []
  (let [current-route @(rf/subscribe [:app/route])]
    (scroll-to-top!)
    [:div
     (when current-route
       [(-> current-route :data :view)])]))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (init-routes!)
  (rdom/render [current-page]
               (.getElementById js/document "app")))

(defn init! []
  (ajax/load-interceptors!)
  (rf/dispatch-sync [:app/initialize])
  (mount-components))
