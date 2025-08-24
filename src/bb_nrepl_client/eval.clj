(ns bb-nrepl-client.eval
  "Convenience wrapper for nREPL evaluation"
  (:require [bb-nrepl-client.client :as client]))

(defn -main
  "Main entry point for nREPL eval command"
  [& args]
  (let [[code port-str host] args
        port (if port-str
               (parse-long port-str)
               (or (client/get-server-port) 1667))
        host (or host "localhost")]
    (if code
      (println (client/eval-code code :host host :port port))
      (do
        (println "nrepl-eval: Evaluate Clojure code via nREPL")
        (println "")
        (println "Usage: bb run nrepl-eval <code> [port] [host]")
        (println "")
        (println "Examples:")
        (println "  bb run nrepl-eval \"(+ 1 2 3)\"")
        (println "  bb run nrepl-eval \"(System/getProperty \\\"user.dir\\\")\" 7890")
        (println "  bb run nrepl-eval \"@counter\" 7890 remote-host")
        (System/exit 1)))))