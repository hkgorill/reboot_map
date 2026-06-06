package com.rebootmap.data.mapper

import com.rebootmap.data.model.PersistedLumpSumExpense
import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.InvestmentDefaults
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.scenario.PurchaseTiming
import com.rebootmap.domain.scenario.RelocationPlan
import com.rebootmap.presentation.simulation.SimulationUiState
import java.time.Year

object SimulationStateMapper {

    fun toPersisted(state: SimulationUiState): SimulationPersistedState {
        val realEstate = state.assets.filterIsInstance<Asset.RealEstate>().firstOrNull()
        val national = state.assets.filterIsInstance<Asset.NationalPension>().firstOrNull()
        val severance = state.assets.filterIsInstance<Asset.SeverancePension>().firstOrNull()
        val personal = state.assets.filterIsInstance<Asset.PersonalPension>().firstOrNull()
        val yellow = state.assets.filterIsInstance<Asset.YellowUmbrella>().firstOrNull()
        val investment = state.assets.filterIsInstance<Asset.Investment>().firstOrNull()
        val cash = state.assets.filterIsInstance<Asset.CashSavings>().firstOrNull()
        val fixedIncome = state.assets.filterIsInstance<Asset.FixedIncome>().firstOrNull()
        val housingPension = state.assets.filterIsInstance<Asset.HousingPension>().firstOrNull()
        val timing = state.relocationPlan.purchaseTiming

        return SimulationPersistedState(
            onboardingCompleted = state.isOnboardingCompleted,
            currentAge = state.profile.currentAge,
            retirementAge = state.profile.retirementAge,
            lifeExpectancy = state.profile.lifeExpectancy,
            monthlyLivingExpense = state.profile.monthlyLivingExpense,
            inflationRate = state.assumptions.inflationRate,
            pensionIncomeTaxRate = state.assumptions.pensionIncomeTaxRate,
            generalIncomeTaxRate = state.assumptions.generalIncomeTaxRate,
            presetSourceNote = state.presetSourceNote,
            realEstateValue = realEstate?.currentValue ?: 0L,
            realEstateDebt = realEstate?.debtAmount ?: 0L,
            realEstateAcquisitionCost = realEstate?.acquisitionCost ?: 0L,
            realEstateHoldingYears = realEstate?.holdingYears ?: 0,
            realEstateIsPrimaryResidence = realEstate?.isPrimaryResidence ?: false,
            realEstateSaleYear = realEstate?.saleYear,
            nationalPensionMonthly = national?.monthlyPayout ?: 0L,
            nationalPensionStartAge = national?.startAge ?: 0,
            severancePensionBalance = severance?.balance ?: 0L,
            severancePensionMonthly = severance?.monthlyContribution ?: 0L,
            severancePensionEndAge = severance?.contributionEndAge ?: 0,
            personalPensionBalance = personal?.balance ?: 0L,
            personalPensionMonthly = personal?.monthlyContribution ?: 0L,
            personalPensionEndAge = personal?.contributionEndAge ?: 0,
            personalPensionPayoutAge = personal?.payoutStartAge ?: 0,
            yellowUmbrellaBalance = yellow?.balance ?: 0L,
            yellowUmbrellaMonthly = yellow?.monthlyContribution ?: 0L,
            yellowUmbrellaEndAge = yellow?.contributionEndAge ?: 0,
            yellowUmbrellaPayoutAge = yellow?.payoutAge ?: 0,
            investmentValue = investment?.currentValue ?: 0L,
            investmentReturnRate = investment?.annualReturnRate ?: 0.0,
            cashSavingsAmount = cash?.maturityAmount ?: 0L,
            cashSavingsYear = cash?.maturityYear ?: 0,
            fixedIncomeMonthly = fixedIncome?.monthlyAmount ?: 0L,
            fixedIncomeStartAge = fixedIncome?.startAge ?: 0,
            fixedIncomeEndAge = fixedIncome?.endAge ?: 0,
            relocationEnabled = state.relocationPlan.enabled,
            relocationNewHomeValue = state.relocationPlan.newHomeValue,
            relocationNewHomeDebt = state.relocationPlan.newHomeDebt,
            relocationPurchaseTimingType = timing.toTypeCode(),
            relocationPurchaseTimingYears = timing.toYears(),
            housingPensionEnabled = housingPension?.enabled ?: false,
            housingPensionStartAge = housingPension?.startAge ?: 0,
            housingPensionHomeEquity = housingPension?.homeEquityOverride ?: 0L,
            lumpSumExpenses = state.lumpSumExpenses.map(PersistedLumpSumExpense::fromDomain),
        )
    }

    fun toUiState(persisted: SimulationPersistedState): SimulationUiState {
        val severanceBalance = persisted.severancePensionBalance
            .takeIf { it > 0 } ?: persisted.retirementPensionBalance
        val severanceMonthly = persisted.severancePensionMonthly
            .takeIf { persisted.severancePensionBalance > 0 || persisted.retirementPensionBalance == 0L }
            ?: persisted.retirementPensionMonthly
        val severanceEndAge = if (persisted.severancePensionBalance > 0) {
            persisted.severancePensionEndAge
        } else {
            persisted.retirementPensionEndAge
        }

        return SimulationUiState(
            isOnboardingCompleted = persisted.onboardingCompleted,
            profile = UserProfile(
                currentAge = persisted.currentAge,
                retirementAge = persisted.retirementAge,
                lifeExpectancy = persisted.lifeExpectancy,
                monthlyLivingExpense = persisted.monthlyLivingExpense,
            ),
            assumptions = EconomicAssumptions(
                inflationRate = persisted.inflationRate,
                pensionIncomeTaxRate = persisted.pensionIncomeTaxRate,
                generalIncomeTaxRate = persisted.generalIncomeTaxRate,
            ),
            assets = defaultAssets(
                realEstateValue = persisted.realEstateValue,
                realEstateDebt = persisted.realEstateDebt,
                realEstateAcquisitionCost = persisted.realEstateAcquisitionCost,
                realEstateHoldingYears = persisted.realEstateHoldingYears,
                realEstateIsPrimaryResidence = persisted.realEstateIsPrimaryResidence,
                realEstateSaleYear = persisted.realEstateSaleYear,
                nationalMonthly = persisted.nationalPensionMonthly,
                nationalStartAge = persisted.nationalPensionStartAge,
                severanceBalance = severanceBalance,
                severanceMonthly = severanceMonthly,
                severanceEndAge = severanceEndAge,
                personalBalance = persisted.personalPensionBalance,
                personalMonthly = persisted.personalPensionMonthly,
                personalEndAge = persisted.personalPensionEndAge,
                personalPayoutAge = persisted.personalPensionPayoutAge,
                yellowBalance = persisted.yellowUmbrellaBalance,
                yellowMonthly = persisted.yellowUmbrellaMonthly,
                yellowEndAge = persisted.yellowUmbrellaEndAge,
                yellowPayoutAge = persisted.yellowUmbrellaPayoutAge,
                investmentValue = persisted.investmentValue,
                investmentReturnRate = persisted.investmentReturnRate,
                cashAmount = persisted.cashSavingsAmount,
                cashYear = persisted.cashSavingsYear,
                fixedIncomeMonthly = persisted.fixedIncomeMonthly,
                fixedIncomeStartAge = persisted.fixedIncomeStartAge,
                fixedIncomeEndAge = persisted.fixedIncomeEndAge,
                housingPensionEnabled = persisted.housingPensionEnabled,
                housingPensionStartAge = persisted.housingPensionStartAge,
                housingPensionHomeEquity = persisted.housingPensionHomeEquity,
            ),
            relocationPlan = RelocationPlan(
                enabled = persisted.relocationEnabled,
                newHomeValue = persisted.relocationNewHomeValue,
                newHomeDebt = persisted.relocationNewHomeDebt,
                purchaseTiming = persisted.relocationPurchaseTimingType.toPurchaseTiming(
                    persisted.relocationPurchaseTimingYears,
                ),
            ),
            presetSourceNote = persisted.presetSourceNote,
            referencePreset = if (persisted.onboardingCompleted) {
                com.rebootmap.domain.preset.AgeBasedPreset.forAge(persisted.currentAge)
            } else {
                null
            },
            lumpSumExpenses = persisted.lumpSumExpenses.map { it.toDomain() },
        )
    }

    fun emptyAssets(): List<Asset> = defaultAssets()

    fun defaultAssets(
        realEstateValue: Long = 0L,
        realEstateDebt: Long = 0L,
        realEstateAcquisitionCost: Long = 0L,
        realEstateHoldingYears: Int = 0,
        realEstateIsPrimaryResidence: Boolean = false,
        realEstateSaleYear: Int? = null,
        nationalMonthly: Long = 0L,
        nationalStartAge: Int = 0,
        severanceBalance: Long = 0L,
        severanceMonthly: Long = 0L,
        severanceEndAge: Int = 0,
        personalBalance: Long = 0L,
        personalMonthly: Long = 0L,
        personalEndAge: Int = 0,
        personalPayoutAge: Int = 0,
        yellowBalance: Long = 0L,
        yellowMonthly: Long = 0L,
        yellowEndAge: Int = 0,
        yellowPayoutAge: Int = 0,
        investmentValue: Long = 0L,
        investmentReturnRate: Double = 0.0,
        cashAmount: Long = 0L,
        cashYear: Int = 0,
        fixedIncomeMonthly: Long = 0L,
        fixedIncomeStartAge: Int = 0,
        fixedIncomeEndAge: Int = 0,
        housingPensionEnabled: Boolean = false,
        housingPensionStartAge: Int = 0,
        housingPensionHomeEquity: Long = 0L,
    ): List<Asset> = listOf(
        Asset.RealEstate(
            currentValue = realEstateValue,
            debtAmount = realEstateDebt,
            acquisitionCost = realEstateAcquisitionCost,
            holdingYears = realEstateHoldingYears,
            isPrimaryResidence = realEstateIsPrimaryResidence,
            saleYear = realEstateSaleYear,
        ),
        Asset.NationalPension(monthlyPayout = nationalMonthly, startAge = nationalStartAge),
        Asset.SeverancePension(
            balance = severanceBalance,
            monthlyContribution = severanceMonthly,
            contributionEndAge = severanceEndAge,
        ),
        Asset.PersonalPension(
            balance = personalBalance,
            monthlyContribution = personalMonthly,
            contributionEndAge = personalEndAge,
            payoutStartAge = personalPayoutAge,
        ),
        Asset.YellowUmbrella(
            balance = yellowBalance,
            monthlyContribution = yellowMonthly,
            contributionEndAge = yellowEndAge,
            payoutAge = yellowPayoutAge,
        ),
        Asset.Investment(currentValue = investmentValue, annualReturnRate = investmentReturnRate),
        Asset.CashSavings(maturityAmount = cashAmount, maturityYear = cashYear),
        Asset.FixedIncome(
            monthlyAmount = fixedIncomeMonthly,
            startAge = fixedIncomeStartAge,
            endAge = fixedIncomeEndAge,
        ),
        Asset.HousingPension(
            enabled = housingPensionEnabled,
            startAge = housingPensionStartAge,
            homeEquityOverride = housingPensionHomeEquity,
        ),
    )

    private fun PurchaseTiming.toTypeCode(): String = when (this) {
        is PurchaseTiming.SameYearAsSale -> "SAME_YEAR"
        is PurchaseTiming.BeforeSale -> "BEFORE_SALE"
        is PurchaseTiming.AfterSale -> "AFTER_SALE"
    }

    private fun PurchaseTiming.toYears(): Int = when (this) {
        is PurchaseTiming.SameYearAsSale -> 0
        is PurchaseTiming.BeforeSale -> years
        is PurchaseTiming.AfterSale -> years
    }

    private fun String.toPurchaseTiming(years: Int): PurchaseTiming = when (this) {
        "BEFORE_SALE" -> PurchaseTiming.BeforeSale(years.coerceIn(1, 5))
        "AFTER_SALE" -> PurchaseTiming.AfterSale(years.coerceIn(1, 5))
        else -> PurchaseTiming.SameYearAsSale
    }
}
