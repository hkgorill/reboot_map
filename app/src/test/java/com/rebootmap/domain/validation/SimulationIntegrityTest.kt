package com.rebootmap.domain.validation

import com.rebootmap.data.mapper.SimulationStateMapper
import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.engine.CashFlowEngine
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.SimulationInput
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.preset.AgeBasedPreset
import com.rebootmap.presentation.simulation.SimulationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Year

class SimulationIntegrityTest {

    private val engine = CashFlowEngine()

    @Test
    fun `완전한 직장 소득은 정합성 경고 없이 시뮬레이션 반영`() {
        val asset = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 60, endAge = 70)
        val issues = SimulationIntegrity.validateAsset(asset)

        assertTrue(asset.isSimulationReady())
        assertFalse(issues.any { it.field.startsWith("employmentIncome") })
    }

    @Test
    fun `직장 소득 월액만 있으면 WARNING — 시뮬레이션 미반영`() {
        val asset = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 0, endAge = 90)
        val issues = SimulationIntegrity.validateAsset(asset)

        assertFalse(asset.isSimulationReady())
        assertTrue(issues.any { it.level == IntegrityLevel.WARNING && it.field == "employmentIncome.age" })
    }

    @Test
    fun `직장 소득 연령 역전은 WARNING — 엔진도 수입 미반영`() {
        val asset = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 70, endAge = 60)
        val issues = SimulationIntegrity.validateAsset(asset)

        assertTrue(asset.isSimulationReady())
        assertTrue(issues.any { it.message.contains("반영되지 않습니다") })

        val valid = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 60, endAge = 70)
        val validProjection = runEngine(listOf(valid))
        val invalidProjection = runEngine(listOf(asset))
        val incomeAt65Valid = validProjection.yearlySnapshots.find { it.age == 65 }?.annualIncome ?: 0L
        val incomeAt65Invalid = invalidProjection.yearlySnapshots.find { it.age == 65 }?.annualIncome ?: 0L
        assertTrue(incomeAt65Valid > 0L)
        assertEquals(0L, incomeAt65Invalid)
    }

    @Test
    fun `국민연금 수령액만 있으면 WARNING`() {
        val asset = Asset.NationalPension(monthlyPayout = 1_500_000L, startAge = 0)
        val issues = SimulationIntegrity.validateAsset(asset)

        assertFalse(asset.isSimulationReady())
        assertTrue(issues.any { it.field == "nationalPension.startAge" })
    }

    @Test
    fun `부동산 부채가 시세 초과면 ERROR`() {
        val asset = Asset.RealEstate(currentValue = 100_000_000L, debtAmount = 200_000_000L, saleYear = null)
        val issues = SimulationIntegrity.validateAsset(asset)

        assertTrue(issues.any { it.level == IntegrityLevel.ERROR })
    }

    @Test
    fun `저장-복원 왕복 후 자산·프로필 값이 일치한다`() {
        val original = SimulationUiState.afterOnboarding(45, 60, 3_000_000L).copy(
            isOnboardingCompleted = true,
            assets = SimulationStateMapper.defaultAssets(
                nationalMonthly = 1_200_000L,
                nationalStartAge = 65,
                employmentMonthly = 2_000_000L,
                employmentStartAge = 60,
                employmentEndAge = 85,
                investmentValue = 50_000_000L,
                investmentReturnRate = 0.05,
            ),
            assumptions = EconomicAssumptions(inflationRate = 0.03),
        )

        val restored = SimulationStateMapper.toUiState(SimulationStateMapper.toPersisted(original))

        assertEquals(original.profile, restored.profile)
        assertEquals(original.assumptions, restored.assumptions)
        assertNoIntegrityErrors(SimulationIntegrity.validateForSimulation(
            restored.profile,
            restored.assumptions,
            restored.assets,
        ))

        val fields = listOf(
            restored.assets.filterIsInstance<Asset.NationalPension>().first(),
            restored.assets.filterIsInstance<Asset.EmploymentIncome>().first(),
            restored.assets.filterIsInstance<Asset.Investment>().first(),
        )
        val originalFields = listOf(
            original.assets.filterIsInstance<Asset.NationalPension>().first(),
            original.assets.filterIsInstance<Asset.EmploymentIncome>().first(),
            original.assets.filterIsInstance<Asset.Investment>().first(),
        )
        assertEquals(originalFields, fields)
    }

    @Test
    fun `기대수명 미입력 시 엔진은 프리셋 fallback을 사용한다`() {
        val profile = UserProfile(currentAge = 45, retirementAge = 60, lifeExpectancy = 0)
        val preset = AgeBasedPreset.forAge(45)
        val normalized = profile.copy(
            lifeExpectancy = preset.profile.lifeExpectancy,
        )

        val endYearGap = normalized.lifeExpectancy - normalized.currentAge
        val projection = engine.project(
            SimulationInput(
                profile = normalized,
                assumptions = EconomicAssumptions(),
                assets = listOf(Asset.Investment(currentValue = 10_000_000L, annualReturnRate = 0.05)),
                startYear = Year.now().value,
            ),
        )

        assertEquals(endYearGap + 1, projection.yearlySnapshots.size)
    }

    @Test
    fun `입력 스트레스 후보 중 시뮬레이션 반영 자산은 ERROR 없음`() {
        val assets = SimulationStateMapper.defaultAssets(
            nationalMonthly = 1_000_000L,
            nationalStartAge = 65,
            employmentMonthly = 1_500_000L,
            employmentStartAge = 58,
            employmentEndAge = 80,
            investmentValue = 30_000_000L,
        )
        val profile = UserProfile(currentAge = 45, retirementAge = 60, lifeExpectancy = 90)

        assertNoIntegrityErrors(
            SimulationIntegrity.validateForSimulation(profile, EconomicAssumptions(), assets),
        )
    }

    @Test
    fun `구버전 fixedIncome 저장값은 employment로 이관된다`() {
        val legacy = SimulationPersistedState(
            onboardingCompleted = true,
            fixedIncomeMonthly = 2_500_000L,
            fixedIncomeStartAge = 50,
            fixedIncomeEndAge = 60,
        )
        val state = SimulationStateMapper.toUiState(legacy)
        val employment = state.assets.filterIsInstance<Asset.EmploymentIncome>().first()

        assertEquals(2_500_000L, employment.monthlyAmount)
        assertEquals(50, employment.startAge)
        assertEquals(60, employment.endAge)
        assertNoIntegrityErrors(
            SimulationIntegrity.validateForSimulation(state.profile, state.assumptions, state.assets),
        )
    }

    private fun assertNoIntegrityErrors(issues: List<IntegrityIssue>) {
        val errors = issues.filter { it.level == IntegrityLevel.ERROR }
        assertTrue("정합성 오류: $errors", errors.isEmpty())
    }

    private fun runEngine(assets: List<Asset>) = engine.project(
        SimulationInput(
            profile = UserProfile(currentAge = 45, retirementAge = 60, lifeExpectancy = 90),
            assumptions = EconomicAssumptions(),
            assets = assets.filter { it.isSimulationReady() },
            startYear = Year.now().value,
        ),
    )
}
