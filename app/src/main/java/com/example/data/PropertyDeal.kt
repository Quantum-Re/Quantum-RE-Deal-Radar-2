package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "property_deals")
data class PropertyDeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceKey: String, // e.g. "quimmo", "iqera", "re_impresa", "bper_leasing", "demanio", "astalegale"
    val sourceName: String,
    val sourceUrl: String,
    val location: String, // e.g. "Milano (MI)"
    val propertyType: String, // "Residenziale", "Commerciale", "Industriale", "Asta Giudiziaria"
    val askingPrice: Double, // Current asking price or auction base price in €
    val estimatedMarketValue: Double, // Market valuation in €
    val surfaceSqm: Int, // m²
    val discountPercent: Int, // e.g. 35 -> 35% below market
    val estimatedCapRate: Double, // e.g. 8.4% gross yield
    val auctionDate: String? = null, // e.g. "15/09/2026"
    val status: String = "LIVE", // "LIVE", "PRICE_CUT", "AUCTION_PENDING", "BOOKMARKED"
    val imageUrl: String = "",
    val notes: String = "",
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
