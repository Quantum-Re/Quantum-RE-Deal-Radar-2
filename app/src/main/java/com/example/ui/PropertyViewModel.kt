package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Property
import com.example.data.PropertyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PropertyUiState(
    val properties: List<Property> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedStatusFilter: String = "ALL",
    val selectedStrategies: Set<String> = emptySet(),
    val showOnlyDistressed: Boolean = false,
    val selectedProperty: Property? = null,
    val errorMessage: String? = null
)

class PropertyViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PropertyRepository = PropertyRepository(
        AppDatabase.getDatabase(application).propertyDao()
    )
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedStrategies = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrategies: StateFlow<Set<String>> = _selectedStrategies.asStateFlow()

    private val _showOnlyDistressed = MutableStateFlow(false)
    val showOnlyDistressed: StateFlow<Boolean> = _showOnlyDistressed.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    // Primary StateFlow exposing all properties from Room database
    val properties: StateFlow<List<Property>> = repository.allProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        android.util.Log.d("PropertyViewModel", "PropertyViewModel init started, checking Room database...")
        viewModelScope.launch {
            if (repository.allProperties.first().isEmpty()) {
                android.util.Log.d("PropertyViewModel", "Database empty, seeding initial properties via repository...")
                repository.insertProperties(com.example.data.InitialSeedData.initialProperties)
            }
        }
    }

    // Filtered property list based on search, status filter, multi-select strategy filter, and distressed toggle
    val filteredProperties: StateFlow<List<Property>> = combine(
        repository.allProperties,
        _searchQuery,
        _selectedStatusFilter,
        _selectedStrategies,
        _showOnlyDistressed
    ) { propertyList, query, status, strategies, onlyDistressed ->
        android.util.Log.d("PropertyViewModel", "filteredProperties combine triggered with ${propertyList.size} items from DB (Status='$status', Search='$query')")
        val searchTokens = query.trim().let { q ->
            java.text.Normalizer.normalize(q, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        val result = propertyList.filter { prop ->
            val matchesSearch = if (searchTokens.isEmpty()) {
                true
            } else {
                val rawText = listOf(
                    prop.title,
                    prop.address,
                    prop.propertyType,
                    prop.distressStatus,
                    prop.strategyTags,
                    prop.notes,
                    "€${prop.price.toInt()}",
                    "${prop.price.toInt()}",
                    "${prop.surfaceSqm} mq",
                    "${prop.surfaceSqm}"
                ).joinToString(" ")

                val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }
            
            val matchesStatus = status == "ALL" ||
                    prop.distressStatus.equals(status, ignoreCase = true)

            val matchesStrategy = strategies.isEmpty() || strategies.any { strat ->
                prop.strategyTags.contains(strat, ignoreCase = true)
            }
            
            val isDistressed = !prop.distressStatus.equals("Nessuno", ignoreCase = true) &&
                    !prop.distressStatus.equals("None", ignoreCase = true) &&
                    !prop.distressStatus.equals("Normale", ignoreCase = true) &&
                    prop.distressStatus.isNotBlank()
            
            val matchesDistressToggle = !onlyDistressed || isDistressed

            matchesSearch && matchesStatus && matchesStrategy && matchesDistressToggle
        }
        android.util.Log.d("PropertyViewModel", "filteredProperties produced ${result.size} filtered items")
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined UI state for easy Compose observation
    val uiState: StateFlow<PropertyUiState> = combine(
        filteredProperties,
        _searchQuery,
        _selectedStatusFilter,
        _selectedStrategies,
        _showOnlyDistressed,
        _selectedProperty
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        PropertyUiState(
            properties = flows[0] as List<Property>,
            searchQuery = flows[1] as String,
            selectedStatusFilter = flows[2] as String,
            selectedStrategies = flows[3] as Set<String>,
            showOnlyDistressed = flows[4] as Boolean,
            selectedProperty = flows[5] as Property?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PropertyUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun toggleStrategyFilter(strategy: String) {
        if (_selectedStrategies.value.contains(strategy)) {
            _selectedStrategies.value = _selectedStrategies.value - strategy
        } else {
            _selectedStrategies.value = _selectedStrategies.value + strategy
        }
    }

    fun clearStrategyFilters() {
        _selectedStrategies.value = emptySet()
    }

    fun setShowOnlyDistressed(onlyDistressed: Boolean) {
        _showOnlyDistressed.value = onlyDistressed
    }

    fun toggleShowOnlyDistressed() {
        _showOnlyDistressed.value = !_showOnlyDistressed.value
    }

    fun selectProperty(property: Property?) {
        _selectedProperty.value = property
    }

    fun addProperty(property: Property) {
        viewModelScope.launch {
            repository.insertProperty(property)
        }
    }

    fun updateProperty(property: Property) {
        viewModelScope.launch {
            repository.updateProperty(property)
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            repository.deleteProperty(property)
        }
    }

    fun clearAllProperties() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
