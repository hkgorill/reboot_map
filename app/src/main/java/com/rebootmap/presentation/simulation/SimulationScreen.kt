package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.MoneyInputField
import com.rebootmap.presentation.components.PercentInputField
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reboot Map",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.projection?.let { projection ->
                item {
                    ResultSummaryCard(projection = projection)
                }
            }

            item {
                SectionCard(title = "기본 정보", icon = Icons.Outlined.Payments) {
                    IntInputField(
                        label = "현재 나이",
                        value = state.profile.currentAge,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(currentAge = age.coerceIn(18, 100)),
                            )
                        },
                    )
                    IntInputField(
                        label = "목표 은퇴 연령",
                        value = state.profile.retirementAge,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(retirementAge = age.coerceIn(state.profile.currentAge, 100)),
                            )
                        },
                    )
                    IntInputField(
                        label = "기대 수명",
                        value = state.profile.lifeExpectancy,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(lifeExpectancy = age.coerceAtLeast(state.profile.retirementAge)),
                            )
                        },
                    )
                    MoneyInputField(
                        label = "목표 월 생활비 (원)",
                        value = state.profile.monthlyLivingExpense,
                        onValueChange = { amount ->
                            viewModel.updateProfile(state.profile.copy(monthlyLivingExpense = amount))
                        },
                        supportingText = "은퇴 후 매월 필요한 생활비",
                    )
                }
            }

            item {
                SectionCard(title = "경제 가정", icon = Icons.Outlined.AccountBalance) {
                    PercentInputField(
                        label = "물가상승률 (%)",
                        value = state.assumptions.inflationRate,
                        onValueChange = { rate ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(inflationRate = rate.coerceIn(0.0, 0.2)),
                            )
                        },
                    )
                }
            }

            itemsIndexed(state.assets) { index, asset ->
                AssetInputCard(
                    asset = asset,
                    onAssetChange = { updated -> viewModel.updateAsset(index, updated) },
                )
            }
        }
    }
}

@Composable
private fun AssetInputCard(
    asset: Asset,
    onAssetChange: (Asset) -> Unit,
) {
    when (asset) {
        is Asset.RealEstate -> SectionCard(title = "부동산", icon = Icons.Outlined.Home) {
            MoneyInputField(
                label = "현재 시세 (원)",
                value = asset.currentValue,
                onValueChange = { onAssetChange(asset.copy(currentValue = it)) },
            )
            IntInputField(
                label = "매각 예정 연도",
                value = asset.saleYear ?: 0,
                onValueChange = { year ->
                    onAssetChange(asset.copy(saleYear = year.takeIf { it > Year.now().value }))
                },
                supportingText = "비워두면 매각 없음",
            )
        }

        is Asset.NationalPension -> SectionCard(title = "국민연금", icon = Icons.Outlined.Payments) {
            MoneyInputField(
                label = "예상 월 수령액 (원)",
                value = asset.monthlyPayout,
                onValueChange = { onAssetChange(asset.copy(monthlyPayout = it)) },
            )
            IntInputField(
                label = "수령 시작 연령",
                value = asset.startAge,
                onValueChange = { onAssetChange(asset.copy(startAge = it.coerceIn(55, 75))) },
            )
        }

        is Asset.RetirementPension -> SectionCard(title = "퇴직·개인연금", icon = Icons.Outlined.Savings) {
            MoneyInputField(
                label = "현재 잔액 (원)",
                value = asset.balance,
                onValueChange = { onAssetChange(asset.copy(balance = it)) },
            )
            MoneyInputField(
                label = "월 납입액 (원)",
                value = asset.monthlyContribution,
                onValueChange = { onAssetChange(asset.copy(monthlyContribution = it)) },
            )
            IntInputField(
                label = "납입 종료 연령",
                value = asset.contributionEndAge,
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it.coerceIn(18, 100))) },
            )
        }

        is Asset.Investment -> SectionCard(title = "주식·재테크", icon = Icons.Outlined.TrendingUp) {
            MoneyInputField(
                label = "현재 평가 자산 (원)",
                value = asset.currentValue,
                onValueChange = { onAssetChange(asset.copy(currentValue = it)) },
            )
            PercentInputField(
                label = "예상 연 수익률 (%)",
                value = asset.annualReturnRate,
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it.coerceIn(-0.5, 1.0))) },
            )
        }

        is Asset.CashSavings -> SectionCard(title = "현금·적금", icon = Icons.Outlined.Savings) {
            MoneyInputField(
                label = "만기 금액 (원)",
                value = asset.maturityAmount,
                onValueChange = { onAssetChange(asset.copy(maturityAmount = it)) },
            )
            IntInputField(
                label = "만기 연도",
                value = asset.maturityYear,
                onValueChange = { onAssetChange(asset.copy(maturityYear = it)) },
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}
