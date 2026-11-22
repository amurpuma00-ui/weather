package com.liquidglass.weather.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liquidglass.weather.data.*
import com.liquidglass.weather.util.CustomFontManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class WeatherUiState(
    val cities: List<SavedCity> = WeatherRepository.DEFAULT_CITIES,
    val selectedCity: SavedCity = WeatherRepository.DEFAULT_CITIES.first(),
    val weather: WeatherData? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SavedCity> = emptyList(),
    val isSearching: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = WeatherSettingsRepository(application)
    private val weatherRepo = WeatherRepository(application)

    val config: StateFlow<WeatherAppConfig> = settingsRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeatherAppConfig())

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _savedThemes = MutableStateFlow(settingsRepo.loadSavedThemes())
    val savedThemes: StateFlow<List<CustomStyleProfile>> = _savedThemes.asStateFlow()

    init {
        val loadedCities = weatherRepo.loadCities()
        val first = loadedCities.first()
        _uiState.update { it.copy(cities = loadedCities, selectedCity = first) }
        refreshWeather(first)
    }

    fun refreshWeather(city: SavedCity = _uiState.value.selectedCity) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCity = city, isLoading = true) }
            val data = weatherRepo.fetchWeather(city)
            _uiState.update { it.copy(weather = data, isLoading = false) }
        }
    }

    fun updateCitySearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val res = weatherRepo.searchCities(query)
            _uiState.update { it.copy(searchResults = res, isSearching = false) }
        }
    }

    fun addAndSelectCity(city: SavedCity) {
        val current = _uiState.value.cities.toMutableList()
        if (current.none { it.name.equals(city.name, ignoreCase = true) }) {
            current.add(0, city)
            weatherRepo.saveCities(current)
        }
        _uiState.update { it.copy(cities = current, searchQuery = "", searchResults = emptyList()) }
        refreshWeather(city)
    }

    fun removeCity(city: SavedCity) {
        val current = _uiState.value.cities.filterNot { it.id == city.id }
        if (current.isEmpty()) return
        weatherRepo.saveCities(current)
        val nextSelected = if (_uiState.value.selectedCity.id == city.id) current.first() else _uiState.value.selectedCity
        _uiState.update { it.copy(cities = current, selectedCity = nextSelected) }
        if (nextSelected.id != _uiState.value.weather?.city?.id) {
            refreshWeather(nextSelected)
        }
    }

    // --- Customization & DIY Theme Builder ---
    fun updateConfig(transform: (WeatherAppConfig) -> WeatherAppConfig) {
        val next = transform(config.value)
        viewModelScope.launch { settingsRepo.saveConfig(next) }
    }

    fun updateCustomStyle(transform: (CustomStyleProfile) -> CustomStyleProfile) {
        updateConfig { cur ->
            cur.copy(
                themeMode = ThemeMode.CUSTOM,
                customStyle = transform(cur.customStyle)
            )
        }
    }

    fun saveCurrentCustomTheme(name: String) {
        val cleanName = name.trim().ifEmpty { "Стиль ${_savedThemes.value.size + 1}" }
        val profile = config.value.customStyle.copy(
            id = UUID.randomUUID().toString(),
            name = cleanName
        )
        _savedThemes.value = settingsRepo.saveCustomTheme(profile)
    }

    fun applySavedTheme(profile: CustomStyleProfile) {
        updateConfig { it.copy(themeMode = ThemeMode.CUSTOM, customStyle = profile.copy(id = "active_custom")) }
    }

    fun deleteSavedTheme(id: String) {
        _savedThemes.value = settingsRepo.deleteCustomTheme(id)
    }

    fun resetConfig() {
        viewModelScope.launch { settingsRepo.saveConfig(WeatherAppConfig()) }
    }

    fun importCustomFont(uri: Uri): Boolean {
        val imported = CustomFontManager.importFont(getApplication(), uri) ?: return false
        updateConfig { it.copy(customFontPath = imported.path, customFontName = imported.displayName) }
        return true
    }
}
