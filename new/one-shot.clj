#!/usr/bin/env bb
;; One-shot nREPL evaluation using project's client

(require '[clojure.string :as str])

;; Load the project's nREPL client
(load-file "src/nrepl_mcp_server/nrepl_client/core.clj")
(load-file "src/nrepl_mcp_server/nrepl_client/bencode.clj")
(load-file "src/nrepl_mcp_server/nrepl_client/transport.clj")

(let [code (first *command-line-args*)
      port 1667]
  (when code
    (try
      (let [conn (nrepl-mcp-server.nrepl-client.core/connect "localhost" port)
            response (nrepl-mcp-server.nrepl-client.core/message conn {:op "eval" :code code})
            value-response (first (filter :value response))]
        (when value-response
          (println (:value value-response)))
        (nrepl-mcp-server.nrepl-client.transport/close conn))
      (catch Exception e
        (println "Error:" (.getMessage e))))))