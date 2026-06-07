package com.rebootmap.data.model

import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.PersonalLoanCategory
import kotlinx.serialization.Serializable

@Serializable
data class PersistedPersonalLoan(
    val id: String = "personal_loan_1",
    val balance: Long = 0L,
    val annualInterestRate: Double = 0.06,
    val monthlyPayment: Long = 0L,
    val repaymentEndAge: Int = 0,
    val category: String = "BANK_CREDIT",
) {
    fun toDomain(): PersonalLoan = PersonalLoan(
        id = id,
        balance = balance,
        annualInterestRate = annualInterestRate,
        monthlyPayment = monthlyPayment,
        repaymentEndAge = repaymentEndAge,
        category = category.toPersonalLoanCategory(),
    )

    companion object {
        fun fromDomain(loan: PersonalLoan): PersistedPersonalLoan = PersistedPersonalLoan(
            id = loan.id,
            balance = loan.balance,
            annualInterestRate = loan.annualInterestRate,
            monthlyPayment = loan.monthlyPayment,
            repaymentEndAge = loan.repaymentEndAge,
            category = loan.category.name,
        )
    }
}

private fun String.toPersonalLoanCategory(): PersonalLoanCategory =
    runCatching { PersonalLoanCategory.valueOf(this) }.getOrDefault(PersonalLoanCategory.OTHER)
