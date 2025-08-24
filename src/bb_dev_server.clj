(ns bb-dev-server
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(def config-file "bb-dev-server.edn")
(def pid-file ".bb-dev-server")

(defn load-config []
  (when (.exists (io/file config-file))
    (edn/read-string (slurp config-file))))

(defn read-server-state []
  (when (.exists (io/file pid-file))
    (try
      (json/parse-string (slurp pid-file) true)
      (catch Exception _
        nil))))

(defn write-server-state [state]
  ;; Convert timestamps to strings for JSON serialization
  (let [json-state (update state :started-at str)]
    (spit pid-file (json/generate-string json-state {:pretty true}))))

(defn delete-server-state []
  (when (.exists (io/file pid-file))
    (.delete (io/file pid-file))))

(defn process-running? [pid]
  (when pid
    (try
      (let [result (shell/sh "kill" "-0" (str pid))]
        (= 0 (:exit result)))
      (catch Exception _
        false))))

(defn get-current-pid []
  ;; Use shell to get current PID for Babashka compatibility
  (try
    (let [result (shell/sh "sh" "-c" "echo $$")]
      (if (= 0 (:exit result))
        (Long/parseLong (str/trim (:out result)))
        (System/currentTimeMillis)))
    (catch Exception _
      (System/currentTimeMillis))))

(defn start-server []
  (let [config (load-config)
        current-state (read-server-state)]
    
    ;; Check if already running
    (when (and current-state 
               (process-running? (:pid current-state)))
      (println "Server already running with PID:" (:pid current-state))
      (System/exit 0))
    
    ;; Clean up stale state file
    (when current-state
      (delete-server-state))
    
    (let [port (get-in config [:server :nrepl-port] 7890)
          hostname (get-in config [:server :hostname] "localhost")
          pid (get-current-pid)
          deps (get-in config [:server :deps] {})]
      
      ;; Write server state
      (write-server-state {:pid pid
                           :hostname hostname  
                           :port port
                           :status "starting"
                           :started-at (java.time.Instant/now)})
      
      (println (format "Starting bb-dev-server on %s:%d with PID %d" hostname port pid))
      
      ;; Start nREPL server using Babashka's built-in capability
      (let [server-process (future 
                            (shell/sh "bb" "--nrepl-server" (str port) 
                                     "--nrepl-host" hostname))]
        
        ;; Update status to running (don't store server object)
        (write-server-state {:pid pid
                             :hostname hostname
                             :port port  
                             :status "running"
                             :started-at (java.time.Instant/now)})
        
        (println (format "✅ nREPL server started on %s:%d (PID: %d)" hostname port pid))
        (println "Server state saved to" pid-file)
        
        ;; Keep server running
        (deref (promise))))))

(defn stop-server []
  (let [state (read-server-state)]
    (if (and state (process-running? (:pid state)))
      (do
        (println (format "Stopping server with PID %d" (:pid state)))
        (shell/sh "kill" (str (:pid state)))
        (delete-server-state)
        (println "✅ Server stopped"))
      (do
        (println "❌ No running server found")
        (delete-server-state)
        (System/exit 1)))))

(defn server-status []
  (let [state (read-server-state)]
    (if state
      (if (process-running? (:pid state))
        (do
          (println (json/generate-string state {:pretty true}))
          (System/exit 0))
        (do
          (println "❌ Server not running (stale PID file)")
          (delete-server-state)
          (System/exit 1)))
      (do
        (println "❌ No server state found")
        (System/exit 1)))))

(defn restart-server []
  (println "Restarting server...")
  (try
    (stop-server)
    (Thread/sleep 1000)
    (start-server)
    (catch Exception e
      (println "Restart failed, starting fresh...")
      (delete-server-state)
      (start-server))))

(defn -main [& args]
  (case (first args)
    "start" (start-server)
    "stop" (stop-server)
    "status" (server-status)
    "restart" (restart-server)
    (do
      (println "Usage: bb run bb-dev-server {start|stop|status|restart}")
      (System/exit 1))))