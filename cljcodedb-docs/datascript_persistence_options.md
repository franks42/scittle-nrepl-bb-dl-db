# DataScript Persistence Options Across Clojure Runtimes

## Overview

DataScript is designed as an in-memory database, but there are several strategies for persistence across different Clojure environments. This document explores options for backing up and restoring DataScript databases in JVM Clojure, ClojureScript, SCI, and Babashka.

## Core DataScript Persistence Concepts

### 1. **Serialization Approaches**
DataScript databases can be persisted through several serialization methods:

```clojure
;; 1. Transaction Log Replay
(def tx-log (d/tx-range db nil nil))
(spit "db-transactions.edn" tx-log)

;; 2. Database Snapshot (EDN)
(def db-data (d/db->data db))
(spit "db-snapshot.edn" db-data)

;; 3. Entity Export
(def all-entities (d/q '[:find (pull ?e [*]) :where [?e]] db))
(spit "db-entities.edn" all-entities)
```

### 2. **Restoration Methods**
```clojure
;; From transaction log
(def restored-db 
  (reduce d/db-with empty-db (read-string (slurp "db-transactions.edn"))))

;; From snapshot
(def restored-db (d/conn-from-data (read-string (slurp "db-snapshot.edn"))))

;; From entities
(def restored-db 
  (d/db-with empty-db (read-string (slurp "db-entities.edn"))))
```

## JVM Clojure Options

### 1. **File-Based Persistence**
```clojure
(ns my.persistence
  (:require [datascript.core :as d]
            [clojure.edn :as edn]))

;; Simple file backup
(defn backup-db [conn filepath]
  (let [db-data (d/db->data @conn)]
    (spit filepath (pr-str db-data))
    filepath))

(defn restore-db [filepath schema]
  (let [data (edn/read-string (slurp filepath))
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data data))
    conn))

;; Usage
(def schema {:statement/fqn {:db/unique :db.unique/identity}
             :statement/source {}
             :statement/namespace {}})

(def conn (d/create-conn schema))
(backup-db conn "code-db.edn")
(def restored-conn (restore-db "code-db.edn" schema))
```

### 2. **PostgreSQL Integration**
```clojure
(ns my.postgres-persistence
  (:require [datascript.core :as d]
            [next.jdbc :as jdbc]
            [clojure.data.json :as json]))

(defn save-db-to-postgres [conn db-spec table-name]
  (let [db-data (d/db->data @conn)
        json-data (json/write-str db-data)]
    (jdbc/execute! db-spec
      [(str "INSERT INTO " table-name " (data, created_at) VALUES (?, NOW())")
       json-data])))

(defn load-db-from-postgres [db-spec table-name schema]
  (let [result (jdbc/execute-one! db-spec 
                 [(str "SELECT data FROM " table-name " ORDER BY created_at DESC LIMIT 1")])
        db-data (json/read-str (:data result) :key-fn keyword)
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data db-data))
    conn))
```

### 3. **SQLite Integration**
```clojure
(ns my.sqlite-persistence
  (:require [datascript.core :as d]
            [next.jdbc :as jdbc]))

(def sqlite-db {:dbtype "sqlite" :dbname "datascript-backup.db"})

(defn init-sqlite-storage []
  (jdbc/execute! sqlite-db
    ["CREATE TABLE IF NOT EXISTS datascript_snapshots (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT UNIQUE,
        data TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )"]))

(defn save-snapshot [conn snapshot-name]
  (let [db-data (pr-str (d/db->data @conn))]
    (jdbc/execute! sqlite-db
      ["INSERT OR REPLACE INTO datascript_snapshots (name, data) VALUES (?, ?)"
       snapshot-name db-data])))

(defn load-snapshot [snapshot-name schema]
  (let [result (jdbc/execute-one! sqlite-db
                 ["SELECT data FROM datascript_snapshots WHERE name = ?" snapshot-name])]
    (when result
      (let [data (read-string (:data result))
            conn (d/create-conn schema)]
        (reset! conn (d/conn-from-data data))
        conn))))
```

## ClojureScript Options

### 1. **LocalStorage Persistence**
```clojure
(ns my.browser-persistence
  (:require [datascript.core :as d]
            [cljs.reader :as reader]))

(defn save-db-to-localstorage [conn key]
  (let [db-data (d/db->data @conn)
        serialized (pr-str db-data)]
    (.setItem js/localStorage key serialized)))

(defn load-db-from-localstorage [key schema]
  (when-let [data (.getItem js/localStorage key)]
    (let [parsed-data (reader/read-string data)
          conn (d/create-conn schema)]
      (reset! conn (d/conn-from-data parsed-data))
      conn)))

;; Auto-save on changes
(defn setup-auto-save [conn storage-key]
  (d/listen! conn :auto-save
    (fn [tx-report]
      (save-db-to-localstorage conn storage-key))))
```

### 2. **IndexedDB Persistence**
```clojure
(ns my.indexeddb-persistence
  (:require [datascript.core :as d]))

;; Using a library like cljs-idb
(defn save-db-to-indexeddb [conn db-name]
  (let [db-data (d/db->data @conn)]
    (-> (idb/open-db "datascript-storage" 1)
        (.then (fn [db]
                 (idb/put! db "snapshots" {:name db-name 
                                          :data db-data
                                          :timestamp (js/Date.now)}))))))

(defn load-db-from-indexeddb [db-name schema]
  (-> (idb/open-db "datascript-storage" 1)
      (.then (fn [db]
               (idb/get db "snapshots" db-name)))
      (.then (fn [result]
               (when result
                 (let [conn (d/create-conn schema)]
                   (reset! conn (d/conn-from-data (:data result)))
                   conn))))))
```

### 3. **Server Synchronization**
```clojure
(ns my.sync-persistence
  (:require [datascript.core :as d]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn sync-to-server [conn endpoint]
  (go
    (let [db-data (d/db->data @conn)
          response (<! (http/post endpoint 
                         {:json-params {:db-data db-data}}))]
      (if (= 200 (:status response))
        :success
        :error))))

(defn sync-from-server [endpoint schema]
  (go
    (let [response (<! (http/get endpoint))]
      (when (= 200 (:status response))
        (let [db-data (get-in response [:body :db-data])
              conn (d/create-conn schema)]
          (reset! conn (d/conn-from-data db-data))
          conn)))))
```

## SCI (Small Clojure Interpreter) Options

### 1. **File-Based (Same as JVM)**
```clojure
;; SCI can use the same file-based approaches as JVM Clojure
(defn sci-backup-db [conn filepath]
  (let [db-data (d/db->data @conn)]
    (spit filepath (pr-str db-data))))

(defn sci-restore-db [filepath schema]
  (let [data (read-string (slurp filepath))
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data data))
    conn))
```

### 2. **SCI Context Integration**
```clojure
(ns my.sci-persistence
  (:require [sci.core :as sci]
            [datascript.core :as d]))

;; Embed DataScript in SCI context
(def sci-ctx 
  (sci/init {:namespaces {'datascript.core d/api
                         'my.persistence {'save-db sci-backup-db
                                        'load-db sci-restore-db}}}))

;; SCI code can now persist DataScript
(sci/eval-string sci-ctx 
  "(let [conn (d/create-conn {})]
     (d/transact! conn [{:name \"test\"}])
     (my.persistence/save-db conn \"test.edn\"))")
```

## Babashka Options

### 1. **Native File Operations**
```clojure
(ns my.bb-persistence
  (:require [datascript.core :as d]
            [babashka.fs :as fs]
            [clojure.edn :as edn]))

(defn bb-backup-with-versioning [conn base-path]
  (let [timestamp (System/currentTimeMillis)
        filename (str base-path "-" timestamp ".edn")
        db-data (d/db->data @conn)]
    (spit filename (pr-str db-data))
    
    ;; Keep only last 10 backups
    (let [backups (sort (filter #(str/starts-with? % base-path) 
                               (map str (fs/list-dir (fs/parent base-path)))))]
      (doseq [old-backup (drop 10 (reverse backups))]
        (fs/delete old-backup)))
    
    filename))

(defn bb-restore-latest [base-path schema]
  (let [backups (sort (filter #(str/starts-with? % base-path)
                             (map str (fs/list-dir (fs/parent base-path)))))
        latest (last backups)]
    (when latest
      (let [data (edn/read-string (slurp latest))
            conn (d/create-conn schema)]
        (reset! conn (d/conn-from-data data))
        conn))))
```

### 2. **Integration with External Databases**
```clojure
(ns my.bb-external-db
  (:require [datascript.core :as d]
            [babashka.curl :as curl]
            [cheshire.core :as json]))

;; Save to external REST API
(defn bb-save-to-api [conn api-endpoint]
  (let [db-data (d/db->data @conn)
        json-data (json/generate-string db-data)]
    (curl/post api-endpoint 
               {:headers {"Content-Type" "application/json"}
                :body json-data})))

;; Load from external REST API  
(defn bb-load-from-api [api-endpoint schema]
  (let [response (curl/get api-endpoint)
        db-data (json/parse-string (:body response) true)
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data db-data))
    conn))
```

## Advanced Persistence Patterns

### 1. **Incremental Backup (Transaction Log)**
```clojure
(ns my.incremental-backup
  (:require [datascript.core :as d]))

(defonce tx-log (atom []))

(defn setup-tx-logging [conn]
  (d/listen! conn :tx-logger
    (fn [tx-report]
      (swap! tx-log conj (:tx-data tx-report)))))

(defn save-incremental-backup [filepath]
  (spit filepath (pr-str @tx-log)))

(defn restore-from-incremental [filepath base-db]
  (let [transactions (read-string (slurp filepath))]
    (reduce (fn [db tx-data]
              (d/db-with db tx-data))
            base-db
            transactions)))
```

### 2. **Compressed Persistence**
```clojure
(ns my.compressed-persistence
  (:require [datascript.core :as d]
            [clojure.java.io :as io])
  (:import [java.util.zip GZIPOutputStream GZIPInputStream]))

(defn save-compressed [conn filepath]
  (let [db-data (pr-str (d/db->data @conn))]
    (with-open [fos (io/output-stream filepath)
                gzos (GZIPOutputStream. fos)]
      (.write gzos (.getBytes db-data "UTF-8")))))

(defn load-compressed [filepath schema]
  (let [data (with-open [fis (io/input-stream filepath)
                        gzis (GZIPInputStream. fis)]
               (slurp gzis))
        parsed-data (read-string data)
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data parsed-data))
    conn))
```

### 3. **Multi-Backend Persistence**
```clojure
(ns my.multi-backend
  (:require [datascript.core :as d]))

(defprotocol PersistenceBackend
  (save [this conn key])
  (load [this key schema]))

(defrecord FileBackend [base-path]
  PersistenceBackend
  (save [this conn key]
    (let [filepath (str base-path "/" key ".edn")
          db-data (d/db->data @conn)]
      (spit filepath (pr-str db-data))))
  (load [this key schema]
    (let [filepath (str base-path "/" key ".edn")]
      (when (fs/exists? filepath)
        (let [data (read-string (slurp filepath))
              conn (d/create-conn schema)]
          (reset! conn (d/conn-from-data data))
          conn)))))

(defrecord SQLiteBackend [db-spec]
  PersistenceBackend
  (save [this conn key]
    ;; SQLite implementation
    )
  (load [this key schema]
    ;; SQLite implementation
    ))

;; Usage
(def backends {:file (->FileBackend "/data/backups")
               :sqlite (->SQLiteBackend sqlite-config)})

(defn save-to-all-backends [conn key]
  (doseq [[name backend] backends]
    (try
      (save backend conn key)
      (println "Saved to" name)
      (catch Exception e
        (println "Failed to save to" name ":" (.getMessage e))))))
```

## Recommendations by Use Case

### **For Your Clojure Code Storage Project:**

#### **Development Environment (bb-server)**
```clojure
;; Primary: File-based with versioning
;; Backup: SQLite for structured queries
;; Sync: Optional PostgreSQL for team sharing

(def persistence-config
  {:primary {:type :file
            :path "data/code-db.edn"
            :backup-count 10}
   :backup {:type :sqlite
           :path "data/code-db.sqlite"}
   :sync {:type :postgres
         :url "postgresql://localhost/codebase"}})
```

#### **Browser Client (ClojureScript)**
```clojure
;; Primary: LocalStorage for immediate persistence
;; Backup: Sync to bb-server periodically
;; Offline: IndexedDB for larger datasets

(def browser-persistence
  {:immediate :localstorage
   :sync-interval 30000  ; 30 seconds
   :offline-storage :indexeddb})
```

#### **Runtime Integration (SCI)**
```clojure
;; Same file-based approach as bb-server
;; Shared storage for consistency
```

This gives you a robust, multi-tier persistence strategy that works across all your target environments while maintaining consistency and providing fallback options.

## Performance Considerations

### **File Size Management**
- **Snapshots**: Full database dumps - larger but complete
- **Transaction logs**: Incremental - smaller but need base snapshot
- **Compression**: Can reduce size by 60-80% for text-heavy data

### **Read/Write Performance**
- **Memory**: DataScript operations are always in-memory (fast)
- **Persistence**: Write operations are asynchronous background tasks
- **Restoration**: Only happens on startup or explicit restore

### **Scalability**
- **Single user**: File-based persistence is sufficient
- **Team development**: PostgreSQL/SQLite for shared state
- **Large codebases**: Consider partitioning by namespace

# DataScript for Clojure Code Storage: Analysis and Persistence Options

## Executive Summary

This document analyzes DataScript vs SQL for storing structured Clojure code and provides comprehensive persistence strategies across different runtime environments. **DataScript is the clear winner** for database-native Clojure development due to its natural handling of code dependencies, Clojure-native integration, and perfect scale match.

## Part I: DataScript vs SQL Analysis

### Core Use Case Requirements

We're storing **structured Clojure code** with these key requirements:
- Statement-level granularity (individual `defn`, `def`, `defprotocol`)
- Dependency tracking between statements
- Namespace organization
- FQN-based references
- AI context queries
- Refactoring operations
- Version history

### Data Model Comparison

#### DataScript Schema
```clojure
(def schema
  {:statement/fqn {:db/unique :db.unique/identity}
   :statement/source {}
   :statement/namespace {}
   :statement/name {}
   :statement/type {}  ; :defn, :def, :defprotocol
   :statement/dependencies {:db/cardinality :db.cardinality/many}
   :statement/pure? {}
   :statement/line-count {}
   :statement/created-at {}
   :statement/modified-at {}
   
   :namespace/name {:db/unique :db.unique/identity}
   :namespace/description {}
   :namespace/statements {:db/cardinality :db.cardinality/many
                         :db/valueType :db.type/ref}
   
   :dependency/from {:db/valueType :db.type/ref}
   :dependency/to {:db/valueType :db.type/ref}
   :dependency/type {}})  ; :function-call, :var-reference, :protocol-impl
```

#### SQL Schema
```sql
-- Statements table
CREATE TABLE statements (
    id SERIAL PRIMARY KEY,
    fqn VARCHAR(255) UNIQUE NOT NULL,
    source TEXT NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- 'defn', 'def', 'defprotocol'
    pure BOOLEAN DEFAULT NULL,
    line_count INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    modified_at TIMESTAMP DEFAULT NOW()
);

-- Dependencies table (many-to-many)
CREATE TABLE dependencies (
    id SERIAL PRIMARY KEY,
    from_statement_id INTEGER REFERENCES statements(id),
    to_statement_id INTEGER REFERENCES statements(id),
    dependency_type VARCHAR(50) NOT NULL
);
```

### Query Comparison

#### 1. Find Dependencies of a Function

**DataScript (Winner):**
```clojure
(d/q '[:find ?dep-fqn ?dep-source
       :in $ ?fqn
       :where
       [?stmt :statement/fqn ?fqn]
       [?stmt :statement/dependencies ?dep-fqn]
       [?dep :statement/fqn ?dep-fqn]
       [?dep :statement/source ?dep-source]]
     db "my.app.core/transform")
```

**SQL:**
```sql
SELECT dep_stmt.fqn, dep_stmt.source
FROM statements stmt
JOIN dependencies d ON stmt.id = d.from_statement_id  
JOIN statements dep_stmt ON d.to_statement_id = dep_stmt.id
WHERE stmt.fqn = 'my.app.core/transform';
```

#### 2. Detect Circular Dependencies

**DataScript (Winner):**
```clojure
(defn find-circular-deps [db]
  (let [deps (d/q '[:find ?from ?to
                   :where
                   [?from-e :statement/fqn ?from]
                   [?from-e :statement/dependencies ?to]]
                 db)]
    (find-cycles-in-graph deps)))
```

**SQL (Complex):**
```sql
-- Recursive CTE for cycle detection
WITH RECURSIVE dep_paths AS (
  -- Base case: direct dependencies
  SELECT from_statement_id as start_id, 
         to_statement_id as current_id,
         ARRAY[from_statement_id] as path
  FROM dependencies
  
  UNION ALL
  
  -- Recursive case: extend paths
  SELECT dp.start_id,
         d.to_statement_id,
         dp.path || d.from_statement_id
  FROM dep_paths dp
  JOIN dependencies d ON dp.current_id = d.from_statement_id
  WHERE NOT (d.from_statement_id = ANY(dp.path))
)
SELECT * FROM dep_paths 
WHERE start_id = current_id;  -- Found a cycle
```

#### 3. Namespace Statistics

**SQL (Winner):**
```sql
SELECT 
  namespace,
  COUNT(*) as total_statements,
  COUNT(CASE WHEN pure = true THEN 1 END) as pure_statements,
  AVG(line_count) as avg_complexity
FROM statements 
GROUP BY namespace;
```

**DataScript:**
```clojure
(d/q '[:find ?ns (count ?stmt) (count ?pure-stmt)
       :where
       [?stmt :statement/namespace ?ns]
       [?pure-stmt :statement/namespace ?ns]
       [?pure-stmt :statement/pure? true]]
     db)
```

### Decision Matrix

| Aspect | DataScript | SQL | Winner |
|--------|------------|-----|---------|
| **Dependency Queries** | Natural graph traversal | Complex JOINs required | DataScript |
| **Schema Flexibility** | Add attributes anytime | Requires migrations | DataScript |
| **Clojure Integration** | Native data structures | String/JDBC overhead | DataScript |
| **Text Search** | Basic string operations | Full-text search engines | SQL |
| **Analytics/Reporting** | Limited aggregations | Rich statistical functions | SQL |
| **Performance (< 100K)** | Sub-ms in-memory | ms-level with disk I/O | DataScript |
| **Performance (> 1M)** | Memory limitations | Scales with hardware | SQL |
| **Team Collaboration** | File-based, git-friendly | Requires DB server | DataScript |
| **Backup/Restore** | Simple file operations | DB dump/restore tools | DataScript |
| **Development Velocity** | Rapid prototyping | More setup overhead | DataScript |

### Recommendation: DataScript

**Why DataScript Wins for Code Storage:**

1. **Perfect Fit for Code Graphs**: Dependencies, references, and relationships are natural
2. **Clojure-Native Development**: No impedance mismatch 
3. **Rapid Development**: Schema-less, file-based, git-friendly
4. **Size is Manageable**: Even large codebases rarely exceed 100K statements
5. **AI Integration**: Easy to pass query results directly to AI without serialization

## Part II: DataScript Persistence Options

### Core Persistence Concepts

#### 1. Serialization Approaches
```clojure
;; 1. Transaction Log Replay (Recommended)
(def tx-log (d/tx-range db nil nil))
(spit "db-transactions.edn" tx-log)

;; 2. Database Snapshot (EDN)
(def db-data (d/db->data db))
(spit "db-snapshot.edn" db-data)

;; 3. Entity Export
(def all-entities (d/q '[:find (pull ?e [*]) :where [?e]] db))
(spit "db-entities.edn" all-entities)
```

#### 2. Restoration Methods
```clojure
;; From transaction log
(def restored-db 
  (reduce d/db-with empty-db (read-string (slurp "db-transactions.edn"))))

;; From snapshot
(def restored-db (d/conn-from-data (read-string (slurp "db-snapshot.edn"))))
```

### Transaction Log vs Snapshot Comparison

#### Transaction Logs (Recommended for Code Storage)
```clojure
;; What gets backed up: The sequence of changes
(def tx-log-example
  [;; First statement added
   [{:statement/fqn "my.app.core/transform"
     :statement/source "(defn transform [data] (map inc data))"
     :statement/namespace "my.app.core"
     :statement/type :defn}]
   
   ;; Statement modified  
   [{:db/id [:statement/fqn "my.app.core/transform"]
     :statement/source "(defn transform [data opts] (map inc data))"}]
   
   ;; New statement added
   [{:statement/fqn "my.app.core/validate"
     :statement/source "(defn validate [x] (some? x))"
     :statement/namespace "my.app.core"
     :statement/type :defn}]])
```

**Advantages:**
- **Incremental and Efficient**: Only backup what changed
- **Git-Friendly**: Diffs show exactly what changed
- **Audit Trail**: Complete history of changes
- **Smaller Files**: Only stores deltas

### Persistence by Runtime Environment

## JVM Clojure Options

### 1. File-Based Persistence
```clojure
(ns my.persistence
  (:require [datascript.core :as d]
            [clojure.edn :as edn]))

;; Simple file backup
(defn backup-db [conn filepath]
  (let [db-data (d/db->data @conn)]
    (spit filepath (pr-str db-data))
    filepath))

(defn restore-db [filepath schema]
  (let [data (edn/read-string (slurp filepath))
        conn (d/create-conn schema)]
    (reset! conn (d/conn-from-data data))
    conn))

;; Usage
(def schema {:statement/fqn {:db/unique :db.unique/identity}
             :statement/source {}
             :statement/namespace {}})

(def conn (d/create-conn schema))
(backup-db conn "code-db.edn")
(def restored-conn (restore-db "code-db.edn" schema))
```

### 2. SQLite Integration
```clojure
(ns my.sqlite-persistence
  (:require [datascript.core :as d]
            [next.jdbc :as jdbc]))

(def sqlite-db {:dbtype "sqlite" :dbname "datascript-backup.db"})

(defn init-sqlite-storage []
  (jdbc/execute! sqlite-db
    ["CREATE TABLE IF NOT EXISTS datascript_snapshots (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT UNIQUE,
        data TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )"]))

(defn save-snapshot [conn snapshot-name]
  (let [db-data (pr-str (d/db->data @conn))]
    (jdbc/execute! sqlite-db
      ["INSERT OR REPLACE INTO datascript_snapshots (name, data) VALUES (?, ?)"
       snapshot-name db-data])))

(defn load-snapshot [snapshot-name schema]
  (let [result (jdbc/execute-one! sqlite-db
                 ["SELECT data FROM datascript_snapshots WHERE name = ?" snapshot-name])]
    (when result
      (let [data (read-string (:data result))
            conn (d/create-conn schema)]
        (reset! conn (d/conn-from-data data))
        conn))))
```

## ClojureScript Options

### 1. LocalStorage Persistence
```clojure
(ns my.browser-persistence
  (:require [datascript.core :as d]
            [cljs.reader :as reader]))

(defn save-db-to-localstorage [conn key]
  (let [db-data (d/db->data @conn)
        serialized (pr-str db-data)]
    (.setItem js/localStorage key serialized)))

(defn load-db-from-localstorage [key schema]
  (when-let [data (.getItem js/localStorage key)]
    (let [parsed-data (reader/read-string data)
          conn (d/create-conn schema)]
      (reset! conn (d/conn-from-data parsed-data))
      conn)))

;; Auto-save on changes
(defn setup-auto-save [conn storage-key]
  (d/listen! conn :auto-save
    (fn [tx-report]
      (save-db-to-localstorage conn storage-key))))
```

### 2. Server Synchronization
```clojure
(ns my.sync-persistence
  (:require [datascript.core :as d]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn sync-to-server [conn endpoint]
  (go
    (let [db-data (d/db->data @conn)
          response (<! (http/post endpoint 
                         {:json-params {:db-data db-data}}))]
      (if (= 200 (:status response))
        :success
        :error))))

(defn sync-from-server [endpoint schema]
  (go
    (let [response (<! (http/get endpoint))]
      (when (= 200 (:status response))
        (let [db-data (get-in response [:body :db-data])
              conn (d/create-conn schema)]
          (reset! conn (d/conn-from-data db-data))
          conn)))))
```

## Babashka Options

### 1. Native File Operations with Versioning
```clojure
(ns my.bb-persistence
  (:require [datascript.core :as d]
            [babashka.fs :as fs]
            [clojure.edn :as edn]))

(defn bb-backup-with-versioning [conn base-path]
  (let [timestamp (System/currentTimeMillis)
        filename (str base-path "-" timestamp ".edn")
        db-data (d/db->data @conn)]
    (spit filename (pr-str db-data))
    
    ;; Keep only last 10 backups
    (let [backups (sort (filter #(str/starts-with? % base-path) 
                               (map str (fs/list-dir (fs/parent base-path)))))]
      (doseq [old-backup (drop 10 (reverse backups))]
        (fs/delete old-backup)))
    
    filename))

(defn bb-restore-latest [base-path schema]
  (let [backups (sort (filter #(str/starts-with? % base-path)
                             (map str (fs/list-dir (fs/parent base-path)))))
        latest (last backups)]
    (when latest
      (let [data (edn/read-string (slurp latest))
            conn (d/create-conn schema)]
        (reset! conn (d/conn-from-data data))
        conn))))
```

## Advanced Persistence Patterns

### 1. Incremental Backup (Transaction Log)
```clojure
(ns my.incremental-backup
  (:require [datascript.core :as d]))

(defonce tx-log (atom []))

(defn setup-tx-logging [conn]
  (d/listen! conn :tx-logger
    (fn [tx-report]
      (swap! tx-log conj (:tx-data tx-report)))))

(defn save-incremental-backup [filepath]
  (spit filepath (pr-str @tx-log)))

(defn restore-from-incremental [filepath base-db]
  (let [transactions (read-string (slurp filepath))]
    (reduce (fn [db tx-data]
              (d/db-with db tx-data))
            base-db
            transactions)))
```

### 2. Auto-Backup Setup
```clojure
(defn setup-auto-backup [conn backup-file]
  (d/listen! conn :file-backup
    (fn [tx-report]
      (when (seq (:tx-data tx-report))
        ;; Append transaction to file
        (spit backup-file 
              (str (pr-str (:tx-data tx-report)) "\n")
              :append true)
        
        ;; Optional: Periodic full snapshots for faster restore
        (when (zero? (mod (count (:tx-data tx-report)) 100))
          (backup-snapshot conn "code-db-snapshot-latest.edn"))))))
```

### 3. Fast Restore Strategy
```clojure
(defn restore-with-snapshots [schema]
  (let [conn (d/create-conn schema)]
    
    ;; 1. Load latest snapshot if available (fast)
    (when (fs/exists? "code-db-snapshot-latest.edn")
      (let [snapshot-data (read-string (slurp "code-db-snapshot-latest.edn"))]
        (reset! conn (d/conn-from-data snapshot-data))))
    
    ;; 2. Apply any transactions since snapshot (incremental)
    (when (fs/exists? "code-db-txlog.edn")
      (let [all-transactions (read-string (str "[" (slurp "code-db-txlog.edn") "]"))
            recent-transactions (filter-transactions-after-snapshot all-transactions)]
        (doseq [tx recent-transactions]
          (d/transact! conn tx))))
    
    conn))
```

## Recommended Architecture for Code Storage

### Multi-Tier Persistence Strategy
```clojure
(def persistence-config
  {:primary {:type :file
            :path "data/code-db.edn"
            :backup-count 10}
   :backup {:type :sqlite
           :path "data/code-db.sqlite"}
   :sync {:type :postgres
         :url "postgresql://localhost/codebase"}})

;; Development Environment (bb-server)
;; Primary: File-based with versioning
;; Backup: SQLite for structured queries
;; Sync: Optional PostgreSQL for team sharing

;; Browser Client (ClojureScript)  
;; Primary: LocalStorage for immediate persistence
;; Backup: Sync to bb-server periodically
;; Offline: IndexedDB for larger datasets

;; Runtime Integration (SCI)
;; Same file-based approach as bb-server for consistency
```

### Hybrid Approach Implementation
```clojure
(defn setup-hybrid-backup [conn]
  {:tx-log "code-db-txlog.edn"     ; Continuous incremental backup
   :snapshots "snapshots/"         ; Periodic full backups for fast restore
   :strategy :tx-log-primary       ; Use tx-log as primary, snapshots for speed
   
   :backup-schedule
   {:on-change :append-tx-log      ; Every change goes to tx-log
    :hourly :create-snapshot       ; Hourly snapshots for fast restore
    :daily :compress-old-logs}})   ; Daily cleanup/archival
```

## Performance Considerations

### File Size Management
- **Snapshots**: Full database dumps - larger but complete
- **Transaction logs**: Incremental - smaller but need base snapshot
- **Compression**: Can reduce size by 60-80% for text-heavy data

### Read/Write Performance
- **Memory**: DataScript operations are always in-memory (fast)
- **Persistence**: Write operations are asynchronous background tasks
- **Restoration**: Only happens on startup or explicit restore

### Scalability
- **Single user**: File-based persistence is sufficient
- **Team development**: PostgreSQL/SQLite for shared state
- **Large codebases**: Consider partitioning by namespace

## Conclusion

**DataScript with transaction log persistence** is the optimal choice for database-native Clojure code storage. This approach provides:

- **Natural code relationship modeling** (dependencies, references)
- **Clojure-native development experience** (no impedance mismatch)
- **Git-friendly persistence** (file-based with meaningful diffs)
- **AI integration ready** (direct Clojure data structures)
- **Team collaboration support** (mergeable transaction logs)
- **Flexible schema evolution** (add attributes without migrations)

The transaction log approach specifically excels for code storage because it provides an audit trail of changes, enables git-friendly diffs, and supports both individual development and team collaboration workflows.