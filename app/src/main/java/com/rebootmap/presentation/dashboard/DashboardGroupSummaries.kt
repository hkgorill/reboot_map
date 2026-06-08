package com.rebootmap.presentation.dashboard

import com.rebootmap.domain.advisory.AssetAdvisoryReport
import com.rebootmap.domain.model.Asset
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.simulation.SimulationUiState
import com.rebootmap.presentation.simulation.displayTitle
import com.rebootmap.domain.portfolio.RealEstateTimingReport
import com.rebootmap.presentation.simulation.summaryText

object DashboardGroupSummaries {

    fun results(
        state: SimulationUiState,
        advisory: AssetAdvisoryReport?,
    ): DashboardGroupSummary {
        val projection = state.projection
            ?: return DashboardGroupSummary(headline = "시뮬레이션 계산 중")

        val depletionLabel = projection.depletionYear?.let { "${it}년 고갈 예상" }
            ?: "기대 수명까지 유지"
        val finalLabel = if (projection.finalBalance < 0) {
            "적자 ${formatKoreanMan(projection.finalBalance)}"
        } else {
            "최종 ${formatKoreanMan(projection.finalBalance)}"
        }
        val scorePart = advisory?.let { "${it.score}점(${it.gradeLabel})" } ?: "총평 산정 중"

        return DashboardGroupSummary(
            headline = "$scorePart · $depletionLabel · $finalLabel",
            detailLines = listOfNotNull(
                advisory?.headline,
                projection.deficitYears.takeIf { it.isNotEmpty() }?.let {
                    "수입 부족 ${projection.yearSpanSummary(it).headline}"
                },
            ),
        )
    }

    fun lifeHousing(state: SimulationUiState): DashboardGroupSummary {
        val expenses = state.lumpSumExpenses
        val milestonePart = when {
            expenses.isEmpty() -> "목돈 없음"
            else -> "목돈 ${expenses.size}건(${formatKoreanMan(expenses.sumOf { it.amount })})"
        }
        return DashboardGroupSummary(
            headline = milestonePart,
            detailLines = expenses.sortedBy { it.year }.take(3).map { expense ->
                "${expense.year}년 ${expense.displayLabel()} ${formatKoreanMan(expense.amount)}"
            },
        )
    }

    fun basicInfo(state: SimulationUiState): DashboardGroupSummary {
        val profile = state.profile
        val assumptions = state.assumptions
        val expenseBase = when (assumptions.livingExpenseInflationBase) {
            com.rebootmap.domain.model.LivingExpenseInflationBase.RETIREMENT_AGE -> "은퇴 시점 물가"
            com.rebootmap.domain.model.LivingExpenseInflationBase.SIMULATION_START -> "현재부터 물가"
        }
        val taxOn = buildList {
            if (assumptions.propertyTaxEnabled) add("재산·종부세")
            if (assumptions.healthInsuranceEnabled) add("건보")
        }.joinToString("·").ifEmpty { "세금 OFF" }

        return DashboardGroupSummary(
            headline = "${profile.currentAge}세 → ${profile.retirementAge}세 은퇴 · 월 ${formatKoreanMan(profile.monthlyLivingExpense)}",
            detailLines = listOf(
                "기대 수명 ${profile.lifeExpectancy}세 · 물가 ${"%.1f".format(assumptions.inflationRate * 100)}% · $expenseBase",
                taxOn,
            ),
        )
    }

    fun realEstate(state: SimulationUiState, timing: RealEstateTimingReport? = null): DashboardGroupSummary {
        val estates = state.assets.filterIsInstance<Asset.RealEstate>()
        val configured = estates.filter { it.summaryText() != "미입력" }
        val totalEquity = configured.sumOf { it.netEquity }
        val unconfigured = estates.size - configured.size

        val headline = when {
            configured.isEmpty() -> "미입력 · ${estates.size}건"
            timing != null && timing.overlapYears.isNotEmpty() ->
                "${configured.size}건 · 2주택 ${timing.overlapYears.size}년 · ${timing.headline}"
            else -> "${configured.size}건 · 순자산 합 ${formatKoreanMan(totalEquity)}"
        }

        return DashboardGroupSummary(
            headline = headline,
            detailLines = estates.mapIndexed { index, estate ->
                "${estate.displayTitle(index, estates.size)}: ${estate.summaryText()}"
            },
            warning = unconfigured.takeIf { it > 0 }?.let { "미입력 ${it}건" },
        )
    }

    fun incomePension(state: SimulationUiState): DashboardGroupSummary {
        val assets = state.assets.filter { it !is Asset.RealEstate }
        val configured = assets.filter { it.summaryText() != "미입력" }
        val unconfigured = assets.size - configured.size

        val highlights = configured.map { asset ->
            "${asset.displayTitle()}: ${asset.summaryText()}"
        }.take(3)

        return DashboardGroupSummary(
            headline = "${configured.size}/${assets.size} 입력",
            detailLines = highlights,
            warning = unconfigured.takeIf { it > 0 }?.let { "미입력 ${it}건" },
        )
    }

    fun debt(state: SimulationUiState): DashboardGroupSummary {
        val loans = state.personalLoans.filter { it.isSimulationReady() }
        if (loans.isEmpty()) {
            val draftCount = state.personalLoans.count { it.balance > 0 || it.monthlyPayment > 0 }
            return DashboardGroupSummary(
                headline = if (draftCount > 0) "입력 중 · ${draftCount}건" else "부채 없음",
            )
        }

        val totalBalance = loans.sumOf { it.balance }
        val totalMonthly = loans.sumOf { it.monthlyPayment }

        val headline = buildString {
            append("${loans.size}건 · 잔액 ${formatKoreanMan(totalBalance)}")
            if (totalMonthly > 0) append(" · 월 ${formatKoreanMan(totalMonthly)}")
        }

        return DashboardGroupSummary(
            headline = headline,
            detailLines = loans.mapIndexed { index, loan ->
                "${loan.displayTitle(index, loans.size)}: ${loan.summaryText()}"
            },
        )
    }
}
