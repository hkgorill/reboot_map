package com.rebootmap.domain.tax

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnualIncomeBreakdownTest {

    @Test
    fun `recurringTotal는 일시 유입을 제외한다`() {
        val income = AnnualIncomeBreakdown(
            nationalPension = 24_000_000L,
            realEstateSale = 400_000_000L,
            cashSavingsMaturity = 30_000_000L,
        )
        assertEquals(454_000_000L, income.total)
        assertEquals(430_000_000L, income.lumpSumTotal)
        assertEquals(24_000_000L, income.recurringTotal)
    }
}
