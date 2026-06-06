package com.rebootmap.data.model

import com.rebootmap.domain.milestone.ExpenseCategory
import com.rebootmap.domain.milestone.LumpSumExpense
import kotlinx.serialization.Serializable

@Serializable
data class PersistedLumpSumExpense(
    val id: String,
    val label: String,
    val categoryCode: String,
    val amount: Long,
    val year: Int,
) {
    fun toDomain(): LumpSumExpense = LumpSumExpense(
        id = id,
        label = label,
        category = categoryCode.toExpenseCategory(),
        amount = amount,
        year = year,
    )

    companion object {
        fun fromDomain(expense: LumpSumExpense): PersistedLumpSumExpense = PersistedLumpSumExpense(
            id = expense.id,
            label = expense.label,
            categoryCode = expense.category.name,
            amount = expense.amount,
            year = expense.year,
        )
    }
}

private fun String.toExpenseCategory(): ExpenseCategory =
    ExpenseCategory.entries.firstOrNull { it.name == this } ?: ExpenseCategory.OTHER
