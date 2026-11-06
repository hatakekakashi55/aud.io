package com.aud.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * AUD — The Listening Post, relaxed.
 *
 * Warmed and softened for long sessions and tired eyes: lower contrast
 * everywhere, no pure black, no pure white, no saturated alarm colors.
 * Brass is desaturated, the "live" dot is a calm terracotta, text is
 * parchment rather than chalk, and borders are barely-there hairlines.
 */
object Ink {
    val Background = Color(0xFF161411)   // soft charcoal — never #000
    val Paper = Color(0xFF1E1B17)        // card surface, warm
    val Raised = Color(0xFF272420)       // raised surface
    val Line = Color(0xFF302C26)         // barely-there hairline
    val Parchment = Color(0xFFDFD7C9)    // primary text — soft, not glaring
    val Muted = Color(0xFFA69C8E)        // secondary text
    val Faint = Color(0xFF776E62)        // tertiary text / hints
    val Brass = Color(0xFFD0A878)        // desaturated brass
    val BrassDeep = Color(0xFF9A7A4C)    // pressed brass
    val OnBrass = Color(0xFF2A2114)      // dark warm text on brass
    val Signal = Color(0xFFC9785B)       // calm terracotta "live"
}

private val InkColorScheme = darkColorScheme(
    background = Ink.Background,
    surface = Ink.Paper,
    surfaceVariant = Ink.Raised,
    onBackground = Ink.Parchment,
    onSurface = Ink.Parchment,
    onSurfaceVariant = Ink.Muted,
    primary = Ink.Brass,
    onPrimary = Ink.OnBrass,
    outline = Ink.Line,
)

@Composable
fun AudTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = InkColorScheme, content = content)
}
