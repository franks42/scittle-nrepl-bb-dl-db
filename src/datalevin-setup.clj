;; Datalevin Setup Script
;; Load this via nrepl-load-file to initialize Datalevin connection

(println "🔧 Setting up Datalevin connection...")

;; Load required namespaces
(require '[babashka.pods :as pods])

;; Load Datalevin pod
(println "📦 Loading Datalevin pod...")
(pods/load-pod 'huahaiy/datalevin "0.9.22")
(require '[pod.huahaiy.datalevin :as d])
(println "✅ Datalevin pod loaded")

;; Database configuration
(def db-config
  {:path "/var/db/datalevin/cljcodedb"
   :schema {:person/name   {:db/cardinality :db.cardinality/one}
            :person/email  {:db/cardinality :db.cardinality/one
                           :db/unique :db.unique/identity}
            :person/role   {:db/cardinality :db.cardinality/one}
            :person/skills {:db/cardinality :db.cardinality/many}
            :person/age    {:db/cardinality :db.cardinality/one}
            :project/name  {:db/cardinality :db.cardinality/one}
            :project/lead  {:db/cardinality :db.cardinality/one
                           :db/valueType :db.type/ref}}})

;; Create database connection
(def conn (d/get-conn (:path db-config) (:schema db-config)))
(println "✅ Database connection established")

;; Helper functions
(defn add-person
  "Add a person to the database"
  [person-map]
  (d/transact! conn [person-map]))

(defn add-people
  "Add multiple people to the database"
  [people]
  (d/transact! conn people))

(defn find-by-name
  "Find person by name"
  [name]
  (d/q '[:find (pull ?e [*])
         :in $ ?name
         :where [?e :person/name ?name]]
       (d/db conn) name))

(defn find-by-role
  "Find people by role"
  [role]
  (d/q '[:find ?name ?email
         :in $ ?role
         :where [?e :person/name ?name]
                [?e :person/email ?email]
                [?e :person/role ?role]]
       (d/db conn) role))

(defn find-by-skill
  "Find people with specific skill"
  [skill]
  (d/q '[:find ?name ?role
         :in $ ?skill
         :where [?e :person/name ?name]
                [?e :person/role ?role]
                [?e :person/skills ?skill]]
       (d/db conn) skill))

(defn count-by-role
  "Count people by role"
  []
  (d/q '[:find ?role (count ?e)
         :where [?e :person/role ?role]]
       (d/db conn)))

(defn all-people
  "Get all people in database"
  []
  (d/q '[:find ?name ?email ?role
         :where [?e :person/name ?name]
                [?e :person/email ?email]
                [?e :person/role ?role]]
       (d/db conn)))

(defn database-stats
  "Get database statistics"
  []
  (let [total-people (ffirst (d/q '[:find (count ?e) :where [?e :person/name]] (d/db conn)))
        roles (set (map first (d/q '[:find ?role :where [?e :person/role ?role]] (d/db conn))))
        skills (set (map first (d/q '[:find ?skill :where [?e :person/skills ?skill]] (d/db conn))))]
    {:total-people total-people
     :unique-roles (count roles)
     :unique-skills (count skills)
     :roles roles
     :skills skills}))

;; Export connection and functions for REPL use
(def datalevin-api
  {:conn conn
   :config db-config
   :add-person add-person
   :add-people add-people
   :find-by-name find-by-name
   :find-by-role find-by-role
   :find-by-skill find-by-skill
   :count-by-role count-by-role
   :all-people all-people
   :database-stats database-stats})

(println "🎯 Available functions:")
(println "  - (add-person {:person/name \"Name\" :person/email \"email\" :person/role \"role\"})")
(println "  - (find-by-name \"Name\")")
(println "  - (find-by-role \"developer\")")
(println "  - (find-by-skill \"Clojure\")")
(println "  - (count-by-role)")
(println "  - (all-people)")
(println "  - (database-stats)")
(println "")
(println "📊 Current database stats:")
(clojure.pprint/pprint (database-stats))