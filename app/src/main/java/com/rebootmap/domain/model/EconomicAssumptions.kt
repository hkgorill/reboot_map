package com.rebootmap.domain.model

data class EconomicAssumptions(
    val inflationRate: Double = 0.02,
    val livingExpenseInflationBase: LivingExpenseInflationBase = LivingExpenseInflationBase.RETIREMENT_AGE,
    val pensionIncomeTaxRate: Double = 0.033,
    val employmentIncomeTaxRate: Double = TaxDefaults.EMPLOYMENT_INCOME_TAX_RATE,
    val businessIncomeTaxRate: Double = TaxDefaults.BUSINESS_INCOME_TAX_RATE,
    val generalIncomeTaxRate: Double = 0.15,
    val propertyTaxEnabled: Boolean = true,
    val propertyTaxRate: Double = TaxDefaults.PROPERTY_TAX_RATE,
    val comprehensiveRealEstateTaxEnabled: Boolean = true,
    val comprehensiveTaxThreshold: Long = TaxDefaults.COMPREHENSIVE_TAX_THRESHOLD,
    val comprehensiveTaxRate: Double = TaxDefaults.COMPREHENSIVE_TAX_RATE,
    val healthInsuranceEnabled: Boolean = true,
    val longTermCareRate: Double = TaxDefaults.LONG_TERM_CARE_RATE,
) {
    init {
        require(inflationRate in 0.0..0.2) { "물가상승률은 0~20% 사이여야 합니다." }
        require(pensionIncomeTaxRate in 0.0..1.0) { "연금소득세율은 0~100% 사이여야 합니다." }
        require(employmentIncomeTaxRate in 0.0..1.0) { "근로소득세율은 0~100% 사이여야 합니다." }
        require(businessIncomeTaxRate in 0.0..1.0) { "사업소득세율은 0~100% 사이여야 합니다." }
        require(generalIncomeTaxRate in 0.0..1.0) { "기타 소득세율은 0~100% 사이여야 합니다." }
        require(propertyTaxRate in 0.0..0.1) { "재산세율이 유효하지 않습니다." }
        require(comprehensiveTaxThreshold >= 0L) { "종부세 공제 임계값은 0 이상이어야 합니다." }
        require(comprehensiveTaxRate in 0.0..0.1) { "종부세율이 유효하지 않습니다." }
        require(longTermCareRate in 0.0..1.0) { "장기요양 요율이 유효하지 않습니다." }
    }
}
