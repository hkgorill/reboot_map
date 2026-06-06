package com.rebootmap.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattedMoneyTest {

    @Test
    fun `금액에 천 단위 콤마가 붙는다`() {
        assertEquals("50,000", formatNumberWithComma(50_000))
        assertEquals("1,234", formatNumberWithComma(1_234))
    }

    @Test
    fun `마이너스 금액은 부호가 붙는다`() {
        assertEquals("-1,800만원", formatKoreanMan(-18_000_000L))
        assertEquals("0만원", formatKoreanMan(0L))
    }
}
