# MineraLog v2.0 - Mineral Aggregates Support

**Version:** 2.0.0-alpha
**Status:** Database & Repository Ready
**Last Updated:** 2025-01-15

---

## 🎯 What's New in v2.0?

MineraLog v2.0 introduces **support for mineral aggregates** - rocks composed of multiple minerals like Granite, Gneiss, Basalt, etc.

### Key Features

✅ **Dual mineral types:**
- **SIMPLE**: Traditional single minerals (Quartz, Pyrite, Amethyst...)
- **AGGREGATE**: Rocks with multiple components (Granite = Quartz + Feldspath + Mica)

✅ **Component management:**
- Each component has its own properties (hardness, density, formula...)
- Percentage composition tracking
- Role classification (PRINCIPAL > 20%, ACCESSORY 5-20%, TRACE < 5%)

✅ **Backward compatible:**
- All v1.x data automatically migrated to v2.0
- Existing minerals become "SIMPLE" type
- Zero data loss guaranteed

---

## 🚀 Quick Start

### 1. Creating a Simple Mineral

Simple minerals work as before, but properties are now in a separate table:

```kotlin
import net.meshcore.mineralog.data.repository.*
import net.meshcore.mineralog.domain.model.SimpleProperties

// Create properties
val quartzProps = SimpleProperties(
    group = "Silicates",
    formula = "SiO₂",
    mohsMin = 7f,
    mohsMax = 7f,
    density = 2.65f,
    crystalSystem = "Hexagonal",
    luster = "Vitreous"
)

// Create simple mineral data
val quartz = SimpleMineralData(
    name = "Quartz Fumé",
    properties = quartzProps,
    notes = "Spécimen du Brésil"
)

// Insert via repository extension
val mineralId = mineralRepository.insertSimpleMineral(quartz)
```

### 2. Creating an Aggregate Mineral

Aggregates are the new feature - rocks with multiple components:

```kotlin
import net.meshcore.mineralog.domain.model.*

// Define components
val graniteComponents = listOf(
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Quartz",
        mineralGroup = "Silicates",
        percentage = 35f,
        role = ComponentRole.PRINCIPAL,
        formula = "SiO₂",
        mohsMin = 7f,
        density = 2.65f
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Feldspath",
        mineralGroup = "Silicates",
        percentage = 40f,
        role = ComponentRole.PRINCIPAL,
        formula = "KAlSi₃O₈",
        mohsMin = 6f,
        density = 2.56f
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Mica",
        mineralGroup = "Silicates",
        percentage = 20f,
        role = ComponentRole.ACCESSORY,
        mohsMin = 2.5f,
        mohsMax = 3f,
        density = 2.83f
    ),
    MineralComponent(
        id = UUID.randomUUID().toString(),
        mineralName = "Biotite",
        mineralGroup = "Silicates",
        percentage = 5f,
        role = ComponentRole.TRACE,
        mohsMin = 2.5f,
        density = 3.1f
    )
)

// Create aggregate data
val granite = AggregateMineralData(
    name = "Granite de Bretagne",
    components = graniteComponents,
    notes = "Granite à gros grains de Huelgoat"
)

// Insert via repository extension
val aggregateId = mineralRepository.insertAggregate(granite)
```

### 3. Querying by Type

```kotlin
// Get all simple minerals
mineralRepository.getAllSimpleMinerals()
    .collect { minerals ->
        println("Simple minerals: ${minerals.size}")
    }

// Get all aggregates
mineralRepository.getAllAggregates()
    .collect { aggregates ->
        println("Aggregates: ${aggregates.size}")
    }

// Count by type
val simpleCount = mineralRepository.countByType("SIMPLE")
val aggregateCount = mineralRepository.countByType("AGGREGATE")
```

### 4. Working with Components

```kotlin
// Get components for an aggregate
val components = mineralRepository.getAggregateComponents(aggregateId)
components.forEach { component ->
    println("${component.mineralName}: ${component.percentage}% (${component.role})")
}

// Search aggregates by component
mineralRepository.searchAggregatesByComponent("%Quartz%")
    .collect { aggregates ->
        println("Found ${aggregates.size} aggregates containing Quartz")
    }

// Update components
val updatedComponents = listOf(/* new components */)
mineralRepository.updateAggregateComponents(aggregateId, updatedComponents)
```

---

## 📁 Project Structure

### New Files in v2.0

```
app/src/main/java/net/meshcore/mineralog/
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── MineralType.kt              ✨ NEW
│   │   │   ├── ComponentRole.kt            ✨ NEW
│   │   │   ├── SimplePropertiesEntity.kt   ✨ NEW
│   │   │   └── MineralComponentEntity.kt   ✨ NEW
│   │   ├── dao/
│   │   │   ├── SimplePropertiesDao.kt      ✨ NEW
│   │   │   └── MineralComponentDao.kt      ✨ NEW
│   │   └── migration/
│   │       └── Migrations.kt (MIGRATION_4_5) ⚡ UPDATED
│   ├── mapper/
│   │   └── EntityMappers.kt (v2.0 mappers) ⚡ UPDATED
│   └── repository/
│       └── MineralRepositoryV2Extensions.kt ✨ NEW
├── domain/
│   └── model/
│       ├── MineralType.kt                  ✨ NEW
│       ├── ComponentRole.kt                ✨ NEW
│       ├── SimpleProperties.kt             ✨ NEW
│       └── MineralComponent.kt             ✨ NEW
└── ui/
    └── components/
        └── v2/
            └── MineralTypeSelector.kt      ✨ NEW

DOCS/
├── ROADMAP_V2.0.md                         📄 Original roadmap
├── V2_IMPLEMENTATION_STATUS.md             📄 Progress tracking
├── V2_USAGE_EXAMPLES.md                    📄 Code examples
└── V2_README.md                            📄 This file
```

---

## 🔧 Database Changes

### Migration 4→5

The database automatically migrates when the app upgrades to v2.0:

1. **Adds `type` column** to `minerals` table (default = 'SIMPLE')
2. **Creates `simple_properties` table** for simple mineral properties
3. **Creates `mineral_components` table** for aggregate components
4. **Migrates existing data** - all v1.x minerals → `simple_properties`
5. **Adds indices** for optimal query performance

**No user action required** - migration is automatic and safe!

---

## 📊 Implementation Status

### ✅ Completed (60%)

- **Phase 1**: Database foundation (entities, migration)
- **Phase 2**: Data access layer (DAOs)
- **Phase 3**: Domain models + mappers
- **Phase 4**: Repository extensions
- **Phase 5 (partial)**: Basic UI component (MineralTypeSelector)

### 🔄 Remaining (40%)

- **Phase 5**: Full UI for add/edit screens
- **Phase 6**: Detail screens with component display
- **Phase 7**: Filters and search UI
- **Phase 8**: Statistics & CSV export/import
- **Phase 9**: Tests & documentation

See `V2_IMPLEMENTATION_STATUS.md` for detailed progress.

---

## 💡 Best Practices

### Component Percentages

Components should sum to approximately 100%:

```kotlin
// Validate before inserting
if (validateComponentPercentages(components)) {
    mineralRepository.insertAggregate(aggregate)
} else {
    // Show error: percentages don't sum to 100%
}
```

### Component Roles

Use appropriate roles based on percentage:

- **PRINCIPAL**: > 20% (major minerals)
- **ACCESSORY**: 5-20% (secondary minerals)
- **TRACE**: < 5% (minor minerals)

```kotlin
val role = ComponentRole.fromPercentage(percentage)
```

### Minimum Components

Aggregates must have at least 2 components:

```kotlin
require(components.size >= 2) {
    "Aggregate must have at least 2 components"
}
```

If you have a single mineral, use **SIMPLE** type instead.

---

## 🧪 Testing

### Test Simple Mineral Creation

```kotlin
@Test
fun testInsertSimpleMineral() = runTest {
    val props = SimpleProperties(
        group = "Silicates",
        formula = "SiO₂",
        mohsMin = 7f
    )

    val mineral = SimpleMineralData(
        name = "Test Quartz",
        properties = props
    )

    val id = repository.insertSimpleMineral(mineral)
    assertNotNull(id)

    val retrieved = repository.getSimpleProperties(id)
    assertEquals("Silicates", retrieved?.group)
}
```

### Test Aggregate Creation

```kotlin
@Test
fun testInsertAggregate() = runTest {
    val components = listOf(
        MineralComponent(
            id = UUID.randomUUID().toString(),
            mineralName = "Quartz",
            percentage = 50f,
            role = ComponentRole.PRINCIPAL
        ),
        MineralComponent(
            id = UUID.randomUUID().toString(),
            mineralName = "Feldspath",
            percentage = 50f,
            role = ComponentRole.PRINCIPAL
        )
    )

    val aggregate = AggregateMineralData(
        name = "Test Granite",
        components = components
    )

    val id = repository.insertAggregate(aggregate)
    assertNotNull(id)

    val retrieved = repository.getAggregateComponents(id)
    assertEquals(2, retrieved.size)
}
```

---

## 📚 Additional Resources

- **Full Roadmap**: `DOCS/ROADMAP_V2.0.md`
- **Code Examples**: `DOCS/V2_USAGE_EXAMPLES.md`
- **Implementation Status**: `DOCS/V2_IMPLEMENTATION_STATUS.md`

---

## 🤝 Contributing

The v2.0 implementation is in **alpha** stage. The database and repository layers are complete and ready for use.

To contribute to completing the UI:

1. Check `V2_IMPLEMENTATION_STATUS.md` for remaining tasks
2. Follow the patterns in `MineralTypeSelector.kt`
3. Use the repository extension methods from `MineralRepositoryV2Extensions.kt`
4. Add tests for new functionality

---

## 📝 License

Apache 2.0

---

## 👥 Credits

- **Design & Roadmap**: Julien Bombled
- **Implementation**: Claude AI Assistant (Anthropic)
- **Version**: 2.0.0-alpha

---

**Questions? Issues?**
Open an issue on GitHub: https://github.com/VBlackJack/MineraLog/issues
