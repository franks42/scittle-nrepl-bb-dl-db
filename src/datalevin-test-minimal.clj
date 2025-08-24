#!/usr/bin/env bb

;; Minimal Datalevin Pod Test

(require '[babashka.pods :as pods]
         '[clojure.java.io :as io])

;; Configuration
(def db-path "/var/db/datalevin/cljcodedb")

(println "🚀 Minimal Datalevin Pod Test")
(println "Database path:" db-path)

;; Load pod using binary path
(try
  (println "📦 Loading Datalevin pod from binary...")
  (pods/load-pod "dtlv")
  (println "✅ Pod loaded successfully")
  
  ;; Import namespace
  (require '[datalevin.core :as d])
  (println "✅ Namespace imported")
  
  ;; Simple schema
  (def schema {:name {:db/cardinality :db.cardinality/one}})
  
  ;; Test connection
  (println "📡 Testing connection...")
  (def conn (d/get-conn db-path schema))
  (println "✅ Connection established")
  
  ;; Test transaction
  (println "💾 Testing transaction...")
  (d/transact! conn [{:name "Test Entry"}])
  (println "✅ Transaction successful")
  
  ;; Test query
  (println "🔍 Testing query...")
  (let [results (d/q '[:find ?name
                       :where [?e :name ?name]]
                     @conn)]
    (println "📊 Found entries:" results))
  
  ;; Check files
  (println "📁 Checking database files...")
  (let [db-dir (io/file db-path)]
    (when (.exists db-dir)
      (println "Files:")
      (doseq [file (.listFiles db-dir)]
        (println "  -" (.getName file) "(" (.length file) "bytes)"))))
  
  (println "🎉 Test completed successfully!")
  
  (catch Exception e
    (println "❌ Error:" (.getMessage e))
    (.printStackTrace e)))