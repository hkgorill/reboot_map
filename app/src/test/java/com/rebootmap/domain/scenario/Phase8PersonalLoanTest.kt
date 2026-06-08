package com.rebootmap.domain.scenario

import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8PersonalLoanTest {

    private val engine = CashFlowEngine()

    @Test
    fun `P8-T01 - 부채 잔액은 총자산에서 차감된다`() {
        val profile = UserProfile(
            currentAge = 50,
            retirementAge = 60,
            lifeExpectancy = 52,
            monthlyLivingExpense = 0L,
            currentMonthlyLivingExpense = 0L,
        )
        val assets = listOf(Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.0))
        val loan = PersonalLoan(balance = 30_000_000L, annualInterestRate = 0.0, monthlyPayment = 0L)

        val withLoan = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = 2026,
                personalLoans = listOf(loan),
            ),
        )
        val baseline = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = 2026,
            ),
        )

        val first = withLoan.yearlySnapshots.first()
        val base = baseline.yearlySnapshots.first()
        assertEquals(base.endingBalance - 30_000_000L, first.endingBalance)
        assertEquals(30_000_000L, first.personalLoanBalance)
    }

    @Test
    fun `P8-T02 - 월 상환은 순자산과 현금흐름에 반영된다`() {
        val profile = UserProfile(
            currentAge = 50,
            retirementAge = 60,
            lifeExpectancy = 52,
            monthlyLivingExpense = 0L,
            currentMonthlyLivingExpense = 0L,
        )
        val assets = listOf(Asset.Investment(currentValue = 50_000_000L, annualReturnRate = 0.0))
        val loan = PersonalLoan(balance = 10_000_000L, annualInterestRate = 0.0, monthlyPayment = 500_000L)
        val assumptions = EconomicAssumptions(
            healthInsuranceEnabled = false,
            propertyTaxEnabled = false,
            comprehensiveRealEstateTaxEnabled = false,
        )

        val projection = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = assumptions,
                assets = assets,
                startYear = 2026,
                personalLoans = listOf(loan),
            ),
        )

        val first = projection.yearlySnapshots.first()
        assertEquals(6_000_000L, first.annualLoanRepayment)
        assertEquals(4_000_000L, first.personalLoanBalance)
        assertEquals(40_000_000L, first.endingBalance)
    }

    @Test
    fun `P8-T03 - personalLoans persist through mapper`() {
        val original = SimulationUiState.afterOnboarding(45, 60, 3_000_000L).copy(
            personalLoans = listOf(
                PersonalLoan(
                    id = "personal_loan_1",
                    balance = 20_000_000L,
                    annualInterestRate = 0.08,
                    monthlyPayment = 300_000L,
                ),
            ),
        )
        val restored = SimulationStateMapper.toUiState(SimulationStateMapper.toPersisted(original))
        assertEquals(1, restored.personalLoans.size)
        assertEquals(20_000_000L, restored.personalLoans.first().balance)
        assertEquals(300_000L, restored.personalLoans.first().monthlyPayment)
    }

    @Test
    fun `P8-T04 - 부채가 있으면 무이자 상환만으로도 고갈이 빨라질 수 있다`() {
        val profile = UserProfile(currentAge = 60, retirementAge = 60, lifeExpectancy = 65, monthlyLivingExpense = 1_000_000L)
        val assets = listOf(Asset.Investment(currentValue = 20_000_000L, annualReturnRate = 0.0))
        val loan = PersonalLoan(balance = 5_000_000L, annualInterestRate = 0.0, monthlyPayment = 1_000_000L)

        val withLoan = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = 2026,
                personalLoans = listOf(loan),
            ),
        )
        val withoutLoan = engine.project(
            SimulationInput(
                profile = profile,
                assumptions = EconomicAssumptions(),
                assets = assets,
                startYear = 2026,
            ),
        )

        assertTrue(
            (withLoan.depletionYear ?: Int.MAX_VALUE) <= (withoutLoan.depletionYear ?: Int.MAX_VALUE),
        )
    }
}
