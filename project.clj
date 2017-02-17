(defproject circleci/bond "0.2.9"
  :version-spec "0.2.~{:env/circle_build_num}"
  :description "Spying library for testing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [com.cemerick/clojurescript.test "0.3.0"]]
  :plugins [[lein-test-out "0.3.1" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.1.3"]]
  :cljsbuild {:builds [{:source-paths ["src" "test"]
                        :compiler {:output-dir "resources/public/js/out"
                                   :output-to "resources/public/js/test-bond.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :test-commands {"unit" ["node_modules/phantomjs-prebuilt/bin/phantomjs"
                                      "resources/test/phantom/runner.js"
                                      "resources/test/test.html"]}})
