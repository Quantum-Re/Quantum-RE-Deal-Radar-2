package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.util.AuctionDateUtils
import com.example.util.BriefMatcher

enum class MainTab {
    RADAR_FEED,
    MY_PROPERTIES,
    MARKET_INSIGHTS,
    DISTRESSED,
    MAP_VIEW,
    INVESTOR_BRIEF,
    PARSER_SANDBOX,
    ROI_CALCULATOR,
    ANALYTICS
}

enum class FilterCategory {
    ALL,
    IN_TARGET_BRIEF,
    HIGH_DISCOUNT,
    AUCTION_NPL,
    BOOKMARKED,
    RECENTLY_VIEWED
}

data class UiState(
    val appLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.IT,
    val searchQuery: String = "",
    val selectedMainTab: MainTab = MainTab.RADAR_FEED,
    val selectedFilterCategory: FilterCategory = FilterCategory.ALL,
    val selectedSourceFilter: String = "ALL", // "ALL" or sourceId
    val selectedPropertyTypeFilter: String = "ALL", // "ALL", "Residenziale", "Commerciale", etc.
    val hideExpiredAuctions: Boolean = false,
    val selectedDealForDetail: PropertyDeal? = null,
    val selectedSourceForParser: ScraperSource? = null,
    val isAddDealDialogOpen: Boolean = false,
    val isParserEditorOpen: Boolean = false,
    val isRunningParserTest: Boolean = false,
    val parserTestLogs: List<String> = emptyList(),
    val parserTestResult: ScraperTestResult? = null,
    val isMarketTrendDrawerOpen: Boolean = false,
    val selectedRegionForReport: String = "Milano",
    val isGeneratingMarketReport: Boolean = false,
    val marketReportContent: String? = null,
    val marketReportError: String? = null,
    val selectedComparisonDealIds: Set<Long> = emptySet(),
    val isComparisonSheetOpen: Boolean = false
)

// ROI Calculator State
data class RoiCalculatorState(
    val purchasePriceStr: String = "185000",
    val renovationCostStr: String = "25000",
    val legalAuctionFeesStr: String = "8500",
    val monthlyRentStr: String = "1200",
    val monthlyExpensesStr: String = "200",
    val expectedResaleStr: String = "260000",
    val downPaymentPercentStr: String = "20",
    val mortgageRateStr: String = "3.2",
    val loanTermYearsStr: String = "25"
) {
    val purchasePrice: Double get() = purchasePriceStr.toDoubleOrNull() ?: 0.0
    val renovationCost: Double get() = renovationCostStr.toDoubleOrNull() ?: 0.0
    val legalFees: Double get() = legalAuctionFeesStr.toDoubleOrNull() ?: 0.0
    val monthlyRent: Double get() = monthlyRentStr.toDoubleOrNull() ?: 0.0
    val monthlyExpenses: Double get() = monthlyExpensesStr.toDoubleOrNull() ?: 0.0
    val expectedResale: Double get() = expectedResaleStr.toDoubleOrNull() ?: 0.0
    val downPaymentPercent: Double get() = downPaymentPercentStr.toDoubleOrNull() ?: 20.0
    val mortgageRate: Double get() = mortgageRateStr.toDoubleOrNull() ?: 0.0
    val loanTermYears: Int get() = loanTermYearsStr.toIntOrNull() ?: 25

    val LTVPercentStr: String get() = (100.0 - downPaymentPercent).toInt().coerceIn(0, 100).toString()

    // Total Project Cost
    val totalProjectCost: Double get() = purchasePrice + renovationCost + legalFees
    val totalInvestment: Double get() = totalProjectCost

    // Down payment & Loan amount
    val downPaymentAmount: Double get() = purchasePrice * (downPaymentPercent / 100.0)
    val loanAmount: Double get() = (purchasePrice - downPaymentAmount).coerceAtLeast(0.0)

    // Initial Out-of-Pocket Cash Required (CoC denominator)
    val initialCashRequired: Double get() = downPaymentAmount + renovationCost + legalFees

    // Annual Income & Expenses
    val annualGrossRent: Double get() = monthlyRent * 12.0
    val annualExpenses: Double get() = monthlyExpenses * 12.0
    val netOperatingIncome: Double get() = (annualGrossRent - annualExpenses).coerceAtLeast(0.0)

    // Monthly Mortgage Payment
    val monthlyMortgagePayment: Double get() {
        if (loanAmount <= 0.0) return 0.0
        if (mortgageRate <= 0.0) return loanAmount / (loanTermYears * 12).toDouble().coerceAtLeast(1.0)
        val monthlyRate = (mortgageRate / 100.0) / 12.0
        val n = (loanTermYears * 12).toDouble()
        val compound = Math.pow(1.0 + monthlyRate, n)
        if (compound == 1.0) return 0.0
        return loanAmount * (monthlyRate * compound) / (compound - 1.0)
    }

    val annualDebtService: Double get() = monthlyMortgagePayment * 12.0
    val annualNetCashFlow: Double get() = netOperatingIncome - annualDebtService
    val monthlyNetCashFlow: Double get() = annualNetCashFlow / 12.0

    // Cash-on-Cash Return (%)
    val cashOnCashReturnPercent: Double
        get() = if (initialCashRequired > 0.0) (annualNetCashFlow / initialCashRequired) * 100.0 else 0.0

    // Cap Rate (%)
    val netCapRatePercent: Double
        get() = if (totalProjectCost > 0.0) (netOperatingIncome / totalProjectCost) * 100.0 else 0.0

    // Gross Yield (%)
    val grossAnnualYieldPercent: Double
        get() = if (totalProjectCost > 0.0) (annualGrossRent / totalProjectCost) * 100.0 else 0.0

    // Flip Profit & ROI
    val totalFlipProfit: Double get() = expectedResale - totalProjectCost
    val flipROIPercent: Double
        get() = if (totalProjectCost > 0.0) (totalFlipProfit / totalProjectCost) * 100.0 else 0.0
}

class DealRadarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DealRadarRepository(
        database.propertyDealDao(),
        database.scraperSourceDao(),
        database.priceHistoryDao(),
        database.investorProfileDao(),
        database.propertyDao(),
        database.distressedPropertyDao()
    )

    private val authManager = com.example.auth.FirebaseAuthManager(application)

    val isFirebaseConfigured: Boolean
        get() = authManager.isFirebaseConfigured()

    val currentAuthUserEmail: String?
        get() = authManager.getCurrentUserEmail()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _roiCalculatorState = MutableStateFlow(RoiCalculatorState())
    val roiCalculatorState: StateFlow<RoiCalculatorState> = _roiCalculatorState.asStateFlow()

    val allDeals: StateFlow<List<PropertyDeal>> = repository.allDeals
    val allSources: StateFlow<List<ScraperSource>> = repository.allSources
    val allSavedProperties: StateFlow<List<Property>> = repository.allSavedProperties
    val recentlyViewedDeals: StateFlow<List<PropertyDeal>> = repository.recentlyViewedDeals
    val savedDeals: StateFlow<List<PropertyDeal>> = repository.bookmarkedDeals
    val investorProfile: StateFlow<InvestorProfile?> = repository.investorProfile

    private val recentSearchRepository = com.example.data.RecentSearchRepository(
        database.recentSearchDao()
    )

    val recentSearches: StateFlow<List<com.example.data.RecentSearchQuery>> = recentSearchRepository.recentSearches

    // Filtered deals flow based on search and selected chips
    val filteredDeals: StateFlow<List<PropertyDeal>> = combine(
        allDeals,
        uiState,
        investorProfile
    ) { deals, state, profile ->
        Log.d("DealRadarViewModel", "filteredDeals combine triggered with ${deals.size} total deals from DB")
        val searchTokens = state.searchQuery.trim().let { query ->
            java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        val filtered = deals.filter { deal ->
            val matchesSearch = if (searchTokens.isEmpty()) {
                true
            } else {
                val rawText = listOf(
                    deal.title,
                    deal.location,
                    deal.sourceName,
                    deal.sourceKey,
                    deal.propertyType,
                    deal.notes,
                    deal.status,
                    deal.dealStage,
                    "€${deal.askingPrice.toInt()}",
                    "${deal.askingPrice.toInt()}",
                    "${deal.surfaceSqm} mq",
                    "${deal.surfaceSqm}",
                    "-${deal.discountPercent}%",
                    "${deal.discountPercent}%",
                    deal.auctionDate ?: ""
                ).joinToString(" ")

                val searchableText = java.text.Normalizer.normalize(rawText, java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .lowercase()

                searchTokens.all { token -> searchableText.contains(token) }
            }

            val matchesCategory = when (state.selectedFilterCategory) {
                FilterCategory.ALL -> true
                FilterCategory.IN_TARGET_BRIEF -> BriefMatcher.evaluate(deal, profile).isTargetMatch
                FilterCategory.HIGH_DISCOUNT -> deal.discountPercent >= 35
                FilterCategory.AUCTION_NPL -> deal.propertyType.contains("Asta", ignoreCase = true) || deal.title.contains("NPL", ignoreCase = true) || deal.notes.contains("NPL", ignoreCase = true) || deal.notes.contains("Asta", ignoreCase = true)
                FilterCategory.BOOKMARKED -> deal.isBookmarked
                FilterCategory.RECENTLY_VIEWED -> deal.lastViewedAt > 0
            }

            val matchesSource = state.selectedSourceFilter == "ALL" || deal.sourceKey == state.selectedSourceFilter
            val matchesType = state.selectedPropertyTypeFilter == "ALL" || deal.propertyType.contains(state.selectedPropertyTypeFilter, ignoreCase = true)
            val matchesActive = !state.hideExpiredAuctions || !AuctionDateUtils.isExpired(deal.auctionDate)

            matchesSearch && matchesCategory && matchesSource && matchesType && matchesActive
        }

        val result = if (state.selectedFilterCategory == FilterCategory.RECENTLY_VIEWED) {
            filtered.sortedByDescending { it.lastViewedAt }
        } else {
            filtered
        }
        Log.d("DealRadarViewModel", "filteredDeals produced ${result.size} filtered deals (Category=${state.selectedFilterCategory}, Search='${state.searchQuery}')")
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        Log.d("DealRadarViewModel", "DealRadarViewModel init started, launching database seed check...")
        viewModelScope.launch {
            val seedResult = AppDatabase.seedDatabaseIfEmpty(getApplication())
            Log.d("DealRadarViewModel", "Database seed completed with result: $seedResult")
            repository.checkAndSeedDatabase()
            Log.d("DealRadarViewModel", "DealRadarViewModel database seed check completed successfully.")
        }
        com.example.service.DistressedWorkManagerScheduler.schedulePeriodicCheck(getApplication())
    }

    fun triggerWorkManagerCriteriaScan() {
        com.example.service.DistressedWorkManagerScheduler.triggerImmediateCheck(getApplication())
    }

    fun resetWorkManagerNotifiedCache() {
        com.example.service.DistressedWorkManagerScheduler.resetNotifiedCache(getApplication())
    }

    fun openDealById(dealId: Long) {
        viewModelScope.launch {
            val deal = repository.getDealById(dealId)
            if (deal != null) {
                setSelectedDealForDetail(deal)
            }
        }
    }

    fun saveInvestorProfile(profile: InvestorProfile) {
        viewModelScope.launch {
            repository.saveInvestorProfile(profile)
        }
    }

    fun signUpInvestor(
        email: String,
        pass: String,
        fullName: String,
        company: String,
        tier: String,
        capital: Double,
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            if (authManager.isFirebaseConfigured()) {
                val res = authManager.signUpWithEmail(email, pass)
                if (res is com.example.auth.AuthResult.Success) {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        fullName = fullName,
                        companyName = company,
                        email = res.email,
                        investorTier = tier,
                        availableCapital = capital,
                        isRegistered = true
                    )
                    repository.saveInvestorProfile(profile)
                }
                onResult(res)
            } else {
                val profile = (investorProfile.value ?: InvestorProfile()).copy(
                    fullName = fullName,
                    companyName = company,
                    email = email,
                    investorTier = tier,
                    availableCapital = capital,
                    isRegistered = true
                )
                repository.saveInvestorProfile(profile)
                onResult(com.example.auth.AuthResult.Success("LOCAL_USER", email))
            }
        }
    }

    fun signInInvestor(
        email: String,
        pass: String,
        onResult: (com.example.auth.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            if (authManager.isFirebaseConfigured()) {
                val res = authManager.signInWithEmail(email, pass)
                if (res is com.example.auth.AuthResult.Success) {
                    val profile = (investorProfile.value ?: InvestorProfile()).copy(
                        email = res.email,
                        isRegistered = true
                    )
                    repository.saveInvestorProfile(profile)
                }
                onResult(res)
            } else {
                val profile = (investorProfile.value ?: InvestorProfile()).copy(
                    email = email,
                    isRegistered = true
                )
                repository.saveInvestorProfile(profile)
                onResult(com.example.auth.AuthResult.Success("LOCAL_USER", email))
            }
        }
    }

    fun signOutInvestor() {
        viewModelScope.launch {
            authManager.signOut()
            val profile = (investorProfile.value ?: InvestorProfile()).copy(
                isRegistered = false
            )
            repository.saveInvestorProfile(profile)
        }
    }

    fun setMainTab(tab: MainTab) {
        _uiState.update { it.copy(selectedMainTab = tab) }
    }

    fun toggleLanguage() {
        _uiState.update {
            val nextLang = if (it.appLanguage == com.example.util.AppLanguage.IT) com.example.util.AppLanguage.EN else com.example.util.AppLanguage.IT
            it.copy(appLanguage = nextLang)
        }
    }

    fun setLanguage(lang: com.example.util.AppLanguage) {
        _uiState.update { it.copy(appLanguage = lang) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun submitSearchQuery(query: String) {
        setSearchQuery(query)
        viewModelScope.launch {
            recentSearchRepository.saveSearchQuery(query)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchRepository.removeSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearAll()
        }
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

    fun toggleHideExpiredAuctions() {
        _uiState.update { it.copy(hideExpiredAuctions = !it.hideExpiredAuctions) }
    }

    fun recordPriceDrop(dealId: Long, newPrice: Double, eventLabel: String, dateRecorded: String) {
        viewModelScope.launch {
            repository.recordPriceDrop(
                dealId = dealId,
                newPrice = newPrice,
                eventLabel = eventLabel,
                dateRecorded = dateRecorded,
                onPriceDropBelowThreshold = { updatedDeal, oldPrice, currentPrice, threshold ->
                    com.example.util.PriceAlertNotificationManager.sendPriceDropAlertNotification(
                        context = getApplication(),
                        deal = updatedDeal,
                        oldPrice = oldPrice,
                        newPrice = currentPrice,
                        thresholdPrice = threshold
                    )
                }
            )
            val currentDetail = _uiState.value.selectedDealForDetail
            if (currentDetail?.id == dealId) {
                val newDiscount = if (currentDetail.estimatedMarketValue > 0) {
                    (((currentDetail.estimatedMarketValue - newPrice) / currentDetail.estimatedMarketValue) * 100).toInt()
                } else currentDetail.discountPercent
                _uiState.update {
                    it.copy(
                        selectedDealForDetail = currentDetail.copy(
                            askingPrice = newPrice,
                            discountPercent = newDiscount,
                            status = "PRICE_CUT"
                        )
                    )
                }
            }
        }
    }

    fun setPriceAlertThreshold(dealId: Long, threshold: Double?) {
        viewModelScope.launch {
            repository.updatePriceAlertThreshold(dealId, threshold)
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(priceAlertThreshold = threshold))
                }
            }
        }
    }

    fun updateDealStage(dealId: Long, newStage: String) {
        viewModelScope.launch {
            repository.updateDealStage(dealId, newStage)
            if (_uiState.value.selectedDealForDetail?.id == dealId) {
                _uiState.update {
                    it.copy(selectedDealForDetail = it.selectedDealForDetail?.copy(dealStage = newStage))
                }
            }
        }
    }

    fun triggerTestNotification(deal: PropertyDeal, targetThreshold: Double) {
        setPriceAlertThreshold(deal.id, targetThreshold)
        // Send immediate test alert notification
        com.example.util.PriceAlertNotificationManager.sendTestAlertNotification(
            context = getApplication(),
            dealTitle = deal.title,
            targetPrice = targetThreshold
        )
    }

    fun setSelectedDealForDetail(deal: PropertyDeal?) {
        _uiState.update { it.copy(selectedDealForDetail = deal) }
        if (deal != null) {
            viewModelScope.launch {
                repository.markDealAsViewed(deal.id)
            }
        }
    }

    fun clearRecentlyViewedHistory() {
        viewModelScope.launch {
            repository.clearRecentlyViewedHistory()
        }
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

    fun importScrapedDeals(deals: List<PropertyDeal>) {
        viewModelScope.launch {
            repository.importScrapedDeals(deals)
            _uiState.update {
                it.copy(
                    parserTestLogs = it.parserTestLogs + ">>> SUCCESS: ${deals.size} Immobili Estratti dallo Scraper Importati nel Radar Feed!"
                )
            }
        }
    }

    fun executeLiveBatchScrape() {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val summary = repository.executeLiveBatchScrapeAllSources()
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = ScraperTestResult(
                        isSuccess = true,
                        extractedTitle = "Scraping Batch Completato con Successo!",
                        extractedPrice = null,
                        extractedMarketValue = null,
                        discountPercent = 0,
                        logs = summary.logs
                    ),
                    parserTestLogs = summary.logs
                )
            }
        }
    }

    fun calibrateSources() {
        _uiState.update { it.copy(isRunningParserTest = true) }
        viewModelScope.launch {
            val summary = repository.calibrateAllSources()
            _uiState.update {
                it.copy(
                    isRunningParserTest = false,
                    parserTestResult = ScraperTestResult(
                        isSuccess = summary.warningCount == 0,
                        extractedTitle = "Calibrazione Fonti Completata (${summary.activeCount}/${summary.totalSources} Attive)",
                        extractedPrice = null,
                        extractedMarketValue = null,
                        discountPercent = 0,
                        logs = summary.logs
                    ),
                    parserTestLogs = summary.logs
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

    fun updateRoiMonthlyExpenses(valStr: String) {
        _roiCalculatorState.update { it.copy(monthlyExpensesStr = valStr) }
    }

    fun updateRoiDownPayment(valStr: String) {
        _roiCalculatorState.update { it.copy(downPaymentPercentStr = valStr) }
    }

    fun updateRoiMortgageRate(valStr: String) {
        _roiCalculatorState.update { it.copy(mortgageRateStr = valStr) }
    }

    fun updateRoiLoanTerm(valStr: String) {
        _roiCalculatorState.update { it.copy(loanTermYearsStr = valStr) }
    }

    fun updateRoiResale(valStr: String) {
        _roiCalculatorState.update { it.copy(expectedResaleStr = valStr) }
    }

    fun applyRoiPreset(downPayment: String, mortgageRate: String, loanTerm: String) {
        _roiCalculatorState.update {
            it.copy(
                downPaymentPercentStr = downPayment,
                mortgageRateStr = mortgageRate,
                loanTermYearsStr = loanTerm
            )
        }
    }

    fun loadDealIntoCalculator(deal: PropertyDeal) {
        _roiCalculatorState.update {
            it.copy(
                purchasePriceStr = deal.askingPrice.toInt().toString(),
                renovationCostStr = (deal.surfaceSqm * 350).toString(),
                legalAuctionFeesStr = (deal.askingPrice * 0.04).toInt().toString(),
                monthlyRentStr = (deal.estimatedMarketValue * 0.005).toInt().toString(),
                monthlyExpensesStr = (deal.estimatedMarketValue * 0.001).toInt().toString(),
                expectedResaleStr = deal.estimatedMarketValue.toInt().toString()
            )
        }
        setMainTab(MainTab.ROI_CALCULATOR)
    }

    fun getPriceHistoryForDeal(dealId: Long): Flow<List<PriceHistory>> {
        return repository.getPriceHistoryForDeal(dealId)
    }

    fun openMarketTrendDrawer(region: String = "Milano") {
        _uiState.update {
            it.copy(
                isMarketTrendDrawerOpen = true,
                selectedRegionForReport = if (region.isNotBlank()) region else it.selectedRegionForReport
            )
        }
    }

    fun closeMarketTrendDrawer() {
        _uiState.update { it.copy(isMarketTrendDrawerOpen = false) }
    }

    fun setSelectedRegionForReport(region: String) {
        _uiState.update { it.copy(selectedRegionForReport = region) }
    }

    fun generateMarketTrendReport(region: String) {
        val regionName = if (region.isNotBlank()) region else _uiState.value.selectedRegionForReport
        _uiState.update {
            it.copy(
                selectedRegionForReport = regionName,
                isGeneratingMarketReport = true,
                marketReportContent = null,
                marketReportError = null
            )
        }

        viewModelScope.launch {
            val allDealsList = allDeals.value
            val regionDeals = allDealsList.filter {
                it.location.contains(regionName, ignoreCase = true) ||
                        it.title.contains(regionName, ignoreCase = true)
            }

            val result = com.example.util.GeminiMarketReportService.generateMonthlyMarketReport(
                regionName = regionName,
                regionDeals = regionDeals
            )

            result.fold(
                onSuccess = { content ->
                    _uiState.update {
                        it.copy(
                            isGeneratingMarketReport = false,
                            marketReportContent = content,
                            marketReportError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingMarketReport = false,
                            marketReportContent = null,
                            marketReportError = error.message ?: "Errore sconosciuto durante la generazione del report."
                        )
                    }
                }
            )
        }
    }

    // --- Offline Map Room Caching ---
    val offlineMapRepository by lazy {
        OfflineMapCacheRepository(AppDatabase.getDatabase(getApplication()).mapTileCacheDao())
    }

    val cachedTileCount = offlineMapRepository.cachedTileCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val totalMapCacheSizeBytes = offlineMapRepository.totalCacheSizeBytes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val offlineRegions = offlineMapRepository.offlineRegions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val isDownloadingMapRegion = MutableStateFlow(false)
    val mapDownloadProgress = MutableStateFlow(Pair(0, 0))

    fun downloadOfflineMapForDeal(deal: PropertyDeal, onComplete: () -> Unit = {}) {
        val coords = com.example.util.GeoUtils.getCoordinatesForLocation(deal.location)
        viewModelScope.launch {
            isDownloadingMapRegion.value = true
            mapDownloadProgress.value = Pair(0, 50)
            offlineMapRepository.downloadRegionForOfflineVisit(
                regionName = "Sopralluogo: ${deal.title.take(20)} (${deal.location})",
                propertyId = deal.id,
                centerLat = coords.latitude,
                centerLng = coords.longitude,
                radiusKm = 1.5,
                minZoom = 12,
                maxZoom = 16
            ) { downloaded, total ->
                mapDownloadProgress.value = Pair(downloaded, total)
            }
            isDownloadingMapRegion.value = false
            onComplete()
        }
    }

    fun downloadOfflineMapForCoordinates(
        regionName: String,
        lat: Double,
        lng: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isDownloadingMapRegion.value = true
            mapDownloadProgress.value = Pair(0, 50)
            offlineMapRepository.downloadRegionForOfflineVisit(
                regionName = regionName,
                propertyId = null,
                centerLat = lat,
                centerLng = lng,
                radiusKm = 2.0,
                minZoom = 12,
                maxZoom = 16
            ) { downloaded, total ->
                mapDownloadProgress.value = Pair(downloaded, total)
            }
            isDownloadingMapRegion.value = false
            onComplete()
        }
    }

    fun clearAllOfflineMapCache() {
        viewModelScope.launch {
            offlineMapRepository.clearAllCache()
        }
    }

    fun deleteOfflineMapRegion(id: Long) {
        viewModelScope.launch {
            offlineMapRepository.deleteRegion(id)
        }
    }

    // Property Comparison Management
    fun toggleDealComparison(dealId: Long) {
        _uiState.update { current ->
            val newSet = if (current.selectedComparisonDealIds.contains(dealId)) {
                current.selectedComparisonDealIds - dealId
            } else {
                current.selectedComparisonDealIds + dealId
            }
            current.copy(selectedComparisonDealIds = newSet)
        }
    }

    fun clearComparisonSelection() {
        _uiState.update { it.copy(selectedComparisonDealIds = emptySet(), isComparisonSheetOpen = false) }
    }

    fun setComparisonSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isComparisonSheetOpen = open) }
    }

    fun removeDealFromComparison(dealId: Long) {
        _uiState.update { current ->
            val newSet = current.selectedComparisonDealIds - dealId
            current.copy(
                selectedComparisonDealIds = newSet,
                isComparisonSheetOpen = if (newSet.size < 2) false else current.isComparisonSheetOpen
            )
        }
    }

    // My Properties Pipeline Actions
    fun addDealToMyProperties(
        deal: PropertyDeal,
        pipelineStatus: PipelineStatus = PipelineStatus.ANALYZED,
        renovationCost: Double = 0.0,
        targetResale: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val property = Property(
                title = deal.title,
                address = deal.location,
                price = deal.askingPrice,
                distressStatus = deal.status,
                propertyType = deal.propertyType,
                estimatedMarketValue = deal.estimatedMarketValue,
                surfaceSqm = deal.surfaceSqm,
                notes = deal.notes.ifBlank { "Analizzato tramite Radar Deals (${deal.sourceName})" },
                pipelineStatus = pipelineStatus.key,
                estimatedRenovationCost = renovationCost,
                targetResalePrice = if (targetResale > 0) targetResale else deal.estimatedMarketValue,
                latitude = deal.effectiveLatitude,
                longitude = deal.effectiveLongitude
            )
            repository.insertSavedProperty(property)
            onSuccess()
        }
    }

    fun updateSavedPropertyPipelineStatus(propertyId: Long, newStatus: PipelineStatus) {
        viewModelScope.launch {
            repository.updateSavedPropertyPipelineStatus(propertyId, newStatus.key)
        }
    }
}
