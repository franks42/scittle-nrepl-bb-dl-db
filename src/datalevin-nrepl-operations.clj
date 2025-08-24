;; Datalevin operations to load via nrepl-load-file

(println "🔧 Loading Datalevin operations file...")

;; Add more complex data
(d/transact! conn 
             [{:name "Charlie FILE" 
               :role "manager" 
               :skills ["Leadership" "Strategy"]
               :team-size 10}
              {:name "Diana FILE"
               :role "designer" 
               :skills ["UI" "UX" "Figma"]
               :experience "senior"}])

(println "✅ Added team data via file loading")

;; Complex query function
(defn find-by-role [role]
  (d/q '[:find ?name ?skills
         :in $ ?role
         :where [?e :name ?name]
                [?e :role ?role]
                [?e :skills ?skills]]
       (d/db conn) role))

;; Query function with aggregation
(defn count-by-role []
  (d/q '[:find ?role (count ?e)
         :where [?e :role ?role]]
       (d/db conn)))

;; Summary function
(defn database-summary []
  {:total-people (ffirst (d/q '[:find (count ?e) :where [?e :name]] (d/db conn)))
   :all-roles (set (map first (d/q '[:find ?role :where [?e :role ?role]] (d/db conn))))
   :all-names (set (map first (d/q '[:find ?name :where [?e :name ?name]] (d/db conn))))})

(println "🎯 Functions defined: find-by-role, count-by-role, database-summary")
(println "📊 Database summary:" (database-summary))