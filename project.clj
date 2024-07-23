(defproject circleci/bond "0.6.0"
  :description "Spying library for testing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies []

  :plugins [[lein-cloverage "1.2.4"]
            [jonase/eastwood "1.4.3" :exclusions [org.clojure/clojure]]]

  :eastwood {:exclude-linters
             [;; clj-kondo will catch the wrong-arity cases
              ;; (eastwood won't let us turn this lint off for a single namespace)
              :wrong-arity]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.2"]
                                  [lambdaisland/kaocha "1.91.1392"]
                                  [lambdaisland/kaocha-cloverage "1.1.89"]
                                  [lambdaisland/kaocha-junit-xml "1.17.101"]]}}

  :aliases {"test"    ["run" "-m" "kaocha.runner"
                       "--no-randomize"]
            "test-ci" ["test"
                       "--plugin" "cloverage"
                       "--plugin" "kaocha.plugin/profiling"
                       "--plugin" "kaocha.plugin/junit-xml"
                       "--junit-xml-file" "target/test-results/results.xml"]}

  :repositories [["releases" {:url "https://clojars.org/repo"
                              :username :env/clojars_username
                              :password :env/clojars_token
                              :sign-releases false}]
                 ["snapshots" {:url "https://clojars.org/repo"
                               :username :env/clojars_username
                               :password :env/clojars_token
                               :sign-releases false}]])
