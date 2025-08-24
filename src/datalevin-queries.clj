;; Datalevin Query Examples
;; Load this via nrepl-load-file to run comprehensive query tests

(println "🔍 Running comprehensive Datalevin query tests...")

;; Ensure setup is loaded
(when-not (bound? #'conn)
  (throw (Exception. "Database not initialized! Load datalevin-setup.clj first.")))

(println "\n=== BASIC QUERIES ===")

;; 1. Simple attribute queries
(println "\n1. All names:")
(let [names (d/q '[:find ?name :where [?e :person/name ?name]] (d/db conn))]
  (doseq [[name] (sort names)]
    (println "  -" name)))

;; 2. Multi-attribute queries
(println "\n2. Name and role pairs:")
(let [people (d/q '[:find ?name ?role 
                    :where [?e :person/name ?name]
                           [?e :person/role ?role]] 
                  (d/db conn))]
  (doseq [[name role] (sort people)]
    (println "  -" name "is a" role)))

(println "\n=== PARAMETERIZED QUERIES ===")

;; 3. Query with input parameter
(println "\n3. Find specific role (developers):")
(let [devs (d/q '[:find ?name ?email
                  :in $ ?role
                  :where [?e :person/name ?name]
                         [?e :person/email ?email]
                         [?e :person/role ?role]]
                (d/db conn) "developer")]
  (doseq [[name email] devs]
    (println "  -" name "(" email ")")))

(println "\n=== AGGREGATION QUERIES ===")

;; 4. Count aggregations
(println "\n4. People count by role:")
(let [counts (d/q '[:find ?role (count ?e)
                    :where [?e :person/role ?role]]
                  (d/db conn))]
  (doseq [[role count] (sort counts)]
    (println "  -" count role "s")))

;; 5. Average age by role
(println "\n5. Average age by role:")
(let [avg-ages (d/q '[:find ?role (avg ?age)
                      :where [?e :person/role ?role]
                             [?e :person/age ?age]]
                    (d/db conn))]
  (doseq [[role avg-age] (sort avg-ages)]
    (println "  -" role ":" (format "%.1f years" (double avg-age)))))

(println "\n=== PREDICATE QUERIES ===")

;; 6. Age range queries
(println "\n6. People aged 30-35:")
(let [age-range (d/q '[:find ?name ?age
                       :where [?e :person/name ?name]
                              [?e :person/age ?age]
                              [(>= ?age 30)]
                              [(<= ?age 35)]]
                     (d/db conn))]
  (doseq [[name age] (sort age-range)]
    (println "  -" name "- Age:" age)))

(println "\n=== COLLECTION QUERIES ===")

;; 7. Multi-value attribute queries (skills)
(println "\n7. People with Clojure skills:")
(let [clojure-users (d/q '[:find ?name ?role
                           :where [?e :person/name ?name]
                                  [?e :person/role ?role]
                                  [?e :person/skills "Clojure"]]
                         (d/db conn))]
  (doseq [[name role] (sort clojure-users)]
    (println "  -" name "(" role ")")))

;; 8. Skill distribution
(println "\n8. Most common skills:")
(let [skill-counts (d/q '[:find ?skill (count ?e)
                          :where [?e :person/skills ?skill]]
                        (d/db conn))]
  (doseq [[skill count] (reverse (sort-by second skill-counts))]
    (println "  -" skill ":" count "people")))

(println "\n=== PULL API QUERIES ===")

;; 9. Pull specific person's complete data
(println "\n9. Alice Johnson's complete profile:")
(let [alice-id (ffirst (d/q '[:find ?e
                              :where [?e :person/email "alice.johnson@techcorp.com"]]
                            (d/db conn)))]
  (when alice-id
    (let [alice-data (d/pull (d/db conn) '[*] alice-id)]
      (clojure.pprint/pprint alice-data))))

;; 10. Pull multiple attributes for all people
(println "\n10. Summary profiles (pull API):")
(let [people-ids (d/q '[:find ?e
                        :where [?e :person/name]]
                      (d/db conn))
      profiles (map #(d/pull (d/db conn) 
                            [:person/name :person/role :person/age] 
                            (first %)) 
                   people-ids)]
  (doseq [profile (sort-by :person/name profiles)]
    (println "  -" (:person/name profile) 
             "(" (:person/role profile) ", " 
             (:person/age profile) " years old)")))

(println "\n=== COMPLEX QUERIES ===")

;; 11. Cross-attribute analysis
(println "\n11. Skill diversity by role:")
(let [role-skills (d/q '[:find ?role (count-distinct ?skill)
                         :where [?e :person/role ?role]
                                [?e :person/skills ?skill]]
                       (d/db conn))]
  (doseq [[role skill-count] (sort role-skills)]
    (println "  -" role ":" skill-count "different skills")))

;; 12. Find people with multiple specific skills
(println "\n12. People with both Clojure AND design skills:")
(let [multi-skilled (d/q '[:find ?name
                           :where [?e :person/name ?name]
                                  [?e :person/skills "Clojure"]
                                  [?e :person/skills ?other-skill]
                                  [(re-find #"(?i)design|ui|ux|figma" ?other-skill)]]
                         (d/db conn))]
  (if (seq multi-skilled)
    (doseq [[name] multi-skilled]
      (println "  -" name))
    (println "  - No matches found")))

(println "\n=== DATABASE STATISTICS ===")

;; 13. Comprehensive database statistics
(println "\n13. Database overview:")
(let [stats (database-stats)]
  (println "  - Total people:" (:total-people stats))
  (println "  - Unique roles:" (:unique-roles stats))
  (println "  - Unique skills:" (:unique-skills stats))
  (println "  - Roles:" (clojure.string/join ", " (sort (:roles stats))))
  (println "  - Skills:" (clojure.string/join ", " (sort (:skills stats)))))

(println "\n🎉 Query tests completed successfully!")
(println "All queries executed without errors.")