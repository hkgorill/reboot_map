package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import kotlin.math.roundToLong

/** 소득 유형별 간이 소득세 (Phase 5) */
object IncomeTaxEngine {

    data class Input(
        val pensionIncome: Long,
        val employmentIncome: Long,
        val businessIncome: Long,
        val otherTaxableIncome: Long,
        val assumptions: EconomicAssumptions,
    )

    fun calculate(input: Input): AnnualTaxBreakdown {
        val pensionTax = if (input.pensionIncome > 0) {
            (input.pensionIncome * input.assumptions.pensionIncomeTaxRate).roundToLong()
        } else {
            0L
        }
        val employmentTax = if (input.employmentIncome > 0) {
            (input.employmentIncome * input.assumptions.employmentIncomeTaxRate).roundToLong()
        } else {
            0L
        }
        val businessTax = if (input.businessIncome > 0) {
            (input.businessIncome * input.assumptions.businessIncomeTaxRate).roundToLong()
        } else {
            0L
        }
        val otherTax = if (input.otherTaxableIncome > 0) {
            (input.otherTaxableIncome * input.assumptions.generalIncomeTaxRate).roundToLong()
        } else {
            0L
        }

        return AnnualTaxBreakdown(
            pensionIncomeTax = pensionTax,
            employmentIncomeTax = employmentTax,
            businessIncomeTax = businessTax,
            otherIncomeTax = otherTax,
        )
    }
}
