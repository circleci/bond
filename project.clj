(defproject circleci/bond "0.5.0"
  :description "Spying library for testing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies []

  :plugins [[lein-cloverage "1.2.2"]
            [jonase/eastwood "0.3.14" :exclusions [org.clojure/clojure]]]

  :eastwood {:exclude-linters
             [;; clj-kondo will catch the wrong-arity cases
              ;; (eastwood won't let us turn this lint off for a single namespace)
              :wrong-arity]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.2"]
                                  [lambdaisland/kaocha "0.0-601"]
                                  [lambdaisland/kaocha-cloverage "0.0-41"]
                                  [lambdaisland/kaocha-junit-xml "0.0-70"]]}}

  :aliases {"test"    ["run" "-m" "kaocha.runner"
                       "--no-randomize"]
            "test-ci" ["test"
                       "--plugin" "cloverage"
                       "--codecov" "--no-cov-html" "--no-cov-summary"
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
