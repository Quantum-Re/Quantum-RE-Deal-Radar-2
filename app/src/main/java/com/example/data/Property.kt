package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val price: Double,
    val distressStatus: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val title: String = "",
    val propertyType: String = "Residenziale",
    val estimatedMarketValue: Double = price,
    val surfaceSqm: Int = 0,
    val notes: String = "",
    val photoUri: String? = null,
    val strategyTags: String = "Fix & Flip",
    val createdAt: Long = System.currentTimeMillis()
)
