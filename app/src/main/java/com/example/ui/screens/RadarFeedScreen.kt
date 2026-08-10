package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.FilterCategory
import com.example.ui.UiState
import com.example.ui.components.DealCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarFeedScreen(
    uiState: UiState,
    deals: List<PropertyDeal>,
    sources: List<com.example.data.ScraperSource>,
    onSearchQueryChange: (String) -> Unit,
    onFilterCategorySelect: (FilterCategory) -> Unit,
    onSourceFilterSelect: (String) -> Unit,
    onPropertyTypeSelect: (String) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit,
    onAddDealClick: () -> Unit
) {
    var isSourceMenuExpanded by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                    text = "Quantum Deal Radar",
                                    color = TextPrimaryDark,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monitoring Aste & Distressed Real Estate",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Source Provider Filter Dropdown Button
                        Box {
                            OutlinedButton(
                                onClick = { isSourceMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                modifier = Modifier.testTag("source_filter_dropdown")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.selectedSourceFilter == "ALL") "Tutte le Fonti" else uiState.selectedSourceFilter,
                                    fontSize = 12.sp
                                )
                            }

                            DropdownMenu(
                                expanded = isSourceMenuExpanded,
                                onDismissRequest = { isSourceMenuExpanded = false },
                                modifier = Modifier.background(SurfaceCardDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tutte le Fonti", color = TextPrimaryDark) },
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

                    // Search Field
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Cerca per città (es. Milano), titolo o codice...", color = TextMutedDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancella", tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
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

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterCategoryChip(
                            label = "Tutti i Deal",
                            icon = Icons.Default.GridView,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.ALL,
                            onClick = { onFilterCategorySelect(FilterCategory.ALL) },
                            testTag = "filter_all"
                        )
                        FilterCategoryChip(
                            label = "Sconto >35%",
                            icon = Icons.Default.TrendingDown,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.HIGH_DISCOUNT,
                            onClick = { onFilterCategorySelect(FilterCategory.HIGH_DISCOUNT) },
                            testTag = "filter_high_discount"
                        )
                        FilterCategoryChip(
                            label = "Aste & NPL",
                            icon = Icons.Default.Gavel,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.AUCTION_NPL,
                            onClick = { onFilterCategorySelect(FilterCategory.AUCTION_NPL) },
                            testTag = "filter_auctions"
                        )
                        FilterCategoryChip(
                            label = "Preferiti",
                            icon = Icons.Default.Bookmark,
                            isSelected = uiState.selectedFilterCategory == FilterCategory.BOOKMARKED,
                            onClick = { onFilterCategorySelect(FilterCategory.BOOKMARKED) },
                            testTag = "filter_bookmarked"
                        )
                    }

                    // Property Type Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "Tutti i Tipi", "Residenziale" to "Residenziale", "Commerciale" to "Commerciale", "Industriale" to "Industriale", "Asta" to "Aste Giudiziarie").forEach { (key, label) ->
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

            // Summary Stats Bar
            Surface(
                color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalDealsCount Opportunità Rilevate",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Sconto Medio: -$avgDiscount%",
                            color = EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Yield Medio: $avgCapRate%",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Deals List or Empty State
            if (deals.isEmpty()) {
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
                            text = "Nessun immobile corrisponde ai filtri",
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
                    items(deals, key = { it.id }) { deal ->
                        DealCard(
                            deal = deal,
                            onCardClick = { onDealClick(deal) },
                            onBookmarkToggle = { onBookmarkToggle(deal) },
                            onCalculateClick = { onCalculateClick(deal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterCategoryChip(
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
