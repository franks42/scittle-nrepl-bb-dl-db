#!/usr/bin/env bb
;; Simple one-shot nREPL client using your existing infrastructure
(require '[clojure.java.io :as io])

(def code (first *command-line-args*))

;; Use bencode for nREPL communication
(defn encode-bencode [data]
  (cond
    (string? data) (str (count data) ":" data)
    (number? data) (str "i" data "e")
    (map? data) (str "d" (apply str (mapcat (fn [[k v]]
                                               [(encode-bencode (name k))
                                                (encode-bencode v)])
                                             data)) "e")
    (sequential? data) (str "l" (apply str (map encode-bencode data)) "e")))

(defn decode-bencode-stream [stream]
  (let [read-until (fn [stream char]
                     (loop [result []]
                       (let [b (.read stream)]
                         (if (= b (int char))
                           (apply str result)
                           (recur (conj result (char b)))))))
        read-string (fn [stream]
                      (let [len-str (read-until stream \:)
                            len (parse-long len-str)
                            bytes (byte-array len)]
                        (.read stream bytes)
                        (String. bytes)))
        decode-value (fn decode-value [stream]
                       (let [b (.read stream)]
                         (case (char b)
                           \d (loop [m {}]
                                (let [peek (.read stream)]
                                  (if (= peek (int \e))
                                    m
                                    (do (.unread stream peek)
                                        (let [k (keyword (decode-value stream))
                                              v (decode-value stream)]
                                          (recur (assoc m k v)))))))
                           \i (parse-long (read-until stream \e))
                           (do (.unread stream b)
                               (read-string stream)))))]
    (decode-value stream)))

;; Connect and send eval request
(with-open [socket (java.net.Socket. "localhost" 1667)
            out (.getOutputStream socket)
            in (java.io.PushbackInputStream. (.getInputStream socket))]
  (.write out (.getBytes (encode-bencode {:op "eval" :code code})))
  (.flush out)
  
  ;; Read responses until we get a value or status done
  (loop []
    (when (.available in)
      (let [response (decode-bencode-stream in)]
        (cond
          (:value response) (println (:value response))
          (and (:status response) (some #{"done"} (:status response))) nil
          :else (recur))))))