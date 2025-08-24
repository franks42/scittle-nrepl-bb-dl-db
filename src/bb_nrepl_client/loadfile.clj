(ns bb-nrepl-client.loadfile
  "Convenience wrapper for loading files via nREPL"
  (:require [bb-nrepl-client.client :as client]
            [clojure.java.io :as io]))

(defn -main
  "Main entry point for nREPL load-file command"
  [& args]
  (let [[file-path port-str host] args
        port (if port-str
               (parse-long port-str)
               (or (client/get-server-port) 1667))
        host (or host "localhost")]
    (cond
      (nil? file-path)
      (do
        (println "nrepl-load-file: Load a Clojure file via nREPL")
        (println "")
        (println "Usage: bb run nrepl-load-file <file-path> [port] [host]")
        (println "")
        (println "Examples:")
        (println "  bb run nrepl-load-file bootstrap.clj")
        (println "  bb run nrepl-load-file src/my_namespace.clj 7890")
        (println "  bb run nrepl-load-file /absolute/path/to/file.clj 7890 remote-host")
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
        (let [result (client/eval-code code :host host :port port)]
          (println result)
          ;; Also try to get the namespace if it was a ns declaration
          (when (.contains file-content "(ns ")
            (let [ns-result (client/eval-code "*ns*" :host host :port port)]
              (when-not (= ns-result "No value returned")
                (println (str "Loaded into namespace: " ns-result))))))))))