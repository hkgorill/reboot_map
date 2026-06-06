package com.rebootmap.presentation.simulation

import com.rebootmap.presentation.components.InvestmentReturnRate
import com.rebootmap.presentation.components.formatKoreanMan

object PresetHints {
    fun manWon(value: Long): String? =
        if (value > 0) "참고: ${formatKoreanMan(value)}" else null

    fun age(value: Int): String? =
        if (value > 0) "참고: ${value}세" else null

    fun year(value: Int): String? =
        if (value > 0) "참고: ${value}년" else null

    fun percent(rate: Double): String? =
        if (rate > 0.0) "참고: ${InvestmentReturnRate.formatPercent(rate)}" else null

    fun ageRange(startAge: Int, endAge: Int): String? = when {
        startAge > 0 && endAge > 0 -> "참고: ${startAge}~${endAge}세"
        startAge > 0 -> age(startAge)
        endAge > 0 -> age(endAge)
        else -> null
    }

    fun withBase(base: String, hint: String?): String =
        if (hint != null) "$base · $hint" else base
}
