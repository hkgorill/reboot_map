package com.rebootmap.presentation.dashboard

data class DashboardGroupSummary(
    val headline: String,
    val detailLines: List<String> = emptyList(),
    val warning: String? = null,
)
