package com.rebootmap.domain.preset

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
import java.time.Year

/**
 * 연령대별 평균 추정 프리셋.
 *
 * 근거 자료 (2023~2024년 공개 통계 기준 근사치):
 * - 생활비: 통계청 가구당 소비지출 / 1·2인 은퇴가구 평균
 * - 물가상승률: 한국은행 물가안정목표 2%
 * - 부동산: 한국부동산원·통계청 주택자산 연령대별 중앙값 근사
 * - 국민연금: 국민연금공단 예상수령액 평균 수준 (만기소득·가입기간 가정)
 * - 퇴직연금: 금융감독원 퇴직연금 적립금·납입 통계
 * - 금융자산: 한국은행 가계금융복지조사 평균 금융자산
 */
data class AgeBasedPreset(
    val profile: UserProfile,
    val assumptions: EconomicAssumptions,
    val assets: List<Asset>,
    val sourceNote: String,
) {
    companion object {
        private const val MAN = 10_000L

        fun manWon(man: Long): Long = man * MAN

        fun forAge(age: Int): AgeBasedPreset {
            val safeAge = age.coerceIn(18, 100)
            val bracket = ageBracket(safeAge)
            val currentYear = Year.now().value

            val retirementAge = when {
                safeAge >= 60 -> 65
                safeAge >= 50 -> 62
                else -> 60
            }
            val lifeExpectancy = if (safeAge >= 70) 88 else 90

            return AgeBasedPreset(
                profile = UserProfile(
                    currentAge = safeAge,
                    retirementAge = retirementAge.coerceAtLeast(safeAge),
                    lifeExpectancy = lifeExpectancy.coerceAtLeast(safeAge),
                    monthlyLivingExpense = bracket.monthlyLivingExpenseMan * MAN,
                ),
                assumptions = EconomicAssumptions(
                    inflationRate = 0.02,
                    pensionIncomeTaxRate = 0.033,
                    generalIncomeTaxRate = 0.15,
                ),
                assets = listOf(
                    Asset.RealEstate(
                        currentValue = bracket.realEstateValueMan * MAN,
                        debtAmount = bracket.realEstateDebtMan * MAN,
                        saleYear = currentYear + (retirementAge - safeAge) + 3,
                    ),
                    Asset.NationalPension(
                        monthlyPayout = bracket.nationalPensionMonthlyMan * MAN,
                        startAge = 65,
                    ),
                    Asset.RetirementPension(
                        balance = bracket.retirementPensionBalanceMan * MAN,
                        monthlyContribution = bracket.retirementPensionMonthlyMan * MAN,
                        contributionEndAge = retirementAge.coerceAtMost(60),
                    ),
                    Asset.Investment(
                        currentValue = bracket.investmentValueMan * MAN,
                        annualReturnRate = bracket.investmentReturnRate,
                    ),
                    Asset.CashSavings(
                        maturityAmount = bracket.cashSavingsMan * MAN,
                        maturityYear = currentYear + bracket.cashMaturityYears,
                    ),
                ),
                sourceNote = bracket.sourceNote,
            )
        }

        private fun ageBracket(age: Int): PresetBracket = when (age) {
            in 18..29 -> PresetBracket(
                monthlyLivingExpenseMan = 200,
                realEstateValueMan = 15_000,
                realEstateDebtMan = 12_000,
                nationalPensionMonthlyMan = 90,
                retirementPensionBalanceMan = 500,
                retirementPensionMonthlyMan = 20,
                investmentValueMan = 300,
                investmentReturnRate = 0.06,
                cashSavingsMan = 500,
                cashMaturityYears = 3,
                sourceNote = "20대 평균 추정 (통계청·가계금융복지조사)",
            )
            in 30..39 -> PresetBracket(
                monthlyLivingExpenseMan = 250,
                realEstateValueMan = 30_000,
                realEstateDebtMan = 16_500,
                nationalPensionMonthlyMan = 110,
                retirementPensionBalanceMan = 2_000,
                retirementPensionMonthlyMan = 35,
                investmentValueMan = 1_500,
                investmentReturnRate = 0.06,
                cashSavingsMan = 1_000,
                cashMaturityYears = 4,
                sourceNote = "30대 평균 추정 (통계청·국민연금공단)",
            )
            in 40..49 -> PresetBracket(
                monthlyLivingExpenseMan = 300,
                realEstateValueMan = 50_000,
                realEstateDebtMan = 17_500,
                nationalPensionMonthlyMan = 130,
                retirementPensionBalanceMan = 5_000,
                retirementPensionMonthlyMan = 45,
                investmentValueMan = 3_000,
                investmentReturnRate = 0.055,
                cashSavingsMan = 2_000,
                cashMaturityYears = 5,
                sourceNote = "40대 평균 추정 (통계청·국민연금공단·금감원)",
            )
            in 50..59 -> PresetBracket(
                monthlyLivingExpenseMan = 280,
                realEstateValueMan = 60_000,
                realEstateDebtMan = 12_000,
                nationalPensionMonthlyMan = 150,
                retirementPensionBalanceMan = 10_000,
                retirementPensionMonthlyMan = 50,
                investmentValueMan = 5_000,
                investmentReturnRate = 0.05,
                cashSavingsMan = 3_000,
                cashMaturityYears = 3,
                sourceNote = "50대 평균 추정 (통계청·국민연금공단·금감원)",
            )
            in 60..69 -> PresetBracket(
                monthlyLivingExpenseMan = 250,
                realEstateValueMan = 55_000,
                realEstateDebtMan = 5_500,
                nationalPensionMonthlyMan = 140,
                retirementPensionBalanceMan = 12_000,
                retirementPensionMonthlyMan = 0,
                investmentValueMan = 4_000,
                investmentReturnRate = 0.045,
                cashSavingsMan = 2_500,
                cashMaturityYears = 2,
                sourceNote = "60대 평균 추정 (통계청·국민연금 수급 통계)",
            )
            else -> PresetBracket(
                monthlyLivingExpenseMan = 220,
                realEstateValueMan = 45_000,
                realEstateDebtMan = 2_250,
                nationalPensionMonthlyMan = 120,
                retirementPensionBalanceMan = 8_000,
                retirementPensionMonthlyMan = 0,
                investmentValueMan = 2_500,
                investmentReturnRate = 0.04,
                cashSavingsMan = 2_000,
                cashMaturityYears = 2,
                sourceNote = "70대+ 평균 추정 (통계청·국민연금 수급 통계)",
            )
        }
    }

    private data class PresetBracket(
        val monthlyLivingExpenseMan: Long,
        val realEstateValueMan: Long,
        val realEstateDebtMan: Long,
        val nationalPensionMonthlyMan: Long,
        val retirementPensionBalanceMan: Long,
        val retirementPensionMonthlyMan: Long,
        val investmentValueMan: Long,
        val investmentReturnRate: Double,
        val cashSavingsMan: Long,
        val cashMaturityYears: Int,
        val sourceNote: String,
    )
}
