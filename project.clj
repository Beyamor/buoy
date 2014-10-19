(defproject buoy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :profiles {:dev {:dependencies [[speclj "3.0.0"]]}}
  :cljsbuild {:builds {:dev  {:source-paths   ["src" "spec"]
                              :compiler       {:output-to     "app/js/main_dev.js"
                                               :optimizations :whitespace}
                              :notify-command ["phantomjs"  "spec/speclj" "app/js/main_dev.js"]}

                       :prod {:source-paths ["src"]
                              :compiler     {:output-to     "app/js/main.js"
                                             :optimizations :whitespace}}}
              :test-commands {"test" ["phantomjs" "spec/speclj" "app/js/main_dev.js"]}})
