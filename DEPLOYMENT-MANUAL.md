# Super Duper BB Server - Deployment Manual

## 🎯 Overview
This manual provides step-by-step instructions for deploying and operating the Super Duper BB Server environment. Suitable for both AI agents and human operators.

## 📋 Prerequisites

### System Requirements
- **Babashka**: Latest version installed and in PATH
- **Java**: JDK 11+ (required by Babashka)
- **Git**: For repository operations  
- **Modern Browser**: Chrome, Firefox, Safari, or Edge
- **Network**: Ports 37373+ available (or fallback to ephemeral)

### Verification Commands
```bash
# Verify prerequisites
bb --version        # Should show Babashka version
java --version      # Should show JDK 11+
git --version       # Should show Git version
```

## 🚀 Deployment Steps

### Phase 1: Repository Setup
```bash
# 1. Clone repository
git clone https://github.com/franks42/scittle-nrepl-bb-dl-db.git
cd scittle-nrepl-bb-dl-db

# 2. Verify bb.edn configuration
cat bb.edn

# 3. Test basic BB functionality
bb tasks
```

### Phase 2: Environment Initialization  
```bash
# 1. Initialize Datalevin database directory
mkdir -p /var/db/datalevin/cljcodedb

# 2. Verify BB nREPL client functionality
bb run test-nrepl-client

# 3. Test legacy nREPL server
bb run bb-dev-nrepl-server start
bb run bb-dev-nrepl-server status
bb run bb-dev-nrepl-server stop
```

### Phase 3: Super Duper Server Deployment
```bash
# 1. Start the complete server environment
bb run super-duper-server start

# Expected output:
# 🚀 Starting Super Duper BB Server...
# 📡 Phase 1: Core Services
#    ✅ nREPL Main:        localhost:54321
#    ✅ Datalevin:         Ready
#    ✅ Config file:       .bb-super-duper-server
# 🌐 Phase 2: Web Infrastructure  
#    ✅ HTTP Server:       localhost:37373
#    ✅ WebSocket:         ws://localhost:37373/ws
#    ✅ Dynamic HTML:      Generated with ports
# 🔗 Phase 3: nREPL Extensions
#    ✅ nREPL Proxy:       localhost:54322
#    ✅ WebSocket Bridge:  Ready
# ✅ Phase 4: Final Validation
# 🎉 Super Duper BB Server Ready!

# 2. Verify all services are running
bb run super-duper-server status

# 3. Run comprehensive test suite
bb run super-duper-server test

# 4. Open browser interface (optional)
bb run super-duper-server open
```

## 🧪 Verification & Testing

### Automated Testing
```bash
# Complete test suite
bb run super-duper-server test

# Individual service tests
bb src/test_http_service.clj
bb src/test_websocket_service.clj  
bb src/test_nrepl_services.clj
bb src/test_datalevin_service.clj
```

### Manual Verification
```bash
# 1. Check configuration file
cat .bb-super-duper-server

# 2. Test HTTP service manually
curl http://localhost:37373/
curl http://localhost:37373/api/discovery

# 3. Test nREPL services manually  
bb run nrepl-eval "(get-server-info)"
bb run nrepl-eval "(+ 1 2 3)"

# 4. Browser verification
open http://localhost:37373/
# Should show: Super Duper BB Server interface
# WebSocket should auto-connect
# Scittle REPL should be functional
```

## 🔧 Operations

### Daily Operations
```bash
# Start environment
bb run super-duper-server start

# Check status
bb run super-duper-server status  

# Open browser
bb run super-duper-server open

# Stop environment  
bb run super-duper-server stop
```

### Maintenance Operations
```bash
# Restart all services
bb run super-duper-server restart

# Run health checks
bb run super-duper-server test

# View logs (if implemented)
bb run super-duper-server logs

# Backup database (if implemented)
bb run super-duper-server backup
```

## 🚨 Troubleshooting

### Common Issues

#### Port Conflicts
**Symptom**: HTTP server fails to start on 37373
**Solution**: Server automatically falls back to ephemeral port
**Verification**: Check `bb run super-duper-server status` for actual port

#### Database Connection Issues  
**Symptom**: Datalevin errors during startup
**Solution**: 
1. Check database directory permissions: `ls -la /var/db/datalevin/`
2. Recreate directory: `rm -rf /var/db/datalevin/cljcodedb && mkdir -p /var/db/datalevin/cljcodedb`
3. Restart server: `bb run super-duper-server restart`

#### WebSocket Connection Failures
**Symptom**: Browser can't connect to WebSocket
**Solution**:
1. Verify HTTP server is running: `curl http://localhost:37373/api/discovery`
2. Check WebSocket URL in browser console
3. Test WebSocket service: `bb src/test_websocket_service.clj`

#### nREPL Service Issues
**Symptom**: nREPL eval commands fail
**Solution**:
1. Check service status: `bb run super-duper-server status`
2. Test individual services: `bb src/test_nrepl_services.clj`
3. Restart if needed: `bb run super-duper-server restart`

### Diagnostic Commands
```bash
# Show all running Java processes (includes BB)
jps -v

# Show port usage
lsof -i :37373
lsof -i :54321
lsof -i :54322

# Check configuration files
ls -la .bb-*
cat .bb-super-duper-server

# Validate BB environment
bb --version
bb tasks
bb -e "(+ 1 2 3)"
```

### Recovery Procedures

#### Complete Reset
```bash
# 1. Stop all services
bb run super-duper-server stop

# 2. Clean state files
rm -f .bb-super-duper-server .bb-dev-nrepl-server

# 3. Reset database (optional - loses data)
rm -rf /var/db/datalevin/cljcodedb
mkdir -p /var/db/datalevin/cljcodedb

# 4. Fresh start
bb run super-duper-server start
bb run super-duper-server test
```

#### Partial Service Restart
```bash
# Individual service management (if implemented)
bb run super-duper-server restart-http
bb run super-duper-server restart-nrepl  
bb run super-duper-server restart-websocket
```

## 📊 Success Criteria

### Deployment Success Indicators
- ✅ All 4 startup phases complete without errors
- ✅ `bb run super-duper-server status` shows all services running
- ✅ `bb run super-duper-server test` shows 100% test success
- ✅ HTTP interface loads at discovered URL
- ✅ WebSocket connects automatically in browser
- ✅ Scittle REPL functional in browser
- ✅ nREPL eval works from command line
- ✅ Database operations work from both BB and browser

### Performance Expectations
- **Startup time**: < 20 seconds total
- **HTTP response**: < 1 second for static assets  
- **nREPL eval**: < 2 seconds for simple expressions
- **WebSocket latency**: < 100ms round-trip
- **Memory usage**: < 200MB total for all services

## 🔄 Update Procedures

### Code Updates
```bash
# 1. Pull latest changes
git pull origin main

# 2. Stop current services
bb run super-duper-server stop

# 3. Start updated services  
bb run super-duper-server start

# 4. Verify functionality
bb run super-duper-server test
```

### Version Management
```bash
# Check current version
git describe --tags

# View available versions
git tag -l

# Deploy specific version
git checkout v1.2.0
bb run super-duper-server start
```

---

## 🤖 AI Agent Instructions

When deploying this environment:

1. **Follow deployment steps sequentially** - do not skip phases
2. **Wait for each phase completion** before proceeding  
3. **Run tests after each major change** - require 100% success
4. **Use status commands frequently** to verify state
5. **Check logs/output** for error messages and act accordingly
6. **Document any deviations** from standard procedure
7. **Verify end-to-end functionality** before declaring success

## 👤 Human Operator Notes

- **Bookmark URL**: Usually http://localhost:37373/ for daily use
- **Configuration persists**: State saved in `.bb-super-duper-server`
- **Multiple instances**: Can run on same machine (different ports)
- **Browser compatibility**: Any modern browser works
- **Development workflow**: Edit code → `bb run super-duper-server restart` → test

This manual ensures reliable, repeatable deployments of the Super Duper BB Server environment.