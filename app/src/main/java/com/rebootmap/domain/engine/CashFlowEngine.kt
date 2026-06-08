package com.rebootmap.domain.engine

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.LivingExpenseInflationBase
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.RealEstateProjection
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.portfolio.RealEstatePortfolioEngine
import com.rebootmap.domain.scenario.RelocationYearFlags
import com.rebootmap.domain.tax.AcquisitionTaxEngine
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import com.rebootmap.domain.tax.BrokerageFeeEngine
import com.rebootmap.domain.tax.CapitalGainsTaxEngine
import com.rebootmap.domain.tax.HealthInsurancePremiumEngine
import com.rebootmap.domain.tax.HousingPensionEngine
import com.rebootmap.domain.tax.IncomeTaxEngine
import com.rebootmap.domain.tax.PropertyHoldingTaxEngine
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
        val employmentIncomes = input.assets.filterIsInstance<Asset.EmploymentIncome>()
        val businessIncomes = input.assets.filterIsInstance<Asset.BusinessIncome>()
        val otherFixedIncomes = input.assets.filterIsInstance<Asset.OtherFixedIncome>()
        val realEstates = input.assets.filterIsInstance<Asset.RealEstate>()
        val housingPensions = input.assets.filterIsInstance<Asset.HousingPension>()
        val personalLoans = input.personalLoans.filter { it.isSimulationReady() }

        val severanceBalances = severancePensions.associateWith { it.balance }.toMutableMap()
        val personalBalances = personalPensions.associateWith { it.balance }.toMutableMap()
        val yellowBalances = yellowUmbrellas.associateWith { it.balance }.toMutableMap()

        var investmentValue = investments.sumOf { it.currentValue }
        val investmentRate = investments.firstOrNull()?.annualReturnRate ?: 0.0
        val soldRealEstates = mutableSetOf<String>()
        val maturedSavings = mutableSetOf<String>()
        val paidYellowUmbrellas = mutableSetOf<String>()

        var liquidBalance = 0L
        val loanBalances = personalLoans.associateWith { it.balance }.toMutableMap()
        val snapshots = mutableListOf<YearSnapshot>()
        var depletionYear: Int? = null
        val deficitYears = mutableListOf<Int>()

        for (year in startYear..endYear) {
            val age = profile.currentAge + (year - startYear)
            var nationalPensionIncome = 0L
            var severancePensionIncome = 0L
            var personalPensionIncome = 0L
            var housingPensionIncome = 0L
            var employmentIncome = 0L
            var businessIncome = 0L
            var otherFixedIncome = 0L
            var realEstateSaleProceeds = 0L
            var cashSavingsMaturity = 0L
            var yellowUmbrellaPayout = 0L
            var pensionIncome = 0L
            var otherIncome = 0L
            var capitalGainsTax = 0L
            var acquisitionTax = 0L
            var brokerageFee = 0L

            severancePensions.forEach { pension ->
                var balance = severanceBalances.getValue(pension)
                val payoutAge = pension.payoutStartAge.takeIf { it > 0 } ?: profile.retirementAge

                if (pension.contributionEndAge > 0 && age < pension.contributionEndAge) {
                    balance += pension.monthlyContribution * 12
                }
                if (balance > 0) {
                    balance = applyReturn(balance, pension.annualReturnRate)
                }
                if (age >= payoutAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    balance -= withdrawal
                    severancePensionIncome += withdrawal
                    pensionIncome += withdrawal
                }
                severanceBalances[pension] = balance
            }

            personalPensions.forEach { pension ->
                var balance = personalBalances.getValue(pension)
                if (pension.contributionEndAge > 0 && age < pension.contributionEndAge) {
                    balance += pension.monthlyContribution * 12
                }
                if (balance > 0) {
                    balance = applyReturn(balance, pension.annualReturnRate)
                }
                if (pension.payoutStartAge > 0 && age >= pension.payoutStartAge && balance > 0) {
                    val yearsRemaining = (profile.lifeExpectancy - age).coerceAtLeast(1)
                    val withdrawal = balance / yearsRemaining
                    balance -= withdrawal
                    personalPensionIncome += withdrawal
                    pensionIncome += withdrawal
                }
                personalBalances[pension] = balance
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
                    yellowUmbrellaPayout += balance
                    otherIncome += balance
                    yellowBalances[umbrella] = 0L
                    paidYellowUmbrellas.add(umbrella.id)
                }
            }

            nationalPensions.forEach { pension ->
                if (pension.startAge > 0 && age >= pension.startAge) {
                    val monthly = inflateFromSimulationStart(
                        baseMonthly = pension.monthlyPayout,
                        year = year,
                        startYear = startYear,
                        inflationRate = assumptions.inflationRate,
                    )
                    nationalPensionIncome += monthly * 12
                    pensionIncome += monthly * 12
                }
            }

            cashSavings.forEach { saving ->
                if (saving.maturityYear > 0 && year == saving.maturityYear && saving.id !in maturedSavings) {
                    cashSavingsMaturity += saving.maturityAmount
                    otherIncome += saving.maturityAmount
                    maturedSavings.add(saving.id)
                }
            }

            realEstates.forEach { estate ->
                val acqYear = estate.acquisitionYear
                if (acqYear != null && acqYear == year) {
                    val purchasePrice = estate.currentValue.coerceAtLeast(0L)
                    if (purchasePrice > 0) {
                        val othersAtBuy = RealEstatePortfolioEngine.homeCount(
                            realEstates,
                            year - 1,
                            startYear,
                            soldRealEstates,
                        )
                        acquisitionTax += AcquisitionTaxEngine.calculate(
                            AcquisitionTaxEngine.Input(
                                acquisitionPrice = purchasePrice,
                                category = estate.category,
                                otherHomesAtAcquisition = othersAtBuy,
                            ),
                        ).tax
                        brokerageFee += BrokerageFeeEngine.calculate(
                            purchasePrice,
                            BrokerageFeeEngine.Side.PURCHASE,
                        )
                    }
                }
            }

            realEstates.forEach { estate ->
                if (estate.saleYear == year && estate.id !in soldRealEstates) {
                    val grossAtSale = RealEstateProjection.projectedGrossValue(estate, year, startYear)
                    val netAtSale = RealEstateProjection.projectedNetEquity(estate, year, startYear)
                    val otherHomes = RealEstatePortfolioEngine.otherHomesAtSale(
                        estate,
                        realEstates,
                        year,
                        startYear,
                        soldRealEstates,
                    )
                    val tempExempt = RealEstatePortfolioEngine.qualifiesTemporaryTwoHomeExemption(
                        estate,
                        realEstates,
                        year,
                        startYear,
                        soldRealEstates,
                    )
                    val holdingYears = RealEstatePortfolioEngine.holdingYearsAtSale(
                        estate,
                        year,
                        startYear,
                    )

                    realEstateSaleProceeds += netAtSale
                    otherIncome += netAtSale
                    capitalGainsTax += CapitalGainsTaxEngine.calculate(
                        CapitalGainsTaxEngine.Input(
                            salePrice = grossAtSale.coerceAtLeast(netAtSale),
                            acquisitionCost = estate.effectiveAcquisitionCost,
                            holdingYears = holdingYears,
                            isPrimaryResidence = estate.isPrimaryResidence,
                            otherHomesAtSale = otherHomes,
                            temporaryTwoHomeExempt = tempExempt,
                        ),
                    ).tax
                    if (grossAtSale > 0) {
                        brokerageFee += BrokerageFeeEngine.calculate(
                            grossAtSale,
                            BrokerageFeeEngine.Side.SALE,
                        )
                    }
                    soldRealEstates.add(estate.id)
                }
            }

            housingPensions.forEach { pension ->
                if (pension.enabled && pension.startAge > 0 && age >= pension.startAge) {
                    val homeEquity = pension.homeEquityOverride.takeIf { it > 0 }
                        ?: RealEstatePortfolioEngine.ownedEstates(
                            realEstates,
                            year,
                            startYear,
                            soldRealEstates,
                        ).sumOf { RealEstateProjection.projectedNetEquity(it, year, startYear) }
                    val monthly = HousingPensionEngine.calculateMonthly(
                        HousingPensionEngine.Input(
                            homeEquity = homeEquity,
                            startAge = pension.startAge,
                            currentAge = age,
                            lifeExpectancy = profile.lifeExpectancy,
                        ),
                    ).monthlyPayout
                    val inflatedMonthly = inflatePensionPayout(
                        baseAnnual = monthly,
                        yearsSincePayoutStart = age - pension.startAge,
                        inflationRate = assumptions.inflationRate,
                    )
                    housingPensionIncome += inflatedMonthly * 12
                    pensionIncome += inflatedMonthly * 12
                }
            }

            employmentIncomes.forEach { income ->
                employmentIncome += annualRangedIncome(income.monthlyAmount, income.startAge, income.endAge, age)
            }
            businessIncomes.forEach { income ->
                businessIncome += annualRangedIncome(income.monthlyAmount, income.startAge, income.endAge, age)
            }
            otherFixedIncomes.forEach { income ->
                otherFixedIncome += annualRangedIncome(income.monthlyAmount, income.startAge, income.endAge, age)
            }

            if (investmentValue > 0 && investmentRate != 0.0) {
                investmentValue = applyReturn(investmentValue, investmentRate)
            }

            val incomeBreakdown = com.rebootmap.domain.tax.AnnualIncomeBreakdown(
                nationalPension = nationalPensionIncome,
                severancePension = severancePensionIncome,
                personalPension = personalPensionIncome,
                housingPension = housingPensionIncome,
                employmentIncome = employmentIncome,
                businessIncome = businessIncome,
                otherFixedIncome = otherFixedIncome,
                realEstateSale = realEstateSaleProceeds,
                cashSavingsMaturity = cashSavingsMaturity,
                yellowUmbrellaPayout = yellowUmbrellaPayout,
            )
            val annualIncome = incomeBreakdown.total

            val annualLivingExpense = if (age >= profile.retirementAge) {
                inflatedAnnualExpense(
                    monthlyExpense = profile.monthlyLivingExpense,
                    inflationRate = assumptions.inflationRate,
                    inflationYears = livingExpenseInflationYears(
                        year = year,
                        startYear = startYear,
                        age = age,
                        profile = profile,
                        base = assumptions.livingExpenseInflationBase,
                    ),
                )
            } else {
                0L
            }

            val countedEstates = RealEstatePortfolioEngine.ownedEstates(
                realEstates,
                year,
                startYear,
                soldRealEstates,
            )
            val illiquidForHolding = countedEstates
                .sumOf { RealEstateProjection.projectedNetEquity(it, year, startYear) }
            val holdingCost = PropertyHoldingTaxEngine.calculate(
                PropertyHoldingTaxEngine.Input(
                    estates = buildEstateHoldingLines(
                        estates = countedEstates,
                        year = year,
                        startYear = startYear,
                    ),
                    assumptions = assumptions,
                ),
            )
            var annualLoanRepayment = 0L
            personalLoans.forEach { loan ->
                val opening = loanBalances.getValue(loan)
                val result = PersonalLoanEngine.processYear(opening, loan, age) ?: return@forEach
                annualLoanRepayment += result.totalRepayment
                loanBalances[loan] = result.endingBalance
            }
            val annualExpense = annualLivingExpense + holdingCost.total + annualLoanRepayment

            val otherTaxableIncome = (otherIncome - realEstateSaleProceeds).coerceAtLeast(0)
            val incomeTaxBreakdown = IncomeTaxEngine.calculate(
                IncomeTaxEngine.Input(
                    pensionIncome = pensionIncome,
                    employmentIncome = employmentIncome,
                    businessIncome = businessIncome,
                    otherTaxableIncome = otherTaxableIncome + otherFixedIncome,
                    assumptions = assumptions,
                ),
            )

            val loanBalanceBeforePayment = loanBalances.values.sum()
            val financialAssetsPreview = liquidBalance + investmentValue +
                severanceBalances.values.sum() + personalBalances.values.sum() +
                yellowBalances.values.sum()
            val netFinancialForHealth = (financialAssetsPreview - loanBalanceBeforePayment).coerceAtLeast(0L)
            val healthResult = HealthInsurancePremiumEngine.calculate(
                HealthInsurancePremiumEngine.Input(
                    monthlyIncomeBasis = (pensionIncome + businessIncome + otherFixedIncome) / 12,
                    financialAssets = netFinancialForHealth,
                    realEstateNetEquity = illiquidForHolding,
                    age = age,
                    retirementAge = profile.retirementAge,
                    assumptions = assumptions,
                ),
            )

            val taxBreakdown = incomeTaxBreakdown.copy(
                capitalGainsTax = capitalGainsTax,
                acquisitionTax = acquisitionTax,
                brokerageFee = brokerageFee,
                healthInsurance = healthResult.annualHealthInsurance,
                longTermCare = healthResult.annualLongTermCare,
            )
            val annualTax = taxBreakdown.totalTax

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

            realEstates.forEach { estate ->
                if (estate.acquisitionYear == year && estate.currentValue > 0) {
                    val downPayment = (estate.currentValue - estate.debtAmount).coerceAtLeast(0L)
                    if (downPayment > 0) {
                        val result = applyOutflow(
                            amount = downPayment,
                            investmentValue = investmentValue,
                            severanceBalances = severanceBalances,
                            personalBalances = personalBalances,
                            liquidBalance = liquidBalance,
                        )
                        investmentValue = result.investmentValue
                        liquidBalance = result.liquidBalance
                    }
                }
            }

            val illiquidAssets = illiquidForHolding
            val liquidAssets = liquidBalance +
                investmentValue +
                severanceBalances.values.sum() +
                personalBalances.values.sum() +
                yellowBalances.values.sum()
            val personalLoanBalance = loanBalances.values.sum()
            val grossAssets = liquidAssets + illiquidAssets
            val totalAssets = grossAssets - personalLoanBalance
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
                    liquidAssets = liquidAssets,
                    illiquidAssets = illiquidAssets,
                    totalAssets = totalAssets,
                    annualIncome = annualIncome,
                    incomeBreakdown = incomeBreakdown,
                    annualExpense = annualExpense,
                    annualLivingExpense = annualLivingExpense,
                    annualHoldingCost = holdingCost,
                    annualTax = annualTax,
                    taxBreakdown = taxBreakdown,
                    netCashFlow = annualIncome - annualExpense - annualTax,
                    endingBalance = endingBalance,
                    relocationFlags = RealEstatePortfolioEngine.portfolioFlags(
                        realEstates,
                        year,
                        startYear,
                        soldRealEstates,
                    ),
                    personalLoanBalance = personalLoanBalance,
                    annualLoanRepayment = annualLoanRepayment,
                ),
            )
        }

        return CashFlowProjection(
            yearlySnapshots = snapshots,
            depletionYear = depletionYear,
            deficitYears = deficitYears,
        )
    }

    private fun annualRangedIncome(monthlyAmount: Long, startAge: Int, endAge: Int, age: Int): Long {
        if (monthlyAmount <= 0 || startAge <= 0 || endAge <= 0 || age !in startAge..endAge) {
            return 0L
        }
        return monthlyAmount * 12
    }

    private fun applyReturn(amount: Long, rate: Double): Long {
        if (amount <= 0 || rate == 0.0) return amount
        return (amount * (1.0 + rate)).roundToLong()
    }

    private fun inflatedAnnualExpense(
        monthlyExpense: Long,
        inflationRate: Double,
        inflationYears: Int,
    ): Long {
        val multiplier = inflationMultiplier(inflationRate, inflationYears)
        return (monthlyExpense * 12 * multiplier).roundToLong()
    }

    private fun livingExpenseInflationYears(
        year: Int,
        startYear: Int,
        age: Int,
        profile: UserProfile,
        base: LivingExpenseInflationBase,
    ): Int = when (base) {
        LivingExpenseInflationBase.SIMULATION_START -> year - startYear
        LivingExpenseInflationBase.RETIREMENT_AGE -> (age - profile.retirementAge).coerceAtLeast(0)
    }

    /** 시뮬 시작 시점 기준 금액 → 해당 연도 명목가치 (국민연금 등) */
    private fun inflateFromSimulationStart(
        baseMonthly: Long,
        year: Int,
        startYear: Int,
        inflationRate: Double,
    ): Long {
        val multiplier = inflationMultiplier(inflationRate, year - startYear)
        return (baseMonthly * multiplier).roundToLong()
    }

    /** 수령 개시 후 매년 물가연동 (퇴직·개인연금·주택연금) */
    private fun inflatePensionPayout(
        baseAnnual: Long,
        yearsSincePayoutStart: Int,
        inflationRate: Double,
    ): Long {
        val multiplier = inflationMultiplier(inflationRate, yearsSincePayoutStart)
        return (baseAnnual * multiplier).roundToLong()
    }

    private fun inflationMultiplier(inflationRate: Double, years: Int): Double {
        if (years <= 0 || inflationRate == 0.0) return 1.0
        return (1.0 + inflationRate).pow(years.toDouble())
    }

    private fun buildEstateHoldingLines(
        estates: List<Asset.RealEstate>,
        year: Int,
        startYear: Int,
    ): List<PropertyHoldingTaxEngine.EstateLine> = estates.map { estate ->
        PropertyHoldingTaxEngine.EstateLine(
            netEquity = RealEstateProjection.projectedNetEquity(estate, year, startYear),
            category = estate.category,
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
