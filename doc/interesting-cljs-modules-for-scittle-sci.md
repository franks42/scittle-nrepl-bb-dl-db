# 🎨 Interesting ClojureScript Modules for Scittle/SCI Browser Environment

This document catalogs ClojureScript libraries and modules that are particularly well-suited for use in a Scittle/SCI browser REPL environment.

## 🔷 Applied Science Shapes Library

**Repository**: https://github.com/applied-science/shapes  
**Type**: Graphics/SVG Drawing Library  
**Compatibility**: ✅ Excellent - Pure ClojureScript, .cljc format  

### Overview
A simple, beginner-friendly ClojureScript library for drawing shapes using SVG. Inspired by functional geometry concepts from computer science literature, including Peter Henderson's "Functional Geometry" (1982) and SICP.

### Key Features
- **Pure Functional API**: All drawing operations are pure functions
- **Composable**: Shapes can be combined, transformed, and layered
- **Browser-Native**: Targets SVG standard in modern browsers
- **No External Dependencies**: Pure ClojureScript implementation

### API Highlights

```clojure
;; Basic Shapes
(circle radius)              ; Create a circle
(rectangle width height)     ; Create a rectangle  
(triangle size)             ; Create an equilateral triangle
(text "Hello")              ; Create text element
(image "path/to/img.png")  ; Add image

;; Transformations (all return new shapes)
(scale amount shape)        ; Scale a shape
(rotate degrees shape)      ; Rotate a shape
(position x y shape)        ; Position at x,y
(fill color shape)          ; Set fill color
(stroke color shape)        ; Set stroke color

;; Composition
(layer shape1 shape2 ...)   ; Layer shapes on top of each other
(beside shape1 shape2 ...)  ; Arrange shapes horizontally
(above shape1 shape2 ...)   ; Stack shapes vertically

;; Color Helpers
(rgb r g b)                 ; RGB color
(hsl h s l)                 ; HSL color
;; Plus predefined color map
```

### Integration with Scittle/SCI

**Loading Strategy**:
```clojure
;; Option 1: Direct load via server file read
(def shapes-src (read-server-file "src/applied_science/shapes.cljc"))
(load-string shapes-src)

;; Option 2: Via require if on classpath
(require '[applied-science.shapes :as shapes])
```

**Example Usage in Browser REPL**:
```clojure
;; Create an animated logo
(defn scittle-logo []
  (-> (layer 
        (fill "lightblue" (circle 100))
        (fill "white" (text "Scittle"))
        (position 0 30 (fill "gray" (text "Browser REPL"))))
      (rotate (* 0.1 (js/Date.now)))
      render-svg))

;; Interactive drawing
(defn draw-at [x y]
  (-> (circle 10)
      (fill (hsl (mod x 360) 100 50))
      (position x y)))
```

### Why It's Perfect for Scittle/SCI

1. **No Build Step Required**: Pure ClojureScript that can be loaded directly
2. **Educational Value**: Great for teaching functional programming concepts
3. **Interactive Development**: Perfect for REPL-driven graphics exploration
4. **Lightweight**: Small library with no complex dependencies
5. **Cross-Platform**: .cljc format works in both Clojure and ClojureScript

### Potential Use Cases in Your Environment

- **Live Coding Demos**: Create graphics interactively in the browser
- **Data Visualization**: Build simple charts and diagrams
- **Educational Tools**: Teach programming concepts visually
- **UI Prototyping**: Quick mockups of visual components
- **Generative Art**: Create algorithmic artwork in the browser

### Integration Notes

With your WebSocket dispatch system, you could even:
- Generate SVG on the server (Babashka) and send to browser
- Create collaborative drawing where browser and server both contribute
- Use database queries to drive visualizations

---

## 🔧 Applied Science JS-Interop Library

**Repository**: https://github.com/applied-science/js-interop  
**Type**: JavaScript Interoperability Library  
**Compatibility**: ✅ Excellent - Pure ClojureScript, designed for browser use  

### Overview
A JavaScript-interop library for ClojureScript that provides clean, Clojure-like methods for working with JavaScript objects. Solves common compiler renaming issues and provides a more idiomatic approach to JS interop.

### Key Features
- **Clojure-like API**: Mirrors core Clojure functions for JS objects
- **Nil-safe Operations**: All operations handle nil gracefully
- **Compiler-friendly**: Handles both static and renamable keys properly
- **Threading Support**: Works seamlessly with `->` and `->>`

### API Highlights

```clojure
;; Reading values
(j/get obj :key)                    ; Get property
(j/get-in obj [:nested :path])      ; Get nested property
(j/select-keys obj [:a :b])         ; Select specific keys

;; Writing values (mutates!)
(j/assoc! obj :key "value")         ; Set property
(j/assoc-in! obj [:nested :key] 42) ; Set nested property
(j/update! obj :count inc)          ; Update with function

;; Function calls
(j/call obj :methodName arg1 arg2)  ; Call method
(j/apply obj :methodName #js[args]) ; Apply with array

;; Object creation
(j/lit {:a 1 :b 2})                 ; Create JS object
(j/lit [1 2 3])                     ; Create JS array

;; Special operations
(j/obj :a 1 :b 2)                   ; Create object from pairs
(j/contains? obj :key)              ; Check if key exists
(j/lookup obj :key "default")       ; Get with default value
```

### Integration with Scittle/SCI

**Why It's Essential for Browser Work**:
1. **DOM Manipulation**: Clean API for working with DOM elements
2. **Event Handling**: Easy access to event properties
3. **Browser APIs**: Simplified interaction with Web APIs
4. **External Libraries**: Better integration with JS libraries

**Example Usage in Browser REPL**:
```clojure
;; DOM manipulation
(-> (js/document.querySelector "#my-div")
    (j/assoc! :innerHTML "Hello from Scittle!")
    (j/update! :style (fn [s] (j/assoc! s :color "blue"))))

;; Event handling
(defn handle-click [event]
  (let [target (j/get event :target)
        x (j/get event :clientX)
        y (j/get event :clientY)]
    (j/call console :log "Clicked at" x y)))

;; Working with fetch API
(-> (js/fetch "/api/data")
    (.then (fn [resp] (j/call resp :json)))
    (.then (fn [data] 
             (j/get-in data [:result :items]))))

;; Creating complex JS objects
(def chart-config
  (j/lit {:type "bar"
          :data {:labels ["Jan" "Feb" "Mar"]
                 :datasets [{:label "Sales"
                            :data [12 19 3]}]}
          :options {:responsive true}}))
```

### Why It's Perfect for Scittle/SCI

1. **Essential for Browser**: Almost all browser work requires JS interop
2. **Cleaner Code**: More readable than native JS interop syntax
3. **Safer Operations**: Nil-safe prevents common runtime errors
4. **REPL-Friendly**: Explore JS objects interactively
5. **No Dependencies**: Pure ClojureScript implementation

### Real-World Use Cases in Your Environment

```clojure
;; WebSocket message handling
(defn handle-ws-message [msg]
  (let [data (j/call js/JSON :parse (j/get msg :data))
        type (j/get data :type)
        payload (j/get data :payload)]
    (case type
      "eval" (eval-code payload)
      "query" (run-query payload)
      nil)))

;; Browser storage
(defn save-to-local-storage [key value]
  (-> js/localStorage
      (j/call :setItem key (js/JSON.stringify (clj->js value)))))

;; Dynamic style updates
(defn animate-element [elem]
  (j/assoc-in! elem [:style :transition] "all 0.3s ease")
  (j/assoc-in! elem [:style :transform] "scale(1.1)"))
```

### Integration Notes

This library is almost **mandatory** for serious browser work in Scittle/SCI because:
- Makes DOM manipulation bearable
- Simplifies event handling
- Enables clean integration with any JavaScript library
- Provides consistent API across all JS operations

With your WebSocket dispatch system, js-interop would be crucial for:
- Handling WebSocket messages
- Manipulating DOM based on server responses
- Integrating with browser APIs (localStorage, fetch, etc.)
- Working with any third-party JS libraries

---

## 📝 Evaluation Criteria for Scittle/SCI Compatibility

When evaluating libraries for use in Scittle/SCI browser environment:

### ✅ Ideal Characteristics
- Pure ClojureScript or .cljc files
- No NPM dependencies
- No complex build requirements
- Self-contained functionality
- Browser-friendly APIs
- Small file size

### ⚠️ Possible with Adaptation
- Libraries with minimal JS interop
- Simple macro usage
- Basic core.async usage

### ❌ Not Compatible
- Libraries requiring Node.js
- Complex macro systems
- Java interop dependencies
- Libraries needing webpack/shadow-cljs specific features

---

## 🔍 Libraries to Investigate

### ⚠️ Fireworks - Pretty-printing and Debugging Library

**Repository**: https://github.com/paintparty/fireworks  
**Type**: Debugging/Pretty-printing Library  
**Compatibility**: ⚠️ **Uncertain** - Complex macro system, may need adaptation  
**Primary Use**: Terminal/console pretty-printing with colors and formatting

#### Overview
A sophisticated debugging library that provides enhanced value printing with syntax coloring, truncation, and metadata display. Works in both terminal and browser console environments.

#### Key Features
- Syntax-colored output for complex data structures
- Aggressive truncation for large collections
- Inline metadata display
- Multiple themes (light/dark)
- Rainbow bracket coloring

#### Potential Use Cases for nREPL Introspection
```clojure
;; In a terminal nREPL client session:
(? some-complex-data)  ; Pretty-prints with colors and context

;; Could be useful for:
;; - Debugging WebSocket messages
;; - Introspecting database query results
;; - Visualizing nested data structures
;; - REPL-driven development feedback
```

#### Compatibility Concerns for Scittle/SCI

**Challenges**:
1. **Heavy Macro Usage**: Core functionality relies on macros (`?`, `!?`, `?>`)
2. **Multiple Dependencies**: Requires several sub-namespaces
3. **Compile-time Features**: Uses reader conditionals and compile-time logic
4. **Terminal Detection**: Complex logic for detecting terminal vs browser

**Potential Workarounds**:
- Extract just the formatting functions (without macros)
- Use only the browser console features
- Adapt the pretty-printing logic for simpler use

#### My Opinion
While Fireworks offers **excellent debugging capabilities**, it might be **overkill for Scittle/SCI** due to:
- **Macro complexity** that SCI might not fully support
- **Terminal-focused features** less relevant for browser REPL
- **Simpler alternatives** available (like `cljs.pprint`)

**Better suited for**:
- Server-side Babashka nREPL debugging
- Terminal-based development workflows
- Complex data structure introspection in traditional REPL

**For your browser environment**, consider simpler alternatives:
```clojure
;; Simple colored console output
(defn log-with-color [color & args]
  (js/console.log 
    (str "%c" (pr-str args))
    (str "color: " color)))

;; Use built-in pprint
(require '[cljs.pprint :as pp])
(pp/pprint complex-data)
```

### Potential Candidates
- **Reagent** - React wrapper (may need adaptation)
- **Garden** - CSS generation (pure Clojure)
- **Clerk** - Notebook system (server-side, but interesting patterns)
- **Oz** - Vega-Lite visualization (depends on external JS)
- **Quil** - Processing-style graphics (may be too heavy)

### Research Notes
*To be expanded as more libraries are evaluated...*

---

## 📚 Resources

- [Scittle Repository](https://github.com/babashka/scittle)
- [SCI Documentation](https://github.com/babashka/sci)
- [ClojureScript Libraries List](https://clojurescript.org/community/libraries)