(ns bb-nrepl-server
  (:require [babashka.nrepl.server :as nrepl]))

(defn start-server [port]
  (println (str "Starting nREPL server on port " port "..."))
  (nrepl/start-server! {:host "localhost" :port port})
  (println (str "nREPL server started on localhost:" port))
  (println "Press Ctrl+C to stop")
  @(promise))

(defn -main [& args]
  (let [port (if (seq args)
               (Integer/parseInt (first args))
               7890)]
    (start-server port)))