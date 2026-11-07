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
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Catalog
import com.aud.android.data.Mood
import com.aud.android.data.TasteMix
import com.aud.android.data.Track
import com.aud.android.data.greetingFor
import java.util.Calendar

/**
 * Home — laid out like a modern streaming dashboard, painted in AUD's muted,
 * low-glare palette instead of the reference's neon.
 *
 *   masthead (greeting + bell + avatar)
 *   search pill
 *   Your Sound Universe      — six muted mood tiles (live Tamil-first queries)
 *   Continue Listening       — last spin, transport, progress, kebab
 *   Made For Your Taste      — cover-flow mix cards
 *   Trending Now             — chart rows with heart + kebab
 *   Because you played …     — artist rail (once there is history)
 *   the deeper Tamil shelves — the long tail from v1.1.0, kept below the fold
 */
@Composable
fun NowScreen(
    catalog: Catalog,
    forYou: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isFreshProfile: Boolean,
    becauseYou: Pair<String, List<Track>>?,
    progressFraction: Float,
    displayName: String,
    likedIds: Set<String>,
    onPlay: (List<Track>, Int) -> Unit,
    onPlayPause: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenMood: (Mood) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenTrack: (Track) -> Unit,
    bottomPadding: Dp,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) { greetingFor(hour) }
    val firstName = remember(displayName) {
        displayName.trim().split(" ").firstOrNull()?.ifBlank { null } ?: "there"
    }

    val moods = remember(catalog) { catalog.moodList }
    val tasteMixes = remember(catalog, becauseYou) { catalog.tasteMixes }
    val trending = remember(catalog) {
        catalog.shelves.firstOrNull { it.key == "charts" }?.tracks.orEmpty().take(8)
    }
    val current = remember(forYou, currentTrackId) {
        forYou.firstOrNull { it.id == currentTrackId } ?: forYou.firstOrNull()
    }
    val heroQueue = forYou.ifEmpty { catalog.tamilTracks.ifEmpty { catalog.tracks } }
    val artistRail = remember(catalog) {
        catalog.tamilTracks.ifEmpty { catalog.tracks }
            .groupBy { it.leadArtist }
            .entries
            .sortedByDescending { it.value.size }
            .take(14)
            .map { it.key to it.value.first().imageUrl }
    }
    val deeperShelves = remember(catalog) {
        catalog.shelves.filter { it.key != "charts" }.take(6)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 22.dp),
    ) {
        /* ── Masthead — dusk gradient, greeting, bell, avatar ── */
        item(key = "masthead") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Ink.HeroTop, Ink.HeroMid, Ink.Background)
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Ink.Brass.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = Ink.Brass,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "AUD",
                            color = Ink.Parchment,
                            fontFamily = Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            letterSpacing = 0.5.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, Ink.Line, CircleShape)
                                .clickable(onClick = onOpenSettings),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Notifications,
                                contentDescription = "What's new",
                                tint = Ink.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Ink.Raised)
                                .border(1.dp, Ink.Line, CircleShape)
                                .clickable(onClick = onOpenSettings),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                                color = Ink.Brass,
                                fontFamily = Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(26.dp))
                    Text(
                        "$greeting,",
                        color = Ink.Muted,
                        fontFamily = Sans,
                        fontSize = 17.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        firstName,
                        color = Ink.Parchment,
                        fontFamily = Sans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 31.sp,
                        letterSpacing = (-0.6).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isFreshProfile) "Let's find your next favourite song…"
                        else "Tamil first — and everything you keep coming back to.",
                        color = Ink.Muted,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        /* ── Search pill ── */
        item(key = "search") {
            Box(Modifier.padding(horizontal = 20.dp)) {
                HomeSearchPill(hint = "Search songs, albums, artists…", onClick = onOpenSearch)
            }
            Spacer(Modifier.height(22.dp))
        }

        /* ── Your Sound Universe — mood tiles ── */
        if (moods.isNotEmpty()) {
            item(key = "moods-header") {
                HomeSectionTitle(
                    title = "Your Sound Universe",
                    actionLabel = "Explore moods",
                    onAction = onOpenSearch,
                )
                Spacer(Modifier.height(12.dp))
            }
            item(key = "moods-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(moods, key = { "mood-${it.key}" }) { mood ->
                        MoodTile(
                            label = mood.label,
                            icon = moodIcon(mood.key),
                            gradient = moodGradient(mood.key),
                            onClick = { onOpenMood(mood) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        /* ── Continue Listening ── */
        if (current != null) {
            item(key = "continue-header") {
                HomeSectionTitle(
                    title = "Continue Listening",
                    actionLabel = "See all",
                    onAction = { onPlay(heroQueue, 0) },
                )
                Spacer(Modifier.height(10.dp))
            }
            item(key = "continue-card") {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    ContinueListeningCard(
                        track = current,
                        fraction = if (current.id == currentTrackId) progressFraction else 0f,
                        isPlaying = isPlaying && current.id == currentTrackId,
                        onPlayPause = {
                            if (current.id == currentTrackId) onPlayPause()
                            else onPlay(heroQueue, heroQueue.indexOf(current).coerceAtLeast(0))
                        },
                        onOpen = {
                            if (current.id == currentTrackId) onPlayPause()
                            else onPlay(heroQueue, heroQueue.indexOf(current).coerceAtLeast(0))
                        },
                        onMenu = { onOpenTrack(current) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        /* ── Made For Your Taste ── */
        if (tasteMixes.isNotEmpty()) {
            item(key = "taste-header") {
                HomeSectionTitle(
                    title = "Made For Your Taste",
                    subtitle = if (isFreshProfile) "Built from the Tamil-first catalogue"
                    else "Sharpened by what you actually play",
                    actionLabel = "See all",
                    onAction = { onPlay(tasteMixes.first().tracks, 0) },
                )
                Spacer(Modifier.height(12.dp))
            }
            item(key = "taste-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(tasteMixes, key = { _, m -> "taste-${m.key}" }) { _, mix: TasteMix ->
                        TasteMixCard(
                            title = mix.title,
                            subtitle = mix.subtitle,
                            track = mix.tracks.firstOrNull(),
                            accentIndex = mix.accentIndex,
                            onPlay = { onPlay(mix.tracks, 0) },
                            onClick = { onPlay(mix.tracks, 0) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        /* ── Trending Now ── */
        if (trending.isNotEmpty()) {
            item(key = "trending-header") {
                HomeSectionTitle(
                    title = "Trending Now",
                    subtitle = "Hot tracks this week",
                    actionLabel = "See all",
                    onAction = { onPlay(trending, 0) },
                )
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(trending, key = { _, t -> "trend-${t.id}" }) { index, track ->
                Box(Modifier.padding(horizontal = 12.dp)) {
                    TrendingRow(
                        track = track,
                        liked = track.id in likedIds,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        onClick = { onPlay(trending, index) },
                        onToggleLike = { onToggleLike(track.id) },
                        onMenu = { onOpenTrack(track) },
                    )
                }
            }
            item(key = "trending-tail") { Spacer(Modifier.height(18.dp)) }
        }

        /* ── Because you played … ── */
        if (becauseYou != null) {
            item(key = "because-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                HomeSectionTitle(
                    title = "Because you played ${becauseYou.first}",
                    subtitle = "More from the artist you keep returning to",
                )
                Spacer(Modifier.height(12.dp))
            }
            item(key = "because-rail") {
                TrackRail(
                    tracks = becauseYou.second,
                    onPlay = onPlay,
                    keyPrefix = "because",
                    cardSize = 124.dp,
                )
                Spacer(Modifier.height(22.dp))
            }
        }

        /* ── Tamil artists ── */
        if (artistRail.isNotEmpty()) {
            item(key = "artists-header") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                HomeSectionTitle(title = "The voices behind it", actionLabel = "All artists")
                Spacer(Modifier.height(12.dp))
            }
            item(key = "artists-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(artistRail, key = { "artist-${it.first}" }) { (name, image) ->
                        ArtistCircle(name = name, imageUrl = image, onClick = { onOpenArtist(name) })
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        /* ── The long tail — v1.1.0's editorial shelves, still below the fold ── */
        deeperShelves.forEach { shelf ->
            item(key = "shelf-header-${shelf.key}") {
                Hairline(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                HomeSectionTitle(
                    title = shelf.title,
                    subtitle = shelf.subtitle,
                    actionLabel = "See all",
                    onAction = { onPlay(shelf.tracks, 0) },
                )
                Spacer(Modifier.height(12.dp))
            }
            item(key = "shelf-rail-${shelf.key}") {
                TrackRail(
                    tracks = shelf.tracks,
                    onPlay = onPlay,
                    keyPrefix = "shelf-${shelf.key}",
                    cardSize = 132.dp,
                )
                Spacer(Modifier.height(22.dp))
            }
        }

        /* ── Colophon ── */
        item(key = "colophon") {
            Column(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Hairline(Modifier.padding(horizontal = 60.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    "AUD · TAMIL FIRST · ${catalog.tracks.size} LIVE TRACKS",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 7.5.sp,
                    letterSpacing = 1.6.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Ink.Line, RoundedCornerShape(50))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        SettingsIcon,
                        contentDescription = null,
                        tint = Ink.Muted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "TUNE YOUR FEED",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 8.sp,
                        letterSpacing = 1.4.sp,
                    )
                }
            }
        }
    }
}

/* ── Mood tile icon + muted gradient per key ── */

private fun moodIcon(key: String): ImageVector = when (key) {
    "chill" -> Icons.Rounded.Headphones
    "romance" -> Icons.Rounded.FavoriteBorder
    "heartbreak" -> Icons.Rounded.Cloud
    "workout" -> Icons.Rounded.FitnessCenter
    "focus" -> Icons.Rounded.CenterFocusStrong
    "travel" -> Icons.Rounded.AirplanemodeActive
    else -> Icons.Rounded.SelfImprovement
}

private fun moodGradient(key: String): List<Color> = when (key) {
    "chill" -> Ink.TileChill
    "romance" -> Ink.TileRomance
    "heartbreak" -> Ink.TileHeartbreak
    "workout" -> Ink.TileWorkout
    "focus" -> Ink.TileFocus
    "travel" -> Ink.TileTravel
    else -> Ink.TileChill
}

/* ── Horizontal rail of square artwork cards (shared with the shelves) ── */

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
