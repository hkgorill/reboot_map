package com.rebootmap.presentation.simulation

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.matching.AssetSuggestion
import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.preset.AgeBasedPreset
import com.rebootmap.domain.scenario.RelocationPlan

data class SimulationUiState(
    val isOnboardingCompleted: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val assumptions: EconomicAssumptions = EconomicAssumptions(),
    val assets: List<Asset> = emptyList(),
    val relocationPlan: RelocationPlan = RelocationPlan(),
    val lumpSumExpenses: List<LumpSumExpense> = emptyList(),
    val expenseMatches: Map<String, List<AssetSuggestion>> = emptyMap(),
    val projection: CashFlowProjection? = null,
    val baselineProjection: CashFlowProjection? = null,
    val isCalculating: Boolean = false,
    val presetSourceNote: String = "",
    val referencePreset: AgeBasedPreset? = null,
    val expandedAssetIds: Set<String> = emptySet(),
    val isBasicInfoExpanded: Boolean = false,
    val isRelocationExpanded: Boolean = false,
    val isMilestoneExpanded: Boolean = false,
    val isLoading: Boolean = true,
) {
    val investmentAsset: Asset.Investment?
        get() = assets.filterIsInstance<Asset.Investment>().firstOrNull()

    val housingPensionAsset: Asset.HousingPension?
        get() = assets.filterIsInstance<Asset.HousingPension>().firstOrNull()

    val showComparison: Boolean
        get() = relocationPlan.isConfigured() && baselineProjection != null

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
