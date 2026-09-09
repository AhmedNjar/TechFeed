package com.techfeed.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** Counts up from 0 to [targetValue] once, then just tracks further changes. */
@Composable
fun AnimatedCounterText(
    targetValue: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    suffix: String = "",
    durationMillis: Int = 700,
) {
    val animated by animateIntAsState(
        targetValue   = targetValue,
        animationSpec = tween(durationMillis),
        label         = "counter",
    )
    Text(text = "$animated$suffix", style = style, modifier = modifier)
}
