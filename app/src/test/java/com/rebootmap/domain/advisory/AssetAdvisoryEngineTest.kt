package com.rebootmap.domain.advisory

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetAdvisoryEngineTest {

    @Test
    fun `projection이 없으면 미산정 리포트를 반환한다`() {
        val report = AssetAdvisoryEngine.evaluate(
            projection = null,
            profile = UserProfile(),
            assets = emptyList(),
        )

        assertEquals(0, report.score)
        assertEquals("미산정", report.gradeLabel)
        assertTrue(report.watchPoints.isNotEmpty())
    }

    @Test
    fun `고갈 없는 시나리오는 높은 점수와 양호 등급에 가깝다`() {
        val profile = UserProfile(currentAge = 55, retirementAge = 60, lifeExpectancy = 85, monthlyLivingExpense = 300_0000L)
        val projection = CashFlowProjection(
            yearlySnapshots = (55..85).map { age ->
                val balance = if (age == 85) 50_000_000L else 200_000_000L
                snap(2020 + (age - 55), age, balance = balance)
            },
            depletionYear = null,
            deficitYears = emptyList(),
        )
        val assets = listOf(
            Asset.NationalPension(monthlyPayout = 2_000_000L, startAge = 65),
            Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.05),
            Asset.CashSavings(maturityAmount = 30_000_000L, maturityYear = 2030),
        )

        val report = AssetAdvisoryEngine.evaluate(projection, profile, assets)

        assertTrue(report.score >= 80)
        assertEquals("양호", report.gradeLabel)
        assertTrue(report.strengths.any { it.contains("기대 수명까지") })
    }

    @Test
    fun `조기 고갈 시나리오는 점수가 낮고 위험 요인을 나열한다`() {
        val profile = UserProfile(currentAge = 58, retirementAge = 60, lifeExpectancy = 80, monthlyLivingExpense = 500_0000L)
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snap(2026, 58, balance = 100_000_000L),
                snap(2028, 60, balance = 80_000_000L),
                snap(2030, 62, balance = 0L),
                snap(2031, 63, balance = -10_000_000L),
            ),
            depletionYear = 2030,
            deficitYears = listOf(2028, 2029, 2030, 2031),
        )

        val report = AssetAdvisoryEngine.evaluate(projection, profile, emptyList())

        assertTrue(report.score < 60)
        assertTrue(report.weaknesses.any { it.contains("고갈") })
        assertTrue(report.weaknesses.any { it.contains("부족") })
    }

    private fun snap(year: Int, age: Int, balance: Long) = YearSnapshot(
        year = year,
        age = age,
        liquidAssets = balance,
        illiquidAssets = 0L,
        totalAssets = balance,
        annualIncome = 0L,
        annualExpense = 0L,
        annualLivingExpense = 0L,
        annualHoldingCost = AnnualHoldingCost(),
        annualTax = 0L,
        taxBreakdown = AnnualTaxBreakdown(),
        netCashFlow = 0L,
        endingBalance = balance,
    )
}
