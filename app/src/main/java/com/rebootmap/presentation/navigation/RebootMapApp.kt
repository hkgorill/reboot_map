package com.rebootmap.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rebootmap.presentation.dashboard.DashboardScreen
import com.rebootmap.presentation.onboarding.OnboardingScreen
import com.rebootmap.presentation.simulation.SimulationViewModel

@Composable
fun RebootMapApp(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        !state.isOnboardingCompleted -> {
            OnboardingScreen(
                onComplete = viewModel::completeOnboarding,
            )
        }
        else -> {
            DashboardScreen(viewModel = viewModel)
        }
    }
}
