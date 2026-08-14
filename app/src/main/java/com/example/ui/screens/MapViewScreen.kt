package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MapOfflineRegion
import com.example.data.PropertyDeal
import com.example.ui.FilterCategory
import com.example.ui.UiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DistressedPropertyViewModel
import com.example.ui.components.DistressedPropertyMapView
import com.example.ui.components.InteractiveVectorMap
import com.example.ui.components.OfflineMapManagerDialog
import com.example.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// Data model for Regional Geo-Heatmap Zones
data class HeatmapRegion(
    val id: String,
    val name: String,
    val code: String,
    val center: LatLng,
    val polygon: List<LatLng>,
    val densityScore: Float, // 0.0f (low) to 1.0f (critical high)
    val totalDistressedAssets: Int,
    val avgDiscountPercent: Int,
    val avgPricePerSqMeter: Int,
    val topOpportunityType: String
) {
    val fillColor: Color
        get() = when {
            densityScore >= 0.75f -> RoseRed.copy(alpha = 0.38f)
            densityScore >= 0.50f -> AmberGold.copy(alpha = 0.32f)
            densityScore >= 0.30f -> CyanAccent.copy(alpha = 0.25f)
            else -> EmeraldGreen.copy(alpha = 0.20f)
        }

    val strokeColor: Color
        get() = when {
            densityScore >= 0.75f -> RoseRed
            densityScore >= 0.50f -> AmberGold
            densityScore >= 0.30f -> CyanAccent
            else -> EmeraldGreen
        }

    val statusBadgeText: String
        get() = when {
            densityScore >= 0.75f -> "🔥 Densità Critica Aste"
            densityScore >= 0.50f -> "⚡ Alta Concentrazione"
            densityScore >= 0.30f -> "📊 Densità Media"
            else -> "🌱 Bassa Densità"
        }
}

// Data model for Hotspot Density Clusters
data class HotspotCluster(
    val id: String,
    val name: String,
    val center: LatLng,
    val deals: List<PropertyDeal>,
    val radiusMeters: Double,
    val densityLevel: DensityLevel,
    val avgDiscount: Float,
    val auctionNplCount: Int
) {
    val fillColor: Color
        get() = when (densityLevel) {
            DensityLevel.CRITICAL_HOTSPOT -> RoseRed.copy(alpha = 0.25f)
            DensityLevel.HIGH_DENSITY -> AmberGold.copy(alpha = 0.22f)
            DensityLevel.MODERATE -> CyanAccent.copy(alpha = 0.18f)
        }

    val strokeColor: Color
        get() = when (densityLevel) {
            DensityLevel.CRITICAL_HOTSPOT -> RoseRed
            DensityLevel.HIGH_DENSITY -> AmberGold
            DensityLevel.MODERATE -> CyanAccent
        }
}

enum class DensityLevel {
    CRITICAL_HOTSPOT, // High discount + high concentration of Aste/NPL
    HIGH_DENSITY,     // Medium-high discount
    MODERATE          // Standard property cluster
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    uiState: UiState,
    deals: List<PropertyDeal>,
    onSearchQueryChange: (String) -> Unit,
    onFilterCategorySelect: (FilterCategory) -> Unit,
    onDealClick: (PropertyDeal) -> Unit,
    onBookmarkToggle: (PropertyDeal) -> Unit,
    onCalculateClick: (PropertyDeal) -> Unit,
    onLanguageToggle: () -> Unit = {},
    cachedTileCount: Int = 0,
    totalCacheSizeBytes: Long = 0L,
    offlineRegions: List<MapOfflineRegion> = emptyList(),
    isDownloadingMap: Boolean = false,
    mapDownloadProgress: Pair<Int, Int> = Pair(0, 0),
    onDownloadDealMap: (PropertyDeal) -> Unit = {},
    onDownloadCustomRegion: (String, Double, Double) -> Unit = { _, _, _ -> },
    onClearMapCache: () -> Unit = {},
    onDeleteMapRegion: (Long) -> Unit = {}
) {
    val strings = com.example.util.LocalAppStrings.current
    val distressedViewModel: DistressedPropertyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val distressedPropertiesList by distressedViewModel.allDistressedProperties.collectAsStateWithLifecycle()
    val savedAlertCriteriaState by distressedViewModel.savedAlertCriteria.collectAsStateWithLifecycle()

    var selectedMapMode by remember { mutableStateOf("RADAR") } // "RADAR" | "DISTRESSED"
    var isOfflineManagerOpen by remember { mutableStateOf(false) }
    var selectedDealId by remember { mutableStateOf<Long?>(deals.firstOrNull()?.id) }
    val selectedDeal = remember(selectedDealId, deals) {
        deals.find { it.id == selectedDealId } ?: deals.firstOrNull()
    }

    var selectedRegionId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(deals.size, distressedPropertiesList.size, selectedMapMode) {
        android.util.Log.d("MapViewScreen", "MapViewScreen state update: Mode=$selectedMapMode, RadarDealsCount=${deals.size}, DistressedPropertiesCount=${distressedPropertiesList.size}")
    }

    // Map Overlay Control States
    var useVectorEngine by remember { mutableStateOf(true) }
    var showHeatmapPolygons by remember { mutableStateOf(true) }
    var showDensityOverlay by remember { mutableStateOf(true) }
    var showDiscountHeatmap by remember { mutableStateOf(true) }
    var showHeatmapLegend by remember { mutableStateOf(true) }

    // Compute Regional Heatmap Zones using mock geo-polygons enriched with deal metrics
    val heatmapRegions = remember(deals) {
        generateHeatmapRegions(deals)
    }

    val selectedRegion = remember(selectedRegionId, heatmapRegions) {
        heatmapRegions.find { it.id == selectedRegionId }
    }

    // Compute Hotspot Clusters from deals list
    val hotspotClusters = remember(deals) {
        computeHotspotClusters(deals)
    }

    // Default Italy center
    val defaultCenter = LatLng(42.5, 12.5)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 5.8f)
    }

    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    val mapProperties by remember(selectedMapType) {
        derivedStateOf {
            MapProperties(
                mapType = selectedMapType,
                isBuildingEnabled = true,
                isIndoorEnabled = true
            )
        }
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        // Top Filter & Map Layer Control Bar
        Surface(
            color = BentoPurpleHeader,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Title & Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = strings.mapViewTitle,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Overlay Heatmap Densità & Hotspot Aste",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SurfaceCardDark,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier
                                .clickable { onLanguageToggle() }
                                .testTag("language_toggle_button_map")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = strings.language.flag, fontSize = 14.sp)
                                Text(text = strings.language.name, color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Room Offline Map Cache Manager Button
                        Surface(
                            color = SurfaceCardDark,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { isOfflineManagerOpen = true }
                                .testTag("offline_map_manager_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (cachedTileCount > 0) "Cache Room ($cachedTileCount)" else "Mappa Offline",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = SurfaceCardDark,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen)
                                )
                                Text(
                                    text = "${deals.size} ${if (strings.isItalian) "Mappati" else "Mapped"}",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Map View Mode Sub-tabs (Radar Deals vs Distressed Assets & Alert)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMapMode == "RADAR",
                        onClick = { selectedMapMode = "RADAR" },
                        label = { Text("📡 Radar Market Deals (${deals.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent,
                            enabled = true,
                            selected = selectedMapMode == "RADAR"
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_mode_radar_chip")
                    )

                    FilterChip(
                        selected = selectedMapMode == "DISTRESSED",
                        onClick = { selectedMapMode = "DISTRESSED" },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🚨 Aste & Distressed (${distressedPropertiesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (savedAlertCriteriaState.alertsEnabled) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseRed,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = RoseRed,
                            enabled = true,
                            selected = selectedMapMode == "DISTRESSED"
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("map_mode_distressed_chip")
                    )
                }

                // Search Box
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(strings.searchPlaceholder, color = TextSecondaryDark, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCardDark,
                        unfocusedContainerColor = SurfaceCardDark,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("map_search_input")
                )

                // Layer Controls: Regional Heatmap Polygons, Hotspot Concentric Circles & Pin Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showHeatmapPolygons,
                        onClick = { showHeatmapPolygons = !showHeatmapPolygons },
                        label = { Text("🗺️ Heatmap Zone (${heatmapRegions.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseRed.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = RoseRed,
                            enabled = true,
                            selected = showHeatmapPolygons
                        ),
                        modifier = Modifier.testTag("map_heatmap_zones_toggle")
                    )

                    FilterChip(
                        selected = showDensityOverlay,
                        onClick = { showDensityOverlay = !showDensityOverlay },
                        label = { Text("🔥 Hotspot (${hotspotClusters.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = AmberGold,
                            enabled = true,
                            selected = showDensityOverlay
                        ),
                        modifier = Modifier.testTag("map_hotspot_toggle")
                    )

                    FilterChip(
                        selected = showDiscountHeatmap,
                        onClick = { showDiscountHeatmap = !showDiscountHeatmap },
                        label = { Text("🏷️ Pins Sconto", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceCardBorder,
                            selectedBorderColor = CyanAccent,
                            enabled = true,
                            selected = showDiscountHeatmap
                        ),
                        modifier = Modifier.testTag("map_heatmap_toggle")
                    )
                }

                // Hotspots & Region Quick Jump Reel
                if (heatmapRegions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(heatmapRegions, key = { it.id }) { region ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSlateBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (region.id == selectedRegionId) AmberGold else region.strokeColor.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedRegionId = region.id
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(region.center, 9.2f)
                                            )
                                        }
                                    }
                                    .testTag("region_chip_${region.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(region.strokeColor)
                                    )
                                    Text(
                                        text = "${region.name}: ${region.totalDistressedAssets} asset (-${region.avgDiscountPercent}%)",
                                        color = TextPrimaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Maps Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("map_canvas_container")
        ) {
            if (selectedMapMode == "DISTRESSED") {
                DistressedPropertyMapView(
                    distressedProperties = distressedPropertiesList,
                    savedAlertCriteria = savedAlertCriteriaState,
                    onSaveAlertCriteria = { query, level, maxPrice, enabled ->
                        distressedViewModel.saveAlertCriteria(query, level, maxPrice, enabled)
                    },
                    onTriggerTestNotification = { address, price, level ->
                        distressedViewModel.triggerTestNotification(address, price, level)
                    },
                    onAddDistressedProperty = { address, price, level ->
                        distressedViewModel.addDistressedProperty(address, price, level)
                    },
                    onDeleteProperty = { property ->
                        distressedViewModel.deleteDistressedProperty(property)
                    },
                    onSaveNotes = { property, notes ->
                        distressedViewModel.updatePropertyNotes(property, notes)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (useVectorEngine) {
                InteractiveVectorMap(
                    deals = deals,
                    heatmapRegions = heatmapRegions,
                    hotspotClusters = hotspotClusters,
                    selectedDealId = selectedDealId,
                    selectedRegionId = selectedRegionId,
                    showHeatmapPolygons = showHeatmapPolygons,
                    showDensityOverlay = showDensityOverlay,
                    onDealSelected = { deal ->
                        selectedDealId = deal.id
                        onDealClick(deal)
                    },
                    onRegionSelected = { region ->
                        selectedRegionId = region.id
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
            ) {
                // LAYER 1: Regional Heatmap Polygons with Density Fill
                if (showHeatmapPolygons) {
                    heatmapRegions.forEach { region ->
                        Polygon(
                            points = region.polygon,
                            fillColor = region.fillColor,
                            strokeColor = region.strokeColor,
                            strokeWidth = if (region.id == selectedRegionId) 6f else 3f,
                            clickable = true,
                            onClick = {
                                selectedRegionId = region.id
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(region.center, 9.5f)
                                    )
                                }
                            }
                        )

                        // Centroid Heatmap Label Marker
                        MarkerComposable(
                            keys = arrayOf<Any>(region.id, "region_label", region.id == selectedRegionId),
                            state = rememberMarkerState(key = "region_${region.id}", position = region.center),
                            title = region.name,
                            snippet = "${region.totalDistressedAssets} Asset | Sconto -${region.avgDiscountPercent}%",
                            onClick = {
                                selectedRegionId = region.id
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(region.center, 9.8f)
                                    )
                                }
                                true
                            }
                        ) {
                            RegionHeatmapLabelBadge(region = region, isSelected = region.id == selectedRegionId)
                        }
                    }
                }

                // LAYER 2: Concentric Circular Hotspots
                if (showDensityOverlay) {
                    hotspotClusters.forEach { cluster ->
                        // Multi-tier gradient rings for realistic density attenuation
                        Circle(
                            center = cluster.center,
                            radius = cluster.radiusMeters * 1.3,
                            fillColor = cluster.fillColor.copy(alpha = 0.10f),
                            strokeColor = Color.Transparent,
                            strokeWidth = 0f
                        )

                        Circle(
                            center = cluster.center,
                            radius = cluster.radiusMeters,
                            fillColor = cluster.fillColor,
                            strokeColor = cluster.strokeColor,
                            strokeWidth = 3f
                        )

                        // Centroid Hotspot Badge Marker
                        MarkerComposable(
                            keys = arrayOf(cluster.id, "centroid_badge"),
                            state = rememberMarkerState(key = "cluster_${cluster.id}", position = cluster.center),
                            title = "🔥 Hotspot ${cluster.name}",
                            snippet = "${cluster.deals.size} Immobili Distressed (-${cluster.avgDiscount.toInt()}%)",
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(cluster.center, 12f)
                                    )
                                }
                                true
                            }
                        ) {
                            HotspotCentroidBadge(cluster = cluster)
                        }
                    }
                }

                // LAYER 3: Data-Driven Pins for Individual Property Deals
                deals.forEach { deal ->
                    val pos = LatLng(deal.effectiveLatitude, deal.effectiveLongitude)
                    val isSelected = (deal.id == selectedDealId)

                    MarkerComposable(
                        keys = arrayOf<Any>(deal.id, isSelected, showDiscountHeatmap),
                        state = rememberMarkerState(key = deal.id.toString(), position = pos),
                        title = deal.title,
                        snippet = "€${deal.askingPrice.toInt()} (-${deal.discountPercent}%)",
                        onClick = {
                            selectedDealId = deal.id
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(pos, 13.5f)
                                )
                            }
                            true
                        }
                    ) {
                        MapPinMarker(
                            deal = deal,
                            isSelected = isSelected,
                            pulseScale = pulseScale,
                            showHeatmapColors = showDiscountHeatmap
                        )
                    }
                }
            }

            // Top Floating Map Type Switcher
            Surface(
                color = SurfaceCardDark.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    MapTypeChip(
                        label = "🌐 Vettoriale",
                        isSelected = useVectorEngine,
                        onClick = { useVectorEngine = true }
                    )
                    MapTypeChip(
                        label = "🛰️ Google Maps",
                        isSelected = !useVectorEngine,
                        onClick = { useVectorEngine = false }
                    )
                    if (!useVectorEngine) {
                        MapTypeChip(
                            label = "Mappa",
                            isSelected = selectedMapType == MapType.NORMAL,
                            onClick = { selectedMapType = MapType.NORMAL }
                        )
                        MapTypeChip(
                            label = "Satellite",
                            isSelected = selectedMapType == MapType.SATELLITE,
                            onClick = { selectedMapType = MapType.SATELLITE }
                        )
                    }
                }
            }

            // Floating Controls: Zoom & Fit Bounds Buttons
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                    },
                    containerColor = SurfaceCardDark,
                    contentColor = TextPrimaryDark,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                    },
                    containerColor = SurfaceCardDark,
                    contentColor = TextPrimaryDark,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                FloatingActionButton(
                    onClick = {
                        if (deals.isNotEmpty()) {
                            val builder = LatLngBounds.builder()
                            deals.forEach { builder.include(LatLng(it.effectiveLatitude, it.effectiveLongitude)) }
                            coroutineScope.launch {
                                try {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
                                } catch (_: Exception) {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultCenter, 5.8f))
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultCenter, 5.8f))
                            }
                        }
                    },
                    containerColor = BentoPurpleHeader,
                    contentColor = AmberGold,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Inquadra Tutti gli Immobili")
                }
            }

            // Floating Heatmap Legend Bar (Bottom Right Overlay)
            if (showHeatmapLegend) {
                Surface(
                    color = SurfaceCardDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (selectedDeal != null || selectedRegion != null) 180.dp else 20.dp, end = 12.dp)
                        .widthIn(max = 220.dp)
                        .testTag("heatmap_legend_hud")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Legenda Heatmap",
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showHeatmapLegend = false },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(12.dp))
                            }
                        }

                        // Gradient Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            EmeraldGreen,
                                            CyanAccent,
                                            AmberGold,
                                            RoseRed
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bassa", color = TextSecondaryDark, fontSize = 9.sp)
                            Text("Media", color = TextSecondaryDark, fontSize = 9.sp)
                            Text("Alta", color = TextSecondaryDark, fontSize = 9.sp)
                            Text("Critica", color = RoseRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selected Region Info Popup (If Region Clicked)
            selectedRegion?.let { region ->
                Surface(
                    color = SurfaceCardDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, region.strokeColor),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .fillMaxWidth()
                        .testTag("selected_region_info_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(region.strokeColor)
                                )
                                Text(
                                    text = region.name,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = region.fillColor,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, region.strokeColor)
                            ) {
                                Text(
                                    text = region.statusBadgeText,
                                    color = TextPrimaryDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { selectedRegionId = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TextSecondaryDark)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Distressed Asset", color = TextSecondaryDark, fontSize = 10.sp)
                                Text("${region.totalDistressedAssets} Immobili", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sconto Medio", color = TextSecondaryDark, fontSize = 10.sp)
                                Text("-${region.avgDiscountPercent}%", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Prezzo Medio/m²", color = TextSecondaryDark, fontSize = 10.sp)
                                Text("€${region.avgPricePerSqMeter}/m²", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "💡 Opportunità Top: ${region.topOpportunityType}",
                            color = TextPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Floating Selected Deal Preview Card at Bottom
            if (selectedRegion == null) {
                selectedDeal?.let { deal ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = SurfaceCardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Image Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF334155))
                                        ) {
                                            if (deal.imageUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = deal.imageUrl,
                                                    contentDescription = deal.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Apartment,
                                                    contentDescription = null,
                                                    tint = TextSecondaryDark,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }

                                            // Discount Badge
                                            Surface(
                                                color = AmberGold,
                                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "-${deal.discountPercent}%",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = deal.sourceName,
                                                    color = CyanAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = deal.location,
                                                    color = TextSecondaryDark,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Text(
                                                text = deal.title,
                                                color = TextPrimaryDark,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "€${String.format(Locale.ITALY, "%,.0f", deal.askingPrice)}",
                                                    color = EmeraldGreen,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Stima: €${String.format(Locale.ITALY, "%,.0f", deal.estimatedMarketValue)}",
                                                    color = TextSecondaryDark,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onBookmarkToggle(deal) }
                                        ) {
                                            Icon(
                                                imageVector = if (deal.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Preferiti",
                                                tint = if (deal.isBookmarked) AmberGold else TextSecondaryDark
                                            )
                                        }
                                    }

                                    val context = LocalContext.current

                                    // Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onDealClick(deal) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleHeader),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scheda", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onCalculateClick(deal) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Calcola ROI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val uri = Uri.parse("geo:${deal.effectiveLatitude},${deal.effectiveLongitude}?q=${Uri.encode(deal.title)}")
                                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Impossibile aprire Mappe", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Icon(Icons.Default.Directions, contentDescription = "Naviga", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mappe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (isOfflineManagerOpen) {
        OfflineMapManagerDialog(
            cachedTileCount = cachedTileCount,
            totalCacheSizeBytes = totalCacheSizeBytes,
            offlineRegions = offlineRegions,
            isDownloading = isDownloadingMap,
            downloadProgress = mapDownloadProgress,
            availableDeals = deals,
            onDownloadDealMap = onDownloadDealMap,
            onDownloadCustomRegion = onDownloadCustomRegion,
            onClearCache = onClearMapCache,
            onDeleteRegion = onDeleteMapRegion,
            onDismiss = { isOfflineManagerOpen = false }
        )
    }
}

/**
 * Generates Regional Heatmap Polygons and geo-density statistics using mock boundaries and deal metrics.
 */
private fun generateHeatmapRegions(deals: List<PropertyDeal>): List<HeatmapRegion> {
    val rawRegions = listOf(
        Triple(
            "Lombardia - Milano Metro", "MI",
            LatLng(45.4642, 9.1900) to listOf(
                LatLng(45.62, 8.95), LatLng(45.65, 9.40),
                LatLng(45.30, 9.45), LatLng(45.28, 8.98)
            )
        ),
        Triple(
            "Lazio - Roma Metro", "RM",
            LatLng(41.9028, 12.4964) to listOf(
                LatLng(42.10, 12.25), LatLng(42.12, 12.75),
                LatLng(41.70, 12.80), LatLng(41.68, 12.20)
            )
        ),
        Triple(
            "Piemonte - Torino", "TO",
            LatLng(45.0703, 7.6869) to listOf(
                LatLng(45.22, 7.48), LatLng(45.25, 7.88),
                LatLng(44.92, 7.90), LatLng(44.90, 7.50)
            )
        ),
        Triple(
            "Emilia - Bologna", "BO",
            LatLng(44.4949, 11.3426) to listOf(
                LatLng(44.62, 11.15), LatLng(44.65, 11.55),
                LatLng(44.32, 11.58), LatLng(44.30, 11.18)
            )
        ),
        Triple(
            "Toscana - Firenze", "FI",
            LatLng(43.7696, 11.2558) to listOf(
                LatLng(43.90, 11.05), LatLng(43.92, 11.45),
                LatLng(43.60, 11.48), LatLng(43.58, 11.08)
            )
        ),
        Triple(
            "Campania - Napoli Metro", "NA",
            LatLng(40.8518, 14.2681) to listOf(
                LatLng(40.98, 14.05), LatLng(41.02, 14.48),
                LatLng(40.72, 14.50), LatLng(40.70, 14.10)
            )
        ),
        Triple(
            "Veneto - Venezia / Mestre", "VE",
            LatLng(45.4371, 12.3326) to listOf(
                LatLng(45.58, 12.10), LatLng(45.62, 12.52),
                LatLng(45.32, 12.55), LatLng(45.28, 12.12)
            )
        ),
        Triple(
            "Puglia - Bari", "BA",
            LatLng(41.1171, 16.8719) to listOf(
                LatLng(41.25, 16.65), LatLng(41.28, 17.05),
                LatLng(40.98, 17.08), LatLng(40.95, 16.68)
            )
        )
    )

    return rawRegions.mapIndexed { idx, (name, code, geo) ->
        val (center, polygon) = geo

        // Filter deals belonging to or close to this region
        val regionalDeals = deals.filter { deal ->
            calculateDistanceMeters(center.latitude, center.longitude, deal.effectiveLatitude, deal.effectiveLongitude) < 40000.0
        }

        val totalAssets = regionalDeals.size.coerceAtLeast((3 + (idx * 2) % 11))
        val avgDiscount = if (regionalDeals.isNotEmpty()) {
            regionalDeals.map { it.discountPercent }.average().toInt()
        } else {
            28 + (idx * 7) % 22
        }

        val densityScore = (totalAssets / 15f + avgDiscount / 60f).coerceIn(0.2f, 0.95f)

        val topType = when {
            densityScore >= 0.75f -> "Esecuzioni Immobiliari BPER / Quimmo (-42%)"
            densityScore >= 0.50f -> "Portafoglio NPL Bancari IQERA (-34%)"
            else -> "Aste Giudiziarie Residenziali Astalegale"
        }

        HeatmapRegion(
            id = "region_$idx",
            name = name,
            code = code,
            center = center,
            polygon = polygon,
            densityScore = densityScore,
            totalDistressedAssets = totalAssets,
            avgDiscountPercent = avgDiscount,
            avgPricePerSqMeter = 850 + (idx * 240) % 1200,
            topOpportunityType = topType
        )
    }
}

/**
 * Computes density hotspot clusters based on geographical distribution of property deals across major Italian urban hubs.
 */
private fun computeHotspotClusters(deals: List<PropertyDeal>): List<HotspotCluster> {
    if (deals.isEmpty()) return emptyList()

    val hubCenters = listOf(
        "Milano" to LatLng(45.4642, 9.1900),
        "Roma" to LatLng(41.9028, 12.4964),
        "Torino" to LatLng(45.0703, 7.6869),
        "Bologna" to LatLng(44.4949, 11.3426),
        "Firenze" to LatLng(43.7696, 11.2558),
        "Napoli" to LatLng(40.8518, 14.2681),
        "Venezia" to LatLng(45.4371, 12.3326),
        "Bari" to LatLng(41.1171, 16.8719)
    )

    val clusters = mutableListOf<HotspotCluster>()

    hubCenters.forEachIndexed { index, (name, center) ->
        // Find deals near this hub (within ~30km)
        val nearbyDeals = deals.filter { deal ->
            val dist = calculateDistanceMeters(
                center.latitude, center.longitude,
                deal.effectiveLatitude, deal.effectiveLongitude
            )
            dist < 35000.0
        }

        if (nearbyDeals.isNotEmpty()) {
            val avgDiscount = nearbyDeals.map { it.discountPercent }.average().toFloat()
            val auctionNplCount = nearbyDeals.count {
                it.propertyType.contains("Asta", ignoreCase = true) ||
                        it.sourceName.contains("Asta", ignoreCase = true) ||
                        it.sourceName.contains("NPL", ignoreCase = true)
            }

            val densityLevel = when {
                nearbyDeals.size >= 4 && avgDiscount >= 35f -> DensityLevel.CRITICAL_HOTSPOT
                nearbyDeals.size >= 2 || avgDiscount >= 25f -> DensityLevel.HIGH_DENSITY
                else -> DensityLevel.MODERATE
            }

            val radiusMeters = (10000.0 + nearbyDeals.size * 2500.0).coerceIn(12000.0, 35000.0)

            clusters.add(
                HotspotCluster(
                    id = "hub_$index",
                    name = name,
                    center = center,
                    deals = nearbyDeals,
                    radiusMeters = radiusMeters,
                    densityLevel = densityLevel,
                    avgDiscount = avgDiscount,
                    auctionNplCount = auctionNplCount
                )
            )
        }
    }

    return clusters.sortedByDescending { it.deals.size }
}

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
private fun RegionHeatmapLabelBadge(region: HeatmapRegion, isSelected: Boolean) {
    Surface(
        color = if (isSelected) AmberGold else SurfaceCardDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.White else region.strokeColor
        ),
        shadowElevation = if (isSelected) 8.dp else 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(region.strokeColor)
            )
            Text(
                text = region.code,
                color = if (isSelected) Color.Black else TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "-${region.avgDiscountPercent}%",
                color = if (isSelected) Color.Black else region.strokeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun HotspotCentroidBadge(cluster: HotspotCluster) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, cluster.strokeColor),
        shadowElevation = 8.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(cluster.strokeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (cluster.densityLevel == DensityLevel.CRITICAL_HOTSPOT) Icons.Default.LocalFireDepartment else Icons.Default.Gavel,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(13.dp)
                )
            }

            Column {
                Text(
                    text = "${cluster.name} Hotspot",
                    color = TextPrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cluster.deals.size} asset | -${cluster.avgDiscount.toInt()}%",
                    color = cluster.strokeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MapTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) AmberGold else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextSecondaryDark,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MapPinMarker(
    deal: PropertyDeal,
    isSelected: Boolean,
    pulseScale: Float,
    showHeatmapColors: Boolean = true
) {
    val pinColor = if (showHeatmapColors) {
        when {
            deal.discountPercent >= 40 -> RoseRed
            deal.discountPercent >= 25 -> AmberGold
            deal.propertyType.contains("Asta", ignoreCase = true) -> BentoPurpleHeader
            else -> CyanAccent
        }
    } else {
        CyanAccent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Price & Discount Badge
        Surface(
            color = if (isSelected) AmberGold else SurfaceCardDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else pinColor
            ),
            shadowElevation = if (isSelected) 10.dp else 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(pinColor)
                )
                Text(
                    text = "€${(deal.askingPrice / 1000).toInt()}k",
                    color = if (isSelected) Color.Black else TextPrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (isSelected) Color.Black else pinColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "-${deal.discountPercent}%",
                        color = if (isSelected) AmberGold else Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Pin Pointer Icon
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size((18 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(AmberGold.copy(alpha = 0.35f))
                )
            }

            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = if (isSelected) AmberGold else pinColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

