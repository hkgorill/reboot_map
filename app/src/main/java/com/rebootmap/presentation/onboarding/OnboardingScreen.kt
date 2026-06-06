package com.rebootmap.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.ManWonInputField

@Composable
fun OnboardingScreen(
    onComplete: (currentAge: Int, retirementAge: Int, monthlyLivingExpense: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var currentAge by remember { mutableIntStateOf(40) }
    var retirementAge by remember { mutableIntStateOf(60) }
    var monthlyExpense by remember { mutableLongStateOf(3_000_000L) }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Reboot Map",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "3가지 질문으로\n나의 노후 현금흐름을 확인해 보세요",
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${step + 1} / 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedContent(targetState = step, label = "onboarding_step") { currentStep ->
                    when (currentStep) {
                        0 -> OnboardingStep(
                            title = "현재 나이가 어떻게 되시나요?",
                            subtitle = "통계 기반 평균 자산값이 자동으로 채워집니다",
                        ) {
                            IntInputField(
                                label = "현재 나이",
                                value = currentAge,
                                onValueChange = { currentAge = it },
                            )
                        }
                        1 -> OnboardingStep(
                            title = "언제 은퇴를 계획하고 계신가요?",
                            subtitle = "목표 은퇴 연령을 입력해 주세요",
                        ) {
                            IntInputField(
                                label = "목표 은퇴 연령",
                                value = retirementAge,
                                onValueChange = { retirementAge = it },
                            )
                        }
                        else -> OnboardingStep(
                            title = "은퇴 후 월 생활비는 얼마로 잡을까요?",
                            subtitle = "물가상승을 반영해 시뮬레이션합니다",
                        ) {
                            ManWonInputField(
                                label = "목표 월 생활비",
                                valueInWon = monthlyExpense,
                                onValueChange = { monthlyExpense = it },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (step < 2) {
                            step++
                        } else {
                            onComplete(currentAge, retirementAge, monthlyExpense)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = when (step) {
                        0 -> currentAge in 18..100
                        1 -> retirementAge in currentAge..100
                        else -> monthlyExpense > 0
                    },
                ) {
                    Text(if (step < 2) "다음" else "시뮬레이션 시작")
                }
                if (step > 0) {
                    TextButton(
                        onClick = { step-- },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("이전")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
