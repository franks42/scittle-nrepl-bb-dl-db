#!/usr/bin/env bb

;; Test Datalevin Integration Service

(require '[babashka.fs :as fs]
         '[babashka.process :as p])

(defn test-datalevin-pod-loading
  "Test Datalevin pod loading functionality"
  []
  (println "🧪 Test 1: Datalevin Pod Loading")

  (let [result (p/shell {:out :string :err :string}
                        "bb" "-e"
                        "(load-file \"src/datalevin_service.clj\") (datalevin-service/load-datalevin-pod!)")]
    (if (zero? (:exit result))
      (do
        (println "✅ Datalevin pod loading test passed")
        (println "📊 Pod loading output:")
        (println (:out result))
        true)
      (do
        (println "❌ Datalevin pod loading test failed")
        (println "Error:" (:err result))
        false))))

(defn test-datalevin-initialization
  "Test complete Datalevin service initialization"
  []
  (println "\n🧪 Test 2: Datalevin Service Initialization")

  (let [result (p/shell {:out :string :err :string}
                        "bb" "-e"
                        "(load-file \"src/datalevin_service.clj\") (datalevin-service/initialize-datalevin)")]
    (if (zero? (:exit result))
      (do
        (println "✅ Datalevin initialization test passed")
        (println "📊 Initialization output:")
        (println (:out result))
        true)
      (do
        (println "❌ Datalevin initialization test failed")
        (println "Error:" (:err result))
        false))))

(defn test-datalevin-status
  "Test Datalevin status reporting"
  []
  (println "\n🧪 Test 3: Datalevin Status Reporting")

  (let [result (p/shell {:out :string :err :string}
                        "bb" "-e"
                        "(load-file \"src/datalevin_service.clj\") (datalevin-service/initialize-datalevin) (datalevin-service/get-datalevin-status)")]
    (if (zero? (:exit result))
      (do
        (println "✅ Datalevin status test passed")
        (println "📊 Status output:")
        (println (:out result))
        true)
      (do
        (println "❌ Datalevin status test failed")
        (println "Error:" (:err result))
        false))))

(defn test-database-directory
  "Test database directory setup"
  []
  (println "\n🧪 Test 4: Database Directory")

  (let [db-dir "/var/db/datalevin/cljcodedb"]
    (if (fs/exists? db-dir)
      (do
        (println "✅ Database directory exists")
        (println (str "📁 Directory: " db-dir))
        (let [files (try (vec (fs/list-dir db-dir)) (catch Exception _ []))]
          (if (empty? files)
            (println "📂 Directory is empty (fresh database)")
            (do
              (println "📂 Database files found:")
              (doseq [file (take 5 files)]
                (println "  -" (fs/file-name file))))))
        true)
      (do
        (println "⚠️  Database directory doesn't exist")
        (println "Creating database directory...")
        (try
          (fs/create-dirs db-dir)
          (println "✅ Database directory created")
          true
          (catch Exception e
            (println (str "❌ Failed to create database directory: " (.getMessage e)))
            false))))))

(defn run-datalevin-tests
  "Run all Datalevin integration tests"
  []
  (println "🚀 Datalevin Integration Tests")
  (println "=============================")

  (let [results [(test-database-directory)
                 (test-datalevin-pod-loading)
                 (test-datalevin-initialization)
                 (test-datalevin-status)]]

    (println "\n📊 Test Results:")
    (println "===============")
    (let [passed (count (filter true? results))
          total (count results)]
      (println (str "Passed: " passed "/" total))

      (if (= passed total)
        (do
          (println "🎉 All Datalevin tests passed!")
          (System/exit 0))
        (do
          (println "❌ Some Datalevin tests failed!")
          (System/exit 1))))))

;; Run tests
(run-datalevin-tests)