# MineraLog 🪨

[![Version](https://img.shields.io/badge/Version-3.0.0-brightgreen.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0+-green.svg)](https://www.android.com)

**Cataloguez et gérez votre collection de minéraux en toute simplicité.**
**Catalog and manage your mineral collection with ease.**

---

## 📑 Table des matières / Table of Contents

**Français:**
- [Qu'est-ce que c'est ?](#quest-ce-que-cest-)
- [Pour qui ?](#pour-qui-)
- [Points forts](#points-forts-)
- [Fonctionnalités principales](#fonctionnalités-principales-)
- [Prérequis](#prérequis-)
- [Installation](#installation-)
- [Démarrage rapide](#démarrage-rapide-)
- [Import/Export](#importexport-)
- [Vie privée & sécurité](#vie-privée--sécurité-)
- [Accessibilité & langues](#accessibilité--langues-)
- [Dépannage](#dépannage-)
- [Support](#support-)
- [Limites connues](#limites-connues-)
- [Licence](#licence-)
- [En 1 minute](#en-1-minute-)

**English:**
- [What is it?](#what-is-it-)
- [Who is it for?](#who-is-it-for-)
- [Key Benefits](#key-benefits-)
- [Main Features](#main-features-)
- [Requirements](#requirements-)
- [Installation](#installation--1)
- [Quick Start](#quick-start-)
- [Import/Export](#importexport--1)
- [Privacy & Security](#privacy--security-)
- [Accessibility & Languages](#accessibility--languages-)
- [Troubleshooting](#troubleshooting-)
- [Support](#support--1)
- [Known Limits](#known-limits-)
- [License](#license-)
- [In 1 Minute](#in-1-minute-)

---

# 🇫🇷 Français

## Qu'est-ce que c'est ? 🪨

**MineraLog** est une application Android gratuite et sans publicité pour **organiser votre collection de minéraux**. Que vous ayez 10 ou 1000 spécimens, MineraLog vous aide à tout cataloguer : photos, propriétés scientifiques, provenance, emplacement de stockage, et bien plus.

Vos données restent **sur votre téléphone** — pas de compte, pas de cloud obligatoire. Vous gardez le contrôle total.

## Pour qui ? 👥

MineraLog s'adresse à :

- **Collectionneurs débutants** : Créez votre premier catalogue sans connaissance technique
- **Collectionneurs passionnés** : Gérez des centaines de spécimens avec recherche avancée et QR codes
- **Étudiants en géologie** : Cataloguez vos échantillons de terrain avec coordonnées GPS
- **Professeurs et formateurs** : Organisez vos collections pédagogiques
- **Héritiers de collections** : Inventoriez et documentez une collection familiale

**Cas d'usage concrets :**
- Retrouver rapidement "Où ai-je rangé ma fluorite ?"
- Imprimer des étiquettes QR pour vos boîtes de rangement
- Partager votre catalogue avec d'autres collectionneurs (export CSV)
- Protéger vos données avec un mot de passe avant de les sauvegarder

## Points forts ✨

✅ **100% hors ligne** — Vos données ne quittent jamais votre téléphone
✅ **Zéro pub, zéro compte** — Ouvrez l'appli et commencez immédiatement
✅ **Sauvegarde complète** — Exportez tout (photos incluses) en un clic
✅ **Accessible à tous** — Interface en français et anglais, compatible lecteur d'écran
✅ **QR codes intelligents** — Scannez une étiquette et affichez la fiche instantanément
✅ **Gratuit et open-source** — Licence Apache 2.0, code transparent

## Fonctionnalités principales 🔬

### 🔬 Identification de minéraux (NOUVEAU v3.0)
- **Assistant d'identification intelligent** : Identifiez vos minéraux inconnus grâce à leurs propriétés physiques
- Filtres multiples : Couleur, dureté (Mohs), couleur du trait, éclat, magnétisme
- **Algorithme de pertinence** : Résultats classés par score de correspondance
- Base de données de 464+ minéraux de référence
- Accès rapide depuis l'écran d'accueil

### 📸 Photos et galerie
- Prenez jusqu'à 4 types de photos par minéral (normale, UV, macro)
- Galerie avec zoom jusqu'à 5x
- Photos stockées en toute sécurité dans l'appli

### 🗂️ Catalogage complet
- **Propriétés scientifiques** : Système cristallin, dureté Mohs, éclat, clivage, habitus, etc.
- **Provenance** : Pays, localité, coordonnées GPS, date d'acquisition, prix
- **Stockage** : Lieu → Armoire → Tiroir → Boîte → Emplacement
- **Notes et étiquettes** : Ajoutez vos observations personnelles
- **Prix et valeur** : Suivi de la valeur estimée et du prix d'achat

### 🔍 Recherche et tri
- Recherche instantanée par nom, formule chimique, groupe, notes ou étiquettes
- Filtres avancés : Dureté, fluorescence, pays, système cristallin
- **Tri complet (7 options)** : Nom (A-Z / Z-A), Date (plus récent / plus ancien), Groupe minéral, Dureté (croissante / décroissante)

### 🏷️ QR codes
- **Génération d'étiquettes** : Créez des PDF imprimables (8 étiquettes par page A4)
- **Scanner** : Scannez une étiquette et affichez la fiche du minéral instantanément
- Idéal pour organiser des boîtes de rangement physiques

### 💾 Import/Export (Amélioré v3.0)
- **Format ZIP sécurisé** : Sauvegarde complète avec toutes les photos
  - Protection par mot de passe (Argon2id + AES-256-GCM)
  - Protection anti-ZIP bomb et validation des chemins
  - Validation automatique de l'intégrité (minerals.json requis)
  - Export de la bibliothèque de référence optionnel
- **Format CSV** : Compatible Excel/LibreOffice pour partage et édition
- **3 modes d'import** : Fusionner, Remplacer, ou Ignorer les doublons
- Aperçu avant import avec détection automatique des colonnes
- Messages d'erreur clairs en cas de corruption

### 📊 Statistiques
- Graphiques de répartition par groupe, système cristallin, pays
- Évolution de votre collection dans le temps

## Prérequis ⚙️

- **Téléphone ou tablette Android 8.0 (Oreo) ou plus récent**
- **Espace de stockage** : ~50 Mo pour l'appli + espace pour vos photos (variable)
- **Autorisations demandées** :
  - **Appareil photo** : Pour photographier vos minéraux (optionnel)
  - **Stockage** : Pour importer/exporter vos données (optionnel)

**Note** : Toutes les fonctionnalités de base (ajout manuel, consultation) fonctionnent sans aucune autorisation.

## Installation 📥

### Méthode 1 : Télécharger le fichier APK (recommandé)

1. Rendez-vous sur la page [Releases](https://github.com/VBlackJack/MineraLog/releases) de GitHub
2. Téléchargez le fichier `MineraLog-v3.0.0.apk` (dernière version)
3. Ouvrez le fichier téléchargé
4. Autorisez l'installation depuis "Sources inconnues" si demandé
5. Tapez sur "Installer"

### Méthode 2 : Construire vous-même (utilisateurs avancés)

Consultez la section [Building from Source](DOCS/developer_guide.md) dans la documentation développeur.

### Mise à jour

Pour mettre à jour, téléchargez simplement la nouvelle version APK et installez-la. Vos données seront conservées automatiquement.

**⚠️ Conseil** : Avant toute mise à jour, faites une sauvegarde (Paramètres → Exporter).

## Démarrage rapide 🚀

### 1️⃣ Ajouter votre premier minéral

1. Ouvrez MineraLog
2. Tapez sur le bouton **➕** en bas à droite
3. Remplissez au minimum le **Nom** (exemple : "Quartz")
4. Ajoutez d'autres infos si vous le souhaitez (groupe, formule, dureté...)
5. Tapez sur **"Enregistrer"**

### 2️⃣ Prendre une photo

1. Ouvrez la fiche d'un minéral
2. Tapez sur l'icône **📷 Appareil photo**
3. Choisissez le type de photo (Normale, UV, Macro)
4. Prenez la photo
5. Validez ✅ ou recommencez 🔄

### 3️⃣ Organiser votre collection

- **Ajouter des étiquettes** : Tapez dans le champ "Étiquettes" (exemple : "gemme", "fluorescent")
- **Indiquer l'emplacement** : Remplissez "Lieu → Contenant → Boîte" (exemple : "Armoire A → Tiroir 1 → Boîte 3")
- **Noter la provenance** : Pays, localité, date d'achat, prix

### 4️⃣ Retrouver un minéral

- **Recherche rapide** : Tapez dans la barre de recherche en haut
- **Filtres** : Tapez sur l'icône de filtre pour filtrer par dureté, pays, fluorescence, etc.
- **QR code** : Scannez l'étiquette de la boîte (Paramètres → Scanner QR)

### 5️⃣ Sauvegarder votre collection

1. Allez dans **Paramètres** (icône ⚙️)
2. Tapez sur **"Exporter (ZIP)"**
3. Choisissez un mot de passe (optionnel mais recommandé)
4. Sélectionnez où enregistrer le fichier (Google Drive, téléchargements, etc.)

![Placeholder: Capture d'écran du flux d'ajout d'un minéral]

## Import/Export 💾

### Exporter vos données

**Format ZIP (recommandé)** :
- Contient **toutes vos photos**
- Protection par mot de passe disponible
- Idéal pour sauvegarde complète

**Format CSV** :
- Compatible Excel, Google Sheets, LibreOffice
- Idéal pour partager avec d'autres collectionneurs ou éditer en masse
- **Attention** : Les photos ne sont pas incluses

**Comment faire** :
1. Paramètres → Exporter (ZIP) ou Exporter (CSV)
2. Choisissez un mot de passe (pour ZIP uniquement)
3. Sélectionnez la destination (Drive, Téléchargements, etc.)

### Importer des données

**Depuis un ZIP** :
1. Paramètres → Importer (ZIP)
2. Sélectionnez votre fichier `.zip`
3. Entrez le mot de passe si protégé
4. Choisissez le mode d'import :
   - **Fusionner** : Combine avec vos données existantes (recommandé)
   - **Remplacer** : Efface tout et importe (⚠️ sauvegardez avant !)
   - **Ignorer doublons** : N'importe que les nouveaux

**Depuis un CSV** :
1. Paramètres → Importer (CSV)
2. Sélectionnez votre fichier `.csv`
3. **Aperçu** : Vérifiez la détection des colonnes
4. Validez pour importer

**Compatibilité** :
- Fichiers créés avec MineraLog
- Fichiers CSV exportés depuis Excel/Sheets (assurez-vous d'avoir une colonne "name")
- Fichiers de collections partagées par d'autres utilisateurs

## Vie privée & sécurité 🔒

### Vos données restent privées

✅ **100% hors ligne** — Aucune connexion Internet requise
✅ **Zéro télémétrie** — Aucune statistique d'utilisation collectée
✅ **Pas de compte** — Pas d'email, pas de numéro de téléphone
✅ **Stockage local** — Tout reste sur votre appareil

### Protection par mot de passe

Lors de l'export en ZIP, vous pouvez protéger vos données avec un mot de passe :
- **Chiffrement fort** : Votre mot de passe protège le fichier avec un algorithme moderne (AES-256)
- **Indicateur de robustesse** : L'appli vous indique si votre mot de passe est faible, moyen ou fort
- **Aucun stockage** : Votre mot de passe n'est jamais enregistré nulle part

⚠️ **Important** : Si vous oubliez votre mot de passe, il est **impossible** de récupérer vos données. Notez-le dans un endroit sûr !

### Autorisations

MineraLog demande uniquement :
- **Appareil photo** : Pour prendre des photos de vos minéraux (refusable — vous pourrez toujours cataloguer)
- **Stockage** : Pour importer/exporter vos sauvegardes (refusable — l'appli fonctionnera en mode consultation)

Vous pouvez **refuser toutes les autorisations** et utiliser MineraLog en mode hors ligne complet (saisie manuelle uniquement).

## Accessibilité & langues ♿

### Langues disponibles

- 🇫🇷 **Français** (interface + propriétés minéralogiques)
- 🇬🇧 **English** (interface + mineralogical properties)

**Changer la langue** : Paramètres → Langue

### Accessibilité

MineraLog est conçu pour être utilisable par tous :

✅ **Lecteur d'écran** : Compatible TalkBack (Android) — toutes les actions sont annoncées
✅ **Contraste** : Tous les textes respectent les normes de lisibilité (WCAG 2.1 AA)
✅ **Taille de texte** : Agrandissement jusqu'à 200% sans perte d'information
✅ **Navigation clavier** : Tous les boutons sont accessibles avec Tab/Entrée
✅ **Infobulles** : Chaque propriété scientifique a une explication
✅ **Thème sombre** : Disponible pour réduire la fatigue oculaire

Pour plus de détails, consultez [ACCESSIBILITY.md](ACCESSIBILITY.md).

## Dépannage 🔧

### ❓ Questions fréquentes (FAQ)

**Q : L'appli ne s'installe pas. Pourquoi ?**
**R :** Vérifiez que vous avez Android 8.0 minimum. Activez "Sources inconnues" dans les paramètres de sécurité de votre téléphone.

**Q : J'ai perdu toutes mes données après une mise à jour !**
**R :** Les mises à jour ne suppriment normalement pas les données. Vérifiez si vous avez une sauvegarde (ZIP) dans vos Téléchargements ou Drive.

**Q : Le fichier CSV importé affiche des erreurs**
**R :** Assurez-vous que :
- Le fichier a une colonne "name" (obligatoire)
- Les valeurs des propriétés correspondent à la langue de l'interface (ex : "Cubique" en FR, "Cubic" en EN)
- Le fichier est encodé en UTF-8

**Q : L'appareil photo ne fonctionne pas**
**R :** Vérifiez que vous avez autorisé l'accès à l'appareil photo dans les paramètres Android (Paramètres → Applications → MineraLog → Autorisations).

**Q : Comment imprimer les étiquettes QR ?**
**R :**
1. Tapez sur un minéral → Menu (⋮) → "Générer QR code"
2. Sauvegardez le PDF
3. Imprimez sur papier A4 (8 étiquettes par page)

**Q : Le QR code ne scanne pas**
**R :** Assurez-vous que :
- L'autorisation caméra est accordée
- L'étiquette est bien éclairée
- Le QR code contient bien l'UUID du minéral (format : `mineralapp://mineral/{uuid}`)

**Q : Puis-je utiliser MineraLog sur plusieurs appareils ?**
**R :** Oui ! Exportez votre collection en ZIP sur l'appareil 1, transférez le fichier vers l'appareil 2, puis importez-le. Répétez à chaque fois que vous voulez synchroniser.

**Q : MineraLog fonctionne-t-il sans Internet ?**
**R :** Oui, 100% hors ligne. Internet n'est nécessaire que si vous sauvegardez vers un cloud (Drive, Dropbox, etc.).

**Q : Combien de minéraux puis-je cataloguer ?**
**R :** Il n'y a pas de limite théorique. Des tests ont été faits avec plus de 1000 minéraux sans ralentissement.

**Q : Les photos prennent trop de place**
**R :** Les photos sont stockées dans le dossier de l'appli. Pour libérer de l'espace, supprimez les photos des minéraux que vous n'utilisez plus, ou exportez votre collection et réinstallez l'appli.

### 🛠️ Problèmes courants

**L'appli se ferme toute seule (crash)**
→ Essayez de redémarrer votre téléphone. Si le problème persiste, signalez-le (voir [Support](#support-)).

**La recherche ne trouve rien**
→ Vérifiez l'orthographe. La recherche est sensible aux accents (ex : "fluorite" ≠ "fluorité").

**Le fichier CSV est grisé lors de l'import**
→ Assurez-vous que le fichier a bien l'extension `.csv` (pas `.txt` ou `.xlsx`).

## Support 💬

### Besoin d'aide ?

- **📖 Guide utilisateur complet** : [DOCS/user_guide.md](DOCS/user_guide.md)
- **🐛 Signaler un bug** : [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)
- **💡 Proposer une fonctionnalité** : [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions)

### Contribuer

MineraLog est open-source ! Vous pouvez :
- Signaler des bugs
- Proposer de nouvelles fonctionnalités
- Traduire l'appli dans d'autres langues
- Améliorer la documentation

Consultez [CONTRIBUTING.md](CONTRIBUTING.md) pour en savoir plus.

## Limites connues ⚠️

- **Pas de synchronisation cloud automatique** : Vous devez exporter/importer manuellement pour synchroniser entre appareils
- **Photos limitées à 4 par minéral** : Si vous avez besoin de plus, utilisez une galerie externe et ajoutez le chemin dans les notes
- **QR codes nécessitent l'impression** : Pas de fonction de scan en réalité augmentée pour l'instant
- **Pas d'API publique** : Impossible d'intégrer MineraLog avec d'autres outils (prévu pour v2.0)

**Feuille de route (v2.0)** :
- Synchronisation cloud optionnelle (Google Drive, Nextcloud)
- Export HTML pour site web
- Graphiques avancés (carte du monde interactive)
- Support de plus de langues (espagnol, allemand, italien)

## Licence 📜

MineraLog est distribué sous licence **Apache 2.0** — vous êtes libre de :
- ✅ Utiliser l'appli à des fins personnelles ou commerciales
- ✅ Modifier le code source
- ✅ Distribuer des copies
- ✅ Incorporer dans d'autres projets

**Note importante** : Si vous distribuez des versions modifiées, vous devez indiquer les changements effectués et conserver les mentions de copyright.

Consultez [LICENSE](LICENSE) pour les détails légaux.

## En 1 minute ⏱️

**MineraLog** est une appli Android **gratuite, sans pub, et hors ligne** pour cataloguer votre collection de minéraux.

**Idéal pour** : Collectionneurs, étudiants, enseignants
**Points forts** : Photos, QR codes, sauvegarde complète, 100% privé
**Langues** : Français, English
**Requis** : Android 8.0+

👉 [Télécharger la dernière version](https://github.com/VBlackJack/MineraLog/releases)
👉 [Lire le guide complet](DOCS/user_guide.md)

---

# 🇬🇧 English

## What is it? 🪨

**MineraLog** is a free, ad-free Android app to **organize your mineral collection**. Whether you have 10 or 1,000 specimens, MineraLog helps you catalog everything: photos, scientific properties, provenance, storage location, and more.

Your data stays **on your phone** — no account, no mandatory cloud. You keep full control.

## Who is it for? 👥

MineraLog is designed for:

- **Beginner collectors**: Create your first catalog without technical knowledge
- **Passionate collectors**: Manage hundreds of specimens with advanced search and QR codes
- **Geology students**: Catalog field samples with GPS coordinates
- **Teachers and trainers**: Organize your educational collections
- **Collection inheritors**: Inventory and document a family collection

**Real-world use cases:**
- Quickly find "Where did I store my fluorite?"
- Print QR labels for storage boxes
- Share your catalog with other collectors (CSV export)
- Protect your data with a password before backing up

## Key Benefits ✨

✅ **100% offline** — Your data never leaves your phone
✅ **Zero ads, zero account** — Open the app and start immediately
✅ **Complete backup** — Export everything (photos included) in one tap
✅ **Accessible to all** — Interface in French and English, screen reader compatible
✅ **Smart QR codes** — Scan a label and display the record instantly
✅ **Free and open-source** — Apache 2.0 license, transparent code

## Main Features 🔬

### 🔬 Mineral Identification (NEW v3.0)
- **Smart Identification Assistant**: Identify unknown minerals using physical properties
- Multiple filters: Color, hardness (Mohs), streak, luster, magnetism
- **Relevance algorithm**: Results ranked by match score
- Database of 464+ reference minerals
- Quick access from home screen

### 📸 Photos and Gallery
- Take up to 4 photo types per mineral (normal, UV, macro)
- Gallery with zoom up to 5x
- Photos securely stored in the app

### 🗂️ Complete Cataloging
- **Scientific properties**: Crystal system, Mohs hardness, luster, cleavage, habit, etc.
- **Provenance**: Country, locality, GPS coordinates, acquisition date, price
- **Storage**: Place → Cabinet → Drawer → Box → Slot
- **Notes and tags**: Add your personal observations
- **Price and value**: Track estimated value and purchase price

### 🔍 Search and Sort
- Instant search by name, chemical formula, group, notes, or tags
- Advanced filters: Hardness, fluorescence, country, crystal system
- **Comprehensive sorting (7 options)**: Name (A-Z / Z-A), Date (newest / oldest), Mineral group, Hardness (ascending / descending)

### 🏷️ QR Codes
- **Label generation**: Create printable PDFs (8 labels per A4 page)
- **Scanner**: Scan a label and display the mineral record instantly
- Ideal for organizing physical storage boxes

### 💾 Import/Export (Enhanced v3.0)
- **Secure ZIP format**: Complete backup with all photos
  - Password protection (Argon2id + AES-256-GCM encryption)
  - ZIP bomb protection and path validation
  - Automatic integrity validation (minerals.json required)
  - Optional reference library export
- **CSV format**: Excel/LibreOffice compatible for sharing and editing
- **3 import modes**: Merge, Replace, or Skip duplicates
- Preview before import with automatic column detection
- Clear error messages for corrupt backups

### 📊 Statistics
- Distribution charts by group, crystal system, country
- Evolution of your collection over time

## Requirements ⚙️

- **Android phone or tablet 8.0 (Oreo) or newer**
- **Storage space**: ~50 MB for the app + space for your photos (variable)
- **Permissions requested**:
  - **Camera**: To photograph your minerals (optional)
  - **Storage**: To import/export your data (optional)

**Note**: All basic features (manual entry, viewing) work without any permissions.

## Installation 📥

### Method 1: Download the APK file (recommended)

1. Go to the [Releases](https://github.com/VBlackJack/MineraLog/releases) page on GitHub
2. Download the `MineraLog-v3.0.0.apk` file (latest version)
3. Open the downloaded file
4. Allow installation from "Unknown sources" if prompted
5. Tap "Install"

### Method 2: Build yourself (advanced users)

See the [Building from Source](DOCS/developer_guide.md) section in the developer documentation.

### Updating

To update, simply download the new APK version and install it. Your data will be automatically preserved.

**⚠️ Tip**: Before any update, make a backup (Settings → Export).

## Quick Start 🚀

### 1️⃣ Add your first mineral

1. Open MineraLog
2. Tap the **➕** button at the bottom right
3. Fill in at least the **Name** (example: "Quartz")
4. Add other info if you wish (group, formula, hardness...)
5. Tap **"Save"**

### 2️⃣ Take a photo

1. Open a mineral record
2. Tap the **📷 Camera** icon
3. Choose the photo type (Normal, UV, Macro)
4. Take the photo
5. Confirm ✅ or retry 🔄

### 3️⃣ Organize your collection

- **Add tags**: Tap in the "Tags" field (example: "gem", "fluorescent")
- **Set location**: Fill in "Place → Container → Box" (example: "Cabinet A → Drawer 1 → Box 3")
- **Record provenance**: Country, locality, purchase date, price

### 4️⃣ Find a mineral

- **Quick search**: Type in the search bar at the top
- **Filters**: Tap the filter icon to filter by hardness, country, fluorescence, etc.
- **QR code**: Scan the box label (Settings → QR Scanner)

### 5️⃣ Back up your collection

1. Go to **Settings** (⚙️ icon)
2. Tap **"Export (ZIP)"**
3. Choose a password (optional but recommended)
4. Select where to save the file (Google Drive, downloads, etc.)

![Placeholder: Screenshot of mineral adding flow]

## Import/Export 💾

### Exporting your data

**ZIP format (recommended)**:
- Contains **all your photos**
- Password protection available
- Ideal for complete backup

**CSV format**:
- Compatible with Excel, Google Sheets, LibreOffice
- Ideal for sharing with other collectors or bulk editing
- **Warning**: Photos are not included

**How to**:
1. Settings → Export (ZIP) or Export (CSV)
2. Choose a password (for ZIP only)
3. Select destination (Drive, Downloads, etc.)

### Importing data

**From a ZIP**:
1. Settings → Import (ZIP)
2. Select your `.zip` file
3. Enter password if protected
4. Choose import mode:
   - **Merge**: Combines with your existing data (recommended)
   - **Replace**: Erases everything and imports (⚠️ back up first!)
   - **Skip duplicates**: Only imports new items

**From a CSV**:
1. Settings → Import (CSV)
2. Select your `.csv` file
3. **Preview**: Check column detection
4. Confirm to import

**Compatibility**:
- Files created with MineraLog
- CSV files exported from Excel/Sheets (make sure you have a "name" column)
- Collection files shared by other users

## Privacy & Security 🔒

### Your data stays private

✅ **100% offline** — No Internet connection required
✅ **Zero telemetry** — No usage statistics collected
✅ **No account** — No email, no phone number
✅ **Local storage** — Everything stays on your device

### Password protection

When exporting to ZIP, you can protect your data with a password:
- **Strong encryption**: Your password protects the file with modern encryption (AES-256)
- **Strength indicator**: The app shows if your password is weak, medium, or strong
- **No storage**: Your password is never saved anywhere

⚠️ **Important**: If you forget your password, it's **impossible** to recover your data. Write it down in a safe place!

### Permissions

MineraLog only requests:
- **Camera**: To take photos of your minerals (refusable — you can still catalog)
- **Storage**: To import/export your backups (refusable — the app will work in view-only mode)

You can **refuse all permissions** and use MineraLog in fully offline mode (manual entry only).

## Accessibility & Languages ♿

### Available languages

- 🇫🇷 **Français** (interface + mineralogical properties)
- 🇬🇧 **English** (interface + mineralogical properties)

**Change language**: Settings → Language

### Accessibility

MineraLog is designed to be usable by everyone:

✅ **Screen reader**: TalkBack compatible (Android) — all actions are announced
✅ **Contrast**: All text meets readability standards (WCAG 2.1 AA)
✅ **Text size**: Enlargement up to 200% without information loss
✅ **Keyboard navigation**: All buttons accessible with Tab/Enter
✅ **Tooltips**: Each scientific property has an explanation
✅ **Dark theme**: Available to reduce eye strain

For more details, see [ACCESSIBILITY.md](ACCESSIBILITY.md).

## Troubleshooting 🔧

### ❓ Frequently Asked Questions (FAQ)

**Q: The app won't install. Why?**
**A:** Check that you have Android 8.0 minimum. Enable "Unknown sources" in your phone's security settings.

**Q: I lost all my data after an update!**
**A:** Updates normally don't delete data. Check if you have a backup (ZIP) in your Downloads or Drive.

**Q: The imported CSV file shows errors**
**A:** Make sure that:
- The file has a "name" column (required)
- Property values match the interface language (e.g., "Cubic" in EN, "Cubique" in FR)
- The file is encoded in UTF-8

**Q: The camera doesn't work**
**A:** Check that you've allowed camera access in Android settings (Settings → Apps → MineraLog → Permissions).

**Q: How do I print QR labels?**
**A:**
1. Tap a mineral → Menu (⋮) → "Generate QR code"
2. Save the PDF
3. Print on A4 paper (8 labels per page)

**Q: The QR code won't scan**
**A:** Make sure that:
- Camera permission is granted
- The label is well lit
- The QR code contains the mineral's UUID (format: `mineralapp://mineral/{uuid}`)

**Q: Can I use MineraLog on multiple devices?**
**A:** Yes! Export your collection to ZIP on device 1, transfer the file to device 2, then import it. Repeat whenever you want to sync.

**Q: Does MineraLog work without Internet?**
**A:** Yes, 100% offline. Internet is only needed if you save to a cloud (Drive, Dropbox, etc.).

**Q: How many minerals can I catalog?**
**A:** There's no theoretical limit. Tests have been done with over 1,000 minerals without slowdown.

**Q: Photos take up too much space**
**A:** Photos are stored in the app folder. To free up space, delete photos of minerals you no longer use, or export your collection and reinstall the app.

### 🛠️ Common Issues

**The app closes by itself (crash)**
→ Try restarting your phone. If the problem persists, report it (see [Support](#support--1)).

**Search finds nothing**
→ Check spelling. Search is accent-sensitive (e.g., "fluorite" ≠ "fluorité").

**CSV file is grayed out during import**
→ Make sure the file has the `.csv` extension (not `.txt` or `.xlsx`).

## Support 💬

### Need help?

- **📖 Complete user guide**: [DOCS/user_guide.md](DOCS/user_guide.md)
- **🐛 Report a bug**: [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)
- **💡 Suggest a feature**: [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions)

### Contributing

MineraLog is open-source! You can:
- Report bugs
- Suggest new features
- Translate the app into other languages
- Improve documentation

See [CONTRIBUTING.md](CONTRIBUTING.md) to learn more.

## Known Limits ⚠️

- **No automatic cloud sync**: You must manually export/import to sync between devices
- **Photos limited to 4 per mineral**: If you need more, use an external gallery and add the path in notes
- **QR codes require printing**: No augmented reality scan function for now
- **No public API**: Cannot integrate MineraLog with other tools (planned for v2.0)

**Roadmap (v2.0)**:
- Optional cloud sync (Google Drive, Nextcloud)
- HTML export for website
- Advanced charts (interactive world map)
- Support for more languages (Spanish, German, Italian)

## License 📜

MineraLog is distributed under the **Apache 2.0 License** — you are free to:
- ✅ Use the app for personal or commercial purposes
- ✅ Modify the source code
- ✅ Distribute copies
- ✅ Incorporate into other projects

**Important note**: If you distribute modified versions, you must indicate the changes made and preserve copyright notices.

See [LICENSE](LICENSE) for legal details.

## In 1 Minute ⏱️

**MineraLog** is a **free, ad-free, offline** Android app to catalog your mineral collection.

**Ideal for**: Collectors, students, teachers
**Key features**: Photos, QR codes, complete backup, 100% private
**Languages**: Français, English
**Requirements**: Android 8.0+

👉 [Download latest version](https://github.com/VBlackJack/MineraLog/releases)
👉 [Read complete guide](DOCS/user_guide.md)

---

**Made with ⛏️ by mineral enthusiasts, for mineral enthusiasts.**

**Author**: Julien Bombled
**Copyright** © 2024-2025 — Apache 2.0 License
