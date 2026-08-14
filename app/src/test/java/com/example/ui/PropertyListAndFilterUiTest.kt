package com.example.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.DistressedProperty
import com.example.data.PropertyDeal
import com.example.ui.components.DealCard
import com.example.ui.screens.DistressedPropertyCard
import com.example.ui.theme.QuantumDealRadarTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PropertyListAndFilterUiTest {


    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDeals = listOf(
        PropertyDeal(
            id = 101L,
            title = "Attico Navigli Milano",
            sourceKey = "pvp",
            sourceName = "PVP",
            sourceUrl = "https://example.com/101",
            location = "Milano, Navigli",
            propertyType = "Residenziale",
            askingPrice = 250000.0,
            estimatedMarketValue = 400000.0,
            surfaceSqm = 120,
            discountPercent = 38,
            estimatedCapRate = 7.5,
            auctionDate = "15/10/2026",
            isBookmarked = true
        ),
        PropertyDeal(
            id = 102L,
            title = "Villa San Dona Venezia",
            sourceKey = "quimmo",
            sourceName = "Quimmo",
            sourceUrl = "https://example.com/102",
            location = "Venezia, Mestre",
            propertyType = "Residenziale",
            askingPrice = 520000.0,
            estimatedMarketValue = 650000.0,
            surfaceSqm = 250,
            discountPercent = 20,
            estimatedCapRate = 5.2,
            auctionDate = "20/11/2026",
            isBookmarked = false
        )
    )

    private val testDistressed = listOf(
        DistressedProperty(
            id = 10L,
            address = "Via Roma 45, Milano",
            price = 120000.0,
            estimatedValue = 200000.0,
            distressLevel = "HIGH",
            status = "ACTIVE"
        ),
        DistressedProperty(
            id = 20L,
            address = "Corso Francia 100, Torino",
            price = 340000.0,
            estimatedValue = 480000.0,
            distressLevel = "CRITICAL",
            status = "ACTIVE"
        )
    )

    @Test
    fun testDealCard_RendersCorrectlyAndCapturesScreenshot() {
        composeTestRule.setContent {
            QuantumDealRadarTheme {
                DealCard(
                    deal = testDeals[0],
                    onCardClick = {},
                    onBookmarkToggle = {},
                    onCalculateClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Attico Navigli Milano", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Milano, Navigli", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bookmark_button_101").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/deal_card.png")
    }

    @Test
    fun testDistressedPropertyCard_RendersCorrectlyAndCapturesScreenshot() {
        composeTestRule.setContent {
            QuantumDealRadarTheme {
                DistressedPropertyCard(
                    property = testDistressed[0],
                    onClick = {},
                    onDelete = {},
                    modifier = Modifier.testTag("card_distressed_property_10")
                )
            }
        }

        composeTestRule.onNodeWithText("Via Roma 45, Milano", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_distressed_property_10").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/distressed_property_card.png")
    }

    @Test
    fun testFilteringLogic_AppliesDiscountsAndSearchCorrectly() {
        val query = "Milano"
        val filteredBySearch = testDeals.filter { it.title.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true) }
        org.junit.Assert.assertEquals(1, filteredBySearch.size)
        org.junit.Assert.assertEquals("Attico Navigli Milano", filteredBySearch[0].title)

        val highDiscountDeals = testDeals.filter { it.discountPercent >= 35 }
        org.junit.Assert.assertEquals(1, highDiscountDeals.size)
        org.junit.Assert.assertEquals(101L, highDiscountDeals[0].id)

        val activeDistressed = testDistressed.filter { it.distressLevel == "HIGH" }
        org.junit.Assert.assertEquals(1, activeDistressed.size)
        org.junit.Assert.assertEquals("Via Roma 45, Milano", activeDistressed[0].address)
    }
}

