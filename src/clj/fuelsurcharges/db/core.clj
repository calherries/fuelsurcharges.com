(ns fuelsurcharges.db.core
  (:require
   [cheshire.core :refer [generate-string parse-string]]
   [next.jdbc.date-time]
   [next.jdbc.prepare]
   [next.jdbc.result-set]
   [next.jdbc.sql :as jsql]
   [next.jdbc :as jdbc]
   [clojure.tools.logging :as log]
   [fuelsurcharges.config :refer [env]]
   [mount.core :refer [defstate]]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [gungnir.model :as gm]
   [gungnir.database :refer [make-datasource! *database*]]
   [gungnir.query :as q]
   [gungnir.changeset :as changeset]
   [clj-bonecp-url.core :refer [parse-url]]
   [gungnir.model :refer [register!]]
   [hikari-cp.core :as hikari-cp]
   [clojure.walk :as walk]
   [honeysql.core :as sql])
  (:import [org.postgresql.util PGobject]
           [java.time LocalDate]
           java.net.URI))

(defstate datasource
  :start (if-let [database-url (env :database-url)]
           (make-datasource! (if (clojure.string/starts-with? database-url "postgres:")
                               (-> (parse-url database-url)
                                   (dissoc :adapter)
                                   (assoc :subprotocol "postgresql"))
                               {:adapter  "postgresql"
                                :jdbc-url database-url
                                :username "fuelsurcharges"}))
           (do (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
               *database*))
  :stop (hikari-cp/close-datasource *database*))

(comment (jdbc/with-db-connection [conn {:datasource *database*}]
           (let [rows (jdbc/query conn "SELECT table_name FROM information_schema.tables where table_schema = 'public'")]
             (println rows))))

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
  (cske/transform-keys  #(-> %
                             (clojure.string/replace #"\." "/")
                             (clojure.string/replace #"_" "-")
                             (subs 1)
                             keyword)
                        m))

(defn query! [query]
  (kebab-case-keys (jsql/query *database* query)))

(defn execute! [query]
  (kebab-case-keys (jdbc/execute! *database* query)))

(defn honey->sql
  ([m] (honey->sql m {}))
  ([m opts]
   (sql/format m
               :namespace-as-table? (:namespace-as-table? opts true)
               :quoting :ansi)))

(defn insert! [table m]
  (-> m
      (changeset/cast table)
      changeset/changeset
      q/save!))

(defn insert-many! [table rows]
  (-> (q/insert-into table)
      (q/values (map #(changeset/cast % table) rows))
      honey->sql
      execute!))

(defn delete!
  ([table id]
   (delete! table (gm/primary-key table) id))
  ([table k v]
   (-> (q/delete-from table)
       (q/where [:= k v])
       honey->sql
       execute!)))

;; Utilities for generating malli for a given table
(defn select-all [table]
  (-> (q/select :*)
      (q/from table)
      honey->sql
      query!))


(defn map->nsmap
  [n m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (name n) (name k))
                              k)]
                 (assoc acc new-kw v)))
             {} m))

(defn select-all-namespaced [table]
  (->> (select-all table)
       (map (partial map->nsmap table))))
