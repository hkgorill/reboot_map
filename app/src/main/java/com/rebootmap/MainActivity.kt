package com.rebootmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rebootmap.presentation.simulation.SimulationScreen
import com.rebootmap.presentation.simulation.SimulationViewModel
import com.rebootmap.presentation.theme.RebootMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RebootMapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SimulationScreen(viewModel = viewModel<SimulationViewModel>())
                }
            }
        }
    }
}
