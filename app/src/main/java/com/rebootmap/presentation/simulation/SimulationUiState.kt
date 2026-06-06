package com.rebootmap.presentation.simulation

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.preset.AgeBasedPreset

data class SimulationUiState(
    val isOnboardingCompleted: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val assumptions: EconomicAssumptions = EconomicAssumptions(),
    val assets: List<Asset> = emptyList(),
    val projection: CashFlowProjection? = null,
    val isCalculating: Boolean = false,
    val presetSourceNote: String = "",
    val expandedAssetIds: Set<String> = emptySet(),
    val isBasicInfoExpanded: Boolean = false,
    val isLoading: Boolean = true,
) {
    val investmentAsset: Asset.Investment?
        get() = assets.filterIsInstance<Asset.Investment>().firstOrNull()

    companion object {
        fun fromPreset(age: Int = 40): SimulationUiState {
            val preset = AgeBasedPreset.forAge(age)
            return SimulationUiState(
                profile = preset.profile,
                assumptions = preset.assumptions,
                assets = preset.assets,
                presetSourceNote = preset.sourceNote,
            )
        }
    }
}
