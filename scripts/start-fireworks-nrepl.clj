#!/usr/bin/env bb

;; Start Babashka nREPL server with Fireworks pretty-printing
(ns fireworks-nrepl
  (:require [babashka.nrepl.server :as nrepl]
            [clojure.pprint :as pprint]))

;; Try to set up pretty printing
(try
  ;; Fireworks might not be available in BB, so let's create our own simple version
  (defn ? 
    "Simple pretty-print with label"
    [x]
    (println "🎆 =>" (pr-str x))
    x)
  
  (defn !? 
    "Silent version - just returns value"
    [x] 
    x)
  
  ;; Simple color functions for terminal
  (defn color-str [color s]
    (case color
      :red (str "\u001b[31m" s "\u001b[0m")
      :green (str "\u001b[32m" s "\u001b[0m")
      :blue (str "\u001b[34m" s "\u001b[0m")
      :yellow (str "\u001b[33m" s "\u001b[0m")
      :magenta (str "\u001b[35m" s "\u001b[0m")
      :cyan (str "\u001b[36m" s "\u001b[0m")
      s))
  
  (defn pp 
    "Pretty print with colors"
    [x]
    (println (color-str :cyan "🎆 Result:"))
    (pprint/pprint x)
    x)
  
  (println (color-str :green "✅ Fireworks-style functions loaded!"))
  (println "Available: ? !? pp color-str")
  
  (catch Exception e
    (println "⚠️ Could not load Fireworks, using basic pretty-printing")))

;; Find available port
(defn find-available-port []
  (loop [port 7890]
    (if (try
          (with-open [socket (java.net.ServerSocket. port)] true)
          (catch Exception _ false))
      port
      (recur (inc port)))))

(def port (find-available-port))

(println (str "\n🚀 Starting Babashka nREPL server on port " port))
(println "📡 Connect with: clojure -M:rebel -m nrepl.cmdline --connect --port" port)
(println "   or with Rebel: rebel-readline --nrepl-port" port)
(println "\n💡 Try these in your REPL:")
(println "  (? {:hello \"world\" :data [1 2 3]})")
(println "  (pp (range 100))")
(println "  (color-str :green \"Success!\")")

;; Start the server
(nrepl/start-server! {:port port})

;; Keep the server running
(println (str "\n✨ Server running on port " port " - Press Ctrl+C to stop"))
@(promise)