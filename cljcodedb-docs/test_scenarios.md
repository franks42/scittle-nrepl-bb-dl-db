# Database-Native Clojure Development: Test Scenarios

## Overview

This document outlines comprehensive test scenarios for validating the database-native Clojure development approach. Tests progress from simple proof-of-concept to complex real-world application migration, validating core hypotheses at each level.

## Test Scenario 1: Basic Database Operations via MCP

### Objective
Validate that DataScript + SCI + MCP integration works for basic code storage and retrieval.

### Setup
- Enhanced bb-server with DataScript code database
- Claude Code connected via MCP
- Basic CRUD operations for code forms

### Test Steps
```clojure
;; 1. Create individual forms
(create-form "my.app.users/validate-user" 
  "(defn validate-user [user] (and (map? user) (:email user)))")

(create-form "my.app.users/process-user"
  "(defn process-user [user] 
     (assoc user :processed-at (java.time.Instant/now)))")

;; 2. List forms in namespace
(list-forms "my.app.users")
;; Expected: [["my.app.users/validate-user" "..."] ["my.app.users/process-user" "..."]]

;; 3. Query database for relationships
(d/q '[:find ?fqn ?ns :where [?e :form/fqn ?fqn] [?e :form/namespace ?ns]] @code-db)

;; 4. Update existing form
(update-form "my.app.users/validate-user" 
  "(defn validate-user [user] 
     (and (map? user) (:email user) (string? (:name user))))")

;; 5. Delete form
(delete-form "my.app.users/process-user")
```

### Success Criteria
- ✅ Forms stored and retrieved correctly
- ✅ Database queries return expected results
- ✅ Updates and deletes work atomically
- ✅ Transaction log records all changes
- ✅ Claude Code can perform all operations via MCP

### Validation Points
- DataScript persistence to filesystem
- SCI evaluation of database functions
- MCP communication reliability
- Basic schema effectiveness

## Test Scenario 2: FQN-Based Development with AI

### Objective
Validate that FQN-only development eliminates alias complexity and provides clear context for AI.

### Setup
- Database with forms using only fully qualified names
- No `require` statements or aliases
- AI builds functionality incrementally

### Test Steps
```clojure
;; 1. AI creates namespace and core function
(create-namespace "my.app.orders")
(create-form "my.app.orders/validate-order" 
  "(defn validate-order [order]
     (and (map? order)
          (my.app.users/validate-user (:user order))
          (pos? (:amount order))))")

;; 2. AI can see exact dependencies
(find-dependencies "my.app.orders/validate-order")
;; Expected: ["my.app.users/validate-user"]

;; 3. AI builds dependent functionality
(create-form "my.app.orders/process-order"
  "(defn process-order [order]
     (when (my.app.orders/validate-order order)
       (-> order
           (assoc :status :validated)
           (my.app.payments/charge-order)
           (my.app.notifications/send-confirmation))))")

;; 4. Validate symbol resolution works
(validate-form-symbols "my.app.orders/process-order")
;; Should identify missing: my.app.payments/charge-order, my.app.notifications/send-confirmation
```

### Success Criteria
- ✅ AI can reference functions by FQN without ambiguity
- ✅ Dependency tracking is accurate and complete
- ✅ Missing symbol detection works reliably
- ✅ No namespace alias confusion occurs
- ✅ Code is readable and understandable with FQNs

### Validation Points
- FQN clarity for AI understanding
- Dependency analysis accuracy
- Symbol resolution effectiveness
- Code readability with full qualification

## Test Scenario 3: Auto-Declaration and Forward References

### Objective
Prove that universal auto-declaration solves forward reference problems while maintaining clean user experience.

### Test Steps
```clojure
;; 1. Create functions with forward references
(create-form "my.app.math/fibonacci"
  "(defn fibonacci [n]
     (if (<= n 1)
       n
       (+ (fibonacci (dec n))
          (my.app.math/fibonacci-helper (- n 2)))))")

(create-form "my.app.math/fibonacci-helper"
  "(defn fibonacci-helper [n]
     (if (zero? n) 0 (fibonacci n)))")

;; 2. Create mutual recursion
(create-form "my.app.logic/even?"
  "(defn even? [n]
     (if (zero? n) true (my.app.logic/odd? (dec n))))")

(create-form "my.app.logic/odd?"
  "(defn odd? [n]
     (if (zero? n) false (my.app.logic/even? (dec n))))")

;; 3. Generate namespace file
(def generated-ns (generate-namespace-file "my.app.math"))

;; 4. Validate generated file compiles and runs
(spit "test-ns.clj" generated-ns)
(load-file "test-ns.clj")
(my.app.math/fibonacci 10)  ; Should work without errors
```

### Expected Generated Output
```clojure
(ns my.app.math)

;; Auto-generated declarations
(declare fibonacci fibonacci-helper)

;; Function implementations (any order)
(defn fibonacci [n]
  (if (<= n 1)
    n
    (+ (fibonacci (dec n))
       (my.app.math/fibonacci-helper (- n 2)))))

(defn fibonacci-helper [n]
  (if (zero? n) 0 (fibonacci n)))
```

### Success Criteria
- ✅ Forward references work without manual declares
- ✅ Mutual recursion compiles correctly
- ✅ Generated files are valid Clojure
- ✅ Functions execute correctly after generation
- ✅ User never sees declaration complexity

### Validation Points
- Auto-declaration generation accuracy
- Forward reference resolution
- Circular dependency handling
- Generated code compilation success

## Test Scenario 4: Purity Analysis and Ordering

### Objective
Validate automatic detection of pure vs impure functions and correct ordering of initialization-dependent code.

### Test Steps
```clojure
;; 1. Create mix of pure and impure functions
(create-form "my.app.config/load-env"
  "(def database-url (System/getenv \"DATABASE_URL\"))")

(create-form "my.app.config/parse-config"
  "(defn parse-config [config-str]
     (clojure.edn/read-string config-str))")  ; Pure

(create-form "my.app.db/connect"
  "(def connection (connect-to-db my.app.config/database-url))")  ; Depends on load-env

(create-form "my.app.utils/format-user"
  "(defn format-user [user]
     {:name (:name user) :email (:email user)})")  ; Pure

;; 2. Analyze purity automatically
(analyze-namespace-purity "my.app.config")

;; 3. Generate namespace with correct ordering
(def generated-config (generate-namespace-file "my.app.config"))
```

### Expected Analysis Results
```clojure
{:pure-forms ["my.app.config/parse-config" "my.app.utils/format-user"]
 :impure-forms ["my.app.config/load-env" "my.app.db/connect"]
 :order-dependencies [["my.app.config/load-env" "my.app.db/connect"]]}
```

### Expected Generated Order
```clojure
(ns my.app.config)

(declare load-env parse-config connect format-user)

;; Pure functions (any order)
(defn parse-config [config-str]
  (clojure.edn/read-string config-str))

(defn format-user [user]
  {:name (:name user) :email (:email user)})

;; Impure functions (dependency order)
(def database-url (System/getenv "DATABASE_URL"))

(def connection (connect-to-db my.app.config/database-url))
```

### Success Criteria
- ✅ Pure functions identified correctly
- ✅ Impure functions detected accurately
- ✅ Order dependencies calculated properly
- ✅ Generated file respects initialization order
- ✅ No runtime errors due to ordering issues

### Validation Points
- Purity analysis accuracy
- Dependency order calculation
- Initialization sequence correctness
- Runtime behavior preservation

## Test Scenario 5: Database-Driven Refactoring

### Objective
Demonstrate that refactoring operations become simple database transactions instead of complex text manipulation.

### Test Steps
```clojure
;; 1. Create initial codebase
(create-forms-for-refactoring-test)

;; 2. Rename function with dependency updates
(rename-form-atomic "my.app.users/validate-user" "my.app.users/valid-user?")

;; 3. Verify all references updated
(find-forms-using "my.app.users/valid-user?")
;; Should show all previously dependent forms now use new name

;; 4. Extract function refactoring
(extract-function "my.app.orders/process-order" 
                 "(charge-payment (:payment order))"
                 "my.app.payments/charge-order")

;; 5. Move function between namespaces
(move-form "my.app.utils/format-email" "my.app.email/format")

;; 6. Validate refactored code works
(generate-all-namespaces)
(test-refactored-functionality)
```

### Success Criteria
- ✅ Rename operations are atomic and complete
- ✅ All references updated consistently
- ✅ Extract function preserves behavior
- ✅ Move operations maintain dependencies
- ✅ No broken references after refactoring

### Validation Points
- Atomic refactoring operations
- Reference tracking accuracy
- Behavior preservation
- Dependency graph maintenance

## Test Scenario 6: AI-Driven Incremental Development

### Objective
Test that AI can effectively develop software using statement-level operations and database context.

### Test Steps
```clojure
;; 1. AI explores existing codebase
(list-all-namespaces)
(d/q '[:find ?ns (count ?form) :where [?form :form/namespace ?ns]] @code-db)

;; 2. AI creates new feature incrementally
;; AI: "I'll create a user notification system"
(create-namespace "my.app.notifications")

;; AI: "First, I need email validation from the users module"
(find-forms-matching "email.*valid")

;; AI: "I'll create the core notification function"
(create-form "my.app.notifications/send-email"
  "(defn send-email [user message]
     (when (my.app.users/valid-email? (:email user))
       (my.app.email/send (:email user) message)))")

;; 3. AI discovers missing dependencies
(validate-form "my.app.notifications/send-email")
;; Reports: my.app.email/send does not exist

;; AI: "I'll create the missing email function"
(create-form "my.app.email/send"
  "(defn send [to-address message]
     (println \"Sending to:\" to-address \"Message:\" message))")

;; 4. AI builds on existing functionality
(create-form "my.app.notifications/notify-user"
  "(defn notify-user [user event]
     (let [message (my.app.templates/format-message event)]
       (my.app.notifications/send-email user message)))")
```

### Success Criteria
- ✅ AI can discover existing code structure
- ✅ AI builds functionality incrementally
- ✅ Dependencies are tracked automatically
- ✅ Missing functions identified clearly
- ✅ Code builds coherent, working features

### Validation Points
- AI code discovery effectiveness
- Incremental development patterns
- Dependency awareness
- Context utilization quality

## Test Scenario 7: Complex Application Migration

### Objective
Prove the approach scales to real-world applications by migrating an existing app through the complete database-native workflow.

### Target Application
A Ring-based web application with:
- Configuration loading (env vars, files)
- Database connection management
- Authentication middleware
- User CRUD operations
- API routes and handlers
- Background job processing

### Test Steps

#### Phase 1: Parse and Analyze
```clojure
;; 1. Parse all source files
(def parsed-app (analyze-application "src/my/webapp/"))

;; 2. Extract forms and analyze dependencies
(def analyzed-forms (mapv enhance-form-analysis parsed-app))

;; 3. Identify purity and ordering requirements
(def categorized-forms (categorize-forms analyzed-forms))

;; Results should show:
;; - Pure forms: user data transformation, validation logic
;; - Order-dependent: config loading, db connection, server startup
;; - Dependencies: clear graph of function relationships
```

#### Phase 2: Rewrite with FQNs
```clojure
;; 1. Build namespace mapping
(def ns-mappings (build-namespace-mappings analyzed-forms))

;; 2. Rewrite all forms with FQNs
(def fqn-forms (mapv #(rewrite-with-fqns % ns-mappings) analyzed-forms))

;; 3. Validate no symbols are unresolved
(def validation-results (validate-all-symbols fqn-forms))
```

#### Phase 3: Store in Database
```clojure
;; 1. Populate DataScript database
(store-application-in-db fqn-forms)

;; 2. Verify database structure
(d/q '[:find ?ns (count ?form) 
       :where [?form :form/namespace ?ns]] 
     @code-db)

;; 3. Test complex queries
(find-circular-dependencies @code-db)
(analyze-namespace-complexity @code-db)
```

#### Phase 4: Generate Reconstructed App
```clojure
;; 1. Generate all namespace files
(reconstruct-application @code-db "reconstructed-src/")

;; 2. Verify generated files compile
(compile-clojure-project "reconstructed-src/")

;; 3. Compare structure with original
(compare-project-structure "src/" "reconstructed-src/")
```

#### Phase 5: Test Functional Equivalence
```clojure
;; 1. Start both versions
(def original-server (start-server "src/"))
(def reconstructed-server (start-server "reconstructed-src/"))

;; 2. Run identical test suites
(run-api-tests original-server)
(run-api-tests reconstructed-server)

;; 3. Compare responses
(compare-server-responses original-server reconstructed-server test-requests)

;; 4. Verify database operations
(test-crud-operations original-server)
(test-crud-operations reconstructed-server)
(compare-database-states)
```

### Success Criteria
- ✅ **Complete Parsing**: All source files parsed without errors
- ✅ **Accurate Analysis**: Dependencies and purity detected correctly
- ✅ **FQN Conversion**: All symbols resolved to fully qualified names
- ✅ **Database Storage**: All application semantics captured in DataScript
- ✅ **Clean Generation**: Generated files compile without errors
- ✅ **Functional Equivalence**: Both versions behave identically
- ✅ **Performance Parity**: No significant performance degradation
- ✅ **State Consistency**: Database operations produce same results

### Validation Points
- Real-world complexity handling
- End-to-end workflow completeness
- Performance and scalability
- Production deployment viability

## Test Scenario 8: AI Context and Development Assistance

### Objective
Validate that the database provides rich context for AI development assistance and that AI can work effectively with the database-native approach.

### Test Steps
```clojure
;; 1. AI analyzes codebase structure
(d/q '[:find ?ns ?type (count ?form)
       :where 
       [?form :form/namespace ?ns]
       [?form :form/type ?type]] 
     @code-db)

;; 2. AI finds code patterns
(find-similar-functions @code-db "validate.*")
(find-functions-by-complexity @code-db :high)

;; 3. AI suggests refactoring opportunities
(detect-code-duplication @code-db)
(find-overly-complex-functions @code-db)

;; 4. AI performs guided refactoring
;; AI: "I notice these three functions have similar validation logic"
(extract-common-validation-logic 
  ["my.app.users/validate-user"
   "my.app.orders/validate-order" 
   "my.app.products/validate-product"])

;; 5. AI builds new features using existing patterns
;; AI: "I'll create a notification system following the existing patterns"
(analyze-existing-patterns @code-db "service")
(create-notification-service-following-patterns)
```

### Success Criteria
- ✅ AI can analyze codebase structure effectively
- ✅ Pattern detection identifies useful refactoring opportunities
- ✅ Context-aware suggestions are relevant and helpful
- ✅ AI can perform complex refactoring operations safely
- ✅ New code follows established patterns consistently

### Validation Points
- Database query utility for AI
- Context richness and relevance
- AI development effectiveness
- Pattern consistency maintenance

## Implementation Timeline

### Phase 1: Foundation (Week 1-2)
- Test Scenario 1: Basic Database Operations
- Test Scenario 2: FQN-Based Development
- Validate core MCP + DataScript + SCI integration

### Phase 2: Core Features (Week 3-4)
- Test Scenario 3: Auto-Declaration
- Test Scenario 4: Purity Analysis
- Test Scenario 5: Database Refactoring
- Prove fundamental concepts work

### Phase 3: AI Integration (Week 5-6)
- Test Scenario 6: AI-Driven Development
- Test Scenario 8: AI Context and Assistance
- Validate AI development patterns

### Phase 4: Real-World Validation (Week 7-8)
- Test Scenario 7: Complex Application Migration
- End-to-end workflow with production-scale application
- Performance and scalability assessment

## Success Metrics

### Technical Metrics
- **Round-trip Accuracy**: 100% functional equivalence after migration
- **Performance**: <10% overhead compared to traditional development
- **Reliability**: Zero data loss during database operations
- **Scalability**: Handle applications with 1000+ functions

### Development Experience Metrics
- **AI Effectiveness**: Successful feature development without manual file editing
- **Error Reduction**: Fewer reference errors due to FQN clarity
- **Refactoring Safety**: Zero broken references after refactoring operations
- **Development Speed**: Faster incremental development cycles

### Adoption Readiness Metrics
- **Migration Feasibility**: Existing applications can be migrated automatically
- **Learning Curve**: Developers productive within 1 day
- **Fallback Reliability**: Traditional files can always be generated
- **Tool Integration**: Works with existing Clojure toolchain

This comprehensive test suite validates the database-native approach from basic operations through complex real-world scenarios, ensuring the system is ready for practical adoption.