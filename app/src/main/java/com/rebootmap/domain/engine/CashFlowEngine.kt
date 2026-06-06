package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.scenario.PurchaseTiming
import com.rebootmap.domain.scenario.RelocationPlan
import com.rebootmap.domain.tax.CapitalGainsTaxEngine
import com.rebootmap.domain.tax.HousingPensionEngine
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 세후 현금흐름을 연 단위로 시뮬레이션하는 순수 계산 엔진.
 */
class CashFlowEngine {

    fun project(input: SimulationInput): CashFlowProjection {
        val profile = input.profile.normalized()
        val assumptions = input.assumptions
        val startYear = input.startYear
        val endYear = startYear + (profile.lifeExpectancy - profile.currentAge)

        val severancePensions = input.assets.filterIsInstance<Asset.SeverancePension>()
        val personalPensions = input.assets.filterIsInstance<Asset.PersonalPension>()
        val yellowUmbrellas = input.assets.filterIsInstance<Asset.YellowUmbrella>()
        val nationalPensions = input.assets.filterIsInstance<Asset.NationalPension>()
        val investments = input.assets.filterIsInstance<Asset.Investment>()
        val cashSavings = input.assets.filterIsInstance<Asset.CashSavings>()
        val fixedIncomes = input.assets.filterIsInstance<Asset.FixedIncome>()
        val realEstates = input.assets.filterIsInstance<Asset.RealEstate>()
        val housingPensions = input.assets.filterIsInstance<Asset.HousingPension>()
        val relocation = input.relocationPlan?.takeIf { it.isConfigured() }
        val relocationSchedule = relocation?.let { buildRelocationSchedule(it, realEstates.firstOrNull()) }

        val severanceBalances = severancePensions.associateWith { it.balance }.toMutableMap()
        val personalBalances = personalPensions.associateWith { it.balance }.toMutableMap()
        val yellowBalances = yellowUmbrellas.associateWith { it.balance }.toMutableMap()

        var investmentValue = investments.sumOf { it.currentValue }
        val investmentRate = investments.firstOrNull()?.annualReturnRate ?: 0.0
        val soldRealEstates = mutableSetOf<String>()
        val maturedSavings = mutableSetOf<String>()
        val paidYellowUmbrellas = mutableSetOf<String>()

        var liquidBalance = 0L
        var newHomeOwned = relocationSchedule?.purchaseYear?.let { it < startYear } ?: false
        val snapshots = mutableListOf<YearSnapshot>()
        var depletionYear: Int? = null
        val deficitYears = mutableListOf<Int>()

        for (year in startYear..endYear) {
            val age = profile.currentAge + (year - startYear)
            var pensionIncome = 0L
            var otherIncome = 0L
            var realEstateSaleProceeds = 0L
            var capitalGainsTax = 0L

            severancePensions.forEach { pension ->
                val balance = severanceBalances.getValue(pension)
                if (pension.contributionEndAge > 0 && age < pension.contributionEndAge) {
                    val contributed = balance + pension.monthlyContribution * 12
                    severanceBalances[pension] = applyReturn(contributed, pension.annualReturnRate)
                } else if (age >= profile.retirementAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    severanceBalances[pension] = balance - withdrawal
                    pensionIncome += withdrawal
                }
            }

            personalPensions.forEach { pension ->
                val balance = personalBalances.getValue(pension)
                if (pension.contributionEndAge > 0 && age < pension.contributionEndAge) {
                    val contributed = balance + pension.monthlyContribution * 12
                    personalBalances[pension] = applyReturn(contributed, pension.annualReturnRate)
                } else if (pension.payoutStartAge > 0 && age >= pension.payoutStartAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    personalBalances[pension] = balance - withdrawal
                    pensionIncome += withdrawal
                }
            }

            yellowUmbrellas.forEach { umbrella ->
                if (umbrella.id in paidYellowUmbrellas) return@forEach
                val balance = yellowBalances.getValue(umbrella)
                if (umbrella.payoutAge > 0 && age < umbrella.payoutAge) {
                    var updated = applyReturn(balance, umbrella.annualReturnRate)
                    if (umbrella.contributionEndAge > 0 && age < umbrella.contributionEndAge) {
                        updated += umbrella.monthlyContribution * 12
                    }
                    yellowBalances[umbrella] = updated
                } else if (umbrella.payoutAge > 0 && balance > 0) {
                    otherIncome += balance
                    yellowBalances[umbrella] = 0L
                    paidYellowUmbrellas.add(umbrella.id)
                }
            }

            nationalPensions.forEach { pension ->
                if (pension.startAge > 0 && age >= pension.startAge) {
                    pensionIncome += pension.monthlyPayout * 12
                }
            }

            cashSavings.forEach { saving ->
                if (saving.maturityYear > 0 && year == saving.maturityYear && saving.id !in maturedSavings) {
                    otherIncome += saving.maturityAmount
                    maturedSavings.add(saving.id)
                }
            }

            realEstates.forEach { estate ->
                if (estate.saleYear == year && estate.id !in soldRealEstates) {
                    val twoHomeAtSale = relocationSchedule?.let { schedule ->
                        schedule.purchaseYear != null && schedule.purchaseYear < year
                    } ?: false
                    val isPrimaryForTax = estate.isPrimaryResidence && !twoHomeAtSale

                    otherIncome += estate.netEquity
                    realEstateSaleProceeds += estate.netEquity
                    capitalGainsTax += CapitalGainsTaxEngine.calculate(
                        CapitalGainsTaxEngine.Input(
                            salePrice = estate.netEquity,
                            acquisitionCost = estate.effectiveAcquisitionCost,
                            holdingYears = estate.holdingYears,
                            isPrimaryResidence = isPrimaryForTax,
                        ),
                    ).tax
                    soldRealEstates.add(estate.id)
                }
            }

            housingPensions.forEach { pension ->
                if (pension.enabled && pension.startAge > 0 && age >= pension.startAge) {
                    val homeEquity = pension.homeEquityOverride.takeIf { it > 0 }
                        ?: unsoldRealEstateValue(realEstates, soldRealEstates)
                    val monthly = HousingPensionEngine.calculateMonthly(
                        HousingPensionEngine.Input(
                            homeEquity = homeEquity,
                            startAge = pension.startAge,
                            currentAge = age,
                            lifeExpectancy = profile.lifeExpectancy,
                        ),
                    ).monthlyPayout
                    pensionIncome += monthly * 12
                }
            }

            fixedIncomes.forEach { income ->
                if (income.startAge > 0 && income.endAge > 0 && age in income.startAge..income.endAge) {
                    otherIncome += income.monthlyAmount * 12
                }
            }

            if (investmentValue > 0 && investmentRate != 0.0) {
                investmentValue = applyReturn(investmentValue, investmentRate)
            }

            val annualIncome = pensionIncome + otherIncome

            val annualExpense = if (age >= profile.retirementAge) {
                inflatedAnnualExpense(
                    monthlyExpense = profile.monthlyLivingExpense,
                    inflationRate = assumptions.inflationRate,
                    yearsFromStart = year - startYear,
                )
            } else {
                0L
            }

            var annualTax = 0L
            if (pensionIncome > 0) {
                annualTax += (pensionIncome * assumptions.pensionIncomeTaxRate).roundToLong()
            }
            val nonRealEstateOther = otherIncome - realEstateSaleProceeds
            if (nonRealEstateOther > 0) {
                annualTax += (nonRealEstateOther * assumptions.generalIncomeTaxRate).roundToLong()
            }
            annualTax += capitalGainsTax

            val incomeGap = annualIncome - annualExpense - annualTax
            if (incomeGap < 0) {
                val result = applyOutflow(
                    amount = -incomeGap,
                    investmentValue = investmentValue,
                    severanceBalances = severanceBalances,
                    personalBalances = personalBalances,
                    liquidBalance = liquidBalance,
                )
                investmentValue = result.investmentValue
                liquidBalance = result.liquidBalance
            } else {
                liquidBalance += incomeGap
            }

            val lumpSumOutflow = input.lumpSumExpenses
                .filter { it.year == year }
                .sumOf { it.amount }
            if (lumpSumOutflow > 0) {
                val result = applyOutflow(
                    amount = lumpSumOutflow,
                    investmentValue = investmentValue,
                    severanceBalances = severanceBalances,
                    personalBalances = personalBalances,
                    liquidBalance = liquidBalance,
                )
                investmentValue = result.investmentValue
                liquidBalance = result.liquidBalance
            }

            if (relocationSchedule?.purchaseYear == year && !newHomeOwned) {
                liquidBalance -= relocationSchedule.newHomeEquity
                newHomeOwned = true
            }

            val newHomeEquity = if (newHomeOwned) relocationSchedule?.newHomeEquity ?: 0L else 0L

            val totalAssets = liquidBalance +
                investmentValue +
                severanceBalances.values.sum() +
                personalBalances.values.sum() +
                yellowBalances.values.sum() +
                unsoldRealEstateValue(realEstates, soldRealEstates) +
                newHomeEquity

            val endingBalance = totalAssets

            if (age >= profile.retirementAge && (annualIncome - annualExpense - annualTax) < 0) {
                deficitYears.add(year)
            }

            if (depletionYear == null && age >= profile.retirementAge && endingBalance <= 0) {
                depletionYear = year
            }

            snapshots.add(
                YearSnapshot(
                    year = year,
                    age = age,
                    totalAssets = totalAssets,
                    annualIncome = annualIncome,
                    annualExpense = annualExpense,
                    annualTax = annualTax,
                    netCashFlow = annualIncome - annualExpense - annualTax,
                    endingBalance = endingBalance,
                ),
            )
        }

        return CashFlowProjection(
            yearlySnapshots = snapshots,
            depletionYear = depletionYear,
            deficitYears = deficitYears,
        )
    }

    private fun applyReturn(amount: Long, rate: Double): Long {
        if (amount <= 0 || rate == 0.0) return amount
        return (amount * (1.0 + rate)).roundToLong()
    }

    private fun inflatedAnnualExpense(
        monthlyExpense: Long,
        inflationRate: Double,
        yearsFromStart: Int,
    ): Long {
        val multiplier = (1.0 + inflationRate).pow(yearsFromStart.toDouble())
        return (monthlyExpense * 12 * multiplier).roundToLong()
    }

    private fun unsoldRealEstateValue(
        estates: List<Asset.RealEstate>,
        sold: Set<String>,
    ): Long = estates
        .filter { it.id !in sold }
        .sumOf { it.netEquity }

    private data class RelocationSchedule(
        val purchaseYear: Int?,
        val newHomeEquity: Long,
    )

    private fun buildRelocationSchedule(
        plan: RelocationPlan,
        estate: Asset.RealEstate?,
    ): RelocationSchedule? {
        val saleYear = estate?.saleYear ?: return null
        val purchaseYear = when (val timing = plan.purchaseTiming) {
            is PurchaseTiming.SameYearAsSale -> saleYear
            is PurchaseTiming.BeforeSale -> saleYear - timing.years
            is PurchaseTiming.AfterSale -> saleYear + timing.years
        }
        return RelocationSchedule(
            purchaseYear = purchaseYear,
            newHomeEquity = plan.newHomeEquity,
        )
    }

    private data class OutflowResult(
        val investmentValue: Long,
        val liquidBalance: Long,
    )

    private fun applyOutflow(
        amount: Long,
        investmentValue: Long,
        severanceBalances: MutableMap<Asset.SeverancePension, Long>,
        personalBalances: MutableMap<Asset.PersonalPension, Long>,
        liquidBalance: Long,
    ): OutflowResult {
        if (amount <= 0) {
            return OutflowResult(investmentValue = investmentValue, liquidBalance = liquidBalance)
        }

        var remaining = amount
        var updatedInvestment = investmentValue
        var updatedLiquid = liquidBalance

        val fromInvestment = updatedInvestment.coerceAtMost(remaining)
        updatedInvestment -= fromInvestment
        remaining -= fromInvestment

        val fromSeverance = severanceBalances.values.sum().coerceAtMost(remaining)
        if (fromSeverance > 0) {
            distributeWithdrawal(severanceBalances, fromSeverance)
        }
        remaining -= fromSeverance

        val fromPersonal = personalBalances.values.sum().coerceAtMost(remaining)
        if (fromPersonal > 0) {
            distributeWithdrawal(personalBalances, fromPersonal)
        }
        remaining -= fromPersonal

        val fromLiquid = updatedLiquid.coerceAtMost(remaining)
        updatedLiquid -= fromLiquid
        remaining -= fromLiquid

        if (remaining > 0) {
            updatedLiquid -= remaining
        }

        return OutflowResult(
            investmentValue = updatedInvestment,
            liquidBalance = updatedLiquid,
        )
    }

    private fun <T> distributeWithdrawal(
        balances: MutableMap<T, Long>,
        amount: Long,
    ) {
        val total = balances.values.sum()
        if (total <= 0) return

        var remaining = amount
        val keys = balances.keys.toList()
        keys.forEachIndexed { index, key ->
            val balance = balances.getValue(key)
            val share = if (index == keys.size - 1) {
                remaining
            } else {
                val proportional = amount * balance / total
                remaining -= proportional
                proportional
            }
            balances[key] = (balance - share).coerceAtLeast(0)
        }
    }
}
