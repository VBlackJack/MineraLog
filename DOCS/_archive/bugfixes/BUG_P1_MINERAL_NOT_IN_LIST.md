# BUG P1: Minéral Créé N'Apparaît Pas Dans Liste

**Severity:** P1 (High - Core Functionality)
**Date Found:** 2025-11-15
**Found By:** User Manual Testing
**Device:** Samsung Galaxy S23 Ultra (Android 16)
**Build:** v1.5.0 RC (with P1 DB fix)

---

## Description

Après création d'une fiche minéral avec photo, le minéral n'apparaît pas dans la liste principale (Home Screen), mais il EST visible dans l'écran Statistiques.

---

## Steps to Reproduce

1. Ouvrir MineraLog
2. Taper bouton `+` pour ajouter minéral
3. Remplir nom + autres champs
4. Ajouter une photo (via caméra ou galerie)
5. Sauvegarder
6. Retourner à la liste principale (Home)

**Expected:** Minéral apparaît dans liste
**Actual:** Minéral N'apparaît PAS dans liste
**BUT:** Minéral visible dans Statistics screen

---

## Hypothèses (À Vérifier)

### Hypothèse 1: Filtre ou Recherche Active ⚠️ PROBABLE

**Analyse Code:**
- `HomeViewModel.kt` lignes 89-104: Flow paginé avec logique de filtre
- Logique: Search > Filter > Show All
- Si `_searchQuery` non vide OU `_isFilterActive` = true → filtre appliqué

**Test:**
- [ ] Vérifier si barre recherche a du texte
- [ ] Vérifier si badge "X filters active" visible
- [ ] Vérifier si minéral apparaît après "Clear filter/search"

**Fix SI confirmé:**
- Ajouter toast après création: "Mineral saved. Clear filters to see it in list"
- OU: Auto-clear filters après création
- OU: Ajouter "View in list" button qui clear filters + scroll to mineral

---

### Hypothèse 2: Problème de Tri/Order

**Analyse:**
- Liste triée alphabétiquement par défaut
- Si nom vide ou commence par caractère spécial → peut être hors vue

**Test:**
- [ ] Vérifier nom du minéral créé (vide? caractère spécial?)
- [ ] Scroller liste complètement (haut en bas)
- [ ] Vérifier ordre de tri actuel

**Fix SI confirmé:**
- Validation nom obligatoire (actuellement optionnel?)
- Scroll automatique vers minéral créé après sauvegarde

---

### Hypothèse 3: Cache PagingData

**Analyse:**
- `mineralsPaged.cachedIn(viewModelScope)` ligne 104
- Cache peut ne pas se rafraîchir immédiatement

**Test:**
- [ ] Force-stop app et relancer → minéral apparaît?
- [ ] Pull-to-refresh (si implementé) → minéral apparaît?

**Fix SI confirmé:**
- Invalider cache après insertion
- Forcer refresh de PagingSource

---

### Hypothèse 4: Race Condition

**Analyse:**
- Photo sauvegardée après minéral
- Si navigation happens avant photo save → minéral en DB mais pas photo?
- Stats query différente de liste query?

**Test:**
- [ ] Vérifier logs pour timing de save
- [ ] Créer minéral SANS photo → apparaît dans liste?
- [ ] Créer minéral avec photo → n'apparaît pas?

**Fix SI confirmé:**
- Attendre sauvegarde photo avant navigation
- Transaction atomique (mineral + photo)

---

## Information Needed from User

**Questions:**
1. Y a-t-il du texte dans la barre de recherche en haut?
2. Voyez-vous un badge "X filters active" ou icône filtre colorée?
3. Quel est le NOM exact du minéral créé? (vide? caractère spécial?)
4. Combien de minéraux total dans Statistics?
5. Si vous scroll la liste complètement, le trouvez-vous?
6. Si vous redémarrez l'app (force-stop), apparaît-il?
7. Si vous créez un minéral SANS photo, apparaît-il dans liste?

---

## Logs Needed

```bash
# Capture logs during reproduction
adb logcat -c
# User performs steps 1-6
adb logcat -d > bug_mineral_not_in_list.log
```

**Look for:**
- `INSERT` SQL statements
- `Flow` emissions from repository
- Navigation events
- Photo save completion

---

## Workaround (Temporaire)

**Pour l'utilisateur:**
1. Aller dans Settings (si filtre/recherche actif)
2. Ou taper icône "Clear filters" (X)
3. Ou redémarrer l'app (force-stop)

---

## Priority Justification

**Severity: P1** parce que:
- ❌ Core functionality (ajouter minéral) semble cassée
- ❌ Très mauvaise UX (utilisateur pense que données perdues)
- ✅ Données SONT sauvegardées (visibles dans stats)
- ✅ Potentiel workaround (clear filters)
- ⚠️ Si c'est juste un problème UI/filtre → P2
- 🔴 Si c'est un vrai bug data → P0

**BLOCKER pour release?**
- Si Hypothèse 1 (filtre actif): **NON** - UX issue, documenté
- Si Hypothèse 2-4 (vrai bug): **OUI** - doit fixer avant release

---

## Next Steps

1. **Immédiat:** Poser questions à l'utilisateur (ci-dessus)
2. **Si Hypothèse 1:** Documenter comportement, ajouter toast aide
3. **Si autre:** Debug complet avec logs + fix

---

**Status:** 🔴 **INVESTIGATING** - Awaiting user info

**Updated:** 2025-11-15 12:50
