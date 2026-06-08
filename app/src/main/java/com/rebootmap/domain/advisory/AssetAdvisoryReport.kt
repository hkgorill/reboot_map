package com.rebootmap.domain.advisory

/** 자산운용 총평 — 교육·참고용 (투자 권유 아님) */
data class AssetAdvisoryReport(
    val score: Int,
    val gradeLabel: String,
    val headline: String,
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val watchPoints: List<String>,
)
