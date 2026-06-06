package com.rebootmap.domain.tax

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapitalGainsTaxEngineTest {

    @Test
    fun `P3-T01 - 1세대1주택 보유 2년 이상이면 비과세`() {
        val result = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 500_000_000L,
                acquisitionCost = 300_000_000L,
                holdingYears = 5,
                isPrimaryResidence = true,
            ),
        )

        assertTrue(result.isExempt)
        assertEquals(0L, result.tax)
    }

    @Test
    fun `P3-T02 - 1주택이어도 보유 1년이면 과세`() {
        val result = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 500_000_000L,
                acquisitionCost = 300_000_000L,
                holdingYears = 1,
                isPrimaryResidence = true,
            ),
        )

        assertFalse(result.isExempt)
        assertTrue(result.tax > 0)
    }

    @Test
    fun `P3-T03 - 양도차익이 없으면 세금 0`() {
        val result = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 300_000_000L,
                acquisitionCost = 300_000_000L,
                holdingYears = 1,
                isPrimaryResidence = false,
            ),
        )

        assertEquals(0L, result.tax)
    }

    @Test
    fun `P3-T04 - 장기보유 10년이면 공제 30퍼센트 적용`() {
        val shortHold = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 200_000_000L,
                acquisitionCost = 100_000_000L,
                holdingYears = 1,
                isPrimaryResidence = false,
            ),
        )
        val longHold = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 200_000_000L,
                acquisitionCost = 100_000_000L,
                holdingYears = 10,
                isPrimaryResidence = false,
            ),
        )

        assertTrue(longHold.tax < shortHold.tax)
        assertEquals(70_000_000L, longHold.taxableGain)
    }

    @Test
    fun `P3-T05 - 누진세율 구간 검증`() {
        assertEquals(840_000L, CapitalGainsTaxEngine.progressiveTax(14_000_000L))
        assertEquals(2_340_000L, CapitalGainsTaxEngine.progressiveTax(14_000_000L + 10_000_000L))
    }
}
