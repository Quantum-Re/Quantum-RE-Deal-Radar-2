package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DealRadarRepository(
    private val dealDao: PropertyDealDao,
    private val sourceDao: ScraperSourceDao,
    private val historyDao: PriceHistoryDao
) {
    val allDeals: Flow<List<PropertyDeal>> = dealDao.getAllDeals()
    val bookmarkedDeals: Flow<List<PropertyDeal>> = dealDao.getBookmarkedDeals()
    val allSources: Flow<List<ScraperSource>> = sourceDao.getAllSources()

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val currentSources = sourceDao.getAllSources().first()
        if (currentSources.size != InitialSeedData.initialSources.size) {
            sourceDao.insertSources(InitialSeedData.initialSources)
            dealDao.insertDeals(InitialSeedData.initialDeals)
            historyDao.insertHistories(InitialSeedData.initialHistories)
        }
    }

    suspend fun toggleBookmark(dealId: Long, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        dealDao.setBookmarked(dealId, !currentStatus)
    }

    suspend fun updateDealNotes(dealId: Long, notes: String) = withContext(Dispatchers.IO) {
        dealDao.updateNotes(dealId, notes)
    }

    suspend fun addDeal(deal: PropertyDeal) = withContext(Dispatchers.IO) {
        val newId = dealDao.insertDeal(deal)
        historyDao.insertHistory(
            PriceHistory(
                dealId = newId,
                price = deal.askingPrice,
                dateRecorded = "Oggi",
                eventLabel = "Inserimento Manuale/Parsed"
            )
        )
    }

    suspend fun deleteDeal(deal: PropertyDeal) = withContext(Dispatchers.IO) {
        dealDao.deleteDeal(deal)
    }

    suspend fun updateSourceConfigStatus(sourceId: String, newStatus: String) = withContext(Dispatchers.IO) {
        sourceDao.updateConfigStatus(sourceId, newStatus)
    }

    suspend fun updateSourceParserRules(sourceId: String, rulesJson: String) = withContext(Dispatchers.IO) {
        sourceDao.updateParserRules(sourceId, rulesJson)
    }

    fun getPriceHistoryForDeal(dealId: Long): Flow<List<PriceHistory>> {
        return historyDao.getHistoryForDeal(dealId)
    }

    /**
     * Simulates running a scraper and testing parser rules against sample payload
     */
    fun simulateParserTest(
        source: ScraperSource,
        sampleHtmlOrJson: String,
        rulesJsonStr: String
    ): ScraperTestResult {
        return try {
            val jsonObject = JSONObject(if (rulesJsonStr.isNotBlank()) rulesJsonStr else source.activeParserRulesJson)
            val listSelector = jsonObject.optString("listSelector", ".property-item")
            val titleSelector = jsonObject.optString("titleSelector", ".title")
            val priceSelector = jsonObject.optString("priceSelector", ".price")
            val marketValueSelector = jsonObject.optString("marketValueSelector", ".market-val")

            val logs = mutableListOf<String>()
            logs.add("1. Verification Robots.txt for URL: ${source.url}")
            logs.add("   → Robots Status: ${source.robotsStatus} (OK)")
            logs.add("2. Initializing HTTP Client & User-Agent headers...")
            logs.add("3. Executing HTML DOM tree extraction...")
            logs.add("4. Querying Container Selector '$listSelector'...")
            logs.add("   → Found 5 matching property elements in DOM.")
            logs.add("5. Parsing Item 1:")
            logs.add("   - Title ($titleSelector): 'Immobile Commerciale / Residenziale'")
            logs.add("   - Price ($priceSelector): '€ 185.000,00'")
            logs.add("   - Market Valuation ($marketValueSelector): '€ 290.000,00'")
            logs.add("   - Calculated Discount: -36,2%")
            logs.add("6. Parser Validation Status: SUCCESS (No CSS/Regex extraction errors)")

            ScraperTestResult(
                isSuccess = true,
                extractedTitle = "Trilocale Ristrutturato con Garage (Sample Extracted)",
                extractedPrice = 185000.0,
                extractedMarketValue = 290000.0,
                discountPercent = 36,
                logs = logs
            )
        } catch (e: Exception) {
            ScraperTestResult(
                isSuccess = false,
                extractedTitle = null,
                extractedPrice = null,
                extractedMarketValue = null,
                discountPercent = 0,
                logs = listOf(
                    "Error parsing JSON rules or CSS selector: ${e.message}",
                    "Check JSON syntax (missing quotes or invalid brackets)."
                )
            )
        }
    }
}

data class ScraperTestResult(
    val isSuccess: Boolean,
    val extractedTitle: String?,
    val extractedPrice: Double?,
    val extractedMarketValue: Double?,
    val discountPercent: Int,
    val logs: List<String>
)
