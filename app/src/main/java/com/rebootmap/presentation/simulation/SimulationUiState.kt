package com.rebootmap.presentation.simulation

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.matching.AssetSuggestion
import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.preset.AgeBasedPreset
import com.rebootmap.domain.scenario.RelocationPlan
import com.rebootmap.presentation.dashboard.DashboardGroupId

data class SimulationUiState(
    val isOnboardingCompleted: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val assumptions: EconomicAssumptions = EconomicAssumptions(),
    val assets: List<Asset> = emptyList(),
    val personalLoans: List<PersonalLoan> = emptyList(),
    val relocationPlan: RelocationPlan = RelocationPlan(),
    val lumpSumExpenses: List<LumpSumExpense> = emptyList(),
    val expenseMatches: Map<String, List<AssetSuggestion>> = emptyMap(),
    val projection: CashFlowProjection? = null,
    val baselineProjection: CashFlowProjection? = null,
    val isCalculating: Boolean = false,
    val presetSourceNote: String = "",
    val referencePreset: AgeBasedPreset? = null,
    val expandedAssetIds: Set<String> = emptySet(),
    val expandedLoanIds: Set<String> = emptySet(),
    val expandedDashboardGroups: Set<DashboardGroupId> = setOf(DashboardGroupId.RESULTS),
    val isTimingConsultExpanded: Boolean = false,
    val isLoading: Boolean = true,
) {
    val investmentAsset: Asset.Investment?
        get() = assets.filterIsInstance<Asset.Investment>().firstOrNull()

    val housingPensionAsset: Asset.HousingPension?
        get() = assets.filterIsInstance<Asset.HousingPension>().firstOrNull()

    val showComparison: Boolean
        get() {
            val estates = assets.filterIsInstance<Asset.RealEstate>()
            val configured = estates.count { it.currentValue > 0 || it.debtAmount > 0 }
            return configured >= 1 && baselineProjection != null
        }

    companion object {
        fun afterOnboarding(
            currentAge: Int,
            retirementAge: Int,
            monthlyLivingExpense: Long,
        ): SimulationUiState {
            val preset = AgeBasedPreset.forAge(currentAge)
            return SimulationUiState(
                isOnboardingCompleted = true,
                profile = UserProfile(
                    currentAge = currentAge,
                    retirementAge = retirementAge,
                    lifeExpectancy = 0,
                    currentMonthlyLivingExpense = preset.profile.monthlyLivingExpense,
                    monthlyLivingExpense = monthlyLivingExpense,
                ),
                assumptions = EconomicAssumptions(inflationRate = 0.0),
                assets = com.rebootmap.data.mapper.SimulationStateMapper.emptyAssets(),
                referencePreset = preset,
                presetSourceNote = preset.sourceNote,
            )
        }

        fun onboarding(): SimulationUiState = SimulationUiState(
            isOnboardingCompleted = false,
            isLoading = false,
        )
    }
}
