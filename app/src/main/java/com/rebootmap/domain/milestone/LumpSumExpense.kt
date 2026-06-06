package com.rebootmap.domain.milestone

import java.util.UUID

enum class ExpenseCategory(val label: String) {
    WEDDING("자녀 결혼"),
    EDUCATION("자녀 교육"),
    HOUSING("주거"),
    MEDICAL("의료"),
    OTHER("기타"),
}

data class LumpSumExpense(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Long,
    val year: Int,
) {
    init {
        require(label.isNotBlank()) { "지출 이름을 입력하세요." }
        require(amount > 0) { "지출 금액은 0보다 커야 합니다." }
        require(year in 1900..2200) { "지출 연도가 유효하지 않습니다." }
    }

    fun displayLabel(): String = label.ifBlank { category.label }
}
