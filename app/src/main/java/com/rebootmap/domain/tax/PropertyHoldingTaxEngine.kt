package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import kotlin.math.roundToLong

/** 부동산 보유 세금 간이 계산 (재산세·종합부동산세) */
object PropertyHoldingTaxEngine {

    data class Input(
        val netEquity: Long,
        val assumptions: EconomicAssumptions,
    )

    fun calculate(input: Input): AnnualHoldingCost {
        val equity = input.netEquity.coerceAtLeast(0)
        if (equity == 0L) return AnnualHoldingCost()

        val assumptions = input.assumptions
        val propertyTax = if (assumptions.propertyTaxEnabled) {
            (equity * assumptions.propertyTaxRate).roundToLong()
        } else {
            0L
        }

        val comprehensiveTax = if (assumptions.comprehensiveRealEstateTaxEnabled) {
            val excess = (equity - assumptions.comprehensiveTaxThreshold).coerceAtLeast(0)
            (excess * assumptions.comprehensiveTaxRate).roundToLong()
        } else {
            0L
        }

        return AnnualHoldingCost(
            propertyTax = propertyTax,
            comprehensiveRealEstateTax = comprehensiveTax,
        )
    }
}
