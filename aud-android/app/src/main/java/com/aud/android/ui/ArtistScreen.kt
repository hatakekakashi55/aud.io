package com.aud.android.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Catalog
import com.aud.android.data.CatalogService
import com.aud.android.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Artist page — local tracks straight away, then a live "more from" fetch so the
 * page is never just the six songs the shelf happened to hold.
 */
@Composable
fun ArtistScreen(
    artist: String,
    catalog: Catalog,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val local = remember(artist, catalog) {
        catalog.tracks.filter { it.artistName.contains(artist, ignoreCase = true) }
    }
    val isTamil = remember(local) { local.any { it.isTamil } }

    var live by remember(artist) { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember(artist) { mutableStateOf(true) }
    var failed by remember(artist) { mutableStateOf(false) }

    LaunchedEffect(artist) {
        loading = true
        failed = false
        delay(150)
        val result = withContext(Dispatchers.IO) {
            runCatching { CatalogService.moreLikeThis(artist, tamil = isTamil || local.isEmpty(), limit = 20) }
        }
        result.onSuccess { live = it }.onFailure { failed = true }
        loading = false
    }

    val combined = remember(local, live) {
        (local + live).distinctBy { it.id }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 20.dp),
    ) {
        item(key = "art-head") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E3A38), Ink.Background)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(Modifier.weight(1f))
                        if (isTamil || local.isEmpty()) Text(
                            "தமிழ்",
                            color = Ink.Brass,
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Artwork(
                        local.firstOrNull()?.imageUrl ?: live.firstOrNull()?.imageUrl,
                        artist,
                        Modifier.size(112.dp),
                        decodeSize = 320,
                        circular = true,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "ARTIST",
                        color = Ink.Brass,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 2.4.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        artist,
                        color = Ink.Parchment,
                        fontFamily = Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${combined.size} TRACKS ON THE WIRE",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    if (combined.isNotEmpty()) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Ink.Brass)
                                .clickable { onPlay(combined, 0) }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Ink.OnBrass,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "PLAY ALL",
                                color = Ink.OnBrass,
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                letterSpacing = 1.6.sp,
                            )
                        }
                    }
                }
            }
        }

        if (local.isNotEmpty()) {
            item(key = "local-header") {
                Spacer(Modifier.height(14.dp))
                SectionHeader(kicker = "On your shelf", title = "In the catalog")
                Spacer(Modifier.height(4.dp))
            }
            itemsIndexed(local, key = { _, t -> "art-local-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(local, index) },
                )
            }
        }

        item(key = "live-header") {
            Spacer(Modifier.height(18.dp))
            SectionHeader(
                kicker = "From the wire",
                title = "More from $artist",
                subtitle = if (isTamil) "Searched live with a Tamil qualifier." else "Searched live on the service.",
            )
        }

        when {
            loading -> item(key = "live-loading") {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.6.dp,
                        color = Ink.Brass,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "ASKING THE WIRE…",
                        color = Ink.Muted,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                    )
                }
            }
            failed -> item(key = "live-failed") {
                Text(
                    "THE WIRE DIDN'T ANSWER — SHOWING WHAT'S ON THE SHELF.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            else -> itemsIndexed(live, key = { _, t -> "art-live-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(live, index) },
                )
            }
        }

        item(key = "art-tail") {
            Spacer(Modifier.height(24.dp))
            Text(
                "AUD · LIVE FROM THE WIRE",
                color = Ink.Faint,
                fontFamily = Mono,
                fontSize = 7.5.sp,
                letterSpacing = 1.6.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
    }
}
