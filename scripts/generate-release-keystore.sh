#!/bin/bash
# Script to generate a release keystore for MineraLog
# This should be run ONCE and the keystore should be kept SECURE

set -e

echo "=== MineraLog Release Keystore Generator ==="
echo ""
echo "This script will generate a release keystore for signing your APK."
echo "The keystore will be created and encoded to Base64 for GitHub Secrets."
echo ""
echo "IMPORTANT: Keep the generated keystore and passwords SECURE!"
echo "           Store the keystore file and passwords in a secure location."
echo ""

# Prompt for keystore details
read -p "Enter keystore filename [release.keystore]: " KEYSTORE_FILE
KEYSTORE_FILE=${KEYSTORE_FILE:-release.keystore}

read -p "Enter key alias [mineralog-release]: " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-mineralog-release}

read -s -p "Enter keystore password: " KEYSTORE_PASSWORD
echo ""
read -s -p "Confirm keystore password: " KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo "ERROR: Passwords do not match!"
    exit 1
fi

read -s -p "Enter key password: " KEY_PASSWORD
echo ""
read -s -p "Confirm key password: " KEY_PASSWORD_CONFIRM
echo ""

if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
    echo "ERROR: Passwords do not match!"
    exit 1
fi

echo ""
echo "Enter certificate details:"
read -p "Your Name: " CN
read -p "Organization Unit (e.g., Development): " OU
read -p "Organization (e.g., MeshCore): " O
read -p "City/Locality: " L
read -p "State/Province: " ST
read -p "Country Code (2 letters, e.g., US): " C

# Generate keystore
echo ""
echo "Generating keystore..."
keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

echo ""
echo "✓ Keystore generated successfully: $KEYSTORE_FILE"

# Encode to Base64 for GitHub Secrets
echo ""
echo "Encoding keystore to Base64 for GitHub Secrets..."
KEYSTORE_BASE64=$(cat "$KEYSTORE_FILE" | base64 | tr -d '\n')

echo ""
echo "=== GitHub Secrets Configuration ==="
echo ""
echo "Add the following secrets to your GitHub repository:"
echo ""
echo "Name: RELEASE_KEYSTORE_BASE64"
echo "Value: $KEYSTORE_BASE64"
echo ""
echo "Name: RELEASE_KEYSTORE_PASSWORD"
echo "Value: $KEYSTORE_PASSWORD"
echo ""
echo "Name: RELEASE_KEY_ALIAS"
echo "Value: $KEY_ALIAS"
echo ""
echo "Name: RELEASE_KEY_PASSWORD"
echo "Value: $KEY_PASSWORD"
echo ""
echo "=== IMPORTANT ==="
echo "1. Save the keystore file ($KEYSTORE_FILE) in a SECURE location"
echo "2. NEVER commit the keystore file to git"
echo "3. Keep a backup of the keystore and passwords"
echo "4. If you lose the keystore, you cannot update your app on Google Play"
echo ""
echo "Keystore fingerprints:"
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" | grep -E "(MD5|SHA1|SHA256)"
