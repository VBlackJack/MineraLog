#!/bin/bash

# i18n Audit Script for MineraLog
# Checks for parity between EN and FR translations

echo "═══════════════════════════════════════"
echo " MineraLog i18n Audit Report"
echo "═══════════════════════════════════════"
echo ""

EN_FILE="app/src/main/res/values/strings.xml"
FR_FILE="app/src/main/res/values-fr/strings.xml"

# Extract string keys (name attributes)
echo "Extracting string keys..."
grep -o 'name="[^"]*"' "$EN_FILE" | sed 's/name="//;s/"//' | sort > /tmp/en_keys.txt
grep -o 'name="[^"]*"' "$FR_FILE" | sed 's/name="//;s/"//' | sort > /tmp/fr_keys.txt

EN_COUNT=$(wc -l < /tmp/en_keys.txt)
FR_COUNT=$(wc -l < /tmp/fr_keys.txt)

echo "EN strings: $EN_COUNT"
echo "FR strings: $FR_COUNT"
echo ""

# Find missing keys
MISSING_FR=$(comm -23 /tmp/en_keys.txt /tmp/fr_keys.txt)
MISSING_EN=$(comm -13 /tmp/en_keys.txt /tmp/fr_keys.txt)

if [ -n "$MISSING_FR" ]; then
    echo "⚠️  Missing in FR ($(echo "$MISSING_FR" | wc -l) keys):"
    echo "───────────────────────────────────────"
    echo "$MISSING_FR" | head -20
    if [ $(echo "$MISSING_FR" | wc -l) -gt 20 ]; then
        echo "... and $(($(echo "$MISSING_FR" | wc -l) - 20)) more"
    fi
    echo ""
else
    echo "✅ No keys missing in FR"
    echo ""
fi

if [ -n "$MISSING_EN" ]; then
    echo "⚠️  Missing in EN ($(echo "$MISSING_EN" | wc -l) keys):"
    echo "───────────────────────────────────────"
    echo "$MISSING_EN" | head -20
    if [ $(echo "$MISSING_EN" | wc -l) -gt 20 ]; then
        echo "... and $(($(echo "$MISSING_EN" | wc -l) - 20)) more"
    fi
    echo ""
else
    echo "✅ No keys missing in EN"
    echo ""
fi

# Check French spacing (should use \u00A0 before :, ?, !, ;)
echo "Checking French spacing..."
IMPROPER_SPACING=$(grep -n '<string[^>]*>[^<]*[^\\u00A0][:?!;]' "$FR_FILE" | grep -v '\u00A0[:?!;]' | wc -l)

if [ "$IMPROPER_SPACING" -gt 0 ]; then
    echo "⚠️  Found $IMPROPER_SPACING potential French spacing issues"
    echo "    (Should use \\u00A0 before : ? ! ;)"
    grep -n '<string[^>]*>[^<]*[^\\u00A0][:?!;]' "$FR_FILE" | grep -v '\u00A0[:?!;]' | head -5
    echo ""
else
    echo "✅ French spacing looks good"
    echo ""
fi

# Summary
echo "═══════════════════════════════════════"
echo " Summary"
echo "═══════════════════════════════════════"

if [ -z "$MISSING_FR" ] && [ -z "$MISSING_EN" ]; then
    echo "✅ EN/FR parity: PASS (100% match)"
else
    PARITY_PERCENT=$(awk "BEGIN {printf \"%.1f\", (1 - $(echo "$MISSING_FR $MISSING_EN" | wc -w) / ($EN_COUNT + $FR_COUNT + 0.0)) * 100}")
    echo "⚠️  EN/FR parity: ${PARITY_PERCENT}%"
fi

# Cleanup
rm -f /tmp/en_keys.txt /tmp/fr_keys.txt

exit 0
