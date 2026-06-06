package com.rebootmap.domain.model

data class EconomicAssumptions(
    val inflationRate: Double = 0.02,
    val pensionIncomeTaxRate: Double = 0.033,
    val generalIncomeTaxRate: Double = 0.15,
) {
    init {
        require(inflationRate in 0.0..0.2) { "물가상승률은 0~20% 사이여야 합니다." }
        require(pensionIncomeTaxRate in 0.0..1.0) { "연금소득세율은 0~100% 사이여야 합니다." }
        require(generalIncomeTaxRate in 0.0..1.0) { "일반 소득세율은 0~100% 사이여야 합니다." }
    }
}
