(defproject fuelsurcharges "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.10.0"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/semantic-ui-react "0.64.0-0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.fasterxml.jackson.core/jackson-core "2.11.0"]
                 [com.fasterxml.jackson.core/jackson-databind "2.11.0"]
                 [com.google.javascript/closure-compiler-unshaded "v20200504" :scope "provided"]
                 [cprop "0.1.17"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [expound "0.8.4"]
                 [funcool/struct "1.4.0"]
                 [luminus-http-kit "0.1.6"]
                 [luminus-migrations "0.6.7"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.4"]
                 [metosin/muuntaja "0.6.7"]
                 [clj-http "3.10.1"]
                 [org.slf4j/slf4j-nop "1.7.2"]
                 [clj-bonecp-url "0.1.1"]
                 [metosin/reitit "0.5.5"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [nrepl "0.7.0"]
                 [org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/clojurescript "1.10.764" :scope "provided"]
                 [org.clojure/core.async "1.1.582"]
                 [org.clojure/google-closure-library "0.0-20191016-6ae1f72f" :scope "provided"]
                 [org.clojure/google-closure-library-third-party "0.0-20191016-6ae1f72f" :scope "provided"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.postgresql/postgresql "42.2.11"]
                 [org.webjars/webjars-locator "0.40"]
                 [clojure.java-time "0.3.2"]
                 [camel-snake-kebab "0.4.1"]
                 [re-frame "0.12.0"]
                 [reagent "0.10.0"]
                 [honeysql "1.0.444"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-defaults "0.3.2"]
                 [kwrooijen/gungnir "da90f233416bd810a3b8310edb07f942b7420c8b"]
                 [selmer "1.12.27"]
                 [metosin/malli "81f38dda15e65efeda09e185d2938f298d4b6704"]
                 [missionary "b.17"]
                 [seancorfield/next.jdbc "1.1.569"]
                 [dk.ative/docjure "1.14.0"]
                 [district0x.re-frame/google-analytics-fx "1.0.0"]
                 [orchestra "2020.07.12-1"]
                 [thheller/shadow-cljs "2.10.18"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot fuelsurcharges.core

  :plugins [[lein-shadow "0.2.1"]
            [reifyhealth/lein-git-down "0.3.7"]]
  :middleware [lein-git-down.plugin/inject-properties]
  :repositories [["public-github" {:url "git://github.com"}]]
  :clean-targets ^{:protect false} [:target-path "target/cljsbuild" ".shadow-cljs"]

  :profiles
  {:uberjar {:omit-source    true
             :prep-tasks     ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
             :aot            :all
             :uberjar-name   "fuelsurcharges.jar"
             :source-paths   ["env/prod/clj" "env/prod/cljc" "env/prod/cljs"]
             :resource-paths ["env/prod/resources"]}

   :dev  [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts       ["-Dconf=dev-config.edn" ]
                  :dependencies   [[binaryage/devtools "1.0.0"]
                                   [cider/piggieback "0.5.0"]
                                   [pjstadig/humane-test-output "0.10.0"]
                                   [jonase/eastwood "0.3.5" :exclusions [org.clojure/clojure]]
                                   [prone "2020-01-17"]
                                   [re-frisk "1.3.2"]
                                   [day8.re-frame/re-frame-10x "0.6.5"]
                                   [ring/ring-devel "1.8.1"]
                                   [ring/ring-mock "0.4.0"]]
                  :plugins        [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                   [jonase/eastwood "0.3.5"]]
                  :source-paths   ["env/dev/clj" "env/dev/cljc" "env/dev/cljs"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options   {:init-ns user
                                   :timeout 240000}
                  :injections     [(require 'pjstadig.humane-test-output)
                                   (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts       ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"]


                  }
   :profiles/dev  {}
   :profiles/test {}})
