# MCP + Datalevin Test Workflow

Complete workflow for testing Datalevin database through MCP nREPL tools.

## Architecture Decision: Datalevin as Primary Database

**Decision**: Use Datalevin (dl-db) as the primary database solution for the project.

**Key Findings**:
- ✅ **DataScript NOT built into Babashka** - Requires custom compilation with feature flags
- ✅ **Datalevin available as pod** - Ready to use, already tested and working
- ✅ **High API compatibility** - Datalevin started as DataScript port, same Datalog queries
- ✅ **Production-ready** - LMDB backend, ACID transactions, persistent storage
- ✅ **Better performance** - Cost-based query optimizer, faster than DataScript for large datasets

**Future Considerations**:
- Browser-local DataScript sync is low priority optimization
- Focus on direct browser-to-Datalevin communication patterns
- ClojureScript-specific entity filtering for browser interactions

## Browser-to-Datalevin Communication Options

### Current Status (2025)
- ❌ **No native HTTP REST API** - Datalevin uses proprietary client/server protocol (port 8898)
- ❌ **No ClojureScript browser client** - Direct browser access not available yet
- ⏳ **JSON API planned** - Version 1.2.0 roadmap includes JSON API for popular languages
- ✅ **Babashka HTTP proxy possible** - Can create Ring handler proxy layer

### Recommended Approach: Babashka HTTP Proxy

**Architecture**: Browser ↔ HTTP/JSON ↔ Babashka Ring Handler ↔ Datalevin Pod

**Components**:
1. **Browser ClojureScript** - Standard HTTP client (cljs-http, fetch API)
2. **Babashka HTTP Server** - Ring handler with babashka.http-server
3. **Datalevin Pod** - Database operations via pod.huahaiy.datalevin
4. **JSON Bridge** - Transform HTTP requests to Datalevin queries/transactions

**Benefits**:
- ✅ **Available now** - All components exist and work together
- ✅ **Standard HTTP** - Browser can use familiar REST patterns
- ✅ **Babashka integration** - Leverages existing BB+Datalevin setup
- ✅ **Custom API design** - Full control over endpoint structure
- ✅ **Security layer** - Add authentication, validation, filtering

### Implementation Patterns

```clojure
;; HTTP API endpoints for browser communication
POST /api/query     - Execute Datalog queries
POST /api/transact  - Submit transactions
GET  /api/entity/:id - Pull entity by ID
GET  /api/schema    - Get database schema
```

**Example request from browser**:
```javascript
// ClojureScript browser code
(POST "/api/query" 
  {:query '[:find ?name ?role 
            :where [?e :person/name ?name]
                   [?e :person/role ?role]]
   :args []})
```

**Babashka Ring handler**:
```clojure
;; Transform HTTP to Datalevin
(defn query-handler [request]
  (let [{:keys [query args]} (:body request)
        result (d/q query (d/db conn) args)]
    {:status 200 
     :body (json/write-str result)}))
```

### Alternative: Wait for Official JSON API

**Timeline**: Datalevin 1.2.0 (no specific release date)
**Benefits**: Official support, optimized protocol, wider language support
**Risk**: Unknown timeline, may not fit project needs

### Recommendation

**Proceed with Babashka HTTP proxy approach** for immediate browser integration capability while maintaining flexibility to migrate to official JSON API when available.

## Prerequisites

- MCP nREPL server running (assumed to be available)
- Scripts in current directory:
  - `bb-start.sh` - Start Babashka nREPL server
  - `bb-stop.sh` - Stop Babashka nREPL server  
  - `datalevin-setup.clj` - Initialize Datalevin connection
  - `datalevin-test-data.clj` - Add sample data
  - `datalevin-queries.clj` - Run comprehensive queries

## Step-by-Step Workflow

### 1. Start Babashka nREPL Server

```bash
./bb-start.sh
```

Expected output:
```
🚀 Starting Babashka nREPL Server for Datalevin
Database path: /var/db/datalevin/cljcodedb
✅ Database directory ready: /var/db/datalevin/cljcodedb
📡 Starting Babashka nREPL server on port 0...
⏳ Waiting for nREPL server to start...
✅ Babashka nREPL server started successfully!
   Port: 54817
   PID: 12345
```

### 2. Connect via MCP nREPL Tools

```clojure
;; Connect to the nREPL server
(mcp__nrepl_mcp_server__nrepl-connection 
  {:op "connect" :connection "54817"})
```

Expected result:
```clojure
{:status "success"
 :operation "connect" 
 :hostname "localhost"
 :port 54817
 :connection-id "..."
 :message "Connected to nREPL server at localhost:54817"}
```

### 3. Initialize Datalevin Database

```clojure
;; Load the setup script
(mcp__nrepl_mcp_server__nrepl-load-file 
  {:file-path "/Users/franksiebenlist/Development/scittle/datalevin-setup.clj"})
```

Expected output shows:
- Datalevin pod loading
- Database connection established
- Available functions listed
- Current database stats

### 4. Add Test Data

```clojure
;; Load test data
(mcp__nrepl_mcp_server__nrepl-load-file 
  {:file-path "/Users/franksiebenlist/Development/scittle/datalevin-test-data.clj"})
```

Expected output shows:
- Sample people inserted
- Data verification
- Sample query results

### 5. Run Comprehensive Queries

```clojure
;; Run query tests
(mcp__nrepl_mcp_server__nrepl-load-file 
  {:file-path "/Users/franksiebenlist/Development/scittle/datalevin-queries.clj"})
```

Expected output shows:
- Basic queries (names, roles)
- Parameterized queries (by role)
- Aggregations (counts, averages)
- Predicate queries (age ranges)
- Collection queries (skills)
- Pull API examples
- Complex multi-join queries
- Database statistics

### 6. Interactive Testing

After loading scripts, test individual functions:

```clojure
;; Test individual queries
(mcp__nrepl_mcp_server__nrepl-eval 
  {:code "(find-by-role \"developer\")"})

(mcp__nrepl_mcp_server__nrepl-eval 
  {:code "(find-by-skill \"Clojure\")"})

(mcp__nrepl_mcp_server__nrepl-eval 
  {:code "(database-stats)"})

;; Add new person
(mcp__nrepl_mcp_server__nrepl-eval 
  {:code "(add-person {:person/name \"Test User\" 
                       :person/email \"test@example.com\" 
                       :person/role \"tester\" 
                       :person/age 25 
                       :person/skills [\"Testing\" \"QA\"]})"})

;; Verify addition
(mcp__nrepl_mcp_server__nrepl-eval 
  {:code "(find-by-name \"Test User\")"})
```

### 7. Persistence Testing

Close and reconnect to verify data persistence:

```clojure
;; Close connection
(mcp__nrepl_mcp_server__nrepl-eval {:code "(d/close conn)"})

;; Disconnect from nREPL
(mcp__nrepl_mcp_server__nrepl-connection {:op "disconnect"})
```

Stop and restart server:
```bash
./bb-stop.sh
./bb-start.sh
```

Reconnect and verify data persists:
```clojure
;; Reconnect
(mcp__nrepl_mcp_server__nrepl-connection {:op "connect" :connection "NEW_PORT"})

;; Reload setup (without test data to avoid duplicates)
(mcp__nrepl_mcp_server__nrepl-load-file 
  {:file-path "/Users/franksiebenlist/Development/scittle/datalevin-setup.clj"})

;; Check existing data
(mcp__nrepl_mcp_server__nrepl-eval {:code "(database-stats)"})
```

### 8. Cleanup

```bash
./bb-stop.sh
```

## Expected Results

After completing the workflow:

- **Database Files**: `/var/db/datalevin/cljcodedb/` contains `data.mdb` and `lock.mdb`
- **Sample Data**: 6+ people with various roles and skills
- **All Queries Work**: Basic, complex, aggregations, pull API
- **Persistence Verified**: Data survives server restarts
- **MCP Integration**: All operations work through nrepl-eval/load-file

## Troubleshooting

### Server Won't Start
- Check if port is already in use
- Ensure `/var/db/datalevin/cljcodedb/` is writable
- Try different port: `./bb-start.sh 8888`

### Connection Failed
- Verify server is running: `ps aux | grep bb`
- Check port in `.nrepl-port` file
- Ensure MCP nREPL server is available

### Queries Return Empty
- Verify data was loaded: `(database-stats)`
- Check connection: `(bound? #'conn)`
- Reload setup if needed

### Pod Loading Failed
- Check internet connection (pod downloads from registry)
- Try manual pod download
- Verify Babashka version supports pods

## Database Schema

```clojure
{:person/name   {:db/cardinality :db.cardinality/one}
 :person/email  {:db/cardinality :db.cardinality/one
                 :db/unique :db.unique/identity}
 :person/role   {:db/cardinality :db.cardinality/one}
 :person/skills {:db/cardinality :db.cardinality/many}
 :person/age    {:db/cardinality :db.cardinality/one}
 :project/name  {:db/cardinality :db.cardinality/one}
 :project/lead  {:db/cardinality :db.cardinality/one
                 :db/valueType :db.type/ref}}
```

## Available Functions

After loading `datalevin-setup.clj`:

- `(add-person person-map)` - Add single person
- `(add-people people-vector)` - Add multiple people
- `(find-by-name name)` - Find person by name
- `(find-by-role role)` - Find people by role
- `(find-by-skill skill)` - Find people with skill
- `(count-by-role)` - Count people per role
- `(all-people)` - Get all people
- `(database-stats)` - Get database statistics