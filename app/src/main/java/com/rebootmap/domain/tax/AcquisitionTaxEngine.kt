package com.rebootmap.domain.tax

import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.TaxDefaults
import kotlin.math.roundToLong

/** 취득세 간이 계산 (교육용) */
object AcquisitionTaxEngine {

    data class Input(
        val acquisitionPrice: Long,
        val category: RealEstateCategory = RealEstateCategory.PRIMARY_RESIDENCE,
        /** 취득 시점 기준 이미 보유 중인 다른 주택 수 */
        val otherHomesAtAcquisition: Int = 0,
    )

    data class Result(
        val tax: Long,
        val appliedRate: Double,
        val note: String,
    )

    fun calculate(input: Input): Result {
        val price = input.acquisitionPrice.coerceAtLeast(0)
        if (price == 0L) {
            return Result(0L, 0.0, "취득가 없음")
        }

        val rate = when {
            input.otherHomesAtAcquisition > 0 -> TaxDefaults.ACQUISITION_TAX_RATE_ADDITIONAL
            input.category == RealEstateCategory.PRIMARY_RESIDENCE ->
                TaxDefaults.ACQUISITION_TAX_RATE_PRIMARY
            else -> TaxDefaults.ACQUISITION_TAX_RATE_ADDITIONAL
        }
        val note = when {
            input.otherHomesAtAcquisition > 0 -> "다주택 취득 가중(간이)"
            else -> "1주택 취득(간이)"
        }
        return Result(
            tax = (price * rate).roundToLong(),
            appliedRate = rate,
            note = note,
        )
    }
}
