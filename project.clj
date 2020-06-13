(defproject fuelsurcharges "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.10.0"]
                 [cljs-ajax "0.8.0"]
                 [clojure.java-time "0.3.2"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.fasterxml.jackson.core/jackson-core "2.11.0"]
                 [com.fasterxml.jackson.core/jackson-databind "2.11.0"]
                 [com.google.javascript/closure-compiler-unshaded "v20200504" :scope "provided"]
                 [conman "0.8.9"]
                 [cprop "0.1.17"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [expound "0.8.4"]
                 [funcool/struct "1.4.0"]
                 [luminus-http-kit "0.1.6"]
                 [luminus-migrations "0.6.7"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.4"]
                 [metosin/jsonista "0.2.6"]
                 [metosin/muuntaja "0.6.7"]
                 [metosin/reitit "0.5.2"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [nrepl "0.7.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.764" :scope "provided"]
                 [org.clojure/core.async "1.1.582"]
                 [org.clojure/google-closure-library "0.0-20191016-6ae1f72f" :scope "provided"]
                 [org.clojure/google-closure-library-third-party "0.0-20191016-6ae1f72f" :scope "provided"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.postgresql/postgresql "42.2.11"]
                 [org.webjars.npm/bulma "0.8.2"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.40"]
                 [re-frame "0.12.0"]
                 [reagent "0.10.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.27"]
                 [thheller/shadow-cljs "2.9.2" :scope "provided"]]

  :min-lein-version "2.0.0"

  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot fuelsurcharges.core

  :plugins [[lein-shadow "0.2.0"]]
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]

  :npm-deps [[shadow-cljs "2.9.2"]
             [create-react-class "15.6.3"]
             [react "16.8.6"]
             [react-dom "16.8.6"]]
  :npm-dev-deps [[xmlhttprequest "1.8.0"]]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks  ["compile" ["shadow" "release" "app"]]

             :aot            :all
             :uberjar-name   "fuelsurcharges.jar"
             :source-paths   ["env/prod"  "env/prod" ]
             :resource-paths ["env/prod/resources"]}

   :dev  [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]

   :project/dev {:jvm-opts     ["-Dconf=dev-config.edn" ]
                 :dependencies [[binaryage/devtools "1.0.0"]
                                [cider/piggieback "0.5.0"]
                                [pjstadig/humane-test-output "0.10.0"]
                                [prone "2020-01-17"]
                                [re-frisk "1.3.2"]
                                [day8.re-frame/re-frame-10x "0.6.5"]
                                [ring/ring-devel "1.8.1"]
                                [ring/ring-mock "0.4.0"]]
                 :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                [jonase/eastwood "0.3.5"]]


                 :source-paths   ["env/dev"  "env/dev" "test" ]
                 :resource-paths ["env/dev/resources"]
                 :repl-options   {:init-ns user
                                  :timeout 120000}
                 :injections     [(require 'pjstadig.humane-test-output)
                                  (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts       ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"]


                  }
   :profiles/dev  {}
   :profiles/test {}})
