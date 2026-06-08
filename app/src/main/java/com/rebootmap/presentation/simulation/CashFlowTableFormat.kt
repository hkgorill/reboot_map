package com.rebootmap.presentation.simulation

import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.presentation.components.formatKoreanMan

object CashFlowTableFormat {

    fun monthlyIncome(snapshot: YearSnapshot): Long = snapshot.incomeBreakdown.recurringTotal / 12

    fun monthlyLiving(snapshot: YearSnapshot): Long = snapshot.annualLivingExpense / 12

    fun monthlyTax(snapshot: YearSnapshot): Long = snapshot.recurringAnnualTaxBurden / 12

    fun monthlyNet(snapshot: YearSnapshot): Long = snapshot.recurringNetCashFlow / 12

    fun compactLine(snapshot: YearSnapshot, labelPrefix: String? = null): String {
        val prefix = labelPrefix?.let { "$it " } ?: ""
        val income = monthlyIncome(snapshot)
        val living = monthlyLiving(snapshot)
        val tax = monthlyTax(snapshot)
        val net = monthlyNet(snapshot)
        return buildString {
            append(prefix)
            append("${snapshot.year}년(${snapshot.age}세)")
            append(" · 수입 ${formatAmount(income)}")
            append(" · 생활 ${formatAmount(living)}")
            append(" · 세금 ${formatAmount(tax)}")
            append(" · 순 ${formatAmount(net)}")
            if (snapshot.incomeBreakdown.lumpSumTotal > 0) {
                append(" · 일시 ${formatKoreanMan(snapshot.incomeBreakdown.lumpSumTotal)}")
            }
        }
    }

    private fun formatAmount(monthly: Long): String =
        if (monthly != 0L) formatKoreanMan(monthly) else "0"
}
