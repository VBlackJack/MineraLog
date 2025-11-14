package net.meshcore.mineralog.ui.constants

/**
 * UI-related constants for the application.
 * Extracted from magic numbers across the codebase for better maintainability.
 */
object UiConstants {

    // Debounce and delays (milliseconds)
    const val SEARCH_DEBOUNCE_MS = 500L
    const val STATE_FLOW_TIMEOUT_MS = 5000L
    const val HAPTIC_FEEDBACK_DELAY_MS = 100L
    const val SNACKBAR_DURATION_MS = 3000L

    // Image dimensions
    const val THUMBNAIL_SIZE_PX = 400
    const val PREVIEW_IMAGE_SIZE_PX = 800
    const val FULL_IMAGE_MAX_SIZE_PX = 2000

    // List and pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val INITIAL_LOAD_SIZE = 60
    const val PREFETCH_DISTANCE = 10

    // Animation durations (milliseconds)
    const val SHORT_ANIMATION_MS = 150
    const val MEDIUM_ANIMATION_MS = 300
    const val LONG_ANIMATION_MS = 500

    // QR Code generation
    const val QR_CODE_SIZE_PX = 512
    const val QR_CODE_MARGIN = 1

    // Charts
    const val PIE_CHART_ANGLE_OFFSET = -90f
    const val BAR_CHART_MAX_BARS = 15

    // Photo grid
    const val PHOTO_GRID_COLUMNS = 3
    const val PHOTO_GRID_SPACING_DP = 4

    // Text limits
    const val MAX_NOTES_LENGTH = 10000
    const val MAX_TAG_LENGTH = 50
    const val MAX_TAGS_COUNT = 20

    // Memory thresholds
    const val LOW_MEMORY_THRESHOLD_MB = 50
    const val PHOTO_CACHE_SIZE_MB = 100
}
