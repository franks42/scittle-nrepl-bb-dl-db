(ns datalevin-service
  "Datalevin database integration for Super Duper BB Server"
  (:require [babashka.pods :as pods]))

(def db-path "/var/db/datalevin/cljcodedb")
(def datalevin-pod-loaded (atom false))

(defn load-datalevin-pod!
  "Load Datalevin pod if not already loaded"
  []
  (when-not @datalevin-pod-loaded
    (try
      (println "📦 Loading Datalevin pod...")
      (pods/load-pod 'huahaiy/datalevin "0.9.22")
      (require '[pod.huahaiy.datalevin :as d])
      (reset! datalevin-pod-loaded true)
      (println "✅ Datalevin pod loaded successfully")
      {:status :loaded :version "0.9.22" :db-path db-path}
      (catch Exception e
        (println (str "❌ Failed to load Datalevin pod: " (.getMessage e)))
        {:status :error :error (.getMessage e)}))))

(defn test-datalevin-connection
  "Test basic Datalevin connectivity and operations"
  []
  (if @datalevin-pod-loaded
    (try
      (println "🧪 Testing Datalevin connection...")
      ;; Dynamic require since pod must be loaded first
      (let [d (resolve 'pod.huahaiy.datalevin/get-conn)
            db-fn (resolve 'pod.huahaiy.datalevin/db)
            q-fn (resolve 'pod.huahaiy.datalevin/q)
            transact-fn (resolve 'pod.huahaiy.datalevin/transact!)
            close-fn (resolve 'pod.huahaiy.datalevin/close)]

        (when (and d db-fn q-fn transact-fn close-fn)
          (let [conn (d db-path)]
            ;; Simple test transaction
            (transact-fn conn [{:test-key "Super Duper BB Server Test"
                                :timestamp (str (java.time.Instant/now))}])

            ;; Query the data back
            (let [result (q-fn '[:find ?v :where [_ :test-key ?v]] (db-fn conn))]
              (close-fn conn)
              (println "✅ Datalevin test successful")
              {:status :success :test-result result :db-path db-path}))))
      (catch Exception e
        (println (str "❌ Datalevin test failed: " (.getMessage e)))
        {:status :error :error (.getMessage e)}))
    {:status :error :error "Datalevin pod not loaded"}))

(defn initialize-datalevin
  "Initialize Datalevin service with pod loading and connection test"
  []
  (println "🗄️  Initializing Datalevin service...")
  (let [load-result (load-datalevin-pod!)]
    (if (= (:status load-result) :loaded)
      (let [test-result (test-datalevin-connection)]
        (merge load-result test-result))
      load-result)))

(defn get-datalevin-status
  "Get current Datalevin service status"
  []
  {:pod-loaded @datalevin-pod-loaded
   :db-path db-path
   :status (if @datalevin-pod-loaded :ready :not-loaded)})