package com.rebootmap.data.mapper

import com.rebootmap.domain.model.Asset
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SimulationStateMapperTest {

    @Test
    fun `저장 상태와 UI 상태 간 왕복 변환이 일관된다`() {
        val original = SimulationUiState.afterOnboarding(
            currentAge = 45,
            retirementAge = 60,
            monthlyLivingExpense = 3_000_000L,
        ).copy(
            isOnboardingCompleted = true,
            assets = SimulationStateMapper.emptyAssets().map { asset ->
                when (asset) {
                    is Asset.RealEstate -> asset.copy(currentValue = 50_000L * 10_000, debtAmount = 17_500L * 10_000)
                    is Asset.Investment -> asset.copy(currentValue = 10_000_000L, annualReturnRate = 0.05)
                    else -> asset
                }
            },
        )

        val persisted = SimulationStateMapper.toPersisted(original)
        val restored = SimulationStateMapper.toUiState(persisted)

        assertEquals(original.profile, restored.profile)
        assertEquals(original.assumptions.inflationRate, restored.assumptions.inflationRate, 0.001)
        assertEquals(original.assets.size, restored.assets.size)

        val originalEstate = original.assets.filterIsInstance<Asset.RealEstate>().first()
        val restoredEstate = restored.assets.filterIsInstance<Asset.RealEstate>().first()
        assertEquals(originalEstate.netEquity, restoredEstate.netEquity)

        val originalInvestment = original.assets.filterIsInstance<Asset.Investment>().first()
        val restoredInvestment = restored.assets.filterIsInstance<Asset.Investment>().first()
        assertEquals(originalInvestment.annualReturnRate, restoredInvestment.annualReturnRate, 0.001)
    }

    @Test
    fun `복수 부동산 저장-복원이 유지된다`() {
        val estates = listOf(
            Asset.RealEstate(
                id = "real_estate_1",
                currentValue = 500_000_000L,
                debtAmount = 100_000_000L,
                saleYear = null,
            ),
            Asset.RealEstate(
                id = "real_estate_2",
                currentValue = 200_000_000L,
                category = com.rebootmap.domain.model.RealEstateCategory.NON_RESIDENTIAL,
                isPrimaryResidence = false,
                saleYear = null,
            ),
        )
        val original = SimulationUiState.afterOnboarding(45, 60, 3_000_000L).copy(
            assets = SimulationStateMapper.defaultAssets(realEstates = estates),
        )

        val restored = SimulationStateMapper.toUiState(SimulationStateMapper.toPersisted(original))
        val restoredEstates = restored.assets.filterIsInstance<Asset.RealEstate>()

        assertEquals(2, restoredEstates.size)
        assertEquals(estates[0].netEquity, restoredEstates[0].netEquity)
        assertEquals(estates[1].category, restoredEstates[1].category)
        assertEquals(2, SimulationStateMapper.toPersisted(original).realEstates.size)
    }

    @Test
    fun `구버전 퇴직연금 필드는 퇴직연금 자산으로 마이그레이션된다`() {
        val legacy = com.rebootmap.data.model.SimulationPersistedState(
            retirementPensionBalance = 50_000_000L,
            retirementPensionMonthly = 300_000L,
            retirementPensionEndAge = 58,
        )

        val restored = SimulationStateMapper.toUiState(legacy)
        val severance = restored.assets.filterIsInstance<Asset.SeverancePension>().first()

        assertEquals(50_000_000L, severance.balance)
        assertEquals(300_000L, severance.monthlyContribution)
        assertEquals(58, severance.contributionEndAge)
    }
}
