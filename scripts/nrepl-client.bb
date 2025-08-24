#!/usr/bin/env bb

;; Simple nREPL client for one-shot evaluations
;; Usage: bb client.bb "(+ 1 2 3)"
;;    or: ./client.bb "(+ 1 2 3)"

(ns nrepl-client
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

;; Simple bencode implementation
(defn encode-bencode [data]
  (cond
    (string? data) (str (count data) ":" data)
    (number? data) (str "i" data "e")
    (keyword? data) (encode-bencode (name data))
    (map? data) (str "d" 
                     (apply str (mapcat (fn [[k v]]
                                          [(encode-bencode k)
                                           (encode-bencode v)])
                                        data))
                     "e")
    (sequential? data) (str "l" (apply str (map encode-bencode data)) "e")
    :else (encode-bencode (str data))))

(defn decode-bencode [s]
  (let [stream (java.io.PushbackInputStream. 
                (java.io.ByteArrayInputStream. (.getBytes s)))]
    (letfn [(read-until [ch]
              (loop [result []]
                (let [b (.read stream)]
                  (if (= b (int ch))
                    (apply str result)
                    (recur (conj result (char b)))))))
            (read-string []
              (let [len-str (read-until \:)
                    len (parse-long len-str)
                    bytes (byte-array len)]
                (.read stream bytes)
                (String. bytes)))
            (decode-value []
              (let [b (.read stream)]
                (case (char b)
                  \d (loop [m {}]
                       (let [peek (.read stream)]
                         (if (= peek (int \e))
                           m
                           (do (.unread stream peek)
                               (let [k (keyword (decode-value))
                                     v (decode-value)]
                                 (recur (assoc m k v)))))))
                  \l (loop [v []]
                       (let [peek (.read stream)]
                         (if (= peek (int \e))
                           v
                           (do (.unread stream peek)
                               (recur (conj v (decode-value)))))))
                  \i (parse-long (read-until \e))
                  (do (.unread stream b)
                      (read-string)))))]
      (decode-value))))

(defn send-and-receive [host port message]
  (with-open [socket (java.net.Socket. host port)
              out (.getOutputStream socket)
              in (java.io.BufferedInputStream. (.getInputStream socket))]
    ;; Send the message
    (.write out (.getBytes (encode-bencode message)))
    (.flush out)
    
    ;; Read responses
    (let [responses (atom [])]
      (loop [attempts 0]
        (Thread/sleep 50) ; Give server time to respond
        (when (or (pos? (.available in)) (< attempts 3))
          (if (pos? (.available in))
            (let [buf (byte-array (.available in))
                  _ (.read in buf)
                  response (decode-bencode (String. buf))]
              (swap! responses conj response)
              (if (and (:status response)
                       (some #{"done"} (:status response)))
                @responses
                (recur 0)))
            (recur (inc attempts)))))
      @responses)))

(defn eval-code [code & {:keys [host port] 
                          :or {host "localhost" port 1667}}]
  (let [responses (send-and-receive host port {:op "eval" :code code})]
    (if-let [value-response (first (filter :value responses))]
      (:value value-response)
      (if-let [err-response (first (filter :err responses))]
        (str "Error: " (:err err-response))
        "No value returned"))))

;; Main execution
(when (= *file* (System/getProperty "babashka.file"))
  (let [args *command-line-args*
        code (first args)
        port (if (second args) 
               (parse-long (second args))
               1667)]
    (if code
      (println (eval-code code :port port))
      (println "Usage: bb client.bb \"(+ 1 2 3)\" [port]"))))