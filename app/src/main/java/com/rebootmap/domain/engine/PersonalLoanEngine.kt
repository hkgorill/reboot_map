package com.rebootmap.domain.engine

import com.rebootmap.domain.model.PersonalLoan
import kotlin.math.roundToLong

object PersonalLoanEngine {

    data class YearResult(
        val totalRepayment: Long,
        val interestPortion: Long,
        val principalPortion: Long,
        val endingBalance: Long,
    )

    /**
     * 1년치 이자·상환 처리.
     * - 월 상환 > 0: 원리금 상환 (이자 우선, 잔여분 원금)
     * - 월 상환 = 0: 이자만 연간 부담, 미납 이자는 잔액에 가산
     */
    fun processYear(
        openingBalance: Long,
        loan: PersonalLoan,
        age: Int,
    ): YearResult? {
        if (openingBalance <= 0) return null
        if (loan.repaymentEndAge > 0 && age > loan.repaymentEndAge) {
            return YearResult(
                totalRepayment = 0L,
                interestPortion = 0L,
                principalPortion = 0L,
                endingBalance = openingBalance,
            )
        }

        val interest = (openingBalance * loan.annualInterestRate).roundToLong()
        val annualPayment = loan.monthlyPayment * 12

        if (annualPayment <= 0) {
            return YearResult(
                totalRepayment = interest,
                interestPortion = interest,
                principalPortion = 0L,
                endingBalance = openingBalance + interest,
            )
        }

        val interestPaid = annualPayment.coerceAtMost(interest)
        val remainder = annualPayment - interestPaid
        val principalPaid = remainder.coerceAtMost(openingBalance)
        val unpaidInterest = interest - interestPaid
        val endingBalance = (openingBalance - principalPaid + unpaidInterest).coerceAtLeast(0L)

        return YearResult(
            totalRepayment = interestPaid + principalPaid,
            interestPortion = interestPaid,
            principalPortion = principalPaid,
            endingBalance = endingBalance,
        )
    }
}
