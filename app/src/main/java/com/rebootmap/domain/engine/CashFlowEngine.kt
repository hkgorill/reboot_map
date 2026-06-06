package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.YearSnapshot
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 세후 현금흐름을 연 단위로 시뮬레이션하는 순수 계산 엔진.
 * Android Framework에 의존하지 않습니다.
 */
class CashFlowEngine {

    fun project(input: SimulationInput): CashFlowProjection {
        val profile = input.profile.normalized()
        val assumptions = input.assumptions
        val startYear = input.startYear
        val endYear = startYear + (profile.lifeExpectancy - profile.currentAge)

        val retirementPensions = input.assets.filterIsInstance<Asset.RetirementPension>()
        val nationalPensions = input.assets.filterIsInstance<Asset.NationalPension>()
        val investments = input.assets.filterIsInstance<Asset.Investment>()
        val cashSavings = input.assets.filterIsInstance<Asset.CashSavings>()
        val realEstates = input.assets.filterIsInstance<Asset.RealEstate>()

        val pensionBalances = retirementPensions.associateWith { it.balance }.toMutableMap()
        var investmentValue = investments.sumOf { it.currentValue }
        val investmentRate = investments.firstOrNull()?.annualReturnRate ?: 0.0
        val soldRealEstates = mutableSetOf<String>()
        val maturedSavings = mutableSetOf<String>()

        var liquidBalance = 0L
        val snapshots = mutableListOf<YearSnapshot>()
        var depletionYear: Int? = null
        val deficitYears = mutableListOf<Int>()

        for (year in startYear..endYear) {
            val age = profile.currentAge + (year - startYear)
            var annualIncome = 0L
            var pensionIncome = 0L
            var otherIncome = 0L

            retirementPensions.forEach { pension ->
                val balance = pensionBalances.getValue(pension)
                if (age < pension.contributionEndAge) {
                    pensionBalances[pension] = balance + pension.monthlyContribution * 12
                } else if (age >= profile.retirementAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    pensionBalances[pension] = balance - withdrawal
                    pensionIncome += withdrawal
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
                    otherIncome += estate.currentValue
                    soldRealEstates.add(estate.id)
                }
            }

            if (investmentValue > 0 && investmentRate != 0.0) {
                investmentValue = (investmentValue * (1.0 + investmentRate)).roundToLong()
            }

            annualIncome = pensionIncome + otherIncome

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

            var netCashFlow = annualIncome - annualExpense - annualTax

            if (netCashFlow < 0) {
                val deficit = -netCashFlow
                val fromInvestment = investmentValue.coerceAtMost(deficit)
                investmentValue -= fromInvestment
                val remainingDeficit = deficit - fromInvestment

                val totalPensionBalance = pensionBalances.values.sum()
                val fromPension = totalPensionBalance.coerceAtMost(remainingDeficit)
                if (fromPension > 0) {
                    distributeWithdrawal(pensionBalances, fromPension)
                }

                val fromLiquid = liquidBalance.coerceAtMost(remainingDeficit - fromPension)
                liquidBalance -= fromLiquid

                netCashFlow = annualIncome - annualExpense - annualTax - deficit
            } else {
                liquidBalance += netCashFlow
                netCashFlow = 0L
            }

            val totalAssets = liquidBalance +
                investmentValue +
                pensionBalances.values.sum() +
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
        .sumOf { it.currentValue }

    private fun distributeWithdrawal(
        balances: MutableMap<Asset.RetirementPension, Long>,
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
