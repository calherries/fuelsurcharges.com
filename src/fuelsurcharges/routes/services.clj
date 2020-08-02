(ns fuelsurcharges.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.malli :as malli-coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [fuelsurcharges.middleware.formats :as formats]
   [fuelsurcharges.middleware.exception :as exception]
   [fuelsurcharges.validation :as validation]
   [fuelsurcharges.market :as market]
   [fuelsurcharges.fuel-surcharge :as fuel-surcharge]
   [ring.util.http-response :refer :all]
   [malli.core :as m]
   [fuelsurcharges.db.core :as db]
   [clojure.java.io :as io]))

(defn ok-body [body]
  {200 {:body body}})

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [ ;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/markets"
    {:get
     {:summary "get all markets and their price data"
      ;; :responses (ok-body {:markets [:vector
      ;;                                [:map
      ;;                                 [:prices
      ;;                                  [:list
      ;;                                   [:map [:market-id int?] [:price double?] [:price-date any?]]]]
      ;;                                 [:id int?]
      ;;                                 [:market-name string?]
      ;;                                 [:source-name string?]]]})
      :handler (fn [_]
                 (ok {:markets (market/markets-list)}))}}]

   ["/fuel-surcharge"
    {:get
     {:summary    "get a fuel surcharge rate table"
      ;; :responses  (ok-body {:table fuel-surcharge/get-fuel-surcharges-history-schema})
      :parameters {:query {:id int?}}
      :handler    (fn [{{{:keys [id]} :query} :parameters}]
                    (ok {:table (fuel-surcharge/get-current-fuel-surcharge-table-rows id)}))}}]

   ["/fuel-surcharges"
    {:get
     {:summary "get all fuel surcharges and their price history"
      :handler (fn [_]
                 (ok {:fuel-surcharges (fuel-surcharge/get-fuel-surcharges-history)}))}}]

   ["/market-prices"
    {:get
     {:summary "get all market prices"
      :handler (fn [_]
                 (ok (market/get-last-year-market-prices)))}}]])
