package com.rebootmap.domain.advisory

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualIncomeBreakdown
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowHighlightPlannerTest {

    @Test
    fun `주요 이벤트 연도에 라벨을 붙인다`() {
        val profile = UserProfile(currentAge = 58, retirementAge = 60, lifeExpectancy = 70, monthlyLivingExpense = 300_0000L)
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snap(2026, 58),
                snap(2028, 60, income = AnnualIncomeBreakdown(employmentIncome = 60_000_000L)),
                snap(2033, 65, income = AnnualIncomeBreakdown(nationalPension = 24_000_000L)),
                snap(2035, 67, income = AnnualIncomeBreakdown(nationalPension = 24_000_000L, realEstateSale = 500_000_000L)),
                snap(2038, 70),
            ),
            depletionYear = 2038,
            deficitYears = listOf(2035),
        )
        val assets = listOf(Asset.NationalPension(monthlyPayout = 2_000_000L, startAge = 65))

        val highlights = CashFlowHighlightPlanner.highlights(projection, profile, assets)
        val labelsByAge = highlights.associate { it.snapshot.age to it.labels }

        assertTrue(labelsByAge[58]?.contains("현재") == true)
        assertTrue(labelsByAge[60]?.contains("은퇴 첫해") == true)
        assertTrue(labelsByAge[65]?.contains("국민연금 개시") == true)
        assertTrue(labelsByAge[67]?.any { it.contains("일시 유입") } == true)
        assertTrue(labelsByAge[67]?.contains("첫 수입 부족") == true)
        assertTrue(labelsByAge[70]?.contains("자산 고갈 예상") == true)
        assertTrue(labelsByAge[70]?.contains("기대 수명") == true)
    }

    @Test
    fun `은퇴 후 연도만 postRetirementSnapshots에 포함된다`() {
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snap(2026, 58),
                snap(2028, 60),
                snap(2029, 61),
            ),
            depletionYear = null,
            deficitYears = emptyList(),
        )

        val post = CashFlowHighlightPlanner.postRetirementSnapshots(projection, retirementAge = 60)

        assertEquals(2, post.size)
        assertEquals(listOf(60, 61), post.map { it.age })
    }

    @Test
    fun `yearlyDetailSnapshots는 요약에 없는 은퇴 전·후 연도를 모두 포함한다`() {
        val profile = UserProfile(currentAge = 58, retirementAge = 60, lifeExpectancy = 62)
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snap(2026, 58),
                snap(2027, 59),
                snap(2028, 60),
                snap(2029, 61),
                snap(2030, 62),
            ),
            depletionYear = null,
            deficitYears = emptyList(),
        )

        val highlights = CashFlowHighlightPlanner.highlights(projection, profile)
        val details = CashFlowHighlightPlanner.yearlyDetailSnapshots(projection, profile)

        assertTrue(highlights.any { it.snapshot.age == 58 })
        assertTrue(highlights.any { it.snapshot.age == 60 })
        assertEquals(listOf(59, 61), details.map { it.age })
        assertEquals(1, details.count { it.age < profile.retirementAge })
        assertEquals(1, details.count { it.age >= profile.retirementAge })
    }

    private fun snap(year: Int, age: Int, income: AnnualIncomeBreakdown = AnnualIncomeBreakdown()) =
        YearSnapshot(
            year = year,
            age = age,
            liquidAssets = 100_000_000L,
            illiquidAssets = 0L,
            totalAssets = 100_000_000L,
            annualIncome = income.total,
            incomeBreakdown = income,
            annualExpense = 0L,
            annualLivingExpense = 0L,
            annualHoldingCost = AnnualHoldingCost(),
            annualTax = 0L,
            taxBreakdown = AnnualTaxBreakdown(),
            netCashFlow = 0L,
            endingBalance = 100_000_000L,
        )
}
