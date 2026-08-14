package com.example.util

import kotlin.math.roundToInt

/**
 * Senior Real Estate Valuation Engine (Metodo Stima Peritale Senior / MCA UNI 10750)
 * Simulates a professional real estate appraiser with 20+ years of experience in the Italian market.
 */
object SeniorValuationEngine {

    enum class PropertyCondition(val label: String, val factor: Double) {
        NEEDS_TOTAL_RENOVATION("Da Ristrutturare Completamente", 0.75),
        HABITABLE_ORIGINAL("Abitabile / Stato Originale", 0.90),
        GOOD_CONDITION("Buono Stato / Parzialmente Ristrutturato", 1.00),
        RENOVATED_NEW("Ristrutturato a Nuovo", 1.15),
        LUXURY_FINISHES("Finiture di Lusso / Nuova Costruzione", 1.25)
    }

    enum class FloorLevel(val label: String, val factorWithElevator: Double, val factorNoElevator: Double) {
        BASEMENT("Seminterrato / Interrato", 0.75, 0.75),
        GROUND_FLOOR("Piano Terra", 0.85, 0.85),
        MEZZANINE("Piano Rialzato / Ammezzato", 0.90, 0.90),
        INTERMEDIATE("Piano Intermedio (1°-4°)", 1.00, 0.95),
        HIGH_FLOOR("Piano Alto (5°+)", 1.05, 0.85),
        PENTHOUSE("Attico / Ultimo Piano", 1.20, 0.90)
    }

    enum class EnergyClass(val label: String, val factor: Double) {
        CLASS_G("Classe G (Elevata Vetustà)", 0.90),
        CLASS_F_E("Classe F - E", 0.95),
        CLASS_D_C("Classe D - C", 1.00),
        CLASS_B_A1("Classe B - A1", 1.06),
        CLASS_A2_A4("Classe A2 - A4 (NZEB Green)", 1.12)
    }

    enum class OccupancyStatus(val label: String, val discountFactor: Double) {
        VACANT("Libero da Persone e Cose", 1.00),
        RENTED_REGULAR("Locato con Contratto Regolare a Rendita", 0.90),
        OCCUPIED_DEBTOR("Occupato dall'Esecutato / Proprietario", 0.85),
        OCCUPIED_NO_TITLE("Occupato Senza Titolo / Contratto Non Opponibile", 0.75)
    }

    data class ValuationInput(
        val mainCoveredSqM: Double,
        val balconyTerraceSqM: Double = 0.0,
        val cellarAtticSqM: Double = 0.0,
        val gardenSqM: Double = 0.0,
        val baseZonePricePerSqM: Double,
        val condition: PropertyCondition = PropertyCondition.GOOD_CONDITION,
        val floorLevel: FloorLevel = FloorLevel.INTERMEDIATE,
        val hasElevator: Boolean = true,
        val energyClass: EnergyClass = EnergyClass.CLASS_D_C,
        val occupancyStatus: OccupancyStatus = OccupancyStatus.VACANT,
        val hasGarageOrParking: Boolean = false,
        val hasPanoramicView: Boolean = false,
        val estimatedSanatoriaCost: Double = 0.0
    )

    data class ValuationResult(
        val commercialSqM: Double,
        val baseMarketValueSqM: Double,
        val adjustedPricePerSqM: Double,
        val totalEstimatedMarketValue: Double,
        val minFairValue: Double,
        val maxFairValue: Double,
        val recommendedAuctionBid20Margin: Double,
        val recommendedAuctionBid30Margin: Double,
        val totalCorrectionPercentage: Double,
        val breakdownFactors: List<Pair<String, String>>,
        val appraisalSummary: String
    )

    fun calculateValuation(input: ValuationInput): ValuationResult {
        // 1. Calculate Commercial Surface Area (UNI 10750)
        val commercialSqM = input.mainCoveredSqM +
                (input.balconyTerraceSqM * 0.35) +
                (input.cellarAtticSqM * 0.25) +
                (input.gardenSqM * 0.10)

        // 2. Determine Micro-location & Building Correction Factors
        val conditionFactor = input.condition.factor
        val floorFactor = if (input.hasElevator) input.floorLevel.factorWithElevator else input.floorLevel.factorNoElevator
        val energyFactor = input.energyClass.factor
        val garageFactor = if (input.hasGarageOrParking) 1.05 else 1.00
        val viewFactor = if (input.hasPanoramicView) 1.05 else 1.00
        val occupancyFactor = input.occupancyStatus.discountFactor

        // Combined Multiplicative Coefficient
        val totalCombinedFactor = conditionFactor * floorFactor * energyFactor * garageFactor * viewFactor * occupancyFactor
        val totalCorrectionPercentage = ((totalCombinedFactor - 1.0) * 1000).roundToInt() / 10.0

        // 3. Adjusted Price per SqM
        val adjustedPricePerSqM = input.baseZonePricePerSqM * totalCombinedFactor

        // 4. Gross Market Value minus Sanatoria/Irregularities Costs
        val rawMarketValue = commercialSqM * adjustedPricePerSqM
        val totalEstimatedMarketValue = (rawMarketValue - input.estimatedSanatoriaCost).coerceAtLeast(0.0)

        // 5. Value Range (+/- 7.5% peritale confidence interval)
        val minFairValue = totalEstimatedMarketValue * 0.925
        val maxFairValue = totalEstimatedMarketValue * 1.075

        // 6. Recommended Auction/Distressed Purchase Target (20% and 30% Net Investor Margin)
        val recommendedAuctionBid20Margin = totalEstimatedMarketValue * 0.80
        val recommendedAuctionBid30Margin = totalEstimatedMarketValue * 0.70

        // Breakdown for transparent expert report
        val breakdown = listOf(
            "Superficie Commerciale Ragguagliata" to "${(commercialSqM * 10).roundToInt() / 10.0} m²",
            "Stato Conservativo" to "${input.condition.label} (${formatFactor(conditionFactor)})",
            "Piano ed Ascensore" to "${input.floorLevel.label} (${formatFactor(floorFactor)})",
            "Prestazione Energetica" to "${input.energyClass.label} (${formatFactor(energyFactor)})",
            "Stato Occupazionale" to "${input.occupancyStatus.label} (${formatFactor(occupancyFactor)})",
            "Pertinenze & Vista" to "Garage: ${if (input.hasGarageOrParking) "+5%" else "Assente"}, Vista: ${if (input.hasPanoramicView) "+5%" else "Standard"}",
            "Detrazione Difformità / Sanatorie" to if (input.estimatedSanatoriaCost > 0) "-€${input.estimatedSanatoriaCost.toInt()}" else "Nessuna rilevata",
            "Variazione Complessiva Peritale" to "${if (totalCorrectionPercentage >= 0) "+" else ""}$totalCorrectionPercentage%"
        )

        val summary = """
            PROSPETTO DI STIMA PERITALE SENIOR (Metodo MCA)
            ------------------------------------------------
            Superficie Commerciale: ${(commercialSqM * 10).roundToInt() / 10.0} m²
            Valore Unitario Corretto: €${adjustedPricePerSqM.roundToInt()}/m²
            Valore di Mercato Stimato: €${totalEstimatedMarketValue.roundToInt()}
            Forchetta di Valore Appropriato: €${minFairValue.roundToInt()} - €${maxFairValue.roundToInt()}
            
            Target Offerta Consigliata (Sconto 20%): €${recommendedAuctionBid20Margin.roundToInt()}
            Target Offerta Opportunistica (Sconto 30%): €${recommendedAuctionBid30Margin.roundToInt()}
        """.trimIndent()

        return ValuationResult(
            commercialSqM = (commercialSqM * 10).roundToInt() / 10.0,
            baseMarketValueSqM = input.baseZonePricePerSqM,
            adjustedPricePerSqM = (adjustedPricePerSqM * 10).roundToInt() / 10.0,
            totalEstimatedMarketValue = totalEstimatedMarketValue,
            minFairValue = minFairValue,
            maxFairValue = maxFairValue,
            recommendedAuctionBid20Margin = recommendedAuctionBid20Margin,
            recommendedAuctionBid30Margin = recommendedAuctionBid30Margin,
            totalCorrectionPercentage = totalCorrectionPercentage,
            breakdownFactors = breakdown,
            appraisalSummary = summary
        )
    }

    private fun formatFactor(factor: Double): String {
        val pct = ((factor - 1.0) * 100).roundToInt()
        return if (pct >= 0) "+$pct%" else "$pct%"
    }
}
