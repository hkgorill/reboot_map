package com.rebootmap.domain.tax

import com.rebootmap.domain.model.TaxDefaults
import kotlin.math.roundToLong

/** 부동산 중개보수 간이 (매수·매도 각각) */
object BrokerageFeeEngine {

    enum class Side { PURCHASE, SALE }

    fun calculate(transactionPrice: Long, side: Side = Side.SALE): Long {
        val price = transactionPrice.coerceAtLeast(0)
        if (price == 0L) return 0L
        return (price * TaxDefaults.BROKERAGE_FEE_RATE).roundToLong()
    }
}
