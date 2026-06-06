package com.rebootmap.domain.tax

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HousingPensionEngineTest {

    @Test
    fun `P3-T06 - 65세 주택연금 LTV 60퍼센트 월 수령액 계산`() {
        val result = HousingPensionEngine.calculateMonthly(
            HousingPensionEngine.Input(
                homeEquity = 300_000_000L,
                startAge = 65,
                currentAge = 65,
                lifeExpectancy = 90,
            ),
        )

        assertEquals(0.60, result.ltvRate, 0.001)
        assertEquals(25, result.payoutYears)
        assertTrue(result.monthlyPayout > 0)
        assertEquals(600_000L, result.monthlyPayout)
    }

    @Test
    fun `P3-T07 - 개시 전에는 월 수령액 0`() {
        val result = HousingPensionEngine.calculateMonthly(
            HousingPensionEngine.Input(
                homeEquity = 300_000_000L,
                startAge = 65,
                currentAge = 60,
                lifeExpectancy = 90,
            ),
        )

        assertEquals(0L, result.monthlyPayout)
    }

    @Test
    fun `P3-T08 - 담보 0이면 수령액 0`() {
        val result = HousingPensionEngine.calculateMonthly(
            HousingPensionEngine.Input(
                homeEquity = 0L,
                startAge = 65,
                currentAge = 70,
                lifeExpectancy = 90,
            ),
        )

        assertEquals(0L, result.monthlyPayout)
    }
}
