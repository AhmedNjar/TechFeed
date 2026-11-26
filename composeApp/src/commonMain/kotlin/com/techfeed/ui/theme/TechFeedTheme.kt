package com.techfeed.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ───────────────────────────────────────────────────────────────────

object TechFeedColors {
    // Backgrounds
    val Surface900  = Color(0xFF0A0C10)   // deepest background
    val Surface800  = Color(0xFF10131A)   // card background
    val Surface700  = Color(0xFF181C26)   // elevated card / chip bg
    val Surface600  = Color(0xFF1E2330)   // borders, dividers

    // Brand accent — electric cyan
    val Accent       = Color(0xFF00E5FF)
    val AccentDim    = Color(0xFF00AABE)
    val AccentGlow   = Color(0x3300E5FF)  // for glow effects

    // Text
    val TextPrimary   = Color(0xFFF0F4FF)
    val TextSecondary = Color(0xFF8A94A8)
    val TextMuted     = Color(0xFF505870)

    // Source brand colors
    val RedditOrange  = Color(0xFFFF5700)
    val DevToBlue     = Color(0xFF3B49DF)
    val MediumGreen   = Color(0xFF02B875)
    val HNOrange      = Color(0xFFFF6600)

    // Status
    val Error   = Color(0xFFFF4D6D)
    val Success = Color(0xFF00E096)
}

val DarkColorScheme = darkColorScheme(
    primary          = TechFeedColors.Accent,
    onPrimary        = TechFeedColors.Surface900,
    primaryContainer = TechFeedColors.AccentGlow,
    background       = TechFeedColors.Surface900,
    surface          = TechFeedColors.Surface800,
    surfaceVariant   = TechFeedColors.Surface700,
    onSurface        = TechFeedColors.TextPrimary,
    onSurfaceVariant = TechFeedColors.TextSecondary,
    outline          = TechFeedColors.Surface600,
    error            = TechFeedColors.Error,
)

// ── Typography ────────────────────────────────────────────────────────────────

object TechFeedType {
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize   = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        color      = TechFeedColors.TextPrimary,
    )
    val Title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        color      = TechFeedColors.TextPrimary,
    )
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        color      = TechFeedColors.TextSecondary,
    )
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color      = TechFeedColors.TextMuted,
    )
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        color      = TechFeedColors.Accent,
    )
}

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun TechFeedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content,
    )
}
