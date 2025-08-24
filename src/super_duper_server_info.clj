(ns super-duper-server-info
  "Server information utilities for nREPL discovery"
  (:require [babashka.fs :as fs]))

(def state-file ".bb-super-duper-server")

(defn read-state []
  (when (fs/exists? state-file)
    (try
      (read-string (slurp state-file))
      (catch Exception _ nil))))

(defn get-server-info
  "Returns comprehensive server information for nREPL discovery"
  []
  (if-let [state (read-state)]
    (assoc state :status :running :state-file state-file)
    {:status :not-running :state-file state-file}))