#!/usr/bin/env bb

(require '[babashka.nrepl.server :as nrepl]
         '[babashka.fs :as fs]
         '[babashka.process :as p])

(def state-file ".bb-dev-nrepl-server")

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
    (let [{:keys [pid port]} state]
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
                                 (spit ~state-file (pr-str {:pid pid#
                                                            :hostname "localhost"
                                                            :port port#
                                                            :started-at started-at#}))
                                 (println (str "Started nREPL server at localhost:" port#))
                                 @(promise))))]
                          {:inherit false})]
      (Thread/sleep 500) ; Give it time to start
      (if-let [{:keys [pid port state]} (check-existing-server)]
        (do
          (binding [*out* *err*]
            (println (str "✅ nREPL server started on localhost:" port " (PID: " pid ")"))
            (println "Server state saved to" state-file))
          (println (pr-str (assoc state :status :started :state-file state-file))))
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
        (when-let [started (:started-at state)]
          (println (str "   Started at: " started))))
      (println (pr-str (assoc state :status :running))))
    (do
      (binding [*out* *err*]
        (println "ℹ️  No nREPL server is running"))
      (println (pr-str {:status :not-running})))))

(defn restart-server []
  (when (check-existing-server)
    (stop-server))
  (Thread/sleep 500)
  (start-server))

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