package com.rebootmap.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ThumbTouchSize = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentReturnSlider(
    returnRate: Double,
    onReturnRateChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snappedRate = InvestmentReturnRate.snap(returnRate)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = { onReturnRateChange(InvestmentReturnRate.decrement(snappedRate)) },
            enabled = InvestmentReturnRate.canDecrement(snappedRate),
        ) {
            Icon(Icons.Default.Remove, contentDescription = "수익률 낮추기")
        }

        Slider(
            value = snappedRate.toFloat(),
            onValueChange = { onReturnRateChange(InvestmentReturnRate.snap(it.toDouble())) },
            valueRange = InvestmentReturnRate.MIN.toFloat()..InvestmentReturnRate.MAX.toFloat(),
            steps = InvestmentReturnRate.STEPS,
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp),
            thumb = {
                Box(
                    modifier = Modifier.size(ThumbTouchSize),
                    contentAlignment = Alignment.Center,
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp),
                    )
                }
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.height(6.dp),
                )
            },
        )

        IconButton(
            onClick = { onReturnRateChange(InvestmentReturnRate.increment(snappedRate)) },
            enabled = InvestmentReturnRate.canIncrement(snappedRate),
        ) {
            Icon(Icons.Default.Add, contentDescription = "수익률 높이기")
        }
    }
}
