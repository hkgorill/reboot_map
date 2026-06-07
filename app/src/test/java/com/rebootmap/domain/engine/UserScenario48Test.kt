package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Year

/**
 * 실기기 재현 시나리오 (48세, 기대수명 90, 월 생활비 300만원) 분석용.
 */
class UserScenario48Test {

    private val engine = CashFlowEngine()

    private fun userAssets() = listOf(
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
    )

    @Test
    fun `시나리오 — 은퇴 60세일 때 61~64세 적자와 65세 이후에도 생활비 부족`() {
        val result = project(retirementAge = 60)

        val age60 = snapshotAt(result.yearlySnapshots, 60)
        val age61 = snapshotAt(result.yearlySnapshots, 61)
        val age65 = snapshotAt(result.yearlySnapshots, 65)
        val age70 = snapshotAt(result.yearlySnapshots, 70)

        // 60세: 고정수입(600만) + 퇴직연금 인출 시작 → 흑자 가능
        assertTrue(age60.netCashFlow > 0)

        // 61세: 고정수입 종료(48~60), 국민연금(65) 전 → 큰 적자
        assertTrue(age61.netCashFlow < 0)
        assertTrue(age61.annualExpense > 0)
        assertTrue(age61.annualIncome < age61.annualExpense)

        // 65세 수입·지출·총자산 스냅샷 (분석용)
        assertTrue(
            debugSnapshot("age60", age60) +
                debugSnapshot("age61", age61) +
                debugSnapshot("age65", age65) +
                debugSnapshot("age70", age70),
            age65.netCashFlow < 0 || age70.totalAssets < age65.totalAssets,
        )
    }

    @Test
    fun `시나리오 — 은퇴 65세로 맞추면 65세 전 생활비 0, 65세에도 연금 합산은 300만 미만`() {
        val result = project(retirementAge = 65)
        val age64 = snapshotAt(result.yearlySnapshots, 64)
        val age65 = snapshotAt(result.yearlySnapshots, 65)

        assertEquals(0L, age64.annualLivingExpense)
        assertTrue(age65.annualLivingExpense > 0)
        assertTrue(age65.annualIncome > age65.annualLivingExpense)
    }

    private fun project(retirementAge: Int) = engine.project(
        SimulationInput(
            profile = UserProfile(
                currentAge = 48,
                retirementAge = retirementAge,
                lifeExpectancy = 90,
                monthlyLivingExpense = 3_000_000L,
            ),
            assumptions = EconomicAssumptions(inflationRate = 0.02),
            assets = userAssets(),
            startYear = Year.now().value,
        ),
    )

    private fun debugSnapshot(label: String, s: YearSnapshot): String =
        "$label: 수입=${s.annualIncome / 10_000}만/년, 지출=${s.annualExpense / 10_000}만/년, " +
            "세금=${s.annualTax / 10_000}만, 순현금=${s.netCashFlow / 10_000}만, 총자산=${s.totalAssets / 10_000}만 | "

    private fun snapshotAt(snapshots: List<YearSnapshot>, age: Int): YearSnapshot =
        snapshots.first { it.age == age }
}
