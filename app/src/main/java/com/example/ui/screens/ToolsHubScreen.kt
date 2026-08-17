package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainTab
import com.example.ui.theme.*
import com.example.util.LocalAppStrings

@Composable
fun ToolsHubScreen(
    onSelectTool: (MainTab) -> Unit,
    onLanguageToggle: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Strumenti & Analisi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Moduli avanzati per investimenti immobiliari",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }

            // Language Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BentoPurpleHeader,
                border = BorderStroke(1.dp, BentoPurpleOnContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clickable { onLanguageToggle() }
                    .testTag("language_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = strings.language.flag, fontSize = 13.sp)
                    Text(
                        text = strings.language.name,
                        color = BentoPurpleOnContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ToolItemCard(
                    title = "Market Insights & Trend AI",
                    subtitle = "Analisi di mercato, andamento quotazioni e reportistica intelligente",
                    icon = Icons.Default.Public,
                    accentColor = BentoPurpleOnContainer,
                    iconBg = BentoPurpleHeader,
                    testTag = "tool_card_market_insights",
                    onClick = { onSelectTool(MainTab.MARKET_INSIGHTS) }
                )
            }

            item {
                ToolItemCard(
                    title = "Aste Giudiziarie & Distressed",
                    subtitle = "Immobili all'asta, NPL, procedure esecutive e stralci",
                    icon = Icons.Default.HouseSiding,
                    accentColor = Color(0xFFE65100),
                    iconBg = Color(0xFFFFE0B2),
                    testTag = "tool_card_distressed",
                    onClick = { onSelectTool(MainTab.DISTRESSED) }
                )
            }

            item {
                ToolItemCard(
                    title = "Profilo Investitore & Criteri Target",
                    subtitle = "Brief di acquisto, parametri di rendimento minimo e matching affari",
                    icon = Icons.Default.Badge,
                    accentColor = Color(0xFF00897B),
                    iconBg = Color(0xFFE0F2F1),
                    testTag = "tool_card_investor_brief",
                    onClick = { onSelectTool(MainTab.INVESTOR_BRIEF) }
                )
            }

            item {
                ToolItemCard(
                    title = "Analytics Portafoglio",
                    subtitle = "Scomposizione asset, plusvalenze e statistiche dettagliate",
                    icon = Icons.Default.Analytics,
                    accentColor = Color(0xFF1565C0),
                    iconBg = Color(0xFFE3F2FD),
                    testTag = "tool_card_analytics",
                    onClick = { onSelectTool(MainTab.ANALYTICS) }
                )
            }

            item {
                ToolItemCard(
                    title = "Fonti Dati & Scraper Sandbox",
                    subtitle = "Gestione portali sorgente, regole di parsing e calibrazione dati",
                    icon = Icons.Default.Terminal,
                    accentColor = Color(0xFF6A1B9A),
                    iconBg = Color(0xFFF3E5F5),
                    testTag = "tool_card_parser_sandbox",
                    onClick = { onSelectTool(MainTab.PARSER_SANDBOX) }
                )
            }
        }
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardDark,
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
