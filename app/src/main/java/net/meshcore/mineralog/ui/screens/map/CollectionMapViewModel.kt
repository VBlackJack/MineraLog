package net.meshcore.mineralog.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository

/**
 * Data class representing a mineral marker on the map.
 * Contains only the minimal data needed for map display and navigation.
 *
 * @property id Unique identifier of the mineral
 * @property name Display name of the mineral
 * @property latitude Geographic latitude (-90.0 to 90.0)
 * @property longitude Geographic longitude (-180.0 to 180.0)
 * @property mainPhotoUri URI of the main photo, or null if no photo available
 */
data class MapMineralItem(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val mainPhotoUri: String?
)

/**
 * ViewModel for the Collection Map screen.
 * Loads all geolocated minerals and exposes them as a StateFlow.
 *
 * Filtering logic:
 * - Only includes minerals that have a Provenance
 * - Provenance must have non-null latitude and longitude
 * - Excludes coordinates at (0.0, 0.0) as they are likely default/invalid values
 */
class CollectionMapViewModel(
    private val mineralRepository: MineralRepository
) : ViewModel() {

    private val _geolocatedMinerals = MutableStateFlow<List<MapMineralItem>>(emptyList())
    val geolocatedMinerals: StateFlow<List<MapMineralItem>> = _geolocatedMinerals.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadGeolocatedMinerals()
    }

    /**
     * Loads all minerals from the repository and filters for those with valid geolocation data.
     * Updates the geolocatedMinerals StateFlow with the filtered list.
     */
    private fun loadGeolocatedMinerals() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Load all minerals with their provenance data
                val allMinerals = mineralRepository.getAll()

                // Filter minerals that have valid geolocation
                val geolocatedItems = allMinerals
                    .filter { mineral ->
                        val provenance = mineral.provenance
                        provenance != null &&
                            provenance.latitude != null &&
                            provenance.longitude != null &&
                            // Exclude (0.0, 0.0) as it's often a default/invalid value
                            !(provenance.latitude == 0.0 && provenance.longitude == 0.0)
                    }
                    .map { mineral ->
                        MapMineralItem(
                            id = mineral.id,
                            name = mineral.name,
                            latitude = mineral.provenance!!.latitude!!,
                            longitude = mineral.provenance.longitude!!,
                            mainPhotoUri = mineral.photos.firstOrNull()?.fileName
                        )
                    }

                _geolocatedMinerals.value = geolocatedItems
            } catch (e: Exception) {
                // On error, emit empty list
                _geolocatedMinerals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refreshes the geolocated minerals list.
     * Useful when called after adding/updating minerals with location data.
     */
    fun refresh() {
        loadGeolocatedMinerals()
    }
}
