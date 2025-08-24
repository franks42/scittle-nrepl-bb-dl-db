# Clojure Code Database API Specification

## Overview

This document defines the comprehensive API for a database-native Clojure development system that stores code as structured data in DataScript rather than traditional files. The API covers core CRUD operations, analysis, validation, formatting, and AI-enhanced development features.

## Core CRUD Operations

### Basic Form Operations

```clojure
(defn create-form 
  [fqn :- String, source :- String] 
  :- {:success Boolean, :fqn String, :error String})

(defn get-form 
  [fqn :- String] 
  :- {:fqn String, :source String, :namespace String, :type Keyword, 
      :created-at java.time.Instant, :modified-at java.time.Instant})

(defn update-form 
  [fqn :- String, new-source :- String] 
  :- {:success Boolean, :fqn String, :old-source String, :new-source String})

(defn delete-form 
  [fqn :- String] 
  :- {:success Boolean, :deleted-fqn String})

(defn form-exists? 
  [fqn :- String] 
  :- Boolean)
```

### Listing and Filtering

```clojure
(defn list-all-forms 
  [] 
  :- [{:fqn String, :namespace String, :type Keyword}])

(defn list-forms-by-namespace 
  [namespace :- String] 
  :- [{:fqn String, :source String, :type Keyword}])

(defn list-forms-by-type 
  [type :- Keyword] ; :defn, :def, :defprotocol, etc.
  :- [{:fqn String, :namespace String, :source String}])

(defn search-forms 
  [pattern :- String, options :- {:regex? Boolean, :case-sensitive? Boolean}] 
  :- [{:fqn String, :source String, :matches [String]}])
```

## Namespace Operations

```clojure
(defn create-namespace 
  [namespace-name :- String, description :- String] 
  :- {:success Boolean, :namespace String})

(defn list-namespaces 
  [] 
  :- [{:namespace String, :form-count Integer, :description String}])

(defn get-namespace-forms 
  [namespace :- String] 
  :- [{:fqn String, :source String, :type Keyword, :dependencies [String]}])

(defn delete-namespace 
  [namespace :- String] 
  :- {:success Boolean, :deleted-namespace String, :deleted-forms [String]})

(defn namespace-exists? 
  [namespace :- String] 
  :- Boolean)
```

## Dependency Analysis

```clojure
(defn get-dependencies 
  [fqn :- String] 
  :- {:fqn String, :dependencies [String], :dependency-types {String Keyword}})

(defn get-dependents 
  [fqn :- String] 
  :- {:fqn String, :dependents [String], :dependent-types {String Keyword}})

(defn find-all-references 
  [fqn :- String] 
  :- [{:fqn String, :source String, :reference-locations [Integer]}])

(defn get-dependency-graph 
  [namespace :- String] ; optional, nil for all
  :- {:nodes [{:fqn String, :type Keyword}], 
      :edges [{:from String, :to String, :type Keyword}]})

(defn find-circular-dependencies 
  [] 
  :- [{:cycle [String], :cycle-type Keyword}])

(defn get-transitive-dependencies 
  [fqn :- String, max-depth :- Integer] 
  :- {:fqn String, :transitive-deps [String], :depth-map {String Integer}})
```

## Search and Query

```clojure
(defn find-forms-containing 
  [text :- String, options :- {:namespace String, :type Keyword}] 
  :- [{:fqn String, :source String, :match-count Integer}])

(defn find-forms-by-complexity 
  [min-complexity :- Integer, max-complexity :- Integer] 
  :- [{:fqn String, :complexity-score Integer, :namespace String}])

(defn find-pure-functions 
  [namespace :- String] ; optional
  :- [{:fqn String, :source String, :purity-confidence Double}])

(defn find-impure-functions 
  [namespace :- String] ; optional
  :- [{:fqn String, :source String, :side-effects [Keyword]}])

(defn find-unused-forms 
  [] 
  :- [{:fqn String, :namespace String, :type Keyword}])

(defn find-similar-forms 
  [fqn :- String, similarity-threshold :- Double] 
  :- [{:fqn String, :similarity-score Double, :similar-aspects [Keyword]}])

(defn query-forms 
  [query-map :- {:namespace String, :type Keyword, :complexity-range [Integer Integer], 
                 :pure? Boolean, :has-dependencies Boolean}] 
  :- [{:fqn String, :source String, :metadata {}}])
```

## Form Analysis and Parsing

```clojure
(defn parse-top-level-form 
  [source :- String] 
  :- {:form-type Keyword, ; :defn, :def, :defprotocol, :defrecord, :comment
      :name String, 
      :fqn String,
      :parameters [String], ; for functions
      :docstring String,
      :metadata {},
      :body-forms [Any],
      :parse-errors [String],
      :line-spans {:start Integer, :end Integer}})

(defn extract-form-metadata 
  [parsed-form :- Any] 
  :- {:complexity-score Integer,
      :cyclomatic-complexity Integer,
      :cognitive-complexity Integer,
      :line-count Integer,
      :parameter-count Integer,
      :nesting-depth Integer,
      :macro-usage [String],
      :special-forms [Keyword]})

(defn analyze-form-dependencies 
  [source :- String, namespace-context :- String] 
  :- {:explicit-dependencies [String], ; fully qualified names found
      :implicit-dependencies [String], ; unqualified symbols that need resolution
      :macro-dependencies [String],
      :java-imports [String],
      :unresolved-symbols [String],
      :dependency-types {String Keyword}}) ; :function-call, :var-reference, :macro-call

(defn analyze-form-purity 
  [source :- String, dependency-purity-map :- {String Boolean}] 
  :- {:pure? Boolean,
      :confidence Double, ; 0.0 to 1.0
      :impure-indicators [Keyword], ; :io-operation, :state-mutation, :side-effect
      :impure-function-calls [String],
      :analysis-warnings [String]})

(defn extract-local-bindings 
  [source :- String] 
  :- {:let-bindings [{:name String, :scope-start Integer, :scope-end Integer}],
      :parameter-bindings [String],
      :loop-bindings [String],
      :for-bindings [String],
      :binding-conflicts [String]})
```

## Code Analysis Operations

```clojure
(defn analyze-form 
  [fqn :- String] 
  :- {:fqn String, :complexity-score Integer, :line-count Integer, 
      :dependencies [String], :pure? Boolean, :side-effects [Keyword],
      :syntax-valid? Boolean, :validation-errors [String]})

(defn get-complexity-metrics 
  [fqn :- String] 
  :- {:fqn String, :cyclomatic-complexity Integer, :cognitive-complexity Integer,
      :nesting-depth Integer, :parameter-count Integer})

(defn get-purity-info 
  [fqn :- String] 
  :- {:fqn String, :pure? Boolean, :confidence Double, 
      :impure-calls [String], :side-effect-types [Keyword]})

(defn check-symbol-resolution 
  [fqn :- String] 
  :- {:fqn String, :all-resolved? Boolean, 
      :unresolved-symbols [String], :available-symbols [String]})
```

## Real-Time Validation

```clojure
(defn validate-syntax-incremental 
  [partial-source :- String, cursor-position :- Integer] 
  :- {:valid? Boolean,
      :syntax-state Keyword, ; :valid, :incomplete, :invalid, :recoverable
      :errors [{:line Integer, :column Integer, :message String, :severity Keyword}],
      :warnings [String],
      :completion-suggestions [String],
      :balanced-delimiters? Boolean,
      :expected-tokens [String]}) ; what would make it valid

(defn check-paren-balance 
  [source :- String, cursor-position :- Integer] 
  :- {:balanced? Boolean,
      :open-parens Integer,
      :open-brackets Integer,
      :open-braces Integer,
      :unmatched-positions [Integer],
      :auto-close-suggestion String})

(defn analyze-partial-form 
  [source :- String, cursor-position :- Integer] 
  :- {:form-structure {:type Keyword, :complete? Boolean},
      :current-context Keyword, ; :function-name, :parameter-list, :body, :docstring
      :local-scope [String],
      :expected-next [Keyword], ; :parameter, :body-expression, :closing-paren
      :syntax-hints [String]})

(defn validate-symbol-at-cursor 
  [source :- String, cursor-position :- Integer, namespace-context :- String] 
  :- {:symbol String,
      :valid? Boolean,
      :resolution-status Keyword, ; :resolved, :unresolved, :ambiguous, :partial
      :suggestions [String],
      :completion-options [{:symbol String, :type Keyword, :documentation String}]})
```

## Edit History and Change Tracking

```clojure
(defn record-form-edit 
  [fqn :- String, old-source :- String, new-source :- String, 
   editor :- String, edit-type :- Keyword] 
  :- {:edit-id String,
      :timestamp java.time.Instant,
      :fqn String,
      :edit-type Keyword, ; :create, :update, :delete, :rename, :refactor
      :diff {:added-lines [String], :removed-lines [String], :changed-lines [String]},
      :size-change {:line-delta Integer, :complexity-delta Integer}})

(defn get-edit-history 
  [fqn :- String, limit :- Integer, from-timestamp :- java.time.Instant] 
  :- [{:edit-id String,
       :timestamp java.time.Instant,
       :editor String,
       :edit-type Keyword,
       :old-source String,
       :new-source String,
       :diff {},
       :metadata {}}])

(defn analyze-edit-impact 
  [fqn :- String, old-source :- String, new-source :- String] 
  :- {:complexity-change Integer,
      :dependency-changes {:added [String], :removed [String]},
      :breaking-changes [String],
      :affected-dependents [String],
      :semantic-changes [Keyword], ; :signature-change, :behavior-change, :performance-impact
      :risk-assessment Keyword}) ; :low, :medium, :high

(defn get-change-statistics 
  [timeframe :- {:start java.time.Instant, :end java.time.Instant},
   filters :- {:namespace String, :editor String, :edit-type Keyword}] 
  :- {:total-edits Integer,
      :forms-modified Integer,
      :lines-added Integer,
      :lines-removed Integer,
      :complexity-trend Double,
      :most-edited-forms [String],
      :edit-frequency-by-hour {Integer Integer}})
```

## Error Detection and Reporting

```clojure
(defn validate-form-syntax 
  [source :- String] 
  :- {:valid? Boolean,
      :syntax-errors [{:line Integer, :column Integer, :message String, :severity Keyword}],
      :warnings [{:line Integer, :column Integer, :message String, :type Keyword}],
      :parsed-successfully? Boolean})

(defn detect-semantic-errors 
  [fqn :- String] 
  :- {:arity-mismatches [{:function String, :expected-arity [Integer], :actual-arity Integer}],
      :type-conflicts [{:symbol String, :expected-type String, :actual-type String}],
      :dead-code-segments [{:description String, :line-range [Integer Integer]}],
      :infinite-recursion-risk [{:function String, :confidence Double}],
      :performance-issues [{:type Keyword, :description String, :suggestion String}]})

(defn analyze-code-quality 
  [fqn :- String] 
  :- {:quality-score Double, ; 0.0 to 1.0
      :complexity-issues [{:type Keyword, :severity Keyword, :suggestion String}],
      :maintainability-score Double,
      :readability-score Double,
      :issues [{:category Keyword, :description String, :fix-suggestion String}],
      :best-practices-violations [String]})

(defn check-namespace-consistency 
  [namespace :- String] 
  :- {:consistent? Boolean,
      :naming-violations [{:fqn String, :issue String}],
      :dependency-cycles [String],
      :unused-requires [String],
      :missing-docstrings [String],
      :inconsistent-patterns [{:pattern String, :violations [String]}]})
```

## Refactoring Support

```clojure
(defn rename-form 
  [old-fqn :- String, new-fqn :- String] 
  :- {:success Boolean, :old-fqn String, :new-fqn String, 
      :updated-references [String], :transaction-id Any})

(defn move-form 
  [fqn :- String, target-namespace :- String] 
  :- {:success Boolean, :old-fqn String, :new-fqn String, 
      :updated-references [String]})

(defn find-rename-impact 
  [old-fqn :- String, new-fqn :- String] 
  :- {:affected-forms [String], :breaking-changes [String], 
      :safe-rename? Boolean, :warnings [String]})

(defn update-references 
  [old-fqn :- String, new-fqn :- String] 
  :- {:updated-forms [String], :update-count Integer})

(defn analyze_refactoring_safety 
  [fqn :- String, proposed-changes :- String] 
  :- {:safety-assessment Keyword, ; :safe, :risky, :dangerous
      :risk-factors [String],
      :required-validations [String],
      :suggested-test-approach String,
      :rollback-strategy String,
      :impact-scope {:affected-forms [String], :affected-namespaces [String]}})
```

## AI-Powered Suggestions and Analysis

```clojure
(defn suggest-improvements 
  [fqn :- String, analysis-context :- {}] 
  :- {:refactoring-suggestions [{:type Keyword, ; :extract-function, :simplify-logic, :reduce-complexity
                                :description String,
                                :confidence Double,
                                :impact-assessment Keyword,
                                :before-code String,
                                :after-code String}],
      :performance-optimizations [{:optimization-type Keyword,
                                   :expected-improvement String,
                                   :code-changes String}],
      :style-improvements [{:style-issue String, :suggested-fix String}]})

(defn analyze-complexity-hotspots 
  [fqn :- String] 
  :- {:complexity-score Integer,
      :hotspots [{:location {:line-start Integer, :line-end Integer},
                  :complexity-contribution Integer,
                  :simplification-suggestions [String]}],
      :refactoring-opportunities [{:type Keyword,
                                   :description String,
                                   :complexity-reduction Integer}]})

(defn suggest-decomposition 
  [fqn :- String, max-complexity :- Integer] 
  :- {:should-decompose? Boolean,
      :suggested-extractions [{:function-name String,
                               :extracted-code String,
                               :parameters [String],
                               :return-type String,
                               :complexity-reduction Integer}],
      :remaining-complexity Integer,
      :decomposition-strategy Keyword}) ; :by-concern, :by-complexity, :by-dependencies

(defn detect-code-patterns 
  [fqn :- String] 
  :- {:detected-patterns [{:pattern-name String,
                           :confidence Double,
                           :pattern-type Keyword, ; :design-pattern, :anti-pattern, :idiom
                           :location {:line-start Integer, :line-end Integer},
                           :improvements [String]}],
      :anti-patterns [{:pattern-name String,
                       :severity Keyword,
                       :fix-suggestions [String]}]})

(defn suggest-similar-implementations 
  [description :- String, context :- {:namespace String, :available-functions [String]}] 
  :- {:suggestions [{:fqn String,
                     :similarity-score Double,
                     :implementation String,
                     :adaptation-needed [String]}],
      :pattern-matches [{:pattern-type String,
                         :example-code String,
                         :customization-points [String]}]})
```

## AI Context Generation

```clojure
(defn get-ai-context 
  [fqn :- String] 
  :- {:current-form {:fqn String, :source String, :type Keyword},
      :dependencies [{:fqn String, :source String}],
      :dependents [String],
      :namespace-context [String],
      :similar-patterns [String],
      :available-symbols [String],
      :complexity-analysis {},
      :refactoring-suggestions [String]})

(defn get-namespace-context 
  [namespace :- String] 
  :- {:namespace String, :forms [String], :dependencies [String],
      :external-dependencies [String], :complexity-summary {},
      :purity-analysis {}})

(defn get-editing-context 
  [fqn :- String, cursor-position :- Integer] 
  :- {:current-form {:fqn String, :source String},
      :local-bindings [String], :available-functions [String],
      :suggested-completions [String], :syntax-context Keyword})

(defn find-patterns-like 
  [fqn :- String, pattern-types :- [Keyword]] 
  :- [{:fqn String, :pattern-type Keyword, :similarity-score Double,
       :pattern-description String}])

(defn get-form-analysis-context 
  [fqn :- String] 
  :- {:current-form {:source String, :metadata {}},
      :usage-patterns [{:pattern String, :frequency Integer}],
      :caller-analysis {:frequent-callers [String], :usage-contexts [String]},
      :similar-functions [{:fqn String, :similarity-aspects [Keyword]}],
      :domain-context {:business-logic-type Keyword, :data-flow-role Keyword},
      :historical-context {:creation-date java.time.Instant, :evolution-summary String}})
```

## Form Comparison and Diff Analysis

```clojure
(defn compare-forms 
  [fqn1 :- String, fqn2 :- String] 
  :- {:structural-similarity Double,
      :semantic-similarity Double,
      :differences [{:type Keyword, ; :parameter-diff, :logic-diff, :complexity-diff
                     :description String,
                     :location1 String,
                     :location2 String}],
      :common-patterns [String],
      :unique-aspects {:form1 [String], :form2 [String]}})

(defn generate-form-diff 
  [old-source :- String, new-source :- String] 
  :- {:unified-diff String,
      :structured-diff [{:operation Keyword, ; :add, :remove, :change
                         :line-number Integer,
                         :old-content String,
                         :new-content String}],
      :semantic-changes [{:type Keyword,
                          :description String,
                          :impact-level Keyword}]})

(defn analyze-evolution 
  [fqn :- String, timeframe :- {:start java.time.Instant, :end java.time.Instant}] 
  :- {:evolution-summary {:total-changes Integer,
                          :complexity-trend [Integer],
                          :size-trend [Integer]},
      :major-changes [{:timestamp java.time.Instant,
                       :change-type Keyword,
                       :description String}],
      :stability-score Double,
      :refactoring-history [String]})
```

## Code Formatting

```clojure
(defn format-form 
  [source :- String, formatting-preferences :- {}] 
  :- String)

(defn get-formatting-preferences 
  [fqn :- String, user-id :- String, project-id :- String] 
  :- {:formatting-standard Keyword, ; :cljfmt-default, :cursive, :community, :google
      :custom-rules {},
      :engine Keyword, ; :cljfmt, :zprint
      :resolved-from Keyword}) ; :user, :project, :namespace, :form

(defn validate-formatting-config 
  [config :- {}] 
  :- {:valid? Boolean, :errors [String], :normalized-config {}})

(defn preview-formatting 
  [source :- String, old-preferences :- {}, new-preferences :- {}] 
  :- {:before String, :after String, :diff String})

(defn detect-manual-formatting-attempt 
  [old-source :- String, new-source :- String] 
  :- {:manual-formatting-detected? Boolean,
      :formatting-type Keyword,
      :suggestion String})

(defn apply-formatting-standard 
  [source :- String, standard :- Keyword, custom-overrides :- {}] 
  :- {:formatted-source String,
      :applied-rules [String],
      :formatting-engine Keyword})
```

## Linting Integration

```clojure
(defn run-clj-kondo-analysis 
  [source :- String, kondo-config :- {}] 
  :- {:findings [{:type Keyword, :level Keyword, :message String, 
                  :row Integer, :col Integer, :filename String}],
      :summary {:error Integer, :warning Integer, :info Integer}})

(defn enhance-kondo-with-db-context 
  [kondo-findings :- [{}], db-context :- {}] 
  :- [{:kondo-finding {}, :db-enhancement {}, :combined-suggestion String}])

(defn get-comprehensive-analysis 
  [fqn :- String] 
  :- {:db-analysis {},        ; Our custom analysis
      :kondo-analysis {},     ; clj-kondo linting
      :combined-suggestions [{}],
      :priority-issues [{}]})
```

## Database Management

```clojure
(defn get-db-stats 
  [] 
  :- {:total-forms Integer, :namespaces Integer, :total-lines Integer,
      :average-complexity Double, :pure-function-ratio Double})

(defn backup-db 
  [backup-name :- String] 
  :- {:success Boolean, :backup-name String, :backup-path String, 
      :timestamp java.time.Instant})

(defn restore-db 
  [backup-name :- String] 
  :- {:success Boolean, :restored-forms Integer, :backup-timestamp java.time.Instant})

(defn optimize-db 
  [] 
  :- {:success Boolean, :operations-performed [String], :performance-gain String})

(defn clear-db 
  [] 
  :- {:success Boolean, :deleted-forms Integer, :deleted-namespaces Integer})
```

## Transaction History

```clojure
(defn get-recent-changes 
  [limit :- Integer] 
  :- [{:transaction-id Any, :timestamp java.time.Instant, 
       :operation Keyword, :affected-forms [String]}])

(defn get-form-history 
  [fqn :- String, limit :- Integer] 
  :- [{:fqn String, :source String, :timestamp java.time.Instant, 
       :operation Keyword, :transaction-id Any}])

(defn undo-last-transaction 
  [] 
  :- {:success Boolean, :undone-transaction-id Any, :affected-forms [String]})

(defn get-transaction-log 
  [from-timestamp :- java.time.Instant, to-timestamp :- java.time.Instant] 
  :- [{:transaction-id Any, :timestamp java.time.Instant, 
       :tx-data [Any], :operation-summary String}])
```

## Import/Export

```clojure
(defn import-from-file 
  [file-path :- String, namespace :- String] 
  :- {:success Boolean, :imported-forms [String], :errors [String], 
      :namespace String})

(defn export-to-file 
  [namespace :- String, file-path :- String, options :- {:include-declares? Boolean}] 
  :- {:success Boolean, :exported-forms [String], :file-path String})

(defn export-all 
  [output-dir :- String, options :- {:format Keyword}] ; :traditional, :database-native
  :- {:success Boolean, :exported-namespaces [String], :file-count Integer})

(defn parse-clojure-source 
  [source-text :- String] 
  :- {:forms [{:fqn String, :source String, :type Keyword}], 
      :namespace String, :parse-errors [String]})
```

## Key Design Principles

### 1. **Database-Native Storage**
- Code stored as structured data in DataScript
- Canonical representation without formatting information
- Statement-level granularity for precise operations

### 2. **FQN-First Development**
- Fully qualified names eliminate require statements
- Unambiguous symbol resolution
- Location-independent code references

### 3. **AI Integration Ready**
- Rich context generation for AI analysis
- Structured data enables sophisticated code understanding
- Form-level operations suitable for AI manipulation

### 4. **Real-Time Validation**
- Keystroke-level syntax checking
- Progressive error recovery
- Immediate feedback on symbol resolution

### 5. **Formatting as Presentation**
- Formatting preferences stored separately from code
- Multiple formatting standards supported (cljfmt, zprint)
- User/project/namespace-level preference hierarchy

### 6. **Enhanced Analysis**
- Integration with clj-kondo for comprehensive linting
- Custom analysis for purity, complexity, and dependencies
- AI-powered suggestions for improvements

### 7. **Safe Refactoring**
- Atomic database transactions for refactoring operations
- Impact analysis before changes
- Complete audit trail of modifications

This API specification provides the foundation for building a comprehensive database-native Clojure development environment that combines the benefits of structured code storage with modern development tooling and AI assistance.
