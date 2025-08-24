#!/usr/bin/env bb
(require '[babashka.nrepl.client :as client])

(let [code (first *command-line-args*)
      port 1667
      conn (client/connect {:port port})
      response (client/message conn {:op "eval" :code code})
      result (first (filter #(contains? % :value) response))]
  (when result
    (println (:value result)))
  (client/close conn))