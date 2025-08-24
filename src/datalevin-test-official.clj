#!/usr/bin/env bb

;; Datalevin Pod Test - Based on Official Example
;; Database storage: /var/db/datalevin/cljcodedb/

(require '[babashka.pods :as pods]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

;; Configuration
(def db-path "/var/db/datalevin/cljcodedb")

(println "🚀 Testing Datalevin Pod (Official Pattern)")
(println "Database path:" db-path)

;; Ensure database directory exists
(let [db-dir (io/file db-path)]
  (when-not (.exists db-dir)
    (println "📁 Creating database directory:" db-path)
    (.mkdirs db-dir))
  (println "✅ Database directory ready:" (.getAbsolutePath db-dir)))

;; Load Datalevin pod (using official pattern)
(println "📦 Loading Datalevin pod...")
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(println "✅ Pod loaded successfully")

;; Require the pod namespace (official pattern)
(require '[pod.huahaiy.datalevin :as d])
(println "✅ Namespace imported")

;; Test 1: Basic Connection and Transaction
(println "\n🔧 Test 1: Basic Operations")
(def conn (d/get-conn db-path))
(println "📡 Connection established")

;; Simple transaction (following official example)
(d/transact! conn [{:greeting "Hello Datalevin!"
                    :timestamp (System/currentTimeMillis)}])
(println "💾 Basic transaction completed")

;; Test 2: Schema and More Complex Data
(println "\n🔧 Test 2: Schema and Complex Data")

;; Define schema for our code database
(def schema {:person/name     {:db/cardinality :db.cardinality/one}
             :person/email    {:db/cardinality :db.cardinality/one
                              :db/unique :db.unique/identity}
             :person/skills   {:db/cardinality :db.cardinality/many}
             :project/name    {:db/cardinality :db.cardinality/one}
             :project/lang    {:db/cardinality :db.cardinality/one}})

;; Create connection with schema
(def conn-with-schema (d/get-conn db-path schema))

;; Add sample data
(d/transact! conn-with-schema
             [{:person/name "Alice"
               :person/email "alice@example.com"
               :person/skills ["Clojure" "Babashka" "DataScript"]}
              {:person/name "Bob" 
               :person/email "bob@example.com"
               :person/skills ["JavaScript" "React"]}
              {:project/name "MCP-nREPL"
               :project/lang "Clojure"}])
(println "💾 Schema-based data inserted")

;; Test 3: Queries
(println "\n🔧 Test 3: Querying Data")

;; Query all people
(let [people (d/q '[:find ?name ?email
                    :where 
                    [?e :person/name ?name]
                    [?e :person/email ?email]]
                  @conn-with-schema)]
  (println "👥 People in database:")
  (doseq [[name email] people]
    (println "  -" name "(" email ")")))

;; Query by skills
(let [clojure-devs (d/q '[:find ?name
                          :where 
                          [?e :person/name ?name]
                          [?e :person/skills "Clojure"]]
                        @conn-with-schema)]
  (println "👨‍💻 Clojure developers:")
  (doseq [[name] clojure-devs]
    (println "  -" name)))

;; Test pull API
(let [alice-id (ffirst (d/q '[:find ?e
                              :where [?e :person/email "alice@example.com"]]
                            @conn-with-schema))
      alice-data (d/pull @conn-with-schema '[*] alice-id)]
  (println "👤 Alice's data (pull API):")
  (pp/pprint alice-data))

;; Test 4: Persistence Check
(println "\n🔧 Test 4: Persistence Verification")

;; Create new connection to same database
(def conn-new (d/get-conn db-path))

;; Count existing entries
(let [greeting-count (count (d/q '[:find ?e
                                   :where [?e :greeting]]
                                 @conn-new))
      person-count (count (d/q '[:find ?e
                                 :where [?e :person/name]]
                               @conn-new))]
  (println "📊 Persisted data found:")
  (println "  - Greetings:" greeting-count)
  (println "  - People:" person-count))

;; Test 5: File System Check
(println "\n🔧 Test 5: Database Files")
(let [db-dir (io/file db-path)]
  (if (.exists db-dir)
    (do
      (println "📂 Database directory:" (.getAbsolutePath db-dir))
      (println "📄 Database files:")
      (doseq [file (.listFiles db-dir)]
        (println "  -" (.getName file) 
                 "(" (.length file) "bytes)"
                 (if (.isDirectory file) "[DIR]" "[FILE]")))
      (let [total-size (reduce + (map #(if (.isFile %) (.length %) 0) 
                                     (.listFiles db-dir)))]
        (println "💾 Total database size:" total-size "bytes")))
    (println "❌ Database directory not found!")))

;; Close connections (following good practices)
(d/close conn)
(d/close conn-with-schema)
(d/close conn-new)

(println "\n🎉 All tests completed successfully!")
(println "💡 Database persisted at:" db-path)
(println "🔧 Ready for integration into Babashka server")

;; Export configuration for reuse
(def datalevin-config
  {:db-path db-path
   :schema schema
   :connect-fn (fn [& [custom-schema]]
                 (d/get-conn db-path (or custom-schema schema)))})

(println "\n📋 Usage Summary:")
(println "- Load pod: (pods/load-pod 'huahaiy/datalevin \"0.9.22\")")
(println "- Require:  (require '[pod.huahaiy.datalevin :as d])")
(println "- Connect:  (d/get-conn \"" db-path "\")")
(println "- Schema:   Available in datalevin-config")