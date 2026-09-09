package com.techfeed.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pragmatic, KMP-portable substitute for Android's WindowSizeClass.
 *
 * There's no shared "window size class" API across Android + iOS in Compose
 * Multiplatform, so instead of pulling in a platform-specific dependency we
 * derive breakpoints directly from the measured width available to the root
 * composable (via BoxWithConstraints in HomeScreen). This is exactly the
 * signal that matters for layout decisions — it works identically for a
 * narrow phone, a phone in landscape, a folded/unfolded foldable, an iPad,
 * or a resizable desktop window, without needing to know the platform at all.
 *
 * Breakpoints follow Material's guidance:
 *   Compact  < 600dp   → phones portrait
 *   Medium   600–840dp → phones landscape, small tablets, folded foldables
 *   Expanded > 840dp   → tablets, unfolded foldables, desktop
 */
enum class WindowWidthClass { Compact, Medium, Expanded }

data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val widthDp: Dp,
) {
    val isCompact  get() = widthClass == WindowWidthClass.Compact
    val isMedium   get() = widthClass == WindowWidthClass.Medium
    val isExpanded get() = widthClass == WindowWidthClass.Expanded

    /** True for Medium and Expanded — i.e. "wide enough for a two-pane layout" */
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

/** Sane default used only as a fallback parameter value — always overridden
 *  in practice by the value computed from BoxWithConstraints in HomeScreen. */
val CompactWindowSizeInfo = WindowSizeInfo(WindowWidthClass.Compact, 360.dp)
