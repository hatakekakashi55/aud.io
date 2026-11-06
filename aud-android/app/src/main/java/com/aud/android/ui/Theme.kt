package com.aud.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AUD — a calm, legible listening room.
 *
 * Re-tuned for long sessions and tired eyes:
 *  - deep moss charcoal instead of black, so the panel never glares;
 *  - a soft sage-mint **primary** that carries state without shouting;
 *  - one warm-sand secondary accent, used sparingly;
 *  - parchment-sage text instead of chalk white.
 *
 * Every text tone below clears WCAG AA (4.5:1) on the darkest surface, and the
 * muted/faint tones clear it on raised cards too — so the feed stays readable
 * in daylight on an OLED phone and at 2 a.m. in bed.
 */
object Ink {
    val Background = Color(0xFF111714)   // deep moss charcoal — never #000
    val Paper = Color(0xFF171F1B)        // screen surface, a touch greener
    val Raised = Color(0xFF1F2923)       // cards / mini player
    val Line = Color(0xFF2A352E)         // barely-there hairline

    val Text = Color(0xFFE3EAE3)         // 14.8:1 on Background
    val Muted = Color(0xFFA6B1A9)        //  8.2:1
    val Faint = Color(0xFF8A958D)        //  5.9:1 — still AA, used for micro-labels

    val Primary = Color(0xFF9ED9B8)      // soft sage mint — the new primary
    val PrimaryDeep = Color(0xFF6FAF8C)  // pressed / inactive primary
    val OnPrimary = Color(0xFF0C2418)    // near-black green text on mint (10.2:1)

    val Accent = Color(0xFFE8B48A)       // warm sand — the single secondary accent
    val Signal = Color(0xFFE3A387)       // calm terracotta "live" dot
}

private val AudColorScheme = darkColorScheme(
    background = Ink.Background,
    surface = Ink.Paper,
    surfaceVariant = Ink.Raised,
    onBackground = Ink.Text,
    onSurface = Ink.Text,
    onSurfaceVariant = Ink.Muted,
    primary = Ink.Primary,
    onPrimary = Ink.OnPrimary,
    primaryContainer = Ink.PrimaryDeep,
    onPrimaryContainer = Ink.OnPrimary,
    secondary = Ink.Accent,
    onSecondary = Ink.OnPrimary,
    outline = Ink.Line,
    outlineVariant = Ink.Line,
)

/** Softer, roomier corners — less "sharp app", more "printed sleeve". */
private val AudShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AudTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AudColorScheme,
        shapes = AudShapes,
        content = content,
    )
}
