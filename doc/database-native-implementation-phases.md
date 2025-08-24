# Database-Native Development: Implementation Phases

## End Goal Architecture (Your Vision)

```
┌─────────────────┐    WebSocket     ┌──────────────────┐    Pod API    ┌─────────────┐
│   Browser       │◄────────────────►│  Babashka        │◄─────────────►│  Datalevin  │
│   Scittle+nREPL │  ↕ Bidirectional │  + nREPL servers │               │  Database   │
└─────────────────┘                  └──────────────────┘               └─────────────┘
│                                    │                                  │
├── Live code eval                   ├── bb-nrepl (MCP bridge)          ├── Top-level forms
├── Real-time updates               ├── websocket-nrepl (browser)      ├── FQN indexing
├── Hot reloading                   ├── Code extraction from db        ├── Rich metadata
└── Interactive dev                 └── Runtime integration            └── Dependencies
```

**Key Components**:
- **Datalevin DB**: Code stored as top-level forms with FQNs and rich metadata
- **Single bootstrap.clj**: Minimal file-based startup, rest from db
- **bb.edn**: Package dependencies
- **Dual nREPL endpoints**: bb-nrepl (MCP) + websocket-nrepl (browser) 
- **Live evaluation**: Code extracted from db and eval'd in both runtimes
- **Bidirectional WebSocket**: ✅ Already implemented and tested
  - `sci.nrepl.browser-server` provides WebSocket bridge
  - Browser Scittle connects via WebSocket to bb nREPL
  - Real-time code evaluation and updates working
  - Session persistence across browser refreshes

## Phased Migration Strategy

### Phase 1: File Content in Database (Simplest Migration)

**Goal**: Direct file-to-database storage without changing structure

```clojure
;; Database schema
{:file/path      {:db/cardinality :db.cardinality/one
                  :db/unique :db.unique/identity}
 :file/content   {:db/cardinality :db.cardinality/one}
 :file/modified  {:db/cardinality :db.cardinality/one}
 :file/checksum  {:db/cardinality :db.cardinality/one}}

;; Migration process
(defn migrate-files-to-db [file-paths]
  (doseq [path file-paths]
    (let [content (slurp path)
          checksum (hash content)]
      (d/transact! conn 
        [{:file/path path
          :file/content content
          :file/modified (fs/last-modified-time path)
          :file/checksum checksum}]))))

;; Code loading
(defn load-file-from-db [path]
  (let [file-data (d/pull (d/db conn) 
                          [:file/content] 
                          [:file/path path])]
    (eval (read-string (:file/content file-data)))))
```

**Benefits**:
- ✅ Minimal disruption to existing code
- ✅ Files work exactly as before
- ✅ Version control through db transactions
- ✅ Easy rollback to file system

**Limitations**:
- ❌ Still monolithic file approach
- ❌ No granular form-level operations
- ❌ Limited metadata capabilities

### Phase 2: Top-Level Forms with FQNs (Gradual Decomposition)

**Goal**: Parse files into individual top-level forms with proper indexing

```clojure
;; Enhanced schema
{:form/fqn       {:db/cardinality :db.cardinality/one
                  :db/unique :db.unique/identity}
 :form/source    {:db/cardinality :db.cardinality/one}
 :form/type      {:db/cardinality :db.cardinality/one}    ; :def, :defn, :defmacro
 :form/namespace {:db/cardinality :db.cardinality/one}
 :form/name      {:db/cardinality :db.cardinality/one}
 :form/line      {:db/cardinality :db.cardinality/one}
 :form/file      {:db/cardinality :db.cardinality/one     ; Reference to original file
                  :db/valueType :db.type/ref}}

;; Migration from Phase 1
(defn decompose-file-to-forms [file-entity]
  (let [content (:file/content file-entity)
        path (:file/path file-entity)
        forms (parse-top-level-forms content path)]
    (d/transact! conn 
      (mapv (fn [form-data]
              {:form/fqn (str (:namespace form-data) "/" (:name form-data))
               :form/source (:source form-data)
               :form/type (:type form-data)
               :form/namespace (:namespace form-data)
               :form/name (:name form-data)
               :form/line (:line form-data)
               :form/file (:db/id file-entity)})
            forms))))

;; Smart form loading
(defn load-namespace-from-db [ns-name]
  (let [forms (d/q '[:find ?fqn ?source
                     :in $ ?ns
                     :where [?e :form/namespace ?ns]
                            [?e :form/fqn ?fqn]
                            [?e :form/source ?source]
                     :order-by ?fqn]
                   (d/db conn) ns-name)]
    (doseq [[fqn source] forms]
      (eval (read-string source)))))
```

**Benefits**:
- ✅ Granular form-level operations
- ✅ FQN-based indexing and lookup
- ✅ Precise dependency tracking
- ✅ Fine-grained hot reloading

### Phase 3: Rich Metadata and Dependencies (Full Database-Native)

**Goal**: Complete semantic understanding with dependency graphs and rich metadata

```clojure
;; Full semantic schema
{:form/fqn          {:db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity}
 :form/source       {:db/cardinality :db.cardinality/one}
 :form/dependencies {:db/cardinality :db.cardinality/many  ; Other forms this depends on
                     :db/valueType :db.type/ref}
 :form/dependents   {:db/cardinality :db.cardinality/many  ; Forms that depend on this
                     :db/valueType :db.type/ref}
 :form/docstring    {:db/cardinality :db.cardinality/one}
 :form/parameters   {:db/cardinality :db.cardinality/many} ; For functions
 :form/return-type  {:db/cardinality :db.cardinality/one}
 :form/examples     {:db/cardinality :db.cardinality/many}
 :form/tests        {:db/cardinality :db.cardinality/many
                     :db/valueType :db.type/ref}
 :form/tags         {:db/cardinality :db.cardinality/many} ; :public, :private, :deprecated
 :form/runtime      {:db/cardinality :db.cardinality/one}  ; :clj, :cljs, :cljc
 :form/complexity   {:db/cardinality :db.cardinality/one}  ; Computed complexity score
 :form/usage-count  {:db/cardinality :db.cardinality/one}} ; How often referenced

;; Advanced dependency analysis
(defn analyze-form-dependencies [form-source]
  (let [parsed (clojure.tools.analyzer/analyze form-source)]
    {:dependencies (extract-dependencies parsed)
     :complexity (calculate-complexity parsed)
     :parameters (extract-parameters parsed)
     :return-type (infer-return-type parsed)}))

;; Smart loading with dependency resolution
(defn load-forms-with-dependencies [fqns]
  (let [dependency-order (resolve-dependency-order fqns)]
    (doseq [fqn dependency-order]
      (load-single-form fqn))))
```

**Benefits**:
- ✅ Complete semantic understanding
- ✅ Automatic dependency resolution
- ✅ Rich IDE-like capabilities
- ✅ Advanced refactoring support
- ✅ Intelligent code generation

## Bootstrap Implementation Sequence

### Stage 1: Infrastructure Setup
```clojure
;; bootstrap.clj - Single file to rule them all
(require '[pod.huahaiy.datalevin :as d])
(require '[sci.nrepl.browser-server :as nrepl])

;; 1. Connect to database
(def conn (d/get-conn "/var/db/datalevin/cljcodedb" schema))

;; 2. Load bb.edn deps from db (or start with file-based)
(load-dependencies!)

;; 3. Start dual nREPL servers
(nrepl/start! {:nrepl-port 1667        ; For MCP bridge
               :websocket-port 1340})   ; For browser

;; 4. Load Phase 1/2/3 runtime based on migration state
(bootstrap-runtime-from-db!)
```

### Stage 2: Migration Control
```clojure
;; Migration state tracking
{:migration/phase    {:db/cardinality :db.cardinality/one}
 :migration/files    {:db/cardinality :db.cardinality/many}
 :migration/progress {:db/cardinality :db.cardinality/one}}

;; Gradual migration commands
(migrate-files-to-db! ["src/core.clj" "src/util.clj"])     ; Phase 1
(decompose-files-to-forms! [:file/path "src/core.clj"])    ; Phase 2  
(enrich-forms-metadata! ["core/add" "core/remove"])        ; Phase 3
```

### Stage 3: Live Development Experience
```clojure
;; Hot reload on database changes
(d/listen! conn :form-changes
  (fn [tx-data]
    (let [changed-forms (extract-changed-forms tx-data)]
      ;; Reload in bb runtime
      (reload-forms-in-runtime! changed-forms :bb)
      ;; Push to browser via WebSocket
      (push-forms-to-browser! changed-forms))))

;; Browser receives and evals updated forms
;; Already implemented WebSocket bidirectional communication
```

## Implementation Priority

1. **Phase 1 File Storage** (1-2 weeks)
   - Minimal disruption, immediate db benefits
   - File backup and version control
   - Foundation for further phases

2. **Phase 2 Form Decomposition** (2-4 weeks) 
   - FQN indexing and lookup
   - Granular hot reloading
   - Preparation for rich metadata

3. **Phase 3 Rich Metadata** (4-8 weeks)
   - Dependency analysis
   - Advanced IDE features
   - Complete database-native experience

**Key Success Metric**: At each phase, the system should work better than the previous phase while maintaining full backwards compatibility.

This phased approach lets you incrementally build toward your end goal architecture while delivering value at each step and minimizing risk.

## AI Integration: Dual Runtime Access

### Revolutionary Development Environment

Once the system is fully operational, AI assistants (like Claude Code) will have **unprecedented access** to the entire development environment through **dual nREPL connections**:

```
┌─────────────────┐    nREPL-1     ┌──────────────────┐    Pod API    ┌─────────────┐
│   AI Assistant  │◄──────────────►│  bb-runtime      │◄─────────────►│  Datalevin  │
│   (Claude Code) │                │  (Server-side)   │               │  Database   │
│                 │    nREPL-2     │                  │               │             │
│                 │◄──────────────►│  browser-scittle │               │             │
│                 │                │  (Client-side)   │               │             │
└─────────────────┘                └──────────────────┘               └─────────────┘
```

### Dual Runtime Capabilities

**bb-runtime nREPL Connection**:
```clojure
;; AI can directly access database
(d/q '[:find ?fqn ?source 
       :where [?e :form/fqn ?fqn]
              [?e :form/source ?source]] 
     (d/db conn))

;; Introspect and modify server-side code
(load-namespace-from-db 'core.analytics)
(hot-reload-form! "core.analytics/process-events")

;; System operations
(analyze-database-schema)
(optimize-query-performance)
```

**browser-scittle-runtime nREPL Connection**:
```clojure
;; AI can directly manipulate DOM
(-> js/document
    (.getElementById "user-input")
    (.-value))

;; Inspect browser state
(get-local-storage-data)
(analyze-component-tree)

;; Test UI interactions
(simulate-user-click "#submit-button")
(validate-form-behavior)
```

### Unified AI Development Experience

**Cross-Runtime Coordination**:
```clojure
;; AI orchestrates operations across both runtimes

;; 1. Analyze data on server
(def analysis-result 
  (bb-eval '(analyze-user-patterns (get-recent-events))))

;; 2. Update UI based on analysis  
(browser-eval `(update-dashboard ~analysis-result))

;; 3. Store results back to database
(bb-eval '(save-analysis-results analysis-result))
```

**Live Development Assistance**:
- **Code Generation**: Write functions in appropriate runtime context
- **Debugging**: Inspect state in both server and browser simultaneously  
- **Testing**: Execute comprehensive full-stack tests
- **Refactoring**: Coordinate changes across runtime boundaries
- **Performance Analysis**: Profile and optimize both server and client code

### Capabilities Unlocked

1. **Complete System Introspection**
   - Database schema and content analysis
   - Runtime state examination (both server and client)
   - Code dependency mapping and analysis
   - Performance bottleneck identification

2. **Interactive Development**
   - Real-time code generation and testing
   - Live debugging with immediate feedback
   - Hot-reloading with instant validation
   - Dynamic system reconfiguration

3. **Full-Stack Coordination**
   - Seamless data flow orchestration
   - Cross-runtime error handling
   - Unified testing strategies
   - End-to-end feature development

4. **Database-Native Operations**
   - Direct form manipulation in dl-db
   - Dependency graph analysis and updates
   - Metadata enrichment and validation
   - Schema evolution management

### Security and Sandboxing

**Production Considerations**:
- **Controlled Environments**: Limit AI access to development/staging
- **Permission Scoping**: Restrict operations based on context
- **Audit Logging**: Track all AI-initiated operations
- **Rollback Capabilities**: Quick recovery from unintended changes

This dual-runtime access transforms AI from a code generation tool into a **full-stack development partner** with complete system awareness and control.

## Feature Requirement: Seamless Page Reload with State Persistence

### **Revolutionary UX Feature**

Transform page reloads from disruptive interruptions into invisible state transitions for browser dependency loading:

**User Experience Flow:**
1. **AI Analysis**: "I need moment.js for this date formatting"
2. **Seamless Transition**: "Loading moment.js..." (brief reload)
3. **Perfect Restoration**: Continue exactly where you left off
4. **No Context Loss**: Cursor position, panels, history all preserved

### **Feature Requirements**

**Core Capability:**
```clojure
;; When AI detects need for new browser library
(defn seamless-library-addition [lib-name lib-url]
  "Transform library loading from disruption to seamless upgrade"
  
  ;; 1. Capture complete development state
  (capture-session-state!)
  
  ;; 2. Queue library for next page load
  (queue-library-for-reload! lib-name lib-url)
  
  ;; 3. Trigger smart reload
  (initiate-seamless-reload!)
  
  ;; 4. Auto-restore on page load
  (on-page-load restore-complete-session!))
```

**State Persistence Schema:**
```clojure
{:session/id               ; Unique session identifier
 :session/ui-state         ; Complete UI component state
 :session/cursor-position  ; Exact cursor/caret position
 :session/open-panels      ; Which panels/tabs are open
 :session/current-form     ; Active form being edited
 :session/evaluation-history ; REPL command history
 :session/scroll-position  ; Viewport scroll state
 :session/selected-text    ; Current text selection
 :session/undo-stack       ; Edit operation history
 :session/timestamp        ; When state was captured
 
 :page/pending-libraries   ; Libraries to load on reload
 :page/load-order          ; Dependency loading sequence}
```

**Implementation Requirements:**

1. **Pre-Reload State Capture**:
   - DOM state serialization
   - Component state extraction  
   - Editor state preservation
   - Navigation state tracking

2. **Smart Page Reload**:
   - Generate HTML with new script tags
   - Preserve URL and routing state
   - Maintain browser history
   - Fast reload optimization

3. **Post-Reload State Restoration**:
   - Reconstruct exact UI state
   - Restore cursor and selection
   - Rebuild component hierarchy
   - Reconnect WebSocket nREPL

4. **Seamless Transition UX**:
   - Loading indicator during reload
   - Progress feedback to user
   - Error handling and fallback
   - Performance optimization

### **Benefits for Database-Native Development**

**🎯 Perfect Architecture Alignment**:
- State lives in database, not browser memory
- Page reload becomes stateless operation  
- Complete development context preserved
- AI can enhance environment transparently

**✨ Revolutionary User Experience**:
- Library loading feels like feature activation
- No interruption to development flow
- Complete context preservation
- Transparent system enhancement

**🛡️ Reliable and Robust**:
- Standard browser loading mechanisms
- No complex dynamic injection failures
- Predictable dependency resolution
- Clean error handling

### **Implementation Priority**

**Phase 1: Basic State Persistence** (1-2 weeks)
- Capture and restore basic UI state
- Simple page reload mechanism
- Core session management

**Phase 2: Enhanced State Capture** (2-3 weeks)  
- Complete editor state preservation
- Cursor position and selection tracking
- Undo/redo stack maintenance

**Phase 3: Seamless UX** (1-2 weeks)
- Loading animations and feedback
- Performance optimization
- Error handling and recovery

**Phase 4: AI Integration** (1 week)
- Automatic library detection
- Transparent dependency resolution
- Smart reload triggering

This feature transforms the Scittle dynamic loading limitation from a constraint into an opportunity for superior user experience through database-native state management.

## Human Developer Terminal Access Requirements

### **Dual nREPL Terminal Access for Human Developers**

To support human developers working alongside AI agents in the database-native environment, we need terminal access to both runtime environments.

### **Browser-Side REPL Access**

**Option 1: Custom nREPL Popup Terminal**
```clojure
;; Browser-embedded nREPL terminal component
(defn nrepl-popup-terminal []
  "Custom popup terminal for ClojureScript nREPL evaluation"
  [:<>
   [:div.terminal-popup
    [:div.terminal-header "ClojureScript nREPL"]
    [:div.terminal-body
     [:textarea.repl-input {:placeholder "=> (+ 1 2 3)"}]
     [:div.repl-output]]
    [:div.terminal-controls
     [:button "Eval"] [:button "Clear"] [:button "History"]]]])

;; WebSocket nREPL integration
(defn eval-in-browser-repl [code]
  (nrepl-eval-via-websocket code :target :browser))
```

**Option 2: Browser Console Integration (EXISTING)**
- **Chrome/Firefox DevTools Console**: Native browser console with Scittle integration
- **Dirac DevTools**: Enhanced ClojureScript developer tools with proper REPL
- **Standard Console Access**: `js/scittle.core.eval_string` from browser console

```javascript
// Direct evaluation in browser console
scittle.core.eval_string("(+ 1 2 3)");

// Access current Scittle context
scittle.core.eval_string("*ns*");

// Switch between ClojureScript and JavaScript
console.log("JavaScript context");
scittle.core.eval_string("(println \"ClojureScript context\")");
```

**Option 3: Dirac DevTools (RECOMMENDED for ClojureScript)**
```html
<!-- Enhanced ClojureScript debugging with Dirac -->
<script src="https://cdn.jsdelivr.net/npm/dirac-devtools@1.6.8/dirac.js"></script>
<script>
  // Provides ClojureScript-aware REPL in DevTools
  dirac.runtime.install();
</script>
```

### **Server-Side bb-nREPL Terminal Access**

**Option 1: Direct Terminal nREPL Client**
```bash
# Standard nREPL clients for Babashka
rlwrap nc localhost 1339                    # Basic netcat REPL
rlwrap telnet localhost 1339                # Telnet-based REPL

# Enhanced nREPL clients
bb --nrepl-server 1339 --repl              # Built-in BB REPL client
clojure -M:repl:connect localhost:1339     # Clojure CLI client
```

**Option 2: Editor Integration**
```clojure
;; CIDER (Emacs) - Connect to bb-nREPL
;; M-x cider-connect-clj
;; Host: localhost, Port: 1339

;; Calva (VS Code) - Connect to bb-nREPL  
;; Ctrl+Shift+P "Calva: Connect to a Running REPL"
;; Select "Babashka" → localhost:1339

;; Cursive (IntelliJ) - Remote nREPL connection
;; Run → Edit Configurations → Remote → nREPL
;; Host: localhost, Port: 1339
```

**Option 3: Web-based Terminal (Custom)**
```clojure
;; HTTP terminal endpoint for web access
(defn web-terminal-handler [request]
  (case (:uri request)
    "/terminal" (terminal-page-response)
    "/eval" (eval-and-respond (:body request))
    "/history" (repl-history-response)))

;; WebSocket terminal for real-time interaction
(defn websocket-terminal-handler [ws-channel]
  (on-receive ws-channel 
    (fn [message]
      (let [result (nrepl-eval message)]
        (send! ws-channel result)))))
```

### **Cross-Runtime Terminal Switching**

**Unified Terminal Interface**:
```clojure
;; Terminal commands for runtime switching
:bb                     ; Switch to Babashka server context
:browser               ; Switch to browser Scittle context  
:dual (+ 1 2 3)        ; Evaluate in both runtimes
:compare (memory-usage) ; Compare results across runtimes

;; Session management
:sessions              ; List active REPL sessions
:session-switch bb-1   ; Switch to specific session
:session-new browser-2 ; Create new session context
```

**Implementation Pattern**:
```clojure
(defn dual-runtime-eval [code]
  "Evaluate code in both BB and browser contexts"
  (let [bb-result (bb-nrepl-eval code)
        browser-result (browser-nrepl-eval code)]
    {:bb bb-result
     :browser browser-result
     :comparison (compare-results bb-result browser-result)}))
```

### **Human-AI Collaboration Patterns**

**Handoff Scenarios**:
1. **AI Development** → **Human Review** via terminal inspection
2. **Human Debugging** → **AI Analysis** via shared session context
3. **Collaborative Development** with simultaneous AI + human access

**Terminal Access Benefits**:
- 🔍 **Live System Introspection**: Human developers can inspect AI-generated code
- 🐛 **Interactive Debugging**: Step through issues with full REPL access
- 🎛️ **Manual Override**: Direct system control when AI encounters limitations
- 📚 **Learning & Exploration**: Human developers can explore database-native patterns
- 🔄 **Context Sharing**: Switch between AI automation and human control seamlessly

### **Security & Session Management**

**Multi-User Considerations**:
```clojure
;; Session isolation
{:session/id "human-dev-1"
 :session/user "alice"  
 :session/runtime :browser
 :session/permissions #{:read :eval :introspect}}

{:session/id "ai-agent-1"
 :session/user "claude-code"
 :session/runtime :both
 :session/permissions #{:read :eval :write :deploy}}
```

**Access Control**:
- **Read-only terminals** for production system monitoring
- **Full-access terminals** for development environment control  
- **Audit logging** for all human and AI terminal activities
- **Session timeouts** and cleanup for security

This dual terminal access system ensures human developers maintain full visibility and control over the AI-enhanced database-native development environment.