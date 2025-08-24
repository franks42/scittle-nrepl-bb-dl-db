#!/usr/bin/env bb

;; Test adding data and querying Datalevin database

(require '[babashka.pods :as pods]
         '[clojure.pprint :as pp])

(def db-path "/var/db/datalevin/cljcodedb")

(println "🧪 Testing Datalevin: Add Data and Query")
(println "Database path:" db-path)

;; Load Datalevin pod
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

;; Schema for our test
(def schema {:person/name   {:db/cardinality :db.cardinality/one}
             :person/email  {:db/cardinality :db.cardinality/one
                            :db/unique :db.unique/identity}
             :person/age    {:db/cardinality :db.cardinality/one}
             :person/skills {:db/cardinality :db.cardinality/many}})

;; Connect with schema
(def conn (d/get-conn db-path schema))
(println "✅ Connected to database")

;; Add some test data
(println "\n📝 Adding test data...")
(d/transact! conn 
             [{:person/name "Alice Cooper"
               :person/email "alice@company.com"
               :person/age 28
               :person/skills ["Clojure" "Python" "SQL"]}
              {:person/name "Bob Martin"
               :person/email "bob@company.com" 
               :person/age 35
               :person/skills ["Java" "Clojure" "Docker"]}
              {:person/name "Charlie Brown"
               :person/email "charlie@company.com"
               :person/age 42
               :person/skills ["JavaScript" "React" "Node.js"]}])

(println "✅ Data added successfully")

;; Test different types of queries
(println "\n🔍 Testing queries...")

;; Query 1: Get all people (without @ - let's see if this works)
(println "\n1. All people in database:")
(try
  (let [people (d/q '[:find ?name ?email ?age
                      :where 
                      [?e :person/name ?name]
                      [?e :person/email ?email]
                      [?e :person/age ?age]]
                    conn)]  ; Try without @
    (doseq [[name email age] people]
      (println "  -" name "(" email ") - Age:" age)))
  (catch Exception e
    (println "❌ Query failed:" (.getMessage e))))

;; Query 2: People with specific skill
(println "\n2. People who know Clojure:")
(try
  (let [clojure-devs (d/q '[:find ?name
                            :where 
                            [?e :person/name ?name]
                            [?e :person/skills "Clojure"]]
                          conn)]
    (doseq [[name] clojure-devs]
      (println "  -" name)))
  (catch Exception e
    (println "❌ Query failed:" (.getMessage e))))

;; Query 3: Count total people
(println "\n3. Total number of people:")
(try
  (let [count-result (d/q '[:find (count ?e)
                            :where [?e :person/name]]
                          conn)]
    (println "  - Total:" (ffirst count-result)))
  (catch Exception e
    (println "❌ Query failed:" (.getMessage e))))

;; Query 4: People older than 30
(println "\n4. People older than 30:")
(try
  (let [older-people (d/q '[:find ?name ?age
                            :where 
                            [?e :person/name ?name]
                            [?e :person/age ?age]
                            [(> ?age 30)]]
                          conn)]
    (doseq [[name age] older-people]
      (println "  -" name "- Age:" age)))
  (catch Exception e
    (println "❌ Query failed:" (.getMessage e))))

;; Test pull API
(println "\n5. Using pull API to get Alice's data:")
(try
  (let [alice-id (ffirst (d/q '[:find ?e
                                :where [?e :person/email "alice@company.com"]]
                              conn))
        alice-data (d/pull conn '[*] alice-id)]
    (pp/pprint alice-data))
  (catch Exception e
    (println "❌ Pull failed:" (.getMessage e))))

;; Add one more person to test incremental updates
(println "\n➕ Adding one more person...")
(d/transact! conn
             [{:person/name "Diana Prince"
               :person/email "diana@company.com"
               :person/age 29
               :person/skills ["Clojure" "ClojureScript" "Design"]}])

;; Query again to see updated count
(println "\n6. Updated count after adding Diana:")
(try
  (let [count-result (d/q '[:find (count ?e)
                            :where [?e :person/name]]
                          conn)]
    (println "  - Total now:" (ffirst count-result)))
  (catch Exception e
    (println "❌ Query failed:" (.getMessage e))))

;; Close connection
(d/close conn)

(println "\n🎉 Test completed!")
(println "💾 All data should be persisted in:" db-path)