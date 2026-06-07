package com.rebootmap.domain.model

import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowProjectionTest {

    @Test
    fun `자산 감소 연도는 전년 대비 endingBalance가 줄어든 은퇴 후 연도만 포함한다`() {
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snapshot(year = 2026, age = 58, balance = 100_000_000L),
                snapshot(year = 2027, age = 59, balance = 120_000_000L),
                snapshot(year = 2028, age = 60, balance = 110_000_000L),
                snapshot(year = 2029, age = 61, balance = 115_000_000L),
                snapshot(year = 2030, age = 62, balance = 90_000_000L),
            ),
            depletionYear = null,
            deficitYears = listOf(2028, 2029, 2030),
        )

        val declineYears = projection.assetDeclineYears(retirementAge = 60)

        assertEquals(listOf(2028, 2030), declineYears)
    }

    @Test
    fun `투자 수익률이 높아 자산이 늘면 감소 연도가 줄어든다`() {
        val lowReturn = CashFlowProjection(
            yearlySnapshots = listOf(
                snapshot(year = 2028, age = 60, balance = 100_000_000L),
                snapshot(year = 2029, age = 61, balance = 90_000_000L),
            ),
            depletionYear = null,
            deficitYears = listOf(2029),
        )
        val highReturn = CashFlowProjection(
            yearlySnapshots = listOf(
                snapshot(year = 2028, age = 60, balance = 100_000_000L),
                snapshot(year = 2029, age = 61, balance = 105_000_000L),
            ),
            depletionYear = null,
            deficitYears = listOf(2029),
        )

        assertEquals(1, lowReturn.assetDeclineYears(60).size)
        assertTrue(highReturn.assetDeclineYears(60).isEmpty())
    }

    @Test
    fun `연도 구간 포맷은 연수와 나이 범위를 함께 표시한다`() {
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(
                snapshot(year = 2046, age = 60, balance = 100_000_000L),
                snapshot(year = 2047, age = 61, balance = 90_000_000L),
                snapshot(year = 2048, age = 62, balance = 80_000_000L),
            ),
            depletionYear = null,
            deficitYears = listOf(2046, 2047),
        )

        assertEquals(
            "2년 · 60~61세 (2046~2047)",
            projection.formatYearSpan(listOf(2046, 2047)),
        )
        val summary = projection.yearSpanSummary(listOf(2046, 2047))
        assertEquals("2년 · 60~61세", summary.headline)
        assertEquals("(2046~2047)", summary.rangeLine)
    }

    private fun snapshot(year: Int, age: Int, balance: Long) = YearSnapshot(
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
