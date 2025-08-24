;; Auto-require Fireworks for REPL sessions
;; Load this with: (load-file "scripts/fireworks-init.clj")

(try
  (require '[fireworks.core :refer [? !? ?> !?>]])
  (println "🎆 Fireworks loaded! Available functions:")
  (println "  (? data)   - Pretty print with form info")
  (println "  (!? data)  - Silent version") 
  (println "  (?> data)  - Send to tap>")
  (println "  (!?> data) - Silent tap>")
  (println "")
  (println "Example: (? {:hello \"world\" :data [1 2 3]})")
  (catch Exception e
    (println "⚠️ Could not load Fireworks:" (.getMessage e))))