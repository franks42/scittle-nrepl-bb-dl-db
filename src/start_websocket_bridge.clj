#!/usr/bin/env bb

(println "🔗 Loading WebSocket bridge dependencies...")
(require '[sci.nrepl.browser-server :as bp])

(println "🚀 Starting nREPL-WebSocket bridge with FIXED ports...")

;; Use fixed ports - no discovery needed
(let [nrepl-port 1339
      websocket-port 1341]
  
  (println (str "🔍 Starting bridge on fixed ports..."))
  (println (str "   nREPL: " nrepl-port))
  (println (str "   WebSocket: " websocket-port))
  
  ;; Start with fixed ports
  (bp/start! {:nrepl-port nrepl-port 
              :websocket-port websocket-port})
  
  (println "✅ nREPL-WebSocket bridge started successfully!")
  (println (str "   nREPL port: " nrepl-port))  
  (println (str "   WebSocket port: " websocket-port)))