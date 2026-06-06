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
}

data class SimulationInput(
    val profile: UserProfile,
    val assumptions: EconomicAssumptions,
    val assets: List<Asset>,
    val startYear: Int,
)
