package com.techfeed.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techfeed.domain.ReadingStats
import com.techfeed.domain.TagCount
import com.techfeed.ui.components.AnimatedCounterText
import com.techfeed.ui.theme.*

@Composable
fun StatsScreen(
    stats: ReadingStats,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechFeedColors.Surface900),
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint               = TechFeedColors.TextPrimary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Your Stats", style = TechFeedType.DisplayLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // ── Streak hero card ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TechFeedColors.Surface800)
                    .padding(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Rounded.List,
                        contentDescription = null,
                        tint               = if (stats.currentStreakDays > 0) TechFeedColors.Accent
                                              else TechFeedColors.TextMuted,
                        modifier           = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCounterText(
                                targetValue = stats.currentStreakDays,
                                style       = TechFeedType.DisplayLarge,
                            )
                            Text(
                                text  = if (stats.currentStreakDays == 1) " day" else " days",
                                style = TechFeedType.Caption,
                                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                            )
                        }
                        Text("Current streak", style = TechFeedType.Caption)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Totals row ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label       = "Articles Read",
                    value       = stats.totalRead.toInt(),
                    modifier    = Modifier.weight(1f),
                )
                StatCard(
                    label       = "Longest Streak",
                    value       = stats.longestStreakDays,
                    suffix      = "d",
                    modifier    = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Top tags ──────────────────────────────────────────────────
            if (stats.topTags.isNotEmpty()) {
                Text("Top Interests", style = TechFeedType.Title)
                Spacer(Modifier.height(12.dp))
                stats.topTags.forEach { tagCount ->
                    TagFrequencyRow(
                        tagCount = tagCount,
                        maxCount = stats.topTags.maxOf { it.count },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier, suffix: String = "") {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TechFeedColors.Surface800)
            .padding(16.dp),
    ) {
        AnimatedCounterText(
            targetValue = value,
            style       = TechFeedType.DisplayLarge.copy(fontSize = 22.sp),
            suffix      = suffix,
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = TechFeedType.Caption)
    }
}

@Composable
private fun TagFrequencyRow(tagCount: TagCount, maxCount: Int) {
    val targetPct = if (maxCount == 0) 0f else tagCount.count.toFloat() / maxCount.toFloat()

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedPct by animateFloatAsState(
        targetValue   = if (started) targetPct else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "tagBarGrowth",
    )

    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("#${tagCount.tag}", style = TechFeedType.Mono)
            Text("${tagCount.count}", style = TechFeedType.Caption)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TechFeedColors.Surface700),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(TechFeedColors.Accent),
            )
        }
    }
}
