(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [fuelsurcharges.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [mount.core :as mount]
   [fuelsurcharges.core :refer [start-app]]
   [fuelsurcharges.db.core]
   [eastwood.lint :refer [eastwood] :as e]
   [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'fuelsurcharges.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'fuelsurcharges.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'fuelsurcharges.db.core/datasource)
  (mount/start #'fuelsurcharges.db.core/datasource))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  ([]
   (migrations/migrate ["migrate"] (select-keys env [:database-url])))
  ([id]
   (migrations/migrate ["migrate" id] (select-keys env [:database-url]))))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn pending
  "Get pending database migrations."
  []
  (migrations/migrate ["pending"] (select-keys env [:database-url])))

(defn destroy
  "Destroy database migration."
  [id]
  (migrations/migrate ["destroy" id] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))

(comment (start))
(comment (restart))
(comment (stop))
(comment (pending))
(comment (reset-db))
(comment (migrate))
(comment (migrate "20200701123614"))
(comment (destroy "202007011158"))
(comment (rollback))
(comment (create-migration "fuel-surcharges-add-fuel-surcahrge-column"))
(comment (create-migration "fuel-surcharge-tables"))
(comment (create-migration "fuel-surcharge-table-rows"))
(comment (eastwood))
