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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Catalog
import com.aud.android.data.Track
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The front page — a Tamil-first listening room with many rails, so there is
 * always another shelf below the fold instead of the same handful of songs.
 *
 * Order is deliberate: personalized For You → fresh Tamil → Tamil artists →
 * the Tamil shelves (romance, energy, canon, composers, voices, indie, roots) →
 * the rest of India → the world → the chart.
 */
@Composable
fun NowScreen(
    catalog: Catalog,
    forYou: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isFreshProfile: Boolean,
    becauseYou: Pair<String, List<Track>>?,
    onPlay: (List<Track>, Int) -> Unit,
    onOpenArtist: (String) -> Unit,
    bottomPadding: Dp,
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

    val shelves = remember(catalog) { catalog.shelves }
    val chartShelf = remember(shelves) { shelves.firstOrNull { it.key == "charts" } }
    val bodyShelves = remember(shelves) { shelves.filterNot { it.key == "charts" } }
    val heroQueue = forYou.ifEmpty { catalog.tamilTracks.ifEmpty { catalog.tracks } }
    val tamilShare = remember(catalog) { (catalog.tamilShare * 100).toInt() }
    val topArtists = remember(catalog) {
        catalog.tamilTracks.ifEmpty { catalog.tracks }
            .groupBy { it.leadArtist }
            .mapValues { (_, rows) -> rows }
            .entries
            .sortedByDescending { it.value.size }
            .take(14)
            .map { entry -> entry.key to entry.value.first().imageUrl }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp),
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
                    "Tamil first,",
                    color = Ink.Parchment,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                )
                Text(
                    "then the whole world.",
                    color = Ink.Brass,
                    fontFamily = Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "$tamilShare% TAMIL · ${catalog.tracks.size} LIVE TRACKS · ${catalog.shelves.size} SHELVES",
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp,
                )
            }
        }

        item(key = "rule1") { Hairline(Modifier.padding(horizontal = 20.dp)) }

        /* ── Hero — the personalized queue, one tap away ── */
        item(key = "hero") {
            Box(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(178.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C3B37), Color(0xFF17211F))))
                    .border(1.dp, Ink.Brass.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                    .clickable { onPlay(heroQueue, 0) }
                    .padding(20.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        if (isFreshProfile) "SIDE A — TAMIL, TO START YOU OFF" else "SIDE A — MADE FOR YOU",
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "The Tonight Tape",
                        color = Ink.Parchment,
                        fontFamily = Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                    )
                    Text(
                        if (isFreshProfile) "${heroQueue.size} TRACKS · TAMIL-FIRST, RESET ANYTIME"
                        else "${heroQueue.size} TRACKS, TUNED TO YOUR SPINS",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
                Surface(
                    color = Ink.Brass,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { onPlay(heroQueue, 0) },
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Ink.OnBrass,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "PLAY",
                            color = Ink.OnBrass,
                            fontFamily = Mono,
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                        )
                    }
                }
            }
        }

        /* ── For You ── */
        item(key = "foryou-header") {
            SectionHeader(
                kicker = "For you",
                title = if (isFreshProfile) "Start here — Tamil first" else "Tuned by your spins",
                subtitle = if (isFreshProfile) "No history yet, so this is the Tamil-first mix. Reset it anytime in Settings."
                else "Ranked on this device from what you actually play.",
            )
            Spacer(Modifier.height(10.dp))
        }
        item(key = "foryou-rail") {
            TrackRail(tracks = forYou, onPlay = onPlay, keyPrefix = "foryou")
            Spacer(Modifier.height(20.dp))
        }

        /* ── Because you played … — only once there is real history ── */
        if (becauseYou != null) {
            item(key = "because-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                SectionHeader(
                    kicker = "Because you played",
                    title = becauseYou.first,
                    subtitle = "More from the artist you keep coming back to.",
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "because-rail") {
                TrackRail(
                    tracks = becauseYou.second,
                    onPlay = onPlay,
                    keyPrefix = "because",
                    cardSize = 128.dp,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        /* ── Fresh Tamil — horizontal square rail ── */
        val fresh = bodyShelves.firstOrNull { it.key == "tamil_fresh" }
        if (fresh != null) {
            item(key = "fresh-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                SectionHeader(kicker = fresh.kicker, title = fresh.title, subtitle = fresh.subtitle)
                Spacer(Modifier.height(10.dp))
            }
            item(key = "fresh-rail") {
                TrackRail(tracks = fresh.tracks, onPlay = onPlay, keyPrefix = "fresh", badgeFor = { "NEW" })
                Spacer(Modifier.height(20.dp))
            }
        }

        /* ── Tamil artists ── */
        if (topArtists.isNotEmpty()) {
            item(key = "artists-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                SectionHeader(kicker = "Tamil · people", title = "The faces behind the songs")
                Spacer(Modifier.height(10.dp))
            }
            item(key = "artists-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(topArtists, key = { "artist-${it.first}" }) { (name, image) ->
                        ArtistCircle(name = name, imageUrl = image, onClick = { onOpenArtist(name) })
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        /* ── Every other shelf, Tamil first, then India, then the world ── */
        bodyShelves.filterNot { it.key == "tamil_fresh" }.forEach { shelf ->
            item(key = "shelf-header-${shelf.key}") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                SectionHeader(kicker = shelf.kicker, title = shelf.title, subtitle = shelf.subtitle)
                Spacer(Modifier.height(10.dp))
            }
            item(key = "shelf-rail-${shelf.key}") {
                TrackRail(
                    tracks = shelf.tracks,
                    onPlay = onPlay,
                    keyPrefix = "shelf-${shelf.key}",
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        /* ── The chart ── */
        if (chartShelf != null && chartShelf.tracks.isNotEmpty()) {
            item(key = "chart-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(14.dp))
                SectionHeader(
                    kicker = chartShelf.kicker,
                    title = chartShelf.title,
                    subtitle = chartShelf.subtitle,
                )
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(chartShelf.tracks, key = { _, t -> "chart-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(chartShelf.tracks, index) },
                )
            }
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

/** One horizontal rail of square cards — the home page's building block. */
@Composable
fun TrackRail(
    tracks: List<Track>,
    onPlay: (List<Track>, Int) -> Unit,
    keyPrefix: String,
    cardSize: Dp = 144.dp,
    badgeFor: (Track) -> String? = { null },
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(tracks, key = { _, t -> "$keyPrefix-${t.id}" }) { index, track ->
            SquareCard(
                track = track,
                size = cardSize,
                badge = badgeFor(track),
                onClick = { onPlay(tracks, index) },
            )
        }
    }
}

@Composable
fun LivePill() {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, Ink.Signal.copy(alpha = 0.5f), RoundedCornerShape(50))
            .background(Ink.Signal.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Ink.Signal))
        Spacer(Modifier.width(6.dp))
        Text("LIVE", color = Ink.Signal, fontFamily = Mono, fontSize = 8.sp, letterSpacing = 2.sp)
    }
}
