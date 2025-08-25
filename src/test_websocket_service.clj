#!/usr/bin/env bb

(require '[websocket-service :refer [start-browser-nrepl-server!
                                     stop-browser-nrepl-server!
                                     websocket-server-status
                                     get-websocket-ports
                                     websocket-discovery-response]])

(println "🔗 Testing WebSocket Service")
(println "")

(println "📊 Initial status:")
(println (websocket-server-status))
(println "")

(println "🚀 Starting browser nREPL server with WebSocket...")
(let [result (start-browser-nrepl-server!)]
  (println "Result:" result)
  (if (= (:status result) :started)
    (do
      (println "✅ WebSocket server started successfully!")
      (println "  nREPL port:" (:nrepl-port result))
      (println "  WebSocket port:" (:websocket-port result))
      (println "")
      
      (println "📊 Server status after start:")
      (println (websocket-server-status))
      (println "")
      
      (println "🔍 WebSocket ports discovery:")
      (println (get-websocket-ports))
      (println "")
      
      (println "📄 Discovery API JSON response:")
      (println (websocket-discovery-response))
      (println "")
      
      (println "🛑 Stopping server...")
      (println (stop-browser-nrepl-server!)))
    (println "❌ Failed to start WebSocket server:" (:error result))))

(println "")
(println "📊 Final status:")
(println (websocket-server-status))
(println "")
(println "✅ WebSocket service test completed!")