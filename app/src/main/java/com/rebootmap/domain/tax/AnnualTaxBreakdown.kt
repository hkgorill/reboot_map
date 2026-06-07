package com.rebootmap.domain.tax

/**
 * 연간 세금·부과 breakdown (Phase 5).
 * [totalTax] = 소득세·건보·양도세 합계 (보유세는 [AnnualHoldingCost]).
 */
data class AnnualTaxBreakdown(
    val pensionIncomeTax: Long = 0L,
    val employmentIncomeTax: Long = 0L,
    val businessIncomeTax: Long = 0L,
    val otherIncomeTax: Long = 0L,
    val capitalGainsTax: Long = 0L,
    val healthInsurance: Long = 0L,
    val longTermCare: Long = 0L,
) {
    val totalTax: Long
        get() = pensionIncomeTax + employmentIncomeTax + businessIncomeTax +
            otherIncomeTax + capitalGainsTax + healthInsurance + longTermCare
}

/** 연간 수입 breakdown — [total]는 [com.rebootmap.domain.model.YearSnapshot.annualIncome]과 일치 */
data class AnnualIncomeBreakdown(
    val nationalPension: Long = 0L,
    val severancePension: Long = 0L,
    val personalPension: Long = 0L,
    val housingPension: Long = 0L,
    val employmentIncome: Long = 0L,
    val businessIncome: Long = 0L,
    val otherFixedIncome: Long = 0L,
    val realEstateSale: Long = 0L,
    val cashSavingsMaturity: Long = 0L,
    val yellowUmbrellaPayout: Long = 0L,
) {
    val total: Long
        get() = nationalPension + severancePension + personalPension + housingPension +
            employmentIncome + businessIncome + otherFixedIncome +
            realEstateSale + cashSavingsMaturity + yellowUmbrellaPayout
}

data class AnnualHoldingCost(
    val propertyTax: Long = 0L,
    val residentialPropertyTax: Long = 0L,
    val nonResidentialPropertyTax: Long = 0L,
    val comprehensiveRealEstateTax: Long = 0L,
) {
    val total: Long get() = propertyTax + comprehensiveRealEstateTax
}
