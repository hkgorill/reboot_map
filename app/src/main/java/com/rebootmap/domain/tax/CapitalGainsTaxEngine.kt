package com.rebootmap.domain.tax

import kotlin.math.roundToLong

/**
 * 양도소득세 간이 계산 엔진 (Phase 3).
 *
 * 2024년 1세대 1주택 비과세 요건을 단순화해 적용하고,
 * 과세 시 장기보유 공제 후 누진세율(간이)을 적용합니다.
 */
object CapitalGainsTaxEngine {

    /** 장기보유특별공제: 보유 연수당 3%, 최대 30% (10년 이상) */
    private const val HOLDING_DEDUCTION_PER_YEAR = 0.03
    private const val MAX_HOLDING_DEDUCTION = 0.30

    /** 1세대 1주택 비과세 최소 보유·거주 연수 */
    const val PRIMARY_RESIDENCE_MIN_YEARS = 2

    data class Input(
        val salePrice: Long,
        val acquisitionCost: Long,
        val holdingYears: Int,
        val isPrimaryResidence: Boolean = true,
    )

    data class Result(
        val taxableGain: Long,
        val tax: Long,
        val isExempt: Boolean,
        val exemptionReason: String?,
    )

    fun calculate(input: Input): Result {
        val salePrice = input.salePrice.coerceAtLeast(0)
        val acquisitionCost = input.acquisitionCost.coerceAtLeast(0)
        val gain = (salePrice - acquisitionCost).coerceAtLeast(0)

        if (gain == 0L) {
            return Result(
                taxableGain = 0L,
                tax = 0L,
                isExempt = true,
                exemptionReason = "양도차익 없음",
            )
        }

        if (input.isPrimaryResidence && input.holdingYears >= PRIMARY_RESIDENCE_MIN_YEARS) {
            return Result(
                taxableGain = 0L,
                tax = 0L,
                isExempt = true,
                exemptionReason = "1세대 1주택 비과세 (보유 ${PRIMARY_RESIDENCE_MIN_YEARS}년 이상)",
            )
        }

        val holdingDeduction = (input.holdingYears * HOLDING_DEDUCTION_PER_YEAR)
            .coerceIn(0.0, MAX_HOLDING_DEDUCTION)
        val taxableGain = (gain * (1.0 - holdingDeduction)).roundToLong()
        val tax = progressiveTax(taxableGain)

        return Result(
            taxableGain = taxableGain,
            tax = tax,
            isExempt = false,
            exemptionReason = null,
        )
    }

    /**
     * 양도소득 누진세율 간이 (2024년 기본세율 구간 근사, 원 단위).
     * 1,400만 이하 6% · 5,000만 이하 15% · 8,800만 이하 24% · 1.5억 이하 35% · 초과 45%
     */
    internal fun progressiveTax(taxableGain: Long): Long {
        if (taxableGain <= 0) return 0L

        var remaining = taxableGain
        var tax = 0.0

        val brackets = listOf(
            14_000_000L to 0.06,
            50_000_000L to 0.15,
            88_000_000L to 0.24,
            150_000_000L to 0.35,
            Long.MAX_VALUE to 0.45,
        )

        var previousLimit = 0L
        for ((limit, rate) in brackets) {
            val bracketSize = if (limit == Long.MAX_VALUE) remaining else (limit - previousLimit).coerceAtLeast(0)
            val inBracket = remaining.coerceAtMost(bracketSize)
            if (inBracket <= 0) break
            tax += inBracket * rate
            remaining -= inBracket
            previousLimit = limit
        }

        return tax.roundToLong()
    }
}
