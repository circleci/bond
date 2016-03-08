(defproject bond "0.2.7"
  :description "Spying library for testing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :plugins [[lein-cloverage "1.0.7-SNAPSHOT"]
            [lein-test-out "0.3.1" :exclusions [org.clojure/clojure]]])
