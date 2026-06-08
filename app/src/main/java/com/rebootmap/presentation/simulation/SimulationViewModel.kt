package com.rebootmap.presentation.simulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rebootmap.data.SimulationDataStoreRepository
import com.rebootmap.data.SimulationRepository
import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.matching.AssetMatchingEngine
import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.PersonalLoanDefaults
import com.rebootmap.domain.model.RealEstateDefaults
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.preset.AgeBasedPreset
import com.rebootmap.domain.portfolio.RealEstateTimingAdvisoryEngine
import com.rebootmap.presentation.components.InvestmentReturnRate
import com.rebootmap.presentation.dashboard.DashboardGroupId
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
        ).copy(
            isLoading = false,
            expandedDashboardGroups = setOf(
                DashboardGroupId.RESULTS,
                DashboardGroupId.BASIC_INFO,
            ),
        )
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

    fun updateAssetById(id: String, asset: Asset) {
        _uiState.update { state ->
            state.copy(assets = state.assets.map { if (it.id == id) asset else it })
        }
        calculate()
    }

    fun addRealEstate() {
        val estates = _uiState.value.assets.filterIsInstance<Asset.RealEstate>()
        if (estates.size >= RealEstateDefaults.MAX_COUNT) return
        val newId = RealEstateDefaults.nextId(estates) ?: return
        val newEstate = RealEstateDefaults.empty(newId)
        _uiState.update { state ->
            val updated = state.assets.toMutableList()
            val lastIndex = updated.indexOfLast { it is Asset.RealEstate }
            val insertAt = if (lastIndex >= 0) lastIndex + 1 else 0
            updated.add(insertAt, newEstate)
            state.copy(
                assets = updated,
                expandedAssetIds = state.expandedAssetIds + newId,
                expandedDashboardGroups = state.expandedDashboardGroups + DashboardGroupId.REAL_ESTATE,
            )
        }
        calculate()
    }

    fun addPersonalLoan() {
        val loans = _uiState.value.personalLoans
        if (loans.size >= PersonalLoanDefaults.MAX_COUNT) return
        val newId = PersonalLoanDefaults.nextId(loans) ?: return
        _uiState.update { state ->
            state.copy(
                personalLoans = state.personalLoans + PersonalLoanDefaults.empty(newId),
                expandedLoanIds = state.expandedLoanIds + newId,
                expandedDashboardGroups = state.expandedDashboardGroups + DashboardGroupId.DEBT,
            )
        }
        calculate()
    }

    fun removePersonalLoan(id: String) {
        _uiState.update { state ->
            state.copy(
                personalLoans = state.personalLoans.filter { it.id != id },
                expandedLoanIds = state.expandedLoanIds - id,
            )
        }
        calculate()
    }

    fun updatePersonalLoan(loan: PersonalLoan) {
        _uiState.update { state ->
            state.copy(
                personalLoans = state.personalLoans.map { if (it.id == loan.id) loan else it },
            )
        }
        calculate()
    }

    fun toggleLoanExpanded(loanId: String) {
        _uiState.update { state ->
            val expanded = state.expandedLoanIds.toMutableSet()
            if (loanId in expanded) expanded.remove(loanId) else expanded.add(loanId)
            state.copy(expandedLoanIds = expanded)
        }
    }

    fun removeRealEstate(id: String) {
        _uiState.update { state ->
            val remainingEstates = state.assets
                .filterIsInstance<Asset.RealEstate>()
                .filter { it.id != id }
            val updated = state.assets.filter { it.id != id || it !is Asset.RealEstate }
            val finalAssets = if (remainingEstates.isEmpty()) {
                updated.toMutableList().apply {
                    add(0, RealEstateDefaults.empty())
                }
            } else {
                updated
            }
            state.copy(
                assets = finalAssets,
                expandedAssetIds = state.expandedAssetIds - id,
            )
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

    fun toggleDashboardGroup(group: DashboardGroupId) {
        _uiState.update { state ->
            val expanded = state.expandedDashboardGroups.toMutableSet()
            if (group in expanded) expanded.remove(group) else expanded.add(group)
            state.copy(expandedDashboardGroups = expanded)
        }
    }

    fun toggleMilestoneExpanded() {
        _uiState.update { it.copy(isMilestoneExpanded = !it.isMilestoneExpanded) }
    }

    fun addLumpSumExpense(expense: LumpSumExpense) {
        _uiState.update { it.copy(lumpSumExpenses = it.lumpSumExpenses + expense) }
        calculate()
    }

    fun updateLumpSumExpense(expense: LumpSumExpense) {
        _uiState.update { state ->
            state.copy(
                lumpSumExpenses = state.lumpSumExpenses.map {
                    if (it.id == expense.id) expense else it
                },
            )
        }
        calculate()
    }

    fun removeLumpSumExpense(expenseId: String) {
        _uiState.update { state ->
            state.copy(lumpSumExpenses = state.lumpSumExpenses.filter { it.id != expenseId })
        }
        calculate()
    }

    fun toggleTimingConsultExpanded() {
        _uiState.update { it.copy(isTimingConsultExpanded = !it.isTimingConsultExpanded) }
    }

    fun updateEstateSaleYear(estateId: String, saleYear: Int) {
        _uiState.update { state ->
            state.copy(
                assets = state.assets.map { asset ->
                    if (asset is Asset.RealEstate && asset.id == estateId) {
                        asset.copy(
                            saleYear = saleYear,
                            expectedSalePrice = asset.expectedSalePrice.takeIf { it > 0 }
                                ?: asset.currentValue,
                        )
                    } else {
                        asset
                    }
                },
            )
        }
        calculate()
    }

    fun applyTimingSuggestions() {
        val estates = _uiState.value.assets.filterIsInstance<Asset.RealEstate>()
        val report = RealEstateTimingAdvisoryEngine.evaluate(estates, Year.now().value)
        if (report.suggestedSaleYears.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                assets = state.assets.map { asset ->
                    if (asset is Asset.RealEstate) {
                        report.suggestedSaleYears[asset.id]?.let { year ->
                            asset.copy(saleYear = year)
                        } ?: asset
                    } else {
                        asset
                    }
                },
            )
        }
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
            val startYear = Year.now().value
            val activeLoans = state.personalLoans.filter { it.isSimulationReady() }
            val baseInput = SimulationInput(
                profile = effectiveProfile,
                assumptions = effectiveAssumptions,
                assets = activeAssets,
                startYear = startYear,
                lumpSumExpenses = state.lumpSumExpenses,
                personalLoans = activeLoans,
            )

            val realEstates = state.assets.filterIsInstance<Asset.RealEstate>()
            val timingReport = RealEstateTimingAdvisoryEngine.evaluate(realEstates, startYear)
            val projection = engine.project(baseInput)
            val baselineProjection = if (timingReport.suggestedSaleYears.isNotEmpty()) {
                val altAssets = state.assets.map { asset ->
                    if (asset is Asset.RealEstate) {
                        timingReport.suggestedSaleYears[asset.id]?.let { year ->
                            asset.copy(saleYear = year)
                        } ?: asset
                    } else {
                        asset
                    }
                }.filter { it.hasValue() }
                engine.project(baseInput.copy(assets = altAssets))
            } else {
                null
            }

            val expenseMatches = state.lumpSumExpenses.associate { expense ->
                expense.id to AssetMatchingEngine.recommend(
                    expense = expense,
                    assets = activeAssets,
                    startYear = startYear,
                    currentAge = effectiveProfile.currentAge,
                )
            }

            if (generation != calculationGeneration) return@launch

            _uiState.update {
                it.copy(
                    projection = projection,
                    baselineProjection = baselineProjection,
                    expenseMatches = expenseMatches,
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
        is Asset.EmploymentIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
        is Asset.BusinessIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
        is Asset.OtherFixedIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
        is Asset.HousingPension -> enabled && startAge > 0
    }
}
