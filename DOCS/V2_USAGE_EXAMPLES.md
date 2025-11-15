# MineraLog v2.0 - Usage Examples

This document provides examples of how to use the new v2.0 mineral aggregates feature.

---

## 📚 Table of Contents

1. [Creating a Simple Mineral](#creating-a-simple-mineral)
2. [Creating an Aggregate Mineral](#creating-an-aggregate-mineral)
3. [Querying Minerals by Type](#querying-minerals-by-type)
4. [Searching Aggregates by Component](#searching-aggregates-by-component)
5. [Getting Component Statistics](#getting-component-statistics)
6. [Updating Components](#updating-components)

---

## Creating a Simple Mineral

Simple minerals work exactly as before in v1.x, but now their properties are stored in a separate `simple_properties` table for better data organization.

### Example: Creating a Quartz Mineral

```kotlin
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
import net.meshcore.mineralog.domain.model.SimpleProperties
import net.meshcore.mineralog.data.mapper.toEntity
import java.time.Instant
import java.util.UUID

// 1. Create the mineral entity with type = SIMPLE
val mineralId = UUID.randomUUID().toString()
val mineralEntity = MineralEntity(
    id = mineralId,
    name = "Quartz Fumé",
    type = "SIMPLE",  // Important: type must be SIMPLE
    notes = "Spécimen de quartz fumé du Brésil",
    createdAt = Instant.now(),
    updatedAt = Instant.now()
)

// 2. Create the properties
val properties = SimpleProperties(
    group = "Silicates",
    formula = "SiO₂",
    mohsMin = 7.0f,
    mohsMax = 7.0f,
    density = 2.65f,
    crystalSystem = "Hexagonal",
    luster = "Vitreous",
    diaphaneity = "Transparent",
    fracture = "Conchoidal"
)

// 3. Convert to entity and save
val propertiesEntity = properties.toEntity(mineralId)

// 4. Save to database (in repository/ViewModel)
mineralDao.insert(mineralEntity)
simplePropertiesDao.insert(propertiesEntity)
```

---

## Creating an Aggregate Mineral

Aggregates are the new feature in v2.0. They represent rocks composed of multiple minerals.

### Example: Creating a Granite Specimen

```kotlin
import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.data.mapper.toEntity

// 1. Create the aggregate mineral entity
val graniteId = UUID.randomUUID().toString()
val graniteEntity = MineralEntity(
    id = graniteId,
    name = "Granite de Bretagne",
    type = "AGGREGATE",  // Important: type must be AGGREGATE
    notes = "Granite à gros grains de la carrière de Huelgoat",
    createdAt = Instant.now(),
    updatedAt = Instant.now()
)

// 2. Define the components
val components = listOf(
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Quartz",
        mineralGroup = "Silicates",
        percentage = 35.0f,
        role = ComponentRole.PRINCIPAL,
        formula = "SiO₂",
        mohsMin = 7.0f,
        mohsMax = 7.0f,
        density = 2.65f,
        crystalSystem = "Hexagonal",
        luster = "Vitreous"
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Feldspath Potassique",
        mineralGroup = "Silicates",
        percentage = 40.0f,
        role = ComponentRole.PRINCIPAL,
        formula = "KAlSi₃O₈",
        mohsMin = 6.0f,
        mohsMax = 6.0f,
        density = 2.56f,
        crystalSystem = "Monoclinic",
        luster = "Vitreous"
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Mica Muscovite",
        mineralGroup = "Silicates",
        percentage = 20.0f,
        role = ComponentRole.ACCESSORY,
        formula = "KAl₂(AlSi₃O₁₀)(OH)₂",
        mohsMin = 2.5f,
        mohsMax = 3.0f,
        density = 2.83f,
        crystalSystem = "Monoclinic",
        luster = "Pearly"
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Biotite",
        mineralGroup = "Silicates",
        percentage = 5.0f,
        role = ComponentRole.TRACE,
        formula = "K(Mg,Fe)₃(AlSi₃O₁₀)(OH)₂",
        mohsMin = 2.5f,
        mohsMax = 3.0f,
        density = 3.1f,
        crystalSystem = "Monoclinic",
        luster = "Vitreous"
    )
)

// 3. Convert components to entities (with display order)
val componentEntities = components.mapIndexed { index, component ->
    component.toEntity(aggregateId = graniteId, displayOrder = index)
}

// 4. Save to database (in repository/ViewModel)
mineralDao.insert(graniteEntity)
mineralComponentDao.insertAll(componentEntities)
```

---

## Querying Minerals by Type

### Get All Simple Minerals

```kotlin
// In ViewModel/Repository
val simpleMinerals: Flow<List<MineralEntity>> = mineralDao.getAllSimpleMinerals()
```

### Get All Aggregates

```kotlin
val aggregates: Flow<List<MineralEntity>> = mineralDao.getAllAggregates()
```

### Filter by Multiple Types

```kotlin
// Get both SIMPLE and AGGREGATE minerals
val allMinerals = mineralDao.getMineralsByType(listOf("SIMPLE", "AGGREGATE"))

// Get only aggregates (as Flow)
val onlyAggregates = mineralDao.getMineralsByType(listOf("AGGREGATE"))
```

### Count by Type

```kotlin
suspend fun getStatistics() {
    val simpleCount = mineralDao.countByType("SIMPLE")
    val aggregateCount = mineralDao.countByType("AGGREGATE")

    println("Simple minerals: $simpleCount")
    println("Aggregates: $aggregateCount")
}
```

---

## Searching Aggregates by Component

Find all aggregates that contain a specific mineral.

### Example: Find All Granites (containing Quartz)

```kotlin
// Search for aggregates containing "Quartz"
val aggregatesWithQuartz: Flow<List<MineralEntity>> =
    mineralComponentDao.searchAggregatesByComponent("%Quartz%")

// Search for exact match
val aggregatesWithFeldspath: Flow<List<MineralEntity>> =
    mineralComponentDao.searchAggregatesByComponent("Feldspath%")
```

### Get Components for a Specific Aggregate

```kotlin
// Get all components for a granite specimen
suspend fun getGraniteComponents(graniteId: String) {
    val components = mineralComponentDao.getByAggregateId(graniteId)

    components.forEach { component ->
        println("${component.mineralName}: ${component.percentage}% (${component.role})")
    }
}

// Output:
// Quartz: 35.0% (PRINCIPAL)
// Feldspath Potassique: 40.0% (PRINCIPAL)
// Mica Muscovite: 20.0% (ACCESSORY)
// Biotite: 5.0% (TRACE)
```

---

## Getting Component Statistics

### Most Frequent Components

```kotlin
suspend fun showMostFrequentComponents() {
    val topComponents = mineralComponentDao.getMostFrequentComponents(limit = 5)

    topComponents.forEach { (mineralName, count) ->
        println("$mineralName appears in $count aggregates")
    }
}

// Output:
// Quartz appears in 45 aggregates
// Feldspath appears in 38 aggregates
// Mica appears in 32 aggregates
// Calcite appears in 28 aggregates
// Biotite appears in 20 aggregates
```

### Get Principal Components

```kotlin
// Get all principal components (>20%) across all aggregates
val principalComponents: Flow<List<MineralComponentEntity>> =
    mineralComponentDao.getAllPrincipalComponents()
```

### Type Distribution

```kotlin
suspend fun showTypeDistribution() {
    val distribution = mineralDao.getTypeDistribution()

    distribution.forEach { (type, count) ->
        println("$type: $count specimens")
    }
}

// Output:
// SIMPLE: 180 specimens
// AGGREGATE: 70 specimens
```

---

## Updating Components

### Add a Component to an Existing Aggregate

```kotlin
suspend fun addComponentToGranite(graniteId: String) {
    // Get current components to determine next display order
    val currentComponents = mineralComponentDao.getByAggregateId(graniteId)
    val nextOrder = currentComponents.size

    // Create new component
    val newComponent = MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Hornblende",
        mineralGroup = "Silicates",
        percentage = 2.0f,
        role = ComponentRole.TRACE,
        mohsMin = 5.0f,
        mohsMax = 6.0f,
        notes = "Présence mineure détectée à l'analyse"
    )

    // Convert and insert
    val componentEntity = newComponent.toEntity(graniteId, nextOrder)
    mineralComponentDao.insert(componentEntity)
}
```

### Update Component Percentage

```kotlin
suspend fun updateComponentPercentage(componentId: String, newPercentage: Float) {
    // Fetch existing component
    val component = mineralComponentDao.getByAggregateId(aggregateId)
        .find { it.id == componentId } ?: return

    // Determine new role based on percentage
    val newRole = when {
        newPercentage > 20f -> "PRINCIPAL"
        newPercentage >= 5f -> "ACCESSORY"
        else -> "TRACE"
    }

    // Update
    val updated = component.copy(
        percentage = newPercentage,
        role = newRole,
        updatedAt = Instant.now()
    )

    mineralComponentDao.update(updated)
}
```

### Delete a Component

```kotlin
suspend fun removeComponent(componentId: String) {
    mineralComponentDao.deleteById(componentId)
}
```

---

## Complete Example: Repository Method

Here's a complete example of a repository method that handles both simple and aggregate minerals:

```kotlin
class MineralRepository(
    private val mineralDao: MineralDao,
    private val simplePropertiesDao: SimplePropertiesDao,
    private val mineralComponentDao: MineralComponentDao
) {
    /**
     * Insert a simple mineral with its properties.
     */
    suspend fun insertSimpleMineral(
        name: String,
        properties: SimpleProperties,
        notes: String? = null
    ): String {
        val mineralId = UUID.randomUUID().toString()

        val mineralEntity = MineralEntity(
            id = mineralId,
            name = name,
            type = "SIMPLE",
            notes = notes,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val propertiesEntity = properties.toEntity(mineralId)

        mineralDao.insert(mineralEntity)
        simplePropertiesDao.insert(propertiesEntity)

        return mineralId
    }

    /**
     * Insert an aggregate mineral with its components.
     */
    suspend fun insertAggregate(
        name: String,
        components: List<MineralComponent>,
        notes: String? = null
    ): String {
        require(components.isNotEmpty()) { "Aggregate must have at least one component" }

        val aggregateId = UUID.randomUUID().toString()

        val mineralEntity = MineralEntity(
            id = aggregateId,
            name = name,
            type = "AGGREGATE",
            notes = notes,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val componentEntities = components.mapIndexed { index, component ->
            component.toEntity(aggregateId, index)
        }

        mineralDao.insert(mineralEntity)
        mineralComponentDao.insertAll(componentEntities)

        return aggregateId
    }

    /**
     * Get a mineral with all its data (simple or aggregate).
     */
    suspend fun getMineralById(mineralId: String): Pair<MineralEntity, Any>? {
        val mineral = mineralDao.getById(mineralId) ?: return null

        return when (mineral.type) {
            "SIMPLE" -> {
                val properties = simplePropertiesDao.getByMineralId(mineralId)
                mineral to (properties ?: return null)
            }
            "AGGREGATE" -> {
                val components = mineralComponentDao.getByAggregateId(mineralId)
                mineral to components
            }
            else -> null
        }
    }
}
```

---

## Migration from v1.x

All existing minerals from v1.x are automatically migrated to v2.0 as `SIMPLE` type minerals. Their properties are copied to the `simple_properties` table.

**No action required from users** - the migration happens automatically on app upgrade.

---

## Best Practices

### 1. Component Percentages Should Sum to 100%

```kotlin
fun validateComponentPercentages(components: List<MineralComponent>): Boolean {
    val total = components.mapNotNull { it.percentage }.sum()
    return total in 99.0f..101.0f  // Allow 1% tolerance for rounding
}
```

### 2. Use Appropriate Roles

- **PRINCIPAL**: > 20% (major minerals defining the rock)
- **ACCESSORY**: 5-20% (significant but not dominant)
- **TRACE**: < 5% (minor minerals)

### 3. Minimum Components for Aggregates

Aggregates should have at least 2 components. A single-component "aggregate" should be a SIMPLE mineral instead.

```kotlin
require(components.size >= 2) {
    "Aggregate must have at least 2 components. Use SIMPLE type for single minerals."
}
```

---

## Next Steps

- **Phase 4**: Repository methods will be added to handle sealed class Mineral variants
- **Phase 5-7**: UI components for creating and viewing aggregates
- **Phase 8**: Statistics and CSV export/import for aggregates

---

**Version:** 2.0.0-alpha
**Last Updated:** 2025-01-15
