package com.rebootmap.presentation.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.matching.AssetSuggestion
import com.rebootmap.domain.milestone.ExpenseCategory
import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.coerceIntPreservingZero
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.formatKoreanMan
import java.time.Year

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MilestoneTimelineCard(
    expenses: List<LumpSumExpense>,
    expenseMatches: Map<String, List<AssetSuggestion>>,
    currentAge: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: (LumpSumExpense) -> Unit,
    onUpdate: (LumpSumExpense) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentYear = Year.now().value
    val summary = when {
        expenses.isEmpty() -> "지출 이벤트 없음"
        else -> "${expenses.size}건 · 총 ${formatKoreanMan(expenses.sumOf { it.amount })}"
    }

    ExpandableCard(
        title = "목돈 지출 타임라인",
        summary = summary,
        icon = Icons.Outlined.Event,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        Text(
            text = "결혼·교육·주거 등 대형 지출을 연도별로 배치하면 현금흐름·차트에 반영됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (expenses.isNotEmpty()) {
            MilestoneScheduleSummary(expenses = expenses, currentYear = currentYear)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        expenses.sortedBy { it.year }.forEach { expense ->
            MilestoneExpenseItem(
                expense = expense,
                suggestions = expenseMatches[expense.id].orEmpty(),
                currentYear = currentYear,
                onUpdate = onUpdate,
                onRemove = { onRemove(expense.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        AddMilestoneForm(
            currentYear = currentYear,
            currentAge = currentAge,
            onAdd = onAdd,
        )
    }
}

@Composable
private fun MilestoneScheduleSummary(
    expenses: List<LumpSumExpense>,
    currentYear: Int,
) {
    val sorted = expenses.sortedBy { it.year }
    val firstYear = sorted.first().year
    val lastYear = sorted.last().year

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "지출 일정 요약",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${currentYear}년 ~ ${lastYear}년 · ${expenses.size}건 · 총 ${formatKoreanMan(expenses.sumOf { it.amount })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        sorted.forEach { expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${expense.year}년",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.displayLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${expense.category.label} · ${formatKoreanMan(expense.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (firstYear > currentYear) {
            Text(
                text = "첫 지출까지 ${firstYear - currentYear}년",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MilestoneExpenseItem(
    expense: LumpSumExpense,
    suggestions: List<AssetSuggestion>,
    currentYear: Int,
    onUpdate: (LumpSumExpense) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.displayLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${expense.category.label} · ${formatKoreanMan(expense.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "삭제")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "지출 연도", style = MaterialTheme.typography.bodyMedium)
            IconButton(
                onClick = {
                    onUpdate(expense.copy(year = (expense.year - 1).coerceAtLeast(currentYear)))
                },
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "1년 앞당기기")
            }
            Text(
                text = "${expense.year}년",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = { onUpdate(expense.copy(year = expense.year + 1)) },
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "1년 미루기")
            }
        }

        if (suggestions.isNotEmpty()) {
            Text(
                text = "추천 자산",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            suggestions.forEach { suggestion ->
                val covers = suggestion.availableAmount >= expense.amount
                Text(
                    text = buildString {
                        append(suggestion.assetLabel)
                        append(" · ")
                        append(formatKoreanMan(suggestion.availableAmount))
                        append(" (")
                        append(suggestion.timingNote)
                        append(")")
                        if (covers) append(" ✓")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else {
            Text(
                text = "해당 연도에 매칭 가능한 자산이 없습니다. 투자·적금·부동산 매각 연도를 확인하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMilestoneForm(
    currentYear: Int,
    currentAge: Int,
    onAdd: (LumpSumExpense) -> Unit,
) {
    var category by remember { mutableStateOf<ExpenseCategory?>(null) }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableLongStateOf(0L) }
    var year by remember { mutableIntStateOf(currentYear + 5) }

    Text(
        text = "새 지출 추가",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpenseCategory.entries.forEach { item ->
            FilterChip(
                selected = category == item,
                onClick = {
                    category = item
                    label = item.label
                },
                label = { Text(item.label) },
            )
        }
    }

    OutlinedTextField(
        value = label,
        onValueChange = { label = it },
        label = { Text("지출 이름") },
        placeholder = { Text(category?.label ?: "카테고리 선택 또는 직접 입력") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    ManWonInputField(
        label = "지출 금액",
        valueInWon = amount,
        onValueChange = { amount = it },
    )

    val maxYear = currentYear + (100 - currentAge)
    IntInputField(
        label = "지출 연도",
        value = year,
        validRange = currentYear..maxYear,
        onValueChange = { year = it },
        onCommit = { year = coerceIntPreservingZero(it, currentYear..maxYear) },
        supportingText = "현재 ${currentYear}년 · ± 버튼으로 기존 항목 연도 조정",
    )

    Button(
        onClick = {
            val selectedCategory = category ?: return@Button
            if (amount <= 0) return@Button
            val safeYear = year.coerceIn(currentYear, currentYear + (100 - currentAge))
            onAdd(
                LumpSumExpense(
                    label = label.ifBlank { selectedCategory.label },
                    category = selectedCategory,
                    amount = amount,
                    year = safeYear,
                ),
            )
            category = null
            label = ""
            amount = 0L
            year = currentYear + 5
        },
        enabled = amount > 0 && category != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Text(text = "  타임라인에 추가")
    }
}
