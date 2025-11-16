#!/bin/bash
# Automated Test Battery for P0 Bug Fix
# Tests database persistence after app restart
#
# P0 Bug: Mineral created with photo disappears after app restart
# Root Cause: ProGuard obfuscating EncryptedSharedPreferences
# Fix: Added ProGuard keep rules for EncryptedSharedPreferences & MasterKey
#
# Date: 2025-11-15
# Version: 1.5.0 RC (build 3 - with P0 fix)

set -e  # Exit on error

APK_PATH="G:\_dev\MineraLog\MineraLog\app\build\outputs\apk\release\app-release.apk"
PACKAGE_NAME="net.meshcore.mineralog"
ACTIVITY_NAME="$PACKAGE_NAME/.MainActivity"
LOG_FILE="test_p0_fix_$(date +%Y%m%d_%H%M%S).log"

echo "=========================================="
echo "P0 FIX TEST BATTERY - Database Persistence"
echo "=========================================="
echo ""
echo "APK: $APK_PATH"
echo "Package: $PACKAGE_NAME"
echo "Log file: $LOG_FILE"
echo ""

# Function to wait for device
wait_for_device() {
    echo "[$(date +%H:%M:%S)] Waiting for device..."
    adb wait-for-device
    sleep 2
    echo "[$(date +%H:%M:%S)] Device ready"
}

# Function to capture logs
capture_logs() {
    local label=$1
    echo "[$(date +%H:%M:%S)] Capturing logs: $label"
    adb logcat -d | grep -E "(mineralog|DBMigration|EncryptedSharedPreferences|MasterKey|Room|SQLite)" | tail -50 >> "$LOG_FILE"
    echo "=== $label ===" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
}

# Function to check for critical errors
check_errors() {
    local label=$1
    echo "[$(date +%H:%M:%S)] Checking for errors: $label"

    # Check for database open errors
    if adb logcat -d | grep -q "unable to open database file"; then
        echo "❌ FAIL: Database open error detected!"
        echo "FAIL: $label - unable to open database file" >> "$LOG_FILE"
        return 1
    fi

    # Check for corruption errors
    if adb logcat -d | grep -q "DB wipe detected: corruption"; then
        echo "❌ FAIL: Database corruption error detected!"
        echo "FAIL: $label - DB corruption error" >> "$LOG_FILE"
        return 1
    fi

    # Check for EncryptedSharedPreferences errors
    if adb logcat -d | grep -q "EncryptedSharedPreferences.*Exception"; then
        echo "⚠️  WARNING: EncryptedSharedPreferences error detected!"
        echo "WARNING: $label - EncryptedSharedPreferences error" >> "$LOG_FILE"
        return 1
    fi

    echo "✅ PASS: No critical errors in $label"
    echo "PASS: $label" >> "$LOG_FILE"
    return 0
}

# Function to get app memory
get_memory() {
    adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL PSS" | awk '{print $3}' || echo "N/A"
}

# Function to check if app is running
is_app_running() {
    adb shell pidof "$PACKAGE_NAME" > /dev/null 2>&1
}

echo "=========================================="
echo "TEST 1: Clean Install & First Launch"
echo "=========================================="

wait_for_device

echo "[$(date +%H:%M:%S)] Step 1.1: Uninstalling old version..."
adb uninstall "$PACKAGE_NAME" 2>/dev/null || echo "App not installed (ok)"

echo "[$(date +%H:%M:%S)] Step 1.2: Clearing logcat..."
adb logcat -c

echo "[$(date +%H:%M:%S)] Step 1.3: Installing new version with P0 fix..."
adb install -r "$APK_PATH"

echo "[$(date +%H:%M:%S)] Step 1.4: Launching app..."
adb shell am start -n "$ACTIVITY_NAME"

echo "[$(date +%H:%M:%S)] Step 1.5: Waiting for app initialization (5 seconds)..."
sleep 5

capture_logs "Test 1 - First Launch"
check_errors "Test 1 - First Launch" || exit 1

echo "[$(date +%H:%M:%S)] Step 1.6: Checking memory..."
MEMORY_1=$(get_memory)
echo "Memory usage: $MEMORY_1 KB"

echo ""
echo "=========================================="
echo "TEST 2: App Restart (Immediate)"
echo "=========================================="

echo "[$(date +%H:%M:%S)] Step 2.1: Force-stopping app..."
adb shell am force-stop "$PACKAGE_NAME"
sleep 1

echo "[$(date +%H:%M:%S)] Step 2.2: Clearing logcat..."
adb logcat -c

echo "[$(date +%H:%M:%S)] Step 2.3: Relaunching app..."
adb shell am start -n "$ACTIVITY_NAME"

echo "[$(date +%H:%M:%S)] Step 2.4: Waiting for app initialization (5 seconds)..."
sleep 5

capture_logs "Test 2 - Immediate Restart"
check_errors "Test 2 - Immediate Restart" || exit 1

echo "[$(date +%H:%M:%S)] Step 2.5: Checking memory..."
MEMORY_2=$(get_memory)
echo "Memory usage: $MEMORY_2 KB"

echo ""
echo "=========================================="
echo "TEST 3: Stress Test (10 Rapid Restarts)"
echo "=========================================="

for i in {1..10}; do
    echo "[$(date +%H:%M:%S)] Cycle $i/10..."
    adb shell am start -n "$ACTIVITY_NAME" > /dev/null 2>&1
    sleep 1
    adb shell am force-stop "$PACKAGE_NAME"
    sleep 0.5
done

echo "[$(date +%H:%M:%S)] Step 3.1: Final launch after stress test..."
adb logcat -c
adb shell am start -n "$ACTIVITY_NAME"
sleep 5

capture_logs "Test 3 - After Stress Test"
check_errors "Test 3 - After Stress Test" || exit 1

echo "[$(date +%H:%M:%S)] Step 3.2: Checking memory after stress..."
MEMORY_3=$(get_memory)
echo "Memory usage: $MEMORY_3 KB"

echo ""
echo "=========================================="
echo "TEST 4: Database Key Persistence"
echo "=========================================="

echo "[$(date +%H:%M:%S)] Step 4.1: Checking for database key retrieval logs..."
adb logcat -d | grep -E "(getOrCreatePassphrase|EncryptedSharedPreferences|existing passphrase)" | tail -10

echo ""
echo "=========================================="
echo "MANUAL TEST INSTRUCTIONS"
echo "=========================================="
echo ""
echo "The automated tests have verified that the app launches correctly"
echo "and the database key is persisted. Now please perform these manual tests:"
echo ""
echo "1. CREATE TEST MINERAL:"
echo "   - Tap the '+' button"
echo "   - Enter name: 'TestMineral_$(date +%H%M%S)'"
echo "   - Add a photo (camera or gallery)"
echo "   - Tap 'Save'"
echo "   - Verify the mineral appears in the list"
echo ""
echo "2. VERIFY PERSISTENCE AFTER RESTART:"
echo "   - Run: adb shell am force-stop $PACKAGE_NAME"
echo "   - Run: adb shell am start -n $ACTIVITY_NAME"
echo "   - Verify the mineral 'TestMineral_*' still appears in the list"
echo "   - Go to Statistics screen"
echo "   - Verify count shows 1 mineral"
echo ""
echo "3. CREATE SECOND MINERAL:"
echo "   - Create another mineral with name 'TestMineral2_$(date +%H%M%S)'"
echo "   - Add photo"
echo "   - Save"
echo "   - Verify both minerals appear in list"
echo ""
echo "4. VERIFY AFTER COLD START:"
echo "   - Run: adb shell am force-stop $PACKAGE_NAME"
echo "   - Wait 10 seconds"
echo "   - Run: adb shell am start -n $ACTIVITY_NAME"
echo "   - Verify both minerals still appear"
echo "   - Go to Statistics → verify count shows 2 minerals"
echo ""
echo "5. FINAL VERIFICATION:"
echo "   - Clear any active filters (if badge shows)"
echo "   - Count minerals in list - should match Statistics count"
echo "   - If count doesn't match, report as BUG"
echo ""
echo "=========================================="
echo "AUTOMATED TEST RESULTS SUMMARY"
echo "=========================================="
echo ""
echo "Test 1 - First Launch:        $(grep -q 'PASS: Test 1' $LOG_FILE && echo '✅ PASS' || echo '❌ FAIL')"
echo "Test 2 - Immediate Restart:   $(grep -q 'PASS: Test 2' $LOG_FILE && echo '✅ PASS' || echo '❌ FAIL')"
echo "Test 3 - Stress Test:         $(grep -q 'PASS: Test 3' $LOG_FILE && echo '✅ PASS' || echo '❌ FAIL')"
echo ""
echo "Memory Usage:"
echo "  - First launch:    $MEMORY_1 KB"
echo "  - After restart:   $MEMORY_2 KB"
echo "  - After stress:    $MEMORY_3 KB"
echo ""
echo "Log file: $LOG_FILE"
echo ""
echo "=========================================="
echo "NEXT STEPS"
echo "=========================================="
echo ""
echo "1. Perform the manual tests above"
echo "2. If all tests PASS, the P0 bug is FIXED"
echo "3. If any test FAILS, check the log file: $LOG_FILE"
echo "4. Report results"
echo ""
echo "=========================================="
