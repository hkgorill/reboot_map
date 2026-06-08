package com.rebootmap.presentation.dashboard

import com.rebootmap.domain.advisory.AssetAdvisoryReport
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardGroupSummariesTest {

    @Test
    fun `결과 그룹 요약에 점수와 고갈 여부가 포함된다`() {
        val projection = CashFlowProjection(
            yearlySnapshots = listOf(snap(2026, 60, 100_000_000L)),
            depletionYear = null,
            deficitYears = emptyList(),
        )
        val state = SimulationUiState(
            isOnboardingCompleted = true,
            profile = UserProfile(currentAge = 58, retirementAge = 60, lifeExpectancy = 85),
            projection = projection,
        )
        val advisory = AssetAdvisoryReport(
            score = 85,
            gradeLabel = "양호",
            headline = "안정적",
            summary = "요약",
            strengths = emptyList(),
            weaknesses = emptyList(),
            watchPoints = emptyList(),
        )

        val summary = DashboardGroupSummaries.results(state, advisory)

        assertTrue(summary.headline.contains("85점"))
        assertTrue(summary.headline.contains("유지"))
    }

    @Test
    fun `부동산 그룹 요약에 건수와 순자산이 포함된다`() {
        val estates = listOf(
            Asset.RealEstate(id = "e1", currentValue = 500_000_000L, debtAmount = 100_000_000L, saleYear = null),
            Asset.RealEstate(id = "e2", currentValue = 0L, debtAmount = 0L, saleYear = null),
        )
        val state = SimulationUiState(
            isOnboardingCompleted = true,
            assets = estates + SimulationUiState().assets.filter { it !is Asset.RealEstate },
        )

        val summary = DashboardGroupSummaries.realEstate(state)

        assertTrue(summary.headline.contains("1건"))
        assertTrue(summary.warning?.contains("미입력") == true)
    }

    @Test
    fun `부채 없을 때 부채 그룹은 부채 없음으로 표시한다`() {
        val summary = DashboardGroupSummaries.debt(SimulationUiState(isOnboardingCompleted = true))

        assertTrue(summary.headline.contains("부채 없음"))
    }

    private fun snap(year: Int, age: Int, balance: Long) = YearSnapshot(
        year = year,
        age = age,
        liquidAssets = balance,
        illiquidAssets = 0L,
        totalAssets = balance,
        annualIncome = 0L,
        annualExpense = 0L,
        annualLivingExpense = 0L,
        annualHoldingCost = AnnualHoldingCost(),
        annualTax = 0L,
        taxBreakdown = AnnualTaxBreakdown(),
        netCashFlow = 0L,
        endingBalance = balance,
    )
}
