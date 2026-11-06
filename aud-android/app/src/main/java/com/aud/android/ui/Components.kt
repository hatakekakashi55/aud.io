package com.aud.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aud.android.data.Track

val Serif = FontFamily.Serif
val Mono = FontFamily.Monospace
val Sans = FontFamily.SansSerif

fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

/* ── VU bars — the "on air" indicator on the current row ── */

@Composable
fun VuBars(active: Boolean, modifier: Modifier = Modifier, color: Color = Ink.Brass) {
    val transition = rememberInfiniteTransition(label = "vu")
    val spec = infiniteRepeatable<Float>(tween(540), RepeatMode.Reverse)
    val b1 by transition.animateFloat(0.25f, 1f, spec, label = "v1")
    val b2 by transition.animateFloat(1f, 0.35f, spec, label = "v2")
    val b3 by transition.animateFloat(0.45f, 0.9f, spec, label = "v3")
    val heights = if (active) listOf(b1, b2, b3) else listOf(0.28f, 0.28f, 0.28f)
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(14.dp * f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/* ── Artwork — Coil with hairline border and warm gradient + serif fallback ── */

private val FALLBACK_PALETTES = listOf(
    listOf(Color(0xFF4A2C17), Ink.Brass),
    listOf(Color(0xFF33261F), Color(0xFFB4653A)),
    listOf(Color(0xFF262B2B), Color(0xFF5F7A72)),
    listOf(Color(0xFF2E2433), Color(0xFF8A6A9B)),
    listOf(Color(0xFF33301E), Color(0xFFA8A35C)),
)

@Composable
fun Artwork(
    url: String?,
    seed: String,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    decodeSize: Int = 224,
) {
    val shape = RoundedCornerShape(corner)
    if (url.isNullOrBlank()) {
        val p = FALLBACK_PALETTES[Math.floorMod(seed.hashCode(), FALLBACK_PALETTES.size)]
        Box(
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(p))
                .border(1.dp, Ink.Line, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                seed.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                color = Ink.Parchment.copy(alpha = 0.9f),
                fontFamily = Serif,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
            )
        }
        return
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(decodeSize)
            .crossfade(120)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .border(1.dp, Ink.Line, shape),
    )
}

/* ── Hairline rule ── */

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(Ink.Line))
}

/* ── Section header — mono kicker + serif title, the editorial signature ── */

@Composable
fun SectionHeader(kicker: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
        Text(
            kicker.uppercase(),
            color = Ink.Brass,
            fontFamily = Mono,
            fontSize = 9.5.sp,
            letterSpacing = 2.2.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            title,
            color = Ink.Parchment,
            fontFamily = Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
        )
        Spacer(Modifier.height(6.dp))
    }
}

/* ── Track row — set like a liner-notes index ── */

@Composable
fun LinerRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(index + 1),
            color = if (isCurrent) Ink.Brass else Ink.Faint,
            fontFamily = Mono,
            fontSize = 11.sp,
            modifier = Modifier.width(26.dp),
        )
        Artwork(track.imageUrl, track.title, Modifier.size(50.dp), corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isCurrent) Ink.Brass else Ink.Parchment,
                fontFamily = Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                track.shortArtists.uppercase(),
                color = Ink.Muted,
                fontFamily = Mono,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isCurrent) {
            VuBars(active = isPlaying)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            formatTime(track.durationSec),
            color = Ink.Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
        )
    }
    Hairline(Modifier.padding(start = 56.dp, end = 20.dp))
}

/* ── Mix "tape" card — horizontal rail item ── */

private val TAPE_GRADIENTS = listOf(
    Brush.verticalGradient(listOf(Color(0xFF4A2C17), Color(0xFF20150B))),
    Brush.verticalGradient(listOf(Color(0xFF2E2433), Color(0xFF1A1322))),
    Brush.verticalGradient(listOf(Color(0xFF24331F), Color(0xFF151F0E))),
    Brush.verticalGradient(listOf(Color(0xFF33261F), Color(0xFF211712))),
    Brush.verticalGradient(listOf(Color(0xFF1F2E33), Color(0xFF131E24))),
    Brush.verticalGradient(listOf(Color(0xFF33331B), Color(0xFF222211))),
)

@Composable
fun TapeCard(
    index: Int,
    title: String,
    trackCount: Int,
    onPlay: () -> Unit,
) {
    val brush = TAPE_GRADIENTS[Math.floorMod(index, TAPE_GRADIENTS.size)]
    Box(
        Modifier
            .width(138.dp)
            .height(196.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .border(1.dp, Ink.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(12.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "TAPE %02d".format(index + 1),
                color = Ink.Brass,
                fontFamily = Mono,
                fontSize = 8.5.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(30.dp))
            Text(
                title,
                color = Ink.Parchment,
                fontFamily = Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                "$trackCount TRACKS",
                color = Ink.Muted,
                fontFamily = Mono,
                fontSize = 8.sp,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Ink.Brass,
                    modifier = Modifier.size(26.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play $title",
                            tint = Ink.OnBrass,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
