package com.techfeed.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techfeed.ui.theme.TechFeedColors
import org.jetbrains.compose.resources.painterResource
import techfeed.composeapp.generated.resources.Res
import techfeed.composeapp.generated.resources.round_bookmark_24
import techfeed.composeapp.generated.resources.rounded_bookmark_24

/**
 * A bookmark toggle used identically in FeedCard, ReadModeScreen, and the
 * bookmark list screens — one shared animation so the gesture feels the same
 * everywhere in the app.
 *
 * On an actual state flip (not on first appearance — e.g. scrolling a
 * card that's already bookmarked into view shouldn't "pop") the icon
 * snaps down and springs back with a slight overshoot, plus a color
 * crossfade between the muted/accent tints.
 */
@Composable
fun AnimatedBookmarkIcon(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    activeTint: Color = TechFeedColors.Accent,
    inactiveTint: Color = TechFeedColors.TextMuted,
) {
    val bounce = remember { Animatable(1f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isBookmarked) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        bounce.snapTo(0.65f)
        bounce.animateTo(
            targetValue   = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium,
            ),
        )
    }

    val tint by animateColorAsState(
        targetValue = if (isBookmarked) activeTint else inactiveTint,
        label       = "bookmarkTint",
    )

    IconButton(onClick = onClick, modifier = modifier.size(buttonSize)) {
        Icon(
            painter = if (isBookmarked) painterResource(Res.drawable.round_bookmark_24)
                      else          painterResource(Res.drawable.rounded_bookmark_24),
            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
            tint     = tint,
            modifier = Modifier.size(iconSize).scale(bounce.value),
        )
    }
}
