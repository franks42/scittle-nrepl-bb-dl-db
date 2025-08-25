(ns html-generator
  (:require [clojure.string :as str]))

(defn get-websocket-ports
  "Get WebSocket ports from service if available"
  []
  (try
    (require 'websocket-service)
    (let [get-ports-fn (resolve 'websocket-service/get-websocket-ports)]
      (if get-ports-fn 
        (get-ports-fn)
        {:status "service-not-loaded"}))
    (catch Exception e
      {:status "error" :error (.getMessage e)})))

(defn generate-html-page
  "Generate index.html from template with port substitution"
  [nrepl-port websocket-port]
  (let [template (slurp "template.html")
        timestamp (str (java.time.Instant/now))
        html (-> template
                 (str/replace "{{NREPL_PORT}}" (str nrepl-port))
                 (str/replace "{{WEBSOCKET_PORT}}" (str websocket-port))
                 (str/replace "{{TIMESTAMP}}" timestamp))]
    (spit "index.html" html)
    {:nrepl-port nrepl-port
     :websocket-port websocket-port
     :timestamp timestamp
     :file "index.html"}))

(defn generate-html-with-discovery
  "Generate index.html with automatic WebSocket port discovery"
  [nrepl-port]
  (let [websocket-info (get-websocket-ports)
        websocket-port (if (= (:status websocket-info) :running)
                         (:websocket-port websocket-info)
                         1340)] ; fallback port
    (generate-html-page nrepl-port websocket-port)))