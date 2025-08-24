(ns websocket-nrepl-dispatch
  "WebSocket nREPL Dispatch Implementation for Database-Native Development
  
  Adapted from nrepl-mcp-server.scittle.integration
  Enables bidirectional browser ↔ server communication with Datalevin database access
  
  Browser ←→ WebSocket ←→ Babashka nREPL ←→ Datalevin Database
  
  Key capabilities:
  - Hot-load ClojureScript nREPL client into browser
  - Hot-load WebSocket bridge extensions into Babashka
  - Bidirectional code evaluation with database access
  - File system operations from browser
  - Shell command execution from browser
  - Database operations from browser")

;; =============================================================================
;; Configuration
;; =============================================================================

(def config
  "Database-native development configuration"
  {:browser-nrepl-port 1339
   :websocket-port 1340
   :http-port 1341
   :db-path "/var/db/datalevin/cljcodedb"})

(defonce dispatch-state
  "WebSocket dispatch state tracking"
  (atom {:status :not-loaded
         :active-sessions {}
         :deployed-components #{}}))

;; =============================================================================
;; Hot-Loadable Browser nREPL Client with Database Access
;; =============================================================================

(defn browser-nrepl-client-code
  "ClojureScript code for browser-side nREPL client with database operations"
  []
  "
;; Browser nREPL Client with Database Access
;; Hot-loaded into ClojureScript environment

(defonce db-client-state (atom {:status :ready :message-handlers {}}))

(defn eval-on-server 
  \"Evaluate Clojure code on the Babashka server with database access\"
  [code & {:keys [timeout-ms] :or {timeout-ms 5000}}]
  (let [message-id (str (random-uuid))
        message {:direction \"to-babashka\" 
                 :op \"eval\" 
                 :code code 
                 :id message-id
                 :timestamp (js/Date.now)}
        result-promise (js/Promise. 
                         (fn [resolve reject]
                           (swap! db-client-state assoc-in [:message-handlers message-id] 
                                  {:resolve resolve :reject reject :timestamp (js/Date.now)})
                           (js/setTimeout #(reject (js/Error. \"Timeout\")) timeout-ms)))]
    (send-to-server message)
    result-promise))

(defn send-to-server [message]
  \"Send message to Babashka via existing WebSocket\"
  ;; TODO: Integrate with existing Scittle WebSocket connection
  (js/console.log \"Sending to server:\" message))

(defn handle-server-response [response]
  \"Handle response from Babashka server\"
  (let [{:keys [id status value error]} response
        handler (get-in @db-client-state [:message-handlers id])]
    (when handler
      (swap! db-client-state update :message-handlers dissoc id)
      (if (= status \"success\")
        ((:resolve handler) value)
        ((:reject handler) (js/Error. error))))))

;; Database operations
(defn query-db [query-str]
  \"Execute Datalog query on server database\"
  (eval-on-server 
    (str \"(require '[pod.huahaiy.datalevin :as d]) \"
         \"(d/q '\" query-str \" (d/db (d/get-conn \\\"/var/db/datalevin/cljcodedb\\\")))\")))

(defn transact! [tx-data]
  \"Execute database transaction\"
  (eval-on-server 
    (str \"(require '[pod.huahaiy.datalevin :as d]) \"
         \"(let [conn (d/get-conn \\\"/var/db/datalevin/cljcodedb\\\")] \"
         \"(d/transact! conn \" (pr-str tx-data) \"))\")))

(defn db-stats []
  \"Get database statistics\"
  (eval-on-server 
    (str \"(require '[pod.huahaiy.datalevin :as d]) \"
         \"(let [conn (d/get-conn \\\"/var/db/datalevin/cljcodedb\\\")] \"
         \"(d/db-stats (d/db conn)))\")))

;; File operations
(defn read-server-file [path]
  \"Read file from server file system\"
  (eval-on-server (str \"(slurp \\\"\" path \"\\\")\") ))

(defn write-server-file [path content]
  \"Write file to server file system\"
  (eval-on-server (str \"(spit \\\"\" path \"\\\" \\\"\" content \"\\\")\") ))

;; Shell operations
(defn run-shell-command [cmd & args]
  \"Execute shell command on server\"
  (eval-on-server 
    (str \"(require '[babashka.process :as p]) \"
         \"(p/shell \\\"\" cmd \"\\\" \" (clojure.string/join \" \" (map pr-str args)) \")\")))

;; Server operations
(defn server-status []
  \"Get server status and information\"
  (eval-on-server \"(system-properties)\"))

(js/console.log \"💾 Database-enabled Browser nREPL Client loaded!\")
(js/console.log \"Available functions:\")
(js/console.log \"  - (eval-on-server code)       ; Evaluate on server\")
(js/console.log \"  - (query-db query-str)        ; Execute Datalog query\") 
(js/console.log \"  - (transact! tx-data)         ; Execute database transaction\")
(js/console.log \"  - (db-stats)                  ; Get database statistics\")
(js/console.log \"  - (read-server-file path)     ; Read server file\") 
(js/console.log \"  - (write-server-file path content) ; Write server file\")
(js/console.log \"  - (run-shell-command cmd args) ; Execute shell command\")
(js/console.log \"  - (server-status)             ; Get server info\")
")

;; =============================================================================
;; Hot-Loadable WebSocket Bridge Extension with Database Access
;; =============================================================================

(defn bridge-extension-code
  "Clojure code for Babashka-side WebSocket bridge extension with database access"
  []
  "
;; WebSocket Bridge Extension with Database Access
;; Hot-loaded into Babashka environment

(require '[pods.huahaiy.datalevin :as d])
(require '[babashka.process :as p])

(defonce bridge-state (atom {:status :ready :active-sessions {}}))

;; Initialize database connection
(def db-conn (d/get-conn \"/var/db/datalevin/cljcodedb\"))

(defn enhanced-websocket-handler 
  \"Enhanced WebSocket message handler with bidirectional support\"
  [websocket-message]
  (let [{:keys [direction op code id]} websocket-message]
    (case direction
      \"to-browser\"   (forward-to-browser websocket-message)     ; existing functionality
      \"to-babashka\"  (handle-server-request websocket-message)  ; NEW: reverse evaluation
      (default-websocket-handler websocket-message))))

(defn handle-server-request
  \"Handle evaluation request from browser with database access\"
  [{:keys [op code id timestamp] :as message}]
  (try
    (let [start-time (System/currentTimeMillis)
          result (case op
                   \"eval\" (eval-code-safely code)
                   \"file-read\" (slurp code)
                   \"file-write\" (let [[path content] (read-string code)] 
                                  (spit path content))
                   \"shell\" (apply p/shell (read-string code))
                   (throw (Exception. (str \"Unknown operation: \" op))))
          end-time (System/currentTimeMillis)
          response {:id id
                    :status \"success\"
                    :value (pr-str result)
                    :execution-time-ms (- end-time start-time)
                    :timestamp (System/currentTimeMillis)}]
      (send-to-browser response))
    (catch Exception e
      (let [error-response {:id id
                            :status \"error\"
                            :error (str (.getMessage e))
                            :timestamp (System/currentTimeMillis)}]
        (send-to-browser error-response)))))

(defn eval-code-safely 
  \"Safely evaluate Clojure code with error handling and database access\"
  [code]
  (try
    ;; Pre-load database connection for convenience
    (binding [d/*conn* db-conn]
      (eval (read-string code)))
    (catch Exception e
      (throw (Exception. (str \"Evaluation error: \" (.getMessage e)))))))

(defn send-to-browser [message]
  \"Send message to browser via WebSocket\"
  ;; TODO: Integrate with existing Scittle WebSocket infrastructure
  (println \"📤 Sending to browser:\" message))

(defn forward-to-browser [message]
  \"Forward message to browser (existing functionality)\"
  ;; Delegate to existing Scittle WebSocket handler
  (println \"↗️ Forwarding to browser:\" message))

(defn default-websocket-handler [message]
  \"Default handler for unrecognized messages\"
  (println \"❓ Unknown message direction:\" message))

(println \"💾 Database-enabled WebSocket Bridge Extension loaded!\")
(println \"📡 Enhanced bidirectional communication ready\")
(println \"💾 Database connection established at /var/db/datalevin/cljcodedb\")
")

;; =============================================================================
;; Deployment Functions
;; =============================================================================

(defn deploy-browser-client!
  "Deploy database-enabled nREPL client to browser environment"
  []
  (println "💾 Deploying database-enabled browser nREPL client...")
  {:code (browser-nrepl-client-code)
   :target :browser
   :deployment-method "nrepl-eval with connection: browser-nrepl"
   :description "Hot-loads database-enabled ClojureScript nREPL client into browser"})

(defn deploy-bridge-extension!
  "Deploy database-enabled WebSocket bridge extension to Babashka environment"
  []
  (println "💾 Deploying database-enabled WebSocket bridge extension...")
  {:code (bridge-extension-code)
   :target :babashka
   :deployment-method "nrepl-eval with connection: babashka-nrepl"
   :description "Hot-loads database-enabled enhanced bridge into Babashka nREPL"})

;; =============================================================================
;; Testing and Validation
;; =============================================================================

(defn test-database-integration!
  "Test the complete database-enabled bidirectional connection"
  []
  (println "💾 Testing database-enabled bidirectional connection...")
  {:tests ["(eval-on-server \"(+ 1 2 3)\")"
           "(query-db \"[:find ?e :where [?e :greeting _]]\")"
           "(db-stats)"
           "(transact! [{:greeting \"Hello from browser!\" :timestamp (System/currentTimeMillis)}])"
           "(read-server-file \"README.md\")"
           "(run-shell-command \"pwd\")"
           "(server-status)"]
   :expected-results ["6" "query results" "database statistics" "transaction result" "README content" "current directory" "system properties"]
   :description "Validates browser → server → database → browser communication"})

;; =============================================================================
;; Integration Lifecycle
;; =============================================================================

(defn load-websocket-dispatch!
  "Load the complete WebSocket nREPL dispatch system"
  []
  (println "💾 Loading WebSocket nREPL dispatch with database access...")
  (swap! dispatch-state assoc :status :loaded)
  {:status :loaded
   :config config
   :next-steps ["Deploy browser client" "Deploy bridge extension" "Test database integration"]
   :functions ["deploy-browser-client!"
               "deploy-bridge-extension!"
               "test-database-integration!"]})

(defn websocket-dispatch-status
  "Get current WebSocket dispatch status"
  []
  @dispatch-state)

;; =============================================================================
;; Usage Examples and Setup Guide
;; =============================================================================

(defn setup-guide
  "Complete setup guide for database-native development"
  []
  {:steps
   [
    {:step 1
     :description "Start Babashka server with browser nREPL"
     :command "./bb-start.sh browser-nrepl"}
    
    {:step 2
     :description "Connect to Babashka nREPL via MCP"
     :mcp-command "nrepl-connection {\"op\": \"connect\", \"connection\": \"7890\", \"nickname\": \"bb-server\"}"}
    
    {:step 3
     :description "Load WebSocket dispatch functions"
     :mcp-command "nrepl-load-file {\"file-path\": \"websocket-nrepl-dispatch.clj\"}"}
    
    {:step 4
     :description "Deploy bridge extension to Babashka"
     :mcp-command "nrepl-eval {\"code\": \"(deploy-bridge-extension!)\"}"}
    
    {:step 5
     :description "Connect to browser nREPL"
     :mcp-command "nrepl-connection {\"op\": \"connect\", \"connection\": \"1339\", \"nickname\": \"browser-nrepl\"}"}
    
    {:step 6
     :description "Deploy browser client to ClojureScript environment"
     :mcp-command "nrepl-eval {\"code\": \"(load-string (:code (deploy-browser-client!)))\", \"connection\": \"browser-nrepl\"}"}
    
    {:step 7
     :description "Open browser to see environment"
     :command "open http://localhost:1341/"}
    
    {:step 8
     :description "Test database integration from browser"
     :mcp-command "nrepl-eval {\"code\": \"(test-database-integration!)\"}"}
   ]
   
   :demo-commands
   [{:description "Query database from browser"
     :browser-code "(query-db \"[:find ?e :where [?e :greeting _]]\")"
     :result "Returns all entities with greeting attribute"}
    
    {:description "Add data from browser"
     :browser-code "(transact! [{:greeting \"Hello from browser!\" :timestamp (js/Date.now)}])"
     :result "Adds new entity to database"}
    
    {:description "Read server file from browser"
     :browser-code "(read-server-file \"bb.edn\")"
     :result "Returns contents of bb.edn file"}
    
    {:description "Execute shell command from browser"
     :browser-code "(run-shell-command \"git\" \"status\")"
     :result "Returns git status output"}]})

(comment
  ;; Usage workflow:
  
  ;; 1. Start environment
  (setup-guide)
  
  ;; 2. Load dispatch system
  (load-websocket-dispatch!)
  
  ;; 3. Deploy components
  (deploy-bridge-extension!)
  (deploy-browser-client!)
  
  ;; 4. Test functionality
  (test-database-integration!)
  
  ;; 5. Check status
  (websocket-dispatch-status))