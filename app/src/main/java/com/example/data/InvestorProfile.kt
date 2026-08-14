package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investor_profiles")
data class InvestorProfile(
    @PrimaryKey val id: Long = 1L,
    val fullName: String = "Marco Rossi",
    val companyName: String = "Quantum Capital RE Srl",
    val email: String = "m.rossi@quantumcapital.it",
    val phone: String = "+39 347 8899000",
    val investorTier: String = "Family Office / Fix & Flip",
    val availableCapital: Double = 750000.0,
    val isRegistered: Boolean = true,
    val registeredAt: String = "10/08/2026",
    
    // Brief di Ricerca Investitore
    val briefActive: Boolean = true,
    val briefTargetLocations: String = "Milano, Roma, Bologna, Torino, Firenze",
    val briefPropertyTypes: String = "Residenziale, Commerciale, Asta, NPL",
    val briefMaxBudget: Double = 350000.0,
    val briefMinDiscountPercent: Int = 30,
    val briefMinTargetRoiPercent: Double = 15.0,
    val briefStrategy: String = "Trading / Flip Rapido",
    val briefMaxRenovationCost: Double = 40000.0,
    val briefAlertsEnabled: Boolean = true
)
