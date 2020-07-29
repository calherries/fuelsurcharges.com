(ns fuelsurcharges.db.core
  (:require
   [cheshire.core :refer [generate-string parse-string]]
   [next.jdbc.date-time]
   [next.jdbc.prepare]
   [next.jdbc.result-set]
   [clojure.tools.logging :as log]
   [conman.core :as conman]
   [fuelsurcharges.config :refer [env]]
   [mount.core :refer [defstate]]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [gungnir.changeset :refer [changeset]]
   [gungnir.database :refer [make-datasource! *database*]]
   [gungnir.query :as q]
   [gungnir.model :refer [register!]]
   [clojure.walk :as walk]
   [hugsql.adapter :as hsqla]
   [hugsql.core :as hsqlc])
  (:import [org.postgresql.util PGobject]
           [java.time LocalDate]))

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defstate datasource
  :start (do (make-datasource! (env :datasource-opts))))

(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
  (let [type  (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json"   (parse-string value true)
      "jsonb"  (parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))
  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))

(defn kebab-case-keys
  "Converts all the keys in the given map to kebab-case"
  [m]
  (cske/transform-keys csk/->kebab-case-keyword m))

(defn result-one-format
  [this result options]
  (->> (hsqla/result-one this result options)
       kebab-case-keys))

(defn result-many-format
  [this result options]
  (->> (hsqla/result-many this result options)
       (map kebab-case-keys)))

(defmethod hsqlc/hugsql-result-fn :1 [_sym] 'fuelsurcharges.db.core/result-one-format)
(defmethod hsqlc/hugsql-result-fn :one [_sym] 'fuelsurcharges.db.core/result-one-format)
(defmethod hsqlc/hugsql-result-fn :* [_sym] 'fuelsurcharges.db.core/result-many-format)
(defmethod hsqlc/hugsql-result-fn :many [_sym] 'fuelsurcharges.db.core/result-many-format)
