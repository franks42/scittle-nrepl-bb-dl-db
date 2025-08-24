# Clojure Runtime Environments and Code Analysis: SCI, Native Eval, and Cross-Platform Development

## Overview

This document explores the different runtime environments available for Clojure code execution, with particular focus on SCI (Small Clojure Interpreter) as a sandboxed, portable alternative to native evaluation. We examine code analysis capabilities, sandboxing features, and architectural considerations for building interactive development environments.

## Runtime Environment Comparison

### 1. Native JVM Clojure

**Architecture**:
- Code compiled directly to JVM bytecode via `clojure.lang.Compiler`
- Uses Clojure's built-in namespace and var system
- Full access to JVM ecosystem and loaded libraries
- Runtime compilation with caching

**Evaluation Process**:
```clojure
;; Direct compilation to bytecode
(eval '(defn my-func [x] (inc x)))
;; Function immediately available with full JVM performance
```

**Characteristics**:
- **Performance**: Maximum speed - compiled bytecode
- **Ecosystem**: Complete Clojure library compatibility
- **Security**: No built-in sandboxing - full system access
- **Debugging**: Standard JVM stack traces and profiling
- **Startup**: Slower JVM startup time
- **Memory**: Higher memory footprint

### 2. SCI (Small Clojure Interpreter)

**Architecture**:
- Pure Clojure interpreter that parses code into AST
- Custom evaluation engine without compilation
- Controlled sandbox environment
- Maintains separate namespace system

**Evaluation Process**:
```clojure
;; Interpretation without compilation
(sci/eval-string 
  "(defn my-func [x] (inc x))" 
  {:namespaces {'clojure.core {'inc inc}}})
```

**Characteristics**:
- **Performance**: Interpretation overhead, slower than compiled
- **Ecosystem**: Limited to explicitly enabled functions
- **Security**: Built-in sandboxing with fine-grained control
- **Debugging**: SCI-specific error handling and stack traces
- **Startup**: Very fast startup
- **Memory**: Lower memory footprint

### 3. ClojureScript

**Architecture**:
- Compiles Clojure to JavaScript
- Uses Google Closure Compiler for optimization
- Targets browser or Node.js environments
- Can be self-hosted (compiles itself in browser)

**Evaluation Process**:
```clojure
;; Traditional compilation
cljs → JavaScript → Browser/Node execution

;; Self-hosted evaluation
(cljs/eval-str repl-state code-string callback)
```

**Characteristics**:
- **Performance**: JavaScript runtime performance
- **Ecosystem**: ClojureScript-compatible libraries only
- **Security**: Browser sandbox or Node.js environment
- **Debugging**: JavaScript developer tools
- **Startup**: Fast in browser, varies in Node.js
- **Memory**: Depends on JavaScript engine

### 4. Babashka

**Architecture**:
- Native binary with embedded SCI interpreter
- Pre-compiled with common libraries
- Fast startup for scripting use cases
- Built on GraalVM native image

**Evaluation Process**:
```clojure
;; SCI evaluation in native binary
bb -e "(+ 1 2 3)"  ; Uses SCI under the hood
```

**Characteristics**:
- **Performance**: SCI interpretation speed, fast startup
- **Ecosystem**: Curated set of libraries included
- **Security**: SCI sandboxing capabilities
- **Debugging**: SCI error handling
- **Startup**: Instant (native binary)
- **Memory**: Very low memory usage

## SCI Sandboxing Features

### Core Sandboxing Concepts

**Controlled Namespace Access**:
```clojure
;; Explicit function exposure
(def safe-context
  {:namespaces {'clojure.core {'+ +, 'map map, 'filter filter}
                'my.utils {'helper-fn helper-fn}}})

;; Only these functions are available to evaluated code
(sci/eval-string "(map inc [1 2 3])" safe-context)  ; Works
(sci/eval-string "(slurp \"file.txt\")" safe-context)  ; Error
```

**Allow/Deny Lists**:
```clojure
;; Whitelist approach
(def whitelist-context
  {:allow '[defn def let if when map filter reduce]})

;; Blacklist approach  
(def blacklist-context
  {:deny '[slurp spit System/getenv eval]})

;; Combined approach
(def combined-context
  {:allow '[defn def let if when]
   :deny '[eval]
   :namespaces {'clojure.core safe-core-functions}})
```

### Advanced Sandboxing

**Function Wrapping and Monitoring**:
```clojure
(defn wrap-with-logging [f fname]
  (fn [& args]
    (println "Calling" fname "with args:" args)
    (let [result (apply f args)]
      (println "Result:" result)
      result)))

(def monitored-context
  {:namespaces 
   {'clojure.core 
    {'map (wrap-with-logging map 'map)
     'filter (wrap-with-logging filter 'filter)}}})
```

**Resource Limiting**:
```clojure
(defn with-timeout [ms f]
  (let [future (future (f))
        result (deref future ms ::timeout)]
    (when (= result ::timeout)
      (future-cancel future)
      (throw (ex-info "Execution timeout" {:timeout-ms ms})))
    result))

(defn eval-with-limits [code context]
  (with-timeout 5000  ; 5 second timeout
    #(sci/eval-string code context)))
```

**Memory and Recursion Control**:
```clojure
(def limited-context
  {:namespaces 
   {'clojure.core 
    (assoc safe-core-functions
           'recur (fn [& args] 
                    (throw (ex-info "Recursion not allowed" {}))))}})
```

### Dynamic Context Management

**Runtime Context Modification**:
```clojure
(def dynamic-context (atom {:namespaces {}}))

(defn enable-function [ctx-atom ns-name fn-name fn-var]
  (swap! ctx-atom assoc-in [:namespaces ns-name fn-name] fn-var))

(defn disable-function [ctx-atom ns-name fn-name]
  (swap! ctx-atom update-in [:namespaces ns-name] dissoc fn-name))

;; Progressive exposure
(enable-function dynamic-context 'clojure.core '+ +)
(enable-function dynamic-context 'clojure.core 'map map)
```

**Context Presets**:
```clojure
(def context-presets
  {:minimal {:namespaces {'clojure.core {'+ +, '- -, 'defn defn}}}
   
   :safe-dev {:namespaces {'clojure.core safe-core-functions
                          'clojure.string safe-string-functions}}
   
   :full-dev {:namespaces (build-full-development-context)}})

(defn apply-preset [preset-name]
  (reset! dynamic-context (get context-presets preset-name)))
```

## SCI as Portable Runtime

### Cross-Platform Consistency

**Same Code, Different Platforms**:
```clojure
;; This exact context works in:
;; - JVM Clojure
;; - ClojureScript (browser)
;; - Babashka

(def portable-context
  {:namespaces {'app.core {'process-data process-data-fn
                          'validate-input validate-input-fn}}
   :allow '[defn def let if when]})

;; Identical behavior across platforms
(sci/eval-string user-code portable-context)
```

**Platform Detection and Adaptation**:
```clojure
(defn create-platform-context []
  (let [base-context {:allow '[defn def let if when]}]
    (cond
      ;; Babashka-specific additions
      (System/getProperty "babashka.version")
      (assoc base-context :namespaces 
             {'babashka.fs filesystem-functions})
      
      ;; ClojureScript-specific additions  
      (exists? js/window)
      (assoc base-context :namespaces
             {'cljs.core cljs-core-functions})
      
      ;; JVM Clojure default
      :else base-context)))
```

### Universal Development Environment

**Consistent Development Experience**:
```clojure
;; Same validation logic everywhere
(defn validate-statement [code-string]
  (try
    (sci/eval-string code-string validation-context)
    {:valid? true}
    (catch Exception e
      {:valid? false :error (.getMessage e)})))

;; Works identically in bb, clj, cljs
```

**Shared Development Tools**:
```clojure
;; Statement analysis - same across platforms
(defn analyze-statement [code-string]
  (let [form (sci/parse-string code-string)]
    {:type (statement-type form)
     :dependencies (extract-dependencies form)
     :pure? (pure-statement? form)}))
```

## Code Analysis and AST Differences

### Native Clojure Code Analysis

**Reader-Based Analysis**:
```clojure
;; Parse to Clojure data structures
(def form (read-string "(defn hello [name] (str \"Hello \" name))"))

;; Direct data structure manipulation
(first form)        ; => defn
(second form)       ; => hello
(nth form 2)        ; => [name]
(nth form 3)        ; => (str "Hello " name)
```

**tools.analyzer Integration**:
```clojure
(require '[clojure.tools.analyzer.jvm :as ana])

(defn deep-analyze [code-string]
  (let [form (read-string code-string)
        ast (ana/analyze form)]
    {:op (:op ast)           ; :def, :fn, etc.
     :var (:var ast)         ; The var being defined
     :deps (extract-deps ast) ; Dependency analysis
     :pure? (analyze-purity ast)}))
```

**Symbol Extraction**:
```clojure
(defn extract-symbols [form]
  (cond
    (symbol? form) #{form}
    (sequential? form) (apply clojure.set/union (map extract-symbols form))
    (map? form) (clojure.set/union 
                  (extract-symbols (keys form))
                  (extract-symbols (vals form)))
    :else #{}))
```

### SCI Code Analysis

**SCI Parse Results**:
```clojure
;; SCI parsing returns similar data structures
(def sci-form (sci/parse-string "(defn hello [name] (str \"Hello \" name))"))

;; Same structure as native Clojure reader
;; Can use identical analysis functions
(analyze-form sci-form)
```

**SCI-Specific Analysis**:
```clojure
(defn sci-analyze-with-context [code-string context]
  (try
    (let [form (sci/parse-string code-string)
          result (sci/eval-form form context)]
      {:parsed-form form
       :evaluation-result result
       :symbols-resolved (check-symbol-resolution form context)
       :valid? true})
    (catch Exception e
      {:valid? false 
       :error (.getMessage e)
       :error-type (type e)})))
```

**Context-Aware Validation**:
```clojure
(defn validate-symbols-in-context [form context]
  (let [symbols (extract-symbols form)
        available-symbols (get-available-symbols context)]
    {:unresolved (clojure.set/difference symbols available-symbols)
     :resolved (clojure.set/intersection symbols available-symbols)}))
```

### AST Comparison: Native vs SCI

**Native Clojure AST** (via tools.analyzer):
```clojure
;; Rich AST with type information
{:op :def
 :form (def x 10)
 :env {...}
 :var #'user/x
 :init {:op :const
        :form 10
        :literal? true
        :type java.lang.Long}}
```

**SCI Parse Result**:
```clojure
;; Simpler data structure
(def x 10)  ; Same as read-string result

;; But can be analyzed with custom functions
{:type :def
 :name 'x
 :init-form 10
 :namespace 'user}
```

**Practical Implications**:
- **Native**: Richer type information, JVM-specific details
- **SCI**: Simpler, more portable, easier to analyze
- **Both**: Support same symbolic analysis approaches
- **Compatibility**: Same reader, similar analysis techniques

## Architecture Considerations

### Hybrid Runtime Strategies

**SCI for Development, Native for Production**:
```clojure
;; Development workflow
(defn develop-statement [code-string]
  ;; 1. Parse and validate in SCI
  (let [validation (sci/eval-string code-string safe-context)]
    (when (:valid? validation)
      ;; 2. Store in database if valid
      (store-statement code-string)
      ;; 3. Generate native Clojure for production
      (generate-production-code))))
```

**Multi-Runtime Testing**:
```clojure
(defn test-across-runtimes [code-string test-data]
  {:sci-result (test-in-sci code-string test-data)
   :native-result (test-in-native code-string test-data)
   :cljs-result (test-in-cljs code-string test-data)})
```

### Performance Trade-offs

**Development Speed vs Runtime Speed**:
```clojure
;; Fast iteration in SCI
(time (sci/eval-string code safe-context))
;; => "Elapsed time: 5 msecs"

;; Compilation overhead in native
(time (eval (read-string code)))
;; => "Elapsed time: 50 msecs" (first time)
;; => "Elapsed time: 0.1 msecs" (subsequent calls)
```

**Memory Usage Patterns**:
- **SCI**: Lower baseline, linear growth with code size
- **Native**: Higher baseline, compilation caching overhead
- **Babashka**: Minimal overhead, fixed memory patterns

### Security Boundaries

**SCI Security Model**:
```clojure
;; Complete isolation by default
(sci/eval-string user-code {})  ; Empty context = maximum security

;; Graduated exposure
(def security-levels
  {:untrusted {:allow '[+ - * /]}
   :basic {:allow '[defn def let if when] 
          :namespaces {'clojure.core basic-functions}}
   :trusted {:namespaces full-development-context}})
```

**Native Clojure Security Challenges**:
```clojure
;; Hard to prevent in native evaluation
(eval '(System/exit 0))  ; Can't easily sandbox this
(eval '(slurp "/etc/passwd"))  ; Or this

;; Requires external sandboxing (containers, JVM security manager)
```

## Best Practices and Recommendations

### Choosing the Right Runtime

**Use SCI When**:
- Sandboxing and security are critical
- Fast startup time is important
- Code needs to run identically across platforms
- Building interactive development tools
- Evaluating untrusted user code

**Use Native Clojure When**:
- Maximum performance is required
- Full ecosystem compatibility is needed
- Complex macro usage is involved
- Production deployment requirements
- Rich debugging and profiling capabilities needed

**Use ClojureScript When**:
- Building web-based development environments
- Client-side code execution is required
- Integration with browser APIs is needed
- Deployment to browser/Node.js environments

**Use Babashka When**:
- Building command-line development tools
- Fast scripting and automation
- CI/CD pipeline integration
- Local development utilities

### Hybrid Architecture Patterns

**Validation Pipeline**:
1. **Parse** in SCI for syntax validation
2. **Analyze** for dependency detection
3. **Sandbox test** in SCI for safety
4. **Store** in database if valid
5. **Compile** to target runtime for production

**Development Environment Stack**:
- **Editor**: ClojureScript + SCI (browser-based)
- **Validation**: SCI (all platforms)
- **Production**: Native Clojure compilation
- **Tooling**: Babashka scripts

This multi-runtime approach provides maximum flexibility while maintaining consistency and safety across the development workflow.

---

*This document serves as a comprehensive guide to understanding the trade-offs and capabilities of different Clojure runtime environments, with particular emphasis on SCI's role in building secure, portable development tools.*