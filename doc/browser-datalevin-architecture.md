# Browser-to-Datalevin Architecture Plan

## Overview

Architecture options for enabling browser-based ClojureScript applications to interact with Datalevin database, comparing HTTP proxy vs existing WebSocket nREPL-eval approaches.

## Architecture Options

### Option A: WebSocket nREPL-eval (RECOMMENDED - Leverage Existing)

```
┌─────────────────┐    WebSocket     ┌──────────────────┐    Pod API    ┌─────────────┐
│   Browser       │◄────────────────►│  Babashka        │◄─────────────►│  Datalevin  │
│   Scittle/SCI   │   nREPL msgs    │  nREPL Server    │               │  Database   │
└─────────────────┘                  └──────────────────┘               └─────────────┘
│                                    │                                  │
├── WebSocket API                    ├── sci.nrepl.browser-server       ├── LMDB files
├── EDN messages                     ├── nREPL protocol                 ├── ACID txns
├── Direct evaluation               ├── Code evaluation                ├── Datalog
└── Real-time                       └── Session management             └── Indexing
```

### Option B: HTTP Proxy (Alternative)

```
┌─────────────────┐    HTTP/JSON     ┌──────────────────┐    Pod API    ┌─────────────┐
│   Browser       │◄────────────────►│  Babashka        │◄─────────────►│  Datalevin  │
│   ClojureScript │                  │  HTTP Server     │               │  Database   │
└─────────────────┘                  └──────────────────┘               └─────────────┘
│                                    │                                  │
├── cljs-http                        ├── Ring handlers                  ├── LMDB files
├── Fetch API                        ├── JSON parsing                   ├── ACID txns
├── Standard REST                    ├── Query routing                  ├── Datalog
└── Authentication                   └── Error handling                 └── Indexing
```

## API Comparison

### Option A: WebSocket nREPL-eval API

**Setup (Server)**:
```clojure
;; Start in Babashka with Datalevin pod loaded
(require '[sci.nrepl.browser-server :as nrepl])
(nrepl/start! {:nrepl-port 1339 :websocket-port 1340})
```

**Setup (Browser)**:
```html
<script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>
<script src="https://cdn.jsdelivr.net/npm/scittle@0.7.26/dist/scittle.nrepl.js"></script>
```

**Usage (Browser)**:
```clojure
;; Direct Clojure evaluation in browser
(d/q '[:find ?name ?role 
       :where [?e :person/name ?name]
              [?e :person/role ?role]] 
     (d/db conn))

;; Direct transactions
(d/transact! conn [{:person/name "Alice" 
                    :person/email "alice@example.com"}])

;; Helper functions work directly
(find-by-role "developer")
(database-stats)
```

### Option B: HTTP Proxy API

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `/api/query` | POST | Execute Datalog queries | `{:query [...] :args [...]}` | Query results |
| `/api/transact` | POST | Submit transactions | `{:tx-data [...]}` | Transaction report |
| `/api/entity/:id` | GET | Pull entity by ID | URL param | Entity map |
| `/api/schema` | GET | Get database schema | None | Schema definition |
| `/api/stats` | GET | Database statistics | None | Stats map |

## Architecture Comparison

| Aspect | WebSocket nREPL-eval | HTTP Proxy |
|--------|---------------------|------------|
| **Infrastructure** | ✅ Already exists | ❌ Needs implementation |
| **Setup Complexity** | ✅ 2 lines of code | ❌ Ring handlers, routing |
| **Browser Integration** | ✅ Direct Clojure code | ❌ HTTP client + serialization |
| **Real-time Updates** | ✅ WebSocket connection | ❌ Polling required |
| **Development Speed** | ✅ Immediate | ❌ API design + implementation |
| **Code Reuse** | ✅ Same code server/browser | ❌ Different patterns |
| **Error Handling** | ✅ Native Clojure exceptions | ❌ HTTP status codes |
| **Session Management** | ✅ Built-in nREPL sessions | ❌ Custom implementation |
| **Security** | ⚠️ Code evaluation risk | ✅ Controlled endpoints |
| **Performance** | ✅ Direct evaluation | ❌ Serialization overhead |
| **Standard Compliance** | ❌ nREPL protocol | ✅ HTTP REST |
| **Debugging** | ✅ Full REPL capabilities | ❌ Limited to endpoints |

## Recommendation: WebSocket nREPL-eval Approach

**Why this is the superior choice:**

### ✅ **Zero Additional Implementation**
```clojure
;; Everything already works!
;; Server: Babashka + Datalevin pod + sci.nrepl.browser-server  
;; Browser: Scittle + WebSocket nREPL

;; One setup step:
(nrepl/start! {:nrepl-port 1339 :websocket-port 1340})
```

### ✅ **Identical Code Patterns**
```clojure
;; Same queries work in both environments
(d/q '[:find ?name :where [?e :person/name ?name]] (d/db conn))

;; Same transactions
(d/transact! conn [{:person/name "Alice"}])

;; Same helper functions  
(find-by-role "developer")
```

### ✅ **Full REPL Experience**
- Interactive development directly in browser
- Complete error messages and stack traces
- Dynamic function redefinition
- Live debugging capabilities

### ✅ **Real-time Collaboration**
- Multiple browser windows connecting to same nREPL
- Live code sharing between developers
- Persistent sessions across browser refreshes

### ❌ **Security Considerations**
- **Risk**: Direct code evaluation in browser context
- **Mitigation**: Controlled environment, authentication, sandboxing

## Implementation Plan - WebSocket nREPL-eval

### Phase 1: Basic Setup (< 1 hour)
1. Add `sci.nrepl.browser-server` to BB startup script
2. Include Scittle nREPL in HTML page
3. Test basic evaluation from browser

### Phase 2: Datalevin Integration (< 2 hours)  
1. Load Datalevin pod in BB nREPL context
2. Test direct queries from browser
3. Create shared helper functions

### Phase 3: Production Hardening
1. Authentication for WebSocket connections
2. Code evaluation sandboxing
3. Session management and cleanup
4. Error handling and logging

## Request/Response Formats

### Query Request
```json
{
  "query": ["find", "?name", "?role", 
           "where", ["?e", ":person/name", "?name"],
                   ["?e", ":person/role", "?role"]],
  "args": [],
  "limit": 100,
  "offset": 0
}
```

### Transaction Request
```json
{
  "tx-data": [
    {":person/name": "Alice Smith",
     ":person/email": "alice@example.com",
     ":person/role": "developer"},
    {":db/id": 123,
     ":person/skills": ["Clojure", "ClojureScript"]}
  ]
}
```

### Response Format
```json
{
  "status": "success",
  "data": [...],
  "meta": {
    "count": 42,
    "execution-time-ms": 15,
    "query-id": "uuid"
  }
}
```

## Implementation Strategy

### Phase 1: Basic HTTP Proxy
- [ ] Set up Babashka HTTP server with Ring
- [ ] Implement basic query and transact endpoints
- [ ] JSON serialization/deserialization
- [ ] Error handling and status codes

### Phase 2: Enhanced API
- [ ] Pull API endpoint
- [ ] Entity retrieval by ID
- [ ] Schema introspection
- [ ] Query explain functionality

### Phase 3: Production Features
- [ ] Authentication and authorization
- [ ] Rate limiting and throttling
- [ ] Query caching layer
- [ ] Connection pooling
- [ ] Logging and monitoring

### Phase 4: Browser Integration
- [ ] ClojureScript client library
- [ ] Connection management
- [ ] Offline support patterns
- [ ] Real-time updates (WebSocket)

## Security Considerations

### Authentication
- Token-based authentication (JWT)
- Session management
- Role-based access control

### Validation
- Query validation and sanitization
- Transaction data validation
- Input size limits
- SQL injection prevention (Datalog context)

### Rate Limiting
- Request throttling per client
- Query complexity analysis
- Resource usage monitoring

## Code Structure

```
src/
├── bb_http_server/
│   ├── core.clj           # Main HTTP server
│   ├── handlers.clj       # Ring request handlers
│   ├── middleware.clj     # Auth, CORS, logging
│   └── datalevin.clj      # Database operations
├── browser_client/
│   ├── core.cljs          # Main client API
│   ├── queries.cljs       # Query helpers
│   └── transactions.cljs  # Transaction helpers
└── shared/
    ├── schema.cljc        # Shared schema definitions
    └── validation.cljc    # Shared validation
```

## Performance Considerations

### Caching Strategy
- Query result caching with TTL
- Schema caching (rarely changes)
- Connection pooling for Datalevin

### Optimization
- Query batching capabilities
- Streaming for large result sets
- Compression for large responses
- CDN for static assets

## Error Handling

### Client Errors (4xx)
- 400: Malformed query/transaction
- 401: Authentication required
- 403: Insufficient permissions
- 404: Entity not found
- 429: Rate limit exceeded

### Server Errors (5xx)
- 500: Internal server error
- 503: Database unavailable
- 504: Query timeout

## Monitoring and Observability

### Metrics
- Request count and latency
- Query execution times
- Database connection health
- Error rates by endpoint

### Logging
- Request/response logging
- Query performance logging
- Error and exception logging
- Security event logging

## Migration Path

### Current State
- Datalevin pod working in Babashka
- MCP nREPL integration functional
- Basic query/transaction capabilities

### Next Steps
1. Implement basic HTTP proxy server
2. Create ClojureScript client library
3. Test with simple browser application
4. Add production features incrementally

### Future Considerations
- Migration to official Datalevin JSON API (v1.2.0+)
- WebSocket support for real-time updates
- GraphQL layer for complex queries
- Multi-database support

## Testing Strategy

### Unit Tests
- Handler function testing
- JSON serialization/deserialization
- Query validation logic
- Error handling scenarios

### Integration Tests
- End-to-end HTTP request/response
- Database transaction testing
- Authentication flow testing
- Performance benchmarking

### Browser Tests
- ClojureScript client functionality
- Cross-browser compatibility
- Network error handling
- Offline behavior

## Deployment Options

### Development
- Local Babashka HTTP server
- In-memory or file-based Datalevin
- Hot reloading for rapid development

### Production
- Containerized Babashka application
- Persistent Datalevin with LMDB
- Load balancer for multiple instances
- Monitoring and alerting setup

This architecture provides a practical path for browser-to-Datalevin integration while maintaining flexibility for future enhancements and official API migrations.