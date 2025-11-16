package net.meshcore.mineralog.ui.common

/**
 * Sealed class representing the state of UI data loading.
 * Provides a consistent pattern for handling Loading, Success, Error, and Empty states.
 *
 * Usage in ViewModels:
 * ```kotlin
 * private val _uiState = MutableStateFlow<UiState<List<Mineral>>>(UiState.Loading)
 * val uiState: StateFlow<UiState<List<Mineral>>> = _uiState.asStateFlow()
 *
 * fun loadData() {
 *     viewModelScope.launch {
 *         _uiState.value = UiState.Loading
 *         try {
 *             val data = repository.getData()
 *             _uiState.value = if (data.isEmpty()) {
 *                 UiState.Empty
 *             } else {
 *                 UiState.Success(data)
 *             }
 *         } catch (e: Exception) {
 *             _uiState.value = UiState.Error(e.message ?: "Unknown error", canRetry = true)
 *         }
 *     }
 * }
 * ```
 *
 * Usage in Composables:
 * ```kotlin
 * when (val state = viewModel.uiState.collectAsState().value) {
 *     is UiState.Loading -> LoadingIndicator()
 *     is UiState.Success -> ContentView(state.data)
 *     is UiState.Error -> ErrorView(state.message, onRetry = { viewModel.retry() })
 *     is UiState.Empty -> EmptyStateView()
 * }
 * ```
 */
sealed class UiState<out T> {
    /**
     * Loading state - data is being fetched.
     */
    data object Loading : UiState<Nothing>()

    /**
     * Success state - data has been loaded successfully.
     * @param data The loaded data
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Error state - an error occurred while loading data.
     * @param message Human-readable error message
     * @param canRetry Whether the operation can be retried
     * @param cause Optional throwable cause for debugging
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val cause: Throwable? = null
    ) : UiState<Nothing>()

    /**
     * Empty state - data loaded successfully but is empty.
     * Use this for empty collections, search results with no matches, etc.
     */
    data object Empty : UiState<Nothing>()
}

/**
 * Extension function to check if state is loading.
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading

/**
 * Extension function to check if state is successful.
 */
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success

/**
 * Extension function to check if state is error.
 */
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error

/**
 * Extension function to check if state is empty.
 */
fun <T> UiState<T>.isEmpty(): Boolean = this is UiState.Empty

/**
 * Extension function to get data if state is successful, null otherwise.
 */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
