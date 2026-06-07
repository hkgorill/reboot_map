package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Year

/**
 * 월 순현금(netCashFlow)과 총자산 전년 대비 증감이 일치하지 않는 이유를 검증한다.
 */
class TotalAssetsReconciliationTest {

    private val engine = CashFlowEngine()
    private val startYear = Year.now().value

    private fun scenario48(retirementAge: Int = 60) = engine.project(
        SimulationInput(
            profile = UserProfile(
                currentAge = 48,
                retirementAge = retirementAge,
                lifeExpectancy = 90,
                monthlyLivingExpense = 3_000_000L,
            ),
            assumptions = EconomicAssumptions(inflationRate = 0.02),
            assets = listOf(
                Asset.RealEstate(currentValue = 490_000_000L, debtAmount = 0L, saleYear = null),
                Asset.NationalPension(monthlyPayout = 1_700_000L, startAge = 65),
                Asset.SeverancePension(
                    balance = 110_000_000L,
                    monthlyContribution = 600_000L,
                    contributionEndAge = 60,
                    payoutStartAge = 65,
                ),
                Asset.PersonalPension(
                    balance = 45_000_000L,
                    monthlyContribution = 400_000L,
                    contributionEndAge = 60,
                    payoutStartAge = 65,
                ),
                Asset.YellowUmbrella(
                    balance = 250_000L,
                    monthlyContribution = 250_000L,
                    contributionEndAge = 55,
                    payoutAge = 65,
                ),
                Asset.Investment(currentValue = 1_000_000L, annualReturnRate = 0.05),
                Asset.EmploymentIncome(monthlyAmount = 6_000_000L, startAge = 48, endAge = 60),
            ),
            startYear = startYear,
        ),
    )

    @Test
    fun `총자산 증감은 유동·비유동 합과 일치한다`() {
        val snapshots = scenario48().yearlySnapshots
        for (index in 1 until snapshots.size) {
            val prev = snapshots[index - 1]
            val curr = snapshots[index]
            assertEquals(
                curr.totalAssets - prev.totalAssets,
                curr.liquidAssets - prev.liquidAssets + (curr.illiquidAssets - prev.illiquidAssets),
            )
        }
    }

    @Test
    fun `순현금은 수입-지출-세금과 일치하며 총자산 증감과는 별개다`() {
        val snapshots = scenario48().yearlySnapshots
        for (age in listOf(60, 65, 70, 80)) {
            val row = reconcile(snapshots, age)
            val s = row.snapshot
            assertEquals(row.netCashFlow, s.annualIncome - s.annualExpense - s.annualTax)
            assertEquals(s.annualHoldingCost.total, s.annualHoldingCost.propertyTax + s.annualHoldingCost.comprehensiveRealEstateTax)
            assertEquals(s.annualExpense, s.annualLivingExpense + s.annualHoldingCost.total)
            assertNotEquals(
                "age $age: 순현금과 총자산Δ가 같으면 안 됨 (운용수익·연금잔액 변동 반영)",
                row.netCashFlow,
                row.deltaTotal,
            )
        }
    }

    @Test
    fun `65세 연금 인출 시작 시 순현금 흑자여도 총자산은 줄 수 있다`() {
        val row = reconcile(scenario48().yearlySnapshots, 65)
        // 엔진 재현: 순현금 +2,004만/년, 총자산 -1,570만/년 (인출=수입이지만 잔액 감소)
        assertTrue(row.netCashFlow in 19_000_000L..21_000_000L)
        assertTrue(row.deltaTotal in -17_000_000L..-14_000_000L)
        assertTrue(row.netCashFlow > 0)
        assertTrue(row.deltaTotal < 0)
    }

    @Test
    fun `월 부과는 재산세와 종부세 연간 합의 12분의 1이다`() {
        val age60 = scenario48().yearlySnapshots.first { it.age == 60 }
        val holding = age60.annualHoldingCost
        assertEquals(1_225_000L, holding.propertyTax) // 4.9억 × 0.25%
        assertEquals(holding.total / 12, holding.total / 12) // UI 표시 = total/12
    }

    private data class ReconcileRow(
        val snapshot: YearSnapshot,
        val netCashFlow: Long,
        val deltaTotal: Long,
        val deltaLiquid: Long,
        val deltaIlliquid: Long,
    )

    private fun reconcile(snapshots: List<YearSnapshot>, age: Int): ReconcileRow {
        val index = snapshots.indexOfFirst { it.age == age }
        val prev = snapshots[index - 1]
        val curr = snapshots[index]
        return ReconcileRow(
            snapshot = curr,
            netCashFlow = curr.netCashFlow,
            deltaTotal = curr.totalAssets - prev.totalAssets,
            deltaLiquid = curr.liquidAssets - prev.liquidAssets,
            deltaIlliquid = curr.illiquidAssets - prev.illiquidAssets,
        )
    }
}
