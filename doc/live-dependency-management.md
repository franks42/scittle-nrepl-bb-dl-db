# Live Dependency Management in Dual Runtime System

## Overview

Investigation of dynamic dependency resolution capabilities for both Babashka (server-side) and browser (client-side) environments to enable runtime dependency addition without system restarts.

## Babashka Live Dependency Resolution

### ✅ **Native Support via `babashka.deps/add-deps`**

Babashka provides excellent runtime dependency resolution through the `babashka.deps` namespace:

```clojure
;; Add dependencies at runtime
(require '[babashka.deps :as deps])

;; Add new library dynamically
(deps/add-deps '{:deps {medley/medley {:mvn/version "1.3.0"}}})

;; Now require and use it
(require '[medley.core :as m])
(m/map-vals inc {:a 1 :b 2})  ; => {:a 2, :b 3}
```

### **Key Features**

**Runtime Resolution**:
- Uses tools.deps internally with Java to resolve and download dependencies
- Calculates classpath and adds it to running Babashka runtime
- No restart required - immediate availability

**Multiple Dependency Sources**:
```clojure
;; Maven dependencies
(deps/add-deps '{:deps {environ/environ {:mvn/version "1.2.0"}}})

;; Git dependencies  
(deps/add-deps '{:deps {my-lib/core {:git/url "https://github.com/user/repo"
                                    :git/sha "abc123"}}})

;; Local dependencies
(deps/add-deps '{:deps {local-lib {:local/root "../local-lib"}}})
```

**Classpath Management**:
```clojure
;; Get current classpath
(deps/get-classpath)

;; Add custom classpath entries
(deps/add-classpath "custom-script-dir")
```

### **Integration with Database-Native System**

**Store Dependencies in Datalevin**:
```clojure
;; Schema for dependency management
{:dep/name        {:db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}
 :dep/version     {:db/cardinality :db.cardinality/one}
 :dep/type        {:db/cardinality :db.cardinality/one}  ; :maven, :git, :local
 :dep/config      {:db/cardinality :db.cardinality/one}  ; Full dependency map
 :dep/required-by {:db/cardinality :db.cardinality/many  ; Which forms need this
                   :db/valueType :db.type/ref}}

;; Dynamic dependency loading from database
(defn load-dependencies-for-forms [form-fqns]
  (let [deps (d/q '[:find ?dep-config
                    :in $ [?fqn ...]
                    :where [?form :form/fqn ?fqn]
                           [?form :form/dependencies ?dep]
                           [?dep :dep/config ?dep-config]]
                  (d/db conn) form-fqns)]
    (doseq [dep-config deps]
      (deps/add-deps dep-config))))
```

**AI-Driven Dependency Resolution**:
```clojure
;; AI can analyze code and automatically resolve dependencies
(defn ai-resolve-dependencies [code-form]
  (let [required-libs (analyze-imports code-form)
        missing-deps (filter #(not (available? %)) required-libs)]
    (doseq [dep missing-deps]
      (let [dep-config (ai-suggest-dependency dep)]
        (deps/add-deps {dep-config})
        (store-dependency-in-db! dep dep-config)))))
```

## Browser Dynamic Module Loading

### ✅ **ES Modules Dynamic Import()**

Modern browsers provide excellent support for dynamic module loading:

```javascript
// Dynamic import with async/await
const module = await import('https://cdn.skypack.dev/lodash-es');
console.log(module.default.chunk([1,2,3,4], 2));

// Dynamic import with promises
import('https://unpkg.com/date-fns@2.29.3/esm/index.js')
  .then(dateFns => {
    console.log(dateFns.format(new Date(), 'yyyy-MM-dd'));
  });

// Conditional loading
if (userWantsAdvancedFeatures) {
  const advancedModule = await import('./advanced-features.js');
  advancedModule.initialize();
}
```

### **Scittle/SCI Integration for ClojureScript**

**Dynamic ClojureScript Evaluation**:
```javascript
// Scittle provides dynamic evaluation capabilities
js/scittle.core.eval_string("(+ 1 2 3)");

// Load ClojureScript code dynamically
const clojureCode = `
(ns dynamic-ns)
(defn greet [name] (str "Hello " name "!"))
`;
js/scittle.core.eval_string(clojureCode);
```

**Plugin Loading System**:
```html
<!-- Load Scittle plugins dynamically -->
<script src="https://cdn.jsdelivr.net/npm/scittle@0.7.26/dist/scittle.js"></script>
<script src="https://cdn.jsdelivr.net/npm/scittle@0.7.26/dist/scittle.re-frame.js"></script>
<script src="https://cdn.jsdelivr.net/npm/scittle@0.7.26/dist/scittle.promesa.js"></script>
```

### **Browser-Side Dependency Management**

**CDN-Based Loading**:
```clojure
;; ClojureScript function to dynamically load JS libraries
(defn load-js-library [lib-url]
  (-> (js/import lib-url)
      (.then (fn [module]
               ;; Make module available in SCI context
               (sci.core/add-namespace! 
                 sci-ctx 
                 {'external-lib module})))))

;; Usage
(load-js-library "https://cdn.skypack.dev/moment@2.29.4")
```

**❌ CRITICAL LIMITATION: Scittle Dynamic Module Loading**

**Current Scittle does NOT support dynamic CDN module loading:**
```clojure
;; ❌ This does NOT work in current Scittle
(require '[new.library :as lib])  
js/scittle.core.eval_string("(require '[\"https://cdn.skypack.dev/moment\" :as moment])")
```

**What Currently Works:**
```html
<!-- ✅ Pre-load libraries before Scittle initialization -->
<script src="https://cdn.jsdelivr.net/npm/lodash@4.17.21/lodash.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/scittle@0.7.26/dist/scittle.js"></script>
<script type="application/x-scittle">
  ;; ✅ Can access pre-loaded globals
  (js/_.map [1 2 3] inc)
</script>
```

**Workarounds for Dynamic Loading:**
```javascript
// Workaround 1: Dynamic script tag injection
function loadLibraryThenEval(libUrl, globalName, code) {
  const script = document.createElement('script');
  script.src = libUrl;
  script.onload = () => {
    // Now can use in Scittle
    js/scittle.core.eval_string(code);
  };
  document.head.appendChild(script);
}

// Workaround 2: ES Modules + Global assignment
import('https://cdn.skypack.dev/lodash-es').then(lodash => {
  window.lodashES = lodash;
  js/scittle.core.eval_string("(js/lodashES.chunk [1 2 3 4] 2)");
});
```

**Future Scittle Development:**
- `scittle.core/eval-string-async` (planned)
- Better `async-load-fn` support
- Import mapping for CDN URLs

**ClojureScript Code Loading (Still Works):**
```clojure
;; ✅ Dynamic ClojureScript evaluation still works
(defn load-cljs-code [code-string]
  (js/scittle.core.eval_string code-string))

;; ✅ Fetch and evaluate ClojureScript from URLs
(defn load-cljs-from-url [url]
  (-> (js/fetch url)
      (.then #(.text %))
      (.then js/scittle.core.eval_string)))
```

## Unified Dependency Management Architecture

### **Database Schema for Cross-Runtime Dependencies**

```clojure
{:dependency/name     {:db/cardinality :db.cardinality/one
                       :db/unique :db.unique/identity}
 :dependency/version  {:db/cardinality :db.cardinality/one}
 :dependency/runtime  {:db/cardinality :db.cardinality/one}  ; :bb, :browser, :both
 :dependency/source   {:db/cardinality :db.cardinality/one}  ; URL, Maven coords, etc.
 :dependency/config   {:db/cardinality :db.cardinality/one}  ; Runtime-specific config
 :dependency/loaded   {:db/cardinality :db.cardinality/one}  ; Load status
 :dependency/forms    {:db/cardinality :db.cardinality/many  ; Forms that need this
                       :db/valueType :db.type/ref}}
```

### **AI-Orchestrated Dependency Loading (Revised for Scittle Limitations)**

```clojure
;; AI coordinates dependency loading with Scittle limitations in mind
(defn ai-load-dependencies [form-fqns]
  (let [deps (analyze-dependencies-needed form-fqns)]
    
    ;; Load server-side dependencies (✅ Works fully)
    (bb-eval 
      `(deps/add-deps ~(filter #(= :bb (:runtime %)) deps)))
    
    ;; Load browser-side dependencies (❌ Limited to workarounds)
    (let [browser-deps (filter #(= :browser (:runtime %)) deps)]
      (doseq [dep browser-deps]
        ;; Use JavaScript workaround for dynamic loading
        (browser-eval 
          `(load-library-via-script-tag 
             ~(:source dep) 
             ~(:global-name dep)))))
    
    ;; Update database with load status
    (update-dependency-status! deps :loaded)))

;; Browser-side workaround functions
(defn load-library-via-script-tag [lib-url global-name]
  (let [script (.createElement js/document "script")]
    (set! (.-src script) lib-url)
    (set! (.-onload script) 
          #(println "Loaded library, available as js/" global-name))
    (.appendChild (.-head js/document) script)))
```

### **Live Development Workflow**

```clojure
;; Example: AI adds a new feature requiring new dependencies

;; 1. AI analyzes code needs
(def new-feature-code 
  "(ns analytics.charts
     (:require [reagent.core :as r]      ; Browser: React wrapper
               [oz.core :as oz]          ; Browser: Vega-Lite charts  
               [tech.ml.dataset :as ds]  ; Server: Data processing
               [semantic-csv.core :as csv])) ; Server: CSV parsing
   
   (defn create-chart [data] ...)")

;; 2. AI determines required dependencies
(def required-deps
  [{:name "reagent" :runtime :browser :source "https://cdn.skypack.dev/reagent"}
   {:name "oz" :runtime :browser :source "https://cdn.skypack.dev/oz"}  
   {:name "tech.ml.dataset" :runtime :bb :source {:mvn/version "6.0.0"}}
   {:name "semantic-csv" :runtime :bb :source {:mvn/version "0.2.1-alpha1"}}])

;; 3. AI loads dependencies in appropriate runtimes
(doseq [dep required-deps]
  (case (:runtime dep)
    :bb (bb-eval `(deps/add-deps {:deps {~(:name dep) ~(:source dep)}}))
    :browser (browser-eval `(load-js-library ~(:source dep)))))

;; 4. AI evaluates the new code
(bb-eval new-feature-code)
(browser-eval new-feature-code)

;; 5. Dependencies and code stored in database for future use
(store-in-db! {:forms [new-feature-code] :dependencies required-deps})
```

## Benefits of Live Dependency Management

### **Development Experience**
- ✅ **No Restarts**: Add libraries without stopping development flow
- ✅ **Instant Feedback**: Test new libraries immediately
- ✅ **Experimentation**: Try different versions/libraries quickly
- ✅ **AI Integration**: Automatic dependency resolution

### **System Architecture**  
- ✅ **Database-Driven**: All dependencies tracked in dl-db
- ✅ **Cross-Runtime**: Coordinate dependencies between bb and browser
- ✅ **Version Control**: Dependency changes tracked in database transactions
- ✅ **Reproducible**: Exact dependency state can be restored

### **Production Benefits**
- ✅ **Minimal Bundles**: Load only required dependencies
- ✅ **Lazy Loading**: Load features/dependencies on demand
- ✅ **Hot Updates**: Update dependencies without full redeploy
- ✅ **Rollback**: Easy dependency version rollback through db

## Implementation Priorities

### **Phase 1: Basic Live Loading** (1-2 weeks)
- Implement `babashka.deps/add-deps` integration
- Basic browser dynamic import() support
- Store dependency metadata in database

### **Phase 2: AI Integration** (2-3 weeks)
- AI dependency analysis from code
- Automatic dependency resolution
- Cross-runtime coordination

### **Phase 3: Advanced Features** (3-4 weeks)
- Dependency version management
- Conflict resolution
- Performance optimization
- Production deployment strategies

This live dependency management capability, combined with the dual nREPL access, creates an incredibly powerful development environment where the AI can:

✅ **Server-side (Babashka)**: Automatically resolve and install any required dependencies via `deps/add-deps`
⚠️ **Browser-side (Scittle)**: Use JavaScript workarounds for dynamic library loading (script injection, global assignment)  
✅ **ClojureScript Code**: Dynamically evaluate and load code from any source

**Critical Update**: While Scittle doesn't support native dynamic `require` for CDN modules, the core database-native development vision remains achievable through JavaScript interop patterns. The AI can still coordinate full-stack development, but browser dependency loading requires the documented workarounds.

## Elegant Alternative: Page Reload with State Persistence

### **Much Better Approach for Browser Dependencies**

Instead of complex JavaScript workarounds, use the database-native architecture for seamless dependency loading:

```clojure
;; AI detects need for new browser library
(defn ai-needs-browser-library [lib-name lib-url]
  ;; 1. Store current session state in database
  (save-session-state! {:ui-state (get-current-ui-state)
                        :cursor-position (get-cursor-pos)
                        :open-panels (get-open-panels)
                        :current-form (get-current-form)
                        :evaluation-history (get-eval-history)})
  
  ;; 2. Add library to required dependencies
  (add-required-library! lib-name lib-url)
  
  ;; 3. Trigger page reload with new dependencies
  (reload-page-with-dependencies!))

;; Page loads with all required libraries pre-loaded
;; Browser reconnects to WebSocket nREPL automatically
;; Restore complete state from database

(defn restore-session-on-load []
  (let [session-state (get-session-state-from-db)]
    (restore-ui-state! (:ui-state session-state))
    (restore-cursor-position! (:cursor-position session-state))
    (restore-open-panels! (:open-panels session-state))
    (restore-current-form! (:current-form session-state))
    (restore-evaluation-history! (:evaluation-history session-state))
    ;; Continue exactly where user left off
    ))
```

### **Database Schema for Session Persistence**

```clojure
{:session/id           {:db/cardinality :db.cardinality/one
                        :db/unique :db.unique/identity}
 :session/ui-state     {:db/cardinality :db.cardinality/one}
 :session/cursor-pos   {:db/cardinality :db.cardinality/one}
 :session/open-panels  {:db/cardinality :db.cardinality/many}
 :session/current-form {:db/cardinality :db.cardinality/one}
 :session/eval-history {:db/cardinality :db.cardinality/many}
 :session/timestamp    {:db/cardinality :db.cardinality/one}
 
 :page/required-libs   {:db/cardinality :db.cardinality/many
                        :db/valueType :db.type/ref}
 :page/load-order      {:db/cardinality :db.cardinality/many}}
```

### **Benefits of Page Reload Approach**

**✅ Cleaner Architecture**:
- No complex JavaScript workarounds needed
- Standard script tag loading (most reliable)
- Leverages database-native design perfectly

**✅ Seamless User Experience**:
- Page reload becomes invisible state transition
- Continue exactly where left off
- No loss of context or progress

**✅ More Reliable**:
- Libraries load in proper order
- No race conditions or loading failures
- Standard browser behavior

**✅ Database-Native Perfect Fit**:
- All state persisted in Datalevin
- Session restoration from database
- Complete development environment state capture

### **Implementation Flow**

1. **AI Analysis**: Detects code needs new library
2. **State Capture**: Save complete UI/development state to database  
3. **Dependency Update**: Add library to page requirements
4. **Smart Reload**: Page reloads with new library script tags
5. **Auto-Reconnect**: Browser WebSocket nREPL reconnects automatically
6. **State Restoration**: Restore exact development context from database
7. **Seamless Continuation**: User continues as if nothing happened

**This approach transforms page reloads from disruptive restarts into invisible state transitions - much more elegant than complex dynamic loading workarounds!**