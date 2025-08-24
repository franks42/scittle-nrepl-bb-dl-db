(ns html-generator
  (:require [clojure.string :as str]))

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