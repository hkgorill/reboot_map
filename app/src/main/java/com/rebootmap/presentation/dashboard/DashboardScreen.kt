package com.rebootmap.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.Asset
import com.rebootmap.presentation.chart.CashFlowChartCard
import com.rebootmap.presentation.components.ExitConfirmBackHandler
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.ResetInputsConfirmDialog
import com.rebootmap.presentation.components.InvestmentReturnRate
import com.rebootmap.presentation.components.InvestmentReturnSlider
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.PercentInputField
import com.rebootmap.presentation.simulation.AssetCardFields
import com.rebootmap.presentation.simulation.PresetHints
import com.rebootmap.presentation.simulation.RelocationScenarioCard
import com.rebootmap.presentation.simulation.ResultSummaryCard
import com.rebootmap.presentation.simulation.SimulationViewModel
import com.rebootmap.presentation.simulation.displayTitle
import com.rebootmap.presentation.simulation.summaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    ExitConfirmBackHandler()

    if (showResetDialog) {
        ResetInputsConfirmDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetAllInputs()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Reboot Map", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "메뉴",
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("입력 정보 초기화") },
                            onClick = {
                                showMenu = false
                                showResetDialog = true
                            },
                        )
                    }
                },
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
                    ResultSummaryCard(
                        projection = projection,
                        retirementAge = state.profile.retirementAge,
                    )
                }

                item {
                    CashFlowChartCard(
                        projection = projection,
                        retirementAge = state.profile.retirementAge,
                        baselineProjection = state.baselineProjection.takeIf { state.showComparison },
                    )
                }
            }

            item {
                val saleYear = state.assets
                    .filterIsInstance<Asset.RealEstate>()
                    .firstOrNull()
                    ?.saleYear
                RelocationScenarioCard(
                    plan = state.relocationPlan,
                    saleYear = saleYear,
                    expanded = state.isRelocationExpanded,
                    onToggle = viewModel::toggleRelocationExpanded,
                    onPlanChange = viewModel::updateRelocationPlan,
                )
            }

            state.investmentAsset?.takeIf { it.currentValue > 0 }?.let { investment ->
                item {
                    InvestmentSliderCard(
                        returnRate = investment.annualReturnRate,
                        onReturnRateChange = viewModel::updateInvestmentReturnRate,
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "기본 정보",
                    summary = "${state.profile.currentAge}세 → ${state.profile.retirementAge}세 은퇴 · 월 ${state.profile.monthlyLivingExpense / 10_000}만원",
                    icon = Icons.Outlined.Payments,
                    expanded = state.isBasicInfoExpanded,
                    onToggle = viewModel::toggleBasicInfoExpanded,
                ) {
                    IntInputField(
                        label = "현재 나이",
                        value = state.profile.currentAge,
                        onValueChange = { viewModel.updateCurrentAge(it) },
                        onCommit = { viewModel.commitCurrentAge(it) },
                        supportingText = "입력 완료 시 연령대별 참고값이 갱신됩니다 (18~100세)",
                    )
                    if (state.presetSourceNote.isNotEmpty()) {
                        Text(
                            text = "📊 ${state.presetSourceNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IntInputField(
                        label = "목표 은퇴 연령",
                        value = state.profile.retirementAge,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(retirementAge = age),
                            )
                        },
                    )
                    IntInputField(
                        label = "기대 수명",
                        value = state.profile.lifeExpectancy,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(lifeExpectancy = age),
                            )
                        },
                        supportingText = state.referencePreset?.profile?.lifeExpectancy?.let {
                            PresetHints.age(it)
                        },
                    )
                    ManWonInputField(
                        label = "목표 월 생활비",
                        valueInWon = state.profile.monthlyLivingExpense,
                        onValueChange = { amount ->
                            viewModel.updateProfile(state.profile.copy(monthlyLivingExpense = amount))
                        },
                    )
                    PercentInputField(
                        label = "물가상승률 (%)",
                        value = state.assumptions.inflationRate,
                        onValueChange = { rate ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(inflationRate = rate),
                            )
                        },
                        onCommit = { rate ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(inflationRate = rate.coerceIn(0.0, 0.2)),
                            )
                        },
                        supportingText = state.referencePreset?.assumptions?.inflationRate?.let {
                            PresetHints.percent(it)
                        },
                    )
                }
            }

            itemsIndexed(state.assets) { index, asset ->
                val referenceAsset = state.referencePreset?.assets?.getOrNull(index)
                ExpandableCard(
                    title = asset.displayTitle(),
                    summary = asset.summaryText(),
                    icon = asset.icon(),
                    expanded = asset.id in state.expandedAssetIds,
                    onToggle = { viewModel.toggleAssetExpanded(asset.id) },
                ) {
                    AssetCardFields(
                        asset = asset,
                        referenceAsset = referenceAsset,
                        onAssetChange = { updated -> viewModel.updateAsset(index, updated) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentSliderCard(
    returnRate: Double,
    onReturnRateChange: (Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "투자 수익률 시뮬레이션",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = InvestmentReturnRate.formatPercent(returnRate),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            InvestmentReturnSlider(
                returnRate = returnRate,
                onReturnRateChange = onReturnRateChange,
            )
            Text(
                text = "− / + 버튼(0.5%p) 또는 슬라이더로 조절 · 차트 즉시 갱신",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

private fun Asset.icon(): ImageVector = when (this) {
    is Asset.RealEstate -> Icons.Outlined.Home
    is Asset.NationalPension -> Icons.Outlined.Payments
    is Asset.SeverancePension -> Icons.Outlined.Work
    is Asset.PersonalPension -> Icons.Outlined.AccountBalance
    is Asset.YellowUmbrella -> Icons.Outlined.Shield
    is Asset.Investment -> Icons.Outlined.TrendingUp
    is Asset.CashSavings -> Icons.Outlined.Savings
    is Asset.FixedIncome -> Icons.Outlined.Paid
    is Asset.HousingPension -> Icons.Outlined.RealEstateAgent
}
