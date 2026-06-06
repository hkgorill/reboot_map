package com.rebootmap.domain.scenario

/**
 * 거주지 이동 시나리오 (Phase 3 P3-01).
 *
 * 기존 주택 매각 후·전 신규 주택 구입과 2주택 겹침 기간을 모델링합니다.
 */
data class RelocationPlan(
    val enabled: Boolean = false,
    val newHomeValue: Long = 0L,
    val newHomeDebt: Long = 0L,
    val purchaseTiming: PurchaseTiming = PurchaseTiming.SameYearAsSale,
) {
    init {
        require(newHomeValue >= 0) { "신규 주택 시세는 0 이상이어야 합니다." }
        require(newHomeDebt >= 0) { "신규 주택 부채는 0 이상이어야 합니다." }
        require(newHomeDebt <= newHomeValue) { "신규 주택 부채는 시세 이하여야 합니다." }
    }

    val newHomeEquity: Long get() = (newHomeValue - newHomeDebt).coerceAtLeast(0L)

    fun isConfigured(): Boolean = enabled && newHomeValue > 0
}

sealed class PurchaseTiming {
    /** 매각과 동일 연도에 신규 주택 구입 */
    data object SameYearAsSale : PurchaseTiming()

    /** 매각 N년 전에 신규 주택 구입 → 2주택 겹침 기간 */
    data class BeforeSale(val years: Int) : PurchaseTiming() {
        init {
            require(years in 1..5) { "2주택 겹침 연수는 1~5년이어야 합니다." }
        }
    }

    /** 매각 N년 후에 신규 주택 구입 → 무주택(임대) 기간 */
    data class AfterSale(val years: Int) : PurchaseTiming() {
        init {
            require(years in 1..5) { "매각 후 구입 지연 연수는 1~5년이어야 합니다." }
        }
    }
}
