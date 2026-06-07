package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.PersonalLoanCategory
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.PercentInputField
import com.rebootmap.presentation.components.formatKoreanMan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalLoanCardFields(
    loan: PersonalLoan,
    loanOrdinal: Int,
    loanCount: Int,
    onLoanChange: (PersonalLoan) -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "유형",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonalLoanCategory.entries.forEach { category ->
                FilterChip(
                    selected = loan.category == category,
                    onClick = { onLoanChange(loan.copy(category = category)) },
                    label = { Text(category.displayLabel()) },
                )
            }
        }

        ManWonInputField(
            label = "현재 잔액 (원금)",
            valueInWon = loan.balance,
            onValueChange = { onLoanChange(loan.copy(balance = it)) },
        )
        PercentInputField(
            label = "연 이자율",
            value = loan.annualInterestRate,
            onValueChange = { onLoanChange(loan.copy(annualInterestRate = it)) },
            validRange = 0.0..0.5,
            supportingText = "신용대출·차용 금리 (예: 6~12%)",
        )
        ManWonInputField(
            label = "월 상환액 (원리금)",
            valueInWon = loan.monthlyPayment,
            onValueChange = { onLoanChange(loan.copy(monthlyPayment = it)) },
            supportingText = "비우면 이자만 연간 부담으로 계산",
        )
        IntInputField(
            label = "상환 종료 연령",
            value = loan.repaymentEndAge,
            validRange = 0..100,
            onValueChange = { onLoanChange(loan.copy(repaymentEndAge = it)) },
            onCommit = { onLoanChange(loan.copy(repaymentEndAge = it)) },
            supportingText = "0 = 잔액 소진 시까지",
        )

        if (loan.balance > 0) {
            val annualInterest = (loan.balance * loan.annualInterestRate).toLong()
            Text(
                text = "연 이자(참고): ${formatKoreanMan(annualInterest)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (onRemove != null) {
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (loanCount > 1) "부채 ${loanOrdinal + 1} 삭제" else "부채 삭제",
                )
            }
        }
    }
}

fun PersonalLoan.displayTitle(ordinal: Int, count: Int): String = when {
    count > 1 -> "신용·차용 ${ordinal + 1}"
    else -> "신용·차용 부채"
}

fun PersonalLoan.summaryText(): String = when {
    balance <= 0 -> "미입력"
    monthlyPayment > 0 -> "${category.displayLabel()} · ${formatKoreanMan(balance)} · 월 ${formatKoreanMan(monthlyPayment)}"
    else -> "${category.displayLabel()} · ${formatKoreanMan(balance)} · 이자 ${(annualInterestRate * 100).toInt()}%"
}

private fun PersonalLoanCategory.displayLabel(): String = when (this) {
    PersonalLoanCategory.BANK_CREDIT -> "신용대출"
    PersonalLoanCategory.PRIVATE_LOAN -> "지인 차용"
    PersonalLoanCategory.OTHER -> "기타"
}
