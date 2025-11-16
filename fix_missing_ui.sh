#!/bin/bash

# Script de diagnostic et correction pour les sections UI manquantes
# MineraLog - Fix Missing UI Sections

set -e  # Exit on error

echo "========================================="
echo "MineraLog - Fix Missing UI Sections"
echo "========================================="
echo ""

# Étape 1 : Nettoyage complet du cache Gradle et Android
echo "[1/6] Nettoyage complet du cache..."
./gradlew clean
rm -rf .gradle/
rm -rf app/build/
rm -rf build/
rm -rf ~/.gradle/caches/
echo "✓ Cache nettoyé"
echo ""

# Étape 2 : Vérification de la version de base de données
echo "[2/6] Vérification de la configuration de base de données..."
DB_VERSION=$(grep -A 5 "@Database" app/src/main/java/net/meshcore/mineralog/data/local/MineraLogDatabase.kt | grep "version =" | grep -oP '\d+')
echo "Version de base de données dans le code : $DB_VERSION"
if [ "$DB_VERSION" != "8" ]; then
    echo "⚠️  ATTENTION : La version devrait être 8 !"
    exit 1
fi
echo "✓ Version correcte (8)"
echo ""

# Étape 3 : Vérification des migrations
echo "[3/6] Vérification que MIGRATION_7_8 est bien enregistrée..."
if grep -q "MIGRATION_7_8" app/src/main/java/net/meshcore/mineralog/data/local/MineraLogDatabase.kt; then
    echo "✓ MIGRATION_7_8 trouvée dans la configuration"
else
    echo "✗ MIGRATION_7_8 introuvable !"
    exit 1
fi
echo ""

# Étape 4 : Build debug APK
echo "[4/6] Compilation de l'APK debug..."
./gradlew assembleDebug --no-daemon --stacktrace
echo "✓ APK compilé"
echo ""

# Étape 5 : Localisation de l'APK
echo "[5/6] Localisation de l'APK..."
APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
if [ -z "$APK_PATH" ]; then
    echo "✗ APK introuvable !"
    exit 1
fi
echo "APK trouvé : $APK_PATH"
echo ""

# Étape 6 : Instructions pour l'installation
echo "[6/6] Instructions d'installation..."
echo ""
echo "========================================="
echo "ÉTAPES À SUIVRE SUR VOTRE TÉLÉPHONE :"
echo "========================================="
echo ""
echo "1. Désinstallez COMPLÈTEMENT l'application MineraLog"
echo "   - Paramètres > Apps > MineraLog > Désinstaller"
echo "   - Ceci supprimera aussi la base de données (sauvegardez vos données avant !)"
echo ""
echo "2. Installez le NOUVEL APK :"
echo "   adb install -r \"$APK_PATH\""
echo ""
echo "3. Lancez l'application et testez :"
echo "   - Créez un nouveau minéral de type AGGREGATE"
echo "   - Vérifiez que les sections suivantes apparaissent :"
echo "     • Propriétés de l'Agrégat (4 champs)"
echo "     • Provenance & Acquisition (5 champs)"
echo ""
echo "========================================="
echo "ALTERNATIVE : Si vous voulez GARDER vos données :"
echo "========================================="
echo ""
echo "1. Exportez vos données depuis l'app (Paramètres > Export)"
echo "2. Désinstallez l'app"
echo "3. Installez le nouvel APK"
echo "4. Importez vos données (Paramètres > Import)"
echo ""
echo "La migration 7→8 sera alors appliquée lors de l'import."
echo ""
echo "✅ Script terminé avec succès !"
