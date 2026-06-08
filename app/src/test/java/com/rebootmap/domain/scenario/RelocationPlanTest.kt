package com.rebootmap.domain.scenario

import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import org.junit.Assert.assertTrue
import org.junit.Test

class RelocationPlanTest {

    private val engine = CashFlowEngine()
    private val baseYear = 2026

    @Test
    fun `P3-T09 - 2주택 겹침 매각 시 baseline 대비 최종 잔액이 줄어든다`() {
        val profile = UserProfile(
            currentAge = 55,
            retirementAge = 60,
            lifeExpectancy = 58,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(
                currentValue = 600_000_000L,
                acquisitionCost = 200_000_000L,
                holdingYears = 10,
                isPrimaryResidence = true,
                saleYear = baseYear + 2,
            ),
        )
        val newHome = Asset.RealEstate(
            id = "real_estate_2",
            currentValue = 400_000_000L,
            acquisitionYear = baseYear + 1,
            saleYear = null,
            isPrimaryResidence = true,
        )

        val withSecondHome = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets + newHome,
                startYear = baseYear,
            ),
        )
        val baseline = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = baseYear,
            ),
        )

        val relocatedSale = withSecondHome.yearlySnapshots.first { it.year == baseYear + 2 }
        val baselineSale = baseline.yearlySnapshots.first { it.year == baseYear + 2 }
        assertTrue(relocatedSale.endingBalance < baselineSale.endingBalance)
    }

    @Test
    fun `P3-T10 - 2주택 겹침 시 양도세가 비과세보다 높다`() {
        val profile = UserProfile(
            currentAge = 50,
            retirementAge = 60,
            lifeExpectancy = 53,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(
                currentValue = 600_000_000L,
                acquisitionCost = 200_000_000L,
                holdingYears = 10,
                isPrimaryResidence = true,
                saleYear = baseYear + 2,
            ),
        )
        val secondHome = Asset.RealEstate(
            id = "real_estate_2",
            currentValue = 400_000_000L,
            saleYear = null,
            isPrimaryResidence = true,
        )

        val twoHome = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets + secondHome,
                startYear = baseYear,
            ),
        )
        val singleHome = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = baseYear,
            ),
        )

        val twoHomeCgt = twoHome.yearlySnapshots.first { it.year == baseYear + 2 }.taxBreakdown.capitalGainsTax
        val afterSaleCgt = singleHome.yearlySnapshots.first { it.year == baseYear + 2 }.taxBreakdown.capitalGainsTax
        assertTrue(twoHomeCgt > afterSaleCgt)
    }
}
