package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DealRadarViewModel
import com.example.ui.MainScreen
import com.example.ui.MainTab
import com.example.ui.theme.QuantumDealRadarTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DealRadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            QuantumDealRadarTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val openScreen = intent?.getStringExtra("OPEN_SCREEN") ?: return
        when (openScreen) {
            "RADAR_FEED" -> {
                viewModel.setMainTab(MainTab.RADAR_FEED)
                val dealId = intent.getLongExtra("DEAL_ID", -1L)
                if (dealId > 0) {
                    viewModel.openDealById(dealId)
                }
            }
            "DISTRESSED_PROPERTIES" -> {
                viewModel.setMainTab(MainTab.DISTRESSED)
            }
        }
    }
}

