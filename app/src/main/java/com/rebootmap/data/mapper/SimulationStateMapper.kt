package com.rebootmap.data.mapper

import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.InvestmentDefaults
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
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
            realEstateSaleYear = realEstate?.saleYear,
            nationalPensionMonthly = national?.monthlyPayout ?: 0L,
            nationalPensionStartAge = national?.startAge ?: 65,
            severancePensionBalance = severance?.balance ?: 0L,
            severancePensionMonthly = severance?.monthlyContribution ?: 0L,
            severancePensionEndAge = severance?.contributionEndAge ?: 60,
            personalPensionBalance = personal?.balance ?: 0L,
            personalPensionMonthly = personal?.monthlyContribution ?: 0L,
            personalPensionEndAge = personal?.contributionEndAge ?: 60,
            personalPensionPayoutAge = personal?.payoutStartAge ?: 55,
            yellowUmbrellaBalance = yellow?.balance ?: 0L,
            yellowUmbrellaMonthly = yellow?.monthlyContribution ?: 0L,
            yellowUmbrellaEndAge = yellow?.contributionEndAge ?: 60,
            yellowUmbrellaPayoutAge = yellow?.payoutAge ?: 60,
            investmentValue = investment?.currentValue ?: 0L,
            investmentReturnRate = investment?.annualReturnRate ?: InvestmentDefaults.DEFAULT_RETURN_RATE,
            cashSavingsAmount = cash?.maturityAmount ?: 0L,
            cashSavingsYear = cash?.maturityYear ?: Year.now().value + 5,
            fixedIncomeMonthly = fixedIncome?.monthlyAmount ?: 0L,
            fixedIncomeStartAge = fixedIncome?.startAge ?: 60,
            fixedIncomeEndAge = fixedIncome?.endAge ?: 90,
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
            ),
            presetSourceNote = persisted.presetSourceNote,
        )
    }

    fun defaultAssets(
        realEstateValue: Long = 0L,
        realEstateDebt: Long = 0L,
        realEstateSaleYear: Int? = null,
        nationalMonthly: Long = 0L,
        nationalStartAge: Int = 65,
        severanceBalance: Long = 0L,
        severanceMonthly: Long = 0L,
        severanceEndAge: Int = 60,
        personalBalance: Long = 0L,
        personalMonthly: Long = 0L,
        personalEndAge: Int = 60,
        personalPayoutAge: Int = 55,
        yellowBalance: Long = 0L,
        yellowMonthly: Long = 0L,
        yellowEndAge: Int = 60,
        yellowPayoutAge: Int = 60,
        investmentValue: Long = 0L,
        investmentReturnRate: Double = InvestmentDefaults.DEFAULT_RETURN_RATE,
        cashAmount: Long = 0L,
        cashYear: Int = Year.now().value + 5,
        fixedIncomeMonthly: Long = 0L,
        fixedIncomeStartAge: Int = 60,
        fixedIncomeEndAge: Int = 90,
    ): List<Asset> = listOf(
        Asset.RealEstate(currentValue = realEstateValue, debtAmount = realEstateDebt, saleYear = realEstateSaleYear),
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
    )
}
