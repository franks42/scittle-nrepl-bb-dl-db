#!/usr/bin/env bb
;; One-shot nREPL evaluation using project's existing client
(require '[clojure.java.io :as io])

(load-file "src/nrepl_mcp_server/nrepl/client.clj")

(let [code (first *command-line-args*)
      port 1667]
  (when code
    (let [conn (nrepl-mcp-server.nrepl.client/connect "localhost" port)
          response (nrepl-mcp-server.nrepl.client/message conn {:op "eval" :code code})
          result (first (filter :value response))]
      (when result
        (println (:value result)))
      (nrepl-mcp-server.nrepl.client/close conn))))