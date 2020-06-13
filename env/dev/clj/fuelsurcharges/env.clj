(ns fuelsurcharges.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [fuelsurcharges.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[fuelsurcharges started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[fuelsurcharges has shut down successfully]=-"))
   :middleware wrap-dev})
