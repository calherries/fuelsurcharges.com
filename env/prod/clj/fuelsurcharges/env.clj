(ns fuelsurcharges.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[fuelsurcharges started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[fuelsurcharges has shut down successfully]=-"))
   :middleware identity})
