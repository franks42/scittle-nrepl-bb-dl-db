#!/bin/bash

# Scittle Development Environment Stop Script
# Usage: ./bb-stop.sh

set -e

echo "🛑 Stopping Scittle Development Environment"

# Kill processes on our known ports first
echo "🧹 Cleaning up processes on ports 1339, 1340, 1341..."
lsof -ti:1339,1340,1341 | xargs kill -9 2>/dev/null || true

# Check if PID file exists
if [ -f .bb-pid ]; then
    BB_PID=$(cat .bb-pid)
    echo "   Found PID: $BB_PID"
    
    # Check if process is still running
    if kill -0 $BB_PID 2>/dev/null; then
        echo "   Stopping main process..."
        kill $BB_PID
        
        # Wait for process to stop
        TIMEOUT=5
        COUNT=0
        while kill -0 $BB_PID 2>/dev/null && [ $COUNT -lt $TIMEOUT ]; do
            sleep 1
            COUNT=$((COUNT + 1))
            echo "   Waiting for shutdown... ($COUNT/$TIMEOUT)"
        done
        
        # Force kill if still running
        if kill -0 $BB_PID 2>/dev/null; then
            echo "   Force killing process..."
            kill -9 $BB_PID
        fi
        
        echo "✅ Main process stopped"
    else
        echo "   Main process not running (PID $BB_PID not found)"
    fi
    
    # Clean up files
    rm -f .bb-pid
    rm -f .nrepl-port
    echo "   Cleaned up PID and port files"
else
    echo "   No .bb-pid file found"
    echo "   Checking for running bb processes..."
    
    # Try to find and kill ONLY our specific bb task processes
    PIDS=$(pgrep -f "bb (browser-nrepl|dev)$" || true)
    if [ -n "$PIDS" ]; then
        echo "   Found our bb task processes: $PIDS"
        echo "$PIDS" | xargs kill
        echo "   Killed our bb task processes"
    fi
    
    # Also check for bb processes specifically running our tasks (exact match)
    PIDS=$(ps aux | grep "bb browser-nrepl\|bb dev" | grep -v grep | awk '{print $2}' || true)
    if [ -n "$PIDS" ]; then
        echo "   Found our specific bb processes: $PIDS"
        echo "$PIDS" | xargs kill
        echo "   Killed our specific bb processes"
    fi
    
    # Clean up any leftover files
    rm -f .nrepl-port
fi

# Final port cleanup to be thorough
echo "🔍 Final port cleanup..."
lsof -ti:1339,1340,1341 | xargs kill -9 2>/dev/null || true

echo "🏁 Shutdown complete"