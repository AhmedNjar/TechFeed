package com.techfeed.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techfeed.data.model.FeedTag
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedType

@Composable
fun FilterChipsRow(
    tags: List<FeedTag>    = FeedTag.entries,
    selectedTag: FeedTag,
    onTagSelected: (FeedTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        tags.forEach { tag ->
            FilterChip(
                tag        = tag,
                isSelected = tag == selectedTag,
                onClick    = { onTagSelected(tag) },
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun FilterChip(
    tag: FeedTag,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val chipShape = RoundedCornerShape(50)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TechFeedColors.Accent else TechFeedColors.Surface700,
        animationSpec = spring(stiffness = 400f),
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TechFeedColors.Surface900 else TechFeedColors.TextSecondary,
        animationSpec = spring(stiffness = 400f),
        label = "chipText",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TechFeedColors.Accent else TechFeedColors.Surface600,
        animationSpec = spring(stiffness = 400f),
        label = "chipBorder",
    )

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(bgColor)
            .border(1.dp, borderColor, chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = tag.label,
            style = TechFeedType.Caption.copy(
                color      = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}
