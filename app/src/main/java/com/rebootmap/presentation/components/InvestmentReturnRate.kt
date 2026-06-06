package com.rebootmap.presentation.components

import com.rebootmap.domain.model.InvestmentDefaults
import kotlin.math.roundToInt

object InvestmentReturnRate {
    const val MIN = 0.0
    const val MAX = 0.15

    /** 0.5%p 단위 (0.005) */
    const val STEP = 0.005

    /** 슬라이더 구간 수: 0%~15% (0.5%p 간격) → 30 steps */
    const val STEPS = 30

    const val DEFAULT_RATE = InvestmentDefaults.DEFAULT_RETURN_RATE

    fun snap(rate: Double): Double {
        val clamped = rate.coerceIn(MIN, MAX)
        val stepIndex = ((clamped - MIN) / STEP).roundToInt().coerceIn(0, STEPS)
        return (MIN + stepIndex * STEP).coerceIn(MIN, MAX)
    }

    fun increment(rate: Double): Double = snap(rate + STEP).coerceAtMost(MAX)

    fun decrement(rate: Double): Double = snap(rate - STEP).coerceAtLeast(MIN)

    fun canDecrement(rate: Double): Boolean = snap(rate) > MIN

    fun canIncrement(rate: Double): Boolean = snap(rate) < MAX

    /** 정수는 "5%", 소수는 "5.5%" 형식으로 통일 */
    fun formatPercent(rate: Double): String {
        val percent = snap(rate) * 100
        return if (percent % 1.0 == 0.0) {
            "${percent.toInt()}%"
        } else {
            "%.1f%%".format(percent)
        }
    }
}
