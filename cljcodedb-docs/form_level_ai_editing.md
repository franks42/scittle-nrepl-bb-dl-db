# Form-Level AI Code Editing: Structural Replacement for Safer AI Modifications

## Overview

This document describes a structural approach to AI code editing that replaces text-based find&replace operations with form-level replacements. By working with complete Clojure forms rather than arbitrary text spans, we can guarantee syntactic correctness and provide better context for AI modifications.

## The Problem with Text-Based AI Editing

### Current AI Editing Failures

AI agents typically modify code through text-based operations that frequently break Clojure's structural syntax:

```clojure
;; Original function
(defn process-user [user]
  (-> user
      (update :name capitalize)
      (assoc :processed-at (now))))

;; AI attempts text replacement:
;; Find: "(update :name capitalize)"
;; Replace: "(update :name capitalize)\n      (validate-user)"

;; Result: Broken syntax - validate-user called on wrong value
(defn process-user [user]
  (-> user
      (update :name capitalize)
      (validate-user)  ; Wrong! Should be inside the threading
      (assoc :processed-at (now))))
```

### Root Causes of Text-Based Failures

1. **No Structural Awareness**: AI doesn't understand Clojure's nested form structure
2. **Paren Mismatch Risks**: Text operations can easily create unbalanced delimiters
3. **Context Ignorance**: AI lacks understanding of surrounding syntactic context
4. **Imprecise Targeting**: Hard to identify exact boundaries of code to replace

## Form-Level Editing Solution

### Core Concept: Structural Addressing

Instead of text coordinates, use **form paths** to precisely identify sub-expressions within top-level forms:

```clojure
;; Function with structural addresses
(defn process-user [user]          ; Root form
  (-> user                         ; Address: [0] - threading macro
      (update :name capitalize)    ; Address: [0 1] - first threaded form
      (assoc :processed-at (now)))) ; Address: [0 2] - second threaded form

;; AI specifies: "Replace form at [0 1] with validation step"
;; System ensures only complete forms are replaced
```

### Architecture Components

1. **Form Parser**: Extract structural addresses from Clojure forms
2. **Context Provider**: Give AI rich information about target forms
3. **Replacement Validator**: Ensure AI provides syntactically correct forms
4. **Structural Editor**: Safely replace forms at specified addresses

## Implementation Design

### 1. Form Structure Analysis

```clojure
(ns form-editor.parser
  (:require [clojure.zip :as zip]
            [clojure.walk :as walk]))

(defn parse-form-structure [form]
  "Parse a form and create a map of addresses to sub-forms"
  (let [structure (atom {})]
    (walk/postwalk-indexed
      (fn [index form]
        (swap! structure assoc-in [:forms index] form)
        form)
      form)
    @structure))

(defn find-form-at-text-position [source-code text-position]
  "Given a text position, find the containing form and its address"
  (let [form (read-string source-code)
        char-map (build-character-to-form-mapping source-code form)]
    (find-containing-form char-map text-position)))

(defn get-form-boundaries [form address]
  "Get the exact boundaries of a form within the larger structure"
  (let [zipper (zip/seq-zip form)
        target-loc (navigate-to-address zipper address)]
    {:start-pos (zip/start-position target-loc)
     :end-pos (zip/end-position target-loc)
     :form (zip/node target-loc)}))
```

### 2. Context-Rich Form Presentation

```clojure
(defn get-form-editing-context [top-level-form address]
  "Provide comprehensive context for AI form editing"
  {:target-form (get-form-at-address top-level-form address)
   :parent-form (get-parent-form top-level-form address)
   :sibling-forms (get-sibling-forms top-level-form address)
   :function-context {:name (extract-function-name top-level-form)
                     :parameters (extract-parameters top-level-form)
                     :docstring (extract-docstring top-level-form)}
   :available-symbols (extract-available-symbols top-level-form)
   :type-hints (infer-type-context top-level-form address)
   :parent-type (classify-parent-form top-level-form address)})

;; Example context for AI
{:target-form "(:email user)"
 :parent-form "(and (map? user) (:email user) (:name user))"
 :sibling-forms ["(map? user)" "(:name user)"]
 :function-context {:name "validate-user" 
                   :parameters ["user"]
                   :docstring "Validate user data structure"}
 :available-symbols ["user" "clojure.core/and" "my.app.utils/valid-email?"]
 :type-hints {:user "map"}
 :parent-type :and-expression}
```

### 3. Safe Form Replacement

```clojure
(defn validate-form-replacement [original-form replacement-form context]
  "Validate that a replacement form is syntactically and semantically correct"
  (try
    ;; 1. Parse as valid Clojure
    (let [parsed (read-string replacement-form)]
      
      ;; 2. Check structural compatibility
      (when (incompatible-with-context? parsed context)
        (throw (ex-info "Form incompatible with context" 
                       {:context context :form parsed})))
      
      ;; 3. Validate symbol references
      (let [undefined-symbols (find-undefined-symbols parsed context)]
        (when (seq undefined-symbols)
          (throw (ex-info "Undefined symbols" 
                         {:symbols undefined-symbols}))))
      
      {:valid true :parsed parsed})
    
    (catch Exception e
      {:valid false 
       :error (.getMessage e)
       :suggestions (suggest-fixes replacement-form context)})))

(defn replace-form-at-address [top-level-form address new-form]
  "Safely replace form at specified address"
  (let [zipper (zip/seq-zip top-level-form)
        target-loc (navigate-to-address zipper address)]
    (if (zip/node target-loc)
      (-> target-loc
          (zip/replace new-form)
          zip/root)
      (throw (ex-info "Invalid address" {:address address})))))
```

### 4. Multi-Operation Coordination

```clojure
(defn apply-coordinated-edits [top-level-form operations]
  "Apply multiple form edits atomically"
  (let [validated-ops (validate-all-operations operations top-level-form)]
    (if (:valid validated-ops)
      (reduce apply-single-operation top-level-form (:operations validated-ops))
      (throw (ex-info "Invalid operation set" validated-ops)))))

;; Example coordinated edit
{:operations 
 [{:type :replace 
   :address [1 1] 
   :new-form "(and (:email user) (valid-email? (:email user)))"}
  {:type :insert 
   :address [1] 
   :new-form "(pos? (:age user))" 
   :position :append}
  {:type :delete 
   :address [1 3]}]}
```

## AI Integration Patterns

### 1. Guided Form Discovery

```clojure
;; AI Workflow: "I want to improve email validation"

;; Step 1: AI describes intent
{:intent "improve email validation in validate-user function"
 :fqn "my.app.users/validate-user"
 :target-description "email validation logic"}

;; Step 2: System finds candidate forms
(find-forms-by-description form "email validation")
;; Returns: [{:address [1 1] :form "(:email user)" :confidence 0.9}
;;          {:address [1 1 1] :form "(valid-email? (:email user))" :confidence 0.7}]

;; Step 3: AI selects target and gets context
(get-form-editing-context form [1 1])
```

### 2. Template-Based Form Generation

```clojure
(def form-templates
  {:validation-and "(and ~original-check ~additional-check)"
   :conditional-check "(when ~condition ~action)"
   :error-handling "(try ~body (catch Exception e ~error-action))"
   :threading-step "~operation"})

;; AI can reference templates for common patterns
{:template :validation-and
 :original-check "(:email user)"
 :additional-check "(valid-email? (:email user))"}
;; Generates: "(and (:email user) (valid-email? (:email user)))"
```

### 3. Progressive Form Building

```clojure
;; AI builds complex forms incrementally
;; Start with simple form
{:new-form "(:email user)"}

;; Add validation
{:template :validation-and
 :wrap-existing true
 :additional-check "(string? (:email user))"}
;; Result: "(and (:email user) (string? (:email user)))"

;; Add format validation  
{:template :validation-and
 :wrap-existing true
 :additional-check "(re-matches email-regex (:email user))"}
;; Result: "(and (:email user) (string? (:email user)) (re-matches email-regex (:email user)))"
```

## MCP Integration

### Enhanced MCP Tools

```clojure
;; Add form-level editing tools to bb-server
(def form-editing-mcp-tools
  {:find-form-at-cursor 
   {:description "Find the complete form at a text position"
    :parameters {:fqn :string :text-position :int}}
   
   :get-form-context
   {:description "Get rich context for a form at given address"
    :parameters {:fqn :string :address :vector}}
   
   :validate-form-replacement
   {:description "Check if a replacement form is valid"
    :parameters {:fqn :string :address :vector :new-form :string}}
   
   :replace-form-safely
   {:description "Replace form with validation"
    :parameters {:fqn :string :address :vector :new-form :string}}
   
   :suggest-form-improvements
   {:description "AI suggests improvements for a specific form"
    :parameters {:fqn :string :address :vector :improvement-type :string}}})
```

### Claude Code Workflow

```clojure
;; Claude Code via MCP:

;; 1. Find target form
(find-form-at-cursor "my.app.users/validate-user" 245)
;; Returns: {:address [1 1] :form "(:email user)" :context {...}}

;; 2. Get detailed context
(get-form-context "my.app.users/validate-user" [1 1])
;; Returns rich context for AI decision making

;; 3. Validate proposed change
(validate-form-replacement "my.app.users/validate-user" [1 1] 
  "(and (:email user) (valid-email? (:email user)))")
;; Returns: {:valid true} or {:valid false :error "..." :suggestions [...]}

;; 4. Apply safe replacement
(replace-form-safely "my.app.users/validate-user" [1 1]
  "(and (:email user) (valid-email? (:email user)))")
;; Updates database with new form
```

## Database Integration

### Enhanced Form Storage

```clojure
;; Store forms with structural metadata
{:form/fqn "my.app.users/validate-user"
 :form/source "(defn validate-user [user] ...)"
 :form/structure {:addresses {[0] "user"
                             [1] "(and (map? user) (:email user) (:name user))"
                             [1 0] "(map? user)"
                             [1 1] "(:email user)"  
                             [1 2] "(:name user)"}
                 :types {[1] :and-expression
                        [1 0] :predicate-call
                        [1 1] :keyword-access
                        [1 2] :keyword-access}}
 :form/edit-history [{:timestamp "2024-01-01T10:00:00Z"
                     :address [1 1]
                     :old-form "(:email user)"
                     :new-form "(and (:email user) (valid-email? (:email user)))"
                     :editor "ai-agent"}]}
```

### Form-Level Change Tracking

```clojure
(defn track-form-edit [fqn address old-form new-form editor]
  "Track individual form changes for audit and rollback"
  (d/transact! code-db
    [{:db/id [:form/fqn fqn]
      :form/edit-history {:timestamp (java.time.Instant/now)
                         :address address
                         :old-form old-form
                         :new-form new-form
                         :editor editor}}]))

;; Query editing patterns
(d/q '[:find ?address (count ?edit)
       :in $ ?fqn
       :where
       [?form :form/fqn ?fqn]
       [?form :form/edit-history ?edit]
       [?edit :address ?address]]
     @code-db "my.app.users/validate-user")
```

## Testing Strategy

### Test Scenario 1: Basic Form Replacement

```clojure
;; Test: Replace simple expression
(def test-function
  "(defn validate-user [user]
     (and (map? user) (:email user) (:name user)))")

;; Target: Replace (:email user) with more complex validation
(def target-address [1 1])  ; Second condition in and expression

;; AI suggests replacement
(def ai-suggestion "(and (:email user) (string? (:email user)))")

;; Test validation
(validate-form-replacement test-function target-address ai-suggestion)
;; Expected: {:valid true}

;; Test replacement
(def result (replace-form-at-address 
              (read-string test-function) 
              target-address 
              (read-string ai-suggestion)))

;; Verify result
(= result 
   '(defn validate-user [user]
      (and (map? user) 
           (and (:email user) (string? (:email user)))
           (:name user))))
```

### Test Scenario 2: Complex Nested Replacement

```clojure
;; Test: Replace form within let binding
(def complex-function
  "(defn process-order [order]
     (let [user (:user order)
           amount (:amount order)]
       (when (and user amount)
         (create-order user amount))))")

;; Target: Improve user validation in when condition
(def target-address [2 0 0])  ; First condition in and

;; AI suggests more thorough validation
(def ai-suggestion "(validate-user user)")

;; Test the replacement maintains let structure
(test-complex-replacement complex-function target-address ai-suggestion)
```

### Test Scenario 3: Multi-Form Coordinated Edit

```clojure
;; Test: Add validation with helper function
(def target-function
  "(defn register-user [user-data]
     (create-user user-data))")

;; AI wants to add validation and error handling
(def coordinated-edit
  {:operations
   [{:type :wrap
     :address [0]
     :template :conditional-check
     :condition "(valid-user-data? user-data)"
     :action "(create-user user-data)"
     :else-action "(throw (ex-info \"Invalid user data\" {:data user-data}))"}]})

;; Test coordinated application
(test-coordinated-edit target-function coordinated-edit)
```

### Test Scenario 4: Error Recovery and Suggestions

```clojure
;; Test: AI provides invalid replacement
(def invalid-suggestion "(:email user")  ; Missing closing paren

;; System should detect and provide helpful suggestions
(def validation-result 
  (validate-form-replacement test-function [1 1] invalid-suggestion))

;; Expected result
{:valid false
 :error "Unmatched delimiter"
 :suggestions ["(:email user)"  ; Add missing paren
              "(get user :email)"  ; Alternative syntax
              "(:email user nil)"]}  ; With default value
```

### Test Scenario 5: AI Context Utilization

```clojure
;; Test: AI uses context to make better suggestions
(def context-test-function
  "(defn update-user [user updates]
     (let [current (get-user (:id user))]
       (merge current updates)))")

;; AI gets context for merge operation
(def context (get-form-editing-context 
               (read-string context-test-function) 
               [2 0]))  ; merge form

;; Context should include:
{:available-symbols ["user" "updates" "current" "merge" "get-user"]
 :parent-type :let-binding
 :function-context {:name "update-user" :parameters ["user" "updates"]}}

;; AI should suggest improvements that use available context
(test-context-aware-suggestions context-test-function [2 0] context)
```

## AI Code Generation for Database-Native Development

### Direct Database Form Generation

AIs are naturally well-suited for generating database-ready code structures. The structural analysis required aligns perfectly with AI strengths:

#### Form Decomposition
```clojure
;; AI can easily break namespace code into individual forms:
(ns my.app.users
  (:require [clojure.string :as str]))

(def email-regex #".+@.+\..+")

(defn valid-email? [email]
  (and (string? email)
       (re-matches email-regex email)))

;; AI generates structured form data:
[{:form/fqn "my.app.users/email-regex"
  :form/source "(def email-regex #\".+@.+\\..+\")"
  :form/type :def
  :form/pure? true
  :form/dependencies []}
 
 {:form/fqn "my.app.users/valid-email?"
  :form/source "(defn valid-email? [email] ...)"
  :form/type :defn
  :form/pure? true
  :form/dependencies ["my.app.users/email-regex" "clojure.core/re-matches"]}]
```

#### Automatic Purity Analysis
```clojure
;; AI can categorize forms by recognizing patterns:

;; PURE - referentially transparent
(defn transform-user [user]
  (assoc user :formatted-name (str/capitalize (:name user))))

;; IMPURE - side effects, order dependent  
(def db-config (load-config-file!))     ; File I/O
(def connection (connect-to-db db-config)) ; Network, depends on db-config

;; AI recognizes impurity indicators:
;; - Functions with ! suffix
;; - System/getenv, slurp, spit, println
;; - Database/network operations
;; - File I/O operations
```

#### Order Dependency Detection
```clojure
;; AI can detect initialization chains:
(def database-url (System/getenv "DATABASE_URL"))     ; Must be first
(def connection (connect! database-url))              ; Depends on database-url
(def schema (load-schema! connection))                ; Depends on connection

;; AI output includes dependency ordering:
{:order-dependent-chain 
 ["my.app.db/database-url" 
  "my.app.db/connection" 
  "my.app.db/schema"]
 :initialization-required true}
```

#### AI Instruction Pattern
```
When generating Clojure code for database storage:

1. FORM DECOMPOSITION: Break into individual top-level forms (defn, def, defprotocol)
2. PURITY ANALYSIS: Mark pure? false if contains: System/getenv, slurp, spit, println, database ops, network calls, file I/O
3. DEPENDENCY EXTRACTION: List all non-clojure.core symbols used
4. FQN GENERATION: Always use fully qualified names like my.app.users/validate-user
5. ORDER ANALYSIS: Mark dependency chains for proper initialization sequence

Output as DataScript transactions ready for direct database insertion.
```

## Database Queries vs File Scanning for AI Analysis

### Superior Code Discovery Through Queries

The shift from file scanning to database queries represents a fundamental improvement in AI code analysis capabilities:

#### Current File-Based Limitations
```clojure
;; Current AI workflow:
;; 1. Linear scan through files looking for text patterns
;; 2. Parse file content to understand context
;; 3. Track cross-file dependencies manually
;; 4. Remember fragile file:line coordinates

;; Problems:
;; - Slow, inefficient scanning
;; - Context limited to current file view
;; - Difficult semantic relationship discovery
;; - Brittle line number references
```

#### Database Query Advantages
```clojure
;; AI can ask direct semantic questions:

;; "Find all validation functions"
(d/q '[:find ?fqn ?source
       :where 
       [?e :form/fqn ?fqn]
       [?e :form/source ?source]
       [(clojure.string/includes? ?fqn "valid")]]
     @code-db)

;; "Find functions that process users and are impure"
(d/q '[:find ?fqn ?source
       :where
       [?e :form/fqn ?fqn] 
       [?e :form/dependencies ?dep]
       [?e :form/source ?source]
       [?e :form/pure? false]
       [(clojure.string/includes? ?dep "user")]]
     @code-db)

;; "Find unreferenced code that could be removed"
(d/q '[:find ?fqn
       :where
       [?e :form/fqn ?fqn]
       (not-join [?fqn]
         [?other :form/dependencies ?fqn])]
     @code-db)
```

#### Rich Contextual Analysis
```clojure
;; AI gets comprehensive context for any form:
(defn get-ai-analysis-context [fqn]
  {:form (get-form fqn)
   :dependencies (find-dependencies fqn)
   :dependents (find-dependents fqn)
   :namespace-context (get-namespace-forms (namespace fqn))
   :similar-functions (find-similar-patterns fqn)
   :complexity-metrics (analyze-complexity fqn)
   :edit-history (get-edit-history fqn)
   :test-coverage (find-related-tests fqn)})

;; Far richer than traditional file:line context
```

#### Pattern Discovery and Code Intelligence
```clojure
;; AI can discover sophisticated patterns:

;; "Functions following similar validation approaches"
(d/q '[:find ?fqn (count ?validation-step)
       :where
       [?e :form/fqn ?fqn]
       [?e :form/source ?source]
       [(extract-validation-pattern ?source) ?validation-step]]
     @code-db)

;; "Candidates for function extraction"
(find-extraction-candidates "my.app.orders")

;; "Circular dependencies that need refactoring"
(find-circular-dependencies @code-db)

;; "Functions with high complexity that need attention"
(d/q '[:find ?fqn ?complexity
       :where
       [?e :form/fqn ?fqn]
       [?e :form/complexity-score ?complexity]
       [(> ?complexity 10)]]
     @code-db)
```

### AI Development Workflow Comparison

#### File-Based Analysis (Current)
```clojure
;; AI: "Add validation to user processing functions"
;; 1. Scan files for text patterns containing "user"
;; 2. Read entire files to understand surrounding context
;; 3. Parse code manually to find insertion points
;; 4. Generate fragile file:line modification instructions
;; 5. Hope other changes don't invalidate line numbers
;; 6. Manually track cross-file dependencies
```

#### Database-Based Analysis (Proposed)
```clojure
;; AI: "Add validation to user processing functions"
;; 1. Query for user-related functions
(find-functions-by-domain "user")

;; 2. Get comprehensive context for each candidate
(get-ai-analysis-context "my.app.orders/process-user")

;; 3. Query for existing validation patterns to follow
(find-validation-patterns @code-db)

;; 4. Generate new form or modify existing with structural addressing
(create-form "my.app.validation/validate-order-user" source)
(update-form-at-address "my.app.orders/process-user" [1 2] new-validation)

;; 5. Update dependencies atomically across entire codebase
(update-all-references old-fqn new-fqn)
```

### Semantic Code Understanding

#### Intent-Based Code Discovery
```clojure
;; Instead of text-based search:
"grep -r 'validate' src/"

;; AI can perform semantic analysis:
(find-forms-by-intent "validation logic")
(find-forms-by-pattern "error-handling") 
(find-forms-by-role "data-transformation")
(find-forms-needing-refactoring)
```

#### Relationship Analysis
```clojure
;; AI easily discovers code relationships:
(analyze-coupling-between-namespaces)
(find-god-functions)  ; Functions doing too much
(suggest-namespace-splits)
(identify-missing-abstractions)
(find-duplicate-logic-patterns)
```

#### Context-Aware Modifications
```clojure
;; AI understands full impact of changes:
{:modifying "my.app.users/validate-user"
 :current-dependencies ["clojure.core/and" "clojure.core/map?"]
 :impact-analysis {:will-affect ["my.app.orders/process-order" 
                                "my.app.api/create-user"]
                  :tests-to-update ["test-user-validation" 
                                   "test-order-processing"]
                  :documentation-updates ["api-docs" "user-guide"]}
 :confidence-score 0.95}
```

## Benefits and Advantages

### ✅ **Guaranteed Syntax Correctness**
- AI can only replace complete, valid forms
- Impossible to create mismatched parentheses
- Structural validation before any changes applied

### ✅ **Precise Targeting**
- Exact boundaries for code replacement
- No ambiguity about what gets modified
- Clear addressing system for complex structures

### ✅ **Rich AI Context**
- AI sees exact form structure and relationships
- Available symbols and type information provided
- Parent context informs better decisions

### ✅ **Better AI Reasoning**
- Form-level operations match AI's natural thinking
- Templates and patterns guide common operations
- Progressive building of complex expressions

### ✅ **Safer Refactoring**
- Atomic form-level operations
- Easy rollback of individual changes
- Audit trail of specific form modifications

### ✅ **Enhanced Development Patterns**
- Works naturally with database-native storage
- Integrates with existing MCP infrastructure
- Enables form-level version control and diffing

### ✅ **Superior Code Intelligence**
- Database queries replace inefficient file scanning
- Semantic search capabilities vs text pattern matching
- Comprehensive relationship analysis built-in
- AI gets rich contextual information for better decisions

### ✅ **Direct Database Integration**
- AI can generate database-ready form structures
- Automatic purity and dependency analysis
- Order dependency detection for proper initialization
- Seamless integration with database-native development workflow

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)
- Basic form parsing and addressing
- Simple replacement operations
- Integration with existing bb-server MCP

### Phase 2: AI Integration (Week 3-4)
- Context-rich form presentation
- Validation and suggestion systems
- Template-based form generation

### Phase 3: Advanced Features (Week 5-6)
- Multi-operation coordination
- Error recovery and suggestions
- Database integration with change tracking

### Phase 4: Production Testing (Week 7-8)
- Comprehensive test suite
- Real-world AI editing scenarios
- Performance optimization and scaling

This form-level editing approach represents a fundamental improvement in AI code modification safety and effectiveness, leveraging Clojure's inherent structural nature to provide better development tools.