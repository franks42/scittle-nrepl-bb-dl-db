#!/usr/bin/env bb

;; Help script for nREPL utilities
;; Usage: ./nrepl-help.bb

(println "
╔══════════════════════════════════════════════════════════════╗
║                    nREPL Utility Scripts                      ║
╚══════════════════════════════════════════════════════════════╝

These Babashka scripts provide simple command-line access to any
nREPL server without needing the MCP infrastructure.

AVAILABLE SCRIPTS:
─────────────────

  nrepl-client.bb    - Core client library (used by other scripts)
  nrepl-eval.bb      - Evaluate Clojure code
  nrepl-load-file.bb - Load a Clojure file
  nrepl-help.bb      - This help message

QUICK START:
───────────

  1. Start an nREPL server:
     bb --nrepl-server 1667 &

  2. Evaluate code:
     ./nrepl-eval.bb \"(+ 1 2 3)\"

  3. Load files:
     ./nrepl-load-file.bb my-code.clj

USAGE EXAMPLES:
──────────────

  # Simple evaluation
  ./nrepl-eval.bb \"(+ 1 2 3)\"

  # Define a function
  ./nrepl-eval.bb \"(defn square [x] (* x x))\"

  # Call the function
  ./nrepl-eval.bb \"(square 5)\"

  # Load a namespace
  ./nrepl-load-file.bb src/my_namespace.clj

  # Use a different port
  ./nrepl-eval.bb \"(+ 1 2)\" 7890

  # Connect to remote host
  ./nrepl-eval.bb \"(+ 1 2)\" 7890 remote-host

BOOTSTRAP PATTERN:
─────────────────

  # Start minimal server
  bb --nrepl-server 7890 &
  
  # Bootstrap your system
  ./nrepl-load-file.bb bootstrap.clj 7890
  ./nrepl-eval.bb \"(start-system!)\" 7890
  
  # Now system is ready for MCP or other clients

STATEFUL SERVER:
───────────────

  The nREPL server maintains state between calls:
  
  ./nrepl-eval.bb \"(def counter (atom 0))\"
  ./nrepl-eval.bb \"(swap! counter inc)\"  # Returns: 1
  ./nrepl-eval.bb \"(swap! counter inc)\"  # Returns: 2
  ./nrepl-eval.bb \"@counter\"              # Returns: 2

TIPS:
────

  • Default port is 1667 if not specified
  • Server runs in background with '&'
  • State persists across client calls
  • Use 'kill' or 'pkill' to stop servers
  • Check .nrepl-port files for auto-discovery

For more information, see the source of each script.
")