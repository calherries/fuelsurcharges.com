(ns fuelsurcharges.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :refer [GET POST]]
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [fuelsurcharges.ajax :refer [as-transit]]
   [clojure.pprint :refer [pprint]]))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
    {:db         {:markets/loading? true
                  :fsc/loading?     true}
     :dispatch-n [[:markets/load]
                  [:fsc/load]]}))

(rf/reg-fx
  :ajax/get
  (fn [{:keys [url success-event error-event success-path]}]
    (GET url (as-transit (cond-> {:headers {"Accept" "application/transit+json"}}
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

;; (defn unparse-date [date]
;;   (tf/unparse (tf/formatter "YYYY-MM-dd") date))

;; (rf/reg-sub
;;   :markets/market-list
;;   :<- [:markets/markets]
;;   (fn [markets _]
;;     (->> markets
;;          (map #(update-in % [:prices :price-date] unparse-date)))))
