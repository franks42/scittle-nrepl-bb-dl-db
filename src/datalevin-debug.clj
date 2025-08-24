#!/usr/bin/env bb

;; Debug Datalevin - check what's actually in the database

(require '[babashka.pods :as pods])

(def db-path "/var/db/datalevin/cljcodedb")

(println "🔍 Debugging Datalevin Database")
(println "Database path:" db-path)

;; Load Datalevin pod
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

;; Connect to existing database
(def conn (d/get-conn db-path))
(println "✅ Connected to database")

;; Try the simplest possible query - get all entities
(println "\n🔍 Testing simplest query...")
(try
  (let [all-entities (d/q '[:find ?e
                            :where [?e]]
                          conn)]
    (println "All entities:" all-entities)
    (println "Count:" (count all-entities)))
  (catch Exception e
    (println "❌ Simple query failed:" (.getMessage e))))

;; Try to get all datoms (lowest level)
(println "\n🔍 Testing datoms...")
(try
  (let [datoms (d/datoms conn :eavt)]
    (println "All datoms:")
    (doseq [datom (take 10 datoms)]  ; Show first 10
      (println "  " datom)))
  (catch Exception e
    (println "❌ Datoms query failed:" (.getMessage e))))

;; Add a very simple entry and test immediately
(println "\n📝 Adding simple test entry...")
(d/transact! conn [{:name "Test Person" :id 1}])

;; Try to query it immediately
(println "\n🔍 Querying simple entry...")
(try
  (let [simple-query (d/q '[:find ?name
                            :where [?e :name ?name]]
                          conn)]
    (println "Simple query result:" simple-query))
  (catch Exception e
    (println "❌ Simple entry query failed:" (.getMessage e))))

;; Try with different connection approach
(println "\n🔍 Testing with new connection...")
(def conn2 (d/get-conn db-path))
(try
  (let [test-query (d/q '[:find ?e ?a ?v
                          :where [?e ?a ?v]]
                        conn2)]
    (println "All EAV triples (first 5):")
    (doseq [triple (take 5 test-query)]
      (println "  " triple)))
  (catch Exception e
    (println "❌ EAV query failed:" (.getMessage e))))

(d/close conn)
(d/close conn2)

(println "\n🏁 Debug completed")