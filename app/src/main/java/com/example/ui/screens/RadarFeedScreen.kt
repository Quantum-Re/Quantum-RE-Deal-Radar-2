package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PropertyDeal
import com.example.ui.FilterCategory
import com.example.ui.UiState
import com.example.ui.components.DealCard
import com.example.ui.components.DealKanbanBoard
import com.example.ui.components.RecentSearchesBar
import com.example.ui.components.QuickAccessDealsBar
import com.example.ui.theme.*
import com.example.util.LocalAppStrings

@Composable
fun FilterCategoryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) CyanAccent else SurfaceCardDark,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isSelected) CyanAccent else SurfaceCardBorder,
                RoundedCornerShape(20.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else TextSecondaryDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.Black else TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarFeedScreen(
    uiState: UiState,
    deals: List<PropertyDeal>,
    sources: List<com.example.data.ScraperSource>,
    recentlyViewedDeals: List<PropertyDeal> = emptyList(),
    savedDeals: List<PropertyDeal> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onFilterCategorySelect: (FilterCategory) -> Unit,
    onSourceFilterSelect: (String) -> Unit,
    onPropertyTypeSelect: (String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit,
    onAddDealClick: () -> Unit,
    onHideExpiredAuctionsToggle: () -> Unit = {},
    onLanguageToggle: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onOpenMarketTrendDrawer: (String) -> Unit = {},
    onStageChange: (dealId: Long, newStage: String) -> Unit = { _, _ -> },
    onCompareToggle: ((PropertyDeal) -> Unit)? = null,
    onOpenComparisonClick: (() -> Unit)? = null,
    recentSearches: List<com.example.data.RecentSearchQuery> = emptyList(),
    onSubmitSearchQuery: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var isSourceMenuExpanded by remember { mutableStateOf(false) }
    var isKanbanView by remember { mutableStateOf(false) }

    val totalDealsCount = deals.size
    val avgDiscount = if (deals.isNotEmpty()) deals.map { it.discountPercent }.average().toInt() else 0
    val avgCapRate = if (deals.isNotEmpty()) String.format("%.1f", deals.map { it.estimatedCapRate }.average()) else "0.0"

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDealClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                text = { Text("Aggiungi Deal", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = EmeraldGreen,
                modifier = Modifier.testTag("add_deal_fab")
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Header Bar
            Surface(
                color = SurfaceCardDark,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(CyanAccent, PurpleIndigo)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    color = TextPrimaryDark,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gemini Market Trend AI Drawer Button
                            Surface(
                                color = CyanAccent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { onOpenMarketTrendDrawer("Milano") }
                                    .testTag("open_market_trend_drawer_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    Text(text = "Trend AI", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Language Toggle Button
                            Surface(
                                color = BentoPurpleHeader,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                modifier = Modifier
                                    .clickable { onLanguageToggle() }
                                    .testTag("language_toggle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(text = strings.language.flag, fontSize = 14.sp)
                                    Text(text = strings.language.name, color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Source Provider Filter Dropdown Button
                            Box {
                                OutlinedButton(
                                    onClick = { isSourceMenuExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("source_filter_dropdown")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.selectedSourceFilter == "ALL") strings.sourceAll else uiState.selectedSourceFilter,
                                        fontSize = 11.sp
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSourceMenuExpanded,
                                    onDismissRequest = { isSourceMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceCardDark)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.sourceAll, color = TextPrimaryDark) },
                                        onClick = {
                                            onSourceFilterSelect("ALL")
                                            isSourceMenuExpanded = false
                                        }
                                    )
                                HorizontalDivider(color = SurfaceCardBorder)
                                sources.forEach { source ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(source.name, color = TextPrimaryDark, fontSize = 13.sp)
                                                Text(
                                                    "[${source.robotsStatus}]",
                                                    color = if (source.robotsStatus == "CONSENTITO") EmeraldGreen else AmberGold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSourceFilterSelect(source.id)
                                            isSourceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchPlaceholder, color = TextMutedDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSubmitSearchQuery(uiState.searchQuery) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSlateBg,
                            unfocusedContainerColor = DarkSlateBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceCardBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )

                    // Recent Searches Bar from Room Storage
                    RecentSearchesBar(
                        recentSearches = recentSearches,
                        onSelectQuery = { query ->
                            onSubmitSearchQuery(query)
                        },
                        onRemoveQuery = { query ->
                            onRemoveRecentSearch(query)
                        },
                        onClearAll = {
                            onClearRecentSearches()
                        }
                    )

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterCategoryChip(
                            label = strings.filterAll,
                            icon = Icons.Default.GridView,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.ALL,
                            onClick = { onFilterCategorySelect(FilterCategory.ALL) },
                            testTag = "filter_all"
                        )
                        FilterCategoryChip(
                            label = strings.filterTargetBrief,
                            icon = Icons.Default.Verified,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.IN_TARGET_BRIEF,
                            onClick = { onFilterCategorySelect(FilterCategory.IN_TARGET_BRIEF) },
                            testTag = "filter_target_brief"
                        )
                        FilterCategoryChip(
                            label = strings.filterHighDiscount,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.HIGH_DISCOUNT,
                            onClick = { onFilterCategorySelect(FilterCategory.HIGH_DISCOUNT) },
                            testTag = "filter_high_discount"
                        )
                        FilterCategoryChip(
                            label = strings.filterAuctionNpl,
                            icon = Icons.Default.Gavel,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.AUCTION_NPL,
                            onClick = { onFilterCategorySelect(FilterCategory.AUCTION_NPL) },
                            testTag = "filter_auctions"
                        )
                        FilterCategoryChip(
                            label = strings.filterBookmarked,
                            icon = Icons.Default.Bookmark,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.BOOKMARKED,
                            onClick = { onFilterCategorySelect(FilterCategory.BOOKMARKED) },
                            testTag = "filter_bookmarked"
                        )
                        FilterCategoryChip(
                            label = "Visti di Recente (${recentlyViewedDeals.size})",
                            icon = Icons.Default.History,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.RECENTLY_VIEWED,
                            onClick = { onFilterCategorySelect(FilterCategory.RECENTLY_VIEWED) },
                            testTag = "filter_recently_viewed"
                        )
                        FilterCategoryChip(
                            label = if (uiState.hideExpiredAuctions) "✓ ${strings.hideExpiredAuctions}" else strings.hideExpiredAuctions,
                            icon = Icons.Default.Event,
                            isSelected = uiState.hideExpiredAuctions,
                            onClick = { onHideExpiredAuctionsToggle() },
                            testTag = "filter_hide_expired"
                        )
                    }

                    // Property Type Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to strings.typeAll,
                            "Residenziale" to (if (strings.isItalian) "Residenziale" else "Residential"),
                            "Commerciale" to (if (strings.isItalian) "Commerciale" else "Commercial"),
                            "Industriale" to (if (strings.isItalian) "Industriale" else "Industrial"),
                            "Asta" to (if (strings.isItalian) "Aste Giudiziarie" else "Court Auctions")
                        ).forEach { (key, label) ->
                            val isSel = uiState.selectedPropertyTypeFilter == key
                            SuggestionChip(
                                onClick = { onPropertyTypeSelect(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSel) Color(0xFF0284C7).copy(alpha = 0.3f) else DarkSlateBg,
                                    labelColor = if (isSel) CyanAccent else TextSecondaryDark
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSel) CyanAccent else SurfaceCardBorder
                                )
                            )
                        }
                    }
                }
            }

            // Summary Stats & View Mode Switcher Bar
            Surface(
                color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$totalDealsCount ${strings.dealsFound}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${strings.avgDiscount}: -$avgDiscount%",
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${strings.avgCapRate}: $avgCapRate%",
                                color = AmberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Feed vs Kanban View Mode Switcher Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlateBg)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isKanbanView) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isKanbanView = false }
                                .testTag("view_mode_list_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = null,
                                    tint = if (!isKanbanView) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Elenco",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isKanbanView) Color.Black else TextSecondaryDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isKanbanView) CyanAccent else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isKanbanView = true }
                                .testTag("view_mode_kanban_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ViewColumn,
                                    contentDescription = null,
                                    tint = if (isKanbanView) Color.Black else TextSecondaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Kanban",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKanbanView) Color.Black else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            if (isKanbanView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    DealKanbanBoard(
                        deals = deals,
                        onStageChange = onStageChange,
                        onDealClick = onDealClick,
                        onBookmarkToggle = onBookmarkToggle,
                        onCalculateClick = onCalculateClick
                    )
                }
            } else if (deals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = strings.noDealsFound,
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prova a modificare i termini di ricerca o la fonte selezionata.",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                onSearchQueryChange("")
                                onFilterCategorySelect(FilterCategory.ALL)
                                onSourceFilterSelect("ALL")
                                onPropertyTypeSelect("ALL")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Text("Azzera Filtri", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("deals_list")
                ) {
                    item {
                        QuickAccessDealsBar(
                            recentlyViewedDeals = recentlyViewedDeals,
                            savedDeals = savedDeals,
                            onDealClick = onDealClick,
                            onBookmarkToggle = onBookmarkToggle,
                            onClearHistory = onClearHistory
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_radar_hero_1786384712474),
                                    contentDescription = "Quantum Deal Radar Hero",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    DarkSlateBg.copy(alpha = 0.95f),
                                                    DarkSlateBg.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "⚡ LIVE RADAR ATTIVO",
                                            color = EmeraldGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scansione Aste & Immobili Distressed",
                                        color = TextPrimaryDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Algoritmo AI di stima sconto di perizia e calcolo yield",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    items(deals, key = { it.id }) { deal ->
                        DealCard(
                            deal = deal,
                            onCardClick = { onDealClick(deal) },
                            onBookmarkToggle = { onBookmarkToggle(deal) },
                            onCalculateClick = { onCalculateClick(deal) },
                            isSelectedForCompare = uiState.selectedComparisonDealIds.contains(deal.id),
                            onCompareToggle = if (onCompareToggle != null) { { onCompareToggle(deal) } } else null
                        )
                    }
                }
            }
        }

        // Floating Compare Bar
        if (uiState.selectedComparisonDealIds.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .testTag("floating_compare_bar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyanAccent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${uiState.selectedComparisonDealIds.size} immobili selezionati",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tocca per confrontare la tabella side-by-side",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenComparisonClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_comparison_button")
                    ) {
                        Text("Confronta Ora", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
}

