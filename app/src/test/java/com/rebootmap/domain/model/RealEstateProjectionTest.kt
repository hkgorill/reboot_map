package com.rebootmap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealEstateProjectionTest {

    private val startYear = 2026

    @Test
    fun `현재시세와 예상매각가로 연평균 상승률을 계산한다`() {
        val estate = Asset.RealEstate(
            currentValue = 400_000_000L,
            debtAmount = 100_000_000L,
            saleYear = startYear + 5,
            expectedSalePrice = 500_000_000L,
        )

        val rate = RealEstateProjection.annualRate(estate, startYear)

        assertTrue(rate > 0.045 && rate < 0.047)
        assertEquals(400_000_000L, RealEstateProjection.projectedGrossValue(estate, startYear, startYear))
        assertEquals(500_000_000L, RealEstateProjection.projectedGrossValue(estate, startYear + 5, startYear))
        assertEquals(300_000_000L, RealEstateProjection.projectedNetEquity(estate, startYear, startYear))
        assertEquals(400_000_000L, RealEstateProjection.projectedNetEquity(estate, startYear + 5, startYear))
    }

    @Test
    fun `예상매각가가 현재시세보다 낮으면 하락률이 적용된다`() {
        val estate = Asset.RealEstate(
            currentValue = 500_000_000L,
            saleYear = startYear + 10,
            expectedSalePrice = 400_000_000L,
        )

        assertTrue(RealEstateProjection.annualRate(estate, startYear) < 0)
        assertEquals(400_000_000L, RealEstateProjection.projectedGrossValue(estate, startYear + 10, startYear))
    }

    @Test
    fun `예상매각가 미입력 시 현재 시세가 유지된다`() {
        val estate = Asset.RealEstate(
            currentValue = 300_000_000L,
            debtAmount = 50_000_000L,
            saleYear = startYear + 3,
            expectedSalePrice = 0L,
        )

        assertEquals(0.0, RealEstateProjection.annualRate(estate, startYear), 0.0)
        assertEquals(250_000_000L, RealEstateProjection.projectedNetEquity(estate, startYear + 2, startYear))
    }
}
