package com.rebootmap.presentation.components

import java.text.NumberFormat
import java.util.Locale

private val koreanFormat = NumberFormat.getNumberInstance(Locale.KOREA)

fun formatNumberWithComma(value: Long): String = koreanFormat.format(value)

fun formatKoreanWon(amount: Long): String = "${koreanFormat.format(amount)}원"

fun formatKoreanMan(amount: Long): String {
    if (amount == 0L) return "0만원"
    if (amount < 0) return "-${formatKoreanMan(-amount)}"
    val man = amount / 10_000
    return if (man >= 10_000) {
        val eok = man / 10_000
        val remainMan = man % 10_000
        if (remainMan > 0) "${koreanFormat.format(eok)}억 ${koreanFormat.format(remainMan)}만원"
        else "${koreanFormat.format(eok)}억원"
    } else {
        "${koreanFormat.format(man)}만원"
    }
}
