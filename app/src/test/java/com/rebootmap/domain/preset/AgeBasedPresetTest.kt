package com.rebootmap.domain.preset

import com.rebootmap.domain.model.Asset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeBasedPresetTest {

    @Test
    fun `40대 프리셋은 5억 부동산과 300만원 생활비를 기본값으로 제공한다`() {
        val preset = AgeBasedPreset.forAge(42)

        assertEquals(42, preset.profile.currentAge)
        assertEquals(300L * 10_000, preset.profile.monthlyLivingExpense)

        val realEstate = preset.assets.filterIsInstance<Asset.RealEstate>().first()
        assertEquals(50_000L * 10_000, realEstate.currentValue)
        assertEquals(17_500L * 10_000, realEstate.debtAmount)
        assertEquals(32_500L * 10_000, realEstate.netEquity)
    }

    @Test
    fun `나이가 바뀌면 연령대별 국민연금 기본값이 달라진다`() {
        val thirties = AgeBasedPreset.forAge(35)
        val fifties = AgeBasedPreset.forAge(55)

        val pension30 = thirties.assets.filterIsInstance<Asset.NationalPension>().first()
        val pension50 = fifties.assets.filterIsInstance<Asset.NationalPension>().first()

        assertTrue(pension50.monthlyPayout > pension30.monthlyPayout)
    }

    @Test
    fun `60대 이상은 퇴직연금과 개인연금 월 납입이 0이다`() {
        val preset = AgeBasedPreset.forAge(63)
        val severance = preset.assets.filterIsInstance<Asset.SeverancePension>().first()
        val personal = preset.assets.filterIsInstance<Asset.PersonalPension>().first()
        val yellow = preset.assets.filterIsInstance<Asset.YellowUmbrella>().first()

        assertEquals(0L, severance.monthlyContribution)
        assertEquals(0L, personal.monthlyContribution)
        assertEquals(0L, yellow.monthlyContribution)
    }

    @Test
    fun `프리셋은 연금 3종과 고정수입을 포함한다`() {
        val preset = AgeBasedPreset.forAge(45)

        assertEquals(1, preset.assets.filterIsInstance<Asset.SeverancePension>().size)
        assertEquals(1, preset.assets.filterIsInstance<Asset.PersonalPension>().size)
        assertEquals(1, preset.assets.filterIsInstance<Asset.YellowUmbrella>().size)
        assertEquals(1, preset.assets.filterIsInstance<Asset.EmploymentIncome>().size)
    }

    @Test
    fun `만원 단위 변환 헬퍼가 올바르게 동작한다`() {
        assertEquals(300_000_000L, AgeBasedPreset.manWon(30_000))
    }
}
