package com.techfeed.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width breakpoints following Material Design guidelines:
 * - Compact (< 600dp)
 * - Medium (600–840dp)
 * - Expanded (> 840dp)
 */
enum class WindowWidthClass { Compact, Medium, Expanded }

data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val widthDp: Dp,
) {
    val isCompact  get() = widthClass == WindowWidthClass.Compact
    val isMedium   get() = widthClass == WindowWidthClass.Medium
    val isExpanded get() = widthClass == WindowWidthClass.Expanded

    /** Returns true for Medium or Expanded layouts. */
    val isWide get() = widthClass != WindowWidthClass.Compact
}

private val CompactMaxWidth = 600.dp
private val MediumMaxWidth  = 840.dp

fun windowSizeInfoOf(widthDp: Dp): WindowSizeInfo {
    val widthClass = when {
        widthDp < CompactMaxWidth -> WindowWidthClass.Compact
        widthDp < MediumMaxWidth  -> WindowWidthClass.Medium
        else                      -> WindowWidthClass.Expanded
    }
    return WindowSizeInfo(widthClass, widthDp)
}

val CompactWindowSizeInfo = WindowSizeInfo(WindowWidthClass.Compact, 360.dp)
