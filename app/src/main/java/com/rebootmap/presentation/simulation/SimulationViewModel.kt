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
import com.rebootmap.domain.scenario.RelocationPlan
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
    private var calculateJob: Job? = null
    private var calculationGeneration = 0
    private var lastPresetAge: Int? = null

    init {
        viewModelScope.launch {
            val saved = repository.load()
            _uiState.update { current ->
                if (saved != null) {
                    SimulationStateMapper.toUiState(saved).copy(isLoading = false)
                } else {
                    SimulationUiState.onboarding().copy(isLoading = false)
                }
            }
            lastPresetAge = _uiState.value.profile.currentAge
            calculate()
        }
    }

    fun completeOnboarding(currentAge: Int, retirementAge: Int, monthlyLivingExpense: Long) {
        val safeAge = currentAge.coerceIn(18, 100)
        saveJob?.cancel()
        calculationGeneration++
        _uiState.value = SimulationUiState.afterOnboarding(
            currentAge = safeAge,
            retirementAge = retirementAge.coerceIn(safeAge, 100),
            monthlyLivingExpense = monthlyLivingExpense.coerceAtLeast(0L),
        ).copy(isLoading = false)
        lastPresetAge = safeAge
        calculate()
        persistState()
    }

    fun updateCurrentAge(age: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(currentAge = age)) }
        if (age in 18..100) {
            calculate()
        }
    }

    /** 나이 입력 완료(포커스 해제) 시 연령대별 참고값만 갱신 */
    fun commitCurrentAge(age: Int) {
        if (age !in 18..100) return
        val preset = AgeBasedPreset.forAge(age)
        lastPresetAge = age
        _uiState.update {
            it.copy(
                referencePreset = preset,
                presetSourceNote = preset.sourceNote,
                profile = it.profile.copy(currentAge = age),
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

    fun toggleRelocationExpanded() {
        _uiState.update { it.copy(isRelocationExpanded = !it.isRelocationExpanded) }
    }

    fun updateRelocationPlan(plan: RelocationPlan) {
        _uiState.update { it.copy(relocationPlan = plan) }
        calculate()
    }

    fun resetAllInputs() {
        saveJob?.cancel()
        calculateJob?.cancel()
        calculationGeneration++
        lastPresetAge = null
        _uiState.value = SimulationUiState.onboarding()
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun calculate() {
        calculateJob?.cancel()
        val generation = ++calculationGeneration
        calculateJob = viewModelScope.launch {
            val state = _uiState.value
            if (!state.isOnboardingCompleted) return@launch

            _uiState.update { it.copy(isCalculating = true) }

            val activeAssets = state.assets.filter { it.hasValue() }
            val preset = state.referencePreset
                ?: AgeBasedPreset.forAge(state.profile.currentAge)
            val effectiveProfile = state.profile.copy(
                lifeExpectancy = state.profile.lifeExpectancy
                    .takeIf { it > state.profile.currentAge }
                    ?: preset.profile.lifeExpectancy,
            )
            val effectiveAssumptions = state.assumptions.copy(
                inflationRate = state.assumptions.inflationRate
                    .takeIf { it > 0.0 }
                    ?: preset.assumptions.inflationRate,
            )
            val baseInput = SimulationInput(
                profile = effectiveProfile,
                assumptions = effectiveAssumptions,
                assets = activeAssets,
                startYear = Year.now().value,
            )

            val relocationPlan = state.relocationPlan.takeIf { it.isConfigured() }
            val projection = engine.project(
                baseInput.copy(relocationPlan = relocationPlan),
            )
            val baselineProjection = if (relocationPlan != null) {
                engine.project(baseInput)
            } else {
                null
            }

            if (generation != calculationGeneration) return@launch

            _uiState.update {
                it.copy(
                    projection = projection,
                    baselineProjection = baselineProjection,
                    isCalculating = false,
                )
            }
            if (_uiState.value.isOnboardingCompleted) {
                persistState()
            }
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
        is Asset.NationalPension -> monthlyPayout > 0 && startAge > 0
        is Asset.SeverancePension -> balance > 0 || monthlyContribution > 0
        is Asset.PersonalPension -> balance > 0 || monthlyContribution > 0
        is Asset.YellowUmbrella -> balance > 0 || monthlyContribution > 0
        is Asset.Investment -> currentValue > 0
        is Asset.CashSavings -> maturityAmount > 0 && maturityYear > 0
        is Asset.FixedIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
        is Asset.HousingPension -> enabled && startAge > 0
    }
}
