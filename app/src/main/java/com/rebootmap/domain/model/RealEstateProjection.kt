package com.rebootmap.domain.model

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 매각 예정 연도·예상 매각가로부터 연평균 시세 변동률(CAGR)을 계산하고
 * 시뮬레이션 연도별 부동산 시세·순자산을 추정한다.
 *
 * 산식: r = (예상매각가 / 현재시세)^(1/년수) − 1
 *       시세(t) = 현재시세 × (1 + r)^t  (매각 연도에는 예상매각가 고정)
 */
object RealEstateProjection {

    fun hasAppreciationPath(estate: Asset.RealEstate, startYear: Int): Boolean {
        val saleYear = estate.saleYear ?: return false
        return saleYear > startYear &&
            estate.expectedSalePrice > 0 &&
            estate.currentValue > 0
    }

    fun annualRate(estate: Asset.RealEstate, startYear: Int): Double {
        if (!hasAppreciationPath(estate, startYear)) return 0.0
        val years = estate.saleYear!! - startYear
        return (estate.expectedSalePrice.toDouble() / estate.currentValue)
            .pow(1.0 / years) - 1.0
    }

    fun projectedGrossValue(estate: Asset.RealEstate, simYear: Int, startYear: Int): Long {
        if (estate.currentValue <= 0) return 0L
        if (!hasAppreciationPath(estate, startYear)) return estate.currentValue

        val saleYear = estate.saleYear!!
        if (simYear >= saleYear) return estate.expectedSalePrice

        val yearsElapsed = (simYear - startYear).coerceAtLeast(0)
        val totalYears = saleYear - startYear
        if (yearsElapsed >= totalYears) return estate.expectedSalePrice
        if (yearsElapsed == 0) return estate.currentValue

        val rate = annualRate(estate, startYear)
        return (estate.currentValue * (1.0 + rate).pow(yearsElapsed.toDouble())).roundToLong()
    }

    fun projectedNetEquity(estate: Asset.RealEstate, simYear: Int, startYear: Int): Long {
        val gross = projectedGrossValue(estate, simYear, startYear)
        if (gross <= 0) return 0L
        return (gross - estate.debtAmount.coerceAtMost(gross)).coerceAtLeast(0L)
    }

    fun formatAnnualRate(rate: Double): String {
        val percent = rate * 100
        val sign = if (percent > 0) "+" else ""
        return if (kotlin.math.abs(percent % 1.0) < 0.05) {
            "$sign${percent.toInt()}%/년"
        } else {
            "$sign%.1f%%/년".format(percent)
        }
    }
}
