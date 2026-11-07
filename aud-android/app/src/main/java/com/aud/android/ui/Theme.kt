package com.aud.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import com.aud.android.data.Accent
import com.aud.android.data.Appearance

/**
 * AUD — The Listening Post, re-tuned for the eyes.
 *
 * The old palette was warm brass on warm charcoal. It looked great in a mockup
 * and hurt after twenty minutes: a yellow-orange primary next to a red-orange
 * "live" dot is the hardest pair for the most common colour-vision deficiencies,
 * and chalk-white body text on a warm page is the classic text-glare headache.
 *
 * The new default is a **cool, deep slate page with a calm sea-glass teal
 * primary**:
 *  - teal reads cleanly on dark surfaces without vibrating, and stays distinct
 *    for protan/deutan vision where brass vs terracotta did not;
 *  - nothing is pure black or pure white;
 *  - body text sits in the ~12:1 contrast band rather than ~17:1;
 *  - secondary text still clears WCAG AA on every surface in this file.
 *
 * Everything the listener can change (accent, text size, high-contrast text)
 * lives on [Ink] as observable state, so a change in Settings repaints the whole
 * app instantly without touching a single call site.
 */
object Ink {
    /* ── Surfaces — cool, deep, never #000 ── */
    val Background = Color(0xFF131719)   // deep slate
    val Paper = Color(0xFF1A1F22)        // card surface
    val Raised = Color(0xFF222A2D)       // raised surface
    val Line = Color(0xFF2C3538)         // barely-there hairline

    /* ── Accent — set from Settings; defaults to sea-glass teal ── */
    private var accentColor by mutableStateOf(Accent.SeaGlass.color)
    private var accentDeep by mutableStateOf(Accent.SeaGlass.deep)
    private var accentOn by mutableStateOf(Accent.SeaGlass.on)

    /** Primary. Named `Brass` historically — one token everywhere, now re-tintable. */
    val Brass: Color get() = accentColor
    val BrassDeep: Color get() = accentDeep
    val OnBrass: Color get() = accentOn

    /* ── Type — gently contrast-adjustable ── */
    private var contrast by mutableStateOf(false)

    val Parchment: Color
        get() = if (contrast) Color(0xFFF3F6F4) else Color(0xFFE3E7E4)
    val Muted: Color
        get() = if (contrast) Color(0xFFC9D2CF) else Color(0xFFA7B2B0)
    val Faint: Color
        get() = if (contrast) Color(0xFF9EAAA7) else Color(0xFF7C8987)

    /* ── Home-page tints ──────────────────────────────────────────
     * The reference layout is built from saturated neon tiles. Each of those
     * hues has a muted, low-chroma equivalent here — same silhouette, none of
     * the glare. All pairs stay under ~18 % saturation so a tile never blooms
     * against the dark page, and the label text on them clears 4.5:1.
     */
    val TileChill = listOf(Color(0xFF2A3B44), Color(0xFF1E2B33))
    val TileRomance = listOf(Color(0xFF3E2F3A), Color(0xFF2C222B))
    val TileHeartbreak = listOf(Color(0xFF2B3444), Color(0xFF1F2735))
    val TileWorkout = listOf(Color(0xFF3E342A), Color(0xFF2C251E))
    val TileFocus = listOf(Color(0xFF2A3B36), Color(0xFF1E2B28))
    val TileTravel = listOf(Color(0xFF2F3A44), Color(0xFF222A33))

    /** Half-tint used behind the masthead — a dusk sky, not a nebula. */
    val HeroTop = Color(0xFF1D2C2C)
    val HeroMid = Color(0xFF172022)

    /* ── Support ── */
    val Signal = Color(0xFFD8A05F)       // warm amber, used sparingly (live / status)
    val Error = Color(0xFFE08A7D)        // soft clay — never alarm red

    /** Applies the listener's choices. Idempotent; safe to call on every recomposition. */
    fun apply(accent: Accent, highContrast: Boolean) {
        if (accentColor != accent.color) accentColor = accent.color
        if (accentDeep != accent.deep) accentDeep = accent.deep
        if (accentOn != accent.on) accentOn = accent.on
        if (contrast != highContrast) contrast = highContrast
    }
}

private fun colorSchemeFor(accent: Accent, highContrast: Boolean) = darkColorScheme(
    background = Ink.Background,
    surface = Ink.Paper,
    surfaceVariant = Ink.Raised,
    onBackground = if (highContrast) Color(0xFFF3F6F4) else Color(0xFFE3E7E4),
    onSurface = if (highContrast) Color(0xFFF3F6F4) else Color(0xFFE3E7E4),
    onSurfaceVariant = if (highContrast) Color(0xFFC9D2CF) else Color(0xFFA7B2B0),
    primary = accent.color,
    onPrimary = accent.on,
    primaryContainer = accent.deep,
    onPrimaryContainer = Color(0xFFF3F6F4),
    secondary = Ink.Signal,
    onSecondary = Color(0xFF241A0C),
    error = Ink.Error,
    onError = Color(0xFF28100F),
    outline = Ink.Line,
)

@Composable
fun AudTheme(appearance: Appearance, content: @Composable () -> Unit) {
    // Keep the observable palette in sync with the listener's settings. Done after
    // the frame commits (never mid-composition), so there is no backwards write:
    // the first paint uses the previous palette, then one frame later the new one.
    SideEffect { Ink.apply(appearance.accent, appearance.highContrast) }

    val density = LocalDensity.current
    val scaled = Density(density.density, density.fontScale * appearance.textScale)

    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = colorSchemeFor(appearance.accent, appearance.highContrast),
            content = content,
        )
    }
}
