#!/usr/bin/env bb

(require '[babashka.nrepl.server :as nrepl]
         '[babashka.fs :as fs]
         '[babashka.process :as p]
         '[org.httpkit.server :as http]
         '[clojure.java.io :as io])

(def state-file ".bb-super-duper-server")

(defn read-state []
  (when (fs/exists? state-file)
    (try
      (read-string (slurp state-file))
      (catch Exception _ nil))))

(defn process-running? [pid]
  (try
    (let [result (p/shell {:out :string :err :string :continue true}
                          "kill" "-0" (str pid))]
      (zero? (:exit result)))
    (catch Exception _ false)))

(defn check-existing-server []
  (when-let [state (read-state)]
    (let [nrepl-main (get-in state [:services :nrepl-main])
          {:keys [pid port]} nrepl-main]
      (when (and pid (process-running? pid))
        {:running true :pid pid :port port :state state}))))

(defn start-server []
  (if-let [{:keys [pid port]} (check-existing-server)]
    (do
      (binding [*out* *err*]
        (println (str "❌ ERROR: nREPL server already running on port " port " (PID: " pid ")"))
        (println "Aborting new server startup."))
      (println (pr-str {:status :error
                        :reason :already-running
                        :existing {:pid pid :port port}}))
      (System/exit 1))
    ;; Fork and start server in background
    (let [_ (p/process ["bb" "-e"
                        (pr-str
                         `(do
                            (require '[babashka.nrepl.server :as nrepl])
                            (let [server# (nrepl/start-server! {:host "localhost" :port 0})
                                  port# (-> server# :socket (.getLocalPort))
                                  pid# (.pid (java.lang.ProcessHandle/current))
                                  started-at# (str (java.time.Instant/now))]
                              (spit ~state-file (pr-str {:services
                                                         {:nrepl-main {:pid pid#
                                                                       :hostname "localhost"
                                                                       :port port#
                                                                       :status :running
                                                                       :started-at started-at#}}
                                                         :startup-phase :nrepl-main
                                                         :created-at started-at#}))
                              (println (str "Started nREPL server at localhost:" port#))
                              @(promise))))]
                       {:inherit false})]
      (Thread/sleep 500) ; Give it time to start
      (if-let [{:keys [pid port state]} (check-existing-server)]
        (do
          (binding [*out* *err*]
            (println (str "✅ nREPL server started on localhost:" port " (PID: " pid ")"))
            (println "Server state saved to" state-file))
          (println (pr-str (assoc-in state [:services :nrepl-main :status] :started))))
        (do
          (binding [*out* *err*]
            (println "❌ Failed to start nREPL server"))
          (println (pr-str {:status :error :reason :start-failed}))
          (System/exit 1))))))

(defn stop-server []
  (if-let [{:keys [pid port]} (check-existing-server)]
    (try
      (p/shell "kill" (str pid))
      (Thread/sleep 500)
      (when (process-running? pid)
        (p/shell "kill" "-9" (str pid)))
      (fs/delete-if-exists state-file)
      (binding [*out* *err*]
        (println (str "✅ Stopped nREPL server on port " port " (PID: " pid ")")))
      (println (pr-str {:status :stopped :pid pid :port port}))
      (catch Exception e
        (binding [*out* *err*]
          (println (str "❌ Error stopping server: " (.getMessage e))))
        (println (pr-str {:status :error :reason :stop-failed :message (.getMessage e)}))
        (System/exit 1)))
    (do
      (binding [*out* *err*]
        (println "ℹ️  No nREPL server is running"))
      (println (pr-str {:status :not-running})))))

(defn server-status []
  (if-let [{:keys [pid port state]} (check-existing-server)]
    (do
      (binding [*out* *err*]
        (println (str "✅ nREPL server is running on port " port " (PID: " pid ")"))
        (when-let [started (get-in state [:services :nrepl-main :started-at])]
          (println (str "   Started at: " started))))
      (println (pr-str (assoc-in state [:services :nrepl-main :status] :running))))
    (do
      (binding [*out* *err*]
        (println "ℹ️  No nREPL server is running"))
      (println (pr-str {:status :not-running})))))

(defn restart-server []
  (when (check-existing-server)
    (stop-server))
  (Thread/sleep 500)
  (start-server))

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

(defn try-port
  "Try to start HTTP server on specific port"
  [port]
  (try
    (let [server (http/run-server static-file-handler {:port port})]
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

(defn get-server-info
  "Returns comprehensive server information for nREPL discovery"
  []
  (if-let [state (read-state)]
    (assoc state :status :running :state-file state-file)
    {:status :not-running :state-file state-file}))

;; Main command dispatcher
(let [command (first *command-line-args*)]
  (case command
    "start" (start-server)
    "stop" (stop-server)
    "status" (server-status)
    "restart" (restart-server)
    (do
      (binding [*out* *err*]
        (println "Usage: bb run bb-dev-nrepl-server [start|stop|status|restart]"))
      (System/exit 1))))