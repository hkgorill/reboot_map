package com.rebootmap.domain.scenario

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateCategory

/**
 * 주거 로드맵 — 매각·구입 부동산 연결 및 2주택·무주택 구간 시뮬 (Phase 7).
 */
data class RelocationPlan(
    val enabled: Boolean = false,
    /** 매각할 부동산 id ([Asset.RealEstate.id]) */
    val sellEstateId: String = "",
    /** 이주 후 거주 부동산 id. 비어 있으면 [newHomeValue]/[newHomeDebt] 가상 신규 주택 */
    val buyEstateId: String = "",
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

    val usesLinkedBuyEstate: Boolean get() = buyEstateId.isNotBlank()

    fun isConfigured(estates: List<Asset.RealEstate> = emptyList()): Boolean {
        if (!enabled) return false
        if (newHomeValue <= 0 && buyEstateId.isBlank()) return false
        if (estates.isEmpty()) {
            return sellEstateId.isNotBlank() || newHomeValue > 0
        }
        return resolveSellEstate(estates)?.saleYear != null
    }

    fun resolveSellEstate(estates: List<Asset.RealEstate>): Asset.RealEstate? {
        if (sellEstateId.isNotBlank()) {
            return estates.find { it.id == sellEstateId }
        }
        return estates.firstOrNull {
            it.category == RealEstateCategory.PRIMARY_RESIDENCE && it.saleYear != null
        } ?: estates.firstOrNull { it.saleYear != null }
    }

    fun resolveBuyEstate(estates: List<Asset.RealEstate>): Asset.RealEstate? =
        buyEstateId.takeIf { it.isNotBlank() }?.let { id -> estates.find { it.id == id } }

    /** 매각가 대비 신규 주택 시세 60% 다운사이징 프리셋 */
    fun withDownsizingPreset(sellEstate: Asset.RealEstate): RelocationPlan = copy(
        buyEstateId = "",
        newHomeValue = sellEstate.currentValue * 60 / 100,
        newHomeDebt = 0L,
        purchaseTiming = PurchaseTiming.SameYearAsSale,
    )
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
