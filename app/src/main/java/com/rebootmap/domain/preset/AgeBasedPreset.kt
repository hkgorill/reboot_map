package com.rebootmap.domain.preset

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.PensionDefaults
import com.rebootmap.domain.model.InvestmentDefaults
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
 * - 퇴직연금: 금융감독원 DC·IRP 적립금·납입 통계 (직장인)
 * - 개인연금: 연금저축·개인 IRP 적립 통계
 * - 노랑우산: 중소벤처기업부 소기업·소상공인 공제 평균 (자영업자)
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
            val contributionEndAge = retirementAge.coerceAtMost(60)

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
                        acquisitionCost = ((bracket.realEstateValueMan - bracket.realEstateDebtMan)
                            .coerceAtLeast(0) * MAN * 65 / 100),
                        holdingYears = (safeAge - 30).coerceIn(2, 30),
                        category = RealEstateCategory.PRIMARY_RESIDENCE,
                        isPrimaryResidence = true,
                        saleYear = currentYear + (retirementAge - safeAge) + 3,
                    ),
                    Asset.NationalPension(
                        monthlyPayout = bracket.nationalPensionMonthlyMan * MAN,
                        startAge = 65,
                    ),
                    Asset.SeverancePension(
                        balance = bracket.severancePensionBalanceMan * MAN,
                        monthlyContribution = bracket.severancePensionMonthlyMan * MAN,
                        contributionEndAge = contributionEndAge,
                        payoutStartAge = retirementAge,
                    ),
                    Asset.PersonalPension(
                        balance = bracket.personalPensionBalanceMan * MAN,
                        monthlyContribution = bracket.personalPensionMonthlyMan * MAN,
                        contributionEndAge = contributionEndAge,
                        payoutStartAge = PensionDefaults.PERSONAL_MIN_PAYOUT_AGE,
                    ),
                    Asset.YellowUmbrella(
                        balance = bracket.yellowUmbrellaBalanceMan * MAN,
                        monthlyContribution = bracket.yellowUmbrellaMonthlyMan * MAN,
                        contributionEndAge = contributionEndAge,
                        payoutAge = 60,
                    ),
                    Asset.Investment(
                        currentValue = bracket.investmentValueMan * MAN,
                        annualReturnRate = bracket.investmentReturnRate,
                    ),
                    Asset.CashSavings(
                        maturityAmount = bracket.cashSavingsMan * MAN,
                        maturityYear = currentYear + bracket.cashMaturityYears,
                    ),
                    Asset.EmploymentIncome(
                        monthlyAmount = bracket.fixedIncomeMonthlyMan * MAN,
                        startAge = safeAge,
                        endAge = retirementAge.coerceAtMost(lifeExpectancy),
                    ),
                    Asset.BusinessIncome(monthlyAmount = 0L, startAge = 0, endAge = 0),
                    Asset.OtherFixedIncome(
                        monthlyAmount = if (retirementAge < lifeExpectancy) {
                            bracket.fixedIncomeMonthlyMan * MAN
                        } else {
                            0L
                        },
                        startAge = (retirementAge + 1).coerceAtMost(lifeExpectancy),
                        endAge = lifeExpectancy,
                    ),
                    Asset.HousingPension(enabled = false, startAge = 65),
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
                severancePensionBalanceMan = 500,
                severancePensionMonthlyMan = 20,
                personalPensionBalanceMan = 200,
                personalPensionMonthlyMan = 10,
                yellowUmbrellaBalanceMan = 100,
                yellowUmbrellaMonthlyMan = 5,
                investmentValueMan = 300,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 500,
                cashMaturityYears = 3,
                fixedIncomeMonthlyMan = 0,
                sourceNote = "20대 평균 추정 (통계청·가계금융복지조사)",
            )
            in 30..39 -> PresetBracket(
                monthlyLivingExpenseMan = 250,
                realEstateValueMan = 30_000,
                realEstateDebtMan = 16_500,
                nationalPensionMonthlyMan = 110,
                severancePensionBalanceMan = 2_000,
                severancePensionMonthlyMan = 35,
                personalPensionBalanceMan = 800,
                personalPensionMonthlyMan = 15,
                yellowUmbrellaBalanceMan = 500,
                yellowUmbrellaMonthlyMan = 15,
                investmentValueMan = 1_500,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 1_000,
                cashMaturityYears = 4,
                fixedIncomeMonthlyMan = 30,
                sourceNote = "30대 평균 추정 (통계청·국민연금공단)",
            )
            in 40..49 -> PresetBracket(
                monthlyLivingExpenseMan = 300,
                realEstateValueMan = 50_000,
                realEstateDebtMan = 17_500,
                nationalPensionMonthlyMan = 130,
                severancePensionBalanceMan = 5_000,
                severancePensionMonthlyMan = 45,
                personalPensionBalanceMan = 1_500,
                personalPensionMonthlyMan = 15,
                yellowUmbrellaBalanceMan = 1_000,
                yellowUmbrellaMonthlyMan = 20,
                investmentValueMan = 3_000,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 2_000,
                cashMaturityYears = 5,
                fixedIncomeMonthlyMan = 80,
                sourceNote = "40대 평균 추정 (통계청·국민연금공단·금감원·중기부)",
            )
            in 50..59 -> PresetBracket(
                monthlyLivingExpenseMan = 280,
                realEstateValueMan = 60_000,
                realEstateDebtMan = 12_000,
                nationalPensionMonthlyMan = 150,
                severancePensionBalanceMan = 10_000,
                severancePensionMonthlyMan = 50,
                personalPensionBalanceMan = 3_000,
                personalPensionMonthlyMan = 20,
                yellowUmbrellaBalanceMan = 2_000,
                yellowUmbrellaMonthlyMan = 25,
                investmentValueMan = 5_000,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 3_000,
                cashMaturityYears = 3,
                fixedIncomeMonthlyMan = 100,
                sourceNote = "50대 평균 추정 (통계청·국민연금공단·금감원·중기부)",
            )
            in 60..69 -> PresetBracket(
                monthlyLivingExpenseMan = 250,
                realEstateValueMan = 55_000,
                realEstateDebtMan = 5_500,
                nationalPensionMonthlyMan = 140,
                severancePensionBalanceMan = 12_000,
                severancePensionMonthlyMan = 0,
                personalPensionBalanceMan = 4_000,
                personalPensionMonthlyMan = 0,
                yellowUmbrellaBalanceMan = 3_000,
                yellowUmbrellaMonthlyMan = 0,
                investmentValueMan = 4_000,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 2_500,
                cashMaturityYears = 2,
                fixedIncomeMonthlyMan = 120,
                sourceNote = "60대 평균 추정 (통계청·국민연금 수급 통계)",
            )
            else -> PresetBracket(
                monthlyLivingExpenseMan = 220,
                realEstateValueMan = 45_000,
                realEstateDebtMan = 2_250,
                nationalPensionMonthlyMan = 120,
                severancePensionBalanceMan = 8_000,
                severancePensionMonthlyMan = 0,
                personalPensionBalanceMan = 2_500,
                personalPensionMonthlyMan = 0,
                yellowUmbrellaBalanceMan = 2_000,
                yellowUmbrellaMonthlyMan = 0,
                investmentValueMan = 2_500,
                investmentReturnRate = InvestmentDefaults.DEFAULT_RETURN_RATE,
                cashSavingsMan = 2_000,
                cashMaturityYears = 2,
                fixedIncomeMonthlyMan = 80,
                sourceNote = "70대+ 평균 추정 (통계청·국민연금 수급 통계)",
            )
        }
    }

    private data class PresetBracket(
        val monthlyLivingExpenseMan: Long,
        val realEstateValueMan: Long,
        val realEstateDebtMan: Long,
        val nationalPensionMonthlyMan: Long,
        val severancePensionBalanceMan: Long,
        val severancePensionMonthlyMan: Long,
        val personalPensionBalanceMan: Long,
        val personalPensionMonthlyMan: Long,
        val yellowUmbrellaBalanceMan: Long,
        val yellowUmbrellaMonthlyMan: Long,
        val investmentValueMan: Long,
        val investmentReturnRate: Double,
        val cashSavingsMan: Long,
        val cashMaturityYears: Int,
        val fixedIncomeMonthlyMan: Long,
        val sourceNote: String,
    )
}
