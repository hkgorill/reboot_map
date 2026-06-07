package com.rebootmap.domain.model

import com.rebootmap.domain.validation.isSimulationReady
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetEmploymentIncomeTest {

    @Test
    fun `월액만 있고 연령이 불완전하면 시뮬레이션 미반영`() {
        val income = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 0, endAge = 90)
        assertFalse(income.isSimulationReady())
    }

    @Test
    fun `시작 연령만 있으면 미반영`() {
        val income = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 60, endAge = 0)
        assertFalse(income.isSimulationReady())
    }

    @Test
    fun `연령 역전이어도 isSimulationReady는 true이나 엔진은 0`() {
        val income = Asset.EmploymentIncome(monthlyAmount = 2_000_000L, startAge = 70, endAge = 60)
        assertTrue(income.isSimulationReady())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 월액은 허용되지 않는다`() {
        Asset.EmploymentIncome(monthlyAmount = -1L, startAge = 0, endAge = 0)
    }
}
