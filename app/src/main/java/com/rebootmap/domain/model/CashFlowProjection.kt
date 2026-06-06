package com.rebootmap.domain.model

data class YearSnapshot(
    val year: Int,
    val age: Int,
    val totalAssets: Long,
    val annualIncome: Long,
    val annualExpense: Long,
    val annualTax: Long,
    val netCashFlow: Long,
    val endingBalance: Long,
)

data class CashFlowProjection(
    val yearlySnapshots: List<YearSnapshot>,
    val depletionYear: Int?,
    val deficitYears: List<Int>,
) {
    val finalBalance: Long
        get() = yearlySnapshots.lastOrNull()?.endingBalance ?: 0L

    val yearsUntilDepletion: Int?
        get() = depletionYear?.let { year ->
            yearlySnapshots.firstOrNull()?.year?.let { start -> year - start }
        }

    /**
     * 은퇴 후 총자산이 전년 대비 줄어든 연도.
     * 투자 수익률 등 자산 성장 요인이 반영되어 타임라인에 사용합니다.
     */
    fun assetDeclineYears(retirementAge: Int): List<Int> = buildList {
        val snapshots = yearlySnapshots
        for (index in 1 until snapshots.size) {
            val previous = snapshots[index - 1]
            val current = snapshots[index]
            if (current.age >= retirementAge && current.endingBalance < previous.endingBalance) {
                add(current.year)
            }
        }
    }

    fun formatYearSpan(years: List<Int>): String {
        val summary = yearSpanSummary(years)
        return if (summary.rangeLine == null) {
            summary.headline
        } else {
            "${summary.headline} ${summary.rangeLine}"
        }
    }

    fun yearSpanSummary(years: List<Int>): YearSpanSummary {
        if (years.isEmpty()) return YearSpanSummary(headline = "없음", rangeLine = null)
        val first = yearlySnapshots.first { it.year == years.first() }
        val last = yearlySnapshots.first { it.year == years.last() }
        return YearSpanSummary(
            headline = "${years.size}년 · ${first.age}~${last.age}세",
            rangeLine = "(${years.first()}~${years.last()})",
        )
    }
}

data class YearSpanSummary(
    val headline: String,
    val rangeLine: String?,
)

data class SimulationInput(
    val profile: UserProfile,
    val assumptions: EconomicAssumptions,
    val assets: List<Asset>,
    val startYear: Int,
)
