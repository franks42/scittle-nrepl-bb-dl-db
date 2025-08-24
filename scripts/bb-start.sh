#!/bin/bash

# Scittle Development Environment Startup Script
# Usage: ./bb-start.sh [mode]
# Modes: browser-nrepl (default), dev (full environment)

set -e

# Configuration
MODE=${1:-browser-nrepl}
DB_PATH="/var/db/datalevin/cljcodedb"

echo "🚀 Starting Scittle Development Environment"
echo "Mode: $MODE"
echo "Database path: $DB_PATH"

# Ensure database directory exists
mkdir -p "$DB_PATH"
echo "✅ Database directory ready: $DB_PATH"

# Clean up any existing processes on our ports
echo "🧹 Cleaning up existing processes..."
lsof -ti:1339,1340,1341 | xargs kill -9 2>/dev/null || true

# Clean up any existing files
rm -f .bb-pid .nrepl-port

# Start appropriate bb task
case $MODE in
    "browser-nrepl")
        echo "📡 Starting browser nREPL (ports 1339/1340)..."
        bb browser-nrepl &
        BB_PID=$!
        echo "✅ Browser nREPL started!"
        echo "   nREPL port: 1339"
        echo "   WebSocket port: 1340"
        ;;
    "dev")
        echo "🌐 Starting full development environment..."
        bb dev &
        BB_PID=$!
        echo "✅ Development environment started!"
        echo "   nREPL port: 1339"
        echo "   WebSocket port: 1340" 
        echo "   HTTP server: http://localhost:1341"
        ;;
    *)
        echo "❌ Unknown mode: $MODE"
        echo "Available modes: browser-nrepl, dev"
        exit 1
        ;;
esac

# Store the process ID
echo $BB_PID > .bb-pid
echo "   PID: $BB_PID"

echo ""
echo "🔧 Test Datalevin pod:"
echo "   bb datalevin-working-test.clj"
echo ""
echo "🌐 Open browser (dev mode):"
echo "   open http://localhost:1341"
echo ""
echo "🛑 To stop: ./bb-stop.sh"