(ns http-server
  "HTTP server functionality with preferred port strategy and API endpoints"
  (:require [org.httpkit.server :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(defn websocket-discovery-handler
  "API endpoint for WebSocket port discovery"
  [request]
  (try
    (require 'websocket-service)
    (let [get-ports-fn (resolve 'websocket-service/get-websocket-ports)
          ports (if get-ports-fn (get-ports-fn) {:status "service-not-loaded"})]
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Access-Control-Allow-Origin" "*"}
       :body (json/generate-string ports {:pretty true})})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"
                 "Access-Control-Allow-Origin" "*"}
       :body (json/generate-string
               {:status "error"
                :error (.getMessage e)
                :message "WebSocket service unavailable"
                :timestamp (str (java.time.Instant/now))}
               {:pretty true})})))

(defn server-status-handler
  "API endpoint for server status"
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body (json/generate-string
           {:server "Super Duper BB Server"
            :status "running"
            :services {:http {:port (get-in request [:server-info :port])}
                       :nrepl {:status "running"}
                       :websocket {:status "not-started"}}
            :timestamp (str (java.time.Instant/now))}
           {:pretty true})})

(defn api-handler
  "Handle API endpoints"
  [request]
  (let [uri (:uri request)]
    (case uri
      "/api/websocket/discovery" (websocket-discovery-handler request)
      "/api/server/status" (server-status-handler request)
      {:status 404
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:error "API endpoint not found" :uri uri})})))

(defn static-file-handler
  "Simple static file handler for resources/public"
  [request]
  (let [uri (:uri request)
        path (if (= uri "/") "/index.html" uri)
        file-path (str "resources/public" path)
        file (io/file file-path)]
    (if (and (.exists file) (.isFile file))
      {:status 200
       :headers {"Content-Type" (cond
                                  (.endsWith path ".html") "text/html"
                                  (.endsWith path ".css") "text/css"
                                  (.endsWith path ".js") "application/javascript"
                                  (.endsWith path ".json") "application/json"
                                  :else "text/plain")}
       :body (slurp file)}
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body "<!DOCTYPE html><html><body><h1>404 - File Not Found</h1></body></html>"})))

(defn router
  "Main request router"
  [request]
  (let [uri (:uri request)]
    (if (.startsWith uri "/api/")
      (api-handler request)
      (static-file-handler request))))

(defn try-port
  "Try to start HTTP server on specific port"
  [port]
  (try
    (let [server (http/run-server router {:port port})]
      (Thread/sleep 500) ; Give server time to start
      {:port port :status :running :server server
       :url (str "http://localhost:" port "/")})
    (catch Exception e
      (println (str "Port " port " failed: " (.getMessage e)))
      nil)))

(defn start-http-server
  "Start HTTP server with preferred port 37373, fallback to ephemeral ports"
  []
  (println "🌐 Starting HTTP server (preferred port 37373)...")
  (let [preferred-port 37373]
    (if-let [result (try-port preferred-port)]
      (do
        (println (str "✅ HTTP server started on preferred port " preferred-port))
        result)
      ;; Try ephemeral port range
      (do
        (println (str "⚠️  Port " preferred-port " unavailable, trying ephemeral ports..."))
        (loop [attempts 0]
          (if (< attempts 10)
            (let [ephemeral-port (+ 37374 attempts)]
              (if-let [result (try-port ephemeral-port)]
                (do
                  (println (str "✅ HTTP server started on ephemeral port " ephemeral-port))
                  result)
                (recur (inc attempts))))
            (do
              (println "❌ Failed to find available port for HTTP server")
              {:status :error :error "No available ports found"})))))))

(defn stop-http-server
  "Stop HTTP server"
  [server-info]
  (when-let [server (:server server-info)]
    (try
      (server) ; httpkit servers are functions to stop them
      (println (str "✅ HTTP server stopped on port " (:port server-info)))
      {:status :stopped :port (:port server-info)}
      (catch Exception e
        (println (str "❌ Error stopping HTTP server: " (.getMessage e)))
        {:status :error :error (.getMessage e)}))))