#!/usr/bin/env bb

;; Datalevin Pod Test Deployment
;; Database storage: /var/db/datalevin/cljcodedb/

(ns datalevin-test
  (:require [babashka.pods :as pods]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

;; Configuration
(def db-base-path "/var/db/datalevin")
(def db-name "cljcodedb")
(def db-path (str db-base-path "/" db-name))

(println "🚀 Testing Datalevin Pod Deployment")
(println "Database path:" db-path)

;; Ensure database directory exists
(defn ensure-db-directory! []
  (let [db-dir (io/file db-path)]
    (when-not (.exists db-dir)
      (println "📁 Creating database directory:" db-path)
      (.mkdirs db-dir))
    (println "✅ Database directory ready:" (.getAbsolutePath db-dir))))

;; Load Datalevin pod
(defn load-datalevin-pod! []
  (try
    (println "📦 Loading Datalevin pod...")
    (pods/load-pod 'huahaiy/datalevin "0.8.25")
    (println "✅ Datalevin pod loaded successfully")
    true
    (catch Exception e
      (println "❌ Failed to load Datalevin pod:" (.getMessage e))
      (println "💡 Try installing dtlv binary or use different version")
      false)))

;; Define schema for our test database
(def schema
  {:person/name     {:db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}
   :person/email    {:db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity}
   :person/age      {:db/valueType :db.type/long
                     :db/cardinality :db.cardinality/one}
   :person/skills   {:db/valueType :db.type/string
                     :db/cardinality :db.cardinality/many}
   :project/name    {:db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}
   :project/members {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many}})

;; Main test function
(defn run-tests []
  (println "=" (apply str (repeat 60 "=")))
  (println "🧪 Datalevin Pod Deployment Test")
  (println "=" (apply str (repeat 60 "=")))
  
  ;; Setup
  (ensure-db-directory!)
  
  (if (load-datalevin-pod!)
    (do
      ;; Import Datalevin namespace after pod loading
      (require '[datalevin.core :as d])
      
      ;; Test basic operations
      (try
        (println "\n🔧 Testing basic Datalevin operations...")
        
        ;; Get connection to database
        (println "📡 Connecting to database...")
        (def conn (d/get-conn db-path schema))
        (println "✅ Database connection established")
        
        ;; Test transaction
        (println "💾 Testing transactions...")
        (d/transact! conn
                     [{:person/name "Alice Smith"
                       :person/email "alice@example.com"
                       :person/age 30
                       :person/skills ["Clojure" "DataScript" "Babashka"]}
                      {:person/name "Bob Johnson" 
                       :person/email "bob@example.com"
                       :person/age 25
                       :person/skills ["JavaScript" "React" "Node.js"]}
                      {:project/name "MCP-nREPL Integration"
                       :project/members [:person/email "alice@example.com"]}])
        (println "✅ Sample data inserted")
        
        ;; Test queries
        (println "🔍 Testing queries...")
        
        ;; Query all people
        (let [all-people (d/q '[:find ?name ?email ?age
                                :where 
                                [?e :person/name ?name]
                                [?e :person/email ?email]
                                [?e :person/age ?age]]
                              @conn)]
          (println "👥 All people in database:")
          (doseq [[name email age] all-people]
            (println "  -" name "(" email ") - Age:" age)))
        
        ;; Query people with specific skills
        (let [clojure-devs (d/q '[:find ?name
                                  :where 
                                  [?e :person/name ?name]
                                  [?e :person/skills "Clojure"]]
                                @conn)]
          (println "👨‍💻 Clojure developers:")
          (doseq [[name] clojure-devs]
            (println "  -" name)))
        
        ;; Test pull syntax
        (let [alice-id (ffirst (d/q '[:find ?e
                                      :where [?e :person/email "alice@example.com"]]
                                    @conn))
              alice-data (d/pull @conn '[*] alice-id)]
          (println "👤 Alice's complete data:")
          (pp/pprint alice-data))
        
        (println "✅ All basic operations successful")
        
        ;; Test persistence across restarts
        (try
          (println "\n🔄 Testing persistence across restarts...")
          
          ;; Close and reconnect
          (println "🔌 Reconnecting to existing database...")
          (def conn2 (d/get-conn db-path))
          
          ;; Query existing data
          (let [person-count (ffirst (d/q '[:find (count ?e)
                                            :where [?e :person/name]]
                                          @conn2))]
            (println "📊 Found" person-count "people in persisted database"))
          
          ;; Add more data
          (d/transact! conn2
                       [{:person/name "Charlie Brown"
                         :person/email "charlie@example.com" 
                         :person/age 35
                         :person/skills ["Python" "Machine Learning"]}])
          
          (let [new-count (ffirst (d/q '[:find (count ?e)
                                         :where [?e :person/name]]
                                       @conn2))]
            (println "📈 After adding Charlie, now have" new-count "people"))
          
          (println "✅ Persistence test successful")
          
          (catch Exception e
            (println "❌ Error during persistence test:" (.getMessage e))
            (.printStackTrace e)))
        
        ;; Check database files
        (println "\n📁 Checking database files...")
        (let [db-dir (io/file db-path)]
          (if (.exists db-dir)
            (do
              (println "📂 Database directory exists at:" (.getAbsolutePath db-dir))
              (println "📄 Database files:")
              (doseq [file (.listFiles db-dir)]
                (println "  -" (.getName file) "(" (.length file) "bytes)"))
              (let [total-size (reduce + (map #(.length %) (.listFiles db-dir)))]
                (println "💾 Total database size:" total-size "bytes")))
            (println "❌ Database directory does not exist!")))
        
        (println "\n🎉 All tests completed!")
        (println "💡 Database is persistent at:" db-path)
        (println "🔧 You can now integrate this into your Babashka server")
        
        (catch Exception e
          (println "❌ Error during testing:" (.getMessage e))
          (println "Stack trace:")
          (.printStackTrace e)))
      )
    (println "❌ Cannot proceed without Datalevin pod")))

;; Run if called directly
(when (= *file* (System/getProperty "babashka.file"))
  (run-tests))

;; Export for reuse
(def datalevin-config
  {:db-path db-path
   :schema schema})

(comment
  ;; Example usage from REPL or other scripts:
  ;; First load the pod and require namespace
  (pods/load-pod 'huahaiy/datalevin "0.8.25")
  (require '[datalevin.core :as d])
  
  ;; Then get connection
  (def conn (d/get-conn (:db-path datalevin-config) (:schema datalevin-config)))
  
  ;; Query all data
  (d/q '[:find ?e ?name ?email
         :where 
         [?e :person/name ?name]
         [?e :person/email ?email]]
       @conn)
  
  ;; Add new person
  (d/transact! conn
               [{:person/name "New Person"
                 :person/email "new@example.com"
                 :person/age 28}])
)