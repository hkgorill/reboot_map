package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.advisory.AssetAdvisoryReport
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed

@Composable
fun AssetAdvisoryCard(
    report: AssetAdvisoryReport,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "자산운용 총평",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ScoreBadge(score = report.score, grade = report.gradeLabel)
            }
            Text(
                text = report.headline,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            )
            if (report.strengths.isNotEmpty()) {
                AdvisorySection(title = "잘된 점", items = report.strengths, positive = true)
            }
            if (report.weaknesses.isNotEmpty()) {
                AdvisorySection(title = "부족한 점", items = report.weaknesses, positive = false)
            }
            if (report.watchPoints.isNotEmpty()) {
                AdvisorySection(title = "유의할 점", items = report.watchPoints, positive = null)
            }
            Text(
                text = "※ 교육·계획 참고용이며 투자·세무 자문이 아닙니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ScoreBadge(score: Int, grade: String) {
    val color = when (grade) {
        "양호" -> SuccessGreen
        "위험", "주의" -> WarningRed
        else -> MaterialTheme.colorScheme.primary
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "${score}점",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = grade,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun AdvisorySection(
    title: String,
    items: List<String>,
    positive: Boolean?,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = when (positive) {
            true -> SuccessGreen
            false -> WarningRed
            null -> MaterialTheme.colorScheme.onSecondaryContainer
        },
    )
    items.forEach { item ->
        Text(
            text = "· $item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
