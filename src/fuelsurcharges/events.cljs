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
    {:db       {:markets/loading? true}
     :dispatch [:markets/load]}))

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

(rf/reg-event-db
  :markets/set
  (fn [db [_ markets]]
    (assoc db
           :markets/list markets
           :markets/loading? false)))

(rf/reg-event-db
  :errors/set
  (fn [db [_ error]]
    (assoc db
           :errors error)))

(rf/reg-event-fx
  :markets/load
  (fn [{:keys [db]} _]
    {:db       (assoc db :markets/loading? false)
     :ajax/get {:url           "/api/market-prices"
                :success-event [:markets/set]
                :error-event   [:errors/set]
                :success-path  [:market-prices]}}))

(comment (rf/dispatch [:markets/load]))

(defn prices-row
  [{:keys [id market-id price currency]}]
  (price))

(rf/reg-sub
  :markets/prices-list
  (fn [db _]
    (:markets/list db)))


(defn unparse-date [date]
  (tf/unparse (tf/formatter "YYYY-MM-dd") date))

(rf/reg-sub
  :markets/prices-list-by-id
  :<- [:markets/prices-list]
  (fn [prices-list [_ id]]
    (->> prices-list
         (filter #(= (:market-id %) id))
         (map #(update % :price-date unparse-date))
         (map #(select-keys % [:price-date :price])))))

(rf/reg-sub
  :markets/list
  :<- [:markets/prices-list]
  (fn [prices-list _]
    (->> prices-list
         (map :market-id)
         set
         vec)))
