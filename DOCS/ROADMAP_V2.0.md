# MineraLog v2.0 - Roadmap: Support des Agrégats Minéraux

**Version:** 2.0.0
**Statut:** Planification
**Date de création:** 2025-01-15
**Auteur:** Julien Bombled

---

## 📋 Résumé Exécutif

MineraLog v2.0 vise à transformer l'application d'un catalogueur de minéraux individuels vers une solution complète capable de gérer:
- **Minéraux simples** (ex: quartz, pyrite)
- **Agrégats minéraux** (ex: granite = quartz + feldspath + mica)
- **Roches polymétalliques** (ex: gneiss avec multiples composants)
- **Assemblages minéralogiques** complexes

Cette évolution répond à un besoin réel: un agrégat ne peut pas avoir une seule valeur de dureté, de densité, ou de composition chimique.

---

## 🎯 Vision

### Version Actuelle (v1.x) - Limitations

```kotlin
data class Mineral(
    val id: String,
    val name: String,
    val group: String?,
    val mohsMin: Float?,      // ❌ Une seule plage de dureté
    val mohsMax: Float?,
    val density: Float?,       // ❌ Une seule densité
    val formula: String?,      // ❌ Une seule formule chimique
    // ...
)
```

**Problèmes identifiés:**
- ❌ Impossible de représenter un granite (quartz 7, feldspath 6, mica 2.5-3)
- ❌ Impossible de documenter les pourcentages de composition
- ❌ Tri par dureté inapproprié pour les agrégats
- ❌ Filtrage par propriétés physiques inexact
- ❌ Pas de notion de "minéral principal" vs "minéraux accessoires"

### Version 2.0 - Solution

```kotlin
data class Mineral(
    val id: String,
    val name: String,
    val type: MineralType,              // ✅ SIMPLE ou AGGREGATE
    val simpleProperties: SimpleProperties?,  // ✅ Pour minéraux simples
    val components: List<MineralComponent>?,  // ✅ Pour agrégats
    // ... autres propriétés communes
)

enum class MineralType {
    SIMPLE,      // Minéral unique (quartz, pyrite...)
    AGGREGATE,   // Agrégat minéral (granite, gneiss...)
    ROCK         // Roche (pour extension future)
}

data class SimpleProperties(
    val group: String?,
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    // ... propriétés d'un minéral simple
)

data class MineralComponent(
    val id: String,
    val mineralName: String,           // Nom du composant (ex: "Quartz")
    val mineralGroup: String?,         // Groupe (ex: "Silicates")
    val percentage: Float?,            // % volumique ou massique (ex: 35.0)
    val role: ComponentRole,           // Principal, Accessoire, Trace
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    val notes: String?
)

enum class ComponentRole {
    PRINCIPAL,    // Composant majoritaire (> 20%)
    ACCESSORY,    // Composant accessoire (5-20%)
    TRACE         // Trace (< 5%)
}
```

---

## 🗄️ Architecture Technique

### 1. Base de Données - Schema Migration

#### Nouvelles Tables (Room Database)

```kotlin
// Table existante modifiée
@Entity(tableName = "minerals")
data class MineralEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,  // "SIMPLE" ou "AGGREGATE"

    // Propriétés communes (provenance, storage, photos, tags, notes...)
    val provenanceId: String?,
    val storageId: String?,
    val acquisitionDate: String?,
    val price: Float?,
    val tags: String?,
    val notes: String?,
    val statusType: String?,
    val qualityRating: Int?,

    // ⚠️ Propriétés simples DEPRECATED pour type=AGGREGATE
    // (gardées pour backward compatibility)
    @Deprecated("Use SimplePropertiesEntity for type=SIMPLE")
    val group: String?,
    @Deprecated("Use SimplePropertiesEntity for type=SIMPLE")
    val mohsMin: Float?,
    @Deprecated("Use SimplePropertiesEntity for type=SIMPLE")
    val mohsMax: Float?,
    // ... autres propriétés deprecated

    val createdAt: String,
    val updatedAt: String
)

// Nouvelle table pour propriétés des minéraux simples
@Entity(
    tableName = "simple_properties",
    foreignKeys = [ForeignKey(
        entity = MineralEntity::class,
        parentColumns = ["id"],
        childColumns = ["mineralId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SimplePropertiesEntity(
    @PrimaryKey val id: String,
    val mineralId: String,
    val group: String?,
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    val luster: String?,
    val diaphaneity: String?,
    val cleavage: String?,
    val fracture: String?,
    val habit: String?,
    val streak: String?,
    val fluorescence: String?
)

// Nouvelle table pour composants d'agrégats
@Entity(
    tableName = "mineral_components",
    foreignKeys = [ForeignKey(
        entity = MineralEntity::class,
        parentColumns = ["id"],
        childColumns = ["aggregateId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aggregateId"), Index("role")]
)
data class MineralComponentEntity(
    @PrimaryKey val id: String,
    val aggregateId: String,       // FK vers minerals table
    val displayOrder: Int,         // Ordre d'affichage (0, 1, 2...)

    val mineralName: String,       // "Quartz"
    val mineralGroup: String?,     // "Silicates"
    val percentage: Float?,        // 35.0 (%)
    val role: String,              // "PRINCIPAL", "ACCESSORY", "TRACE"

    // Propriétés physiques du composant
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    val luster: String?,
    val diaphaneity: String?,
    val cleavage: String?,
    val fracture: String?,
    val habit: String?,
    val streak: String?,
    val fluorescence: String?,

    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

#### Migration Strategy

```kotlin
@Database(
    entities = [
        MineralEntity::class,
        SimplePropertiesEntity::class,  // NEW
        MineralComponentEntity::class,   // NEW
        ProvenanceEntity::class,
        StorageEntity::class,
        PhotoEntity::class,
        FilterPresetEntity::class
    ],
    version = 2,  // Upgrade from v1
    exportSchema = true
)
abstract class MineraLogDatabase : RoomDatabase() {
    // ...
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Créer nouvelles tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS simple_properties (
                id TEXT PRIMARY KEY NOT NULL,
                mineralId TEXT NOT NULL,
                `group` TEXT,
                mohsMin REAL,
                mohsMax REAL,
                density REAL,
                formula TEXT,
                crystalSystem TEXT,
                luster TEXT,
                diaphaneity TEXT,
                cleavage TEXT,
                fracture TEXT,
                habit TEXT,
                streak TEXT,
                fluorescence TEXT,
                FOREIGN KEY(mineralId) REFERENCES minerals(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS mineral_components (
                id TEXT PRIMARY KEY NOT NULL,
                aggregateId TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                mineralName TEXT NOT NULL,
                mineralGroup TEXT,
                percentage REAL,
                role TEXT NOT NULL,
                mohsMin REAL,
                mohsMax REAL,
                density REAL,
                formula TEXT,
                crystalSystem TEXT,
                luster TEXT,
                diaphaneity TEXT,
                cleavage TEXT,
                fracture TEXT,
                habit TEXT,
                streak TEXT,
                fluorescence TEXT,
                notes TEXT,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                FOREIGN KEY(aggregateId) REFERENCES minerals(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("CREATE INDEX index_mineral_components_aggregateId ON mineral_components(aggregateId)")
        database.execSQL("CREATE INDEX index_mineral_components_role ON mineral_components(role)")

        // 2. Ajouter colonne 'type' à minerals table (default = 'SIMPLE')
        database.execSQL("ALTER TABLE minerals ADD COLUMN type TEXT NOT NULL DEFAULT 'SIMPLE'")

        // 3. Migrer données existantes vers simple_properties
        database.execSQL("""
            INSERT INTO simple_properties (
                id, mineralId, `group`, mohsMin, mohsMax, density, formula,
                crystalSystem, luster, diaphaneity, cleavage, fracture,
                habit, streak, fluorescence
            )
            SELECT
                id || '_props' as id,
                id as mineralId,
                `group`, mohsMin, mohsMax, density, formula,
                crystalSystem, luster, diaphaneity, cleavage, fracture,
                habit, streak, fluorescence
            FROM minerals
        """)

        // Note: Les colonnes deprecated restent dans minerals table pour compatibilité
        // Elles seront supprimées dans migration 2->3
    }
}
```

---

### 2. Data Layer - Repository & DAO

```kotlin
@Dao
interface MineralDao {
    // Queries existantes (inchangées)
    @Query("SELECT * FROM minerals WHERE type = 'SIMPLE' ORDER BY name ASC")
    fun getAllSimpleMinerals(): Flow<List<MineralEntity>>

    // Nouvelles queries pour agrégats
    @Query("SELECT * FROM minerals WHERE type = 'AGGREGATE' ORDER BY name ASC")
    fun getAllAggregates(): Flow<List<MineralEntity>>

    @Query("SELECT * FROM minerals ORDER BY name ASC")
    fun getAllMineralsAndAggregates(): Flow<List<MineralEntity>>

    // Filtrage par type
    @Query("SELECT * FROM minerals WHERE type IN (:types) ORDER BY name ASC")
    fun getMineralsByType(types: List<String>): Flow<List<MineralEntity>>
}

@Dao
interface SimplePropertiesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(properties: SimplePropertiesEntity)

    @Update
    suspend fun update(properties: SimplePropertiesEntity)

    @Query("SELECT * FROM simple_properties WHERE mineralId = :mineralId")
    suspend fun getByMineralId(mineralId: String): SimplePropertiesEntity?

    @Query("SELECT * FROM simple_properties WHERE mineralId = :mineralId")
    fun getByMineralIdFlow(mineralId: String): Flow<SimplePropertiesEntity?>

    @Query("DELETE FROM simple_properties WHERE mineralId = :mineralId")
    suspend fun deleteByMineralId(mineralId: String)
}

@Dao
interface MineralComponentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(component: MineralComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<MineralComponentEntity>)

    @Update
    suspend fun update(component: MineralComponentEntity)

    @Delete
    suspend fun delete(component: MineralComponentEntity)

    @Query("SELECT * FROM mineral_components WHERE aggregateId = :aggregateId ORDER BY displayOrder ASC")
    suspend fun getByAggregateId(aggregateId: String): List<MineralComponentEntity>

    @Query("SELECT * FROM mineral_components WHERE aggregateId = :aggregateId ORDER BY displayOrder ASC")
    fun getByAggregateIdFlow(aggregateId: String): Flow<List<MineralComponentEntity>>

    @Query("DELETE FROM mineral_components WHERE aggregateId = :aggregateId")
    suspend fun deleteByAggregateId(aggregateId: String)

    @Query("DELETE FROM mineral_components WHERE id = :componentId")
    suspend fun deleteById(componentId: String)

    // Recherche par composant (ex: "tous les agrégats contenant du quartz")
    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        INNER JOIN mineral_components c ON m.id = c.aggregateId
        WHERE c.mineralName LIKE :componentName
        ORDER BY m.name ASC
    """)
    fun searchAggregatesByComponent(componentName: String): Flow<List<MineralEntity>>
}
```

---

### 3. Domain Layer - Models

```kotlin
sealed class Mineral {
    abstract val id: String
    abstract val name: String
    abstract val provenance: Provenance?
    abstract val storage: Storage?
    abstract val photos: List<Photo>
    abstract val acquisitionDate: String?
    abstract val price: Float?
    abstract val tags: String?
    abstract val notes: String?
    abstract val statusType: StatusType?
    abstract val qualityRating: Int?
    abstract val createdAt: String
    abstract val updatedAt: String

    data class Simple(
        override val id: String,
        override val name: String,
        val properties: SimpleProperties,
        override val provenance: Provenance?,
        override val storage: Storage?,
        override val photos: List<Photo>,
        override val acquisitionDate: String?,
        override val price: Float?,
        override val tags: String?,
        override val notes: String?,
        override val statusType: StatusType?,
        override val qualityRating: Int?,
        override val createdAt: String,
        override val updatedAt: String
    ) : Mineral()

    data class Aggregate(
        override val id: String,
        override val name: String,
        val components: List<MineralComponent>,
        override val provenance: Provenance?,
        override val storage: Storage?,
        override val photos: List<Photo>,
        override val acquisitionDate: String?,
        override val price: Float?,
        override val tags: String?,
        override val notes: String?,
        override val statusType: StatusType?,
        override val qualityRating: Int?,
        override val createdAt: String,
        override val updatedAt: String
    ) : Mineral() {
        // Propriétés calculées pour agrégats
        val primaryComponents: List<MineralComponent>
            get() = components.filter { it.role == ComponentRole.PRINCIPAL }

        val hardnessRange: ClosedFloatingPointRange<Float>?
            get() {
                val allHardnesses = components.mapNotNull {
                    listOfNotNull(it.mohsMin, it.mohsMax)
                }.flatten()
                return if (allHardnesses.isNotEmpty()) {
                    allHardnesses.minOrNull()!!..allHardnesses.maxOrNull()!!
                } else null
            }

        val averageDensity: Float?
            get() {
                val densities = components.mapNotNull {
                    it.density?.let { d -> it.percentage?.let { p -> d * (p / 100f) } }
                }
                return if (densities.isNotEmpty()) densities.sum() else null
            }
    }
}

data class SimpleProperties(
    val group: String?,
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    val luster: String?,
    val diaphaneity: String?,
    val cleavage: String?,
    val fracture: String?,
    val habit: String?,
    val streak: String?,
    val fluorescence: String?
)

data class MineralComponent(
    val id: String,
    val mineralName: String,
    val mineralGroup: String?,
    val percentage: Float?,
    val role: ComponentRole,
    val mohsMin: Float?,
    val mohsMax: Float?,
    val density: Float?,
    val formula: String?,
    val crystalSystem: String?,
    val luster: String?,
    val diaphaneity: String?,
    val cleavage: String?,
    val fracture: String?,
    val habit: String?,
    val streak: String?,
    val fluorescence: String?,
    val notes: String?
)

enum class ComponentRole {
    PRINCIPAL,
    ACCESSORY,
    TRACE
}
```

---

## 🎨 UI/UX Changes

### 1. Écran d'Ajout/Édition - Type Selector

```
┌─────────────────────────────────────┐
│ Ajouter un Minéral                  │
├─────────────────────────────────────┤
│                                     │
│ Type de minéral                     │
│ ┌─────────────┐ ┌─────────────┐   │
│ │  Minéral    │ │   Agrégat   │   │
│ │   Simple    │ │  (Roche)    │   │
│ │ ┌─────────┐ │ │ ┌─────────┐ │   │
│ │ │  💎     │ │ │ │  🪨     │ │   │
│ │ └─────────┘ │ │ └─────────┘ │   │
│ │  Sélectionné│ │             │   │
│ └─────────────┘ └─────────────┘   │
│                                     │
│ Nom du minéral                      │
│ ┌─────────────────────────────────┐│
│ │ Quartz                          ││
│ └─────────────────────────────────┘│
│                                     │
│ [Propriétés physiques...]           │
│                                     │
└─────────────────────────────────────┘
```

### 2. Mode Agrégat - Component Editor

```
┌─────────────────────────────────────┐
│ Ajouter un Agrégat                  │
├─────────────────────────────────────┤
│ Nom de l'agrégat                    │
│ ┌─────────────────────────────────┐│
│ │ Granite de Bretagne             ││
│ └─────────────────────────────────┘│
│                                     │
│ Composants minéraux                 │
│ ┌─────────────────────────────────┐│
│ │ 🔷 Quartz               35%     ││
│ │    Principal • Dureté: 7        ││
│ │    Silicates                    ││
│ │                          [✏️] [🗑️]│
│ ├─────────────────────────────────┤│
│ │ 🔶 Feldspath            40%     ││
│ │    Principal • Dureté: 6        ││
│ │    Silicates                    ││
│ │                          [✏️] [🗑️]│
│ ├─────────────────────────────────┤│
│ │ ⚫ Mica                  20%     ││
│ │    Accessoire • Dureté: 2.5-3   ││
│ │    Silicates                    ││
│ │                          [✏️] [🗑️]│
│ ├─────────────────────────────────┤│
│ │ 🟤 Biotite               5%     ││
│ │    Trace • Dureté: 2.5          ││
│ │    Silicates                    ││
│ │                          [✏️] [🗑️]│
│ └─────────────────────────────────┘│
│                                     │
│ [+ Ajouter un composant]            │
│                                     │
│ Propriétés calculées                │
│ ┌─────────────────────────────────┐│
│ │ Dureté globale: 2.5 - 7         ││
│ │ Densité moyenne: 2.68 g/cm³     ││
│ │ Composants principaux: 2        ││
│ └─────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### 3. Fiche Détail - Aggregate View

```
┌─────────────────────────────────────┐
│ 🪨 Granite de Bretagne              │
├─────────────────────────────────────┤
│ [Photo principale]                  │
│                                     │
│ Type: Agrégat minéral               │
│                                     │
│ Composition                         │
│ ┌─────────────────────────────────┐│
│ │ ████████████░░░░░░░░░ 35%       ││ Quartz
│ │ █████████████░░░░░░░░ 40%       ││ Feldspath
│ │ ██████░░░░░░░░░░░░░░░ 20%       ││ Mica
│ │ ██░░░░░░░░░░░░░░░░░░░  5%       ││ Biotite
│ └─────────────────────────────────┘│
│                                     │
│ Propriétés physiques                │
│ • Dureté: 2.5 - 7 (Mohs)            │
│ • Densité: 2.68 g/cm³ (moyenne)     │
│ • Système cristallin: Mixte         │
│                                     │
│ Composants détaillés                │
│ ┌─────────────────────────────────┐│
│ │ 🔷 Quartz (35% - Principal)     ││
│ │    Silicates • SiO₂             ││
│ │    Dureté: 7 • Densité: 2.65    ││
│ │    Hexagonal • Éclat vitreux    ││
│ │    [Voir détails →]             ││
│ ├─────────────────────────────────┤│
│ │ 🔶 Feldspath (40% - Principal)  ││
│ │    Silicates • KAlSi₃O₈         ││
│ │    Dureté: 6 • Densité: 2.56    ││
│ │    Monoclinique • Nacré          ││
│ │    [Voir détails →]             ││
│ └─────────────────────────────────┘│
│                                     │
│ [Provenance] [Stockage] [Photos]    │
└─────────────────────────────────────┘
```

### 4. Liste - Badge pour Agrégats

```
┌─────────────────────────────────────┐
│ 🔍 Rechercher...        [🔽] [🔍]   │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐│
│ │ 💎 Améthyste                    ││
│ │    Silicates • Dureté: 7        ││
│ │    Brésil                       ││
│ ├─────────────────────────────────┤│
│ │ 🪨 Granite de Bretagne [🏷️]     ││
│ │    Agrégat (4 composants)       ││
│ │    Dureté: 2.5-7 • France       ││
│ ├─────────────────────────────────┤│
│ │ 💎 Pyrite                       ││
│ │    Sulfures • Dureté: 6-6.5     ││
│ │    Espagne                      ││
│ └─────────────────────────────────┘│
└─────────────────────────────────────┘

🏷️ Badge "Agrégat" pour différenciation visuelle
```

### 5. Recherche & Filtres Avancés

```
┌─────────────────────────────────────┐
│ Filtres Avancés                     │
├─────────────────────────────────────┤
│ Type de minéral                     │
│ ☑ Minéraux simples                  │
│ ☑ Agrégats                          │
│                                     │
│ Recherche par composant             │
│ ┌─────────────────────────────────┐│
│ │ Quartz                          ││
│ └─────────────────────────────────┘│
│ → Trouve tous les agrégats          │
│   contenant du quartz               │
│                                     │
│ Dureté (pour agrégats)              │
│ ○ Dureté minimale du plus tendre    │
│ ○ Dureté maximale du plus dur       │
│ ● Plage de dureté complète          │
│                                     │
│ ┌─────────────────────────────────┐│
│ │ Min: [1.0] ━━●━━━━━━━ Max: [7.0]││
│ └─────────────────────────────────┘│
│                                     │
│ Pourcentage de composant            │
│ Quartz ≥ [20]%                      │
│                                     │
│ Rôle dans l'agrégat                 │
│ ☑ Principal                         │
│ ☐ Accessoire                        │
│ ☐ Trace                             │
│                                     │
│         [Réinitialiser] [Appliquer] │
└─────────────────────────────────────┘
```

---

## 📊 Tri & Statistiques

### Tri Intelligent pour Agrégats

```kotlin
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    GROUP,

    // Tri par dureté adapté au type
    HARDNESS_LOW,    // Simple: mohsMin ASC
                     // Aggregate: MIN(components.mohsMin) ASC

    HARDNESS_HIGH,   // Simple: mohsMax DESC
                     // Aggregate: MAX(components.mohsMax) DESC

    // Nouveaux tris pour agrégats
    COMPONENT_COUNT_ASC,   // Nombre de composants (ASC)
    COMPONENT_COUNT_DESC,  // Nombre de composants (DESC)
    COMPLEXITY_ASC,        // Complexité (diversité des groupes)
    COMPLEXITY_DESC
}
```

### Statistiques Étendues

```
📊 Statistiques

Collection totale: 250 entrées
├─ Minéraux simples: 180 (72%)
└─ Agrégats: 70 (28%)

Par type d'agrégat:
├─ Roches ignées: 25
├─ Roches métamorphiques: 30
└─ Roches sédimentaires: 15

Composants les plus fréquents:
1. Quartz - présent dans 45 agrégats
2. Feldspath - présent dans 38 agrégats
3. Mica - présent dans 32 agrégats
4. Calcite - présent dans 28 agrégats
5. Biotite - présent dans 20 agrégats

Complexité moyenne: 3.2 composants/agrégat
```

---

## 🚀 Plan d'Implémentation

### Phase 1: Fondations (Sprint 1-2) - 3 semaines

**Objectifs:**
- Migration de base de données (v1 → v2)
- Nouvelles tables et DAOs
- Couche Domain avec sealed class Mineral
- Tests de migration sur données réelles

**Livrables:**
- ✅ Schema v2 + migration automatique
- ✅ SimplePropertiesDao + MineralComponentDao
- ✅ Domain models (Mineral.Simple, Mineral.Aggregate)
- ✅ Tests unitaires de migration
- ✅ Backward compatibility garantie

**Risques:**
- ⚠️ Migration peut échouer sur grandes collections
- ⚠️ Performances requêtes avec JOINs multiples

### Phase 2: UI - Création/Édition (Sprint 3-4) - 4 semaines

**Objectifs:**
- Type selector (Simple vs Aggregate)
- Component editor pour agrégats
- Validation des pourcentages (total = 100%)
- Auto-completion des noms de minéraux

**Livrables:**
- ✅ AddMineralScreen avec type selector
- ✅ EditMineralScreen adapté aux 2 types
- ✅ ComponentEditorScreen (modal)
- ✅ Validation côté client
- ✅ Preview des propriétés calculées

**Risques:**
- ⚠️ UX complexe pour utilisateurs non experts
- ⚠️ Performance avec listes longues de composants (> 10)

### Phase 3: UI - Affichage (Sprint 5) - 2 semaines

**Objectifs:**
- MineralDetailScreen adapté aux agrégats
- Composable ComponentCard réutilisable
- Graphiques de composition (pie chart)
- Badge "Agrégat" dans la liste

**Livrables:**
- ✅ MineralDetailScreen.Aggregate
- ✅ ComponentListCard
- ✅ Composition chart (Material3)
- ✅ Badge visuel dans HomeScreen

### Phase 4: Recherche & Filtres (Sprint 6) - 2 semaines

**Objectifs:**
- Recherche par composant
- Filtres avancés pour agrégats
- Tri adaptatif (simple vs aggregate)
- Performances optimisées (indexation)

**Livrables:**
- ✅ searchAggregatesByComponent() DAO method
- ✅ FilterCriteria.Aggregate
- ✅ Smart sorting (adapté au type)
- ✅ Index database pour composants

### Phase 5: Statistiques & Outils (Sprint 7) - 2 semaines

**Objectifs:**
- Statistiques par type (simple/aggregate)
- Top composants les plus fréquents
- Export CSV avec composants
- Import CSV avec parsing composants

**Livrables:**
- ✅ StatisticsRepository.getAggregateStats()
- ✅ CSV export format v2 (multi-row pour agrégats)
- ✅ CSV import parser v2
- ✅ Migration export v1 → v2

### Phase 6: Tests & Polissage (Sprint 8) - 2 semaines

**Objectifs:**
- Tests d'intégration E2E
- Tests utilisateurs alpha
- Documentation utilisateur
- Corrections de bugs

**Livrables:**
- ✅ Suite de tests automatisés complète
- ✅ Documentation FR/EN
- ✅ Tutoriel in-app
- ✅ Beta release sur Google Play

---

## 📅 Timeline Globale

```
Janvier 2025   │ Phase 1: Fondations
Février 2025   │ Phase 1 (fin) + Phase 2: UI Création
Mars 2025      │ Phase 2 (fin) + Phase 3: UI Affichage
Avril 2025     │ Phase 4: Recherche & Filtres
Mai 2025       │ Phase 5: Statistiques & Outils
Juin 2025      │ Phase 6: Tests & Polissage
Juillet 2025   │ Release v2.0.0 stable
```

**Estimation totale:** 17 semaines (≈ 4 mois)

---

## 🔄 Rétrocompatibilité

### Stratégie de Migration

**Minéraux existants (v1.x):**
- Tous migrés comme `type = SIMPLE`
- Propriétés copiées dans `simple_properties` table
- Colonnes deprecated conservées (lecture seule)
- Zéro perte de données garantie

**Export/Import:**
- CSV v1 format toujours supporté (import only)
- CSV v2 format avec support agrégats (export/import)
- Détection automatique du format à l'import

**API Publique:**
```kotlin
// v1 - DEPRECATED mais supporté
@Deprecated("Use MineralRepository.insert(Mineral.Simple)")
suspend fun insertMineral(mineral: LegacyMineral) {
    // Conversion automatique vers Mineral.Simple
}

// v2 - Nouvelle API
suspend fun insert(mineral: Mineral.Simple)
suspend fun insert(mineral: Mineral.Aggregate)
```

---

## 🎓 Documentation Utilisateur

### Guide d'Utilisation

**"Quand utiliser un Minéral Simple vs Agrégat?"**

| Type | Exemples | Utilisation |
|------|----------|-------------|
| **Minéral Simple** | Quartz, Pyrite, Améthyste, Malachite | Un seul minéral identifiable avec des propriétés homogènes |
| **Agrégat** | Granite, Gneiss, Basalte, Gabbro | Roche composée de plusieurs minéraux distincts |

**Workflow typique - Créer un agrégat:**

1. **Identifier les composants** (observation microscopique ou macroscopique)
2. **Estimer les pourcentages** (volumique ou massique)
3. **Classer par rôle** (Principal > 20%, Accessoire 5-20%, Trace < 5%)
4. **Documenter chaque composant** (nom, groupe, propriétés)
5. **Laisser l'app calculer** les propriétés globales automatiquement

**Bonnes pratiques:**
- ✅ Utiliser noms standards (ex: "Feldspath potassique" pas "Feldspath rose")
- ✅ Total des % = 100% (validation automatique)
- ✅ Minimum 2 composants pour un agrégat
- ✅ Documenter l'incertitude dans les notes

---

## 🔬 Fonctionnalités Avancées (Post-v2.0)

### v2.1 - Bibliothèque de Minéraux

**Problème:** Utilisateurs doivent ressaisir propriétés de chaque composant

**Solution:**
- Base de données intégrée de ~500 minéraux communs
- Auto-completion avec pré-remplissage des propriétés
- "Ajouter depuis bibliothèque" → ajuste seulement le %

```
[Ajouter un composant]
  → Depuis la bibliothèque (500+ minéraux)
     ┌─────────────────────────────┐
     │ 🔍 Quartz                   │
     │ ┌─────────────────────────┐ │
     │ │ 💎 Quartz               │ │
     │ │    SiO₂ • Dureté: 7     │ │
     │ │    Densité: 2.65        │ │
     │ │    [Sélectionner]       │ │
     │ └─────────────────────────┘ │
     └─────────────────────────────┘
  → Saisie manuelle
```

### v2.2 - Lames Minces Virtuelles

**Fonctionnalité:**
- Import photo de lame mince (microscope)
- Annotation interactive des composants
- Calcul automatique des % par analyse d'image
- Export rapport PDF avec photos annotées

### v2.3 - Analyse Chimique

**Fonctionnalité:**
- Saisie composition chimique élémentaire (%)
- Calcul formule chimique théorique
- Validation cohérence composition/minéralogie
- Comparaison avec références

---

## 📚 Ressources & Références

### Standards Géologiques

- **IMA** (International Mineralogical Association) - Nomenclature officielle
- **QAPF Diagram** - Classification des roches ignées
- **Streckeisen Classification** - Roches plutoniques et volcaniques
- **Dunham Classification** - Roches carbonatées

### Bibliothèques Tierces

- **mindat.org API** - Base de données minéralogique (7000+ espèces)
- **webmineral.com** - Propriétés physiques et chimiques
- **rruff.info** - Spectres Raman et données cristallographiques

---

## ✅ Critères de Succès

**Technique:**
- ✅ Migration automatique sans perte de données (100% des cas)
- ✅ Performance: affichage agrégat < 500ms
- ✅ Couverture tests: > 80%
- ✅ Backward compatibility complète avec v1.x

**Utilisateur:**
- ✅ 90% des utilisateurs comprennent la différence Simple/Agrégat (onboarding)
- ✅ Temps moyen de création d'un agrégat < 5 minutes
- ✅ Taux d'erreur validation < 5%
- ✅ Note Google Play maintenue ≥ 4.5/5

**Business:**
- ✅ 0 bug critique en production (30 premiers jours)
- ✅ Adoption feature agrégats: > 30% des utilisateurs actifs (3 mois)
- ✅ Rétention maintenue ou améliorée

---

## 🤝 Contribution

Cette roadmap est ouverte aux suggestions de la communauté.

**Pour proposer une amélioration:**
1. Ouvrir une issue GitHub avec label `v2.0-proposal`
2. Décrire le cas d'usage et l'impact utilisateur
3. Proposer une solution technique (optionnel)

**Domaines prioritaires:**
- UX pour utilisateurs débutants en minéralogie
- Performances avec grandes collections (> 1000 entrées)
- Accessibilité (lecteurs d'écran, contraste...)
- Internationalisation (termes géologiques)

---

## 📝 Changelog Prévisionnel

### v2.0.0 (Juillet 2025)

**BREAKING CHANGES:**
- Database schema v1 → v2 (migration automatique)
- `Mineral` data class devient sealed class
- API Repository modifiée (nouveaux paramètres)

**Features:**
- ✨ Support complet des agrégats minéraux
- ✨ Éditeur de composants avec validation
- ✨ Propriétés calculées automatiques (dureté, densité)
- ✨ Recherche par composant
- ✨ Statistiques agrégats
- ✨ Export/Import CSV v2

**Improvements:**
- 🚀 Performances requêtes avec indexation avancée
- 🎨 UI redesign pour agrégats
- 📊 Charts de composition (pie charts)
- 🔍 Filtres avancés pour agrégats

**Fixes:**
- 🐛 Tri par dureté incorrect pour agrégats
- 🐛 Filtrage densité inexact
- 🐛 Export CSV perte informations composants

---

**Auteur:** Julien Bombled
**Contact:** [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)
**License:** Apache 2.0
**Dernière mise à jour:** 2025-01-15

---

*Ce document est vivant et sera mis à jour régulièrement selon les retours de la communauté et l'avancement du développement.*
