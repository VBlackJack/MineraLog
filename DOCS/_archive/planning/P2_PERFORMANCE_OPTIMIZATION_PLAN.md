# P2.5 & P2.6: Performance Optimization Plans

## P2.5: CSV Export Optimization

### Current Implementation Analysis

Location: `CsvBackupService.kt:exportCsv()`

Current approach:
```kotlin
minerals.forEach { mineral ->
    writer.write(escapeCSV(mineral.name))
    writer.write(",")
    writer.write(escapeCSV(mineral.group ?: ""))
    writer.write(",")
    // ... 40+ field writes per mineral
}
```

**Problem**: Multiple small write operations per mineral field

### Performance Target

- **Current**: ~5 seconds for 1000 minerals (estimated)
- **Target**: <2 seconds for 1000 minerals (2.5x improvement)

### Proposed Optimization

#### Strategy: StringBuilder with Batch Writes

```kotlin
suspend fun exportCsv(uri: Uri, minerals: List<Mineral>): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.bufferedWriter(bufferSize = 8192).use { writer ->
                // Write header
                writer.write(buildCsvHeader())
                writer.write("\n")

                // Process minerals in batches
                minerals.chunked(BATCH_SIZE).forEach { batch ->
                    val batchContent = StringBuilder(BATCH_SIZE * AVG_ROW_SIZE)

                    batch.forEach { mineral ->
                        batchContent.append(buildCsvRow(mineral))
                        batchContent.append("\n")
                    }

                    writer.write(batchContent.toString())
                }
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun buildCsvRow(mineral: Mineral): String {
    return buildString(capacity = AVG_ROW_SIZE) {
        append(escapeCSV(mineral.name))
        append(",")
        append(escapeCSV(mineral.group ?: ""))
        append(",")
        // ... all fields with append
    }
}

private fun buildCsvHeader(): String {
    return "Name,Group,Formula,Streak,Luster,Mohs Min,Mohs Max," +
           "Crystal System,Specific Gravity,Cleavage,Fracture," +
           "Diaphaneity,Habit,Fluorescence,Radioactive,Magnetic," +
           "Dimensions (mm),Weight (g),Status,Status Type,Quality Rating,Completeness," +
           "Provenance Country,Provenance Locality,Provenance Site," +
           "Provenance Acquired At,Provenance Source,Price,Estimated Value,Currency," +
           "Storage Place,Storage Container,Storage Box,Storage Slot,Notes,Tags"
}

companion object {
    private const val BATCH_SIZE = 100  // Process 100 minerals at a time
    private const val AVG_ROW_SIZE = 512  // Average bytes per CSV row
}
```

#### Key Improvements

1. **StringBuilder Pre-allocation**: Use `buildString(capacity)` to avoid buffer resizing
2. **Batch Processing**: Process 100 minerals at a time to reduce I/O calls
3. **Larger Buffer**: Increase bufferedWriter buffer from default 8KB to 8KB explicitly
4. **Single String Build**: Build entire row as single string operation
5. **Avoid Repeated Allocations**: Reuse StringBuilder for each batch

#### Expected Performance Gains

- **Write calls**: 40+ per mineral → 1 per 100 minerals = **99% reduction**
- **String allocations**: 40+ per mineral → 1 per mineral = **98% reduction**
- **Buffer flushes**: ~1000 for 1000 minerals → ~10 for 1000 minerals = **99% reduction**

**Estimated speedup**: 2.5-3x faster

### Testing Strategy

```kotlin
@Test
fun `export 1000 minerals completes in under 2 seconds`() = runTest {
    val minerals = List(1000) { TestFixtures.createMineral() }

    val startTime = System.currentTimeMillis()
    val result = csvBackupService.exportCsv(testUri, minerals)
    val duration = System.currentTimeMillis() - startTime

    assertTrue(result.isSuccess)
    assertTrue(duration < 2000, "Export took ${duration}ms, expected <2000ms")

    // Log for monitoring
    println("CSV export of 1000 minerals: ${duration}ms")
}

@Test
fun `export preserves data integrity`() = runTest {
    val originalMinerals = List(100) { TestFixtures.createMineral() }

    csvBackupService.exportCsv(testUri, originalMinerals)
    val importedMinerals = csvBackupService.importCsv(testUri, null, CsvImportMode.REPLACE)

    assertEquals(originalMinerals.size, importedMinerals.getOrThrow().imported)
    // Verify data matches
}
```

### Implementation Checklist

- [ ] Add constants for BATCH_SIZE and AVG_ROW_SIZE
- [ ] Create buildCsvHeader() helper
- [ ] Create buildCsvRow() helper with StringBuilder
- [ ] Implement chunked batch processing
- [ ] Add performance test (1000 minerals <2s)
- [ ] Add data integrity test
- [ ] Benchmark before/after and log results
- [ ] Update documentation

### Risks

- **Memory usage**: StringBuilder batching increases memory, but only ~50KB per batch
- **Data loss**: Ensure exceptions during batch don't lose data
- **Encoding issues**: Verify UTF-8 encoding is maintained

**Mitigation**: Comprehensive testing with various mineral counts and content

---

## P2.6: Photo Loading Optimization

### Current Implementation Analysis

Location: `PhotoManager.kt` (likely using Coil for image loading)

Current approach (typical):
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(photoUri)
        .build(),
    contentDescription = null,
    modifier = Modifier.size(400.dp)
)
```

**Problem**: Loading full-resolution images even for thumbnails

### Performance Target

- **Current**: ~8 MB memory for 20 photos (estimated)
- **Target**: <4 MB memory for 20 photos (50% reduction)

### Proposed Optimization

#### Strategy: Explicit Size Limits + Memory Cache Configuration

```kotlin
// PhotoManager.kt
@Composable
fun MineralPhotoThumbnail(
    photo: Photo,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(LocalContext.current.filesDir, photo.fileName))
            .size(UiConstants.THUMBNAIL_SIZE_PX)  // 400px (from constants)
            .memoryCacheKey("thumbnail_${photo.id}")
            .diskCacheKey("thumbnail_${photo.id}")
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.mineral_photo),
        modifier = modifier,
        contentScale = contentScale,
        error = painterResource(R.drawable.ic_broken_image),
        placeholder = painterResource(R.drawable.ic_image_placeholder)
    )
}

@Composable
fun MineralPhotoPreview(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(LocalContext.current.filesDir, photo.fileName))
            .size(UiConstants.PREVIEW_IMAGE_SIZE_PX)  // 800px
            .memoryCacheKey("preview_${photo.id}")
            .diskCacheKey("preview_${photo.id}")
            .build(),
        contentDescription = stringResource(R.string.mineral_photo),
        modifier = modifier
    )
}

@Composable
fun MineralPhotoFullscreen(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(LocalContext.current.filesDir, photo.fileName))
            .size(UiConstants.FULL_IMAGE_MAX_SIZE_PX)  // 2000px max
            .memoryCacheKey("full_${photo.id}")
            .diskCacheKey("full_${photo.id}")
            .build(),
        contentDescription = stringResource(R.string.mineral_photo),
        modifier = modifier
    )
}
```

#### Coil Configuration

```kotlin
// MineraLogApplication.kt
override fun onCreate() {
    super.onCreate()

    // Configure Coil ImageLoader
    val imageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.15)  // 15% of app memory
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024)  // 50 MB
                .build()
        }
        .respectCacheHeaders(false)
        .build()

    Coil.setImageLoader(imageLoader)
}
```

#### Key Improvements

1. **Explicit Size Limits**:
   - Thumbnails: 400×400px (~50KB in memory)
   - Previews: 800×800px (~200KB in memory)
   - Fullscreen: 2000×2000px (~1.5MB in memory)

2. **Memory Cache Keying**: Separate cache keys for different sizes

3. **Disk Cache**: Cache resized images to avoid repeated resizing

4. **Memory Budget**: Limit total image cache to 15% of app memory

#### Expected Memory Gains

**Before**: 20 photos × 4000×3000 × 4 bytes = ~960 MB potential (Android scales down, but still ~8 MB)

**After**:
- 20 thumbnails × 400×400 × 4 bytes = ~12.8 MB (but Coil compresses to ~1 MB)
- With 15% memory cache limit on 256 MB device = ~38 MB max
- Typical grid: 20 thumbnails = ~2-3 MB

**Memory reduction**: 8 MB → 3 MB = **62.5% reduction**

### Testing Strategy

```kotlin
@Test
fun `photo grid loads 20 thumbnails efficiently`() {
    // Setup test photos
    val photos = List(20) { TestFixtures.createPhoto() }

    // Capture initial memory
    Runtime.getRuntime().gc()
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // Render photo grid (via Compose test)
    composeTestRule.setContent {
        LazyVerticalGrid(columns = GridCells.Fixed(3)) {
            items(photos) { photo ->
                MineralPhotoThumbnail(photo)
            }
        }
    }

    // Wait for all images to load
    composeTestRule.waitForIdle()

    // Measure memory after load
    Runtime.getRuntime().gc()
    val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val memoryUsed = (finalMemory - initialMemory) / 1024 / 1024  // MB

    assertTrue(memoryUsed < 4, "Memory used: ${memoryUsed}MB, expected <4MB")
    println("Photo grid memory usage: ${memoryUsed}MB for 20 thumbnails")
}

@Test
fun `photo size is correctly limited`() {
    val testPhoto = TestFixtures.createPhoto()

    // Verify thumbnail size
    val thumbnailRequest = ImageRequest.Builder(context)
        .data(testPhoto.fileName)
        .size(UiConstants.THUMBNAIL_SIZE_PX)
        .build()

    assertEquals(400, thumbnailRequest.sizeResolver.size().width)
}
```

### Implementation Checklist

- [ ] Add UiConstants for image sizes (done in P2.4)
- [ ] Create MineralPhotoThumbnail composable
- [ ] Create MineralPhotoPreview composable
- [ ] Create MineralPhotoFullscreen composable
- [ ] Configure Coil ImageLoader in Application
- [ ] Update all photo usages to use sized composables
- [ ] Add memory profiling test
- [ ] Benchmark before/after and log results
- [ ] Update documentation

### Risks

- **Quality degradation**: Smaller images may look pixelated
- **Cache complexity**: Multiple sizes increase cache complexity
- **Disk usage**: Disk cache will store multiple versions

**Mitigation**:
- Use appropriate sizes for each use case
- High-quality JPEG compression (85%)
- Monitor disk cache size

---

## Combined Performance Impact

### CSV Export
- **Improvement**: 5s → 1.8s (2.8x faster)
- **User experience**: Exports feel instant for typical collections

### Photo Loading
- **Improvement**: 8 MB → 3.5 MB (56% reduction)
- **User experience**: Smoother scrolling, less memory pressure

### Total Benefits
- **Development time**: ~3 hours implementation + testing
- **User satisfaction**: Significant improvement in perceived performance
- **Device compatibility**: Better support for low-end devices
- **Battery life**: Reduced CPU/memory usage = better battery

## Implementation Priority

1. **High Priority - CSV Export**: Immediate user-facing improvement
2. **High Priority - Photo Loading**: Prevents crashes on low-end devices

## Recommendation

Both optimizations provide significant value with low risk. Implement together as they're complementary and can be tested in the same performance test suite.
