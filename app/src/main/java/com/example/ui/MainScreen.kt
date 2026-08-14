package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarketTrendDrawer
import com.example.ui.components.AddDealDialog
import com.example.ui.components.DealDetailBottomSheet
import com.example.ui.components.ParserEditorDialog
import com.example.ui.components.PropertyComparisonBottomSheet
import com.example.ui.components.toComparableProperty
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DistressedPropertiesScreen
import com.example.ui.screens.InvestorBriefScreen
import com.example.ui.screens.MapViewScreen
import com.example.ui.screens.ParserSandboxScreen
import com.example.ui.screens.RadarFeedScreen
import com.example.ui.screens.RoiCalculatorScreen
import com.example.ui.theme.*

import androidx.compose.runtime.CompositionLocalProvider
import com.example.util.LocalAppStrings

@Composable
fun MainScreen(
    viewModel: DealRadarViewModel,
    distressedViewModel: DistressedPropertyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(uiState.appLanguage) { com.example.util.AppStrings(uiState.appLanguage) }
    val roiState by viewModel.roiCalculatorState.collectAsStateWithLifecycle()
    val allDeals by viewModel.allDeals.collectAsStateWithLifecycle()
    val allSavedProperties by viewModel.allSavedProperties.collectAsStateWithLifecycle()
    val filteredDeals by viewModel.filteredDeals.collectAsStateWithLifecycle()
    val allSources by viewModel.allSources.collectAsStateWithLifecycle()
    val investorProfile by viewModel.investorProfile.collectAsStateWithLifecycle()
    val recentlyViewedDeals by viewModel.recentlyViewedDeals.collectAsStateWithLifecycle()
    val savedDeals by viewModel.savedDeals.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val cachedMapTileCount by viewModel.cachedTileCount.collectAsStateWithLifecycle()
    val totalMapCacheSizeBytes by viewModel.totalMapCacheSizeBytes.collectAsStateWithLifecycle()
    val offlineMapRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val isDownloadingMapRegion by viewModel.isDownloadingMapRegion.collectAsStateWithLifecycle()
    val mapDownloadProgress by viewModel.mapDownloadProgress.collectAsStateWithLifecycle()

    val selectedDealHistory by remember(uiState.selectedDealForDetail) {
        if (uiState.selectedDealForDetail != null) {
            viewModel.getPriceHistoryForDeal(uiState.selectedDealForDetail!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    CompositionLocalProvider(LocalAppStrings provides strings) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    contentColor = TextSecondaryDark,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.RADAR_FEED,
                        onClick = { viewModel.setMainTab(MainTab.RADAR_FEED) },
                        icon = { Icon(Icons.Default.Radar, contentDescription = null) },
                        label = { Text(strings.navRadar, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_radar")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.DISTRESSED,
                        onClick = { viewModel.setMainTab(MainTab.DISTRESSED) },
                        icon = { Icon(Icons.Default.HouseSiding, contentDescription = null) },
                        label = { Text("Distressed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_distressed")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.MAP_VIEW,
                        onClick = { viewModel.setMainTab(MainTab.MAP_VIEW) },
                        icon = { Icon(Icons.Default.Map, contentDescription = null) },
                        label = { Text(strings.navMap, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_map")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.INVESTOR_BRIEF,
                        onClick = { viewModel.setMainTab(MainTab.INVESTOR_BRIEF) },
                        icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        label = { Text(strings.navBrief, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_brief")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.PARSER_SANDBOX,
                        onClick = { viewModel.setMainTab(MainTab.PARSER_SANDBOX) },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                        label = { Text(strings.navSources, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_sandbox")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.ROI_CALCULATOR,
                        onClick = { viewModel.setMainTab(MainTab.ROI_CALCULATOR) },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                        label = { Text(strings.navRoi, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_roi")
                    )

                    NavigationBarItem(
                        selected = uiState.selectedMainTab == MainTab.ANALYTICS,
                        onClick = { viewModel.setMainTab(MainTab.ANALYTICS) },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                        label = { Text(strings.navAnalytics, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoPurpleOnContainer,
                            selectedTextColor = BentoPurpleOnContainer,
                            indicatorColor = BentoPurpleHeader,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )
                }
            },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedMainTab) {
                MainTab.RADAR_FEED -> {
                    RadarFeedScreen(
                        uiState = uiState,
                        deals = filteredDeals,
                        sources = allSources,
                        recentlyViewedDeals = recentlyViewedDeals,
                        savedDeals = savedDeals,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onFilterCategorySelect = { viewModel.setFilterCategory(it) },
                        onSourceFilterSelect = { viewModel.setSourceFilter(it) },
                        onPropertyTypeSelect = { viewModel.setPropertyTypeFilter(it) },
                        onDealClick = { viewModel.setSelectedDealForDetail(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onCalculateClick = { viewModel.loadDealIntoCalculator(it) },
                        onAddDealClick = { viewModel.setAddDealDialogOpen(true) },
                        onHideExpiredAuctionsToggle = { viewModel.toggleHideExpiredAuctions() },
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        onClearHistory = { viewModel.clearRecentlyViewedHistory() },
                        onOpenMarketTrendDrawer = { viewModel.openMarketTrendDrawer(it) },
                        onStageChange = { id, stage -> viewModel.updateDealStage(id, stage) },
                        onCompareToggle = { viewModel.toggleDealComparison(it.id) },
                        onOpenComparisonClick = { viewModel.setComparisonSheetOpen(true) },
                        recentSearches = recentSearches,
                        onSubmitSearchQuery = { viewModel.submitSearchQuery(it) },
                        onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                        onClearRecentSearches = { viewModel.clearRecentSearches() }
                    )
                }
                MainTab.DISTRESSED -> {
                    DistressedPropertiesScreen(
                        viewModel = distressedViewModel
                    )
                }
                MainTab.MAP_VIEW -> {
                    MapViewScreen(
                        uiState = uiState,
                        deals = filteredDeals,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onFilterCategorySelect = { viewModel.setFilterCategory(it) },
                        onDealClick = { viewModel.setSelectedDealForDetail(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onCalculateClick = { viewModel.loadDealIntoCalculator(it) },
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        cachedTileCount = cachedMapTileCount,
                        totalCacheSizeBytes = totalMapCacheSizeBytes,
                        offlineRegions = offlineMapRegions,
                        isDownloadingMap = isDownloadingMapRegion,
                        mapDownloadProgress = mapDownloadProgress,
                        onDownloadDealMap = { viewModel.downloadOfflineMapForDeal(it) },
                        onDownloadCustomRegion = { name, lat, lng -> viewModel.downloadOfflineMapForCoordinates(name, lat, lng) },
                        onClearMapCache = { viewModel.clearAllOfflineMapCache() },
                        onDeleteMapRegion = { viewModel.deleteOfflineMapRegion(it) }
                    )
                }
                MainTab.INVESTOR_BRIEF -> {
                    InvestorBriefScreen(
                        profile = investorProfile,
                        allDeals = allDeals,
                        isFirebaseConfigured = viewModel.isFirebaseConfigured,
                        onSaveProfile = { viewModel.saveInvestorProfile(it) },
                        onSignUp = { email, pass, name, company, tier, capital, onResult ->
                            viewModel.signUpInvestor(email, pass, name, company, tier, capital, onResult)
                        },
                        onSignIn = { email, pass, onResult ->
                            viewModel.signInInvestor(email, pass, onResult)
                        },
                        onSignOut = { viewModel.signOutInvestor() },
                        onDealClick = { viewModel.setSelectedDealForDetail(it) },
                        onCalculateRoiClick = {
                            viewModel.loadDealIntoCalculator(it)
                            viewModel.setMainTab(MainTab.ROI_CALCULATOR)
                        },
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        onStageChange = { id, stage -> viewModel.updateDealStage(id, stage) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onTriggerScan = { viewModel.triggerWorkManagerCriteriaScan() },
                        onResetNotifiedCache = { viewModel.resetWorkManagerNotifiedCache() }
                    )
                }
                MainTab.PARSER_SANDBOX -> {
                    ParserSandboxScreen(
                        uiState = uiState,
                        sources = allSources,
                        onSourceSelectForTest = { viewModel.setSelectedSourceForParser(it) },
                        onRunTestClick = { source, sample, rules -> viewModel.runParserTest(source, sample, rules) },
                        onUpdateStatusClick = { id, status -> viewModel.updateSourceConfigStatus(id, status) },
                        onEditRulesClick = {
                            viewModel.setSelectedSourceForParser(it)
                            viewModel.setParserEditorOpen(true)
                        },
                        onCalibrateClick = { viewModel.calibrateSources() },
                        onImportDealsClick = { deals -> viewModel.importScrapedDeals(deals) },
                        onExecuteBatchScrapeClick = { viewModel.executeLiveBatchScrape() }
                    )
                }
                MainTab.ROI_CALCULATOR -> {
                    RoiCalculatorScreen(
                        state = roiState,
                        onPurchasePriceChange = { viewModel.updateRoiPrice(it) },
                        onRenovationChange = { viewModel.updateRoiRenovation(it) },
                        onLegalFeesChange = { viewModel.updateRoiLegalFees(it) },
                        onMonthlyRentChange = { viewModel.updateRoiMonthlyRent(it) },
                        onMonthlyExpensesChange = { viewModel.updateRoiMonthlyExpenses(it) },
                        onDownPaymentPercentChange = { viewModel.updateRoiDownPayment(it) },
                        onMortgageRateChange = { viewModel.updateRoiMortgageRate(it) },
                        onLoanTermYearsChange = { viewModel.updateRoiLoanTerm(it) },
                        onResaleChange = { viewModel.updateRoiResale(it) },
                        onApplyPreset = { down, rate, term -> viewModel.applyRoiPreset(down, rate, term) }
                    )
                }
                MainTab.ANALYTICS -> {
                    AnalyticsScreen(
                        deals = allDeals,
                        properties = allSavedProperties,
                        onOpenMarketTrendDrawer = { viewModel.openMarketTrendDrawer(it) }
                    )
                }
            }

            // Market Trend Info Drawer (Gemini AI)
            MarketTrendDrawer(
                isOpen = uiState.isMarketTrendDrawerOpen,
                onClose = { viewModel.closeMarketTrendDrawer() },
                selectedRegion = uiState.selectedRegionForReport,
                onRegionChange = { viewModel.setSelectedRegionForReport(it) },
                isGenerating = uiState.isGeneratingMarketReport,
                reportContent = uiState.marketReportContent,
                errorMessage = uiState.marketReportError,
                onGenerateReport = { region -> viewModel.generateMarketTrendReport(region) },
                regionDeals = allDeals
            )

            // BottomSheet for Deal Detail
            uiState.selectedDealForDetail?.let { deal ->
                DealDetailBottomSheet(
                    deal = deal,
                    priceHistory = selectedDealHistory,
                    onDismiss = { viewModel.setSelectedDealForDetail(null) },
                    onBookmarkToggle = { viewModel.toggleBookmark(deal) },
                    onCalculateRoiClick = {
                        viewModel.setSelectedDealForDetail(null)
                        viewModel.loadDealIntoCalculator(deal)
                    },
                    onSaveNotes = { notes -> viewModel.updateDealNotes(deal.id, notes) },
                    onDeleteClick = { viewModel.deleteDeal(deal) },
                    onRecordPriceDrop = { dealId, newPrice, label, date ->
                        viewModel.recordPriceDrop(dealId, newPrice, label, date)
                    },
                    onSetPriceAlertThreshold = { dealId, threshold ->
                        viewModel.setPriceAlertThreshold(dealId, threshold)
                    },
                    onTriggerTestNotification = { dealItem, threshold ->
                        viewModel.triggerTestNotification(dealItem, threshold)
                    },
                    onDownloadOfflineMap = { viewModel.downloadOfflineMapForDeal(it) },
                    onUpdateStage = { id, stage -> viewModel.updateDealStage(id, stage) }
                )
            }

            // Dialog for Adding a Deal
            if (uiState.isAddDealDialogOpen) {
                AddDealDialog(
                    onDismiss = { viewModel.setAddDealDialogOpen(false) },
                    onConfirm = { title, key, name, loc, type, price, mkt, sqm, date ->
                        viewModel.addNewDeal(title, key, name, loc, type, price, mkt, sqm, date)
                    }
                )
            }

            // Dialog for Editing Parser Rules
            if (uiState.isParserEditorOpen && uiState.selectedSourceForParser != null) {
                ParserEditorDialog(
                    source = uiState.selectedSourceForParser!!,
                    onDismiss = { viewModel.setParserEditorOpen(false) },
                    onSaveRules = { sourceId, rulesJson ->
                        viewModel.saveParserRules(sourceId, rulesJson)
                    }
                )
            }

            // BottomSheet for Property Comparison Table
            if (uiState.isComparisonSheetOpen) {
                val selectedComparisonDeals = remember(uiState.selectedComparisonDealIds, allDeals) {
                    allDeals.filter { uiState.selectedComparisonDealIds.contains(it.id) }.map { it.toComparableProperty() }
                }

                PropertyComparisonBottomSheet(
                    properties = selectedComparisonDeals,
                    onDismissRequest = { viewModel.setComparisonSheetOpen(false) },
                    onRemoveProperty = { idStr ->
                        val dealId = idStr.removePrefix("deal_").toLongOrNull()
                        if (dealId != null) {
                            viewModel.removeDealFromComparison(dealId)
                        }
                    },
                    onSimulateInRoiCalculator = { compProp ->
                        compProp.rawDeal?.let { deal ->
                            viewModel.loadDealIntoCalculator(deal)
                            viewModel.setComparisonSheetOpen(false)
                        }
                    }
                )
            }
        }
    }
}
}


