#!/usr/bin/env bb

;; Test Multi-Service State Management

(require '[babashka.fs :as fs]
         '[babashka.process :as p]
         '[clojure.pprint :as pprint])

(def test-state-file ".bb-super-duper-server")

(defn test-clean-start
  "Test starting from clean state"
  []
  (println "🧪 Test 1: Clean Start")

  ;; Clean up any existing state
  (when (fs/exists? test-state-file)
    (fs/delete test-state-file))

  ;; Start server
  (let [result (p/shell {:out :string :err :string}
                        "bb" "src/bb_dev_nrepl_server.clj" "start")]
    (if (zero? (:exit result))
      (do
        (println "✅ Server started successfully")
        (println "📊 State file created:")
        (when (fs/exists? test-state-file)
          (let [state (read-string (slurp test-state-file))]
            (pprint/pprint state)
            ;; Validate structure
            (if (and (contains? state :services)
                     (contains? (:services state) :nrepl-main)
                     (contains? state :startup-phase)
                     (contains? state :created-at))
              (println "✅ State structure is correct")
              (println "❌ State structure is incorrect"))))
        true)
      (do
        (println "❌ Failed to start server")
        (println "Error:" (:err result))
        false))))

(defn test-status
  "Test status command with multi-service state"
  []
  (println "\n🧪 Test 2: Status Command")

  (let [result (p/shell {:out :string :err :string}
                        "bb" "src/bb_dev_nrepl_server.clj" "status")]
    (if (zero? (:exit result))
      (do
        (println "✅ Status command succeeded")
        (println "📊 Status output:")
        (println (:out result))
        true)
      (do
        (println "❌ Status command failed")
        (println "Error:" (:err result))
        false))))

(defn test-nrepl-discovery
  "Test nREPL-based discovery via get-server-info"
  []
  (println "\n🧪 Test 3: nREPL Discovery")

  ;; First read the state to get the port
  (if-let [state (when (fs/exists? test-state-file)
                   (read-string (slurp test-state-file)))]
    (let [port (get-in state [:services :nrepl-main :port])
          result (p/shell {:out :string :err :string}
                          "bb" "run" "nrepl-eval" "(load-file \"src/super_duper_server_info.clj\") (super-duper-server-info/get-server-info)" (str port))]
      (if (zero? (:exit result))
        (do
          (println "✅ nREPL discovery succeeded")
          (println "📊 Server info:")
          (println (:out result))
          true)
        (do
          (println "❌ nREPL discovery failed")
          (println "Error:" (:err result))
          false)))
    (do
      (println "❌ No state file found for nREPL discovery")
      false)))

(defn test-stop
  "Test stopping server"
  []
  (println "\n🧪 Test 4: Stop Server")

  (let [result (p/shell {:out :string :err :string}
                        "bb" "src/bb_dev_nrepl_server.clj" "stop")]
    (if (zero? (:exit result))
      (do
        (println "✅ Server stopped successfully")
        (println "📊 Stop output:")
        (println (:out result))
        ;; Verify state file cleanup
        (if (fs/exists? test-state-file)
          (println "❌ State file still exists after stop")
          (println "✅ State file cleaned up"))
        (not (fs/exists? test-state-file)))
      (do
        (println "❌ Failed to stop server")
        (println "Error:" (:err result))
        false))))

(defn run-tests
  "Run all multi-service state tests"
  []
  (println "🚀 Multi-Service State Management Tests")
  (println "=====================================")

  (let [results [(test-clean-start)
                 (test-status)
                 (test-nrepl-discovery)
                 (test-stop)]]

    (println "\n📊 Test Results:")
    (println "===============")
    (let [passed (count (filter true? results))
          total (count results)]
      (println (str "Passed: " passed "/" total))

      (if (= passed total)
        (do
          (println "🎉 All tests passed!")
          (System/exit 0))
        (do
          (println "❌ Some tests failed!")
          (System/exit 1))))))

;; Run tests
(run-tests)