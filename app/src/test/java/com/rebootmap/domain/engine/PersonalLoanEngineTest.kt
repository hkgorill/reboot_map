package com.rebootmap.domain.engine

import com.rebootmap.domain.model.PersonalLoan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalLoanEngineTest {

    @Test
    fun `월 상환으로 원금과 이자가 분리된다`() {
        val loan = PersonalLoan(balance = 10_000_000L, annualInterestRate = 0.12, monthlyPayment = 500_000L)
        val result = PersonalLoanEngine.processYear(10_000_000L, loan, age = 50)!!

        assertEquals(1_200_000L, result.interestPortion)
        assertEquals(4_800_000L, result.principalPortion)
        assertEquals(6_000_000L, result.totalRepayment)
        assertEquals(5_200_000L, result.endingBalance)
    }

    @Test
    fun `월 상환 없으면 이자만 부담하고 잔액이 늘어난다`() {
        val loan = PersonalLoan(balance = 10_000_000L, annualInterestRate = 0.06, monthlyPayment = 0L)
        val result = PersonalLoanEngine.processYear(10_000_000L, loan, age = 50)!!

        assertEquals(600_000L, result.totalRepayment)
        assertEquals(10_600_000L, result.endingBalance)
    }

    @Test
    fun `상환 종료 연령 이후 상환은 중단된다`() {
        val loan = PersonalLoan(
            balance = 10_000_000L,
            annualInterestRate = 0.06,
            monthlyPayment = 500_000L,
            repaymentEndAge = 60,
        )
        val result = PersonalLoanEngine.processYear(10_000_000L, loan, age = 61)!!

        assertEquals(0L, result.totalRepayment)
        assertEquals(10_000_000L, result.endingBalance)
    }

    @Test
    fun `잔액이 소진되면 0까지 감소한다`() {
        val loan = PersonalLoan(balance = 2_000_000L, annualInterestRate = 0.06, monthlyPayment = 500_000L)
        val result = PersonalLoanEngine.processYear(2_000_000L, loan, age = 50)!!

        assertTrue(result.endingBalance <= 2_000_000L)
        assertEquals(0L, result.endingBalance)
    }
}
