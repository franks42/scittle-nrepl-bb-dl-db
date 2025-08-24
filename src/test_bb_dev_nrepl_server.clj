#!/usr/bin/env bb

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[bb-nrepl-client.client :as client])

(defn run-command [& args]
  (let [result (p/shell {:out :string :err :string :continue true}
                        "bb" "run" "bb-dev-nrepl-server" (str/join " " args))]
    {:out (:out result)
     :err (:err result)
     :exit (:exit result)}))

(defn parse-edn-output [output]
  (try
    (read-string (first (str/split-lines output)))
    (catch Exception _ nil)))

(defn test-status-when-not-running []
  (println "\n📋 Test: Status when not running")
  (let [result (run-command "status")
        edn-data (parse-edn-output (:out result))]
    (if (= :not-running (:status edn-data))
      (println "✅ PASS: Server correctly reports not running")
      (println "❌ FAIL: Expected :not-running status"))))

(defn test-start-server []
  (println "\n🚀 Test: Start server")
  (let [result (run-command "start")
        edn-data (parse-edn-output (:out result))]
    (if (and (= :started (:status edn-data))
             (:pid edn-data)
             (:port edn-data))
      (do
        (println (str "✅ PASS: Server started on port " (:port edn-data) " (PID: " (:pid edn-data) ")"))
        true)
      (do
        (println "❌ FAIL: Server did not start correctly")
        false))))

(defn test-duplicate-start []
  (println "\n🔁 Test: Duplicate start prevention")
  (let [result (run-command "start")
        edn-data (parse-edn-output (:out result))]
    (if (and (= :error (:status edn-data))
             (= :already-running (:reason edn-data)))
      (println "✅ PASS: Duplicate start correctly prevented")
      (println "❌ FAIL: Expected duplicate start to be prevented"))))

(defn test-status-when-running []
  (println "\n📊 Test: Status when running")
  (let [result (run-command "status")
        edn-data (parse-edn-output (:out result))]
    (if (and (= :running (:status edn-data))
             (:pid edn-data)
             (:port edn-data))
      (println (str "✅ PASS: Server status shows running on port " (:port edn-data)))
      (println "❌ FAIL: Expected :running status"))))

(defn test-restart-server []
  (println "\n🔄 Test: Restart server")
  (let [status-before (parse-edn-output (:out (run-command "status")))
        old-pid (:pid status-before)
        _ (Thread/sleep 100)
        _ (run-command "restart")
        _ (Thread/sleep 1000)
        status-after (parse-edn-output (:out (run-command "status")))
        new-pid (:pid status-after)
        new-port (:port status-after)]
    (if (and (not= old-pid new-pid)
             (number? new-pid)
             (number? new-port))
      (println (str "✅ PASS: Server restarted with new PID " new-pid " (was " old-pid ")"))
      (println "❌ FAIL: Restart did not create new process"))))

(defn test-stop-server []
  (println "\n🛑 Test: Stop server")
  (let [result (run-command "stop")
        edn-data (parse-edn-output (:out result))]
    (if (= :stopped (:status edn-data))
      (println "✅ PASS: Server stopped successfully")
      (println "❌ FAIL: Server did not stop correctly"))))

(defn test-stop-when-not-running []
  (println "\n🚫 Test: Stop when not running")
  (let [result (run-command "stop")
        edn-data (parse-edn-output (:out result))]
    (if (= :not-running (:status edn-data))
      (println "✅ PASS: Correctly handled stop when not running")
      (println "❌ FAIL: Expected :not-running status"))))

(defn test-nrepl-connectivity []
  (println "\n🔗 Test: nREPL server connectivity")
  (let [status-result (run-command "status")
        status-data (parse-edn-output (:out status-result))]
    (if (= :running (:status status-data))
      (let [port (:port status-data)]
        (try
          (println (str "   Connecting to port " port "..."))
          (let [result (client/eval-code "(+ 1 2 3)" :port port)]
            (if (= "6" result)
              (println "✅ PASS: nREPL server accepts connections and evaluates code")
              (do
                (println "❌ FAIL: nREPL evaluation failed")
                (println (str "   Expected: 6, Got: " (pr-str result))))))
          (catch Exception e
            (println "❌ FAIL: Could not connect to nREPL server")
            (println (str "   Error: " (.getMessage e))))))
      (println "⚠️  SKIP: nREPL connectivity test (server not running)"))))

(defn test-nrepl-functionality []
  (println "\n⚡ Test: nREPL server functionality")
  (let [status-result (run-command "status")
        status-data (parse-edn-output (:out status-result))]
    (if (= :running (:status status-data))
      (let [port (:port status-data)]
        (try
          ;; Test multiple evaluations
          (let [tests [["(str \"Hello \" \"World\")" "\"Hello World\""]
                       ["(count [1 2 3 4 5])" "5"]
                       ["(inc 42)" "43"]
                       ["(keyword \"test\")" ":test"]]]
            (println "   Testing multiple evaluations...")
            (let [results (map (fn [[code expected]]
                                 (let [result (client/eval-code code :port port)]
                                   (if (= expected result)
                                     true
                                     (do
                                       (println (str "   ❌ Failed: " code))
                                       (println (str "      Expected: " expected ", Got: " (pr-str result)))
                                       false))))
                               tests)
                  passed (count (filter identity results))
                  total (count results)]
              (if (= passed total)
                (println (str "✅ PASS: All " total " nREPL evaluations successful"))
                (println (str "❌ FAIL: Only " passed "/" total " nREPL evaluations successful")))))
          (catch Exception e
            (println "❌ FAIL: nREPL functionality test failed")
            (println (str "   Error: " (.getMessage e))))))
      (println "⚠️  SKIP: nREPL functionality test (server not running)"))))

(defn test-nrepl-error-handling []
  (println "\n⚠️  Test: nREPL error handling")
  (let [status-result (run-command "status")
        status-data (parse-edn-output (:out status-result))]
    (if (= :running (:status status-data))
      (let [port (:port status-data)]
        (try
          (println "   Testing error condition...")
          (let [result (client/eval-code "(/ 1 0)" :port port)]
            (if (str/includes? result "Error:")
              (println "✅ PASS: nREPL server properly handles errors")
              (do
                (println "❌ FAIL: Expected error message")
                (println (str "   Got: " (pr-str result))))))
          (catch Exception e
            (println "❌ FAIL: nREPL error handling test failed")
            (println (str "   Error: " (.getMessage e))))))
      (println "⚠️  SKIP: nREPL error handling test (server not running)"))))

(defn cleanup []
  (println "\n🧹 Cleaning up...")
  (run-command "stop")
  (Thread/sleep 500))

(defn run-all-tests []
  (println "========================================")
  (println "🧪 Testing bb-dev-nrepl-server commands")
  (println "========================================")

  (cleanup)

  (test-status-when-not-running)
  (Thread/sleep 100)

  (if (test-start-server)
    (do
      (Thread/sleep 1000)
      (test-duplicate-start)
      (Thread/sleep 100)
      (test-status-when-running)
      (Thread/sleep 100)
      (test-nrepl-connectivity)
      (Thread/sleep 100)
      (test-nrepl-functionality)
      (Thread/sleep 100)
      (test-nrepl-error-handling)
      (Thread/sleep 100)
      (test-restart-server)
      (Thread/sleep 100)
      (test-stop-server)
      (Thread/sleep 100)
      (test-stop-when-not-running))
    (println "\n⚠️  Skipping remaining tests due to start failure"))

  (println "\n========================================")
  (println "✨ Test suite complete")
  (println "========================================"))

(run-all-tests)