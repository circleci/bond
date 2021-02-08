(defproject circleci/bond "0.4.0"
  :description "Spying library for testing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :plugins [[lein-cloverage "1.0.9"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  ;; by default, lein-cloverage will
                                  ;; use the latest release of
                                  ;; cloverage. Specify the version of
                                  ;; cloverage to override this.
                                  [cloverage "1.0.9"]]}})
