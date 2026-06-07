package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.TaxDefaults
import com.rebootmap.domain.tax.PropertyHoldingTaxEngine.EstateLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5TaxEngineTest {

    @Test
    fun `T30 - 직장 소득은 근로소득세율이 적용된다`() {
        val breakdown = IncomeTaxEngine.calculate(
            IncomeTaxEngine.Input(
                pensionIncome = 0L,
                employmentIncome = 100_000_000L,
                businessIncome = 0L,
                otherTaxableIncome = 0L,
                assumptions = EconomicAssumptions(),
            ),
        )

        assertEquals(6_000_000L, breakdown.employmentIncomeTax)
        assertEquals(0L, breakdown.businessIncomeTax)
    }

    @Test
    fun `T31 - 사업 소득은 사업소득세율이 적용된다`() {
        val breakdown = IncomeTaxEngine.calculate(
            IncomeTaxEngine.Input(
                pensionIncome = 0L,
                employmentIncome = 0L,
                businessIncome = 100_000_000L,
                otherTaxableIncome = 0L,
                assumptions = EconomicAssumptions(),
            ),
        )

        assertEquals(15_000_000L, breakdown.businessIncomeTax)
    }

    @Test
    fun `T32 - 부동산 보유 시 재산세와 종부세가 산출된다`() {
        val holding = PropertyHoldingTaxEngine.calculate(
            PropertyHoldingTaxEngine.Input(
                estates = listOf(EstateLine(netEquity = 800_000_000L)),
                assumptions = EconomicAssumptions(),
            ),
        )

        assertEquals(2_000_000L, holding.propertyTax)
        assertEquals(1_200_000L, holding.comprehensiveRealEstateTax)
        assertEquals(3_200_000L, holding.total)
    }

    @Test
    fun `T33 - 은퇴 후 건강보험료가 산출된다`() {
        val result = HealthInsurancePremiumEngine.calculate(
            HealthInsurancePremiumEngine.Input(
                monthlyIncomeBasis = 3_000_000L,
                financialAssets = 200_000_000L,
                realEstateNetEquity = 500_000_000L,
                age = 65,
                retirementAge = 60,
                assumptions = EconomicAssumptions(),
            ),
        )

        assertTrue(result.annualHealthInsurance > 0)
        assertTrue(result.annualLongTermCare > 0)
    }

    @Test
    fun `T34 - fixedIncome 마이그레이션 필드는 mapper에서 employment로 이관된다`() {
        val persisted = com.rebootmap.data.model.SimulationPersistedState(
            fixedIncomeMonthly = 3_000_000L,
            fixedIncomeStartAge = 48,
            fixedIncomeEndAge = 60,
        )
        val resolved = com.rebootmap.data.mapper.SimulationStateMapper.resolveIncomeFields(persisted)

        assertEquals(3_000_000L, resolved.employmentMonthly)
        assertEquals(48, resolved.employmentStartAge)
        assertEquals(60, resolved.employmentEndAge)
    }

    @Test
    fun `T35 - breakdown 합계가 totalTax와 일치한다`() {
        val breakdown = AnnualTaxBreakdown(
            pensionIncomeTax = 1_000_000L,
            employmentIncomeTax = 600_000L,
            businessIncomeTax = 300_000L,
            otherIncomeTax = 200_000L,
            capitalGainsTax = 50_000L,
            healthInsurance = 2_640_000L,
            longTermCare = 341_880L,
        )

        assertEquals(5_131_880L, breakdown.totalTax)
    }

    @Test
    fun `보유세 OFF 시 재산세와 종부세가 0이다`() {
        val holding = PropertyHoldingTaxEngine.calculate(
            PropertyHoldingTaxEngine.Input(
                estates = listOf(EstateLine(netEquity = 1_000_000_000L)),
                assumptions = EconomicAssumptions(
                    propertyTaxEnabled = false,
                    comprehensiveRealEstateTaxEnabled = false,
                ),
            ),
        )

        assertEquals(0L, holding.total)
    }
}
