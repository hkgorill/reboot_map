package com.rebootmap.domain.scenario

/** 주거 로드맵(이주) 시 해당 연도 상태 — UI 타임라인·강조용 */
data class RelocationYearFlags(
    val active: Boolean = false,
    /** 매각 전 신규 구입 — 2주택 겹침 */
    val isTwoHomeOverlap: Boolean = false,
    /** 매각 후 구입 전 — 무주택(임대) 구간 */
    val isGapPeriod: Boolean = false,
)
