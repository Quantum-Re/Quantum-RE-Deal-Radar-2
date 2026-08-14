package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.SeniorValuationEngine
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorAppraisalDialog(
    initialCoveredSqM: Double = 85.0,
    initialPricePerSqM: Double = 2200.0,
    onDismissRequest: () -> Unit,
    onApplyValuation: (estimatedValue: Double, commercialSqM: Double) -> Unit
) {
    var coveredSqMText by remember { mutableStateOf(initialCoveredSqM.toString()) }
    var basePriceSqMText by remember { mutableStateOf(initialPricePerSqM.toString()) }
    var terraceSqMText by remember { mutableStateOf("12") }
    var cellarSqMText by remember { mutableStateOf("6") }
    var gardenSqMText by remember { mutableStateOf("0") }
    var sanatoriaCostText by remember { mutableStateOf("0") }

    var selectedCondition by remember { mutableStateOf(SeniorValuationEngine.PropertyCondition.GOOD_CONDITION) }
    var selectedFloor by remember { mutableStateOf(SeniorValuationEngine.FloorLevel.INTERMEDIATE) }
    var hasElevator by remember { mutableStateOf(true) }
    var selectedEnergyClass by remember { mutableStateOf(SeniorValuationEngine.EnergyClass.CLASS_D_C) }
    var selectedOccupancy by remember { mutableStateOf(SeniorValuationEngine.OccupancyStatus.VACANT) }
    var hasGarage by remember { mutableStateOf(false) }
    var hasPanoramicView by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }

    // Live Calculation
    val valuationResult = remember(
        coveredSqMText, basePriceSqMText, terraceSqMText, cellarSqMText, gardenSqMText, sanatoriaCostText,
        selectedCondition, selectedFloor, hasElevator, selectedEnergyClass, selectedOccupancy, hasGarage, hasPanoramicView
    ) {
        val coveredSqM = coveredSqMText.toDoubleOrNull() ?: 85.0
        val basePriceSqM = basePriceSqMText.toDoubleOrNull() ?: 2200.0
        val terraceSqM = terraceSqMText.toDoubleOrNull() ?: 0.0
        val cellarSqM = cellarSqMText.toDoubleOrNull() ?: 0.0
        val gardenSqM = gardenSqMText.toDoubleOrNull() ?: 0.0
        val sanatoriaCost = sanatoriaCostText.toDoubleOrNull() ?: 0.0

        val input = SeniorValuationEngine.ValuationInput(
            mainCoveredSqM = coveredSqM,
            balconyTerraceSqM = terraceSqM,
            cellarAtticSqM = cellarSqM,
            gardenSqM = gardenSqM,
            baseZonePricePerSqM = basePriceSqM,
            condition = selectedCondition,
            floorLevel = selectedFloor,
            hasElevator = hasElevator,
            energyClass = selectedEnergyClass,
            occupancyStatus = selectedOccupancy,
            hasGarageOrParking = hasGarage,
            hasPanoramicView = hasPanoramicView,
            estimatedSanatoriaCost = sanatoriaCost
        )

        SeniorValuationEngine.calculateValuation(input)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Perizia Senior",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Perizia Estimativa Senior (UNI 10750)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "20 anni di esperienza in estimo immobiliare",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Result Summary Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Valore Peritale Stimato",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = currencyFormat.format(valuationResult.totalEstimatedMarketValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Forchetta Peritale: ${currencyFormat.format(valuationResult.minFairValue)} - ${currencyFormat.format(valuationResult.maxFairValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Sup. Commerciale",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${valuationResult.commercialSqM} m²",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Prezzo/m² Corretto",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "€${valuationResult.adjustedPricePerSqM}/m²",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Var. Ponderata",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${if (valuationResult.totalCorrectionPercentage >= 0) "+" else ""}${valuationResult.totalCorrectionPercentage}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (valuationResult.totalCorrectionPercentage >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Section 1: Surface & Base Zone Price
                    Text(
                        text = "1. Superfici e Prezzo Base di Zona OMI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = coveredSqMText,
                            onValueChange = { coveredSqMText = it },
                            label = { Text("Coperti Principali (m²)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = basePriceSqMText,
                            onValueChange = { basePriceSqMText = it },
                            label = { Text("Prezzo Base OMI (€/m²)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = terraceSqMText,
                            onValueChange = { terraceSqMText = it },
                            label = { Text("Balconi/Terrazzi (m²)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cellarSqMText,
                            onValueChange = { cellarSqMText = it },
                            label = { Text("Cantina/Soffitta (m²)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gardenSqMText,
                            onValueChange = { gardenSqMText = it },
                            label = { Text("Giardino Privato (m²)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sanatoriaCostText,
                            onValueChange = { sanatoriaCostText = it },
                            label = { Text("Costo Sanatorie (€)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Section 2: Building Characteristics
                    Text(
                        text = "2. Coefficienti Intrinseci dell'Immobile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Stato Manutentivo
                    Text("Stato Conservativo:", style = MaterialTheme.typography.labelMedium)
                    SeniorValuationEngine.PropertyCondition.entries.forEach { condition ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedCondition == condition,
                                onClick = { selectedCondition = condition }
                            )
                            Text(
                                text = condition.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Piano ed Ascensore
                    Text("Piano e Servizio Ascensore:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasElevator,
                            onCheckedChange = { hasElevator = it }
                        )
                        Text("Presente Ascensore", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Piano Selection Dropdown/Radios
                    SeniorValuationEngine.FloorLevel.entries.forEach { floor ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedFloor == floor,
                                onClick = { selectedFloor = floor }
                            )
                            Text(
                                text = floor.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Classe Energetica
                    Text("Classe Energetica (APE):", style = MaterialTheme.typography.labelMedium)
                    SeniorValuationEngine.EnergyClass.entries.forEach { eClass ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedEnergyClass == eClass,
                                onClick = { selectedEnergyClass = eClass }
                            )
                            Text(
                                text = eClass.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Section 3: Legal & Distress Factors
                    Text(
                        text = "3. Stato Legale e Pertinenze",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SeniorValuationEngine.OccupancyStatus.entries.forEach { occupancy ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedOccupancy == occupancy,
                                onClick = { selectedOccupancy = occupancy }
                            )
                            Text(
                                text = occupancy.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasGarage, onCheckedChange = { hasGarage = it })
                            Text("Garage / Box", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasPanoramicView, onCheckedChange = { hasPanoramicView = it })
                            Text("Vista Panoramica", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Section 4: Peritale Breakdown
                    Text(
                        text = "4. Prospetto Fattori di Correzione",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        valuationResult.breakdownFactors.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }
                    Button(
                        onClick = {
                            onApplyValuation(
                                valuationResult.totalEstimatedMarketValue,
                                valuationResult.commercialSqM
                            )
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Applica Valore Stima")
                    }
                }
            }
        }
    }
}
