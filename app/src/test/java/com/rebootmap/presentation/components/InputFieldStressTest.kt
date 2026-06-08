package com.rebootmap.presentation.components

import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.scenario.PurchaseTiming
import com.rebootmap.domain.scenario.RelocationPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Year

/**
 * UI 입력 필드 로직을 재현해 다양한 값·타이핑 시퀀스가 모델/엔진에서 크래시를 유발하지 않는지 검증한다.
 */
class InputFieldStressTest {

    private val engine = CashFlowEngine()
    private val currentYear = Year.now().value

    @Test
    fun `슬라이더로 수익률 변경 시 포커스 중에도 입력폼 텍스트 동기화`() {
        assertTrue(shouldSyncPercentTextFromValue("5", 0.07, isFocused = true))
        assertFalse(shouldSyncPercentTextFromValue("5", 0.05, isFocused = true))
        assertTrue(shouldSyncPercentTextFromValue("5", 0.05, isFocused = false))
    }

    @Test
    fun `coerceIntPreservingZero는 0을 구간 하한으로 바꾸지 않는다`() {
        assertEquals(0, coerceIntPreservingZero(0, 55..75))
        assertEquals(0, coerceIntPreservingZero(0, 18..100))
        assertEquals(0, coerceIntPreservingZero(0, currentYear..(currentYear + 50)))
        assertEquals(55, coerceIntPreservingZero(6, 55..75))
        assertEquals(18, coerceIntPreservingZero(9, 18..100))
    }

    @Test
    fun `단일 자릿수 타이핑은 validRange 밖이면 모델에 반영되지 않는다`() {
        val singles = (1..9).toList()
        for (digit in singles) {
            assertFalse(isIntInputAllowed(digit, 55..75))
            assertFalse(isIntInputAllowed(digit, 18..100))
        }
        assertTrue(isIntInputAllowed(0, 55..75))
        assertTrue(isIntInputAllowed(65, 55..75))
    }

    @Test
    fun `국민연금 수령 연령 — 허용값과 onCommit 보정 후 모델 생성`() {
        val base = Asset.NationalPension(monthlyPayout = 0L, startAge = 0)
        for (candidate in testIntCandidates()) {
            if (isIntInputAllowed(candidate, 55..75)) {
                base.copy(startAge = candidate) // throws면 실패
            }
            base.copy(startAge = coerceIntPreservingZero(candidate, 55..75))
        }
    }

    @Test
    fun `직장 소득 연령 — 편집 중간 상태와 역전 순서 허용`() {
        val base = Asset.EmploymentIncome(monthlyAmount = 0L, startAge = 0, endAge = 0)
        val combos = listOf(
            0 to 0, 0 to 90, 60 to 0, 60 to 90, 70 to 60, 18 to 100, 100 to 100,
        )
        for ((start, end) in combos) {
            if (isIntInputAllowed(start, 18..100) || start == 0) {
                if (isIntInputAllowed(end, 18..100) || end == 0) {
                    base.copy(startAge = start, endAge = end)
                }
            }
        }
        // 타이핑 시퀀스: 60세 시작 후 종료 연령을 지우고 다시 입력
        simulateTyping("90", 18..100) { parsed ->
            if (isIntInputAllowed(parsed, 18..100)) {
                base.copy(startAge = 60, endAge = parsed)
            }
        }
    }

    @Test
    fun `모든 자산 카드 Int 필드 — 후보값 onCommit 시뮬레이션`() {
        val assets = SimulationStateMapper.emptyAssets()
        for (candidate in testIntCandidates()) {
            applyIntFieldStress(assets, candidate)
        }
    }

    @Test
    fun `만원 입력 — 다양한 금액이 모델을 깨뜨리지 않는다`() {
        val amounts = listOf(0L, 1L, 10_000L, 1_000_000L, 999_999_999_999L)
        val base = SimulationStateMapper.emptyAssets()
        for (amount in amounts) {
            val updated = base.map { asset ->
                when (asset) {
                    is Asset.RealEstate -> asset.copy(currentValue = amount, debtAmount = amount / 2)
                    is Asset.NationalPension -> asset.copy(monthlyPayout = amount)
                    is Asset.Investment -> asset.copy(currentValue = amount)
                    is Asset.EmploymentIncome -> asset.copy(monthlyAmount = amount)
                    is Asset.BusinessIncome -> asset.copy(monthlyAmount = amount)
                    is Asset.OtherFixedIncome -> asset.copy(monthlyAmount = amount)
                    is Asset.CashSavings -> asset.copy(maturityAmount = amount)
                    else -> asset
                }
            }
            runEngine(updated)
        }
    }

    @Test
    fun `투자 수익률·물가상승률 — 후보 비율 onCommit`() {
        val investment = Asset.Investment(currentValue = 10_000_000L, annualReturnRate = 0.0)
        val rates = listOf(0.0, 0.005, 0.05, 0.2, 0.5, 1.0, -0.5, -0.1, 1.5, -0.9)
        for (rate in rates) {
            if (isPercentInputAllowed(rate, -0.5..1.0)) {
                investment.copy(annualReturnRate = rate)
            }
            investment.copy(annualReturnRate = coercePercentPreservingZero(rate, -0.5..1.0))
        }

        val assumptions = EconomicAssumptions()
        for (rate in rates) {
            if (isPercentInputAllowed(rate, 0.0..0.2)) {
                assumptions.copy(inflationRate = rate)
            }
            assumptions.copy(inflationRate = coercePercentPreservingZero(rate, 0.0..0.2))
        }
    }

    @Test
    fun `저장 기본값 로드 후 시뮬레이션`() {
        val persisted = SimulationPersistedState(onboardingCompleted = true)
        val state = SimulationStateMapper.toUiState(persisted)
        runEngine(state.assets)
    }

    @Test
    fun `이주 시나리오 N년 — 1~5 및 중간 입력`() {
        for (years in 0..10) {
            val safe = if (years == 0) years else years.coerceIn(1, 5)
            if (safe in 1..5) {
                PurchaseTiming.BeforeSale(safe)
                PurchaseTiming.AfterSale(safe)
            }
        }
    }

    @Test
    fun `랜덤 조합 200건 시뮬레이션 엔진`() {
        val ageSeed = listOf(0, 18, 55, 60, 65, 70, 75, 80, 90, 100)
        val pensionStartSeed = listOf(0, 55, 60, 65, 70, 75)
        repeat(200) { index ->
            val assets = SimulationStateMapper.defaultAssets(
                realEstateValue = (index % 50) * 100_000_000L,
                realEstateDebt = (index % 30) * 50_000_000L,
                nationalMonthly = (index % 10) * 100_000L,
                nationalStartAge = pensionStartSeed[index % pensionStartSeed.size],
                investmentValue = (index % 20) * 5_000_000L,
                investmentReturnRate = listOf(-0.3, 0.0, 0.05, 0.15, 0.5)[index % 5],
                employmentMonthly = if (index % 3 == 0) 2_000_000L else 0L,
                employmentStartAge = ageSeed[(index + 1) % ageSeed.size],
                employmentEndAge = ageSeed[(index + 2) % ageSeed.size],
            )
            runEngine(assets)
        }
    }

    private fun applyIntFieldStress(assets: List<Asset>, candidate: Int) {
        assets.forEach { asset ->
            when (asset) {
                is Asset.RealEstate -> {
                    if (isIntInputAllowed(candidate, 0..50)) {
                        asset.copy(holdingYears = candidate)
                    }
                    asset.copy(holdingYears = coerceIntPreservingZero(candidate, 0..50))
                    val acqRange = (currentYear - 50)..(currentYear + 30)
                    if (isIntInputAllowed(candidate, acqRange)) {
                        val acq = candidate.takeIf { it != 0 }
                        if (acq == null || asset.saleYear == null || acq <= asset.saleYear) {
                            asset.copy(acquisitionYear = acq)
                        }
                    }
                    if (isIntInputAllowed(candidate, currentYear..(currentYear + 50))) {
                        val sale = candidate.takeIf { it > currentYear }
                        if (sale == null || asset.acquisitionYear == null || asset.acquisitionYear <= sale) {
                            asset.copy(saleYear = sale)
                        }
                    }
                }
                is Asset.NationalPension -> {
                    if (isIntInputAllowed(candidate, 55..75)) {
                        asset.copy(startAge = candidate)
                    }
                    asset.copy(startAge = coerceIntPreservingZero(candidate, 55..75))
                }
                is Asset.SeverancePension -> {
                    if (isIntInputAllowed(candidate, 18..100)) {
                        asset.copy(contributionEndAge = candidate)
                    }
                    asset.copy(contributionEndAge = coerceIntPreservingZero(candidate, 18..100))
                    if (isIntInputAllowed(candidate, 55..70)) {
                        asset.copy(payoutStartAge = candidate)
                    }
                    asset.copy(payoutStartAge = coerceIntPreservingZero(candidate, 55..70))
                }
                is Asset.PersonalPension -> {
                    if (isIntInputAllowed(candidate, 18..100)) {
                        asset.copy(contributionEndAge = candidate)
                    }
                    asset.copy(contributionEndAge = coerceIntPreservingZero(candidate, 18..100))
                    if (isIntInputAllowed(candidate, 55..70)) {
                        asset.copy(payoutStartAge = candidate)
                    }
                    asset.copy(payoutStartAge = coerceIntPreservingZero(candidate, 55..70))
                }
                is Asset.YellowUmbrella -> {
                    if (isIntInputAllowed(candidate, 18..100)) {
                        asset.copy(contributionEndAge = candidate)
                    }
                    asset.copy(contributionEndAge = coerceIntPreservingZero(candidate, 18..100))
                    if (isIntInputAllowed(candidate, 55..70)) {
                        asset.copy(payoutAge = candidate)
                    }
                    asset.copy(payoutAge = coerceIntPreservingZero(candidate, 55..70))
                }
                is Asset.CashSavings -> {
                    if (isIntInputAllowed(candidate, currentYear..(currentYear + 50))) {
                        asset.copy(maturityYear = candidate)
                    }
                }
                is Asset.HousingPension -> {
                    if (isIntInputAllowed(candidate, 55..80)) {
                        asset.copy(startAge = candidate)
                    }
                    asset.copy(startAge = coerceIntPreservingZero(candidate, 55..80))
                }
                is Asset.EmploymentIncome, is Asset.BusinessIncome, is Asset.OtherFixedIncome -> {
                    if (isIntInputAllowed(candidate, 18..100)) {
                        when (asset) {
                            is Asset.EmploymentIncome -> {
                                asset.copy(startAge = candidate)
                                asset.copy(endAge = candidate)
                                asset.copy(startAge = 60, endAge = candidate)
                                asset.copy(startAge = candidate, endAge = 90)
                            }
                            is Asset.BusinessIncome -> {
                                asset.copy(startAge = candidate, endAge = candidate)
                            }
                            is Asset.OtherFixedIncome -> {
                                asset.copy(startAge = candidate, endAge = candidate)
                            }
                            else -> Unit
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun simulateTyping(target: String, range: IntRange, apply: (Int) -> Unit) {
        var buffer = ""
        for (ch in target) {
            buffer += ch
            val parsed = buffer.toIntOrNull() ?: 0
            apply(parsed)
        }
        val committed = coerceIntPreservingZero(buffer.toIntOrNull() ?: 0, range)
        apply(committed)
    }

    private fun testIntCandidates(): List<Int> =
        listOf(0) + (1..17) + (18..25) + listOf(54, 55, 59, 60, 64, 65, 66, 70, 75, 76, 80, 89, 90, 99, 100, 101, 150, 999)

    private fun runEngine(assets: List<Asset>) {
        val input = SimulationInput(
            profile = UserProfile(currentAge = 45, retirementAge = 60, lifeExpectancy = 90),
            assumptions = EconomicAssumptions(inflationRate = 0.02),
            assets = assets,
            startYear = currentYear,
            relocationPlan = RelocationPlan(
                enabled = true,
                newHomeValue = 300_000_000L,
                newHomeDebt = 100_000_000L,
                purchaseTiming = PurchaseTiming.BeforeSale(2),
            ),
        )
        engine.project(input)
    }
}
