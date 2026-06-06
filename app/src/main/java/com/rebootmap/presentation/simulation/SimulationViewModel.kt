package com.rebootmap.presentation.simulation

import androidx.lifecycle.ViewModel
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Year

class SimulationViewModel(
    private val engine: CashFlowEngine = CashFlowEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    init {
        calculate()
    }

    fun updateProfile(profile: UserProfile) {
        _uiState.update { it.copy(profile = profile) }
        calculate()
    }

    fun updateAssumptions(assumptions: EconomicAssumptions) {
        _uiState.update { it.copy(assumptions = assumptions) }
        calculate()
    }

    fun updateAsset(index: Int, asset: Asset) {
        _uiState.update { state ->
            val updated = state.assets.toMutableList()
            if (index in updated.indices) {
                updated[index] = asset
            }
            state.copy(assets = updated)
        }
        calculate()
    }

    fun calculate() {
        val state = _uiState.value
        _uiState.update { it.copy(isCalculating = true) }

        val activeAssets = state.assets.filter { it.hasValue() }
        val projection = engine.project(
            SimulationInput(
                profile = state.profile,
                assumptions = state.assumptions,
                assets = activeAssets,
                startYear = Year.now().value,
            ),
        )

        _uiState.update {
            it.copy(projection = projection, isCalculating = false)
        }
    }

    private fun Asset.hasValue(): Boolean = when (this) {
        is Asset.RealEstate -> currentValue > 0
        is Asset.NationalPension -> monthlyPayout > 0
        is Asset.RetirementPension -> balance > 0 || monthlyContribution > 0
        is Asset.Investment -> currentValue > 0
        is Asset.CashSavings -> maturityAmount > 0
    }
}
