package com.rebootmap.domain.model

data class UserProfile(
    val currentAge: Int = 40,
    val retirementAge: Int = 60,
    val lifeExpectancy: Int = 90,
    /** 은퇴 전(현재~은퇴 직전) 매년 차감할 월 생활비 */
    val currentMonthlyLivingExpense: Long = 3_000_000L,
    /** 은퇴 후 목표 월 생활비 */
    val monthlyLivingExpense: Long = 3_000_000L,
) {
    fun normalized(): UserProfile {
        val safeCurrentAge = currentAge.coerceIn(18, 100)
        val safeRetirementAge = retirementAge.coerceIn(safeCurrentAge, 100)
        return copy(
            currentAge = safeCurrentAge,
            retirementAge = safeRetirementAge,
            lifeExpectancy = if (lifeExpectancy <= 0) {
                0
            } else {
                lifeExpectancy.coerceIn(safeCurrentAge, 100)
            },
            currentMonthlyLivingExpense = currentMonthlyLivingExpense.coerceAtLeast(0L),
            monthlyLivingExpense = monthlyLivingExpense.coerceAtLeast(0L),
        )
    }
}
