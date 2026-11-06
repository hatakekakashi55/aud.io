package com.aud.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** The front page — a relaxed, Tamil-first listening room typeset like a music paper. */
@Composable
fun NowScreen(
    catalog: Catalog,
    forYou: List<Track>,
    freshShelf: Boolean,
    recentTracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    onResetFeed: () -> Unit,
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
    val trending = remember(catalog) { catalog.trending }
    val tamilChart = remember(catalog) { catalog.trendingTamil.take(12) }
    val chart = remember(trending) { trending.take(30) }
    val heroQueue = forYou.ifEmpty { catalog.tracks }
    val artists = remember(catalog) { catalog.artistShelf.take(24) }
    val tamilCount = remember(catalog) { catalog.tamilTracks.size }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 18.dp),
    ) {
        /* ── Masthead ── */
        item(key = "masthead") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$greeting — THE LISTENING POST",
                        color = Ink.Primary,
                        fontFamily = Mono,
                        fontSize = 9.5.sp,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    LivePill()
                }
                Spacer(Modifier.height(5.dp))
                Text(dateLine, color = Ink.Faint, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(18.dp))
                Text(
                    "Your Tamil room,",
                    color = Ink.Text,
                    fontFamily = Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                )
                Text(
                    "the world outside.",
                    color = Ink.Primary,
                    fontFamily = Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$tamilCount TAMIL TRACKS · ${catalog.tracks.size} TOTAL · ALL LIVE",
                    color = Ink.Muted,
                    fontFamily = Mono,
                    fontSize = 9.sp,
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
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF22392C), Color(0xFF161F1A)))
                    )
                    .border(1.dp, Ink.Primary.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
                    .clickable { onPlay(heroQueue, 0) }
                    .padding(20.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        if (freshShelf) "SIDE A — FRESH FEED · TAMIL FIRST" else "SIDE A — MADE FOR YOU",
                        color = Ink.Accent,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (freshShelf) "The Tamil Tape" else "Your Own Tape",
                        color = Ink.Text,
                        fontFamily = Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                    Text(
                        "${heroQueue.size} TRACKS, TUNED TO YOUR SPINS",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                    )
                }
                Surface(
                    color = Ink.Primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { onPlay(heroQueue, 0) },
                ) {
                    Text(
                        "PLAY FOR YOU",
                        color = Ink.OnPrimary,
                        fontFamily = Mono,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                    )
                }
            }
        }

        /* ── For You — personalized rail, with the reset control ── */
        item(key = "foryou-header") {
            SectionHeader(
                kicker = "For you",
                title = if (freshShelf) "The Tamil shelf" else "Tuned by your spins",
            )
        }
        item(key = "foryou-reset") {
            ResetFeedRow(freshShelf = freshShelf, onReset = onResetFeed)
            Spacer(Modifier.height(10.dp))
        }
        item(key = "foryou-rail") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(forYou, key = { _, t -> "foryou-${t.id}" }) { index, track ->
                    ForYouCard(track = track, onClick = { onPlay(forYou, index) })
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        /* ── Jump back in — the last few spins, one tap away ── */
        if (recentTracks.isNotEmpty()) {
            item(key = "recent-header") {
                SectionHeader(kicker = "Jump back in", title = "Your last spins")
                Spacer(Modifier.height(10.dp))
            }
            item(key = "recent-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(recentTracks, key = { _, t -> "recent-${t.id}" }) { index, track ->
                        ForYouCard(track = track, onClick = { onPlay(recentTracks, index) })
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        /* ── Tamil now — the home-language chart ── */
        if (tamilChart.isNotEmpty()) {
            item(key = "tamil-header") {
                SectionHeader(kicker = "Tamil now", title = "Trending in Tamil")
                Spacer(Modifier.height(10.dp))
            }
            item(key = "tamil-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(tamilChart, key = { _, t -> "tamil-${t.id}" }) { index, track ->
                        ForYouCard(track = track, onClick = { onPlay(tamilChart, index) })
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        /* ── Artists you'll love — grouped from the live shelf, Tamil first ── */
        if (artists.isNotEmpty()) {
            item(key = "artists-header") {
                SectionHeader(kicker = "Voices", title = "Artists you'll love")
                Spacer(Modifier.height(12.dp))
            }
            item(key = "artists-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(artists, key = { _, a -> "artist-${a.name}" }) { _, artist ->
                        ArtistTile(
                            name = artist.name,
                            imageUrl = artist.imageUrl,
                            subtitle = "${artist.trackCount} TRACKS",
                            onClick = { onPlay(artist.tracks, 0) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item(key = "mix-rule") { Hairline(Modifier.padding(horizontal = 20.dp)) }

        /* ── The mixes — horizontal tape rail ── */
        item(key = "mix-header") {
            Spacer(Modifier.height(14.dp))
            SectionHeader(kicker = "The Shelf", title = "Pick a tape, any corner")
            Spacer(Modifier.height(10.dp))
        }
        item(key = "mix-rail") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

        /* ── The chart — Tamil leads, the world follows ── */
        item(key = "chart-header") {
            Spacer(Modifier.height(14.dp))
            SectionHeader(kicker = "The Chart", title = "Trending right now")
            Spacer(Modifier.height(4.dp))
            Text(
                "TAMIL LEADS · A WORLD OF GUESTS",
                color = Ink.Faint,
                fontFamily = Mono,
                fontSize = 8.sp,
                letterSpacing = 1.6.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
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

/**
 * The reset control under the For You rail.
 *
 * Two taps, no dialog: the first tap arms it, a second within four seconds
 * wipes the taste signals and rebuilds the feed as a fresh **Tamil** shelf.
 * When the feed is already fresh, it shows a quiet status line instead.
 */
@Composable
private fun ResetFeedRow(freshShelf: Boolean, onReset: () -> Unit) {
    var armed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4_000)
            armed = false
        }
    }
    val label = when {
        freshShelf -> "FRESH FEED · TAMIL FIRST"
        armed -> "TAP AGAIN TO RESET"
        else -> "RESET FEED"
    }
    val tint by animateColorAsState(
        when {
            freshShelf -> Ink.Faint
            armed -> Ink.Signal
            else -> Ink.Primary
        },
        animationSpec = tween(200),
        label = "resetTint",
    )
    val border by animateColorAsState(
        when {
            freshShelf -> Ink.Line
            armed -> Ink.Signal.copy(alpha = 0.8f)
            else -> Ink.Primary.copy(alpha = 0.45f)
        },
        animationSpec = tween(200),
        label = "resetBorder",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (freshShelf) "Nothing played yet — this is the fresh Tamil shelf."
            else "Ranks by your spins · always Tamil-first",
            color = Ink.Faint,
            fontFamily = Mono,
            fontSize = 8.5.sp,
            letterSpacing = 0.6.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, border, RoundedCornerShape(50))
                .clickable(enabled = !freshShelf) {
                    if (armed) {
                        armed = false
                        onReset()
                    } else {
                        armed = true
                    }
                }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                color = tint,
                fontFamily = Mono,
                fontSize = 8.5.sp,
                letterSpacing = 1.3.sp,
            )
        }
    }
}

/** One artist on the home rail — circular portrait, name, shelf depth. */
@Composable
private fun ArtistTile(
    name: String,
    imageUrl: String?,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            url = imageUrl,
            seed = name,
            modifier = Modifier.size(96.dp),
            corner = 48.dp,
            decodeSize = 192,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = Ink.Text,
            fontFamily = Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            color = Ink.Faint,
            fontFamily = Mono,
            fontSize = 7.5.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
        )
    }
}

/** One personalized pick — bigger art, calm typography, language tag. */
@Composable
private fun ForYouCard(track: Track, onClick: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        Artwork(track.imageUrl, track.title, Modifier.size(132.dp), corner = 16.dp, decodeSize = 288)
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            color = Ink.Text,
            fontFamily = Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 15.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            track.creditLine,
            color = if (track.isTamil) Ink.Primary else Ink.Muted,
            fontFamily = Mono,
            fontSize = 7.5.sp,
            letterSpacing = 0.7.sp,
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
