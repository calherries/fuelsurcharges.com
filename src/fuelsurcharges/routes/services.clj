(ns fuelsurcharges.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [fuelsurcharges.middleware.formats :as formats]
   [fuelsurcharges.middleware.exception :as exception]
   [fuelsurcharges.markets :as markets]
   [fuelsurcharges.fuel-surcharges :as fsc]
   [ring.util.http-response :refer :all]
   [fuelsurcharges.db.core :as db]
   [clojure.java.io :as io]))

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
     (fn [_]
       (ok {:markets (markets/markets-list)}))}]

   ["/fuel-surcharge"
    {:get
     {:responses  {200 {:body {:table [{:price            number?
                                        :surcharge-amount number?}]}}}
      :parameters {:query {:id int?}}
      :handler    (fn [{{{:keys [id]} :query} :parameters}]
                    (ok {:table (db/get-current-fuel-surcharge-table-rows {:id id})}))}}]

   ["/fuel-surcharges"
    {:get
     (fn [_]
       (ok {:fuel-surcharges (fsc/get-fuel-surcharges-history)}))}]
   ;; {:get
   ;;  {:responses
   ;;   {200 {:body
   ;;         {:messages
   ;;          [{:id        pos-int? ;;            :name      string?
   ;;            :message   string?
   ;;            :timestamp inst?}]}}}
   ;;   :handler
   ;;   (fn [_]
   ;;     (ok (msg/message-list)))}}]
   ["/market-prices"
    {:get
     (fn [_]
       (ok (markets/market-prices-list)))}]])
