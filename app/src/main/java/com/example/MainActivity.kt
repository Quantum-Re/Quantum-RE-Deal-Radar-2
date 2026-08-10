package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DealRadarViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.QuantumDealRadarTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DealRadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuantumDealRadarTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
