package com.rebootmap.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType

private const val MAN_WON = 10_000L

private fun parseManWonInput(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L

private fun formatIntForDisplay(value: Int): String =
    if (value == 0) "" else value.toString()

@Composable
fun ManWonInputField(
    label: String,
    valueInWon: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val manValue = valueInWon / MAN_WON
    var text by remember(valueInWon) {
        mutableStateOf(if (manValue == 0L) "" else formatNumberWithComma(manValue))
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
        supportingText = {
            Text(supportingText ?: "단위: 만원 (예: 300 = 300만원)")
        },
        suffix = { Text("만원") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
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
    onCommit: ((Int) -> Unit)? = null,
) {
    var text by remember { mutableStateOf(formatIntForDisplay(value)) }
    var isFocused by remember { mutableStateOf(false) }

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
            onValueChange(digits.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) {
                    onCommit?.invoke(text.toIntOrNull() ?: 0)
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
    onCommit: ((Double) -> Unit)? = null,
) {
    fun formatPercent(percent: Double): String =
        if (percent == 0.0) "" else {
            if (percent % 1.0 == 0.0) percent.toInt().toString() else "%.1f".format(percent)
        }

    val percentValue = value * 100
    var text by remember { mutableStateOf(formatPercent(percentValue)) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isFocused) {
            text = formatPercent(value * 100)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() || it == '.' }
            val parsed = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
            onValueChange(parsed)
        },
        label = { Text(label) },
        supportingText = { Text("예: 5 = 5%") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) {
                    val parsed = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
                    onCommit?.invoke(parsed)
                }
                isFocused = focus.isFocused
                if (!focus.isFocused) {
                    text = formatPercent(value * 100)
                }
            },
        singleLine = true,
    )
}
