package com.aud.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Accent
import com.aud.android.data.Appearance
import com.aud.android.data.Catalog
import com.aud.android.data.Personalize
import com.aud.android.data.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings — where the feed is reset and the look is tuned.
 *
 * Two blocks: **Your feed** (what AUD has learned, + the one-tap reset) and
 * **Look & feel** (accent, text size, high-contrast text) for comfortable viewing.
 */
@Composable
fun SettingsScreen(
    catalog: Catalog,
    store: Store,
    appearance: Appearance,
    libraryVersion: Int,
    onAppearanceChange: (Appearance) -> Unit,
    onFeedReset: () -> Unit,
    bottomPadding: Dp,
) {
    // Recomputed whenever a reset or a spin changes the library version.
    val spins = remember(libraryVersion) { Personalize.totalSpins(store) }
    val taste = remember(libraryVersion) { Personalize.tasteSummary(store) }
    val lastReset = remember(libraryVersion) { store.lastResetMs() }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp),
    ) {
        item(key = "heading") {
            Spacer(Modifier.height(16.dp))
            SectionHeader(
                kicker = "Settings",
                title = "Make it yours",
                subtitle = "The feed, and how the page looks at 1 a.m.",
            )
        }

        /* ── Feed block ── */
        item(key = "feed-card") {
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink.Paper)
                    .border(1.dp, Ink.Line, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Text(
                    "YOUR FEED",
                    color = Ink.Brass,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (spins == 0) "Nothing learned yet" else "Built from $spins spin${if (spins == 1) "" else "s"}",
                    color = Ink.Parchment,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    taste.ifBlank { "A Tamil-first mix — 80 % Tamil, with the rest of the world sprinkled in." },
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 9.5.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (lastReset > 0) "LAST RESET ${formatDate(lastReset)}" else "NEVER RESET",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    letterSpacing = 1.4.sp,
                )

                Spacer(Modifier.height(16.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Reset the feed",
                    color = Ink.Parchment,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Forgets every spin, artist and language signal AUD has collected, then rebuilds a fresh " +
                        "Tamil-first feed. Your liked songs and recently played stay exactly where they are.",
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                )
                Spacer(Modifier.height(14.dp))

                if (!confirmReset) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Ink.Brass.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable { confirmReset = true }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = Ink.Brass,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "RESET MY FEED",
                            color = Ink.Brass,
                            fontFamily = Mono,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.6.sp,
                        )
                    }
                } else {
                    Text(
                        "This clears your listening history for the feed. Likes and recents are kept.",
                        color = Ink.Signal,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Ink.Brass)
                                .clickable {
                                    onFeedReset()
                                    confirmReset = false
                                }
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                        ) {
                            Text(
                                "YES, RESET TO TAMIL",
                                color = Ink.OnBrass,
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp,
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Ink.Line, RoundedCornerShape(50))
                                .clickable { confirmReset = false }
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                        ) {
                            Text(
                                "KEEP IT",
                                color = Ink.Muted,
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp,
                            )
                        }
                    }
                }
            }
        }

        /* ── Look & feel ── */
        item(key = "look-header") {
            Spacer(Modifier.height(20.dp))
            Hairline(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(14.dp))
            SectionHeader(
                kicker = "Look & feel",
                title = "Comfortable to look at",
                subtitle = "Low-glare surfaces, muted accents, no pure white.",
            )
        }

        item(key = "accent-card") {
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink.Paper)
                    .border(1.dp, Ink.Line, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Text(
                    "ACCENT",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Accent.entries.forEach { accent ->
                        val selected = accent == appearance.accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onAppearanceChange(appearance.copy(accent = accent))
                            },
                        ) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accent.color)
                                    .border(1.dp, Ink.Line, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = accent.on,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                accent.label.uppercase(),
                                color = if (selected) Ink.Parchment else Ink.Faint,
                                fontFamily = Mono,
                                fontSize = 7.sp,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))

                Text(
                    "TEXT SIZE",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.92f to "S", 1f to "M", 1.12f to "L", 1.24f to "XL").forEach { (scale, label) ->
                        val selected = kotlin.math.abs(appearance.textScale - scale) < 0.02f
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Ink.Brass else androidx.compose.ui.graphics.Color.Transparent)
                                .border(1.dp, if (selected) Ink.Brass else Ink.Line, RoundedCornerShape(8.dp))
                                .clickable { onAppearanceChange(appearance.copy(textScale = scale)) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Text(
                                label,
                                color = if (selected) Ink.OnBrass else Ink.Muted,
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${appearance.textScaleLabel} · also follows your system font size.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    letterSpacing = 0.6.sp,
                )

                Spacer(Modifier.height(20.dp))
                Hairline()
                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Contrast,
                        contentDescription = null,
                        tint = Ink.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "High-contrast text",
                            color = Ink.Parchment,
                            fontFamily = Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "Brighter body copy, if the default reads too soft.",
                            color = Ink.Faint,
                            fontFamily = Mono,
                            fontSize = 8.sp,
                            lineHeight = 12.sp,
                        )
                    }
                    Switch(
                        checked = appearance.highContrast,
                        onCheckedChange = {
                            onAppearanceChange(appearance.copy(highContrast = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Ink.OnBrass,
                            checkedTrackColor = Ink.Brass,
                            uncheckedThumbColor = Ink.Muted,
                            uncheckedTrackColor = Ink.Raised,
                        ),
                    )
                }
            }
        }

        item(key = "colophon") {
            Column(
                Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "AUD ${catalog.tracks.size} TRACKS · ${catalog.shelves.size} SHELVES · LIVE",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 7.5.sp,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tamil first. Everything else after.",
                    color = Ink.Faint,
                    fontFamily = Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(ms)).uppercase(Locale.ENGLISH)
