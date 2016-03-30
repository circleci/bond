(ns bond.test.test-runner
  (:require [bond.test.james]
            [cljs.test :as test :include-macros true]))

(enable-console-print!)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  ;; test/run-tests does not return anything, so use this test watcher
  ;; to set the exit code on the window to be used by phantomjs later.
  (aset js/window "exit-code"
        (if (cljs.test/successful? m)
          0
          1)))

(defn ^:export run-tests []
  (test/run-tests 'bond.test.james))
