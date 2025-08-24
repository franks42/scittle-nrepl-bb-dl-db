#!/usr/bin/env bb

;; Convenience script for nREPL evaluation
;; Usage: ./nrepl-eval.bb "(+ 1 2 3)" [port] [host]
;;    or: bb nrepl-eval.bb "(+ 1 2 3)" [port] [host]
;; 
;; Examples:
;;   ./nrepl-eval.bb "(+ 1 2 3)"              # Uses localhost:1667
;;   ./nrepl-eval.bb "(+ 1 2 3)" 7890         # Uses localhost:7890
;;   ./nrepl-eval.bb "(+ 1 2 3)" 7890 myhost  # Uses myhost:7890

(load-file (str (System/getProperty "user.dir") "/scripts/nrepl-client.bb"))

(let [[code port-str host] *command-line-args*
      port (if port-str (parse-long port-str) 1667)
      host (or host "localhost")]
  (if code
    (println (nrepl-client/eval-code code :host host :port port))
    (do
      (println "nrepl-eval: Evaluate Clojure code via nREPL")
      (println "")
      (println "Usage: ./nrepl-eval.bb <code> [port] [host]")
      (println "")
      (println "Examples:")
      (println "  ./nrepl-eval.bb \"(+ 1 2 3)\"")
      (println "  ./nrepl-eval.bb \"(System/getProperty \\\"user.dir\\\")\" 7890")
      (println "  ./nrepl-eval.bb \"@counter\" 7890 remote-host")
      (System/exit 1))))