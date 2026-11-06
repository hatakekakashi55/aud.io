package com.aud.android.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.player.PlaybackState

/**
 * Full-screen player — the "listening card".
 * Artwork under a brass rule, a deterministic tape-waveform scrubber,
 * and transport set in mono/serif rather than icons-on-black.
 */
@Composable
fun FullPlayerContent(
    playback: PlaybackState,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val track = playback.current ?: return
    val duration = playback.durationSec.coerceAtLeast(1)
    val progress = playback.positionSec.toFloat() / duration.toFloat()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF241B11), Ink.Background, Ink.Background)
                )
            ),
    ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                /* Top bar */
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Ink.Parchment,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "NOW SPINNING",
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 2.6.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(48.dp))
                }

                Spacer(Modifier.height(12.dp))

                /* Artwork under the brass rule */
                Hairline(Modifier.padding(bottom = 14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .shadow(26.dp, RoundedCornerShape(20.dp), clip = false, spotColor = Ink.Brass.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Ink.Brass.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                ) {
                    Artwork(track.imageUrl, track.title, Modifier.fillMaxSize(), corner = 20.dp)
                }

                /* Caption row — SIDE A · track position */
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text("SIDE A", color = Ink.Faint, fontFamily = Mono, fontSize = 8.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "%02d / %02d".format(playback.index + 1, playback.queue.size),
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 8.sp,
                        letterSpacing = 2.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))

                /* Title + artist */
                Text(
                    track.title,
                    color = Ink.Parchment,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(track.artistName.uppercase())
                        if (track.releaseYear > 0) append("  ·  ${track.releaseYear}")
                    },
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(14.dp))

                /* Tape waveform scrubber */
                WaveformBar(
                    progress = progress,
                    seed = track.id,
                    onSeekFraction = onSeekFraction,
                )
                Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    Text(formatTime(playback.positionSec), color = Ink.Muted, fontFamily = Mono, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(playback.durationSec), color = Ink.Muted, fontFamily = Mono, fontSize = 10.sp)
                }

                Spacer(Modifier.height(12.dp))

                /* Transport */
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val shuffleTint by animateColorAsState(
                        if (playback.shuffle) Ink.Brass else Ink.Faint,
                        animationSpec = tween(200),
                        label = "shuffleTint",
                    )
                    IconButton(onClick = onToggleShuffle) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = shuffleTint)
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Ink.Parchment,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Surface(
                        color = Ink.Brass,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp).clickable(onClick = onToggle),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playback.isPreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.4.dp,
                                    color = Ink.OnBrass,
                                )
                            } else {
                                Crossfade(
                                    targetState = playback.isPlaying,
                                    animationSpec = tween(160),
                                    label = "transport",
                                ) { playing ->
                                    Icon(
                                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playing) "Pause" else "Play",
                                        tint = Ink.OnBrass,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Ink.Parchment,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    val repeatTint by animateColorAsState(
                        if (playback.repeatOne) Ink.Brass else Ink.Faint,
                        animationSpec = tween(200),
                        label = "repeatTint",
                    )
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            if (playback.repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = repeatTint,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                /* Save to shelf */
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    val likeTint by animateColorAsState(
                        if (liked) Ink.Brass else Ink.Parchment,
                        animationSpec = tween(200),
                        label = "likeTint",
                    )
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Ink.Line, RoundedCornerShape(50))
                            .clickable(onClick = onToggleLike)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = likeTint,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (liked) "ON YOUR SHELF" else "SAVE TO SHELF",
                            color = likeTint,
                            fontFamily = Mono,
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "LIVE STREAM",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 7.5.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
}
