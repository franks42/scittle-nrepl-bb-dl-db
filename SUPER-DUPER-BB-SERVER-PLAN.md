# Super Duper BB Server Implementation Plan

## Executive Summary
Transform the existing `bb-dev-nrepl-server` into a comprehensive development environment that combines:
- Multi-instance nREPL servers (3 total)
- HTTP server for Scittle web interface
- WebSocket bidirectional communication
- Datalevin database integration
- Browser-BB code evaluation bridge

## Current State Analysis

### ✅ Existing Functional Components

#### 1. Core nREPL Server (`src/bb_dev_nrepl_server.clj`)
- **Status**: ✅ Working - Single instance management with lifecycle controls
- **Features**: start/stop/status/restart commands, PID tracking, EDN output
- **Port**: 7890 (current)
- **Usage**: Primary BB introspection and development

#### 2. Scittle Server Functions (`src/scittle-server.clj`) 
- **Status**: ✅ Ready to integrate - Complete server management functions
- **Features**: 
  - `start-scittle-nrepl!` - Browser nREPL server (port 1339)
  - `start-http-server!` - Static file serving (port 1341)
  - `start-scittle-full!` - Combined startup
- **Key Functions**: All server lifecycle management already implemented

#### 3. WebSocket nREPL Dispatch (`src/websocket-nrepl-dispatch.clj`)
- **Status**: ✅ Architecture ready - Complete bidirectional design
- **Features**:
  - Browser → BB code evaluation via WebSocket
  - BB → Browser code execution (Scittle proxy)
  - Database-enabled operations from browser
  - File and shell operations from browser

#### 4. Datalevin Integration (`src/datalevin-working-test.clj`)
- **Status**: ✅ Working - Proven pod loading and operations  
- **Features**: Schema creation, transactions, queries, persistence
- **Database Path**: `/var/db/datalevin/cljcodedb`
- **Pod Version**: `huahaiy/datalevin "0.9.22"`

#### 5. HTML/Browser Interface (`resources/public/nrepl.html`)
- **Status**: ✅ Basic structure ready
- **Features**: Scittle bootstrap, WebSocket port configuration
- **Enhancement Needed**: Latest CDN versions, additional GUI libs

#### 6. nREPL Client Library (`src/bb_nrepl_client/`)
- **Status**: ✅ Working - Simplified 64-line implementation
- **Features**: Eval, load-file, auto-port detection
- **Usage**: Ready for multi-server communication

## 🎯 Implementation Architecture

```
┌─────────────────── SUPER-DUPER-BB-SERVER ───────────────────┐
│                                                             │
│ ┌─ nREPL Server #1 ──────┐  ┌─ HTTP Server ──────────────┐  │
│ │ Port: 7890             │  │ Port: 8080                 │  │
│ │ BB Introspection       │  │ Serve: resources/public/   │  │
│ │ (existing)             │  │ Scittle + GUI libs         │  │
│ └────────────────────────┘  └────────────────────────────┘  │
│                                                             │
│ ┌─ nREPL Server #2 ──────┐  ┌─ WebSocket Endpoint ──────┐  │
│ │ Port: 7891             │  │ Path: /ws                  │  │
│ │ BB→Browser Proxy       │◄─┤ Bidirectional Bridge      │  │
│ │ (eval in Scittle)      │  │ Message Routing            │  │
│ └────────────────────────┘  └────────────────────────────┘  │
│                                                             │
│ ┌─ nREPL Server #3 ──────┐  ┌─ Datalevin Pod ───────────┐  │
│ │ Internal (WebSocket)   │  │ Path: /var/db/datalevin/   │  │
│ │ Browser→BB Requests    │  │ Schema: Extensible         │  │
│ │ (eval in BB)           │  │ Access: All nREPL servers  │  │
│ └────────────────────────┘  └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ WebSocket ws://localhost:8080/ws
                              ▼
                    ┌─ BROWSER ─────────┐
                    │ http://localhost:8080/ │
                    │                   │
                    │ ┌─ Scittle/SCI ─┐ │
                    │ │ nREPL Server  │ │
                    │ │ + nREPL Client│ │
                    │ │ + Datalevin   │ │
                    │ │ + GUI Libs    │ │
                    │ └───────────────┘ │
                    └───────────────────┘
```

## 🔄 Ephemeral Port Discovery Strategy

### Existing Pattern (Perfect!)
The current `bb-dev-nrepl-server` already uses the ideal pattern:

```clojure
;; Server startup with ephemeral port
(nrepl/start-server! {:host "localhost" :port 0})  ; :port 0 = ephemeral
(let [port (.getLocalPort (:socket server))]       ; Discover actual port
  (spit state-file {:port port :pid pid}))         ; Save to state file
```

### Enhanced Multi-Server State Management
```clojure
;; Enhanced state file structure for multiple services
(def enhanced-state-file ".bb-super-duper-server")

(defn save-server-state! [servers]
  (spit enhanced-state-file 
    (pr-str {:services servers
             :started-at (str (java.time.Instant/now))
             :pid (.pid (java.lang.ProcessHandle/current))})))

;; Example state file content:
{:services {:nrepl-main    {:port 54321 :status :running}
            :nrepl-proxy   {:port 54322 :status :running} 
            :http-server   {:port 54323 :status :running}
            :websocket-url "ws://localhost:54323/ws"}
 :started-at "2024-08-24T..."
 :pid 12345}
```

### Browser Discovery Challenge & Solution
**Challenge**: Browser needs to know WebSocket URL before page loads
**Solution**: Dynamic JavaScript injection + discovery endpoint

```clojure
;; Add discovery endpoint to HTTP server
(defn discovery-handler [req]
  (if (= "/api/discovery" (:uri req))
    {:status 200 
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (read-server-state))}
    (static-file-handler req)))
```

```javascript
// Browser auto-discovery pattern
async function discoverWebSocket() {
  const response = await fetch('/api/discovery');
  const config = await response.json();
  return config.services.websocketUrl;
}
```

## 📋 Implementation Steps

### Phase 1: Enhanced Ephemeral Port Foundation
**Estimated Time**: 2-3 hours

#### Step 1.1: Enhance bb_dev_nrepl_server.clj with Multi-Service State
```clojure
;; Replace single service state with multi-service state
(def state-file ".bb-super-duper-server")

(defn update-service-state! [service-name service-info]
  (let [current-state (read-state)
        updated-state (assoc-in current-state [:services service-name] service-info)]
    (spit state-file (pr-str updated-state))))

(defn start-ephemeral-nrepl [service-name]
  (let [server (nrepl/start-server! {:host "localhost" :port 0})
        port (.getLocalPort (:socket server))]
    (update-service-state! service-name {:port port :status :running})
    {:server server :port port}))
```

#### Step 1.2: "Kind-of Stable" HTTP Server with WebSocket Discovery
```clojure
;; Smart HTTP server: preferred port + graceful fallback
(require '[org.httpkit.server :as http])

(def preferred-http-port 37373)  ; High port, unlikely to conflict

(defn start-stable-http-server! []
  "Try preferred port first, fallback to ephemeral if busy"
  (let [handler (fn [req]
                  (case (:uri req)
                    "/ws" (websocket-handler req)
                    "/api/discovery" (discovery-handler req)
                    (static-file-handler req)))]
    (try
      ;; Try preferred port first
      (let [server (http/run-server handler {:port preferred-http-port})]
        (println (str "✅ HTTP Server: localhost:" preferred-http-port " (preferred)"))
        (update-service-state! :http-server {:port preferred-http-port :status :running :type :preferred})
        (update-service-state! :websocket-url (str "ws://localhost:" preferred-http-port "/ws"))
        {:server server :port preferred-http-port :type :preferred})
      (catch java.net.BindException _
        ;; Port busy - fallback to ephemeral
        (println (str "⚠️  Port " preferred-http-port " busy, using ephemeral fallback..."))
        (let [server (http/run-server handler {:port 0})
              port (get-server-port server)]
          (println (str "✅ HTTP Server: localhost:" port " (ephemeral fallback)"))
          (update-service-state! :http-server {:port port :status :running :type :ephemeral})
          (update-service-state! :websocket-url (str "ws://localhost:" port "/ws"))
          {:server server :port port :type :ephemeral})))))

(defn get-server-port [http-server]
  ;; HttpKit server port extraction - implementation depends on HttpKit internals
  (.getLocalPort (:server-socket http-server)))
```

#### Step 1.3: Client Library Auto-Discovery Enhancement
```clojure
;; Enhance bb-nrepl-client to discover multiple services
(defn discover-services []
  (let [state-file ".bb-super-duper-server"]
    (when (fs/exists? state-file)
      (:services (read-string (slurp state-file))))))

(defn get-service-port [service-name]
  (get-in (discover-services) [service-name :port]))

;; Usage examples:
;; (get-service-port :nrepl-main)   ; Current BB server port
;; (get-service-port :nrepl-proxy)  ; Browser proxy port
;; (get-service-port :http-server)  ; Web interface port
```

### Phase 2: Datalevin Integration
**Estimated Time**: 1-2 hours

#### Step 2.1: Embed Datalevin Pod
```clojure
;; Add to server startup
(require '[babashka.pods :as pods])
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

(def db-conn (atom nil))

(defn init-database! []
  (reset! db-conn (d/get-conn "/var/db/datalevin/cljcodedb")))
```

#### Step 2.2: Database Operations API
- Reuse existing patterns from `datalevin-working-test.clj`
- Expose via all nREPL servers
- Add convenient helper functions

### Phase 3: Ephemeral Multi-nREPL Server Setup
**Estimated Time**: 2-3 hours

#### Step 3.1: nREPL Server #2 (BB→Browser Proxy) - Ephemeral
```clojure
(defn start-browser-proxy-nrepl! []
  ;; Ephemeral nREPL server that forwards to browser via WebSocket
  (let [{:keys [server port]} (start-ephemeral-nrepl :nrepl-proxy)]
    (nrepl/set-default-handler! server (proxy-to-browser-handler))
    (update-service-state! :nrepl-proxy 
      {:port port 
       :status :running 
       :description "BB→Browser proxy via WebSocket"})
    {:server server :port port}))

(defn proxy-to-browser-handler [msg]
  ;; Forward nREPL eval to browser Scittle via discovered WebSocket
  (let [ws-url (get-in (discover-services) [:websocket-url])]
    ;; Forward to browser, return results to nREPL client
    ))
```

#### Step 3.2: nREPL Server #3 (Browser→BB Handler)
```clojure
(defn websocket-to-nrepl-handler [websocket-msg]
  ;; Receive from browser WebSocket
  ;; Route to internal nREPL evaluation
  ;; Send results back via WebSocket
)
```

### Phase 4: WebSocket Bridge Implementation  
**Estimated Time**: 3-4 hours

#### Step 4.1: Integrate WebSocket Dispatch
- Use existing `websocket-nrepl-dispatch.clj` architecture
- Implement bidirectional message routing
- Add error handling and reconnection logic

#### Step 4.2: Browser Client Integration
```javascript
// Enhance resources/public/nrepl.html with:
// - Latest Scittle CDN versions
// - WebSocket client code
// - Database operation functions
// - GUI libraries (CodeMirror, etc.)
```

### Phase 5: Testing & Integration
**Estimated Time**: 2-3 hours

#### Step 5.1: Enhanced Test Suite
- Extend existing test files
- Test all 3 nREPL servers
- Test WebSocket bidirectional communication
- Test database operations from browser

#### Step 5.2: Integration Validation
- Complete round-trip: Browser → WebSocket → BB → Database
- Complete round-trip: BB Client → nREPL → WebSocket → Browser
- Performance and stability testing

## 🔧 Implementation Priority

### Critical Path Components
1. **HTTP Server + WebSocket** (needed for browser communication)
2. **Datalevin Integration** (requested to be moved up)
3. **nREPL Server #2** (BB→Browser proxy)
4. **WebSocket Routing** (bidirectional dispatch)
5. **Browser Enhancement** (latest Scittle + GUI libs)

### Reusable Code Catalog

#### ✅ Ready to Integrate (No Changes Needed)
- `src/bb_nrepl_client/` - Complete client library
- `src/datalevin-working-test.clj` - Database operations patterns
- `src/websocket-nrepl-dispatch.clj` - WebSocket architecture
- `resources/public/nrepl.html` - Basic browser template

#### 🔄 Needs Integration (Merge into Main Server)
- `src/scittle-server.clj` - HTTP server + lifecycle functions
- `src/bb_dev_nrepl_server.clj` - Main server (extend this)

#### 🆕 Needs Creation (New Components)
- WebSocket handler in main server
- nREPL Server #2 (proxy handler)
- Enhanced browser HTML with latest CDNs
- Integration test suite

## 🚀 Critical Startup Sequence Plan

### Startup Dependencies & Timing
The ephemeral port bootstrap requires careful orchestration:

```
Phase 1: Core Services (Sequential)
├─ 1. nREPL Main Server (BB introspection)    → Port: 54321
├─ 2. Datalevin Database                       → Ready
├─ 3. Config file creation                     → .bb-super-duper-server
└─ 4. Validate core foundation                 → ✅

Phase 2: Web Infrastructure (Sequential) 
├─ 5. HTTP Server (preferred: 37373)          → Port: 37373 (or fallback)
├─ 6. WebSocket endpoint (/ws)                 → ws://localhost:37373/ws
├─ 7. Update config with web services         → Config updated
└─ 8. Generate dynamic HTML                    → index.html with ports

Phase 3: nREPL Extensions (Parallel)
├─ 9a. nREPL Proxy Server (BB→Browser)        → Port: 54323
└─ 9b. WebSocket message routing               → Internal bridge ready

Phase 4: Validation & Ready (Sequential)
├─ 10. Health checks all services             → All ✅
├─ 11. Final config file update               → Complete state
├─ 12. Print user-friendly status             → Ready message
└─ 13. Optional: Auto-launch browser          → http://localhost:54322/
```

### Startup Implementation Strategy

```clojure
(defn start-super-duper-server! []
  "Orchestrated startup with proper dependency resolution"
  (let [state (atom {:phase :starting :services {}})]
    
    (println "🚀 Starting Super Duper BB Server...")
    (println "   Orchestrating service startup sequence...\n")
    
    ;; PHASE 1: Core Services Foundation
    (println "📡 Phase 1: Core Services")
    (let [{:keys [port]} (start-main-nrepl-server!)]
      (swap! state assoc-in [:services :nrepl-main] {:port port :status :running})
      (println (str "   ✅ nREPL Main:        localhost:" port)))
    
    (init-datalevin-database!)
    (println "   ✅ Datalevin:         Ready")
    
    (save-config-state! @state)
    (println "   ✅ Config file:       .bb-super-duper-server")
    (Thread/sleep 500) ; Let core services settle
    
    ;; PHASE 2: Web Infrastructure  
    (println "\n🌐 Phase 2: Web Infrastructure")
    (let [{:keys [port server]} (start-ephemeral-http-server!)]
      (swap! state assoc-in [:services :http-server] {:port port :status :running})
      (swap! state assoc-in [:services :websocket-url] (str "ws://localhost:" port "/ws"))
      (println (str "   ✅ HTTP Server:       localhost:" port))
      (println (str "   ✅ WebSocket:         ws://localhost:" port "/ws")))
    
    (save-config-state! @state)
    (generate-dynamic-html-with-ports! @state)
    (println "   ✅ Dynamic HTML:      Generated with ports")
    (Thread/sleep 500) ; Let web services settle
    
    ;; PHASE 3: nREPL Extensions (can be parallel)
    (println "\n🔗 Phase 3: nREPL Extensions")
    (let [proxy-future (future (start-browser-proxy-nrepl!))
          bridge-future (future (setup-websocket-bridge!))]
      
      (let [{:keys [port]} @proxy-future]
        (swap! state assoc-in [:services :nrepl-proxy] {:port port :status :running})
        (println (str "   ✅ nREPL Proxy:       localhost:" port)))
      
      @bridge-future
      (println "   ✅ WebSocket Bridge:  Ready"))
    
    ;; PHASE 4: Validation & Ready
    (println "\n✅ Phase 4: Final Validation")
    (validate-all-services! @state)
    (save-config-state! (assoc @state :phase :running :ready-at (str (java.time.Instant/now))))
    
    (print-ready-status! @state)
    @state))

(defn print-ready-status! [state]
  "User-friendly ready message with actionable information"
  (let [http-port (get-in state [:services :http-server :port])
        browser-url (str "http://localhost:" http-port "/")]
    (println "\n🎉 Super Duper BB Server Ready!")
    (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    (println (str "🌐 Browser Interface:  " browser-url))
    (when (= :preferred (get-in state [:services :http-server :type]))
      (println "💡 Pro Tip: This URL is stable - bookmark it!"))
    (println (str "📡 nREPL Main:         localhost:" (get-in state [:services :nrepl-main :port])))
    (println (str "🔗 nREPL Proxy:        localhost:" (get-in state [:services :nrepl-proxy :port])))
    (println "💾 Database:           Ready")
    (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    (println)
    (println "🚀 Quick Start:")
    (println (str "   bb run super-duper-server open    # Launch browser"))
    (println "   bb run super-duper-server status  # Show all info")
    (println)))
```

### Error Handling & Recovery

```clojure
(defn handle-startup-failure! [phase error state]
  "Graceful failure handling with partial cleanup"
  (println (str "\n❌ Startup failed at " phase ": " (.getMessage error)))
  (println "🧹 Cleaning up started services...")
  
  ;; Stop services in reverse order
  (doseq [service (reverse (keys (:services @state)))]
    (try
      (stop-service! service)
      (println (str "   ✅ Stopped " service))
      (catch Exception e
        (println (str "   ⚠️  Failed to stop " service ": " (.getMessage e))))))
  
  (fs/delete-if-exists ".bb-super-duper-server")
  (println "❌ Super Duper BB Server startup failed")
  (System/exit 1))
```

### Restart Strategy

```clojure
(defn restart-super-duper-server! []
  "Safe restart that preserves user sessions when possible"
  (println "🔄 Restarting Super Duper BB Server...")
  
  ;; Graceful shutdown
  (println "🛑 Stopping services gracefully...")
  (stop-super-duper-server!)
  (Thread/sleep 2000) ; Let everything clean up
  
  ;; Fresh startup
  (start-super-duper-server!))
```

### Critical Timing Considerations

#### ⏱️ Service Dependency Chain
```
nREPL Main → Config File → HTTP Server → WebSocket → HTML Generation → nREPL Proxy
     ↓           ↓            ↓             ↓              ↓               ↓
   Required   Discovery   Web Base    WS Endpoint   Dynamic Page    Browser Bridge
```

#### ⏱️ Race Condition Prevention
- **Config File**: Must be written before any service depends on discovery
- **HTTP Server**: Must be ready before WebSocket endpoint registration  
- **WebSocket URL**: Must be known before HTML generation
- **HTML Generation**: Must complete before serving first browser request
- **nREPL Proxy**: Can start in parallel, depends only on WebSocket being ready

#### ⏱️ User Experience Timing
- **Status Updates**: Real-time progress feedback during 15-20 second startup
- **Browser Launch**: Only after ALL services confirmed ready
- **Error Messages**: Immediate failure notification with cleanup
- **Ready Message**: Clear, actionable information when everything works

#### ⏱️ Startup Performance Target
- **Total Time**: < 20 seconds from command to browser ready
- **Core Services**: < 5 seconds (nREPL + database)
- **Web Services**: < 5 seconds (HTTP + WebSocket + HTML)
- **Extensions**: < 5 seconds (proxy + bridge)  
- **Validation**: < 5 seconds (health checks + ready state)

## 🚀 Deployment Plan

### Single Command Startup with Port Discovery
```bash
bb run super-duper-server start
```

This will:
1. Start all 3 nREPL servers (ephemeral ports)
2. Start HTTP server with WebSocket (ephemeral port)  
3. Initialize Datalevin database
4. Save all discovered ports to `.bb-super-duper-server`
5. Print connection info with actual ports

**Example Output:**
```
🚀 Super Duper BB Server Started!

📡 Services Running:
  • nREPL Main (BB):     localhost:54321
  • nREPL Proxy (→Browser): localhost:54322  
  • HTTP Server:         localhost:37373 (preferred)
  • WebSocket:           ws://localhost:37373/ws
  • Database:            /var/db/datalevin/cljcodedb

🌐 Browser Interface: http://localhost:37373/  ← Bookmark this!
📄 Connection Info:    .bb-super-duper-server

✅ All services started successfully!
💡 Pro Tip: http://localhost:37373/ should be stable for bookmarking!
```

### Development Workflow with Auto-Discovery
1. **BB Development**: `bb run nrepl-eval "(get-service-port :nrepl-main)"` → auto-connect
2. **Browser Development**: Open discovered URL, WebSocket auto-connects
3. **Cross-Platform**: All ports discovered automatically, no conflicts
4. **Database Operations**: Query/transact from both environments seamlessly

### Port Discovery Tools
```bash
# Discover all running services  
bb run super-duper-server status

# Connect to specific service
bb run nrepl-eval "(+ 1 2)" --service nrepl-main
bb run nrepl-eval "(+ 1 2)" --service nrepl-proxy  # Evals in browser!

# Open browser to discovered HTTP port
bb run super-duper-server open-browser
```

## 📊 Success Metrics

### Technical Validation  
- ✅ All 3 nREPL servers running on ephemeral ports
- ✅ HTTP server + WebSocket on ephemeral ports
- ✅ Port discovery working for all services
- ✅ WebSocket bidirectional communication working
- ✅ Database accessible from browser and BB
- ✅ File operations from browser working
- ✅ Latest Scittle + GUI libraries loaded
- ✅ No port conflicts, even with multiple instances

### User Experience Validation  
- ✅ Single command startup/shutdown with auto-discovery
- ✅ Seamless code evaluation in both directions  
- ✅ Database operations intuitive from browser
- ✅ Existing bb-nrepl-client workflows unchanged
- ✅ Browser interface responsive and full-featured
- ✅ Zero port configuration required
- ✅ Multiple server instances can coexist

This plan leverages all existing functional components while creating the unified "super duper BB server that slices bread!" 🍞⚡