package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.tax.PropertyHoldingTaxEngine.EstateLine
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase6PropertyHoldingTest {

    @Test
    fun `주택과 비주택 재산세가 유형별 세율로 분리 산출된다`() {
        val holding = PropertyHoldingTaxEngine.calculate(
            PropertyHoldingTaxEngine.Input(
                estates = listOf(
                    EstateLine(netEquity = 400_000_000L, category = RealEstateCategory.PRIMARY_RESIDENCE),
                    EstateLine(netEquity = 200_000_000L, category = RealEstateCategory.NON_RESIDENTIAL),
                ),
                assumptions = EconomicAssumptions(),
            ),
        )

        assertEquals(1_000_000L, holding.residentialPropertyTax)
        assertEquals(800_000L, holding.nonResidentialPropertyTax)
        assertEquals(1_800_000L, holding.propertyTax)
    }

    @Test
    fun `종부세는 복수 부동산 순자산 합산 기준이다`() {
        val holding = PropertyHoldingTaxEngine.calculate(
            PropertyHoldingTaxEngine.Input(
                estates = listOf(
                    EstateLine(netEquity = 400_000_000L, category = RealEstateCategory.PRIMARY_RESIDENCE),
                    EstateLine(netEquity = 400_000_000L, category = RealEstateCategory.NON_RESIDENTIAL),
                ),
                assumptions = EconomicAssumptions(),
            ),
        )

        assertEquals(1_200_000L, holding.comprehensiveRealEstateTax)
        assertEquals(3_800_000L, holding.total)
    }
}
