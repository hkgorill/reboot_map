package com.rebootmap.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattedMoneyTest {

    @Test
    fun `금액에 천 단위 콤마가 붙는다`() {
        assertEquals("50,000", formatNumberWithComma(50_000))
        assertEquals("1,234", formatNumberWithComma(1_234))
    }
}
