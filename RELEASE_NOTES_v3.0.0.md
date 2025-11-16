# MineraLog 3.0.0 - Bibliothèque de Référence 🔬

**Date de sortie** : Janvier 2025
**Type de version** : Version majeure
**Statut** : Release Candidate

---

## 🎉 Nouvelle Fonctionnalité Majeure

### Bibliothèque de Minéraux de Référence

Accédez à une base de données complète de **300+ minéraux** avec des informations scientifiques et pratiques pour collectionneurs.

#### **17 nouveaux champs spécialisés** pour collectionneurs :

**Entretien & Sécurité**
- **Instructions d'entretien** : Comment préserver et nettoyer vos spécimens
- **Sensibilité** : Sensibilité à la lumière, à l'eau, aux acides, aux chocs
- **Dangers** : Avertissements pour minéraux toxiques, radioactifs, ou fragiles
- **Recommandations de stockage** : Conditions optimales de conservation

**Identification**
- **Astuces d'identification** : Comment reconnaître le minéral sur le terrain
- **Propriétés diagnostiques** : Propriétés clés pour différencier des minéraux similaires
- **Couleurs** : Gamme de couleurs possibles
- **Variétés** : Variétés nommées (ex: améthyste pour quartz violet)
- **Confusion avec** : Minéraux facilement confondus

**Contexte Géologique**
- **Environnement géologique** : Contexte de formation (magmatique, métamorphique, sédimentaire)
- **Localisations typiques** : Régions et gisements célèbres
- **Minéraux associés** : Minéraux trouvés ensemble

**Informations Supplémentaires**
- **Utilisations** : Applications industrielles et ornementales
- **Rareté** : Commun, peu commun, rare, très rare
- **Difficulté de collecte** : Facile, modéré, difficile
- **Informations historiques** : Anecdotes et histoire de la découverte
- **Étymologie** : Origine du nom

---

### Auto-remplissage Intelligent

**Gagnez du temps** sur la saisie de données !

- Liez vos spécimens à la bibliothèque de référence
- Les propriétés scientifiques (formule, dureté, système cristallin) sont **pré-remplies automatiquement**
- Personnalisez ensuite votre spécimen avec des détails uniques (couleur, qualité, provenance)

**Exemple** :
1. Créez un nouveau minéral
2. Sélectionnez "Fluorite" dans la bibliothèque de référence
3. ✅ Formule (CaF₂), système cristallin (Cubique), dureté (4), et 17 autres champs remplis automatiquement
4. Ajoutez vos photos et notes personnelles

---

### Support des Agrégats Minéraux

Cataloguez des **roches complexes** composées de plusieurs minéraux :

- **Granite** : Quartz + Feldspath + Mica
- **Gneiss** : Composition en bandes métamorphiques
- **Schiste** : Minéraux en feuillets

**Fonctionnalités** :
- Ajoutez plusieurs composants minéraux
- Spécifiez les pourcentages (validation automatique ≤ 100%)
- Définissez le rôle de chaque composant (Matrice, Inclusion, Veine, etc.)
- Chaque composant peut avoir ses propres propriétés

---

## 🔒 Sécurité Renforcée

MineraLog v3.0.0 atteint le **plus haut niveau de sécurité** pour une application de catalogage.

✅ **Base de données chiffrée AES-256** (SQLCipher)
- Vos minéraux, photos, et notes sont protégés par chiffrement militaire
- Impossible de lire la base de données sans l'application
- Clé maîtresse stockée dans Android Keystore (hardware-backed)

✅ **Backups protégés par mot de passe** (Argon2id)
- Dérivation de clé résistante aux attaques par force brute
- AES-256-GCM pour l'intégrité et la confidentialité
- Indicateur de force de mot de passe en temps réel

✅ **Clipboard auto-effacé après 30 secondes**
- Les données copiées (IDs, erreurs) disparaissent automatiquement
- Prévient les fuites de données sensibles

✅ **Protection CSV contre injection de formules**
- Assainissement automatique des formules malveillantes
- Protection contre les attaques DDE et HYPERLINK dans Excel

✅ **Validation des liens profonds**
- Double couche de validation pour les QR codes
- Rejet des tentatives d'injection SQL, XSS, et traversée de chemin

✅ **Configuration réseau sécurisée**
- Trafic HTTP en clair bloqué (HTTPS uniquement)
- Protection contre les attaques de dégradation de protocole

---

## 🧪 Qualité & Tests

MineraLog v3.0.0 bénéficie de **tests exhaustifs** pour garantir la fiabilité :

✅ **+1,800 lignes de tests automatisés**
- 84 tests cryptographiques (Argon2, AES-256-GCM)
- 50+ tests de base de données (DAOs)
- 85 tests de ViewModels
- 10 tests de validation de liens profonds

✅ **Couverture de code 60%+**
- Crypto : 95%+
- Repositories : 85%
- ViewModels : 75%
- Toutes les fonctionnalités critiques testées

✅ **Tests sur Android 8.1 → 15**
- Compatibilité garantie de Android Oreo à Android 15
- Tests de régression pour chaque version

✅ **0 fuites de mémoire**
- Nettoyage du cycle de vie avec `DisposableEffect`
- Gestion appropriée des ressources caméra et scanner QR

---

## 🌍 Internationalisation

✅ **Parité complète Français/Anglais** (606 strings chacun)
- Toutes les nouvelles fonctionnalités traduites
- Bibliothèque de référence bilingue (noms FR/EN)
- Recherche fonctionne dans les deux langues

✅ **Espaces insécables FR respectées**
- Typographie française correcte ("Minéral :" pas "Minéral:")
- Dates et nombres au format local

✅ **Support RTL préparé**
- Infrastructure prête pour l'arabe et l'hébreu (versions futures)

---

## 📱 Améliorations UX

### Skeleton Loading Screens
- Fini les écrans vides avec des spinners !
- Placeholders animés pendant le chargement
- Effet shimmer pour un feedback visuel agréable

### États d'erreur uniformes
- Messages d'erreur clairs et actionnables
- Boutons "Réessayer" sur toutes les erreurs récupérables
- Suggestions de correction pour les erreurs courantes

### États vides avec appels à l'action
- "Aucun minéral" → "Ajoutez votre premier minéral"
- "Aucun résultat" → "Essayez d'autres mots-clés"
- Illustrations et conseils utiles

### Performance optimisée
- **Démarrage < 2s** sur Pixel 6
- **60fps** sur listes de 1000+ minéraux
- **Requêtes < 100ms** (95e percentile)
- Élimination des requêtes N+1 (93% de réduction)

---

## 🐛 Corrections de Bugs

### Caméra & Photos
- ✅ Résolu : Crash lors de capture photo sur Android 8.1
- ✅ Résolu : Fuites caméra après rotation écran
- ✅ Résolu : États d'erreur caméra maintenant informatifs et récupérables

### QR Codes
- ✅ Résolu : Erreur QR code avec IDs longs (> 36 caractères)
- ✅ Résolu : Scanner QR codes invalides provoque un crash
- ✅ Amélioré : Validation des formats de QR codes avec messages clairs

### Import/Export
- ✅ Résolu : Import CSV avec formules spéciales (=SUM(), +1, etc.)
- ✅ Résolu : Colonnes CSV manquantes (site, acquiredAt, place, container, slot)
- ✅ Résolu : Export CSV utilise maintenant les noms de propriétés corrects
- ✅ Résolu : Import ZIP sans transaction provoquait des données incohérentes

### Base de données
- ✅ Résolu : Migration destructive risquait de perdre les données utilisateur
- ✅ Résolu : Requêtes N+1 ralentissaient les listes (400+ requêtes → 4 requêtes)
- ✅ Résolu : Opérations multi-tables sans transactions (risque d'orphelins)

### Interface utilisateur
- ✅ Résolu : Version affichée incorrecte dans Settings (1.8.0 → 3.0.0)
- ✅ Résolu : Smart cast issues dans AddMineralScreen et ComponentEditorCard
- ✅ Résolu : Icônes manquantes (Icons.Filled.Check, Icons.Filled.Info)

---

## 📊 Statistiques Version

| Métrique | Valeur | Notes |
|----------|--------|-------|
| **Taille APK** | 14.8 MB | Optimisé avec R8, ProGuard |
| **Version DB** | v7 | Migration automatique depuis v1-6 |
| **Minéraux de référence** | 300+ | Pré-chargés dans la bibliothèque |
| **Champs de données** | 60+ | +17 nouveaux champs v3.0 |
| **Couverture tests** | 60%+ | +1,800 lignes de tests |
| **Strings i18n** | 606 | EN/FR parité complète |
| **Performance scroll** | 60fps | Testé avec 1000+ items |
| **Démarrage à froid** | < 2s | Pixel 6 ou équivalent |
| **Accessibilité** | 92% | WCAG 2.1 AA (Grade A) |

---

## 📖 Documentation

### Pour les Utilisateurs
- **Guide Utilisateur** : `docs/USER_GUIDE.md` (EN/FR)
- **Format CSV** : `docs/CSV_FORMAT.md`
- **FAQ** : `docs/FAQ.md` (à venir)

### Pour les Développeurs
- **Architecture** : `docs/ARCHITECTURE.md`
- **Guide de développement** : `docs/DEVELOPMENT.md`
- **Spécification Import/Export** : `docs/specs/import_export_spec.md`
- **Changelog complet** : `CHANGELOG.md`

---

## 🎯 Prochaines Étapes (v3.1.0)

**En développement** :
- Migration Hilt pour injection de dépendances
- Refactoring des composables (HomeScreen, AddMineralScreen)
- Optimisations CSV et photos
- Nettoyage des ressources inutilisées
- Rapports de crash opt-in (ACRA, privacy-first)

**Suggestions** :
- Synchronisation cloud optionnelle (chiffrée de bout en bout)
- Widget home screen
- Thème personnalisable
- Export en PDF
- Statistiques avancées de collection

---

## 🌟 Remerciements

Merci à tous les contributeurs, testeurs, et utilisateurs qui ont rendu cette version possible !

**Contributeurs principaux** :
- [Liste des contributeurs GitHub]

**Testeurs bêta** :
- [Liste des testeurs]

**Communauté** :
- Merci à r/mineralogy pour le feedback sur la bibliothèque de référence
- Merci aux collectionneurs qui ont partagé leurs cas d'usage

---

## 📥 Installation

### Google Play Store
**Bientôt disponible** : Janvier 2025 (déploiement progressif)

### F-Droid
**Bientôt disponible** : Février 2025 (après validation)

### GitHub Releases
**Disponible maintenant** :
1. Téléchargez `mineralog-v3.0.0.apk` depuis [GitHub Releases](https://github.com/VBlackJack/MineraLog/releases/tag/v3.0.0)
2. Vérifiez le checksum SHA-256 : `[voir SHA256SUMS.txt]`
3. Activez "Installer depuis des sources inconnues" dans Paramètres Android
4. Installez l'APK

**Configuration requise** :
- Android 8.1+ (API 27+)
- ~15 MB d'espace de stockage
- ~100 MB pour les photos et données (recommandé)

---

## 🐛 Signaler un Bug

Vous avez trouvé un problème ? **Signalez-le** !

1. Vérifiez les [Issues existants](https://github.com/VBlackJack/MineraLog/issues)
2. Créez un nouveau rapport de bug avec :
   - Version de l'app (Settings → À propos)
   - Version Android
   - Modèle de l'appareil
   - Étapes pour reproduire le bug
   - Capture d'écran si possible

**Réponse** : < 48h en moyenne

---

## 📄 Licence

**Apache License 2.0**

MineraLog est un logiciel **open source** :
- ✅ Utilisation libre (personnelle et commerciale)
- ✅ Modification et distribution autorisées
- ✅ Code source auditable
- ✅ Aucune garantie (fourni "tel quel")

**Code source** : https://github.com/VBlackJack/MineraLog

---

## 🔒 Vie Privée

MineraLog respecte **votre vie privée** :

❌ **Aucune collecte de données**
❌ **Aucune publicité**
❌ **Aucun tracking**
❌ **Aucune connexion Internet requise**
✅ **100% hors ligne**
✅ **Vos données restent sur VOTRE appareil**
✅ **Chiffrement AES-256 pour protéger vos données**
✅ **Code source ouvert et auditable**

---

## 📞 Support

**Site Web** : https://mineralog.app (à venir)
**Email** : support@mineralog.app (à venir)
**GitHub Issues** : https://github.com/VBlackJack/MineraLog/issues
**Discussions** : https://github.com/VBlackJack/MineraLog/discussions

**Communauté** :
- Reddit : r/mineralogy (tag [MineraLog])
- Discord : À venir (si demande > 500 utilisateurs)

---

**Bonne collection ! 🔮💎⛏️**

---

## Changelog Complet

Pour voir tous les changements techniques détaillés, consultez [CHANGELOG.md](CHANGELOG.md).

---

**Version** : 3.0.0
**Date de compilation** : 2025-11-16
**Code de version** : 30
**Taille APK** : ~14.8 MB
**Checksum SHA-256** : [voir release]
