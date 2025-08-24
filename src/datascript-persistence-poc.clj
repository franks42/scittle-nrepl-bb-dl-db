#!/usr/bin/env bb

;; DataScript Persistence Proof-of-Concept for Babashka
;; This demonstrates different approaches to persisting DataScript databases to files

(ns datascript-persistence-poc
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

;; Note: DataScript needs to be compiled into Babashka with feature flag
;; For now, this is a conceptual implementation showing the patterns

;; ==============================================================================
;; Option 1: Simple EDN Persistence (Most Basic)
;; ==============================================================================

(defn save-db-as-edn
  "Save database as EDN by extracting all datoms"
  [db filepath]
  (let [datoms (when db
                 ;; In real DataScript: (d/datoms db :eavt)
                 ;; This would return all datoms as [e a v tx added]
                 (comment "Extract all datoms here"))
        data {:schema (comment "db schema here")
              :datoms datoms
              :max-eid (comment "max entity id")
              :max-tx (comment "max transaction id")}]
    (spit filepath (pr-str data))))

(defn load-db-from-edn
  "Reconstruct database from EDN file"
  [filepath]
  (when (.exists (io/file filepath))
    (let [{:keys [schema datoms]} (edn/read-string (slurp filepath))]
      ;; Would reconstruct with: (d/conn-from-datoms datoms schema)
      (comment "Reconstruct DB here"))))

;; ==============================================================================
;; Option 2: Transit Persistence (More Efficient)
;; ==============================================================================

(defn save-db-as-transit
  "Save database using Transit format for better performance"
  [db filepath]
  ;; Requires datascript-transit library
  ;; (require '[datascript.transit :as dt])
  ;; (dt/write-transit-str db)
  (comment "Transit serialization here"))

(defn load-db-from-transit
  "Load database from Transit format"
  [filepath]
  ;; (dt/read-transit-str (slurp filepath))
  (comment "Transit deserialization here"))

;; ==============================================================================
;; Option 3: Transaction Log Persistence (Event Sourcing Style)
;; ==============================================================================

(defn create-tx-log-persistence
  "Create a transaction log based persistence system"
  []
  (let [log-file "datascript-tx-log.edn"
        tx-log (atom [])]
    
    {:append-tx (fn [tx-data]
                  ;; Append transaction to log file
                  (swap! tx-log conj tx-data)
                  (spit log-file (pr-str @tx-log) :append false))
     
     :replay-log (fn []
                   ;; Replay all transactions to rebuild DB
                   (when (.exists (io/file log-file))
                     (let [txs (edn/read-string (slurp log-file))]
                       ;; (reduce (fn [conn tx] (d/transact! conn tx) conn)
                       ;;         (d/create-conn schema)
                       ;;         txs)
                       (comment "Replay transactions"))))}))

;; ==============================================================================
;; Option 4: Snapshot + Delta Persistence (Hybrid Approach)
;; ==============================================================================

(defn create-snapshot-delta-persistence
  "Combine periodic snapshots with incremental deltas"
  [snapshot-interval]
  (let [snapshot-file "datascript-snapshot.edn"
        delta-file "datascript-deltas.edn"
        tx-counter (atom 0)]
    
    {:save (fn [db tx-data]
             (swap! tx-counter inc)
             (if (zero? (mod @tx-counter snapshot-interval))
               ;; Time for a new snapshot
               (do
                 (save-db-as-edn db snapshot-file)
                 (spit delta-file "[]"))  ; Clear deltas
               ;; Just append delta
               (let [deltas (if (.exists (io/file delta-file))
                             (edn/read-string (slurp delta-file))
                             [])]
                 (spit delta-file (pr-str (conj deltas tx-data))))))
     
     :load (fn []
             (let [base-db (load-db-from-edn snapshot-file)
                   deltas (when (.exists (io/file delta-file))
                           (edn/read-string (slurp delta-file)))]
               ;; Apply deltas to base snapshot
               (comment "Apply deltas to base DB")))}))

;; ==============================================================================
;; Option 5: Simple Atom-Based Persistence (For Small DBs)
;; ==============================================================================

(defn create-atom-persistence
  "Simple persistence by saving entire DB state on every change"
  [filepath]
  (let [save-fn (fn [db]
                  (spit filepath (pr-str {:db-state db
                                         :timestamp (System/currentTimeMillis)})))
        load-fn (fn []
                  (when (.exists (io/file filepath))
                    (:db-state (edn/read-string (slurp filepath)))))]
    
    {:save save-fn
     :load load-fn
     :wrap-conn (fn [conn]
                  ;; Add listener to auto-save on changes
                  ;; (d/listen! conn :persistence
                  ;;           (fn [tx-report]
                  ;;             (save-fn (:db-after tx-report))))
                  conn)}))

;; ==============================================================================
;; Performance Considerations
;; ==============================================================================

(def performance-notes
  {:edn {:pros ["Human readable" "Built into Clojure" "Simple"]
         :cons ["Larger file size" "Slower parse/serialize"]
         :use-case "Small databases, debugging, configuration"}
   
   :transit {:pros ["Compact format" "Fast serialization" "Type preservation"]
             :cons ["Binary format" "Requires library"]
             :use-case "Production systems, larger databases"}
   
   :tx-log {:pros ["Event sourcing pattern" "Audit trail" "Time travel"]
            :cons ["File grows over time" "Slow replay for large logs"]
            :use-case "Systems requiring audit, debugging, undo/redo"}
   
   :snapshot-delta {:pros ["Balanced performance" "Bounded replay time"]
                    :cons ["More complex" "Two files to manage"]
                    :use-case "Long-running systems with frequent updates"}
   
   :atom-based {:pros ["Simplest implementation" "Immediate consistency"]
                :cons ["Writes entire DB each time" "Not suitable for large DBs"]
                :use-case "Small databases, prototypes, config storage"}})

;; ==============================================================================
;; Deployment Considerations
;; ==============================================================================

(def deployment-notes
  {:babashka-compilation
   ["DataScript must be compiled into Babashka with feature flag"
    "Set BABASHKA_XMX=\"-J-Xmx8g\" for compilation"
    "Use bb.edn to specify DataScript dependency"]
   
   :file-system
   ["Ensure write permissions for persistence files"
    "Consider using XDG_DATA_HOME or similar for file locations"
    "Implement file locking for concurrent access"]
   
   :alternatives
   ["Datahike - Built-in persistence, available as Babashka pod"
    "Datalevin - SQLite-based, available as Babashka pod"
    "Plain EDN files - For simple key-value needs"
    "SQLite via pod - For relational data"]
   
   :production-checklist
   ["Backup strategy for persistence files"
    "Corruption recovery mechanism"
    "Migration strategy for schema changes"
    "Monitoring disk usage"
    "Performance benchmarks for your data size"]})

;; ==============================================================================
;; Example Usage Pattern
;; ==============================================================================

(comment
  ;; Initialize persistence
  (def persistence (create-atom-persistence "myapp-db.edn"))
  
  ;; Create connection with schema
  (def schema {:user/name {:db/cardinality :db.cardinality/one}
               :user/email {:db/cardinality :db.cardinality/one
                           :db/unique :db.unique/identity}})
  
  ;; Load existing DB or create new
  (def conn (if-let [db ((:load persistence))]
              (d/conn-from-db db)
              (d/create-conn schema)))
  
  ;; Wrap connection with auto-persistence
  ((:wrap-conn persistence) conn)
  
  ;; Now all transactions are automatically persisted
  (d/transact! conn [{:user/name "Alice"
                      :user/email "alice@example.com"}])
  
  ;; DB is automatically saved to disk
  )

(defn -main [& args]
  (println "DataScript Persistence Options for Babashka")
  (println "=" (apply str (repeat 50 "=")))
  (println "\nPerformance Considerations:")
  (pp/pprint performance-notes)
  (println "\nDeployment Notes:")
  (pp/pprint deployment-notes))

;; Run with: bb datascript-persistence-poc.clj