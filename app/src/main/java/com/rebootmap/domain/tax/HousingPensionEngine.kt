package com.rebootmap.domain.tax

import kotlin.math.roundToLong

/**
 * 주택연금(역모기지) 월 수령액 간이 계산 (Phase 3 P3-04).
 *
 * 한국주택금융공사 주택연금 LTV·연령별 한도를 단순화해 적용합니다.
 */
object HousingPensionEngine {

    const val MIN_START_AGE = 55

    data class Input(
        val homeEquity: Long,
        val startAge: Int,
        val currentAge: Int,
        val lifeExpectancy: Int,
    )

    data class Result(
        val monthlyPayout: Long,
        val ltvRate: Double,
        val payoutYears: Int,
    )

    fun calculateMonthly(input: Input): Result {
        val homeEquity = input.homeEquity.coerceAtLeast(0)
        if (homeEquity == 0L || input.currentAge < input.startAge) {
            return Result(monthlyPayout = 0L, ltvRate = 0.0, payoutYears = 0)
        }

        val ltv = ltvRateForAge(input.startAge)
        val payoutYears = (input.lifeExpectancy - input.startAge).coerceAtLeast(1)
        val loanPrincipal = (homeEquity * ltv).roundToLong()
        val monthly = (loanPrincipal / payoutYears / 12.0).roundToLong()

        return Result(
            monthlyPayout = monthly,
            ltvRate = ltv,
            payoutYears = payoutYears,
        )
    }

    /** 연령별 LTV 한도 (HUG 주택연금 근사) */
    fun ltvRateForAge(age: Int): Double = when {
        age < 55 -> 0.0
        age < 60 -> 0.50
        age < 65 -> 0.55
        age < 70 -> 0.60
        else -> 0.65
    }
}
