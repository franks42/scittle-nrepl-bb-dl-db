(ns websocket-service
  "WebSocket service for Super Duper BB Server - Browser nREPL Bridge"
  (:require [sci.nrepl.browser-server :as bp]
            [cheshire.core :as json]))

(defonce websocket-state
  (atom {:status :not-started
         :servers {}}))

(defn port-available?
  "Check if port is available"
  [port]
  (try
    (with-open [_ (java.net.ServerSocket. port)]
      true)
    (catch Exception _
      false)))

(defn find-available-port
  "Find an available port starting from preferred port"
  [preferred-port]
  (let [max-port (+ preferred-port 100)]
    (loop [port preferred-port]
      (cond
        (>= port max-port)
        (throw (Exception. (str "No available port found starting from " preferred-port)))

        (port-available? port)
        port

        :else
        (recur (inc port))))))

(defn start-browser-nrepl-server!
  "Start browser nREPL server with WebSocket bridge"
  [& {:keys [nrepl-port websocket-port]
      :or {nrepl-port 1339 websocket-port 1340}}]
  (try
    (let [actual-nrepl-port (find-available-port nrepl-port)
          actual-websocket-port (find-available-port websocket-port)
          server-info (bp/start! {:nrepl-port actual-nrepl-port
                                  :websocket-port actual-websocket-port})]
      (swap! websocket-state assoc
             :status :running
             :servers {:browser-nrepl {:port actual-nrepl-port
                                       :websocket-port actual-websocket-port
                                       :started-at (str (java.time.Instant/now))
                                       :server-info server-info}})

      {:status :started
       :nrepl-port actual-nrepl-port
       :websocket-port actual-websocket-port
       :urls {:nrepl (str "nrepl://localhost:" actual-nrepl-port)
              :websocket (str "ws://localhost:" actual-websocket-port)}
       :message "Browser nREPL server with WebSocket bridge started"})

    (catch Exception e
      {:status :error
       :error (.getMessage e)
       :message "Failed to start browser nREPL server"})))

(defn stop-browser-nrepl-server!
  "Stop browser nREPL server"
  []
  (try
    ;; Note: sci.nrepl.browser-server doesn't provide explicit stop function
    ;; The servers will stop when the process exits
    (swap! websocket-state assoc :status :stopped :servers {})
    {:status :stopped
     :message "Browser nREPL server marked as stopped"}
    (catch Exception e
      {:status :error
       :error (.getMessage e)
       :message "Failed to stop browser nREPL server"})))

(defn websocket-server-status
  "Get WebSocket server status"
  []
  @websocket-state)

(defn get-websocket-ports
  "Get current WebSocket and nREPL ports for discovery API"
  []
  (let [state @websocket-state]
    (if (= (:status state) :running)
      (let [browser-nrepl (get-in state [:servers :browser-nrepl])]
        {:status :running
         :nrepl-port (:port browser-nrepl)
         :websocket-port (:websocket-port browser-nrepl)
         :started-at (:started-at browser-nrepl)})
      {:status (:status state)
       :message "WebSocket server not running"})))

(defn websocket-discovery-response
  "Generate JSON response for WebSocket discovery API"
  []
  (let [ports (get-websocket-ports)]
    (json/generate-string ports {:pretty true})))

(comment
  ;; Test WebSocket service
  (start-browser-nrepl-server!)
  (websocket-server-status)
  (get-websocket-ports)
  (websocket-discovery-response)
  (stop-browser-nrepl-server!))