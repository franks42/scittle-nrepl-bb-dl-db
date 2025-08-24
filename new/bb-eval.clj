#!/usr/bin/env bb
;; Simple one-shot nREPL eval using netcat-like approach

(require '[clojure.java.io :as io]
         '[clojure.string :as str])

(def code (first *command-line-args*))

;; Very simple bencode just for our use case
(defn encode [msg]
  (str "d"
       "2:op" (str (count "eval") ":eval")
       "4:code" (str (count code) ":" code)
       "e"))

(defn parse-response [s]
  ;; Quick and dirty - just extract the value if present
  (when-let [match (re-find #"5:value(\d+):(.+?)6:status" s)]
    (nth match 2)))

(when code
  (with-open [socket (java.net.Socket. "localhost" 1667)
              out (io/writer (.getOutputStream socket))
              in (io/reader (.getInputStream socket))]
    (.write out (encode {:op "eval" :code code}))
    (.flush out)
    
    ;; Read response (simplified - just grab first complete response)
    (Thread/sleep 50) ; Give server time to respond
    (let [buf (char-array 4096)
          n (.read in buf)
          response (String. buf 0 n)]
      (if-let [value (parse-response response)]
        (println value)
        (when (str/includes? response "value")
          ;; Try to extract value another way
          (when-let [m (re-find #"value\d+:([^e]+)" response)]
            (println (second m))))))))