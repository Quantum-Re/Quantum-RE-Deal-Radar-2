package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.MyPropertiesNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PipelineMetrics(
    val totalPropertiesCount: Int = 0,
    val totalAcquisitionCost: Double = 0.0,
    val totalRenovationBudget: Double = 0.0,
    val totalEstimatedExitValue: Double = 0.0,
    val totalProjectedProfit: Double = 0.0,
    val averageRoiPercent: Double = 0.0,
    val activeEscrowDealCount: Int = 0,
    val activeRenovatingCount: Int = 0,
    val soldCompletedCount: Int = 0,
    val listedCount: Int = 0,
    val rentedCount: Int = 0,
    val analyzedCount: Int = 0,
    val archivedCount: Int = 0
)

data class PropertyUiState(
    val properties: List<Property> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedStatusFilter: String = "ALL", // Distress status
    val selectedPipelineFilter: String = "ALL", // PipelineStatus (e.g., "ALL", "ANALYZED", "IN_ESCROW", "RENOVATING", "LISTED", "RENTED", "SOLD", "ARCHIVED")
    val selectedSortOption: PropertySortOption = PropertySortOption.DATE_ADDED_DESC,
    val selectedStrategies: Set<String> = emptySet(),
    val showOnlyDistressed: Boolean = false,
    val selectedProperty: Property? = null,
    val selectedPropertyIds: Set<Long> = emptySet(),
    val isSelectionModeActive: Boolean = false,
    val metrics: PipelineMetrics = PipelineMetrics(),
    val alertHistory: List<PropertyAlertRecord> = emptyList(),
    val alertPreferences: MyPropertiesAlertPreferences = MyPropertiesAlertPreferences(),
    val unreadAlertCount: Int = 0,
    val isAlertsSheetOpen: Boolean = false,
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

    private val _selectedPipelineFilter = MutableStateFlow("ALL")
    val selectedPipelineFilter: StateFlow<String> = _selectedPipelineFilter.asStateFlow()

    private val _selectedSortOption = MutableStateFlow(PropertySortOption.DATE_ADDED_DESC)
    val selectedSortOption: StateFlow<PropertySortOption> = _selectedSortOption.asStateFlow()

    private val _selectedStrategies = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrategies: StateFlow<Set<String>> = _selectedStrategies.asStateFlow()

    private val _showOnlyDistressed = MutableStateFlow(false)
    val showOnlyDistressed: StateFlow<Boolean> = _showOnlyDistressed.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _selectedPropertyIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPropertyIds: StateFlow<Set<Long>> = _selectedPropertyIds.asStateFlow()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<PropertyAlertRecord>>(
        MyPropertiesNotificationManager.loadAlertHistory(application)
    )
    val alertHistory: StateFlow<List<PropertyAlertRecord>> = _alertHistory.asStateFlow()

    private val _alertPreferences = MutableStateFlow(
        MyPropertiesNotificationManager.loadPreferences(application)
    )
    val alertPreferences: StateFlow<MyPropertiesAlertPreferences> = _alertPreferences.asStateFlow()

    private val _isAlertsSheetOpen = MutableStateFlow(false)
    val isAlertsSheetOpen: StateFlow<Boolean> = _isAlertsSheetOpen.asStateFlow()

    // Primary StateFlow exposing all properties from centralized repository
    val properties: StateFlow<List<Property>> = repository.allProperties

    init {
        android.util.Log.d("PropertyViewModel", "PropertyViewModel init started, checking Room database...")
        MyPropertiesNotificationManager.ensureNotificationChannel(application)
        viewModelScope.launch {
            if (repository.allProperties.first().isEmpty()) {
                android.util.Log.d("PropertyViewModel", "Database empty, seeding initial properties via repository...")
                repository.insertProperties(com.example.data.InitialSeedData.initialProperties)
            }
        }
    }

    // Filtered and sorted property list based on search, status filter, pipeline filter, sort option, multi-select strategy filter, and distressed toggle
    val filteredProperties: StateFlow<List<Property>> = combine(
        repository.allProperties,
        _searchQuery,
        _selectedStatusFilter,
        _selectedPipelineFilter,
        _selectedSortOption,
        _selectedStrategies,
        _showOnlyDistressed
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val propertyList = flows[0] as List<Property>
        val query = flows[1] as String
        val status = flows[2] as String
        val pipeline = flows[3] as String
        val sortOption = flows[4] as PropertySortOption
        @Suppress("UNCHECKED_CAST")
        val strategies = flows[5] as Set<String>
        val onlyDistressed = flows[6] as Boolean

        android.util.Log.d("PropertyViewModel", "filteredProperties combine triggered with ${propertyList.size} items from DB (Pipeline='$pipeline', Sort='$sortOption', Search='$query')")
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
                    prop.pipelineStatus,
                    prop.notes,
                    prop.contractorNotes,
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

            val matchesPipeline = pipeline == "ALL" ||
                    prop.pipelineStatus.equals(pipeline, ignoreCase = true)

            val matchesStrategy = strategies.isEmpty() || strategies.any { strat ->
                prop.strategyTags.contains(strat, ignoreCase = true)
            }

            val isDistressed = !prop.distressStatus.equals("Nessuno", ignoreCase = true) &&
                    !prop.distressStatus.equals("None", ignoreCase = true) &&
                    !prop.distressStatus.equals("Normale", ignoreCase = true) &&
                    prop.distressStatus.isNotBlank()

            val matchesDistressToggle = !onlyDistressed || isDistressed

            matchesSearch && matchesStatus && matchesPipeline && matchesStrategy && matchesDistressToggle
        }
        val sortedResult = result.sortProperties(sortOption)
        android.util.Log.d("PropertyViewModel", "filteredProperties produced ${sortedResult.size} filtered & sorted items")
        sortedResult
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Pipeline Analytics Metrics calculated reactively from Room properties
    val pipelineMetrics: StateFlow<PipelineMetrics> = repository.allProperties.map { list ->
        if (list.isEmpty()) {
            PipelineMetrics()
        } else {
            val totalAcquisition = list.sumOf { it.price }
            val totalRenovation = list.sumOf { if (it.actualRenovationCost > 0) it.actualRenovationCost else it.estimatedRenovationCost }
            val totalExit = list.sumOf { it.effectiveExitValue }
            val totalProfit = list.sumOf { it.projectedProfit }
            val validRoiItems = list.filter { it.totalCostBasis > 0 }
            val avgRoi = if (validRoiItems.isNotEmpty()) {
                validRoiItems.map { it.projectedRoiPercent }.average()
            } else 0.0

            PipelineMetrics(
                totalPropertiesCount = list.size,
                totalAcquisitionCost = totalAcquisition,
                totalRenovationBudget = totalRenovation,
                totalEstimatedExitValue = totalExit,
                totalProjectedProfit = totalProfit,
                averageRoiPercent = avgRoi,
                activeEscrowDealCount = list.count { it.pipelineStatus == PipelineStatus.IN_ESCROW.key },
                activeRenovatingCount = list.count { it.pipelineStatus == PipelineStatus.RENOVATING.key },
                soldCompletedCount = list.count { it.pipelineStatus == PipelineStatus.SOLD.key },
                listedCount = list.count { it.pipelineStatus == PipelineStatus.LISTED.key },
                rentedCount = list.count { it.pipelineStatus == PipelineStatus.RENTED.key },
                analyzedCount = list.count { it.pipelineStatus == PipelineStatus.ANALYZED.key },
                archivedCount = list.count { it.pipelineStatus == PipelineStatus.ARCHIVED.key }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PipelineMetrics()
    )

    // Combined UI state for easy Compose observation
    val uiState: StateFlow<PropertyUiState> = combine(
        filteredProperties,
        _searchQuery,
        _selectedStatusFilter,
        _selectedPipelineFilter,
        _selectedSortOption,
        _selectedStrategies,
        _showOnlyDistressed,
        _selectedProperty,
        _selectedPropertyIds,
        _isSelectionModeActive,
        pipelineMetrics,
        _alertHistory,
        _alertPreferences,
        _isAlertsSheetOpen
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val alertList = flows[11] as List<PropertyAlertRecord>
        val unread = alertList.count { !it.isRead }

        @Suppress("UNCHECKED_CAST")
        PropertyUiState(
            properties = flows[0] as List<Property>,
            searchQuery = flows[1] as String,
            selectedStatusFilter = flows[2] as String,
            selectedPipelineFilter = flows[3] as String,
            selectedSortOption = flows[4] as PropertySortOption,
            selectedStrategies = flows[5] as Set<String>,
            showOnlyDistressed = flows[6] as Boolean,
            selectedProperty = flows[7] as Property?,
            selectedPropertyIds = flows[8] as Set<Long>,
            isSelectionModeActive = flows[9] as Boolean,
            metrics = flows[10] as PipelineMetrics,
            alertHistory = alertList,
            alertPreferences = flows[12] as MyPropertiesAlertPreferences,
            unreadAlertCount = unread,
            isAlertsSheetOpen = flows[13] as Boolean
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

    fun updatePipelineFilter(pipelineStatus: String) {
        _selectedPipelineFilter.value = pipelineStatus
    }

    fun updateSortOption(sortOption: PropertySortOption) {
        _selectedSortOption.value = sortOption
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

    fun setAlertsSheetOpen(open: Boolean) {
        _isAlertsSheetOpen.value = open
    }

    fun updateAlertPreferences(prefs: MyPropertiesAlertPreferences) {
        _alertPreferences.value = prefs
        MyPropertiesNotificationManager.savePreferences(getApplication(), prefs)
    }

    fun markAlertAsRead(alertId: String) {
        val updated = MyPropertiesNotificationManager.markAlertAsRead(getApplication(), alertId)
        _alertHistory.value = updated
    }

    fun clearAllAlerts() {
        val updated = MyPropertiesNotificationManager.clearAlertHistory(getApplication())
        _alertHistory.value = updated
    }

    fun sendTestPriceDropAlert(propertyId: Long? = null) {
        val target = if (propertyId != null) {
            properties.value.find { it.id == propertyId }
        } else {
            properties.value.firstOrNull()
        }
        val record = MyPropertiesNotificationManager.sendTestPriceDropNotification(getApplication(), target)
        _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
    }

    fun sendTestStatusChangeAlert(propertyId: Long? = null) {
        val target = if (propertyId != null) {
            properties.value.find { it.id == propertyId }
        } else {
            properties.value.firstOrNull()
        }
        val record = MyPropertiesNotificationManager.sendTestStatusChangeNotification(getApplication(), target)
        _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
    }

    /**
     * Simulates an automatic market price drop on a saved property (e.g. -5%, -10%, -15%).
     * Updates Room database, triggers system push notification and records to Alert History.
     */
    fun simulatePriceDropForProperty(propertyId: Long, dropPercent: Double = 8.0) {
        viewModelScope.launch {
            val oldProp = repository.getPropertyById(propertyId) ?: return@launch
            val dropAmount = oldProp.price * (dropPercent / 100.0)
            val newPrice = (oldProp.price - dropAmount).coerceAtLeast(10000.0)

            val updatedProp = oldProp.copy(price = newPrice)
            MyPropertiesNotificationManager.checkAndNotifyPropertyChange(
                context = getApplication(),
                oldProperty = oldProp,
                newProperty = updatedProp,
                prefs = _alertPreferences.value
            )
            repository.updateProperty(updatedProp)
            _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
        }
    }

    fun updatePropertyPipelineStatus(propertyId: Long, newStatus: PipelineStatus) {
        viewModelScope.launch {
            val oldProp = repository.getPropertyById(propertyId)
            if (oldProp != null) {
                val updatedProp = oldProp.copy(pipelineStatus = newStatus.key)
                MyPropertiesNotificationManager.checkAndNotifyPropertyChange(
                    context = getApplication(),
                    oldProperty = oldProp,
                    newProperty = updatedProp,
                    prefs = _alertPreferences.value
                )
            }
            repository.updatePipelineStatus(propertyId, newStatus.key)
            _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
        }
    }

    fun updateRenovationProgress(propertyId: Long, progressPercent: Int, actualCost: Double) {
        viewModelScope.launch {
            val oldProp = repository.getPropertyById(propertyId)
            if (oldProp != null) {
                val updatedProp = oldProp.copy(
                    renovationProgressPercent = progressPercent,
                    actualRenovationCost = actualCost
                )
                MyPropertiesNotificationManager.checkAndNotifyPropertyChange(
                    context = getApplication(),
                    oldProperty = oldProp,
                    newProperty = updatedProp,
                    prefs = _alertPreferences.value
                )
            }
            repository.updateRenovationProgress(propertyId, progressPercent, actualCost)
            _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
        }
    }

    fun addProperty(property: Property) {
        viewModelScope.launch {
            repository.insertProperty(property)
        }
    }

    fun updateProperty(property: Property) {
        viewModelScope.launch {
            val oldProp = repository.getPropertyById(property.id)
            if (oldProp != null) {
                MyPropertiesNotificationManager.checkAndNotifyPropertyChange(
                    context = getApplication(),
                    oldProperty = oldProp,
                    newProperty = property,
                    prefs = _alertPreferences.value
                )
            }
            repository.updateProperty(property)
            _alertHistory.value = MyPropertiesNotificationManager.loadAlertHistory(getApplication())
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            repository.deleteProperty(property)
        }
    }

    // -------------------------------------------------------------
    // Multi-Selection Capabilities & Batch Operations
    // -------------------------------------------------------------
    fun setSelectionMode(active: Boolean) {
        _isSelectionModeActive.value = active
        if (!active) {
            _selectedPropertyIds.value = emptySet()
        }
    }

    fun togglePropertySelection(propertyId: Long) {
        val current = _selectedPropertyIds.value
        if (current.contains(propertyId)) {
            val updated = current - propertyId
            _selectedPropertyIds.value = updated
        } else {
            _selectedPropertyIds.value = current + propertyId
            _isSelectionModeActive.value = true
        }
    }

    fun selectAllProperties(targetProperties: List<Property>? = null) {
        val listToSelect = targetProperties ?: filteredProperties.value
        _selectedPropertyIds.value = listToSelect.map { it.id }.toSet()
        _isSelectionModeActive.value = true
    }

    fun clearSelection() {
        _selectedPropertyIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    /**
     * Batch updates pipeline status for all selected properties in Room.
     */
    fun batchUpdatePipelineStatus(newStatus: PipelineStatus, onComplete: ((Int) -> Unit)? = null) {
        val ids = _selectedPropertyIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val count = repository.updatePipelineStatusForMultiple(ids, newStatus.key)
            clearSelection()
            onComplete?.invoke(count)
        }
    }

    /**
     * Batch archives all selected properties.
     */
    fun batchArchiveSelectedProperties(onComplete: ((Int) -> Unit)? = null) {
        val ids = _selectedPropertyIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val count = repository.archiveProperties(ids)
            clearSelection()
            onComplete?.invoke(count)
        }
    }

    /**
     * Batch deletes all selected properties permanently from Room.
     */
    fun batchDeleteSelectedProperties(onComplete: ((Int) -> Unit)? = null) {
        val ids = _selectedPropertyIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val count = repository.deletePropertiesByIds(ids)
            clearSelection()
            onComplete?.invoke(count)
        }
    }

    fun clearAllProperties() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}


