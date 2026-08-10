package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RoiCalculatorState
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RoiCalculatorScreen(
    state: RoiCalculatorState,
    onPurchasePriceChange: (String) -> Unit,
    onRenovationChange: (String) -> Unit,
    onLegalFeesChange: (String) -> Unit,
    onMonthlyRentChange: (String) -> Unit,
    onResaleChange: (String) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.ITALY).apply {
        maximumFractionDigits = 0
    }

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
                    .background(EmeraldGreen, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Calcolatore Rendimento & ROI",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Valutazione Finanziaria Operazioni Immobiliari",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Section 1: Financial Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Parametri Operazione Iniziale",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Row 1: Purchase Price & Renovation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoiInputField(
                        label = "Prezzo Acquisto / Asta (€)",
                        value = state.purchasePriceStr,
                        onValueChange = onPurchasePriceChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_price")
                    )
                    RoiInputField(
                        label = "Costo Ristrutturazione (€)",
                        value = state.renovationCostStr,
                        onValueChange = onRenovationChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_renovation")
                    )
                }

                // Row 2: Legal Fees & Monthly Rent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoiInputField(
                        label = "Spese Legali & Asta (€)",
                        value = state.legalAuctionFeesStr,
                        onValueChange = onLegalFeesChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_fees")
                    )
                    RoiInputField(
                        label = "Canone Affitto Atteso (€/m)",
                        value = state.monthlyRentStr,
                        onValueChange = onMonthlyRentChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roi_input_rent")
                    )
                }

                // Resale Price for Flip
                RoiInputField(
                    label = "Prezzo Rivendita / Target Flip (€)",
                    value = state.expectedResaleStr,
                    onValueChange = onResaleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_input_resale")
                )
            }
        }

        // Section 2: Calculated Financial Dashboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoPurpleContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Risultati di Rendimento",
                        color = BentoPurpleOnContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BentoPurpleHeader
                    ) {
                        Text(
                            text = "INVESTIMENTO: ${currencyFormat.format(state.totalInvestment)}",
                            color = BentoPurpleOnContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                // Grid 2x2 for Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricResultCard(
                        title = "Rendimento Lordo Annuale",
                        value = "${String.format("%.1f", state.grossAnnualYieldPercent)}%",
                        subtext = "Sulla base dei canoni annui",
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricResultCard(
                        title = "Cap Rate Netto Stimato",
                        value = "${String.format("%.1f", state.netCapRatePercent)}%",
                        subtext = "Al netto di imposte e spese (82%)",
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricResultCard(
                        title = "Profitto Operazione Flip",
                        value = currencyFormat.format(state.totalFlipProfit),
                        subtext = "Capital gain da rivendita",
                        color = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricResultCard(
                        title = "ROI Operativo Total",
                        value = "${String.format("%.1f", state.flipROIPercent)}%",
                        subtext = "Ritorno sul capitale investito",
                        color = PurpleIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoiInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSlateBg,
                unfocusedContainerColor = DarkSlateBg,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricResultCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, color = TextSecondaryDark, fontSize = 11.sp)
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = subtext, color = TextMutedDark, fontSize = 10.sp)
        }
    }
}
