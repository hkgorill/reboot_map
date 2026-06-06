package com.rebootmap.domain.matching

import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.model.Asset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetMatchingEngineTest {

    @Test
    fun `P4-T01 - 적금 만기 연도 일치 시 최고 점수`() {
        val expense = LumpSumExpense(label = "자녀 교육", amount = 10_000_000L, year = 2030)
        val assets = listOf(
            Asset.CashSavings(maturityAmount = 15_000_000L, maturityYear = 2030),
        )

        val result = AssetMatchingEngine.recommend(expense, assets, startYear = 2026, currentAge = 45)

        assertEquals(1, result.size)
        assertEquals(100, result.first().matchScore)
        assertEquals("현금·적금", result.first().assetLabel)
    }

    @Test
    fun `P4-T02 - 부동산 매각 연도 일치 시 높은 점수`() {
        val expense = LumpSumExpense(label = "자녀 결혼", amount = 50_000_000L, year = 2032)
        val assets = listOf(
            Asset.RealEstate(currentValue = 80_000_000L, debtAmount = 10_000_000L, saleYear = 2032),
        )

        val result = AssetMatchingEngine.recommend(expense, assets, startYear = 2026, currentAge = 50)

        assertTrue(result.isNotEmpty())
        assertEquals(95, result.first().matchScore)
        assertEquals("부동산 매각", result.first().assetLabel)
    }

    @Test
    fun `P4-T03 - 투자 자산은 즉시 인출 가능으로 추천`() {
        val expense = LumpSumExpense(label = "의료", amount = 5_000_000L, year = 2027)
        val assets = listOf(
            Asset.Investment(currentValue = 20_000_000L, annualReturnRate = 0.05),
        )

        val result = AssetMatchingEngine.recommend(expense, assets, startYear = 2026, currentAge = 55)

        assertEquals(1, result.size)
        assertEquals(60, result.first().matchScore)
        assertTrue(result.first().timingNote.contains("즉시"))
    }
}
