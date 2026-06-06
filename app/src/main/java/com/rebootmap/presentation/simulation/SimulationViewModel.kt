package com.rebootmap.presentation.simulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rebootmap.data.SimulationDataStoreRepository
import com.rebootmap.data.SimulationRepository
import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.preset.AgeBasedPreset
import com.rebootmap.presentation.components.InvestmentReturnRate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year

class SimulationViewModel(
    application: Application,
    private val engine: CashFlowEngine = CashFlowEngine(),
    private val repository: SimulationRepository = SimulationDataStoreRepository(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var lastPresetAge: Int? = null

    init {
        viewModelScope.launch {
            val saved = repository.load()
            _uiState.update { current ->
                if (saved != null) {
                    SimulationStateMapper.toUiState(saved).copy(isLoading = false)
                } else {
                    SimulationUiState.fromPreset(age = 40).copy(isLoading = false)
                }
            }
            lastPresetAge = _uiState.value.profile.currentAge
            calculate()
        }
    }

    fun completeOnboarding(currentAge: Int, retirementAge: Int, monthlyLivingExpense: Long) {
        val preset = AgeBasedPreset.forAge(currentAge.coerceIn(18, 100))
        _uiState.update {
            it.copy(
                isOnboardingCompleted = true,
                profile = preset.profile.copy(
                    currentAge = currentAge.coerceIn(18, 100),
                    retirementAge = retirementAge.coerceIn(currentAge.coerceIn(18, 100), 100),
                    monthlyLivingExpense = monthlyLivingExpense,
                ),
                assumptions = preset.assumptions,
                assets = preset.assets,
                presetSourceNote = preset.sourceNote,
            )
        }
        lastPresetAge = currentAge.coerceIn(18, 100)
        calculate()
        persistState()
    }

    fun updateCurrentAge(age: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(currentAge = age)) }
        if (age in 18..100) {
            calculate()
        }
    }

    /** 나이 입력 완료(포커스 해제) 시 연령대별 평균 프리셋 적용 */
    fun commitCurrentAge(age: Int) {
        if (age !in 18..100) return
        if (lastPresetAge == age) return
        lastPresetAge = age
        applyPreset(AgeBasedPreset.forAge(age))
    }

    fun applyPreset(preset: AgeBasedPreset) {
        lastPresetAge = preset.profile.currentAge
        _uiState.update {
            it.copy(
                profile = preset.profile,
                assumptions = preset.assumptions,
                assets = preset.assets,
                presetSourceNote = preset.sourceNote,
            )
        }
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

    fun updateInvestmentReturnRate(rate: Double) {
        val index = _uiState.value.assets.indexOfFirst { it is Asset.Investment }
        if (index < 0) return
        val investment = _uiState.value.assets[index] as Asset.Investment
        updateAsset(index, investment.copy(annualReturnRate = InvestmentReturnRate.snap(rate)))
    }

    fun toggleAssetExpanded(assetId: String) {
        _uiState.update { state ->
            val expanded = state.expandedAssetIds.toMutableSet()
            if (assetId in expanded) expanded.remove(assetId) else expanded.add(assetId)
            state.copy(expandedAssetIds = expanded)
        }
    }

    fun toggleBasicInfoExpanded() {
        _uiState.update { it.copy(isBasicInfoExpanded = !it.isBasicInfoExpanded) }
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
        if (state.isOnboardingCompleted) {
            persistState()
        }
    }

    private fun persistState() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            repository.save(SimulationStateMapper.toPersisted(_uiState.value))
        }
    }

    private fun Asset.hasValue(): Boolean = when (this) {
        is Asset.RealEstate -> currentValue > 0 || debtAmount > 0
        is Asset.NationalPension -> monthlyPayout > 0
        is Asset.SeverancePension -> balance > 0 || monthlyContribution > 0
        is Asset.PersonalPension -> balance > 0 || monthlyContribution > 0
        is Asset.YellowUmbrella -> balance > 0 || monthlyContribution > 0
        is Asset.Investment -> currentValue > 0
        is Asset.CashSavings -> maturityAmount > 0
        is Asset.FixedIncome -> monthlyAmount > 0
    }
}
