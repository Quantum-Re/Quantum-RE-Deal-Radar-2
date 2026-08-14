package com.example

import com.example.ui.components.RoiCalculationData
import com.example.ui.components.YieldGrade
import org.junit.Assert.*
import org.junit.Test

class RoiCalculatorTest {

    @Test
    fun testGrossRentalYieldCalculation() {
        // Purchase: 200,000, Renovation: 20,000, Legal: 5,000 -> Total: 225,000
        // Rent: 1,500/month -> Annual: 18,000
        // Gross Yield = 18,000 / 225,000 = 8.0%
        val data = RoiCalculationData(
            purchasePrice = 200000.0,
            renovationCost = 20000.0,
            legalFees = 5000.0,
            estimatedMonthlyRent = 1500.0
        )

        assertEquals(225000.0, data.totalProjectCost, 0.01)
        assertEquals(18000.0, data.annualGrossRent, 0.01)
        assertEquals(8.0, data.grossYieldPercent, 0.01)
    }

    @Test
    fun testCashOnCashReturnCalculation() {
        // Purchase: 100,000, Renovation: 10,000, Legal: 5,000
        // 20% down payment = 20,000
        // Initial Cash = 20,000 + 10,000 + 5,000 = 35,000
        // Monthly Rent = 1,000 (12,000/yr), Expenses = 100 (1,200/yr) -> NOI = 10,800
        // 100% cash test (mortgageRate = 0, downPayment = 100%) -> Annual Cash Flow = 10,800
        val allCashData = RoiCalculationData(
            purchasePrice = 100000.0,
            renovationCost = 10000.0,
            legalFees = 5000.0,
            estimatedMonthlyRent = 1000.0,
            monthlyExpenses = 100.0,
            downPaymentPercent = 100.0,
            mortgageRatePercent = 0.0
        )

        assertEquals(115000.0, allCashData.totalProjectCost, 0.01)
        assertEquals(115000.0, allCashData.initialCashRequired, 0.01)
        assertEquals(10800.0, allCashData.netOperatingIncome, 0.01)
        assertEquals(10800.0, allCashData.annualNetCashFlow, 0.01)
        // CoC = 10,800 / 115,000 = 9.39%
        assertEquals(9.39, allCashData.cashOnCashReturnPercent, 0.05)
    }

    @Test
    fun testFixAndFlipRoi() {
        val data = RoiCalculationData(
            purchasePrice = 150000.0,
            renovationCost = 30000.0,
            legalFees = 10000.0,
            expectedResalePrice = 240000.0
        )
        // Total cost = 190,000
        // Profit = 240,000 - 190,000 = 50,000
        // ROI = (50,000 / 190,000) * 100 = 26.31%
        assertEquals(190000.0, data.totalProjectCost, 0.01)
        assertEquals(50000.0, data.totalFlipProfit, 0.01)
        assertEquals(26.31, data.flipRoiPercent, 0.05)
    }

    @Test
    fun testYieldGradeClassification() {
        val excellentData = RoiCalculationData(
            purchasePrice = 100000.0,
            renovationCost = 10000.0,
            legalFees = 2000.0,
            estimatedMonthlyRent = 1500.0,
            downPaymentPercent = 20.0
        )
        assertEquals(YieldGrade.EXCELLENT, excellentData.yieldRating)
    }
}
