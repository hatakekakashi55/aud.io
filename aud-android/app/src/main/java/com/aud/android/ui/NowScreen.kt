package com.aud.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Catalog
import com.aud.android.data.Track
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** The front page — a relaxed, worldwide listening room typeset like a music paper. */
@Composable
fun NowScreen(
    catalog: Catalog,
    forYou: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val now = remember { Calendar.getInstance() }
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "GOOD MORNING"
        in 12..16 -> "GOOD AFTERNOON"
        in 17..21 -> "GOOD EVENING"
        else -> "UP LATE"
    }
    val dateLine = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(Calendar.getInstance().time)
            .uppercase(Locale.ENGLISH)
    }
    val chart = remember(catalog) { catalog.tracks.take(30) }
    val heroQueue = forYou.ifEmpty { catalog.tracks }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomPadding + 18.dp),
    ) {
        /* ── Masthead ── */
        item(key = "masthead") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$greeting — THE LISTENING POST",
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    LivePill()
                }
                Spacer(Modifier.height(4.dp))
                Text(dateLine, color = Ink.Faint, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Every song live,",
                    color = Ink.Parchment,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 37.sp,
                    lineHeight = 43.sp,
                )
                Text(
                    "straight from the wire.",
                    color = Ink.Brass,
                    fontFamily = Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 37.sp,
                    lineHeight = 43.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "WORLDWIDE CATALOG · ALWAYS LIVE · NO FILLER",
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.5.sp,
                )
            }
        }

        item(key = "rule1") { Hairline(Modifier.padding(horizontal = 20.dp)) }

        /* ── Hero — personalized queue, one tap away ── */
        item(key = "hero") {
            Box(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2B2318), Color(0xFF1B1815))))
                    .border(1.dp, Ink.Brass.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .clickable { onPlay(heroQueue, 0) }
                    .padding(20.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "SIDE A — MADE FOR YOU",
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "The Tonight Tape",
                        color = Ink.Parchment,
                        fontFamily = Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                    )
                    Text(
                        "${heroQueue.size} TRACKS, TUNED TO YOUR SPINS",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.4.sp,
                    )
                }
                Surface(
                    color = Ink.Brass,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { onPlay(heroQueue, 0) },
                ) {
                    Text(
                        "PLAY FOR YOU",
                        color = Ink.OnBrass,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        /* ── For You — personalized rail ── */
        item(key = "foryou-header") {
            SectionHeader(kicker = "For you", title = "Tuned by your spins")
            Spacer(Modifier.height(10.dp))
        }
        item(key = "foryou-rail") {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(forYou, key = { _, t -> "foryou-${t.id}" }) { index, track ->
                    ForYouCard(track = track, onClick = { onPlay(forYou, index) })
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item(key = "mix-rule") { Hairline(Modifier.padding(horizontal = 20.dp)) }

        /* ── The mixes — horizontal tape rail (worldwide shelf) ── */
        item(key = "mix-header") {
            Spacer(Modifier.height(14.dp))
            SectionHeader(kicker = "The Shelf", title = "Pick a tape, any corner")
            Spacer(Modifier.height(10.dp))
        }
        item(key = "mix-rail") {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(catalog.mixes, key = { _, mix -> mix.title }) { index, mix ->
                    TapeCard(
                        index = index,
                        title = mix.title,
                        trackCount = mix.trackCount,
                        onPlay = { onPlay(mix.tracks, 0) },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item(key = "chart-rule") { Hairline(Modifier.padding(horizontal = 20.dp)) }

        /* ── The chart — still there, but not the whole story ── */
        item(key = "chart-header") {
            Spacer(Modifier.height(14.dp))
            SectionHeader(kicker = "The Chart", title = "Trending right now")
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(chart, key = { _, t -> "chart-${t.id}" }) { index, track ->
            LinerRow(
                index = index,
                track = track,
                isCurrent = track.id == currentTrackId,
                isPlaying = isPlaying,
                onClick = { onPlay(chart, index) },
            )
        }

        /* ── Colophon ── */
        item(key = "colophon") {
            Column(Modifier.padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Hairline(Modifier.padding(horizontal = 60.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "AUD · THE LISTENING POST",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    letterSpacing = 2.2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "EVERY TRACK STREAMS LIVE — NOTHING PRE-DOWNLOADED",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 7.sp,
                    letterSpacing = 1.3.sp,
                )
            }
        }
    }
}

/** One personalized pick — bigger art, calm typography. */
@Composable
private fun ForYouCard(track: Track, onClick: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Artwork(track.imageUrl, track.title, Modifier.size(132.dp), corner = 14.dp, decodeSize = 288)
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            color = Ink.Parchment,
            fontFamily = Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
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

@Composable
fun LivePill() {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, Ink.Signal.copy(alpha = 0.55f), RoundedCornerShape(50))
            .background(Ink.Signal.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Ink.Signal))
        Spacer(Modifier.width(6.dp))
        Text("LIVE", color = Ink.Signal, fontFamily = Mono, fontSize = 8.sp, letterSpacing = 2.sp)
    }
}
