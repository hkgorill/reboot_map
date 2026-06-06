package com.rebootmap.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

private const val MAN_WON = 10_000L

private fun parseManWonInput(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L

@Composable
fun ManWonInputField(
    label: String,
    valueInWon: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val manValue = valueInWon / MAN_WON
    OutlinedTextField(
        value = if (manValue == 0L) "" else formatNumberWithComma(manValue),
        onValueChange = { text ->
            onValueChange(parseManWonInput(text) * MAN_WON)
        },
        label = { Text(label) },
        supportingText = {
            Text(supportingText ?: "단위: 만원 (예: 300 = 300만원)")
        },
        suffix = { Text("만원") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
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
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
            onValueChange(parsed)
        },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
fun PercentInputField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = if (value == 0.0) "" else (value * 100).let {
        if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)
    }
    OutlinedTextField(
        value = display,
        onValueChange = { text ->
            val parsed = text.replace(",", ".").toDoubleOrNull()?.div(100) ?: 0.0
            onValueChange(parsed)
        },
        label = { Text(label) },
        supportingText = { Text("예: 5 = 5%") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}
