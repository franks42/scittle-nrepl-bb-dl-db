#!/usr/bin/env bb

;; Working Datalevin Test - Based on Official Examples

(require '[babashka.pods :as pods])

(def db-path "/var/db/datalevin/cljcodedb")

(println "🧪 Working Datalevin Test (Official Pattern)")
(println "Database path:" db-path)

;; Load Datalevin pod
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])

;; Test 1: Simple greeting (exact official example)
(println "\n🔧 Test 1: Simple Greeting (Official Example)")
(def conn (d/get-conn db-path))
(d/transact! conn [{:greeting "Hello world!"}])
(println "Query result:" (d/q '[:find ?g :where [_ :greeting ?g]] (d/db conn)))
(d/close conn)

;; Test 2: Schema-based data (following README pattern)
(println "\n🔧 Test 2: Schema-based Data")
(def schema {:name {:db/valueType :db.type/string 
                   :db/unique :db.unique/identity}
             :nation {:db/cardinality :db.cardinality/one}
             :aka {:db/cardinality :db.cardinality/many}})

(def conn2 (d/get-conn db-path schema))

;; Add data following README example
(d/transact! conn2 [{:name "Frege" :nation "France" :aka ["foo" "fred"]}
                    {:name "Peirce" :nation "USA"} 
                    {:name "De Morgan" :nation "England"}])

;; Query by name
(println "All names:" 
         (d/q '[:find ?name :where [?e :name ?name]] (d/db conn2)))

;; Query by nation
(println "Nations:" 
         (d/q '[:find ?nation :where [?e :nation ?nation]] (d/db conn2)))

;; Query with join (README example)
(println "Nation for alias 'fred':" 
         (d/q '[:find ?nation :in $ ?alias 
                :where [?e :aka ?alias] 
                       [?e :nation ?nation]] 
              (d/db conn2) "fred"))

(d/close conn2)

;; Test 3: Person database
(println "\n🔧 Test 3: Person Database")
(def person-schema {:person/name {:db/cardinality :db.cardinality/one}
                    :person/email {:db/cardinality :db.cardinality/one
                                  :db/unique :db.unique/identity}
                    :person/age {:db/cardinality :db.cardinality/one}
                    :person/skills {:db/cardinality :db.cardinality/many}})

(def conn3 (d/get-conn db-path person-schema))

;; Add people
(d/transact! conn3 [{:person/name "Alice Cooper"
                     :person/email "alice@company.com"
                     :person/age 28
                     :person/skills ["Clojure" "Python"]}
                    {:person/name "Bob Martin"
                     :person/email "bob@company.com" 
                     :person/age 35
                     :person/skills ["Java" "Clojure"]}])

;; Query people
(println "All people:" 
         (d/q '[:find ?name ?email 
                :where [?e :person/name ?name]
                       [?e :person/email ?email]] 
              (d/db conn3)))

;; Query by skill
(println "Clojure developers:" 
         (d/q '[:find ?name 
                :where [?e :person/name ?name]
                       [?e :person/skills "Clojure"]] 
              (d/db conn3)))

;; Count people
(println "Total people:" 
         (d/q '[:find (count ?e) 
                :where [?e :person/name]] 
              (d/db conn3)))

(d/close conn3)

;; Test 4: Persistence check with fresh connection
(println "\n🔧 Test 4: Persistence Check")
(def conn4 (d/get-conn db-path))

(println "Greetings in database:" 
         (d/q '[:find ?g :where [_ :greeting ?g]] (d/db conn4)))

(println "Names in database:" 
         (d/q '[:find ?n :where [_ :name ?n]] (d/db conn4)))

(println "People in database:" 
         (d/q '[:find ?n :where [_ :person/name ?n]] (d/db conn4)))

(d/close conn4)

(println "\n🎉 All tests completed!")
(println "💾 Database files should contain persistent data")

;; Final file check
(require '[clojure.java.io :as io])
(let [db-dir (io/file db-path)]
  (when (.exists db-dir)
    (println "\n📁 Database files:")
    (doseq [file (.listFiles db-dir)]
      (println "  -" (.getName file) "(" (.length file) "bytes)"))))