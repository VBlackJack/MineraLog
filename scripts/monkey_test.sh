#!/bin/bash

# MineraLog v3.0.0 - Monkey Testing Script
# Performs stress testing with random UI events
# Usage: ./scripts/monkey_test.sh [event_count]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
PACKAGE_NAME="net.meshcore.mineralog"
EVENT_COUNT="${1:-10000}"
OUTPUT_DIR="$PROJECT_ROOT/test_results/monkey"
LOG_FILE="$OUTPUT_DIR/monkey_test_$(date +%Y%m%d_%H%M%S).log"
SUMMARY_FILE="$OUTPUT_DIR/monkey_summary.txt"

# Monkey test parameters
THROTTLE_MS=100          # Delay between events (ms)
PCT_TOUCH=40             # Touch events (40%)
PCT_MOTION=25            # Motion events (swipes, 25%)
PCT_NAV=15               # Navigation events (back, home, 15%)
PCT_TRACKBALL=0          # Trackball (disabled)
PCT_MAJORNAV=10          # Major navigation (menu, 10%)
PCT_SYSKEYS=5            # System keys (volume, power, 5%)
PCT_APPSWITCH=5          # App switching (5%)

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "========================================"
echo "MineraLog Monkey Testing"
echo "========================================"
echo "Package: $PACKAGE_NAME"
echo "Events: $EVENT_COUNT"
echo "Output: $LOG_FILE"
echo "========================================"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ ERROR: No Android device/emulator connected"
    echo "Please connect a device or start an emulator"
    exit 1
fi

# Check if APK is installed
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "⚠️  WARNING: $PACKAGE_NAME not installed"
    echo "Installing release APK..."

    APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"

    if [ ! -f "$APK_PATH" ]; then
        echo "❌ ERROR: Release APK not found at $APK_PATH"
        echo "Please build the release APK first:"
        echo "  ./gradlew assembleRelease"
        exit 1
    fi

    echo "Installing APK: $APK_PATH"
    adb install -r "$APK_PATH"

    if [ $? -ne 0 ]; then
        echo "❌ ERROR: Failed to install APK"
        exit 1
    fi

    echo "✅ APK installed successfully"
fi

# Clear app data for clean state
echo "🧹 Clearing app data for clean state..."
adb shell pm clear "$PACKAGE_NAME"

# Launch app
echo "🚀 Launching $PACKAGE_NAME..."
adb shell am start -n "$PACKAGE_NAME/.ui.MainActivity"
sleep 3

# Pre-test: Get initial database state
echo "📊 Capturing initial database state..."
INITIAL_DB_SIZE=$(adb shell "run-as $PACKAGE_NAME du -sh /data/data/$PACKAGE_NAME/databases" 2>/dev/null | awk '{print $1}' || echo "unknown")
echo "Initial DB size: $INITIAL_DB_SIZE"

# Run monkey test
echo "========================================"
echo "🐵 Starting Monkey Test (${EVENT_COUNT} events)..."
echo "========================================"
echo "Started at: $(date)"
echo ""

adb shell monkey -p "$PACKAGE_NAME" \
  --throttle "$THROTTLE_MS" \
  --pct-touch "$PCT_TOUCH" \
  --pct-motion "$PCT_MOTION" \
  --pct-nav "$PCT_NAV" \
  --pct-trackball "$PCT_TRACKBALL" \
  --pct-majornav "$PCT_MAJORNAV" \
  --pct-syskeys "$PCT_SYSKEYS" \
  --pct-appswitch "$PCT_APPSWITCH" \
  --ignore-crashes \
  --ignore-timeouts \
  --ignore-security-exceptions \
  -v -v \
  "$EVENT_COUNT" 2>&1 | tee "$LOG_FILE"

MONKEY_EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo "========================================"
echo "Monkey Test Completed"
echo "Finished at: $(date)"
echo "========================================"

# Post-test: Get final database state
echo "📊 Capturing final database state..."
FINAL_DB_SIZE=$(adb shell "run-as $PACKAGE_NAME du -sh /data/data/$PACKAGE_NAME/databases" 2>/dev/null | awk '{print $1}' || echo "unknown")
echo "Final DB size: $FINAL_DB_SIZE"

# Analyze results
echo ""
echo "========================================"
echo "📊 Analyzing Results..."
echo "========================================"

CRASH_COUNT=$(grep -c "CRASH:" "$LOG_FILE" || echo "0")
ANR_COUNT=$(grep -c "ANR in" "$LOG_FILE" || echo "0")
EXCEPTION_COUNT=$(grep -c -i "exception" "$LOG_FILE" || echo "0")
EVENTS_COMPLETED=$(grep "Events injected:" "$LOG_FILE" | tail -1 | awk '{print $3}' || echo "unknown")

# Generate summary report
cat > "$SUMMARY_FILE" <<EOF
MineraLog Monkey Test Summary
======================================
Date: $(date)
Package: $PACKAGE_NAME
Events Requested: $EVENT_COUNT
Events Completed: $EVENTS_COMPLETED

Results:
- App Crashes: $CRASH_COUNT
- ANRs (App Not Responding): $ANR_COUNT
- Exceptions: $EXCEPTION_COUNT
- Exit Code: $MONKEY_EXIT_CODE

Database State:
- Initial Size: $INITIAL_DB_SIZE
- Final Size: $FINAL_DB_SIZE

Event Distribution:
- Touch: ${PCT_TOUCH}%
- Motion: ${PCT_MOTION}%
- Navigation: ${PCT_NAV}%
- Major Nav: ${PCT_MAJORNAV}%
- System Keys: ${PCT_SYSKEYS}%
- App Switch: ${PCT_APPSWITCH}%

Acceptance Criteria:
- Crashes: $CRASH_COUNT (Target: 0) $([ "$CRASH_COUNT" -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL")
- ANRs: $ANR_COUNT (Target: < 3) $([ "$ANR_COUNT" -lt 3 ] && echo "✅ PASS" || echo "⚠️  WARNING")
- Database Corruption: $(adb shell "run-as $PACKAGE_NAME sqlite3 /data/data/$PACKAGE_NAME/databases/mineralog_database 'PRAGMA integrity_check;'" 2>/dev/null | grep -q "ok" && echo "✅ NO" || echo "❌ YES")

Full Log: $LOG_FILE
EOF

# Display summary
cat "$SUMMARY_FILE"

# Extract crash details if any
if [ "$CRASH_COUNT" -gt 0 ]; then
    echo ""
    echo "========================================"
    echo "❌ CRASH DETAILS"
    echo "========================================"
    grep -A 20 "CRASH:" "$LOG_FILE" | head -50
fi

# Extract ANR details if any
if [ "$ANR_COUNT" -gt 0 ]; then
    echo ""
    echo "========================================"
    echo "⚠️  ANR DETAILS"
    echo "========================================"
    grep -A 10 "ANR in" "$LOG_FILE" | head -30
fi

# Verify database integrity
echo ""
echo "========================================"
echo "🔍 Database Integrity Check"
echo "========================================"

DB_INTEGRITY=$(adb shell "run-as $PACKAGE_NAME sqlite3 /data/data/$PACKAGE_NAME/databases/mineralog_database 'PRAGMA integrity_check;'" 2>/dev/null || echo "error")

if echo "$DB_INTEGRITY" | grep -q "ok"; then
    echo "✅ Database integrity: OK"
else
    echo "❌ Database integrity: CORRUPTED"
    echo "Details: $DB_INTEGRITY"
fi

# Check for data loss
echo ""
echo "Checking for data loss..."
MINERAL_COUNT=$(adb shell "run-as $PACKAGE_NAME sqlite3 /data/data/$PACKAGE_NAME/databases/mineralog_database 'SELECT COUNT(*) FROM minerals;'" 2>/dev/null || echo "0")
echo "Minerals in database: $MINERAL_COUNT"

# Final verdict
echo ""
echo "========================================"
echo "🎯 FINAL VERDICT"
echo "========================================"

if [ "$CRASH_COUNT" -eq 0 ] && [ "$ANR_COUNT" -lt 3 ] && echo "$DB_INTEGRITY" | grep -q "ok"; then
    echo "✅ PASSED - App is stable and ready for release"
    exit 0
elif [ "$CRASH_COUNT" -gt 0 ]; then
    echo "❌ FAILED - App crashed $CRASH_COUNT times"
    echo "Review crash logs and fix critical issues before release"
    exit 1
elif [ "$ANR_COUNT" -ge 3 ]; then
    echo "⚠️  WARNING - App had $ANR_COUNT ANRs (acceptable < 3)"
    echo "Consider performance optimization"
    exit 0
else
    echo "⚠️  WARNING - Database integrity check failed"
    echo "Investigate data corruption issues"
    exit 1
fi
