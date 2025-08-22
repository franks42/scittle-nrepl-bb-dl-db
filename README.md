# scittle-nrepl-bb-dl-db

Database-Native Clojure Development Environment combining:
- **Scittle**: ClojureScript in the browser
- **nREPL**: Network REPL for remote evaluation
- **Babashka**: Fast Clojure scripting
- **Datalevin**: Durable Datalog database

## Quick Start

### 1. Start Development Environment

```bash
# Start both nREPL and HTTP server
bb dev

# Or start components separately:
bb browser-nrepl  # nREPL on 1339, WebSocket on 1340
bb http-server    # HTTP server on 1341
```

### 2. Open Browser

Navigate to http://localhost:1341

### 3. Connect Your Editor

Connect your editor's nREPL client to `localhost:1339`

- **CIDER (Emacs)**: `M-x cider-connect-cljs` → Select port 1339 → Choose `nbb` REPL type
- **Calva (VS Code)**: Connect to Running REPL → Babashka → localhost:1339
- **Cursive (IntelliJ)**: Remote nREPL → localhost:1339

## Architecture

```
┌─────────────────┐    WebSocket     ┌──────────────────┐    Pod API    ┌─────────────┐
│   Browser       │◄────────────────►│  Babashka        │◄─────────────►│  Datalevin  │
│   Scittle       │      :1340       │  nREPL Server    │               │  Database   │
└─────────────────┘                  └──────────────────┘               └─────────────┘
        ▲                                     ▲
        │                                     │
    HTTP :1341                           nREPL :1339
        │                                     │
        ▼                                     ▼
    index.html                          Your Editor
```

## Project Structure

```
.
├── bb.edn           # Babashka configuration and tasks
├── public/          # Static files served by HTTP server
│   └── index.html   # Browser application
└── README.md        # This file
```

## Key Innovation

This setup enables:
- **Browser-based ClojureScript** evaluation via Scittle
- **Remote REPL access** from your favorite editor
- **Persistent database** integration with Datalevin (coming soon)
- **Minimal dependencies** - just Babashka and a browser!

## Next Steps

- [ ] Add Datalevin pod integration
- [ ] Implement database-native code storage
- [ ] Add CodeMirror editor with Clojure support
- [ ] Create live dependency management system

## Requirements

- Babashka (bb) installed
- Modern web browser
- Your favorite Clojure editor with nREPL support
