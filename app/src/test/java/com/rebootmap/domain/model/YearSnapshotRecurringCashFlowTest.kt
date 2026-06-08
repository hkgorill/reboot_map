package com.rebootmap.domain.model

import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualIncomeBreakdown
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import org.junit.Assert.assertEquals
import org.junit.Test

class YearSnapshotRecurringCashFlowTest {

    @Test
    fun `매각 연도 월표용 순현금은 일시 매각·양도세를 제외한다`() {
        val snapshot = YearSnapshot(
            year = 2038,
            age = 60,
            liquidAssets = 0L,
            illiquidAssets = 0L,
            totalAssets = 0L,
            annualIncome = 424_000_000L,
            incomeBreakdown = AnnualIncomeBreakdown(
                nationalPension = 24_000_000L,
                realEstateSale = 400_000_000L,
            ),
            annualExpense = 3_600_000L,
            annualLivingExpense = 3_600_000L,
            annualHoldingCost = AnnualHoldingCost(),
            annualTax = 50_000_000L,
            taxBreakdown = AnnualTaxBreakdown(
                pensionIncomeTax = 792_000L,
                capitalGainsTax = 48_000_000L,
            ),
            netCashFlow = 370_400_000L,
            endingBalance = 500_000_000L,
        )

        assertEquals(24_000_000L, snapshot.incomeBreakdown.recurringTotal)
        assertEquals(24_000_000L - 3_600_000L - 792_000L, snapshot.recurringNetCashFlow)
        assertEquals(1_634_000L, snapshot.recurringNetCashFlow / 12)
    }
}
