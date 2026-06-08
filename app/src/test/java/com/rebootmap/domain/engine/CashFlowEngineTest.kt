package com.rebootmap.domain.engine

import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.LivingExpenseInflationBase
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowEngineTest {

    private val engine = CashFlowEngine()
    private val baseYear = 2026

    /** Phase 5 이전 회귀 테스트용 — 보유세·건보 미반영 */
    private val legacyAssumptions = EconomicAssumptions(
        propertyTaxEnabled = false,
        comprehensiveRealEstateTaxEnabled = false,
        healthInsuranceEnabled = false,
    )

    private fun input(
        profile: UserProfile = UserProfile(),
        assumptions: EconomicAssumptions = EconomicAssumptions(),
        assets: List<Asset> = emptyList(),
        lumpSumExpenses: List<LumpSumExpense> = emptyList(),
    ) = SimulationInput(
        profile = profile,
        assumptions = assumptions,
        assets = assets,
        startYear = baseYear,
        lumpSumExpenses = lumpSumExpenses,
    )

    @Test
    fun `T01 - 자산 없이 은퇴 후 생활비만 발생하면 적자 연도가 생긴다`() {
        val profile = UserProfile(
            currentAge = 58,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 2_000_000L,
        )

        val result = engine.project(input(profile = profile))

        assertTrue(result.deficitYears.isNotEmpty())
        assertNotNull(result.depletionYear)
        assertTrue(result.finalBalance < 0)
    }

    @Test
    fun `T02 - 현금성 투자 1억으로 월 200만 생활비를 일정 기간 버틴다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 65,
            monthlyLivingExpense = 2_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 160_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))

        assertEquals(6, result.yearlySnapshots.size)
        assertTrue(result.finalBalance > 0)
    }

    @Test
    fun `T03 - 국민연금은 수령 시작 연령부터 연 수입에 반영된다`() {
        val profile = UserProfile(
            currentAge = 63,
            retirementAge = 60,
            lifeExpectancy = 66,
            monthlyLivingExpense = 500_000L,
        )
        val assets = listOf(
            Asset.NationalPension(monthlyPayout = 1_500_000L, startAge = 65),
            Asset.Investment(currentValue = 50_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(
            input(
                profile = profile,
                assets = assets,
                assumptions = EconomicAssumptions(inflationRate = 0.0),
            ),
        )
        val at64 = result.yearlySnapshots.first { it.age == 64 }
        val at65 = result.yearlySnapshots.first { it.age == 65 }

        assertEquals(0L, at64.annualIncome)
        assertEquals(18_000_000L, at65.annualIncome)
        assertEquals(at65.annualIncome, at65.incomeBreakdown.total)
        assertEquals(18_000_000L, at65.incomeBreakdown.nationalPension)
    }

    @Test
    fun `T03b - 국민연금 수령액은 시뮬 시작부터 물가상승이 반영된다`() {
        val profile = UserProfile(
            currentAge = 63,
            retirementAge = 60,
            lifeExpectancy = 66,
            monthlyLivingExpense = 500_000L,
        )
        val assets = listOf(
            Asset.NationalPension(monthlyPayout = 1_500_000L, startAge = 65),
            Asset.Investment(currentValue = 50_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(
            input(
                profile = profile,
                assets = assets,
                assumptions = EconomicAssumptions(inflationRate = 0.02),
            ),
        )
        val at65 = result.yearlySnapshots.first { it.age == 65 }

        assertEquals(18_727_200L, at65.annualIncome)
    }

    @Test
    fun `T04a - 퇴직연금은 수령 개시 연령 전까지 인출되지 않는다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 70,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.SeverancePension(
                balance = 100_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 60,
                payoutStartAge = 65,
            ),
            Asset.Investment(currentValue = 50_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at64 = result.yearlySnapshots.first { it.age == 64 }
        val at65 = result.yearlySnapshots.first { it.age == 65 }

        assertEquals(0L, at64.annualIncome)
        assertTrue(at65.annualIncome > 0L)
    }

    @Test
    fun `T04 - 퇴직연금은 납입 종료 전까지 적립되고 은퇴 후 인출된다`() {
        val profile = UserProfile(
            currentAge = 50,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.SeverancePension(
                balance = 10_000_000L,
                monthlyContribution = 500_000L,
                contributionEndAge = 55,
                annualReturnRate = 0.0,
            ),
            Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at53 = result.yearlySnapshots.first { it.age == 53 }
        val at54 = result.yearlySnapshots.first { it.age == 54 }

        assertTrue(at54.totalAssets > at53.totalAssets)
    }

    @Test
    fun `T04b - 유동·비유동·총자산 합계가 일치한다`() {
        val profile = UserProfile(currentAge = 48, retirementAge = 60, lifeExpectancy = 55)
        val assets = listOf(
            Asset.RealEstate(currentValue = 300_000_000L, debtAmount = 50_000_000L, saleYear = null),
            Asset.Investment(currentValue = 10_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        result.yearlySnapshots.forEach { snapshot ->
            assertEquals(snapshot.liquidAssets + snapshot.illiquidAssets, snapshot.totalAssets)
            assertEquals(snapshot.totalAssets, snapshot.endingBalance)
        }
        assertEquals(250_000_000L, result.yearlySnapshots.first().illiquidAssets)
    }

    @Test
    fun `T05 - 투자 자산은 연 수익률만큼 복리 성장한다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.07),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val afterOneYear = result.yearlySnapshots[0]

        assertEquals(107_000_000L, afterOneYear.totalAssets)
    }

    @Test
    fun `T06 - 적금은 만기 연도에 일시 유입된다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.CashSavings(maturityAmount = 30_000_000L, maturityYear = baseYear + 2),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val maturity = result.yearlySnapshots.first { it.year == baseYear + 2 }

        assertEquals(30_000_000L, maturity.annualIncome)
    }

    @Test
    fun `T07 - 부동산은 매각 연도에 일시 유입된다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(currentValue = 500_000_000L, saleYear = baseYear + 1),
        )

        val result = engine.project(
            input(profile = profile, assumptions = legacyAssumptions, assets = assets),
        )
        val beforeSale = result.yearlySnapshots.first { it.year == baseYear }
        val saleYear = result.yearlySnapshots.first { it.year == baseYear + 1 }

        assertEquals(500_000_000L, beforeSale.totalAssets)
        assertEquals(500_000_000L, saleYear.annualIncome)
        assertTrue(result.yearlySnapshots.last().totalAssets > 0L)
    }

    @Test
    fun `T07b - 예상 매각가에 따라 보유 중 순자산과 매각 수입이 연도별 반영된다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 46,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(
                currentValue = 400_000_000L,
                debtAmount = 100_000_000L,
                saleYear = baseYear + 5,
                expectedSalePrice = 500_000_000L,
            ),
        )

        val result = engine.project(
            input(profile = profile, assumptions = legacyAssumptions, assets = assets),
        )
        val year0 = result.yearlySnapshots.first { it.year == baseYear }
        val year3 = result.yearlySnapshots.first { it.year == baseYear + 3 }
        val sale = result.yearlySnapshots.first { it.year == baseYear + 5 }

        assertEquals(300_000_000L, year0.illiquidAssets)
        assertTrue(year3.illiquidAssets > year0.illiquidAssets)
        assertEquals(400_000_000L, sale.annualIncome)
    }

    @Test
    fun `T08 - 물가상승률 2퍼센트가 10년 후 생활비에 반영된다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 70,
            monthlyLivingExpense = 1_000_000L,
        )
        val assumptions = EconomicAssumptions(inflationRate = 0.02)
        val assets = listOf(
            Asset.Investment(currentValue = 1_000_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assumptions = assumptions, assets = assets))
        val year0 = result.yearlySnapshots.first()
        val year10 = result.yearlySnapshots.first { it.year == baseYear + 10 }

        assertEquals(12_000_000L, year0.annualExpense)
        assertEquals(14_627_933L, year10.annualExpense)
    }

    @Test
    fun `T09 - 복합 자산 시나리오에서 스냅샷 연도와 나이가 일관된다`() {
        val profile = UserProfile(
            currentAge = 55,
            retirementAge = 60,
            lifeExpectancy = 65,
            monthlyLivingExpense = 2_500_000L,
        )
        val assets = listOf(
            Asset.NationalPension(monthlyPayout = 1_200_000L, startAge = 65),
            Asset.SeverancePension(
                balance = 50_000_000L,
                monthlyContribution = 300_000L,
                contributionEndAge = 60,
            ),
            Asset.PersonalPension(
                balance = 20_000_000L,
                monthlyContribution = 200_000L,
                contributionEndAge = 60,
                payoutStartAge = 55,
            ),
            Asset.YellowUmbrella(
                balance = 10_000_000L,
                monthlyContribution = 100_000L,
                contributionEndAge = 60,
                payoutAge = 60,
            ),
            Asset.Investment(currentValue = 80_000_000L, annualReturnRate = 0.05),
            Asset.CashSavings(maturityAmount = 20_000_000L, maturityYear = baseYear + 3),
            Asset.RealEstate(currentValue = 300_000_000L, saleYear = baseYear + 5),
        )

        val result = engine.project(input(profile = profile, assets = assets))

        assertEquals(11, result.yearlySnapshots.size)
        result.yearlySnapshots.forEachIndexed { index, snapshot ->
            assertEquals(baseYear + index, snapshot.year)
            assertEquals(55 + index, snapshot.age)
        }
    }

    @Test
    fun `T10 - 기대수명이 은퇴연령과 같으면 1년 시뮬레이션`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 60,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 50_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))

        assertEquals(1, result.yearlySnapshots.size)
        assertEquals(60, result.yearlySnapshots.first().age)
    }

    @Test
    fun `T08b - 생활비 물가는 은퇴 시점 기준이면 은퇴 직후 목표 금액과 같다`() {
        val profile = UserProfile(
            currentAge = 48,
            retirementAge = 60,
            lifeExpectancy = 70,
            monthlyLivingExpense = 3_000_000L,
        )
        val assumptions = EconomicAssumptions(
            inflationRate = 0.02,
            livingExpenseInflationBase = LivingExpenseInflationBase.RETIREMENT_AGE,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 1_000_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assumptions = assumptions, assets = assets))
        val at60 = result.yearlySnapshots.first { it.age == 60 }
        val at70 = result.yearlySnapshots.first { it.age == 70 }

        assertEquals(36_000_000L, at60.annualExpense)
        assertEquals(43_883_799L, at70.annualExpense)
    }

    @Test
    fun `T08c - 생활비 물가가 현재부터 누적이면 은퇴 전에도 물가가 반영된다`() {
        val profile = UserProfile(
            currentAge = 48,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 3_000_000L,
        )
        val assumptions = EconomicAssumptions(
            inflationRate = 0.02,
            livingExpenseInflationBase = LivingExpenseInflationBase.SIMULATION_START,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 1_000_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assumptions = assumptions, assets = assets))
        val at60 = result.yearlySnapshots.first { it.age == 60 }

        assertEquals(45_656_705L, at60.annualExpense)
    }

    @Test
    fun `T11 - 물가상승률 0퍼센트면 생활비가 고정된다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 63,
            monthlyLivingExpense = 2_000_000L,
        )
        val assumptions = EconomicAssumptions(inflationRate = 0.0)
        val assets = listOf(
            Asset.Investment(currentValue = 500_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assumptions = assumptions, assets = assets))

        result.yearlySnapshots.forEach { snapshot ->
            assertEquals(24_000_000L, snapshot.annualExpense)
        }
    }

    @Test
    fun `T12 - 충분한 자산이면 고갈 연도가 없다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 65,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 1_000_000_000L, annualReturnRate = 0.03),
            Asset.NationalPension(monthlyPayout = 2_000_000L, startAge = 60),
        )

        val result = engine.project(input(profile = profile, assets = assets))

        assertNull(result.depletionYear)
        assertTrue(result.finalBalance > 0)
    }

    @Test
    fun `T13 - 연금 소득에 간이 연금소득세가 적용된다`() {
        val profile = UserProfile(
            currentAge = 65,
            retirementAge = 60,
            lifeExpectancy = 66,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.NationalPension(monthlyPayout = 1_000_000L, startAge = 65),
        )

        val result = engine.project(
            input(profile = profile, assumptions = legacyAssumptions, assets = assets),
        )
        val snapshot = result.yearlySnapshots.first()

        assertEquals(396_000L, snapshot.annualTax)
        assertEquals(396_000L, snapshot.taxBreakdown.pensionIncomeTax)
    }

    @Test
    fun `T14 - 은퇴 전에는 생활비가 발생하지 않는다`() {
        val profile = UserProfile(
            currentAge = 50,
            retirementAge = 60,
            lifeExpectancy = 55,
            monthlyLivingExpense = 3_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 10_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))

        result.yearlySnapshots.forEach { snapshot ->
            assertEquals(0L, snapshot.annualExpense)
        }
    }

    @Test
    fun `T16 - 부동산 부채는 순자산 기준으로 계산된다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(
                currentValue = 500_000_000L,
                debtAmount = 200_000_000L,
                saleYear = baseYear + 1,
            ),
        )

        val result = engine.project(
            input(profile = profile, assumptions = legacyAssumptions, assets = assets),
        )
        val beforeSale = result.yearlySnapshots.first { it.year == baseYear }
        val saleYear = result.yearlySnapshots.first { it.year == baseYear + 1 }

        assertEquals(300_000_000L, beforeSale.totalAssets)
        assertEquals(300_000_000L, saleYear.annualIncome)
    }

    @Test
    fun `T15 - 부동산 미매각 시 총자산에 포함된다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(currentValue = 200_000_000L, saleYear = null),
        )

        val result = engine.project(
            input(profile = profile, assumptions = legacyAssumptions, assets = assets),
        )

        result.yearlySnapshots.forEach { snapshot ->
            assertEquals(200_000_000L, snapshot.totalAssets)
        }
    }

    @Test
    fun `T19 - 개인연금은 수령 개시 연령부터 연 수입에 반영된다`() {
        val profile = UserProfile(
            currentAge = 53,
            retirementAge = 60,
            lifeExpectancy = 57,
            monthlyLivingExpense = 500_000L,
        )
        val assets = listOf(
            Asset.PersonalPension(
                balance = 60_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 53,
                payoutStartAge = 55,
                annualReturnRate = 0.0,
            ),
            Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at54 = result.yearlySnapshots.first { it.age == 54 }
        val at55 = result.yearlySnapshots.first { it.age == 55 }

        assertEquals(0L, at54.annualIncome)
        assertTrue(at55.annualIncome > 0)
    }

    @Test
    fun `T19a - 월 순현금 흑자여도 연금 인출로 총자산은 줄어들 수 있다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 2_000_000L,
        )
        val assets = listOf(
            Asset.SeverancePension(
                balance = 100_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 60,
                payoutStartAge = 60,
                annualReturnRate = 0.0,
            ),
        )

        val result = engine.project(
            input(
                profile = profile,
                assets = assets,
                assumptions = EconomicAssumptions(
                    inflationRate = 0.0,
                    pensionIncomeTaxRate = 0.0,
                    generalIncomeTaxRate = 0.0,
                ),
            ),
        )
        val at60 = result.yearlySnapshots.first { it.age == 60 }
        val at61 = result.yearlySnapshots.first { it.age == 61 }

        assertTrue(at60.netCashFlow > 0)
        assertTrue(at61.totalAssets < at60.totalAssets)
    }

    @Test
    fun `T19b - 퇴직연금 수령 중에도 운용 수익률이 잔액에 반영된다`() {
        val profile = UserProfile(
            currentAge = 65,
            retirementAge = 60,
            lifeExpectancy = 66,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.SeverancePension(
                balance = 100_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 60,
                payoutStartAge = 65,
                annualReturnRate = 0.10,
            ),
        )

        val result = engine.project(
            input(
                profile = profile,
                assets = assets,
                assumptions = EconomicAssumptions(inflationRate = 0.0),
            ),
        )
        val at65 = result.yearlySnapshots.first { it.age == 65 }

        // 1억 × 1.10 ÷ 1년 ≈ 1.1억 (물가연동 없음, 운용수익만)
        assertEquals(110_000_000L, at65.annualIncome)
    }

    @Test
    fun `T20 - 노랑우산공제는 수령 연령에 일시금으로 유입된다`() {
        val profile = UserProfile(
            currentAge = 58,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.YellowUmbrella(
                balance = 30_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 58,
                payoutAge = 60,
                annualReturnRate = 0.0,
            ),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at59 = result.yearlySnapshots.first { it.age == 59 }
        val at60 = result.yearlySnapshots.first { it.age == 60 }

        assertEquals(0L, at59.annualIncome)
        assertEquals(30_000_000L, at60.annualIncome)
    }

    @Test
    fun `T21 - 자산 소진 후 미충당 적자는 부채로 누적되어 마이너스 잔액이 된다`() {
        val profile = UserProfile(
            currentAge = 60,
            retirementAge = 60,
            lifeExpectancy = 61,
            monthlyLivingExpense = 2_000_000L,
        )
        val assumptions = EconomicAssumptions(inflationRate = 0.0)
        val assets = listOf(
            Asset.Investment(currentValue = 30_000_000L, annualReturnRate = 0.0),
        )

        val result = engine.project(
            input(profile = profile, assumptions = assumptions.copy(healthInsuranceEnabled = false), assets = assets),
        )
        val last = result.yearlySnapshots.last()

        assertNotNull(result.depletionYear)
        assertTrue(last.endingBalance < 0)
        assertEquals(-18_000_000L, last.endingBalance)
    }

    @Test
    fun `T22 - 고정수입은 지정 연령 구간 동안 연 수입에 반영된다`() {
        val profile = UserProfile(
            currentAge = 58,
            retirementAge = 60,
            lifeExpectancy = 62,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 60, endAge = 62),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at59 = result.yearlySnapshots.first { it.age == 59 }
        val at60 = result.yearlySnapshots.first { it.age == 60 }
        val at63 = result.yearlySnapshots.firstOrNull { it.age == 63 }

        assertEquals(0L, at59.annualIncome)
        assertEquals(24_000_000L, at60.annualIncome)
        assertNull(at63)
    }

    @Test
    fun `T23 - 1세대1주택 비과세 매각 시 양도소득세가 0이다`() {
        val profile = UserProfile(
            currentAge = 40,
            retirementAge = 65,
            lifeExpectancy = 42,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(
                currentValue = 500_000_000L,
                acquisitionCost = 300_000_000L,
                holdingYears = 5,
                isPrimaryResidence = true,
                saleYear = baseYear + 1,
            ),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val saleYear = result.yearlySnapshots.first { it.year == baseYear + 1 }

        assertEquals(500_000_000L, saleYear.annualIncome)
        assertEquals(0L, saleYear.taxBreakdown.capitalGainsTax)
        assertTrue(saleYear.taxBreakdown.brokerageFee > 0L)
    }

    @Test
    fun `T24 - 주택연금 개시 후 연 수입에 반영된다`() {
        val profile = UserProfile(
            currentAge = 63,
            retirementAge = 65,
            lifeExpectancy = 67,
            monthlyLivingExpense = 0L,
        )
        val assets = listOf(
            Asset.RealEstate(currentValue = 300_000_000L, saleYear = null),
            Asset.HousingPension(enabled = true, startAge = 65),
        )

        val result = engine.project(input(profile = profile, assets = assets))
        val at64 = result.yearlySnapshots.first { it.age == 64 }
        val at65 = result.yearlySnapshots.first { it.age == 65 }

        assertEquals(0L, at64.annualIncome)
        assertTrue(at65.annualIncome > 0)
    }

    @Test
    fun `T25 - 목돈 지출 연도에 자산이 차감된다`() {
        val profile = UserProfile(
            currentAge = 58,
            retirementAge = 58,
            lifeExpectancy = 62,
            monthlyLivingExpense = 1_000_000L,
        )
        val assets = listOf(
            Asset.Investment(currentValue = 100_000_000L, annualReturnRate = 0.0),
        )
        val expense = LumpSumExpense(
            label = "자녀 결혼",
            amount = 20_000_000L,
            year = baseYear,
        )

        val baseline = engine.project(input(profile = profile, assets = assets))
        val withExpense = engine.project(
            input(profile = profile, assets = assets, lumpSumExpenses = listOf(expense)),
        )

        val baselineBalance = baseline.yearlySnapshots.first { it.year == baseYear }.endingBalance
        val withExpenseBalance = withExpense.yearlySnapshots.first { it.year == baseYear }.endingBalance

        assertEquals(baselineBalance - 20_000_000L, withExpenseBalance)
    }
}
