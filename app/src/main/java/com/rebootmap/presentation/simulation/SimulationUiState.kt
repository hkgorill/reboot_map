package com.rebootmap.presentation.simulation

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile

data class SimulationUiState(
    val profile: UserProfile = UserProfile(),
    val assumptions: EconomicAssumptions = EconomicAssumptions(),
    val assets: List<Asset> = defaultAssets(),
    val projection: CashFlowProjection? = null,
    val isCalculating: Boolean = false,
) {
    companion object {
        fun defaultAssets(): List<Asset> = listOf(
            Asset.RealEstate(currentValue = 0L, saleYear = null),
            Asset.NationalPension(monthlyPayout = 0L),
            Asset.RetirementPension(balance = 0L, monthlyContribution = 0L, contributionEndAge = 60),
            Asset.Investment(currentValue = 0L, annualReturnRate = 0.05),
            Asset.CashSavings(maturityAmount = 0L, maturityYear = java.time.Year.now().value + 5),
        )
    }
}
