#!/usr/bin/env bb

(require '[http-server :refer [start-http-server stop-http-server]]
         '[html-generator :refer [generate-html-page generate-html-with-discovery get-websocket-ports]]
         '[websocket-service :refer [start-browser-nrepl-server! websocket-server-status]]
         '[cheshire.core :as json]
         '[clojure.java.shell :as shell])

(println "🔗 Testing WebSocket Integration - Both HTTP API and HTML Embedding")
(println "")

;; Test 1: HTTP Server with API endpoints
(println "📊 Test 1: HTTP Server with API Endpoints")
(println "🚀 Starting HTTP server...")
(let [http-server (start-http-server)]
  (if (= (:status http-server) :running)
    (do
      (println "✅ HTTP server started on port" (:port http-server))
      (println "")
      
      ;; Test API endpoints
      (println "🔍 Testing API endpoints:")
      (let [port (:port http-server)
            base-url (str "http://localhost:" port)]
        
        ;; Test WebSocket discovery API
        (println "  📡 WebSocket Discovery API:")
        (let [result (shell/sh "curl" "-s" (str base-url "/api/websocket/discovery"))]
          (if (= (:exit result) 0)
            (do
              (println "    ✅ Response received:")
              (println "   " (:out result)))
            (println "    ❌ Failed to call API:" (:err result))))
        
        (println "")
        
        ;; Test server status API
        (println "  📊 Server Status API:")
        (let [result (shell/sh "curl" "-s" (str base-url "/api/server/status"))]
          (if (= (:exit result) 0)
            (do
              (println "    ✅ Response received:")
              (println "   " (:out result)))
            (println "    ❌ Failed to call API:" (:err result)))))
      
      (println "")
      (println "🛑 Stopping HTTP server...")
      (stop-http-server http-server))
    (println "❌ Failed to start HTTP server")))

(println "")

;; Test 2: HTML Generation with WebSocket Discovery
(println "📊 Test 2: HTML Generation with Port Discovery")
(println "")

(println "🔍 WebSocket service status:")
(println (websocket-server-status))
(println "")

(println "📡 WebSocket port discovery:")
(let [ports (get-websocket-ports)]
  (println ports)
  (println ""))

(println "📄 Generating HTML with discovery:")
(let [nrepl-port 7890
      result (generate-html-with-discovery nrepl-port)]
  (println "Result:" result)
  (if (:file result)
    (do
      (println "✅ HTML generated successfully!")
      (println "📄 Generated HTML preview:")
      (let [html-content (slurp (:file result))
            lines (take 20 (clojure.string/split-lines html-content))]
        (dorun (map-indexed #(println (format "%2d: %s" (inc %1) %2)) lines)))
      (println "... (truncated)"))
    (println "❌ Failed to generate HTML")))

(println "")

;; Test 3: Manual HTML Generation
(println "📊 Test 3: Manual HTML Generation with Fixed Ports")
(let [nrepl-port 7890
      websocket-port 1340
      result (generate-html-page nrepl-port websocket-port)]
  (println "Manual generation result:" result)
  (if (:file result)
    (println "✅ Manual HTML generation successful!")
    (println "❌ Manual HTML generation failed")))

(println "")
(println "✅ WebSocket integration test completed!")
(println "")
(println "📋 Summary:")
(println "  • HTTP API endpoints: /api/websocket/discovery, /api/server/status")
(println "  • HTML generation with automatic WebSocket discovery")
(println "  • Template substitution for embedded port configuration")
(println "  • Both approaches available for port discovery")