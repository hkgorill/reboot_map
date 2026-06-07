package com.rebootmap.data.mapper



import com.rebootmap.data.model.PersistedLumpSumExpense

import com.rebootmap.data.model.SimulationPersistedState

import com.rebootmap.domain.model.Asset

import com.rebootmap.domain.model.EconomicAssumptions

import com.rebootmap.domain.model.LivingExpenseInflationBase

import com.rebootmap.domain.model.PensionDefaults

import com.rebootmap.domain.model.TaxDefaults

import com.rebootmap.domain.model.UserProfile

import com.rebootmap.domain.scenario.PurchaseTiming

import com.rebootmap.domain.scenario.RelocationPlan

import com.rebootmap.presentation.simulation.SimulationUiState

import java.time.Year



object SimulationStateMapper {



    fun toPersisted(state: SimulationUiState): SimulationPersistedState {

        val realEstatesList = state.assets.filterIsInstance<Asset.RealEstate>()

        val legacyRealEstate = RealEstatePersistence.syncLegacyFields(realEstatesList.firstOrNull())

        val national = state.assets.filterIsInstance<Asset.NationalPension>().firstOrNull()

        val severance = state.assets.filterIsInstance<Asset.SeverancePension>().firstOrNull()

        val personal = state.assets.filterIsInstance<Asset.PersonalPension>().firstOrNull()

        val yellow = state.assets.filterIsInstance<Asset.YellowUmbrella>().firstOrNull()

        val investment = state.assets.filterIsInstance<Asset.Investment>().firstOrNull()

        val cash = state.assets.filterIsInstance<Asset.CashSavings>().firstOrNull()

        val employment = state.assets.filterIsInstance<Asset.EmploymentIncome>().firstOrNull()

        val business = state.assets.filterIsInstance<Asset.BusinessIncome>().firstOrNull()

        val otherFixed = state.assets.filterIsInstance<Asset.OtherFixedIncome>().firstOrNull()

        val housingPension = state.assets.filterIsInstance<Asset.HousingPension>().firstOrNull()

        val timing = state.relocationPlan.purchaseTiming

        val assumptions = state.assumptions



        return SimulationPersistedState(

            onboardingCompleted = state.isOnboardingCompleted,

            currentAge = state.profile.currentAge,

            retirementAge = state.profile.retirementAge,

            lifeExpectancy = state.profile.lifeExpectancy,

            monthlyLivingExpense = state.profile.monthlyLivingExpense,

            inflationRate = assumptions.inflationRate,

            livingExpenseInflationBase = assumptions.livingExpenseInflationBase.name,

            pensionIncomeTaxRate = assumptions.pensionIncomeTaxRate,

            employmentIncomeTaxRate = assumptions.employmentIncomeTaxRate,

            businessIncomeTaxRate = assumptions.businessIncomeTaxRate,

            generalIncomeTaxRate = assumptions.generalIncomeTaxRate,

            propertyTaxEnabled = assumptions.propertyTaxEnabled,

            propertyTaxRate = assumptions.propertyTaxRate,

            nonResidentialPropertyTaxRate = assumptions.nonResidentialPropertyTaxRate,

            comprehensiveRealEstateTaxEnabled = assumptions.comprehensiveRealEstateTaxEnabled,

            comprehensiveTaxThreshold = assumptions.comprehensiveTaxThreshold,

            comprehensiveTaxRate = assumptions.comprehensiveTaxRate,

            healthInsuranceEnabled = assumptions.healthInsuranceEnabled,

            longTermCareRate = assumptions.longTermCareRate,

            presetSourceNote = state.presetSourceNote,

            realEstates = RealEstatePersistence.toPersistedList(realEstatesList),

            realEstateValue = legacyRealEstate.value,

            realEstateDebt = legacyRealEstate.debt,

            realEstateAcquisitionCost = legacyRealEstate.acquisitionCost,

            realEstateHoldingYears = legacyRealEstate.holdingYears,

            realEstateIsPrimaryResidence = legacyRealEstate.isPrimaryResidence,

            realEstateSaleYear = legacyRealEstate.saleYear,

            realEstateExpectedSalePrice = legacyRealEstate.expectedSalePrice,

            nationalPensionMonthly = national?.monthlyPayout ?: 0L,

            nationalPensionStartAge = national?.startAge ?: 0,

            severancePensionBalance = severance?.balance ?: 0L,

            severancePensionMonthly = severance?.monthlyContribution ?: 0L,

            severancePensionEndAge = severance?.contributionEndAge ?: 0,

            severancePensionPayoutAge = severance?.payoutStartAge ?: 0,

            severancePensionReturnRate = severance?.annualReturnRate ?: PensionDefaults.SEVERANCE_RETURN_RATE,

            personalPensionBalance = personal?.balance ?: 0L,

            personalPensionMonthly = personal?.monthlyContribution ?: 0L,

            personalPensionEndAge = personal?.contributionEndAge ?: 0,

            personalPensionPayoutAge = personal?.payoutStartAge ?: 0,

            personalPensionReturnRate = personal?.annualReturnRate ?: PensionDefaults.PERSONAL_RETURN_RATE,

            yellowUmbrellaBalance = yellow?.balance ?: 0L,

            yellowUmbrellaMonthly = yellow?.monthlyContribution ?: 0L,

            yellowUmbrellaEndAge = yellow?.contributionEndAge ?: 0,

            yellowUmbrellaPayoutAge = yellow?.payoutAge ?: 0,

            yellowUmbrellaReturnRate = yellow?.annualReturnRate ?: PensionDefaults.YELLOW_UMBRELLA_RETURN_RATE,

            investmentValue = investment?.currentValue ?: 0L,

            investmentReturnRate = investment?.annualReturnRate ?: 0.0,

            cashSavingsAmount = cash?.maturityAmount ?: 0L,

            cashSavingsYear = cash?.maturityYear ?: 0,

            employmentIncomeMonthly = employment?.monthlyAmount ?: 0L,

            employmentIncomeStartAge = employment?.startAge ?: 0,

            employmentIncomeEndAge = employment?.endAge ?: 0,

            businessIncomeMonthly = business?.monthlyAmount ?: 0L,

            businessIncomeStartAge = business?.startAge ?: 0,

            businessIncomeEndAge = business?.endAge ?: 0,

            otherFixedIncomeMonthly = otherFixed?.monthlyAmount ?: 0L,

            otherFixedIncomeStartAge = otherFixed?.startAge ?: 0,

            otherFixedIncomeEndAge = otherFixed?.endAge ?: 0,

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

        val severancePayoutAge = persisted.severancePensionPayoutAge

        val income = resolveIncomeFields(persisted)



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

                livingExpenseInflationBase = LivingExpenseInflationBase.fromPersisted(

                    persisted.livingExpenseInflationBase,

                ),

                pensionIncomeTaxRate = persisted.pensionIncomeTaxRate,

                employmentIncomeTaxRate = persisted.employmentIncomeTaxRate,

                businessIncomeTaxRate = persisted.businessIncomeTaxRate,

                generalIncomeTaxRate = persisted.generalIncomeTaxRate,

                propertyTaxEnabled = persisted.propertyTaxEnabled,

                propertyTaxRate = persisted.propertyTaxRate,

                nonResidentialPropertyTaxRate = persisted.nonResidentialPropertyTaxRate,

                comprehensiveRealEstateTaxEnabled = persisted.comprehensiveRealEstateTaxEnabled,

                comprehensiveTaxThreshold = persisted.comprehensiveTaxThreshold,

                comprehensiveTaxRate = persisted.comprehensiveTaxRate,

                healthInsuranceEnabled = persisted.healthInsuranceEnabled,

                longTermCareRate = persisted.longTermCareRate,

            ),

            assets = defaultAssets(

                realEstates = RealEstatePersistence.resolveFromPersisted(persisted),

                nationalMonthly = persisted.nationalPensionMonthly,

                nationalStartAge = persisted.nationalPensionStartAge,

                severanceBalance = severanceBalance,

                severanceMonthly = severanceMonthly,

                severanceEndAge = severanceEndAge,

                severancePayoutAge = severancePayoutAge,

                severanceReturnRate = persisted.severancePensionReturnRate,

                personalBalance = persisted.personalPensionBalance,

                personalMonthly = persisted.personalPensionMonthly,

                personalEndAge = persisted.personalPensionEndAge,

                personalPayoutAge = persisted.personalPensionPayoutAge,

                personalReturnRate = persisted.personalPensionReturnRate,

                yellowBalance = persisted.yellowUmbrellaBalance,

                yellowMonthly = persisted.yellowUmbrellaMonthly,

                yellowEndAge = persisted.yellowUmbrellaEndAge,

                yellowPayoutAge = persisted.yellowUmbrellaPayoutAge,

                yellowReturnRate = persisted.yellowUmbrellaReturnRate,

                investmentValue = persisted.investmentValue,

                investmentReturnRate = persisted.investmentReturnRate,

                cashAmount = persisted.cashSavingsAmount,

                cashYear = persisted.cashSavingsYear,

                employmentMonthly = income.employmentMonthly,

                employmentStartAge = income.employmentStartAge,

                employmentEndAge = income.employmentEndAge,

                businessMonthly = income.businessMonthly,

                businessStartAge = income.businessStartAge,

                businessEndAge = income.businessEndAge,

                otherFixedMonthly = income.otherFixedMonthly,

                otherFixedStartAge = income.otherFixedStartAge,

                otherFixedEndAge = income.otherFixedEndAge,

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

        realEstates: List<Asset.RealEstate>? = null,

        realEstateValue: Long = 0L,

        realEstateDebt: Long = 0L,

        realEstateAcquisitionCost: Long = 0L,

        realEstateHoldingYears: Int = 0,

        realEstateIsPrimaryResidence: Boolean = true,

        realEstateSaleYear: Int? = null,

        realEstateExpectedSalePrice: Long = 0L,

        nationalMonthly: Long = 0L,

        nationalStartAge: Int = 0,

        severanceBalance: Long = 0L,

        severanceMonthly: Long = 0L,

        severanceEndAge: Int = 0,

        severancePayoutAge: Int = 0,

        severanceReturnRate: Double = PensionDefaults.SEVERANCE_RETURN_RATE,

        personalBalance: Long = 0L,

        personalMonthly: Long = 0L,

        personalEndAge: Int = 0,

        personalPayoutAge: Int = 0,

        personalReturnRate: Double = PensionDefaults.PERSONAL_RETURN_RATE,

        yellowBalance: Long = 0L,

        yellowMonthly: Long = 0L,

        yellowEndAge: Int = 0,

        yellowPayoutAge: Int = 0,

        yellowReturnRate: Double = PensionDefaults.YELLOW_UMBRELLA_RETURN_RATE,

        investmentValue: Long = 0L,

        investmentReturnRate: Double = 0.0,

        cashAmount: Long = 0L,

        cashYear: Int = 0,

        employmentMonthly: Long = 0L,

        employmentStartAge: Int = 0,

        employmentEndAge: Int = 0,

        businessMonthly: Long = 0L,

        businessStartAge: Int = 0,

        businessEndAge: Int = 0,

        otherFixedMonthly: Long = 0L,

        otherFixedStartAge: Int = 0,

        otherFixedEndAge: Int = 0,

        housingPensionEnabled: Boolean = false,

        housingPensionStartAge: Int = 0,

        housingPensionHomeEquity: Long = 0L,

    ): List<Asset> = DefaultAssets.build(

        realEstates = realEstates ?: DefaultAssets.legacyRealEstate(

            value = realEstateValue,

            debt = realEstateDebt,

            acquisitionCost = realEstateAcquisitionCost,

            holdingYears = realEstateHoldingYears,

            isPrimaryResidence = realEstateIsPrimaryResidence,

            saleYear = realEstateSaleYear,

            expectedSalePrice = realEstateExpectedSalePrice,

        ),

        nationalMonthly = nationalMonthly,

        nationalStartAge = nationalStartAge,

        severanceBalance = severanceBalance,

        severanceMonthly = severanceMonthly,

        severanceEndAge = severanceEndAge,

        severancePayoutAge = severancePayoutAge,

        severanceReturnRate = severanceReturnRate,

        personalBalance = personalBalance,

        personalMonthly = personalMonthly,

        personalEndAge = personalEndAge,

        personalPayoutAge = personalPayoutAge,

        personalReturnRate = personalReturnRate,

        yellowBalance = yellowBalance,

        yellowMonthly = yellowMonthly,

        yellowEndAge = yellowEndAge,

        yellowPayoutAge = yellowPayoutAge,

        yellowReturnRate = yellowReturnRate,

        investmentValue = investmentValue,

        investmentReturnRate = investmentReturnRate,

        cashAmount = cashAmount,

        cashYear = cashYear,

        employmentMonthly = employmentMonthly,

        employmentStartAge = employmentStartAge,

        employmentEndAge = employmentEndAge,

        businessMonthly = businessMonthly,

        businessStartAge = businessStartAge,

        businessEndAge = businessEndAge,

        otherFixedMonthly = otherFixedMonthly,

        otherFixedStartAge = otherFixedStartAge,

        otherFixedEndAge = otherFixedEndAge,

        housingPensionEnabled = housingPensionEnabled,

        housingPensionStartAge = housingPensionStartAge,

        housingPensionHomeEquity = housingPensionHomeEquity,

    )



    internal fun resolveIncomeFields(persisted: SimulationPersistedState): ResolvedIncomeFields {

        val hasNewIncome = persisted.employmentIncomeMonthly > 0 ||

            persisted.businessIncomeMonthly > 0 ||

            persisted.otherFixedIncomeMonthly > 0

        if (hasNewIncome) {

            return ResolvedIncomeFields(

                employmentMonthly = persisted.employmentIncomeMonthly,

                employmentStartAge = persisted.employmentIncomeStartAge,

                employmentEndAge = persisted.employmentIncomeEndAge,

                businessMonthly = persisted.businessIncomeMonthly,

                businessStartAge = persisted.businessIncomeStartAge,

                businessEndAge = persisted.businessIncomeEndAge,

                otherFixedMonthly = persisted.otherFixedIncomeMonthly,

                otherFixedStartAge = persisted.otherFixedIncomeStartAge,

                otherFixedEndAge = persisted.otherFixedIncomeEndAge,

            )

        }

        if (persisted.fixedIncomeMonthly > 0) {

            return ResolvedIncomeFields(

                employmentMonthly = persisted.fixedIncomeMonthly,

                employmentStartAge = persisted.fixedIncomeStartAge,

                employmentEndAge = persisted.fixedIncomeEndAge,

                businessMonthly = 0L,

                businessStartAge = 0,

                businessEndAge = 0,

                otherFixedMonthly = 0L,

                otherFixedStartAge = 0,

                otherFixedEndAge = 0,

            )

        }

        return ResolvedIncomeFields()

    }



    internal data class ResolvedIncomeFields(

        val employmentMonthly: Long = 0L,

        val employmentStartAge: Int = 0,

        val employmentEndAge: Int = 0,

        val businessMonthly: Long = 0L,

        val businessStartAge: Int = 0,

        val businessEndAge: Int = 0,

        val otherFixedMonthly: Long = 0L,

        val otherFixedStartAge: Int = 0,

        val otherFixedEndAge: Int = 0,

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


