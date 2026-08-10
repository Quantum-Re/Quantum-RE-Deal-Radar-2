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
import com.example.ui.components.AddDealDialog
import com.example.ui.components.DealDetailBottomSheet
import com.example.ui.components.ParserEditorDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ParserSandboxScreen
import com.example.ui.screens.RadarFeedScreen
import com.example.ui.screens.RoiCalculatorScreen
import com.example.ui.theme.*

@Composable
fun MainScreen(viewModel: DealRadarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roiState by viewModel.roiCalculatorState.collectAsStateWithLifecycle()
    val allDeals by viewModel.allDeals.collectAsStateWithLifecycle()
    val filteredDeals by viewModel.filteredDeals.collectAsStateWithLifecycle()
    val allSources by viewModel.allSources.collectAsStateWithLifecycle()

    val selectedDealHistory by remember(uiState.selectedDealForDetail) {
        if (uiState.selectedDealForDetail != null) {
            viewModel.getPriceHistoryForDeal(uiState.selectedDealForDetail!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

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
                    label = { Text("Radar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    selected = uiState.selectedMainTab == MainTab.PARSER_SANDBOX,
                    onClick = { viewModel.setMainTab(MainTab.PARSER_SANDBOX) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Sources", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("ROI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onFilterCategorySelect = { viewModel.setFilterCategory(it) },
                        onSourceFilterSelect = { viewModel.setSourceFilter(it) },
                        onPropertyTypeSelect = { viewModel.setPropertyTypeFilter(it) },
                        onDealClick = { viewModel.setSelectedDealForDetail(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onCalculateClick = { viewModel.loadDealIntoCalculator(it) },
                        onAddDealClick = { viewModel.setAddDealDialogOpen(true) }
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
                        }
                    )
                }
                MainTab.ROI_CALCULATOR -> {
                    RoiCalculatorScreen(
                        state = roiState,
                        onPurchasePriceChange = { viewModel.updateRoiPrice(it) },
                        onRenovationChange = { viewModel.updateRoiRenovation(it) },
                        onLegalFeesChange = { viewModel.updateRoiLegalFees(it) },
                        onMonthlyRentChange = { viewModel.updateRoiMonthlyRent(it) },
                        onResaleChange = { viewModel.updateRoiResale(it) }
                    )
                }
                MainTab.ANALYTICS -> {
                    AnalyticsScreen(deals = allDeals)
                }
            }

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
                    onDeleteClick = { viewModel.deleteDeal(deal) }
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
        }
    }
}
