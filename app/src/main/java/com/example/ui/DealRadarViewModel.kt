package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    RADAR_FEED,
    PARSER_SANDBOX,
    ROI_CALCULATOR,
    ANALYTICS
}

enum class FilterCategory {
    ALL,
    HIGH_DISCOUNT,
    AUCTION_NPL,
    BOOKMARKED
}

data class UiState(
    val searchQuery: String = "",
    val selectedMainTab: MainTab = MainTab.RADAR_FEED,
    val selectedFilterCategory: FilterCategory = FilterCategory.ALL,
    val selectedSourceFilter: String = "ALL", // "ALL" or sourceId
    val selectedPropertyTypeFilter: String = "ALL", // "ALL", "Residenziale", "Commerciale", etc.
    val selectedDealForDetail: PropertyDeal? = null,
    val selectedSourceForParser: ScraperSource? = null,
    val isAddDealDialogOpen: Boolean = false,
    val isParserEditorOpen: Boolean = false,
    val isRunningParserTest: Boolean = false,
    val parserTestLogs: List<String> = emptyList(),
    val parserTestResult: ScraperTestResult? = null
)

// ROI Calculator State
data class RoiCalculatorState(
    val purchasePriceStr: String = "185000",
    val renovationCostStr: String = "25000",
    val legalAuctionFeesStr: String = "8500",
    val monthlyRentStr: String = "1200",
    val expectedResaleStr: String = "260000",
    val LTVPercentStr: String = "70",
    val mortgageRateStr: String = "3.2"
) {
    val purchasePrice: Double get() = purchasePriceStr.toDoubleOrNull() ?: 0.0
    val renovationCost: Double get() = renovationCostStr.toDoubleOrNull() ?: 0.0
    val legalFees: Double get() = legalAuctionFeesStr.toDoubleOrNull() ?: 0.0
    val monthlyRent: Double get() = monthlyRentStr.toDoubleOrNull() ?: 0.0
    val expectedResale: Double get() = expectedResaleStr.toDoubleOrNull() ?: 0.0

    val totalInvestment: Double get() = purchasePrice + renovationCost + legalFees

    val grossAnnualYieldPercent: Double
        get() = if (totalInvestment > 0) ((monthlyRent * 12.0) / totalInvestment) * 100.0 else 0.0

    val netCapRatePercent: Double
        get() = if (totalInvestment > 0) (((monthlyRent * 12.0 * 0.82) - 1200) / totalInvestment) * 100.0 else 0.0

    val totalFlipProfit: Double
        get() = expectedResale - totalInvestment

    val flipROIPercent: Double
        get() = if (totalInvestment > 0) (totalFlipProfit / totalInvestment) * 100.0 else 0.0
}

class DealRadarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DealRadarRepository(
        database.propertyDealDao(),
        database.scraperSourceDao(),
        database.priceHistoryDao()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _roiCalculatorState = MutableStateFlow(RoiCalculatorState())
    val roiCalculatorState: StateFlow<RoiCalculatorState> = _roiCalculatorState.asStateFlow()

    val allDeals: StateFlow<List<PropertyDeal>> = repository.allDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<ScraperSource>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered deals flow based on search and selected chips
    val filteredDeals: StateFlow<List<PropertyDeal>> = combine(
        allDeals,
        uiState
    ) { deals, state ->
        deals.filter { deal ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    deal.title.contains(state.searchQuery, ignoreCase = true) ||
                    deal.location.contains(state.searchQuery, ignoreCase = true) ||
                    deal.sourceName.contains(state.searchQuery, ignoreCase = true)

            val matchesCategory = when (state.selectedFilterCategory) {
                FilterCategory.ALL -> true
                FilterCategory.HIGH_DISCOUNT -> deal.discountPercent >= 35
                FilterCategory.AUCTION_NPL -> deal.propertyType.contains("Asta", ignoreCase = true) || deal.title.contains("NPL", ignoreCase = true)
                FilterCategory.BOOKMARKED -> deal.isBookmarked
            }

            val matchesSource = state.selectedSourceFilter == "ALL" || deal.sourceKey == state.selectedSourceFilter
            val matchesType = state.selectedPropertyTypeFilter == "ALL" || deal.propertyType.contains(state.selectedPropertyTypeFilter, ignoreCase = true)

            matchesSearch && matchesCategory && matchesSource && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    fun setMainTab(tab: MainTab) {
        _uiState.update { it.copy(selectedMainTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterCategory(category: FilterCategory) {
        _uiState.update { it.copy(selectedFilterCategory = category) }
    }

    fun setSourceFilter(sourceKey: String) {
        _uiState.update { it.copy(selectedSourceFilter = sourceKey) }
    }

    fun setPropertyTypeFilter(type: String) {
        _uiState.update { it.copy(selectedPropertyTypeFilter = type) }
    }

    fun setSelectedDealForDetail(deal: PropertyDeal?) {
        _uiState.update { it.copy(selectedDealForDetail = deal) }
    }

    fun toggleBookmark(deal: PropertyDeal) {
        viewModelScope.launch {
            repository.toggleBookmark(deal.id, deal.isBookmarked)
            // update selected deal view if open
            if (_uiState.value.selectedDealForDetail?.id == deal.id) {
                _uiState.update {
                    it.copy(selectedDealForDetail = deal.copy(isBookmarked = !deal.isBookmarked))
                }
            }
        }
    }

    fun updateDealNotes(dealId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateDealNotes(dealId, notes)
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(notes = notes))
                }
            }
        }
    }

    fun setAddDealDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddDealDialogOpen = open) }
    }

    fun setSelectedSourceForParser(source: ScraperSource?) {
        _uiState.update { it.copy(selectedSourceForParser = source, parserTestResult = null) }
    }

    fun setParserEditorOpen(open: Boolean) {
        _uiState.update { it.copy(isParserEditorOpen = open) }
    }

    fun updateSourceConfigStatus(sourceId: String, status: String) {
        viewModelScope.launch {
            repository.updateSourceConfigStatus(sourceId, status)
        }
    }

    fun runParserTest(source: ScraperSource, sampleContent: String, rulesJson: String) {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val result = repository.simulateParserTest(source, sampleContent, rulesJson)
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = result,
                    parserTestLogs = result.logs
                )
            }
        }
    }

    fun saveParserRules(sourceId: String, rulesJson: String) {
        viewModelScope.launch {
            repository.updateSourceParserRules(sourceId, rulesJson)
            setParserEditorOpen(false)
        }
    }

    fun addNewDeal(
        title: String,
        sourceKey: String,
        sourceName: String,
        location: String,
        propertyType: String,
        askingPrice: Double,
        marketValue: Double,
        sqm: Int,
        auctionDate: String?
    ) {
        viewModelScope.launch {
            val discount = if (marketValue > 0) (((marketValue - askingPrice) / marketValue) * 100).toInt() else 0
            val capRate = if (askingPrice > 0) ((askingPrice * 0.08) / askingPrice) * 100 else 8.0

            val newDeal = PropertyDeal(
                title = title.ifBlank { "Immobile Opportunità Radar" },
                sourceKey = sourceKey,
                sourceName = sourceName.ifBlank { "Fonte Personalizzata" },
                sourceUrl = "https://www.quimmo.it/annunci-immobiliari",
                location = location.ifBlank { "Milano (MI)" },
                propertyType = propertyType,
                askingPrice = askingPrice,
                estimatedMarketValue = marketValue,
                surfaceSqm = sqm,
                discountPercent = discount,
                estimatedCapRate = capRate,
                auctionDate = auctionDate,
                status = "LIVE",
                imageUrl = "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80",
                notes = "Aggiunto manualmente a Quantum Deal Radar."
            )
            repository.addDeal(newDeal)
            setAddDealDialogOpen(false)
        }
    }

    fun deleteDeal(deal: PropertyDeal) {
        viewModelScope.launch {
            repository.deleteDeal(deal)
            if (_uiState.value.selectedDealForDetail?.id == deal.id) {
                _uiState.update { it.copy(selectedDealForDetail = null) }
            }
        }
    }

    // ROI Calculator updates
    fun updateRoiPrice(valStr: String) {
        _roiCalculatorState.update { it.copy(purchasePriceStr = valStr) }
    }

    fun updateRoiRenovation(valStr: String) {
        _roiCalculatorState.update { it.copy(renovationCostStr = valStr) }
    }

    fun updateRoiLegalFees(valStr: String) {
        _roiCalculatorState.update { it.copy(legalAuctionFeesStr = valStr) }
    }

    fun updateRoiMonthlyRent(valStr: String) {
        _roiCalculatorState.update { it.copy(monthlyRentStr = valStr) }
    }

    fun updateRoiResale(valStr: String) {
        _roiCalculatorState.update { it.copy(expectedResaleStr = valStr) }
    }

    fun loadDealIntoCalculator(deal: PropertyDeal) {
        _roiCalculatorState.update {
            it.copy(
                purchasePriceStr = deal.askingPrice.toInt().toString(),
                renovationCostStr = (deal.surfaceSqm * 350).toString(),
                legalAuctionFeesStr = (deal.askingPrice * 0.04).toInt().toString(),
                monthlyRentStr = (deal.estimatedMarketValue * 0.005).toInt().toString(),
                expectedResaleStr = deal.estimatedMarketValue.toInt().toString()
            )
        }
        setMainTab(MainTab.ROI_CALCULATOR)
    }

    fun getPriceHistoryForDeal(dealId: Long): Flow<List<PriceHistory>> {
        return repository.getPriceHistoryForDeal(dealId)
    }
}
