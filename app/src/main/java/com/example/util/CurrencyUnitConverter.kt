package com.example.util

import java.text.NumberFormat
import java.util.Locale

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val rateFromEur: Double,
    val displayName: String
) {
    EUR("EUR", "€", 1.0, "Euro (€)"),
    USD("USD", "$", 1.09, "US Dollar ($)"),
    GBP("GBP", "£", 0.86, "British Pound (£)"),
    CHF("CHF", "Fr.", 0.96, "Swiss Franc (CHF)")
}

enum class AreaUnit(
    val code: String,
    val symbol: String,
    val factorFromSqm: Double, // 1 m² = 1.0 m², 1 m² = 10.7639 sq ft
    val displayName: String
) {
    SQ_METERS("SQM", "m²", 1.0, "Square Meters (m²)"),
    SQ_FEET("SQFT", "sq ft", 10.7639, "Square Feet (sq ft)")
}

data class ConvertedPropertyMetrics(
    val currency: AppCurrency,
    val unit: AreaUnit,
    val surfaceInSqm: Double,
    val surfaceInUnit: Double,
    val basePriceEur: Double,
    val baseArvEur: Double,
    val convertedPrice: Double,
    val convertedArv: Double,
    val convertedRenovationCost: Double,
    val convertedNetProfit: Double,
    val pricePerUnit: Double,
    val arvPerUnit: Double,
    val profitPerUnit: Double,
    val roiPercent: Double
) {
    fun formatCurrencyValue(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 0
        return "${currency.symbol}${formatter.format(amount.toInt())}"
    }

    fun formatUnitPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 1
        return "${currency.symbol}${formatter.format(amount.toInt())}/${unit.symbol}"
    }
}

object CurrencyUnitConverter {

    /**
     * Converts property values given base EUR price, estimated ARV, surface area in SQM, target currency and area unit.
     */
    fun computeMetrics(
        priceEur: Double,
        arvEur: Double?,
        surfaceSqm: Double = 100.0,
        currency: AppCurrency = AppCurrency.EUR,
        unit: AreaUnit = AreaUnit.SQ_METERS
    ): ConvertedPropertyMetrics {
        val safePrice = priceEur.coerceAtLeast(0.0)
        val calculatedArv = arvEur ?: if (safePrice > 0) safePrice * 1.35 else 0.0
        val safeSurfaceSqm = surfaceSqm.coerceAtLeast(10.0)

        val renoCostEur = safePrice * 0.15
        val netProfitEur = calculatedArv - safePrice - renoCostEur
        val roi = if (safePrice + renoCostEur > 0) (netProfitEur / (safePrice + renoCostEur)) * 100.0 else 0.0

        val rate = currency.rateFromEur
        val convertedPrice = safePrice * rate
        val convertedArv = calculatedArv * rate
        val convertedReno = renoCostEur * rate
        val convertedProfit = netProfitEur * rate

        val surfaceInUnit = safeSurfaceSqm * unit.factorFromSqm

        val pricePerUnit = convertedPrice / surfaceInUnit
        val arvPerUnit = convertedArv / surfaceInUnit
        val profitPerUnit = convertedProfit / surfaceInUnit

        return ConvertedPropertyMetrics(
            currency = currency,
            unit = unit,
            surfaceInSqm = safeSurfaceSqm,
            surfaceInUnit = surfaceInUnit,
            basePriceEur = safePrice,
            baseArvEur = calculatedArv,
            convertedPrice = convertedPrice,
            convertedArv = convertedArv,
            convertedRenovationCost = convertedReno,
            convertedNetProfit = convertedProfit,
            pricePerUnit = pricePerUnit,
            arvPerUnit = arvPerUnit,
            profitPerUnit = profitPerUnit,
            roiPercent = roi
        )
    }
}
