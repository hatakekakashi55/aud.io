package com.aud.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aud.android.data.CatalogService
import com.aud.android.data.Personalize
import com.aud.android.data.Catalog
import com.aud.android.data.Store
import com.aud.android.data.Track
import com.aud.android.player.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface CatalogState {
    data object Loading : CatalogState
    data class Error(val message: String) : CatalogState
    data class Ready(val catalog: Catalog) : CatalogState
}

@Composable
fun AudApp(player: PlaybackManager, store: Store) {
    val context = LocalContext.current
    var catalogState by remember { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        catalogState = CatalogState.Loading
        catalogState = try {
            val catalog = withContext(Dispatchers.IO) {
                CatalogService.loadCatalog(context, forceRefresh = reloadKey > 0)
            }
            CatalogState.Ready(catalog)
        } catch (t: Throwable) {
            CatalogState.Error(t.message ?: "Service unreachable")
        }
    }

    val playback by player.state.collectAsStateWithLifecycle()
    var likedIds by remember { mutableStateOf(store.likedIds()) }
    var fullPlayerOpen by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(NOW) }

    // Bumped whenever the listener resets the feed, so the ranking recomputes
    // even though the catalog and the current track stayed the same.
    var feedEpoch by remember { mutableIntStateOf(0) }
    var resetNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resetNotice) {
        if (resetNotice != null) {
            kotlinx.coroutines.delay(3_200)
            resetNotice = null
        }
    }

    // Recents are keyed off the current track so the shelf refreshes on every play.
    val currentId = playback.current?.id
    val recentIds = remember(currentId) { store.recentIds() }

    val onPlay: (List<Track>, Int) -> Unit = remember(player) {
        { queue, index -> player.playQueue(queue, index) }
    }
    // Wipes play/artist/language signals and the feed falls back to Tamil.
    val onResetFeed: () -> Unit = {
        store.resetTaste()
        feedEpoch += 1
        resetNotice = "Feed reset — the Tamil shelf is back"
    }
    val onToggleLike: (String) -> Unit = remember(store) {
        { trackId -> likedIds = if (store.toggleLike(trackId)) likedIds + trackId else likedIds - trackId }
    }

    Box(Modifier.fillMaxSize().background(Ink.Background)) {
        when (val state = catalogState) {
            is CatalogState.Loading -> LoadingScreen()
            is CatalogState.Error -> ErrorScreen(message = state.message, onRetry = { reloadKey++ })
            is CatalogState.Ready -> {
                val catalog = state.catalog
                val forYou = remember(catalog, currentId, feedEpoch) {
                    Personalize.forYou(catalog, store, limit = 18)
                }
                val freshShelf = remember(feedEpoch, currentId) { Personalize.isFreshShelf(store) }
                val recentTracks = remember(recentIds, catalog) {
                    recentIds.mapNotNull { id -> catalog.tracks.firstOrNull { it.id == id } }.take(10)
                }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            NOW -> NowScreen(
                                catalog = catalog,
                                forYou = forYou,
                                freshShelf = freshShelf,
                                recentTracks = recentTracks,
                                currentTrackId = currentId,
                                isPlaying = playback.isPlaying,
                                onPlay = onPlay,
                                onResetFeed = onResetFeed,
                                bottomPadding = 0.dp,
                            )
                            FIND -> SearchScreen(
                                catalog = catalog,
                                currentTrackId = currentId,
                                isPlaying = playback.isPlaying,
                                onPlay = onPlay,
                                bottomPadding = 0.dp,
                            )
                            SHELF -> CollectionScreen(
                                catalog = catalog,
                                likedIds = likedIds,
                                recentIds = recentIds,
                                currentTrackId = currentId,
                                isPlaying = playback.isPlaying,
                                onPlay = onPlay,
                                onResetFeed = onResetFeed,
                                bottomPadding = 0.dp,
                            )
                            else -> Unit
                        }
                    }
                    AnimatedVisibility(
                        visible = resetNotice != null,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 2 },
                        exit = fadeOut(tween(220)),
                    ) {
                        resetNotice?.let { notice -> ResetToast(notice) }
                    }
                    if (playback.hasTrack) {
                        MiniPlayer(
                            playback = playback,
                            liked = playback.current?.id?.let { it in likedIds } == true,
                            onToggleLike = { currentId?.let(onToggleLike) },
                            onNext = player::next,
                            onToggle = player::toggle,
                            onExpand = { fullPlayerOpen = true },
                        )
                    }
                    BottomBar(active = tab, onSelect = { tab = it })
                }
            }
        }

        /* Full-screen player — slides up over everything */
        AnimatedVisibility(
            visible = fullPlayerOpen && playback.current != null,
            enter = slideInVertically(tween(340), initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(tween(300), targetOffsetY = { it }) + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            playback.current?.let { current ->
                FullPlayerContent(
                    playback = playback,
                    liked = current.id in likedIds,
                    onToggleLike = { onToggleLike(current.id) },
                    onSeekFraction = { fraction ->
                        val d = playback.durationSec
                        if (d > 0) player.seekTo((fraction * d.toFloat()).toInt())
                    },
                    onToggle = player::toggle,
                    onNext = player::next,
                    onPrevious = player::previous,
                    onToggleShuffle = player::toggleShuffle,
                    onToggleRepeat = player::toggleRepeat,
                    onDismiss = { fullPlayerOpen = false },
                )
            }
        }
    }
}

private const val NOW = "now"
private const val FIND = "find"
private const val SHELF = "shelf"

/* ── Mini player — 2dp primary progress hairline on top ── */

@Composable
private fun MiniPlayer(
    playback: com.aud.android.player.PlaybackState,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    val track = playback.current ?: return
    val duration = playback.durationSec.coerceAtLeast(1)
    val fraction = (playback.positionSec.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = Ink.Raised,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, Ink.Line, RoundedCornerShape(14.dp)),
    ) {
        Box {
            Column {
                // Primary tape-counter line
                Box(Modifier.fillMaxWidth().height(2.dp).background(Ink.Line)) {
                    Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(Ink.Primary))
                }
                Row(
                    Modifier
                        .clickable(onClick = onExpand)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(track.imageUrl, track.title, Modifier.size(52.dp), corner = 11.dp, decodeSize = 160)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "NOW SPINNING",
                            color = Ink.Primary,
                            fontFamily = Mono,
                            fontSize = 6.5.sp,
                            letterSpacing = 1.6.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            track.title,
                            color = Ink.Text,
                            fontFamily = Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            track.creditLine + if (track.releaseYear > 0) "  ·  ${track.releaseYear}" else "",
                            color = Ink.Muted,
                            fontFamily = Mono,
                            fontSize = 7.5.sp,
                            letterSpacing = 0.7.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (liked) Ink.Primary else Ink.Muted,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onToggleLike)
                            .padding(7.dp),
                    )
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Ink.Muted,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onNext)
                            .padding(6.dp),
                    )
                    if (playback.isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp).padding(5.dp),
                            strokeWidth = 2.dp,
                            color = Ink.Primary,
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Ink.Primary,
                            modifier = Modifier.size(38.dp).clickable(onClick = onToggle),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playback.isPlaying) "Pause" else "Play",
                                    tint = Ink.OnPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ── Bottom bar — NOW · FIND · SHELF with primary tick ── */

@Composable
private fun BottomBar(active: String, onSelect: (String) -> Unit) {
    Surface(color = Ink.Paper, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(NOW, "NOW", Icons.Rounded.Radio, active, onSelect)
            BottomItem(FIND, "FIND", Icons.Rounded.Search, active, onSelect)
            BottomItem(SHELF, "SHELF", Icons.Rounded.LibraryMusic, active, onSelect)
        }
        }
    }
}

@Composable
private fun BottomItem(
    tab: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: String,
    onSelect: (String) -> Unit,
) {
    val isActive = tab == active
    val tint by animateColorAsState(if (isActive) Ink.Primary else Ink.Muted, animationSpec = tween(200), label = "tabTint")
    val barAlpha by animateFloatAsState(if (isActive) 1f else 0f, animationSpec = tween(220), label = "tabBar")
    Column(
        Modifier
            .clickable { onSelect(tab) }
            .padding(horizontal = 18.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontFamily = Mono, fontSize = 8.sp, letterSpacing = 1.9.sp)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Ink.Primary.copy(alpha = barAlpha))
        )
    }
}

/* ── Reset confirmation — a quiet, self-dismissing line ── */

@Composable
private fun ResetToast(message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Ink.Primary.copy(alpha = 0.12f))
            .border(1.dp, Ink.Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Ink.Primary))
        Spacer(Modifier.width(9.dp))
        Text(
            message.uppercase(),
            color = Ink.Primary,
            fontFamily = Mono,
            fontSize = 8.5.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

/* ── Loading — the wire is being tuned ── */

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "AUD",
                color = Ink.Text,
                fontFamily = Serif,
                fontWeight = FontWeight.Black,
                fontSize = 54.sp,
                letterSpacing = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "THE LISTENING POST",
                color = Ink.Primary,
                fontFamily = Mono,
                fontSize = 8.sp,
                letterSpacing = 3.4.sp,
            )
            Spacer(Modifier.height(22.dp))
            VuBars(active = true, modifier = Modifier.height(20.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                "TUNING THE WIRE — LIVE CATALOG",
                color = Ink.Muted,
                fontFamily = Mono,
                fontSize = 8.5.sp,
                letterSpacing = 1.6.sp,
            )
        }
    }
}

/* ── Error — honest failure, one tap to retry ── */

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Ink.Line, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "SIGNAL LOST",
                color = Ink.Signal,
                fontFamily = Mono,
                fontSize = 9.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Can't reach the wire.",
                color = Ink.Text,
                fontFamily = Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                color = Ink.Muted,
                fontFamily = Mono,
                fontSize = 9.5.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                color = Ink.Primary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable(onClick = onRetry),
            ) {
                Row(
                    Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Ink.OnPrimary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "RETRY THE WIRE",
                        color = Ink.OnPrimary,
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "EVERY TRACK STREAMS LIVE — A MISSING NETWORK MEANS SILENCE, NOT FILLER.",
                color = Ink.Faint,
                fontFamily = Mono,
                fontSize = 7.sp,
                letterSpacing = 1.1.sp,
                lineHeight = 12.sp,
            )
        }
    }
}

