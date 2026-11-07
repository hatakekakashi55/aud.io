package com.aud.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Mood
import com.aud.android.data.Track

/**
 * A mood page — the tiles on the home page open straight into their live rows.
 * Deliberately plain: artwork, one sentence, the tracks.
 */
@Composable
fun MoodScreen(
    mood: Mood,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val tracks = remember(mood) { mood.tracks }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp),
    ) {
        item(key = "mood-head") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Ink.HeroTop, Ink.Background)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Ink.Parchment,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onBack)
                        .padding(6.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "MOOD",
                    color = Ink.Brass,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 2.4.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    mood.label,
                    color = Ink.Parchment,
                    fontFamily = Sans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.8).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${tracks.size} tracks · Tamil first · live from the wire",
                    color = Ink.Muted,
                    fontFamily = Sans,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                if (tracks.isNotEmpty()) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Ink.Brass)
                            .clickable { onPlay(tracks, 0) }
                            .padding(start = 16.dp, end = 20.dp, top = 11.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Ink.OnBrass,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Play ${mood.label}",
                            color = Ink.OnBrass,
                            fontFamily = Sans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (tracks.isEmpty()) {
            item(key = "mood-empty") {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 28.dp)) {
                    Text(
                        "Nothing landed in this mood yet.",
                        color = Ink.Parchment,
                        fontFamily = Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The wire answered with no ${mood.label.lowercase()} rows this time. Pull a fresh catalogue from Settings, or pick another mood.",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
        } else {
            itemsIndexed(tracks, key = { _, t -> "mood-${mood.key}-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(tracks, index) },
                )
            }
        }

        item(key = "mood-tail") {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Ink.Line, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "Moods are seeded from live queries — no hand-picked filler. " +
                        "They sharpen as the For You feed learns.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    lineHeight = 13.sp,
                )
            }
        }
    }
}
