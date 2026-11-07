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

/* ── Artwork — Coil with hairline border and calm gradient + serif fallback ── */

/** Built per-composition so it follows the listener's accent. */
private fun fallbackPalettes(): List<List<Color>> = listOf(
    listOf(Color(0xFF1E3A38), Ink.Brass),
    listOf(Color(0xFF26303A), Color(0xFF7FA8C4)),
    listOf(Color(0xFF2A3330), Color(0xFF8FBF9E)),
    listOf(Color(0xFF2E2A3A), Color(0xFFA99AC8)),
    listOf(Color(0xFF332E24), Color(0xFFC4B384)),
)

@Composable
fun Artwork(
    url: String?,
    seed: String,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    decodeSize: Int = 224,
    circular: Boolean = false,
) {
    val shape = if (circular) CircleShape else RoundedCornerShape(corner)
    if (url.isNullOrBlank()) {
        val palettes = fallbackPalettes()
        val p = palettes[Math.floorMod(seed.hashCode(), palettes.size)]
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
fun SectionHeader(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                kicker.uppercase(),
                color = Ink.Brass,
                fontFamily = Mono,
                fontSize = 9.5.sp,
                letterSpacing = 2.2.sp,
            )
            if (trailing != null) {
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            title,
            color = Ink.Parchment,
            fontFamily = Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 26.sp,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = Ink.Muted,
                fontFamily = Mono,
                fontSize = 9.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.4.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/* ── Track row — set like a liner-notes index, roomier for tired eyes ── */

@Composable
fun LinerRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    index: Int = -1,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index >= 0) {
            Text(
                "%02d".format(index + 1),
                color = if (isCurrent) Ink.Brass else Ink.Faint,
                fontFamily = Mono,
                fontSize = 11.sp,
                modifier = Modifier.width(26.dp),
            )
        }
        Artwork(track.imageUrl, track.title, Modifier.size(52.dp), corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isCurrent) Ink.Brass else Ink.Parchment,
                fontFamily = Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    append(track.shortArtists)
                    if (track.isTamil) append("  ·  தமிழ்") else if (track.language.isNotBlank()) append("  ·  ${track.language}")
                }.uppercase(),
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
    Hairline(Modifier.padding(start = 20.dp + (if (index >= 0) 26.dp else 0.dp) + 52.dp + 12.dp, end = 20.dp))
}

/* ── Square card — the workhorse of the home page (Spotify-style rail item) ── */

@Composable
fun SquareCard(
    track: Track,
    onClick: () -> Unit,
    size: Dp = 144.dp,
    corner: Dp = 12.dp,
    badge: String? = null,
) {
    Column(
        Modifier
            .width(size)
            .clip(RoundedCornerShape(corner))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Box {
            Artwork(track.imageUrl, track.title, Modifier.size(size), corner = corner, decodeSize = 320)
            if (!badge.isNullOrBlank()) {
                Surface(
                    color = Ink.Background.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        badge,
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 7.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            color = Ink.Parchment,
            fontFamily = Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            track.shortArtists.uppercase(),
            color = Ink.Muted,
            fontFamily = Mono,
            fontSize = 7.5.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ── Artist circle — "artists you'd recognise" row ── */

@Composable
fun ArtistCircle(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
    size: Dp = 96.dp,
) {
    Column(
        Modifier
            .width(size + 26.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(imageUrl, name, Modifier.size(size), decodeSize = 224, circular = true)
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = Ink.Parchment,
            fontFamily = Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 2,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/* ── Mix "tape" card — horizontal rail item for the shelf row ── */

private fun tapeGradients(): List<Brush> = listOf(
    Brush.verticalGradient(listOf(Color(0xFF1F3B36), Color(0xFF11201E))),
    Brush.verticalGradient(listOf(Color(0xFF27313D), Color(0xFF141A21))),
    Brush.verticalGradient(listOf(Color(0xFF2D2A3C), Color(0xFF18161F))),
    Brush.verticalGradient(listOf(Color(0xFF2C3428), Color(0xFF171C14))),
    Brush.verticalGradient(listOf(Color(0xFF322C24), Color(0xFF1C1813))),
    Brush.verticalGradient(listOf(Color(0xFF22343A), Color(0xFF111D21))),
)

@Composable
fun TapeCard(
    index: Int,
    title: String,
    trackCount: Int,
    onPlay: () -> Unit,
) {
    val gradients = tapeGradients()
    val brush = gradients[Math.floorMod(index, gradients.size)]
    Box(
        Modifier
            .width(148.dp)
            .height(188.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .border(1.dp, Ink.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(13.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "TAPE %02d".format(index + 1),
                color = Ink.Brass,
                fontFamily = Mono,
                fontSize = 8.5.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(26.dp))
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
            Surface(shape = CircleShape, color = Ink.Brass, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play $title",
                        tint = Ink.OnBrass,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
