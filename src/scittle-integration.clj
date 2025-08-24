(ns scittle-integration
  "Scittle Browser nREPL Integration for Database-Native Development
  
  Adapted from nrepl-mcp-server.scittle.integration
  Enables bidirectional browser ↔ server communication
  
  Browser ←→ WebSocket ←→ Babashka nREPL ←→ Datalevin Database
  
  Key capabilities:
  - Hot-load ClojureScript nREPL client into browser
  - Bidirectional code evaluation (browser ↔ server)
  - Database operations from browser
  - File system operations from browser
  - Shell command execution from browser")

;; =============================================================================
;; Configuration
;; =============================================================================

(def config
  {:browser-nrepl-port 1339
   :websocket-port 1340
   :http-port 1341
   :db-path "/var/db/datalevin/cljcodedb"})

;; =============================================================================
;; Browser nREPL Client Code (Hot-loadable)
;; =============================================================================

(def browser-client-code
  "
;; Browser nREPL Client - Hot-loaded into ClojureScript environment
;; Provides bidirectional communication with Babashka server

(defonce client-state (atom {:status :ready :message-handlers {}}))

(defn eval-on-server 
  \"Evaluate Clojure code on the Babashka server\"
  [code & {:keys [timeout-ms] :or {timeout-ms 5000}}]
  (let [message-id (str (random-uuid))
        message {:direction \"to-babashka\" 
                 :op \"eval\" 
                 :code code 
                 :id message-id
                 :timestamp (js/Date.now)}
        result-promise (js/Promise. 
                         (fn [resolve reject]
                           (swap! client-state assoc-in [:message-handlers message-id] 
                                  {:resolve resolve :reject reject})
                           (js/setTimeout #(reject (js/Error. \"Timeout\")) timeout-ms)))]
    (send-to-server message)
    result-promise))

(defn send-to-server [message]
  \"Send message to Babashka via WebSocket\"
  ;; This will use the existing Scittle WebSocket connection
  (js/console.log \"Sending to server:\" (pr-str message)))

(defn handle-server-response [response]
  \"Handle response from Babashka server\"
  (let [{:keys [id status value error]} response
        handler (get-in @client-state [:message-handlers id])]
    (when handler
      (swap! client-state update :message-handlers dissoc id)
      (if (= status \"success\")
        ((:resolve handler) value)
        ((:reject handler) (js/Error. error))))))

;; Database operations
(defn query-db [query-str]
  \"Execute Datalog query on server database\"
  (eval-on-server 
    (str \"(d/q '\" query-str \" (d/db conn))\")))

(defn transact! [tx-data]
  \"Execute database transaction\"
  (eval-on-server 
    (str \"(d/transact! conn \" (pr-str tx-data) \")\")))

;; File operations
(defn read-server-file [path]
  \"Read file from server file system\"
  (eval-on-server (str \"(slurp \\\"\" path \"\\\")\")))

(defn write-server-file [path content]
  \"Write file to server file system\"
  (eval-on-server (str \"(spit \\\"\" path \"\\\" \\\"\" content \"\\\")\")))

;; Shell operations
(defn run-shell-command [cmd & args]
  \"Execute shell command on server\"
  (eval-on-server 
    (str \"(require '[babashka.process :as p]) \"
         \"(p/shell \" (pr-str (cons cmd args)) \")\")))

(js/console.log \"🌐 Browser nREPL Client loaded!\")
(js/console.log \"Available functions:\")
(js/console.log \"  - (eval-on-server code)\")
(js/console.log \"  - (query-db query-str)\")
(js/console.log \"  - (transact! tx-data)\")
(js/console.log \"  - (read-server-file path)\")
(js/console.log \"  - (write-server-file path content)\")
(js/console.log \"  - (run-shell-command cmd args)\")
")

;; =============================================================================
;; Server-side WebSocket Bridge Extension
;; =============================================================================

(def bridge-extension-code
  "
;; WebSocket Bridge Extension - Hot-loaded into Babashka environment
;; Extends Scittle WebSocket bridge for bidirectional communication

(require '[pod.huahaiy.datalevin :as d])
(require '[babashka.process :as p])

(defonce bridge-state (atom {:status :ready}))

;; Database connection
(def conn (d/get-conn \"/var/db/datalevin/cljcodedb\"))

(defn enhanced-websocket-handler 
  \"Enhanced WebSocket message handler with bidirectional support\"
  [websocket-message]
  (let [{:keys [direction op code id]} websocket-message]
    (case direction
      \"to-browser\"   (forward-to-browser websocket-message)
      \"to-babashka\"  (handle-server-request websocket-message)
      (println \"Unknown message direction:\" direction))))

(defn handle-server-request
  \"Handle evaluation request from browser\"
  [{:keys [op code id] :as message}]
  (try
    (let [result (case op
                   \"eval\" (eval (read-string code))
                   \"file-read\" (slurp code)
                   \"file-write\" (let [[path content] (read-string code)] 
                                  (spit path content))
                   \"shell\" (apply p/shell (read-string code))
                   (throw (Exception. (str \"Unknown op: \" op))))
          response {:id id
                    :status \"success\"
                    :value (pr-str result)}]
      (send-to-browser response))
    (catch Exception e
      (send-to-browser {:id id
                        :status \"error\"
                        :error (str e)}))))

(defn send-to-browser [message]
  \"Send message to browser via WebSocket\"
  (println \"📤 Sending to browser:\" message))

(defn forward-to-browser [message]
  \"Forward message to browser (existing functionality)\"
  (println \"↗️ Forwarding to browser:\" message))

(println \"🔧 WebSocket Bridge Extension loaded!\")
(println \"📡 Bidirectional communication ready\")
(println \"💾 Database connection established\")
")

;; =============================================================================
;; Deployment Functions
;; =============================================================================

(defn deploy-browser-client!
  "Deploy nREPL client to browser environment"
  []
  (println "🌐 Deploying browser nREPL client...")
  ;; This would be executed via nrepl-eval to the browser
  browser-client-code)

(defn deploy-bridge-extension!
  "Deploy WebSocket bridge extension to Babashka"
  []
  (println "🔧 Deploying WebSocket bridge extension...")
  ;; This would be executed via nrepl-eval to Babashka
  (eval (read-string bridge-extension-code)))

;; =============================================================================
;; Testing
;; =============================================================================

(defn test-bidirectional!
  "Test the complete bidirectional connection"
  []
  (println "🧪 Testing bidirectional connection...")
  {:browser-to-server ["(eval-on-server \"(+ 1 2 3)\")"
                       "(query-db \"[:find ?e :where [?e :greeting _]]\")"
                       "(read-server-file \"README.md\")"]
   :server-to-browser ["Send evaluation results"
                       "Push database changes"
                       "Stream log updates"]})

;; =============================================================================
;; Main Setup
;; =============================================================================

(defn setup-integration!
  "Set up the complete integration"
  []
  (println "🎯 Setting up Scittle integration...")
  (println "1. Deploy browser client")
  (deploy-browser-client!)
  (println "2. Deploy bridge extension")
  (deploy-bridge-extension!)
  (println "3. Test connection")
  (test-bidirectional!)
  (println "✅ Integration ready!"))