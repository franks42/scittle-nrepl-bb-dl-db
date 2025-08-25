# Super Duper BB Server - Development Context

## Completed Features
- ✅ Multi-service state management in `.bb-super-duper-server`
- ✅ HTTP server on preferred port 37373 with fallback (http_server.clj)
- ✅ Datalevin pod integration (datalevin_service.clj)
- ✅ Test scripts for all components with 100% success

## WebSocket Architecture (CRITICAL)
- **WebSocket server comes from `sci.nrepl.browser-server/start!`** (Scittle library)
- Port 1340 hardcoded everywhere, needs dynamic
- **HTML embeds port via**: `<script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>`
- Template exists: `/template.html` with `{{WEBSOCKET_PORT}}` substitution
- HTML generator exists: `html_generator.clj`

## Bidirectional Bridge Pattern
```clojure
enhanced-websocket-handler routes by :direction field:
- "to-browser" → forward-to-browser  
- "to-babashka" → handle-server-request
```

## Key Files
- `websocket-nrepl-dispatch.clj` - Complete bridge architecture (hot-loadable)
- `scittle-server.clj` - Has `start-scittle-nrepl!` function
- `template.html` - HTML template with port placeholders

## Next Steps (TODO)
1. WebSocket discovery API endpoint on HTTP server
2. Dynamic HTML generation with discovered ports
3. nREPL Server #2 (BB→Browser proxy)
4. Orchestrated startup with proper service dependencies
5. Update bb.edn with super-duper-server task

## Important Notes
- Scittle libs only available in Scittle directory context
- httpkit available for WebSocket (has `with-channel`)
- Port discovery critical for dynamic HTML generation
- Test everything autonomously before integration

## Git Tags
- v1.2.0-phase1: Multi-service state
- v1.2.1-http-server: HTTP with port 37373
- v1.2.2-datalevin: Datalevin integration