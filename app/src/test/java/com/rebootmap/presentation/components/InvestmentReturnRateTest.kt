package com.rebootmap.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentReturnRateTest {

    @Test
    fun `수익률은 0_5퍼센트포인트씩 증감한다`() {
        assertEquals(0.055, InvestmentReturnRate.increment(0.05), 0.0001)
        assertEquals(0.045, InvestmentReturnRate.decrement(0.05), 0.0001)
    }

    @Test
    fun `최소와 최대에서 증감이 막힌다`() {
        assertFalse(InvestmentReturnRate.canDecrement(InvestmentReturnRate.MIN))
        assertFalse(InvestmentReturnRate.canIncrement(InvestmentReturnRate.MAX))
    }

    @Test
    fun `퍼센트 표기는 정수와 소수를 통일한다`() {
        assertEquals("5%", InvestmentReturnRate.formatPercent(0.05))
        assertEquals("5.5%", InvestmentReturnRate.formatPercent(0.055))
        assertEquals("15%", InvestmentReturnRate.formatPercent(0.15))
    }

    @Test
    fun `기본 수익률은 5퍼센트이다`() {
        assertEquals(0.05, InvestmentReturnRate.DEFAULT_RATE, 0.0001)
        assertEquals(0.05, InvestmentReturnRate.snap(0.05), 0.0001)
    }
}
