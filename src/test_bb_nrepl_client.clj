#!/usr/bin/env bb

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn run-task [task & args]
  (let [result (p/shell {:out :string :err :string :continue true}
                        "bb" "run" task (str/join " " args))]
    {:out (:out result)
     :err (:err result)
     :exit (:exit result)}))

(defn parse-edn-output [output]
  (try
    (read-string (first (str/split-lines output)))
    (catch Exception _ nil)))

(defn get-server-port []
  (let [status-result (run-task "bb-dev-nrepl-server" "status")
        edn-data (parse-edn-output (:out status-result))]
    (when (= :running (:status edn-data))
      (:port edn-data))))

(defn ensure-server-running []
  (let [port (get-server-port)]
    (if port
      (do
        (println (str "✅ Server already running on port " port))
        port)
      (do
        (println "🚀 Starting nREPL server...")
        (run-task "bb-dev-nrepl-server" "start")
        (Thread/sleep 1000)
        (get-server-port)))))

(defn test-basic-eval []
  (println "\n📊 Test: Basic evaluation")
  (let [result (run-task "nrepl-eval" "(+ 1 2 3)")]
    (if (and (zero? (:exit result))
             (= "6\n" (:out result)))
      (println "✅ PASS: Basic arithmetic evaluation")
      (do
        (println "❌ FAIL: Basic arithmetic evaluation")
        (println "Expected: 6")
        (println "Got:" (pr-str (:out result)))
        false))))

(defn test-string-eval []
  (println "\n📝 Test: String evaluation")
  (let [result (run-task "nrepl-eval" "(str \"Hello \" \"World\")")]
    (if (and (zero? (:exit result))
             (= "\"Hello World\"\n" (:out result)))
      (println "✅ PASS: String concatenation")
      (do
        (println "❌ FAIL: String concatenation")
        (println "Expected: \"Hello World\"")
        (println "Got:" (pr-str (:out result)))
        false))))

(defn test-error-handling []
  (println "\n⚠️  Test: Error handling")
  (let [result (run-task "nrepl-eval" "(/ 1 0)")]
    (if (and (zero? (:exit result))
             (str/includes? (:out result) "Error:"))
      (println "✅ PASS: Error properly caught and reported")
      (do
        (println "❌ FAIL: Error handling")
        (println "Expected error message")
        (println "Got:" (pr-str (:out result)))
        false))))

(defn test-load-file []
  (println "\n📂 Test: File loading")
  ;; Create a test file (no namespace, so defs go to user ns)
  (spit "test-nrepl-client.clj"
        "(def test-value 123)\n(println \"Test file loaded!\")")

  (let [result (run-task "nrepl-load-file" "test-nrepl-client.clj")]
    (io/delete-file "test-nrepl-client.clj" true) ; cleanup
    (if (and (zero? (:exit result))
             (str/includes? (:out result) "Loading")
             (str/includes? (:out result) "test-nrepl-client.clj"))
      (do
        (println "✅ PASS: File loading")
        ;; Test that the loaded value is accessible
        (let [val-result (run-task "nrepl-eval" "test-value")]
          (if (= "123\n" (:out val-result))
            (println "✅ PASS: Loaded value accessible")
            (do
              (println "❌ FAIL: Loaded value not accessible")
              (println "Got:" (pr-str (:out val-result)))
              false))))
      (do
        (println "❌ FAIL: File loading")
        (println "Got:" (pr-str (:out result)))
        false))))

(defn test-missing-file []
  (println "\n🚫 Test: Missing file handling")
  (let [result (run-task "nrepl-load-file" "nonexistent.clj")]
    (if (and (not (zero? (:exit result)))
             (str/includes? (:out result) "File not found"))
      (println "✅ PASS: Missing file properly handled")
      (do
        (println "❌ FAIL: Missing file handling")
        (println "Expected error about file not found")
        (println "Got:" (pr-str result))
        false))))

(defn test-usage-messages []
  (println "\n❓ Test: Usage messages")
  (let [eval-result (run-task "nrepl-eval")
        load-result (run-task "nrepl-load-file")]
    ;; Both commands should fail with non-zero exit when no args provided
    (if (and (not (zero? (:exit eval-result)))
             (not (zero? (:exit load-result))))
      (println "✅ PASS: Commands properly exit with error when no args provided")
      (do
        (println "❌ FAIL: Commands should exit with error when no args")
        (println "nrepl-eval exit:" (:exit eval-result))
        (println "nrepl-load-file exit:" (:exit load-result))
        false))))

(defn run-all-tests []
  (println "========================================")
  (println "🧪 Testing bb-nrepl-client functionality")
  (println "========================================")

  (if-let [port (ensure-server-running)]
    (do
      (println (str "🔗 Connected to nREPL server on port " port))
      (Thread/sleep 500) ; Give server time to settle

      (let [tests [(test-basic-eval)
                   (test-string-eval)
                   (test-error-handling)
                   (test-load-file)
                   (test-missing-file)
                   (test-usage-messages)]
            passed (count (filter identity tests))
            total (count tests)]

        (println "\n========================================")
        (println (str "📈 Results: " passed "/" total " tests passed"))
        (if (= passed total)
          (println "✨ All tests passed!")
          (println "⚠️  Some tests failed"))
        (println "========================================")))
    (do
      (println "❌ Could not start or connect to nREPL server")
      (println "Skipping tests")
      (System/exit 1))))

(run-all-tests)