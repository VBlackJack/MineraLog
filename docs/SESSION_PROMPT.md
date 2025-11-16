# Prompt de Session - Bibliothèque de Minéraux de Référence

> **Instructions :** Copier-coller ce prompt au début de chaque nouvelle session de travail sur cette roadmap

---

## 📋 PROMPT GÉNÉRIQUE POUR CHAQUE SESSION

```
Contexte : Je travaille sur l'implémentation de la roadmap "Bibliothèque de Minéraux de Référence"
pour l'application Android MineraLog (Kotlin + Jetpack + Room).

Fichiers de référence :
- Roadmap complète : docs/ROADMAP_REFERENCE_LIBRARY.md
- État d'avancement : docs/IMPLEMENTATION_STATUS.yaml

Tâches pour cette session :

1. **Vérifier l'état actuel**
   - Lire docs/IMPLEMENTATION_STATUS.yaml
   - Afficher un résumé de l'avancement global (% par phase)
   - Identifier la phase en cours et les tâches non terminées

2. **Reprendre le travail**
   - [OPTIONNEL : Préciser la phase/tâche à traiter, sinon continuer là où on s'est arrêté]
   - Phase à traiter : [Phase X - Nom de la phase]
   - Tâche spécifique : [X.Y - Nom de la tâche]

3. **Mettre à jour le suivi**
   - Au fur et à mesure de l'avancement, mettre à jour docs/IMPLEMENTATION_STATUS.yaml
   - Marquer les subtasks comme "done: true" quand complétées
   - Mettre à jour le "progress" (0-100) de chaque tâche
   - Mettre à jour le "status" (not_started → in_progress → completed)
   - Ajouter une entrée dans "sessions" avec date, durée, phases travaillées, notes

4. **Commit réguliers**
   - Commiter après chaque tâche/sous-tâche complétée
   - Messages de commit clairs : "feat(reference-library): [Phase X.Y] Description"
   - Pousser régulièrement vers la branche : claude/mineral-reference-library-01QesBzGZhi24Mep1j3a9xRJ

Contraintes :
- Suivre STRICTEMENT la roadmap dans docs/ROADMAP_REFERENCE_LIBRARY.md
- Respecter les dépendances entre phases
- Ne pas sauter d'étapes (sauf si explicitement demandé)
- Documenter tout changement significatif dans les notes de IMPLEMENTATION_STATUS.yaml

Tu es prêt ? Commençons par afficher l'état d'avancement actuel.
```

---

## 🎯 VARIANTES DU PROMPT (SELON LE BESOIN)

### Variante 1 : Démarrage d'une nouvelle phase

```
Je souhaite démarrer la [Phase X - Nom] de la roadmap "Bibliothèque de Minéraux de Référence".

1. Vérifier que les phases dépendantes sont complétées (voir dependencies dans IMPLEMENTATION_STATUS.yaml)
2. Lire la description complète de la phase dans ROADMAP_REFERENCE_LIBRARY.md
3. Lister toutes les tâches de cette phase
4. Commencer par la première tâche non complétée
5. Mettre à jour IMPLEMENTATION_STATUS.yaml (status: in_progress, start_date: aujourd'hui)
```

### Variante 2 : Continuer une phase en cours

```
Je veux continuer la [Phase X - Nom] là où je m'étais arrêté.

1. Lire IMPLEMENTATION_STATUS.yaml pour voir l'état de la phase
2. Identifier la prochaine sous-tâche (done: false) à traiter
3. Continuer l'implémentation
4. Mettre à jour IMPLEMENTATION_STATUS.yaml au fur et à mesure
```

### Variante 3 : Tâche spécifique

```
Je veux travailler spécifiquement sur la tâche [X.Y - Nom de la tâche].

1. Vérifier l'état de cette tâche dans IMPLEMENTATION_STATUS.yaml
2. Lire les spécifications dans ROADMAP_REFERENCE_LIBRARY.md
3. Implémenter les sous-tâches une par une
4. Marquer chaque sous-tâche comme done: true au fur et à mesure
5. Mettre à jour le progress de la tâche
```

### Variante 4 : Résumé d'avancement uniquement

```
Affiche-moi un résumé de l'état d'avancement de la roadmap "Bibliothèque de Minéraux de Référence".

Pour chaque phase :
- Nom de la phase
- Statut (not_started / in_progress / completed / blocked)
- Pourcentage de progression (progress)
- Nombre de tâches complétées / total
- Dates de début et fin (si applicable)
- Blockers éventuels

Puis affiche le pourcentage global d'avancement du projet.
```

### Variante 5 : Déblocage d'une phase bloquée

```
La [Phase X - Nom] est bloquée. Voici le problème : [Description du blocage]

1. Analyser le blocage
2. Proposer des solutions
3. Mettre à jour IMPLEMENTATION_STATUS.yaml (status: blocked, ajouter dans blockers[])
4. Si résolu : retirer de blockers[], status: in_progress
```

---

## 📊 FORMAT DE RAPPORT D'AVANCEMENT ATTENDU

À chaque début de session, Claude devrait afficher :

```
=== ÉTAT D'AVANCEMENT - BIBLIOTHÈQUE DE MINÉRAUX DE RÉFÉRENCE ===

Progression globale : [X]% ████████░░░░░░░░░░░░

┌─────────────────────────────────────────────────────────────┐
│ Phase 1 : Modèle de données et migration                    │
│ Status : [completed] ✅                                      │
│ Progress : 100% ████████████████████████████████            │
│ Dates : 2025-01-17 → 2025-01-21 (4 jours)                   │
│ Tâches : 7/7 complétées                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Phase 2 : Écran Bibliothèque de minéraux                    │
│ Status : [in_progress] 🔄                                    │
│ Progress : 45% ████████████░░░░░░░░░░░░░░░░                │
│ Dates : 2025-01-22 → en cours                                │
│ Tâches : 3/6 complétées                                      │
│ Dernière tâche : 2.3 Fiche détaillée (en cours)             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Phase 3 : Autocomplétion minéraux simples                   │
│ Status : [not_started] ⏸️                                    │
│ Progress : 0% ░░░░░░░░░░░░░░░░░░░░░░░░░░░░                │
│ Dependencies : Phase 1 ✅                                    │
└─────────────────────────────────────────────────────────────┘

...

Prochaine action recommandée :
→ Continuer la Phase 2, Tâche 2.3 (Fiche détaillée)
  Sous-tâche à traiter : "Créer ReferenceMineralDetailScreen.kt"

Prêt à continuer ?
```

---

## 🔄 PROCESSUS DE MISE À JOUR DU STATUS

### Quand une sous-tâche est complétée :

1. Marquer `done: true` dans IMPLEMENTATION_STATUS.yaml
2. Recalculer le `progress` de la tâche parente (nb done / nb total * 100)
3. Si toutes les sous-tâches d'une tâche sont done → task `status: completed`
4. Si toutes les tâches d'une phase sont completed → phase `status: completed`, `completion_date: aujourd'hui`
5. Recalculer `overall_progress` du projet (moyenne pondérée des phases)

### Exemple de mise à jour :

**Avant :**
```yaml
1_1_entity:
  name: "Création de ReferenceMineralEntity"
  status: "in_progress"
  progress: 50
  subtasks:
    - done: true
      description: "Créer ReferenceMineralEntity.kt avec tous les champs"
    - done: false
      description: "Ajouter les indices appropriés"
```

**Après (indices ajoutés) :**
```yaml
1_1_entity:
  name: "Création de ReferenceMineralEntity"
  status: "completed"
  progress: 100
  subtasks:
    - done: true
      description: "Créer ReferenceMineralEntity.kt avec tous les champs"
    - done: true
      description: "Ajouter les indices appropriés"
```

---

## 📝 FORMAT DES COMMITS

**Convention de nommage :**
```
<type>(reference-library): [Phase X.Y] <description courte>

<description détaillée optionnelle>

- Subtask 1 complétée
- Subtask 2 complétée

Refs: docs/IMPLEMENTATION_STATUS.yaml (updated)
```

**Types de commit :**
- `feat` : nouvelle fonctionnalité
- `fix` : correction de bug
- `refactor` : refactoring
- `test` : ajout de tests
- `docs` : documentation
- `chore` : tâches de maintenance

**Exemples :**
```
feat(reference-library): [Phase 1.1] Create ReferenceMineralEntity

- Added all fields (id, names, properties, metadata)
- Added Room annotations and indices
- Prepared for database migration

Refs: docs/IMPLEMENTATION_STATUS.yaml (phase_1.tasks.1_1_entity: completed)
```

```
test(reference-library): [Phase 1.7] Add ReferenceMineralDao tests

- CRUD operations tests
- Search and filter tests
- Usage statistics tests
- Migration tests

Refs: docs/IMPLEMENTATION_STATUS.yaml (phase_1.tasks.1_7_tests: progress 60%)
```

---

## 🚀 CHECKLIST DE FIN DE SESSION

Avant de terminer une session de travail :

- [ ] Tous les commits sont poussés vers la branche distante
- [ ] `docs/IMPLEMENTATION_STATUS.yaml` est à jour
- [ ] Une entrée de session est ajoutée dans `sessions[]`
- [ ] Les tests unitaires des tâches complétées passent
- [ ] Aucune régression sur les fonctionnalités existantes
- [ ] Les notes importantes sont documentées dans `notes[]`

**Exemple d'entrée de session :**
```yaml
sessions:
  - date: "2025-01-17"
    duration_hours: 3
    phases_worked: ["phase_1"]
    tasks_completed: ["1_1_entity", "1_2_dao"]
    notes: |
      Création de l'entité et du DAO. Prochaine étape: migration DB.
      Note: Décidé d'ajouter un champ 'colorVariants' pour gérer les variétés (ex: quartz rose, fumé).
```

---

## 🆘 EN CAS DE PROBLÈME

**Problème : Phase bloquée**
```
→ Mettre à jour IMPLEMENTATION_STATUS.yaml:
  status: "blocked"
  blockers: ["Description du blocage"]
→ Documenter dans notes[]
→ Passer à une autre phase non dépendante si possible
→ Consulter la roadmap pour solutions alternatives
```

**Problème : Tests échouent**
```
→ Ne PAS marquer la tâche comme complétée
→ Créer une note de session avec détails de l'échec
→ Fixer les tests avant de continuer
→ Si besoin, revenir sur les étapes précédentes
```

**Problème : Incompatibilité avec code existant**
```
→ Analyser l'impact
→ Proposer un refactoring si nécessaire
→ Documenter dans notes[]
→ Éventuellement réviser la roadmap si changement majeur
```

---

**Date de dernière mise à jour :** 2025-01-16
