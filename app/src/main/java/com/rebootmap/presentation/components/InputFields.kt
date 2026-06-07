package com.rebootmap.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val MAN_WON = 10_000L

/** 미입력(0)은 유지하고, 그 외 값만 구간으로 보정 */
fun coerceIntPreservingZero(value: Int, range: IntRange): Int =
    if (value == 0) 0 else value.coerceIn(range)

/** 미입력(0.0)은 유지하고, 그 외 값만 구간으로 보정 */
fun coercePercentPreservingZero(value: Double, range: ClosedFloatingPointRange<Double>): Double =
    if (value == 0.0) 0.0 else value.coerceIn(range)

/** IntInputField와 동일 — 타이핑 중 모델 반영 여부 */
fun isIntInputAllowed(parsed: Int, validRange: IntRange?): Boolean =
    validRange == null || parsed == 0 || parsed in validRange

/** PercentInputField와 동일 — 타이핑 중 모델 반영 여부 */
fun isPercentInputAllowed(parsed: Double, validRange: ClosedFloatingPointRange<Double>?): Boolean =
    validRange == null || parsed == 0.0 || parsed in validRange

/** 슬라이더 등 외부에서 value가 바뀌었을 때 입력 텍스트를 동기화할지 판단 */
fun shouldSyncPercentTextFromValue(text: String, value: Double, isFocused: Boolean): Boolean {
    if (!isFocused) return true
    val parsedFromText = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
    return abs(parsedFromText - value) > 1e-9
}

private fun parseManWonInput(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L

private fun formatIntForDisplay(value: Int): String =
    if (value == 0) "" else value.toString()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.bringIntoViewWhenFocused(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return bringIntoViewRequester(requester)
        .onFocusChanged { focus ->
            if (focus.isFocused) {
                scope.launch { requester.bringIntoView() }
            }
        }
}

@Composable
fun ManWonInputField(
    label: String,
    valueInWon: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String? = null,
) {
    var text by remember {
        mutableStateOf(
            if (valueInWon == 0L) "" else formatNumberWithComma(valueInWon / MAN_WON),
        )
    }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(valueInWon) {
        if (!isFocused) {
            val displayMan = valueInWon / MAN_WON
            text = if (displayMan == 0L) "" else formatNumberWithComma(displayMan)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            text = if (digits.isEmpty()) "" else formatNumberWithComma(digits.toLongOrNull() ?: 0L)
            onValueChange(parseManWonInput(digits) * MAN_WON)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = {
            Text(supportingText ?: "단위: 만원 (예: 300 = 300만원)")
        },
        suffix = { Text("만원") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewWhenFocused()
            .onFocusChanged { focus ->
                isFocused = focus.isFocused
                if (!focus.isFocused) {
                    val displayMan = valueInWon / MAN_WON
                    text = if (displayMan == 0L) "" else formatNumberWithComma(displayMan)
                }
            },
        singleLine = true,
    )
}

@Composable
fun IntInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String? = null,
    onCommit: ((Int) -> Unit)? = null,
    /** 설정 시 0(미입력) 또는 구간 내 값만 모델에 반영 — 타이핑 중간값(예: 6→65) 크래시 방지 */
    validRange: IntRange? = null,
) {
    var text by remember { mutableStateOf(formatIntForDisplay(value)) }
    var isFocused by remember { mutableStateOf(false) }

    fun isAllowed(parsed: Int): Boolean = isIntInputAllowed(parsed, validRange)

    LaunchedEffect(value) {
        if (!isFocused) {
            text = formatIntForDisplay(value)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            text = digits
            val parsed = digits.toIntOrNull() ?: 0
            if (isAllowed(parsed)) {
                onValueChange(parsed)
            }
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewWhenFocused()
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) {
                    val parsed = text.toIntOrNull() ?: 0
                    when {
                        onCommit != null -> onCommit(parsed)
                        validRange != null && parsed != 0 -> onValueChange(parsed.coerceIn(validRange))
                    }
                }
                isFocused = focus.isFocused
                if (!focus.isFocused) {
                    text = formatIntForDisplay(value)
                }
            },
        singleLine = true,
    )
}

@Composable
fun PercentInputField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String? = null,
    onCommit: ((Double) -> Unit)? = null,
    /** 소수 비율 기준 (예: 0.0..0.2 = 0~20%) */
    validRange: ClosedFloatingPointRange<Double>? = null,
) {
    fun isAllowed(parsed: Double): Boolean = isPercentInputAllowed(parsed, validRange)

    fun formatPercent(percent: Double): String =
        if (percent == 0.0) "" else {
            if (percent % 1.0 == 0.0) percent.toInt().toString() else "%.1f".format(percent)
        }

    val percentValue = value * 100
    var text by remember { mutableStateOf(formatPercent(percentValue)) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (shouldSyncPercentTextFromValue(text, value, isFocused)) {
            text = formatPercent(value * 100)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() || it == '.' || it == '-' }
            val parsed = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
            if (isAllowed(parsed)) {
                onValueChange(parsed)
            }
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = { Text(supportingText ?: "예: 5 = 5%") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewWhenFocused()
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) {
                    val parsed = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
                    when {
                        onCommit != null -> onCommit(parsed)
                        validRange != null && parsed != 0.0 -> onValueChange(parsed.coerceIn(validRange))
                    }
                }
                isFocused = focus.isFocused
                if (!focus.isFocused) {
                    text = formatPercent(value * 100)
                }
            },
        singleLine = true,
    )
}
