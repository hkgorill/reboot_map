package com.rebootmap.domain.model

/** 신용·차용 부채 유형 */
enum class PersonalLoanCategory {
    BANK_CREDIT,
    PRIVATE_LOAN,
    OTHER,
}

object PersonalLoanDefaults {
    const val MAX_COUNT = 5
    const val DEFAULT_INTEREST_RATE = 0.06

    fun empty(id: String = "personal_loan_1"): PersonalLoan = PersonalLoan(
        id = id,
        balance = 0L,
        annualInterestRate = DEFAULT_INTEREST_RATE,
        monthlyPayment = 0L,
    )

    fun nextId(existing: List<PersonalLoan>): String? {
        val used = existing.map { it.id }.toSet()
        for (index in 1..MAX_COUNT) {
            val id = "personal_loan_$index"
            if (id !in used) return id
        }
        return null
    }
}

/**
 * 개인 신용·차용 부채 (주택담보 대출과 별도).
 * - [balance]: 현재 원금
 * - [monthlyPayment]: 월 원리금 상환액 (0이면 이자만 연간 부담으로 간주)
 */
data class PersonalLoan(
    val id: String = "personal_loan_1",
    val balance: Long,
    val annualInterestRate: Double = PersonalLoanDefaults.DEFAULT_INTEREST_RATE,
    val monthlyPayment: Long = 0L,
    /** 0 = 잔액 소진 시까지. 양수면 해당 연령까지 상환 */
    val repaymentEndAge: Int = 0,
    val category: PersonalLoanCategory = PersonalLoanCategory.BANK_CREDIT,
) {
    init {
        require(balance >= 0) { "대출 잔액은 0 이상이어야 합니다." }
        require(annualInterestRate in 0.0..0.5) { "연 이자율은 0~50% 사이여야 합니다." }
        require(monthlyPayment >= 0) { "월 상환액은 0 이상이어야 합니다." }
        require(repaymentEndAge in 0..100) { "상환 종료 연령이 유효하지 않습니다." }
    }

    fun isSimulationReady(): Boolean = balance > 0
}
