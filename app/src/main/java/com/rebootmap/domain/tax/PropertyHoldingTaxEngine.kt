package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.RealEstateDefaults
import kotlin.math.roundToLong

/** 부동산 보유 세금 간이 계산 (재산세·종합부동산세) */
object PropertyHoldingTaxEngine {

    data class EstateLine(
        val netEquity: Long,
        val category: RealEstateCategory = RealEstateCategory.PRIMARY_RESIDENCE,
    )

    data class Input(
        val estates: List<EstateLine>,
        val assumptions: EconomicAssumptions,
    )

    fun calculate(input: Input): AnnualHoldingCost {
        if (input.estates.isEmpty() || !input.assumptions.propertyTaxEnabled &&
            !input.assumptions.comprehensiveRealEstateTaxEnabled
        ) {
            return AnnualHoldingCost()
        }

        var residentialTax = 0L
        var nonResidentialTax = 0L
        var totalEquity = 0L

        input.estates.forEach { line ->
            val equity = line.netEquity.coerceAtLeast(0)
            if (equity == 0L) return@forEach
            totalEquity += equity
            if (input.assumptions.propertyTaxEnabled) {
                val rate = RealEstateDefaults.propertyTaxRate(line.category, input.assumptions)
                val tax = (equity * rate).roundToLong()
                when (line.category) {
                    RealEstateCategory.PRIMARY_RESIDENCE -> residentialTax += tax
                    RealEstateCategory.NON_RESIDENTIAL -> nonResidentialTax += tax
                }
            }
        }

        val comprehensiveTax = if (input.assumptions.comprehensiveRealEstateTaxEnabled && totalEquity > 0) {
            val excess = (totalEquity - input.assumptions.comprehensiveTaxThreshold).coerceAtLeast(0)
            (excess * input.assumptions.comprehensiveTaxRate).roundToLong()
        } else {
            0L
        }

        return AnnualHoldingCost(
            propertyTax = residentialTax + nonResidentialTax,
            residentialPropertyTax = residentialTax,
            nonResidentialPropertyTax = nonResidentialTax,
            comprehensiveRealEstateTax = comprehensiveTax,
        )
    }
}
