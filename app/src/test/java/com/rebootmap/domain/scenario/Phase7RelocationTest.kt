package com.rebootmap.domain.scenario

import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7RelocationTest {

    private val engine = CashFlowEngine()
    private val baseYear = 2026

    @Test
    fun `P7-T01 - 취득 연도 전에는 신규 주택이 보유·2주택에 포함되지 않는다`() {
        val profile = UserProfile(currentAge = 50, retirementAge = 60, lifeExpectancy = 54, monthlyLivingExpense = 0L)
        val sell = Asset.RealEstate(
            id = "real_estate_1",
            currentValue = 500_000_000L,
            saleYear = baseYear + 3,
            category = RealEstateCategory.PRIMARY_RESIDENCE,
        )
        val buy = Asset.RealEstate(
            id = "real_estate_2",
            currentValue = 300_000_000L,
            acquisitionYear = baseYear + 2,
            saleYear = null,
            category = RealEstateCategory.PRIMARY_RESIDENCE,
        )
        val projection = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = listOf(sell, buy),
                startYear = baseYear,
            ),
        )
        val beforeTwoHome = projection.yearlySnapshots.first { it.year == baseYear + 1 }
        val twoHomeYear = projection.yearlySnapshots.first { it.year == baseYear + 2 }
        assertFalse(beforeTwoHome.relocationFlags.isTwoHomeOverlap)
        assertTrue(twoHomeYear.relocationFlags.isTwoHomeOverlap)
        assertTrue(twoHomeYear.annualHoldingCost.total > beforeTwoHome.annualHoldingCost.total)
    }

    @Test
    fun `P7-T02 - 매각 후 취득 전 무주택 구간 플래그`() {
        val profile = UserProfile(currentAge = 50, retirementAge = 60, lifeExpectancy = 54, monthlyLivingExpense = 0L)
        val sell = Asset.RealEstate(
            id = "real_estate_1",
            currentValue = 500_000_000L,
            saleYear = baseYear + 1,
            category = RealEstateCategory.PRIMARY_RESIDENCE,
        )
        val futureBuy = Asset.RealEstate(
            id = "real_estate_2",
            currentValue = 200_000_000L,
            acquisitionYear = baseYear + 3,
            saleYear = null,
            category = RealEstateCategory.PRIMARY_RESIDENCE,
        )
        val projection = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = listOf(sell, futureBuy),
                startYear = baseYear,
            ),
        )
        val gapYear = projection.yearlySnapshots.first { it.year == baseYear + 2 }
        assertTrue(gapYear.relocationFlags.isGapPeriod)
        assertFalse(gapYear.relocationFlags.isTwoHomeOverlap)
    }

    @Test
    fun `P7-T03 - downsizing preset sets new home value to 60 percent of sell estate`() {
        val sell = Asset.RealEstate(
            id = "real_estate_1",
            currentValue = 500_000_000L,
            saleYear = baseYear + 2,
        )
        val preset = RelocationPlan(enabled = true, sellEstateId = sell.id)
            .withDownsizingPreset(sell)
        assertEquals(300_000_000L, preset.newHomeValue)
        assertEquals("", preset.buyEstateId)
        assertEquals(0L, preset.newHomeDebt)
    }

    @Test
    fun `P7-T04 - sell and buy estate ids persist through mapper`() {
        val original = SimulationUiState.afterOnboarding(45, 60, 3_000_000L).copy(
            relocationPlan = RelocationPlan(
                enabled = true,
                sellEstateId = "real_estate_1",
                buyEstateId = "real_estate_2",
                newHomeValue = 100_000_000L,
            ),
        )
        val restored = SimulationStateMapper.toUiState(SimulationStateMapper.toPersisted(original))
        assertEquals("real_estate_1", restored.relocationPlan.sellEstateId)
        assertEquals("real_estate_2", restored.relocationPlan.buyEstateId)
        assertTrue(restored.relocationPlan.enabled)
    }

    @Test
    fun `P7-T05 - isConfigured requires sell estate sale year when estates provided`() {
        val estates = listOf(
            Asset.RealEstate(id = "real_estate_1", currentValue = 100_000_000L, saleYear = null),
        )
        val plan = RelocationPlan(enabled = true, sellEstateId = "real_estate_1", newHomeValue = 50_000_000L)
        assertFalse(plan.isConfigured(estates))
        val withYear = estates.first().copy(saleYear = baseYear + 1)
        assertTrue(plan.isConfigured(listOf(withYear)))
    }

    @Test
    fun `P7-T06 - linked buy satisfies isConfigured without virtual new home`() {
        val estates = listOf(
            Asset.RealEstate(id = "real_estate_1", currentValue = 100_000_000L, saleYear = baseYear + 1),
            Asset.RealEstate(id = "real_estate_2", currentValue = 80_000_000L, saleYear = null),
        )
        val plan = RelocationPlan(
            enabled = true,
            sellEstateId = "real_estate_1",
            buyEstateId = "real_estate_2",
        )
        assertTrue(plan.isConfigured(estates))
        assertNotNull(plan.resolveBuyEstate(estates))
    }
}
