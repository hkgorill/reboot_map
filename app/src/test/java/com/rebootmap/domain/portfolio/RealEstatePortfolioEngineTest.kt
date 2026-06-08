package com.rebootmap.domain.portfolio

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.tax.CapitalGainsTaxEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealEstatePortfolioEngineTest {

    @Test
    fun `취득 연도 이전에는 보유하지 않는다`() {
        val estate = Asset.RealEstate(
            currentValue = 500_000_000L,
            acquisitionYear = 2030,
            saleYear = 2035,
        )
        assertTrue(!RealEstatePortfolioEngine.isOwned(estate, 2028, 2026, emptySet()))
        assertTrue(RealEstatePortfolioEngine.isOwned(estate, 2032, 2026, emptySet()))
    }

    @Test
    fun `일시적 1가구2주택 — 신규 취득 후 3년 이내 구주택 매각 시 비과세`() {
        val old = Asset.RealEstate(
            id = "old",
            currentValue = 600_000_000L,
            acquisitionCost = 300_000_000L,
            holdingYears = 10,
            saleYear = 2030,
            isPrimaryResidence = true,
        )
        val newHome = Asset.RealEstate(
            id = "new",
            currentValue = 400_000_000L,
            acquisitionYear = 2028,
            saleYear = null,
            isPrimaryResidence = true,
        )
        val estates = listOf(old, newHome)
        assertTrue(
            RealEstatePortfolioEngine.qualifiesTemporaryTwoHomeExemption(
                old,
                estates,
                saleYear = 2030,
                startYear = 2026,
                soldIds = emptySet(),
            ),
        )
        val result = CapitalGainsTaxEngine.calculate(
            CapitalGainsTaxEngine.Input(
                salePrice = 600_000_000L,
                acquisitionCost = 300_000_000L,
                holdingYears = 10,
                isPrimaryResidence = true,
                otherHomesAtSale = 1,
                temporaryTwoHomeExempt = true,
            ),
        )
        assertTrue(result.isExempt)
    }
}
