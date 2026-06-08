package com.rebootmap.domain.model

object TaxDefaults {
    const val EMPLOYMENT_INCOME_TAX_RATE = 0.06
    const val BUSINESS_INCOME_TAX_RATE = 0.15
    const val PROPERTY_TAX_RATE = 0.0025
    const val NON_RESIDENTIAL_PROPERTY_TAX_RATE = 0.004
    const val COMPREHENSIVE_TAX_THRESHOLD = 600_000_000L
    const val COMPREHENSIVE_TAX_RATE = 0.006
    const val LONG_TERM_CARE_RATE = 0.1295

    /** 주택 취득세 — 1주택 간이 (취득가액 대비, 지방교육세 포함 근사) */
    const val ACQUISITION_TAX_RATE_PRIMARY = 0.011

    /** 추가 주택(2주택 등) 취득세 간이 */
    const val ACQUISITION_TAX_RATE_ADDITIONAL = 0.08

    /** 부동산 중개보수율 (매수·매도 각각, VAT 포함 근사) */
    const val BROKERAGE_FEE_RATE = 0.005

    /** 일시적 1가구 2주택 — 신규 취득 후 구주택 처분 허용 연수 */
    const val TEMPORARY_TWO_HOME_SALE_YEARS = 3
}
