package com.rebootmap.data.mapper

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.PensionDefaults
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.RealEstateDefaults

internal object DefaultAssets {

    fun build(
        realEstates: List<Asset.RealEstate> = listOf(RealEstateDefaults.empty()),
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
    ): List<Asset> = realEstates + listOf(
        Asset.NationalPension(monthlyPayout = nationalMonthly, startAge = nationalStartAge),
        Asset.SeverancePension(
            balance = severanceBalance,
            monthlyContribution = severanceMonthly,
            contributionEndAge = severanceEndAge,
            payoutStartAge = severancePayoutAge,
            annualReturnRate = severanceReturnRate,
        ),
        Asset.PersonalPension(
            balance = personalBalance,
            monthlyContribution = personalMonthly,
            contributionEndAge = personalEndAge,
            payoutStartAge = personalPayoutAge,
            annualReturnRate = personalReturnRate,
        ),
        Asset.YellowUmbrella(
            balance = yellowBalance,
            monthlyContribution = yellowMonthly,
            contributionEndAge = yellowEndAge,
            payoutAge = yellowPayoutAge,
            annualReturnRate = yellowReturnRate,
        ),
        Asset.Investment(currentValue = investmentValue, annualReturnRate = investmentReturnRate),
        Asset.CashSavings(maturityAmount = cashAmount, maturityYear = cashYear),
        Asset.EmploymentIncome(
            monthlyAmount = employmentMonthly,
            startAge = employmentStartAge,
            endAge = employmentEndAge,
        ),
        Asset.BusinessIncome(
            monthlyAmount = businessMonthly,
            startAge = businessStartAge,
            endAge = businessEndAge,
        ),
        Asset.OtherFixedIncome(
            monthlyAmount = otherFixedMonthly,
            startAge = otherFixedStartAge,
            endAge = otherFixedEndAge,
        ),
        Asset.HousingPension(
            enabled = housingPensionEnabled,
            startAge = housingPensionStartAge,
            homeEquityOverride = housingPensionHomeEquity,
        ),
    )

    fun legacyRealEstate(
        value: Long = 0L,
        debt: Long = 0L,
        acquisitionCost: Long = 0L,
        holdingYears: Int = 0,
        isPrimaryResidence: Boolean = false,
        saleYear: Int? = null,
        expectedSalePrice: Long = 0L,
    ): List<Asset.RealEstate> = listOf(
        Asset.RealEstate(
            id = "real_estate_1",
            currentValue = value,
            debtAmount = debt,
            acquisitionCost = acquisitionCost,
            holdingYears = holdingYears,
            category = RealEstateCategory.fromLegacyPrimary(isPrimaryResidence),
            isPrimaryResidence = isPrimaryResidence,
            saleYear = saleYear,
            expectedSalePrice = expectedSalePrice,
        ),
    )
}
