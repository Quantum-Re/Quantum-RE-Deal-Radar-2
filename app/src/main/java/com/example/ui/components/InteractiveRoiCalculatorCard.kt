package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Data model representing the inputs and derived financial calculations for the ROI Calculator.
 */
data class RoiCalculationData(
    val purchasePrice: Double = 180000.0,
    val renovationCost: Double = 25000.0,
    val legalFees: Double = 7500.0,
    val estimatedMonthlyRent: Double = 1200.0,
    val monthlyExpenses: Double = 180.0,
    val downPaymentPercent: Double = 20.0,
    val mortgageRatePercent: Double = 3.2,
    val loanTermYears: Int = 25,
    val expectedResalePrice: Double = 260000.0
) {
    val totalProjectCost: Double get() = purchasePrice + renovationCost + legalFees
    val downPaymentAmount: Double get() = purchasePrice * (downPaymentPercent / 100.0)
    val loanAmount: Double get() = (purchasePrice - downPaymentAmount).coerceAtLeast(0.0)
    val initialCashRequired: Double get() = downPaymentAmount + renovationCost + legalFees

    val annualGrossRent: Double get() = estimatedMonthlyRent * 12.0
    val annualExpenses: Double get() = monthlyExpenses * 12.0
    val netOperatingIncome: Double get() = (annualGrossRent - annualExpenses).coerceAtLeast(0.0)

    val monthlyMortgagePayment: Double get() {
        if (loanAmount <= 0.0) return 0.0
        if (mortgageRatePercent <= 0.0) return loanAmount / (loanTermYears * 12).toDouble().coerceAtLeast(1.0)
        val monthlyRate = (mortgageRatePercent / 100.0) / 12.0
        val n = (loanTermYears * 12).toDouble()
        val compound = Math.pow(1.0 + monthlyRate, n)
        if (compound == 1.0) return 0.0
        return loanAmount * (monthlyRate * compound) / (compound - 1.0)
    }

    val annualDebtService: Double get() = monthlyMortgagePayment * 12.0
    val annualNetCashFlow: Double get() = netOperatingIncome - annualDebtService
    val monthlyNetCashFlow: Double get() = annualNetCashFlow / 12.0

    // Yield Metrics
    val grossYieldPercent: Double
        get() = if (totalProjectCost > 0.0) (annualGrossRent / totalProjectCost) * 100.0 else 0.0

    val netYieldCapRatePercent: Double
        get() = if (totalProjectCost > 0.0) (netOperatingIncome / totalProjectCost) * 100.0 else 0.0

    val cashOnCashReturnPercent: Double
        get() = if (initialCashRequired > 0.0) (annualNetCashFlow / initialCashRequired) * 100.0 else 0.0

    val breakEvenYears: Double
        get() = if (annualNetCashFlow > 0.0) initialCashRequired / annualNetCashFlow else 0.0

    // Flip Metrics
    val totalFlipProfit: Double get() = expectedResalePrice - totalProjectCost
    val flipRoiPercent: Double
        get() = if (totalProjectCost > 0.0) (totalFlipProfit / totalProjectCost) * 100.0 else 0.0

    val yieldRating: YieldGrade
        get() = when {
            cashOnCashReturnPercent >= 12.0 || grossYieldPercent >= 10.0 -> YieldGrade.EXCELLENT
            cashOnCashReturnPercent >= 8.0 || grossYieldPercent >= 7.5 -> YieldGrade.VERY_GOOD
            cashOnCashReturnPercent >= 5.0 || grossYieldPercent >= 5.5 -> YieldGrade.GOOD
            else -> YieldGrade.MODERATE
        }
}

enum class YieldGrade(val label: String, val color: Color, val description: String) {
    EXCELLENT("⭐ Rendimento Eccezionale", EmeraldGreen, "Rendimento nettamente superiore alla media di mercato"),
    VERY_GOOD("✨ Ottimo Rendimento", CyanAccent, "Ottimo bilanciamento tra cashflow e rendimento"),
    GOOD("👍 Rendimento Buono", AmberGold, "Investimento solido con rendimento in linea col mercato"),
    MODERATE("ℹ️ Rendimento Moderato", TextMutedDark, "Rendimento prudenziale, valuta ottimizazione costi o affitto")
}

/**
 * Interactive ROI & Yield Calculator Component.
 * Allows instant manipulation of Purchase Price, Renovation Costs, and Estimated Rent
 * to calculate real-time investment yields (Gross Yield, Cap Rate, Cash-on-Cash, Monthly Cashflow).
 */
@Composable
fun InteractiveRoiCalculatorCard(
    modifier: Modifier = Modifier,
    initialPurchasePrice: Double = 180000.0,
    initialRenovationCost: Double = 25000.0,
    initialEstimatedRent: Double = 1200.0,
    initialLegalFees: Double = 7500.0,
    initialMonthlyExpenses: Double = 180.0,
    initialDownPaymentPercent: Double = 20.0,
    initialMortgageRate: Double = 3.2,
    initialLoanTermYears: Int = 25,
    propertyTitle: String = "Immobile Target Investimento",
    propertyLocation: String = "Italia",
    surfaceSqm: Int = 80,
    onCalculateCompleted: ((RoiCalculationData) -> Unit)? = null,
    onExportPdfClick: ((RoiCalculationData) -> Unit)? = null
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }

    // Internal State
    var purchasePriceStr by remember(initialPurchasePrice) {
        mutableStateOf(initialPurchasePrice.toInt().toString())
    }
    var renovationCostStr by remember(initialRenovationCost) {
        mutableStateOf(initialRenovationCost.toInt().toString())
    }
    var estimatedRentStr by remember(initialEstimatedRent) {
        mutableStateOf(initialEstimatedRent.toInt().toString())
    }
    var legalFeesStr by remember(initialLegalFees) {
        mutableStateOf(initialLegalFees.toInt().toString())
    }
    var monthlyExpensesStr by remember(initialMonthlyExpenses) {
        mutableStateOf(initialMonthlyExpenses.toInt().toString())
    }
    var downPaymentPercentStr by remember(initialDownPaymentPercent) {
        mutableStateOf(initialDownPaymentPercent.toInt().toString())
    }
    var mortgageRateStr by remember(initialMortgageRate) {
        mutableStateOf(initialMortgageRate.toString())
    }
    var loanTermYearsStr by remember(initialLoanTermYears) {
        mutableStateOf(initialLoanTermYears.toString())
    }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var selectedStrategyTab by remember { mutableStateOf(0) } // 0 = Buy & Hold (Rental), 1 = Fix & Flip

    // Derived Calculation
    val calculationData = remember(
        purchasePriceStr,
        renovationCostStr,
        estimatedRentStr,
        legalFeesStr,
        monthlyExpensesStr,
        downPaymentPercentStr,
        mortgageRateStr,
        loanTermYearsStr
    ) {
        val pPrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
        val rCost = renovationCostStr.toDoubleOrNull() ?: 0.0
        val eRent = estimatedRentStr.toDoubleOrNull() ?: 0.0
        val lFees = legalFeesStr.toDoubleOrNull() ?: 0.0
        val mExp = monthlyExpensesStr.toDoubleOrNull() ?: 0.0
        val dp = downPaymentPercentStr.toDoubleOrNull() ?: 20.0
        val mRate = mortgageRateStr.toDoubleOrNull() ?: 3.2
        val term = loanTermYearsStr.toIntOrNull() ?: 25
        val expectedResale = (pPrice + rCost + lFees) * 1.25

        val data = RoiCalculationData(
            purchasePrice = pPrice,
            renovationCost = rCost,
            legalFees = lFees,
            estimatedMonthlyRent = eRent,
            monthlyExpenses = mExp,
            downPaymentPercent = dp,
            mortgageRatePercent = mRate,
            loanTermYears = term,
            expectedResalePrice = expectedResale
        )
        onCalculateCompleted?.invoke(data)
        data
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with Title and Yield Rating Badge
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
                            .background(CyanAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calcolatore ROI",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Calcolatore Interattivo ROI",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulazione in tempo reale di rendimenti e flussi",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                // Yield Grade Pill
                Surface(
                    color = calculationData.yieldRating.color.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, calculationData.yieldRating.color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = calculationData.yieldRating.label,
                        color = calculationData.yieldRating.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Strategy Selector Tabs
            TabRow(
                selectedTabIndex = selectedStrategyTab,
                containerColor = DarkSlateBg,
                contentColor = CyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedStrategyTab]),
                        color = CyanAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedStrategyTab == 0,
                    onClick = { selectedStrategyTab = 0 },
                    text = {
                        Text(
                            text = "Locazione / Rendita (Messa a Reddito)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStrategyTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("roi_calc_tab_rental")
                )
                Tab(
                    selected = selectedStrategyTab == 1,
                    onClick = { selectedStrategyTab = 1 },
                    text = {
                        Text(
                            text = "Fix & Flip (Rivendita)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStrategyTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("roi_calc_tab_flip")
                )
            }

            // Key Results Hero Grid
            Surface(
                color = BentoPurpleContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedStrategyTab == 0) {
                        // CoC Return & Monthly Cash Flow Highlight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricBox(
                                label = "Cash-on-Cash Return",
                                value = "${String.format(Locale.US, "%.2f", calculationData.cashOnCashReturnPercent)}%",
                                subtext = "Ritorno annuo su €${currencyFormat.format(calculationData.initialCashRequired)} versati",
                                valueColor = EmeraldGreen,
                                testTag = "roi_calc_coc_value",
                                modifier = Modifier.weight(1.1f)
                            )
                            HeroMetricBox(
                                label = "Cash Flow Netto",
                                value = "${currencyFormat.format(calculationData.monthlyNetCashFlow)}/m",
                                subtext = "${currencyFormat.format(calculationData.annualNetCashFlow)}/anno",
                                valueColor = if (calculationData.monthlyNetCashFlow >= 0) CyanAccent else Color(0xFFFF5252),
                                testTag = "roi_calc_cashflow_value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Secondary Row: Gross Yield, Cap Rate, Break-even
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniMetricBox(
                                label = "Rendimento Lordo",
                                value = "${String.format(Locale.US, "%.1f", calculationData.grossYieldPercent)}%",
                                tag = "roi_calc_gross_yield_value",
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricBox(
                                label = "Cap Rate Netto",
                                value = "${String.format(Locale.US, "%.1f", calculationData.netYieldCapRatePercent)}%",
                                tag = "roi_calc_net_yield_value",
                                modifier = Modifier.weight(1f)
                            )
                            MiniMetricBox(
                                label = "Payback Time",
                                value = if (calculationData.breakEvenYears > 0) "${String.format(Locale.US, "%.1f", calculationData.breakEvenYears)} anni" else "N/D",
                                tag = "roi_calc_payback_value",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Flip View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricBox(
                                label = "Profitto Netto Flip",
                                value = currencyFormat.format(calculationData.totalFlipProfit),
                                subtext = "Costo totale: ${currencyFormat.format(calculationData.totalProjectCost)}",
                                valueColor = AmberGold,
                                testTag = "roi_calc_flip_profit_value",
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricBox(
                                label = "ROI Operazione",
                                value = "${String.format(Locale.US, "%.1f", calculationData.flipRoiPercent)}%",
                                subtext = "Margine su capitale investito",
                                valueColor = PurpleIndigo,
                                testTag = "roi_calc_flip_roi_value",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceCardBorder)

            // Section 1: Purchase Price Input & Steppers
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "1. Prezzo di Acquisto / Asta (€)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = currencyFormat.format(calculationData.purchasePrice),
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = purchasePriceStr,
                    onValueChange = { purchasePriceStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_purchase_price_input")
                )

                // Quick Step Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepChip("- €25.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr - 25000.0).coerceAtLeast(10000.0).toInt().toString()
                    }
                    StepChip("- €5.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr - 5000.0).coerceAtLeast(10000.0).toInt().toString()
                    }
                    StepChip("+ €5.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr + 5000.0).toInt().toString()
                    }
                    StepChip("+ €25.000") {
                        val curr = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        purchasePriceStr = (curr + 25000.0).toInt().toString()
                    }
                }
            }

            // Section 2: Renovation Costs Input & Tier Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "2. Costi di Ristrutturazione (€)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = currencyFormat.format(calculationData.renovationCost),
                        color = AmberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = renovationCostStr,
                    onValueChange = { renovationCostStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_renovation_input")
                )

                // Renovation Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(
                        label = "Nessuna (€0)",
                        selected = renovationCostStr == "0",
                        onClick = { renovationCostStr = "0" }
                    )
                    PresetChip(
                        label = "Rinfresco (€10k)",
                        selected = renovationCostStr == "10000",
                        onClick = { renovationCostStr = "10000" }
                    )
                    PresetChip(
                        label = "Media (€30k)",
                        selected = renovationCostStr == "30000",
                        onClick = { renovationCostStr = "30000" }
                    )
                    PresetChip(
                        label = "Totale (€60k)",
                        selected = renovationCostStr == "60000",
                        onClick = { renovationCostStr = "60000" }
                    )
                }
            }

            // Section 3: Estimated Monthly Rent Input & Strategy Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "3. Canone Affitto Mensile Stimato (€/m)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${currencyFormat.format(calculationData.estimatedMonthlyRent)}/m",
                        color = EmeraldGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = estimatedRentStr,
                    onValueChange = { estimatedRentStr = it.filter { char -> char.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSlateBg,
                        unfocusedContainerColor = DarkSlateBg,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roi_calc_rent_input")
                )

                // Rental Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepChip("- €100") {
                        val curr = estimatedRentStr.toDoubleOrNull() ?: 0.0
                        estimatedRentStr = (curr - 100.0).coerceAtLeast(100.0).toInt().toString()
                    }
                    StepChip("+ €100") {
                        val curr = estimatedRentStr.toDoubleOrNull() ?: 0.0
                        estimatedRentStr = (curr + 100.0).toInt().toString()
                    }
                    PresetChip(
                        label = "Monolocale (€650)",
                        selected = estimatedRentStr == "650",
                        onClick = { estimatedRentStr = "650" }
                    )
                    PresetChip(
                        label = "Bilocale (€950)",
                        selected = estimatedRentStr == "950",
                        onClick = { estimatedRentStr = "950" }
                    )
                    PresetChip(
                        label = "Stanze / Studenti (€1.400)",
                        selected = estimatedRentStr == "1400",
                        onClick = { estimatedRentStr = "1400" }
                    )
                    PresetChip(
                        label = "Short Rent / Airbnb (€1.900)",
                        selected = estimatedRentStr == "1900",
                        onClick = { estimatedRentStr = "1900" }
                    )
                }
            }

            // Advanced Financing & Outgoing Expenses Toggle
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (showAdvancedSettings) "Nascondi Mutuo & Spese Operative" else "Parametri Avanzati (Mutuo, Notai, Spese IMU)",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlateBg.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spese Legali & Asta (€)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = legalFeesStr,
                                onValueChange = { legalFeesStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_legal_fees_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spese Gest./IMU (€/m)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = monthlyExpensesStr,
                                onValueChange = { monthlyExpensesStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_expenses_input")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acconto Equity (%)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = downPaymentPercentStr,
                                onValueChange = { downPaymentPercentStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_downpayment_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tasso Mutuo (%)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = mortgageRateStr,
                                onValueChange = { mortgageRateStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_mortgage_rate_input")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Durata (Anni)", fontSize = 11.sp, color = TextSecondaryDark)
                            OutlinedTextField(
                                value = loanTermYearsStr,
                                onValueChange = { loanTermYearsStr = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roi_calc_loan_term_input")
                            )
                        }
                    }
                }
            }

            // Waterfall Cashflow Summary
            Surface(
                color = DarkSlateBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Riepilogo Costi & Finanziamento",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Costo Totale Operazione:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(currencyFormat.format(calculationData.totalProjectCost), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Capitale Proprio Versato:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(currencyFormat.format(calculationData.initialCashRequired), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Importo Mutuo Richiesto:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text("${currencyFormat.format(calculationData.loanAmount)} (${100 - calculationData.downPaymentPercent.toInt()}% LTV)", fontSize = 11.sp, color = AmberGold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rata Mutuo Stimata:", fontSize = 11.sp, color = TextSecondaryDark)
                        Text("${currencyFormat.format(calculationData.monthlyMortgagePayment)}/mese", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }
                }
            }

            // Quick Actions: Copy Summary & Export PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val summaryText = buildString {
                            appendLine("📊 SIMULAZIONE INVESTIMENTO IMMOBILIARE")
                            appendLine("💶 Prezzo Acquisto: ${currencyFormat.format(calculationData.purchasePrice)}")
                            appendLine("🔨 Ristrutturazione: ${currencyFormat.format(calculationData.renovationCost)}")
                            appendLine("🏢 Costo Totale: ${currencyFormat.format(calculationData.totalProjectCost)}")
                            appendLine("💰 Canone Affitto: ${currencyFormat.format(calculationData.estimatedMonthlyRent)}/m (${currencyFormat.format(calculationData.annualGrossRent)}/anno)")
                            appendLine("📈 Rendimento Lordo: ${String.format(Locale.US, "%.2f", calculationData.grossYieldPercent)}%")
                            appendLine("⚡ Cash-on-Cash Return: ${String.format(Locale.US, "%.2f", calculationData.cashOnCashReturnPercent)}%")
                            appendLine("💵 Cash Flow Netto: ${currencyFormat.format(calculationData.monthlyNetCashFlow)}/mese (${currencyFormat.format(calculationData.annualNetCashFlow)}/anno)")
                            appendLine("⏳ Payback Period: ${String.format(Locale.US, "%.1f", calculationData.breakEvenYears)} anni")
                        }
                        clipboardManager.setText(AnnotatedString(summaryText))
                        android.widget.Toast.makeText(context, "Sintesi ROI copiata negli appunti!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roi_calc_copy_summary_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copia Sintesi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (onExportPdfClick != null) {
                            onExportPdfClick(calculationData)
                        } else {
                            showExportDialog = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("roi_calc_export_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Esporta PDF", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showExportDialog) {
        PdfReportExportDialog(
            calcData = calculationData,
            initialTitle = propertyTitle,
            initialLocation = propertyLocation,
            initialSurfaceSqm = surfaceSqm,
            onDismissRequest = { showExportDialog = false }
        )
    }
}

@Composable
private fun HeroMetricBox(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, valueColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag(testTag)
            )
            Text(text = subtext, color = TextMutedDark, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MiniMetricBox(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 9.sp)
            Text(
                text = value,
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

@Composable
private fun StepChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCardDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SurfaceCardBorder),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) CyanAccent else SurfaceCardBorder,
        animationSpec = tween(200)
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) CyanAccent.copy(alpha = 0.15f) else SurfaceCardDark,
        animationSpec = tween(200)
    )
    val textColor = if (selected) CyanAccent else TextSecondaryDark

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
