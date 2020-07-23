(ns fuelsurcharges.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :refer [GET POST]]
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [reitit.frontend.controllers :as rtfc]
   [reitit.frontend.easy :as rtfe]
   [fuelsurcharges.ajax :refer [as-transit]]
   [clojure.pprint :refer [pprint]]))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
    {:db         {:markets/loading? true
                  :fsc/loading?     true
                  :app/route        nil}
     :dispatch-n [[:markets/load]
                  [:fsc/load]]}))

;; Routing

;; Effects

;; Triggering navigation from events.
(rf/reg-fx
  :app/navigate
  (fn [route]
    (apply rtfe/push-state route)))

;; Events

(rf/reg-event-fx
  :app/navigate
  (fn [db [_ & route]]
    ;; See `navigate` effect in routes.cljs
    {:app/navigate route}))

(rf/reg-event-db
  :app/navigated
  (fn [db [_ new-match]]
    (let [old-match   (:app/route db)
          controllers (rtfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :app/route (assoc new-match :controllers controllers)))))

;; Subscriptions

(rf/reg-sub
  :app/route
  (fn [db]
    (:app/route db)))

;; Other Effects

(rf/reg-fx
  :ajax/get
  (fn [{:keys [url success-event error-event success-path params]}]
    (GET url (as-transit (cond-> {:headers {"Accept" "application/transit+json"}}
                           params        (assoc :params params)
                           success-event (assoc :handler
                                                #(rf/dispatch
                                                   (conj success-event
                                                         (if success-path
                                                           (get-in % success-path)
                                                           %))))
                           error-event   (assoc :error-handler
                                                #(rf/dispatch
                                                   (conj error-event %))))))))

;; how to extract date from local-date-time
(comment (tf/unparse (tf/formatter "yyyyMMdd") "hello"))

(rf/reg-sub
  :markets/loading?
  (fn [db _]
    (:markets/loading? db)))

(rf/reg-sub
  :fsc/loading?
  (fn [db _]
    (:fsc/loading? db)))

(rf/reg-event-db
  :markets/set
  (fn [db [_ markets]]
    (assoc db
           :markets/list markets
           :markets/loading? false)))

(rf/reg-event-db
  :fsc/set
  (fn [db [_ fscs]]
    (assoc db
           :fsc/list fscs
           :fsc/loading? false)))

(rf/reg-event-db
  :fsc/set-fsc-page-data
  (fn [db [_ fsc]]
    (assoc db
           :fsc/fsc-page-data fsc)))

(comment (rf/dispatch [:fsc/load]))

(rf/reg-event-db
  :errors/set
  (fn [db [_ error]]
    (assoc db
           :errors error)))

(rf/reg-event-fx
  :markets/load
  (fn [{:keys [db]} _]
    {:db       (assoc db :markets/loading? false)
     :ajax/get {:url           "/api/markets"
                :success-event [:markets/set]
                :error-event   [:errors/set]
                :success-path  [:markets]}}))

(rf/reg-event-fx
  :fsc/load
  (fn [{:keys [db]} _]
    {:db       (assoc db :fsc/loading? false)
     :ajax/get {:url           "/api/fuel-surcharges"
                :success-event [:fsc/set]
                :error-event   [:errors/set]
                :success-path  [:fuel-surcharges]}}))

(rf/reg-event-fx
  :fsc/load-fsc-page-data
  (fn [{:keys [db]} [_ id]]
    {:ajax/get {:url           "/api/fuel-surcharge"
                :params        {:id id}
                :success-event [:fsc/set-fsc-page-data]
                :error-event   [:errors/set]}}))

(defn prices-row
  [{:keys [id market-id price currency]}]
  (price))

(rf/reg-sub
  :db
  (fn [db _]
    (identity db)))

(rf/reg-sub
  :markets/markets
  (fn [db _]
    (:markets/list db)))

(rf/reg-sub
  :fsc/list
  (fn [db _]
    (:fsc/list db)))

(rf/reg-sub
  :fsc/fsc-by-id
  :<- [:fsc/list]
  (fn [fscs [_ id]]
    (->> fscs
         (filter (comp #{1} :id))
         first)))

(rf/reg-sub
  :fsc/current-id
  (fn [fscs [_ id]]
    (->> fscs
         (filter (comp #{1} :id))
         first)))

(rf/reg-sub
  :markets/selected
  :<- [:markets/markets]
  :<- [:app/route]
  (fn [[fscs route] [_ id]]
    (when (#{:market} (-> route :data :name))
      (let [id (-> route :parameters :path :id)]
        (->> fscs
             (filter (comp #{id} :id))
             first)))))

(rf/reg-sub
  :fsc/selected-fsc
  :<- [:fsc/list]
  :<- [:app/route]
  (fn [[fscs route] [_ id]]
    (when (#{:fsc} (-> route :data :name))
      (let [id (-> route :parameters :path :id)]
        (->> fscs
             (filter (comp #{id} :id))
             first)))))

(rf/reg-sub
  :fsc/selected-fsc-table
  (fn [db _]
    (-> db :fsc/fsc-page-data :table)))

(rf/reg-event-db
  :user/subscribe
  (fn [db [_ email]]
    (assoc db :user/email email)))
