package com.rebootmap.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.LivingExpenseInflationBase
import com.rebootmap.domain.model.PersonalLoanDefaults
import com.rebootmap.domain.model.RealEstateDefaults
import com.rebootmap.presentation.chart.CashFlowChartCard
import com.rebootmap.presentation.components.ExitConfirmBackHandler
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.ResetInputsConfirmDialog
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.coercePercentPreservingZero
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.PercentInputField
import com.rebootmap.presentation.simulation.AssetCardFields
import com.rebootmap.presentation.simulation.MilestoneTimelineCard
import com.rebootmap.presentation.simulation.PresetHints
import com.rebootmap.presentation.simulation.PersonalLoanCardFields
import com.rebootmap.presentation.simulation.RelocationScenarioCard
import com.rebootmap.presentation.simulation.displayTitle
import com.rebootmap.presentation.simulation.summaryText
import com.rebootmap.presentation.simulation.MonthlyCashFlowSummaryCard
import com.rebootmap.presentation.simulation.ResultSummaryCard
import com.rebootmap.presentation.guide.UserGuideDialog
import com.rebootmap.presentation.report.SimulationPdfExporter
import com.rebootmap.presentation.simulation.SimulationViewModel
import com.rebootmap.presentation.simulation.displayTitle
import com.rebootmap.presentation.simulation.summaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showUserGuide by remember { mutableStateOf(false) }

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

    if (showUserGuide) {
        UserGuideDialog(onDismiss = { showUserGuide = false })
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
                            text = { Text("이용 가이드") },
                            onClick = {
                                showMenu = false
                                showUserGuide = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("PDF 리포트 공유") },
                            onClick = {
                                showMenu = false
                                SimulationPdfExporter.exportAndShare(context, state)
                            },
                        )
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
                .padding(padding)
                .imePadding(),
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
                    MonthlyCashFlowSummaryCard(
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
                MilestoneTimelineCard(
                    expenses = state.lumpSumExpenses,
                    expenseMatches = state.expenseMatches,
                    currentAge = state.profile.currentAge,
                    expanded = state.isMilestoneExpanded,
                    onToggle = viewModel::toggleMilestoneExpanded,
                    onAdd = viewModel::addLumpSumExpense,
                    onUpdate = viewModel::updateLumpSumExpense,
                    onRemove = viewModel::removeLumpSumExpense,
                )
            }

            item {
                val estates = state.assets.filterIsInstance<Asset.RealEstate>()
                RelocationScenarioCard(
                    plan = state.relocationPlan,
                    realEstates = estates,
                    expanded = state.isRelocationExpanded,
                    onToggle = viewModel::toggleRelocationExpanded,
                    onPlanChange = viewModel::updateRelocationPlan,
                    onAddBuyEstate = viewModel::addBuyEstateForRelocation,
                )
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
                        validRange = 18..100,
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
                        validRange = 18..100,
                        onValueChange = { age ->
                            viewModel.updateProfile(
                                state.profile.copy(retirementAge = age),
                            )
                        },
                    )
                    IntInputField(
                        label = "기대 수명",
                        value = state.profile.lifeExpectancy,
                        validRange = 18..100,
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
                        validRange = 0.0..0.2,
                        onValueChange = { rate ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(inflationRate = rate),
                            )
                        },
                        onCommit = { rate ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(
                                    inflationRate = coercePercentPreservingZero(rate, 0.0..0.2),
                                ),
                            )
                        },
                        supportingText = state.referencePreset?.assumptions?.inflationRate?.let {
                            PresetHints.percent(it)
                        },
                    )
                    LivingExpenseInflationBaseSelector(
                        selected = state.assumptions.livingExpenseInflationBase,
                        onSelect = { base ->
                            viewModel.updateAssumptions(
                                state.assumptions.copy(livingExpenseInflationBase = base),
                            )
                        },
                    )
                    TaxAssumptionSection(
                        assumptions = state.assumptions,
                        onUpdate = viewModel::updateAssumptions,
                    )
                }
            }

            val realEstates = state.assets.filterIsInstance<Asset.RealEstate>()
            val nonRealEstateAssets = state.assets.filter { it !is Asset.RealEstate }

            item {
                if (realEstates.size < RealEstateDefaults.MAX_COUNT) {
                    OutlinedButton(
                        onClick = viewModel::addRealEstate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("+ 부동산 추가 (최대 ${RealEstateDefaults.MAX_COUNT}건)")
                    }
                }
            }

            itemsIndexed(
                items = realEstates,
                key = { _, estate -> estate.id },
            ) { _, estate ->
                val referenceAsset = state.referencePreset?.assets?.find { it.id == estate.id }
                    ?: if (realEstates.size == 1) {
                        state.referencePreset?.assets?.filterIsInstance<Asset.RealEstate>()?.firstOrNull()
                    } else {
                        null
                    }
                val estateOrdinal = realEstates.indexOfFirst { it.id == estate.id }
                val canRemoveEstate = realEstates.size > 1 || estate.currentValue > 0 || estate.debtAmount > 0
                ExpandableCard(
                    title = estate.displayTitle(estateOrdinal, realEstates.size),
                    summary = estate.summaryText(),
                    icon = estate.icon(),
                    expanded = estate.id in state.expandedAssetIds,
                    onToggle = { viewModel.toggleAssetExpanded(estate.id) },
                ) {
                    AssetCardFields(
                        asset = estate,
                        referenceAsset = referenceAsset,
                        onAssetChange = { updated -> viewModel.updateAssetById(estate.id, updated) },
                        onRemove = if (canRemoveEstate) {
                            { viewModel.removeRealEstate(estate.id) }
                        } else {
                            null
                        },
                    )
                }
            }

            itemsIndexed(
                items = nonRealEstateAssets,
                key = { _, asset -> asset.id },
            ) { _, asset ->
                val referenceAsset = state.referencePreset?.assets?.find { it.id == asset.id }
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
                        onAssetChange = { updated -> viewModel.updateAssetById(asset.id, updated) },
                        onRemove = null,
                    )
                }
            }

            val personalLoans = state.personalLoans

            item {
                if (personalLoans.size < PersonalLoanDefaults.MAX_COUNT) {
                    OutlinedButton(
                        onClick = viewModel::addPersonalLoan,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("+ 신용·차용 부채 추가 (최대 ${PersonalLoanDefaults.MAX_COUNT}건)")
                    }
                }
            }

            itemsIndexed(
                items = personalLoans,
                key = { _, loan -> loan.id },
            ) { _, loan ->
                val loanOrdinal = personalLoans.indexOfFirst { it.id == loan.id }
                ExpandableCard(
                    title = loan.displayTitle(loanOrdinal, personalLoans.size),
                    summary = loan.summaryText(),
                    icon = Icons.Outlined.CreditCard,
                    expanded = loan.id in state.expandedLoanIds,
                    onToggle = { viewModel.toggleLoanExpanded(loan.id) },
                ) {
                    PersonalLoanCardFields(
                        loan = loan,
                        loanOrdinal = loanOrdinal,
                        loanCount = personalLoans.size,
                        onLoanChange = viewModel::updatePersonalLoan,
                        onRemove = { viewModel.removePersonalLoan(loan.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LivingExpenseInflationBaseSelector(
    selected: LivingExpenseInflationBase,
    onSelect: (LivingExpenseInflationBase) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "생활비 물가 기준",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "목표 월 생활비를 언제 기준 금액으로 볼지 선택합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected == LivingExpenseInflationBase.RETIREMENT_AGE,
                onClick = { onSelect(LivingExpenseInflationBase.RETIREMENT_AGE) },
                label = { Text("은퇴 시점") },
            )
            FilterChip(
                selected = selected == LivingExpenseInflationBase.SIMULATION_START,
                onClick = { onSelect(LivingExpenseInflationBase.SIMULATION_START) },
                label = { Text("현재부터") },
            )
        }
    }
}

@Composable
private fun TaxAssumptionSection(
    assumptions: com.rebootmap.domain.model.EconomicAssumptions,
    onUpdate: (com.rebootmap.domain.model.EconomicAssumptions) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "세금·보험 (간이 추정)",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "재산세·종부세·지역가입자 건강보험료 반영 여부",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TaxToggleRow(
            label = "재산세·종부세",
            checked = assumptions.propertyTaxEnabled && assumptions.comprehensiveRealEstateTaxEnabled,
            onCheckedChange = { enabled ->
                onUpdate(
                    assumptions.copy(
                        propertyTaxEnabled = enabled,
                        comprehensiveRealEstateTaxEnabled = enabled,
                    ),
                )
            },
        )
        TaxToggleRow(
            label = "건강보험·장기요양",
            checked = assumptions.healthInsuranceEnabled,
            onCheckedChange = { enabled ->
                onUpdate(assumptions.copy(healthInsuranceEnabled = enabled))
            },
        )
    }
}

@Composable
private fun TaxToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    is Asset.EmploymentIncome -> Icons.Outlined.Work
    is Asset.BusinessIncome -> Icons.Outlined.Store
    is Asset.OtherFixedIncome -> Icons.Outlined.Paid
    is Asset.HousingPension -> Icons.Outlined.RealEstateAgent
}
