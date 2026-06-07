package com.rebootmap.domain.tax

import com.rebootmap.domain.model.EconomicAssumptions
import kotlin.math.roundToLong

/**
 * 지역가입자 건강보험료·장기요양 간이 계산 (Phase 5).
 * 2024 요율표를 구간 고정값으로 단순화.
 */
object HealthInsurancePremiumEngine {

    data class Input(
        val monthlyIncomeBasis: Long,
        val financialAssets: Long,
        val realEstateNetEquity: Long,
        val age: Int,
        val retirementAge: Int,
        val assumptions: EconomicAssumptions,
    )

    data class Result(
        val annualHealthInsurance: Long,
        val annualLongTermCare: Long,
    )

    fun calculate(input: Input): Result {
        if (!input.assumptions.healthInsuranceEnabled) {
            return Result(annualHealthInsurance = 0L, annualLongTermCare = 0L)
        }
        // 은퇴 전 직장가입자 가정 — 은퇴 연령 이후 지역가입자 산정
        if (input.age < input.retirementAge) {
            return Result(annualHealthInsurance = 0L, annualLongTermCare = 0L)
        }

        val monthlyIncome = input.monthlyIncomeBasis.coerceAtLeast(0)
        val incomePremium = incomePremiumMonthly(monthlyIncome)
        val assetPremium = assetPremiumMonthly(
            financialAssets = input.financialAssets,
            realEstateNetEquity = input.realEstateNetEquity,
        )
        val monthlyTotal = (incomePremium + assetPremium).coerceIn(MIN_MONTHLY, MAX_MONTHLY)
        val annualHealth = monthlyTotal * 12
        val annualCare = (annualHealth * input.assumptions.longTermCareRate).roundToLong()

        return Result(
            annualHealthInsurance = annualHealth,
            annualLongTermCare = annualCare,
        )
    }

    private const val MIN_MONTHLY = 50_000L
    private const val MAX_MONTHLY = 5_000_000L

    private fun incomePremiumMonthly(monthlyIncome: Long): Long = when {
        monthlyIncome < 1_000_000L -> 0L
        monthlyIncome < 2_000_000L -> 120_000L
        monthlyIncome < 3_500_000L -> 220_000L
        monthlyIncome < 5_500_000L -> 350_000L
        else -> 520_000L
    }

    /** 재산 점수: 금융자산 + 부동산 순자산×58% 공제 근사 */
    private fun assetPremiumMonthly(financialAssets: Long, realEstateNetEquity: Long): Long {
        val propertyPoints = (realEstateNetEquity * 0.58).roundToLong()
        val totalPoints = financialAssets + propertyPoints
        return when {
            totalPoints < 50_000_000L -> 0L
            totalPoints < 150_000_000L -> 60_000L
            totalPoints < 300_000_000L -> 120_000L
            else -> 200_000L
        }
    }
}
