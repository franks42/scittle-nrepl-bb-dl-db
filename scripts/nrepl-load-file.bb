#!/usr/bin/env bb

;; Convenience script for loading files via nREPL
;; Usage: ./nrepl-load-file.bb <file-path> [port] [host]
;;    or: bb nrepl-load-file.bb <file-path> [port] [host]
;;
;; Examples:
;;   ./nrepl-load-file.bb bootstrap.clj           # Uses localhost:1667
;;   ./nrepl-load-file.bb src/my_ns.clj 7890      # Uses localhost:7890
;;   ./nrepl-load-file.bb init.clj 7890 myhost    # Uses myhost:7890

(require '[clojure.java.io :as io])

(load-file "nrepl-client.bb")

(let [[file-path port-str host] *command-line-args*
      port (if port-str (parse-long port-str) 1667)
      host (or host "localhost")]
  (cond
    (nil? file-path)
    (do
      (println "nrepl-load-file: Load a Clojure file via nREPL")
      (println "")
      (println "Usage: ./nrepl-load-file.bb <file-path> [port] [host]")
      (println "")
      (println "Examples:")
      (println "  ./nrepl-load-file.bb bootstrap.clj")
      (println "  ./nrepl-load-file.bb src/my_namespace.clj 7890")
      (println "  ./nrepl-load-file.bb /absolute/path/to/file.clj 7890 remote-host")
      (System/exit 1))
    
    (not (.exists (io/file file-path)))
    (do
      (println (str "Error: File not found: " file-path))
      (System/exit 1))
    
    :else
    (let [file-content (slurp file-path)
          ;; Use load-file form for proper file loading semantics
          code (str "(load-file \"" (.getAbsolutePath (io/file file-path)) "\")")]
      (println (str "Loading " file-path "..."))
      (let [result (nrepl-client/eval-code code :host host :port port)]
        (println result)
        ;; Also try to get the namespace if it was a ns declaration
        (when (.contains file-content "(ns ")
          (let [ns-result (nrepl-client/eval-code "*ns*" :host host :port port)]
            (when-not (= ns-result "No value returned")
              (println (str "Loaded into namespace: " ns-result)))))))))