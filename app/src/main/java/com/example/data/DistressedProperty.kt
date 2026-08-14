package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distressed_properties")
data class DistressedProperty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val price: Double = 0.0,
    val estimatedValue: Double = 0.0,
    val distressLevel: String = "Medium",
    val status: String = "Active",
    val category: String = "Residential",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val notes: String = "",
    val estimatedArv: Double? = null,
    val aiAnalysisReport: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
