# Guide de Tests Manuels QA - MineraLog v1.5.0

**Version:** 1.5.0 Release Candidate
**Date:** 2025-11-15
**Device Testé:** Samsung Galaxy S23 Ultra (Android 16, SDK 36)
**Status Tests Automatisés:** ✅ PASS (0 crashes, 0 corruption errors)

---

## ✅ Tests Automatisés Complétés

| Test | Résultat | Détails |
|---|---|---|
| Deep Links | ✅ PASS | `mineralapp://mineral/{uuid}` fonctionne |
| Permissions | ✅ PASS | Camera, Media, Location accordées |
| Stabilité (20 cycles) | ✅ PASS | 0 crashes, 0 erreurs corruption |
| Mémoire | ✅ PASS | 93 MB PSS (normal) |
| P1 Bug Fix | ✅ PASS | 0 erreurs "DB wipe detected: corruption" |

---

## 📋 Tests Manuels Requis (UI Interaction)

### Workflow 1: Gestion des Minéraux ⏳

#### 1.1 Ajouter un Minéral
**Steps:**
1. Ouvrir l'app (icône MineraLog)
2. Taper sur le bouton `+` (flottant, coin bas-droit)
3. Remplir les champs:
   - **Nom:** `Quartz`
   - **Formule:** `SiO2`
   - **Groupe:** `Silicates`
   - **Système cristallin:** `Hexagonal`
   - **Dureté Mohs:** `7`
4. Taper `Sauvegarder`

**Vérifications:**
- ✅ Formulaire se remplit sans lag
- ✅ Auto-save fonctionne (draft sauvegardé après 500ms)
- ✅ Tooltips apparaissent sur les champs techniques
- ✅ Minéral apparaît dans la liste

#### 1.2 Modifier un Minéral
**Steps:**
1. Taper sur le minéral `Quartz` dans la liste
2. Modifier le champ **Notes:** `Specimen transparent, bonne qualité`
3. Retourner en arrière (bouton back)

**Vérifications:**
- ✅ Modification sauvegardée automatiquement
- ✅ Notes apparaissent dans la vue détail

#### 1.3 Supprimer un Minéral
**Steps:**
1. Taper sur `Quartz` → Menu (3 points) → `Supprimer`
2. Confirmer la suppression

**Vérifications:**
- ✅ Dialog de confirmation apparaît
- ✅ Minéral supprimé de la liste
- ✅ Pas de crash

---

### Workflow 2: Gestion des Photos 📸 ⏳

#### 2.1 Prendre une Photo avec la Caméra
**Steps:**
1. Créer ou sélectionner un minéral
2. Dans la vue détail, section Photos → Taper `Caméra`
3. Autoriser permission caméra si demandé
4. Sélectionner type de photo: `Normal`
5. Taper le bouton de capture (cercle blanc)
6. Attendre confirmation `Photo captured successfully`

**Vérifications:**
- ✅ Preview caméra s'affiche
- ✅ Bouton capture responsive
- ✅ Photo apparaît dans la galerie
- ✅ Badge "Normal" visible sur la photo

#### 2.2 Ajouter Photo depuis Galerie
**Steps:**
1. Section Photos → Taper `Galerie`
2. Sélectionner une photo
3. Choisir type: `UV Shortwave`
4. Ajouter caption: `Under UV light`

**Vérifications:**
- ✅ Photo importée correctement
- ✅ Badge "UV-SW" bleu visible
- ✅ Caption affichée

#### 2.3 Fullscreen Viewer & Zoom
**Steps:**
1. Taper sur une photo dans la galerie
2. Swiper gauche/droite pour naviguer
3. Pincer pour zoomer (1x → 5x)
4. Double-tap pour reset zoom

**Vérifications:**
- ✅ Fullscreen mode fonctionne
- ✅ Swipe navigation fluide
- ✅ Zoom 1x-5x smooth
- ✅ Double-tap reset à 1x

#### 2.4 Supprimer une Photo
**Steps:**
1. En fullscreen → Menu → `Supprimer`
2. Confirmer

**Vérifications:**
- ✅ Photo supprimée
- ✅ Fichier supprimé du storage

---

### Workflow 3: Backup/Restore avec Chiffrement 🔒 ⏳

#### 3.1 Export Chiffré
**Steps:**
1. Menu principal → `Settings` (engrenage)
2. Section Backup → `Export ZIP`
3. Activer `Encrypt backup`
4. Entrer mot de passe: `TestPass123!`
5. Confirmer mot de passe: `TestPass123!`
6. Vérifier indicateur de force: `Strong` (vert)
7. Choisir emplacement de sauvegarde
8. Attendre "Export successful"

**Vérifications:**
- ✅ Password strength indicator fonctionne:
  - < 8 chars: `Weak` (rouge)
  - 8-12 chars: `Medium` (orange)
  - > 12 chars + symbols: `Strong` (vert)
- ✅ Passwords non-matching: erreur affichée
- ✅ Export progress visible
- ✅ Toast "Export successful" apparaît
- ✅ Fichier ZIP créé dans Downloads

#### 3.2 Import Chiffré
**Steps:**
1. Settings → `Import ZIP`
2. Sélectionner le fichier exporté
3. Entrer mot de passe: `TestPass123!`
4. Choisir mode: `Merge (upsert)`
5. Confirmer import

**Vérifications:**
- ✅ Password prompt apparaît
- ✅ Mauvais password: erreur "Incorrect password"
- ✅ Bon password: import réussit
- ✅ Preview des données avant import
- ✅ Minéraux importés visibles dans liste

---

### Workflow 4: Import/Export CSV 📊 ⏳

#### 4.1 Export CSV
**Steps:**
1. Settings → `Export CSV`
2. Sélectionner colonnes:
   - ✅ Name
   - ✅ Formula
   - ✅ Group
   - ✅ Mohs Hardness
3. Choisir emplacement
4. Confirmer

**Vérifications:**
- ✅ Sélecteur de colonnes fonctionne
- ✅ Preview des 5 premières lignes
- ✅ Export successful
- ✅ Fichier CSV ouvrable dans Excel/Sheets

#### 4.2 Import CSV avec Column Mapping
**Steps:**
1. Settings → `Import CSV`
2. Sélectionner un fichier CSV test
3. Vérifier auto-detection:
   - Délimiteur détecté (`,` ou `;`)
   - Encoding détecté (UTF-8)
   - Headers détectés
4. Mapper les colonnes:
   - `mineral_name` → `Name`
   - `chemical_formula` → `Formula`
   - (Fuzzy matching automatique)
5. Preview → Confirmer import

**Vérifications:**
- ✅ Auto-detection fonctionne
- ✅ Column mapping intuitif
- ✅ Fuzzy matching suggère bonnes colonnes
- ✅ Preview montre 5 lignes
- ✅ Warnings pour colonnes manquantes
- ✅ Import successful

---

### Workflow 5: QR Codes & Deep Links 🏷️ ⏳

#### 5.1 Générer QR Label PDF
**Steps:**
1. Sélectionner 1-8 minéraux (checkbox)
2. Menu → `Generate QR Labels`
3. Attendre génération PDF
4. Ouvrir PDF généré

**Vérifications:**
- ✅ PDF généré (format A4)
- ✅ Layout 2×4 (8 labels par page)
- ✅ Chaque label contient:
  - QR code scannable
  - Nom du minéral
  - Formule chimique
  - Groupe
- ✅ QR codes scannables

#### 5.2 Scanner QR Code
**Steps:**
1. Imprimer un QR label OU afficher à l'écran
2. App → Menu → `Scan QR Code`
3. Pointer caméra vers QR code
4. Attendre reconnaissance

**Vérifications:**
- ✅ Scanner s'ouvre avec preview
- ✅ Torch button fonctionne (si sombre)
- ✅ QR code reconnu automatiquement
- ✅ Navigation vers détail minéral
- ✅ Deep link `mineralapp://mineral/{uuid}` fonctionne

---

### Workflow 6: Recherche & Filtrage 🔍 ⏳

#### 6.1 Recherche Full-Text
**Steps:**
1. Dans liste des minéraux → Barre de recherche
2. Taper `qua`
3. Observer résultats en temps réel
4. Effacer recherche

**Vérifications:**
- ✅ Résultats apparaissent < 300ms (debounced)
- ✅ Recherche dans: Name, Formula, Notes, Tags
- ✅ Highlighting du texte recherché
- ✅ Clear button (X) fonctionne

#### 6.2 Filtres Avancés
**Steps:**
1. Taper icône filtre
2. Sélectionner critères:
   - **Mohs range:** 6-8
   - **Crystal system:** `Hexagonal`
   - **Fluorescence:** `UV-SW`
3. Appliquer filtres

**Vérifications:**
- ✅ Filtres multiples combinables (AND logic)
- ✅ Liste se met à jour instantanément
- ✅ Badge "X filters active" visible
- ✅ Clear all filters fonctionne

---

### Workflow 7: Accessibilité TalkBack ♿ ⏳

#### 7.1 Activation TalkBack
**Steps:**
1. Settings Android → Accessibility → TalkBack → ON
2. Retourner à MineraLog

#### 7.2 Tests des 5 Écrans Principaux

**A. Home Screen / Liste Minéraux**
- ✅ Chaque item de liste annoncé avec nom + formule
- ✅ Bouton `+` annoncé: "Add mineral button"
- ✅ Search field annoncé: "Search minerals"
- ✅ Filter button annoncé: "Filter button"

**B. Add/Edit Mineral Screen**
- ✅ Chaque champ annoncé avec label
- ✅ Tooltips lus automatiquement sur focus
- ✅ Save button annoncé: "Save button"
- ✅ Live region pour auto-save: "Draft saved"

**C. Camera Capture Screen**
- ✅ Preview annoncé: "Camera preview"
- ✅ Capture button annoncé: "Capture photo button"
- ✅ Photo type selector annoncé: "Normal, UV Shortwave, UV Longwave, Macro"
- ✅ Live regions pour états:
  - "Capturing photo..."
  - "Photo captured successfully"
  - "Photo capture failed. Please try again"

**D. Settings Screen**
- ✅ Export button annoncé: "Export backup"
- ✅ Import button annoncé: "Import backup"
- ✅ Switches liés sémantiquement:
  - "Copy Photos to Backup, switch, off"
  - "Encrypt Backups by Default, switch, on"
- ✅ Live regions pour opérations:
  - "Exporting backup... Please wait"
  - "Backup exported successfully"

**E. Photo Manager / Gallery**
- ✅ Gallery button annoncé: "Open gallery"
- ✅ Camera button annoncé: "Take photo"
- ✅ Photos annoncées:
  - "Photo: Normal type. Caption: Beautiful specimen"
  - "Photo: UV Shortwave type. No caption"
- ✅ Empty state annoncé: "No photos. Add photos to document your mineral"

**Critères WCAG 2.1 AA:**
- ✅ **1.1.1** Non-text Content: Toutes images ont contentDescription
- ✅ **1.3.1** Info and Relationships: Structure sémantique correcte
- ✅ **4.1.2** Name, Role, Value: Tous composants ont role + description
- ✅ **4.1.3** Status Messages: Live regions pour états dynamiques

---

## 🎯 Critères de Validation Globaux

### Must Pass (Bloquant pour Release)

- [ ] **Zéro crash** pendant les 7 workflows
- [ ] **Zéro erreur P0** (data loss, corruption, security)
- [ ] **Toutes les features principales fonctionnent**
- [ ] **Accessibilité Grade A (92%)** maintenu
- [ ] **Mémoire < 150 MB** après usage normal

### Should Pass (Important mais non-bloquant)

- [ ] **Performance fluide** (60 FPS, < 300ms latency)
- [ ] **UI responsive** sur toutes tailles d'écran
- [ ] **Permissions gérées proprement** (pas de crash si refusées)
- [ ] **Error messages clairs** et actionnables

### Nice to Have

- [ ] **Animations smooth** (respect prefers-reduced-motion)
- [ ] **Haptic feedback** sur actions critiques
- [ ] **Tooltips utiles** pour champs techniques

---

## 📊 Formulaire de Résultats

### Tests Effectués

| Workflow | Status | Notes | Bugs Trouvés |
|---|---|---|---|
| 1. Gestion Minéraux | ⏳ | | |
| 2. Gestion Photos | ⏳ | | |
| 3. Backup/Restore | ⏳ | | |
| 4. CSV Import/Export | ⏳ | | |
| 5. QR Codes | ⏳ | | |
| 6. Recherche/Filtres | ⏳ | | |
| 7. TalkBack | ⏳ | | |

### Bugs Identifiés

| ID | Sévérité | Description | Steps to Reproduce | Status |
|---|---|---|---|---|
| | | | | |

### Métriques

- **Crashes:** 0
- **Erreurs P0:** 0
- **Erreurs P1:** 0
- **Warnings:**
- **Performance:**
- **Mémoire max:** 93 MB (automatisé)

---

## ✅ Sign-Off

**Testeur:** _________________
**Date:** _________________
**Device:** Samsung Galaxy S23 Ultra (Android 16)
**Build:** v1.5.0 RC (39 MB)

**Résultat Global:** [ ] PASS  [ ] FAIL  [ ] PASS with minor issues

**Commentaires:**
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

**Approuvé pour Release:** [ ] OUI  [ ] NON (raison: _________________)

---

**Guide créé le:** 2025-11-15
**Dernière mise à jour:** 2025-11-15
**Version Guide:** 1.0
