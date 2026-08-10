package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyDeal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(deals: List<PropertyDeal>) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

    val totalAskingPrice = deals.sumOf { it.askingPrice }
    val totalMarketValue = deals.sumOf { it.estimatedMarketValue }
    val totalPotentialEquity = totalMarketValue - totalAskingPrice

    val sourceGroup = deals.groupBy { it.sourceName }
    val typeGroup = deals.groupBy { it.propertyType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PurpleIndigo, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Analytics Radar",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Statistiche e Distribuzione Valore Portafoglio",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Hero Card: Equity Surplus / Savings Potential
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Delta Valore Mercato vs Prezzo Iniziale",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "+ ${currencyFormat.format(totalPotentialEquity)}",
                    color = EmeraldGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Prezzo Asta / Richiesta", color = TextMutedDark, fontSize = 11.sp)
                        Text(currencyFormat.format(totalAskingPrice), color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Stima Perizia Totale", color = TextMutedDark, fontSize = 11.sp)
                        Text(currencyFormat.format(totalMarketValue), color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Distribution by Source Portal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = CyanAccent)
                    Text("Distribuzione Opportunità per Portale", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SurfaceCardBorder)

                sourceGroup.forEach { (sourceName, list) ->
                    val percent = if (deals.isNotEmpty()) (list.size.toFloat() / deals.size) else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sourceName, color = TextPrimaryDark, fontSize = 13.sp)
                            Text("${list.size} Deal (${(percent * 100).toInt()}%)", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanAccent,
                            trackColor = DarkSlateBg
                        )
                    }
                }
            }
        }

        // Section 3: Property Type Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AmberGold)
                    Text("Tipologia Asset Immobiliari", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SurfaceCardBorder)

                typeGroup.forEach { (type, list) ->
                    val percent = if (deals.isNotEmpty()) (list.size.toFloat() / deals.size) else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(type, color = TextPrimaryDark, fontSize = 13.sp)
                            Text("${list.size} Immobili", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AmberGold,
                            trackColor = DarkSlateBg
                        )
                    }
                }
            }
        }
    }
}
