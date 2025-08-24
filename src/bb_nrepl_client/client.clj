(ns bb-nrepl-client.client
  "Simplified nREPL client using Babashka's built-in bencode"
  (:require [bencode.core :as bencode]))

(defn bytes->string
  "Convert byte array to string"
  [byte-array]
  (String. byte-array))

(defn decode-response-values
  "Convert byte arrays in response to strings"
  [response]
  (into {}
        (map (fn [[k v]]
               [(keyword (bytes->string k))
                (if (bytes? v)
                  (bytes->string v)
                  v)])
             response)))

(defn send-nrepl-message
  "Send a message to nREPL server and collect responses"
  [host port message]
  (with-open [socket (java.net.Socket. host port)
              out (.getOutputStream socket)
              in (java.io.PushbackInputStream. (.getInputStream socket))]
    ;; Send message
    (bencode/write-bencode out message)
    (.flush out)

    ;; Read responses until we get "done" status
    (loop [responses []
           attempts 0]
      (Thread/sleep 50)
      (if (pos? (.available (.getInputStream socket)))
        (let [raw-response (bencode/read-bencode in)
              response (decode-response-values raw-response)
              new-responses (conj responses response)]
          (if (and (:status response)
                   (some #{"done"} (:status response)))
            new-responses
            (recur new-responses 0)))
        (if (< attempts 10)
          (recur responses (inc attempts))
          responses)))))

(defn eval-code
  "Evaluate Clojure code on nREPL server"
  [code & {:keys [host port]
           :or {host "localhost" port 1667}}]
  (let [responses (send-nrepl-message host port {:op "eval" :code code})]
    (if-let [value-response (first (filter :value responses))]
      (:value value-response)
      (if-let [err-response (first (filter :err responses))]
        (str "Error: " (:err err-response))
        "No value returned"))))

(defn get-server-port
  "Read the current server port from state file"
  []
  (let [state-file ".bb-dev-nrepl-server"]
    (when (.exists (java.io.File. state-file))
      (let [state (read-string (slurp state-file))]
        (:port state)))))