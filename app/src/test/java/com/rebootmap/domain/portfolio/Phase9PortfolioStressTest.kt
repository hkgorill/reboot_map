package com.rebootmap.domain.portfolio

import com.rebootmap.data.mapper.RealEstatePersistence
import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.data.model.PersistedRealEstate
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.presentation.dashboard.DashboardGroupSummaries
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Year
import kotlin.random.Random

class Phase9PortfolioStressTest {

    private val engine = CashFlowEngine()
    private val startYear = Year.now().value

    @Test
    fun `복수 부동산 랜덤 취득·매각 조합 300건 엔진 크래시 없음`() {
        val random = Random(42)
        repeat(300) { index ->
            val count = 1 + (index % 3)
            val estates = (1..count).map { n ->
                val value = random.nextLong(0, 2_000_000_000L)
                val debt = random.nextLong(0, value.coerceAtLeast(1))
                val acq = random.nextInt(startYear - 5, startYear + 15).takeIf { random.nextBoolean() }
                val sale = random.nextInt(startYear + 1, startYear + 25).takeIf { random.nextBoolean() }
                val safeSale = when {
                    sale == null -> null
                    acq != null && acq > sale -> null
                    else -> sale
                }
                Asset.RealEstate(
                    id = "real_estate_$n",
                    currentValue = value,
                    debtAmount = debt,
                    acquisitionCost = value / 2,
                    holdingYears = random.nextInt(0, 20),
                    category = if (random.nextBoolean()) {
                        RealEstateCategory.PRIMARY_RESIDENCE
                    } else {
                        RealEstateCategory.NON_RESIDENTIAL
                    },
                    isPrimaryResidence = random.nextBoolean(),
                    acquisitionYear = acq,
                    saleYear = safeSale,
                    expectedSalePrice = if (safeSale != null) value + random.nextLong(0, 500_000_000L) else 0L,
                )
            }
            val profile = UserProfile(
                currentAge = 45 + (index % 20),
                retirementAge = 60,
                lifeExpectancy = 70 + (index % 15),
                monthlyLivingExpense = random.nextLong(0, 5_000_000L),
            )
            val result = engine.project(
                SimulationInput(
                    profile = profile,
                    assumptions = EconomicAssumptions(),
                    assets = estates,
                    startYear = startYear,
                ),
            )
            assertTrue(result.yearlySnapshots.isNotEmpty())
            RealEstateTimingAdvisoryEngine.evaluate(estates, startYear)
            RealEstateTimingAdvisoryEngine.estimateTransactionTaxWon(estates, startYear)
            DashboardGroupSummaries.realEstate(
                SimulationUiState(isOnboardingCompleted = true, assets = estates),
                RealEstateTimingAdvisoryEngine.evaluate(estates, startYear),
            )
        }
    }

    @Test
    fun `취득 연도 persistence 왕복`() {
        val estate = Asset.RealEstate(
            id = "real_estate_1",
            currentValue = 400_000_000L,
            acquisitionYear = startYear + 2,
            saleYear = startYear + 8,
        )
        val persisted = PersistedRealEstate.fromDomain(estate)
        val restored = persisted.toDomain()
        assertNotNull(restored.acquisitionYear)
        assertTrue(restored.acquisitionYear!! <= restored.saleYear!!)
        val list = RealEstatePersistence.toPersistedList(listOf(estate))
        assertTrue(list.first().acquisitionYear == startYear + 2)
    }

    @Test
    fun `시세 0인 미래 취득만 있는 카드는 보유 주택 수에 포함되지 않는다`() {
        val placeholder = Asset.RealEstate(
            id = "future",
            currentValue = 0L,
            acquisitionYear = startYear + 3,
            saleYear = null,
        )
        val owned = RealEstatePortfolioEngine.homeCount(
            listOf(placeholder),
            year = startYear + 5,
            startYear = startYear,
            soldIds = emptySet(),
        )
        assertTrue(owned == 0)
    }
}
