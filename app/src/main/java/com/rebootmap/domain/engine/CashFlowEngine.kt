package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.YearSnapshot
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

        val severanceBalances = severancePensions.associateWith { it.balance }.toMutableMap()
        val personalBalances = personalPensions.associateWith { it.balance }.toMutableMap()
        val yellowBalances = yellowUmbrellas.associateWith { it.balance }.toMutableMap()

        var investmentValue = investments.sumOf { it.currentValue }
        val investmentRate = investments.firstOrNull()?.annualReturnRate ?: 0.0
        val soldRealEstates = mutableSetOf<String>()
        val maturedSavings = mutableSetOf<String>()
        val paidYellowUmbrellas = mutableSetOf<String>()

        var liquidBalance = 0L
        val snapshots = mutableListOf<YearSnapshot>()
        var depletionYear: Int? = null
        val deficitYears = mutableListOf<Int>()

        for (year in startYear..endYear) {
            val age = profile.currentAge + (year - startYear)
            var pensionIncome = 0L
            var otherIncome = 0L

            severancePensions.forEach { pension ->
                val balance = severanceBalances.getValue(pension)
                if (age < pension.contributionEndAge) {
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
                if (age < pension.contributionEndAge) {
                    val contributed = balance + pension.monthlyContribution * 12
                    personalBalances[pension] = applyReturn(contributed, pension.annualReturnRate)
                } else if (age >= pension.payoutStartAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    personalBalances[pension] = balance - withdrawal
                    pensionIncome += withdrawal
                }
            }

            yellowUmbrellas.forEach { umbrella ->
                if (umbrella.id in paidYellowUmbrellas) return@forEach
                val balance = yellowBalances.getValue(umbrella)
                if (age < umbrella.payoutAge) {
                    var updated = applyReturn(balance, umbrella.annualReturnRate)
                    if (age < umbrella.contributionEndAge) {
                        updated += umbrella.monthlyContribution * 12
                    }
                    yellowBalances[umbrella] = updated
                } else if (balance > 0) {
                    otherIncome += balance
                    yellowBalances[umbrella] = 0L
                    paidYellowUmbrellas.add(umbrella.id)
                }
            }

            nationalPensions.forEach { pension ->
                if (age >= pension.startAge) {
                    pensionIncome += pension.monthlyPayout * 12
                }
            }

            cashSavings.forEach { saving ->
                if (year == saving.maturityYear && saving.id !in maturedSavings) {
                    otherIncome += saving.maturityAmount
                    maturedSavings.add(saving.id)
                }
            }

            realEstates.forEach { estate ->
                if (estate.saleYear == year && estate.id !in soldRealEstates) {
                    otherIncome += estate.netEquity
                    soldRealEstates.add(estate.id)
                }
            }

            fixedIncomes.forEach { income ->
                if (age in income.startAge..income.endAge) {
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
            if (otherIncome > 0) {
                annualTax += (otherIncome * assumptions.generalIncomeTaxRate).roundToLong()
            }

            val incomeGap = annualIncome - annualExpense - annualTax
            if (incomeGap < 0) {
                val deficit = -incomeGap
                val fromInvestment = investmentValue.coerceAtMost(deficit)
                investmentValue -= fromInvestment
                val remainingDeficit = deficit - fromInvestment

                val fromSeverance = severanceBalances.values.sum().coerceAtMost(remainingDeficit)
                if (fromSeverance > 0) {
                    distributeWithdrawal(severanceBalances, fromSeverance)
                }
                val afterSeverance = remainingDeficit - fromSeverance
                val fromPersonal = personalBalances.values.sum().coerceAtMost(afterSeverance)
                if (fromPersonal > 0) {
                    distributeWithdrawal(personalBalances, fromPersonal)
                }
                val afterPersonal = afterSeverance - fromPersonal
                val fromLiquid = liquidBalance.coerceAtMost(afterPersonal)
                liquidBalance -= fromLiquid
                val uncoveredDeficit = afterPersonal - fromLiquid
                if (uncoveredDeficit > 0) {
                    liquidBalance -= uncoveredDeficit
                }
            } else {
                liquidBalance += incomeGap
            }

            val totalAssets = liquidBalance +
                investmentValue +
                severanceBalances.values.sum() +
                personalBalances.values.sum() +
                yellowBalances.values.sum() +
                unsoldRealEstateValue(realEstates, soldRealEstates)

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
