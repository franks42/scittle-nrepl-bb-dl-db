;; Datalevin Test Data Script
;; Load this via nrepl-load-file to add sample data

(println "📝 Adding test data to Datalevin database...")

;; Ensure setup is loaded
(when-not (bound? #'conn)
  (throw (Exception. "Database not initialized! Load datalevin-setup.clj first.")))

;; Sample data
(def sample-people
  [{:person/name "Alice Johnson"
    :person/email "alice.johnson@techcorp.com"
    :person/role "architect"
    :person/age 32
    :person/skills ["Clojure" "System Design" "Microservices" "Kafka"]}
   
   {:person/name "Bob Williams"
    :person/email "bob.williams@techcorp.com"
    :person/role "developer"
    :person/age 28
    :person/skills ["Babashka" "Clojure" "Docker" "PostgreSQL"]}
   
   {:person/name "Carol Martinez"
    :person/email "carol.martinez@techcorp.com"
    :person/role "manager"
    :person/age 38
    :person/skills ["Leadership" "Agile" "Strategy"]}
   
   {:person/name "David Chen"
    :person/email "david.chen@techcorp.com"
    :person/role "developer"
    :person/age 26
    :person/skills ["ClojureScript" "React" "Node.js" "GraphQL"]}
   
   {:person/name "Elena Rodriguez"
    :person/email "elena.rodriguez@techcorp.com"
    :person/role "designer"
    :person/age 30
    :person/skills ["UI/UX" "Figma" "Design Systems" "User Research"]}
   
   {:person/name "Frank Liu"
    :person/email "frank.liu@techcorp.com"
    :person/role "devops"
    :person/age 34
    :person/skills ["Kubernetes" "AWS" "Terraform" "Monitoring"]}])

;; Add the sample data
(println "💾 Inserting sample people...")
(add-people sample-people)
(println "✅ Sample data added successfully")

;; Verification queries
(println "\n📊 Data verification:")
(println "Total people:" (ffirst (d/q '[:find (count ?e) :where [?e :person/name]] (d/db conn))))
(println "Roles distribution:" (count-by-role))

;; Show some sample queries
(println "\n🔍 Sample queries:")
(println "Developers:")
(doseq [[name email] (find-by-role "developer")]
  (println "  -" name "(" email ")"))

(println "\nClojure experts:")
(doseq [[name role] (find-by-skill "Clojure")]
  (println "  -" name "(" role ")"))

(println "\nPeople over 30:")
(let [older-people (d/q '[:find ?name ?age
                          :where [?e :person/name ?name]
                                 [?e :person/age ?age]
                                 [(>= ?age 30)]]
                        (d/db conn))]
  (doseq [[name age] older-people]
    (println "  -" name "- Age:" age)))

(println "\n🎯 Test data loaded successfully!")
(println "Database ready for testing and queries.")