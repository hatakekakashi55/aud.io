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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.aud.android.data.Mix
import com.aud.android.data.Track

/**
 * Playlists — the editorial mixes as a browsable index ("tapes" from this app's
 * own vocabulary, presented the way a streaming app presents playlist covers).
 */
@Composable
fun PlaylistScreen(
    catalog: Catalog,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    bottomPadding: Dp,
) {
    val mixes = remember(catalog) { catalog.mixes }
    val tamilMixes = remember(mixes) { mixes.filter { it.tamilCount * 2 >= it.tracks.size } }
    val otherMixes = remember(mixes) { mixes.filterNot { it.tamilCount * 2 >= it.tracks.size } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 22.dp),
    ) {
        item(key = "pl-head") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    "Your shelf of tapes",
                    color = Ink.Parchment,
                    fontFamily = Sans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tamilMixes.size} Tamil tapes · ${otherMixes.size} from the rest of the map",
                    color = Ink.Muted,
                    fontFamily = Sans,
                    fontSize = 12.sp,
                )
            }
        }

        item(key = "pl-tamil-title") {
            HomeSectionTitle(title = "Tamil tapes", subtitle = "Kollywood, in every shade")
            Spacer(Modifier.height(12.dp))
        }
        itemsIndexed(tamilMixes, key = { _, m -> "pl-tamil-${m.title}" }) { index, mix ->
            MixCard(
                mix = mix,
                index = index,
                isCurrent = currentTrackId != null && mix.tracks.any { it.id == currentTrackId },
                isPlaying = isPlaying,
                onPlay = { onPlay(mix.tracks, 0) },
            )
        }

        if (otherMixes.isNotEmpty()) {
            item(key = "pl-other-title") {
                Spacer(Modifier.height(16.dp))
                HomeSectionTitle(title = "Beyond Tamil", subtitle = "India and the world")
                Spacer(Modifier.height(12.dp))
            }
            itemsIndexed(otherMixes, key = { _, m -> "pl-other-${m.title}" }) { index, mix ->
                MixCard(
                    mix = mix,
                    index = index + tamilMixes.size,
                    isCurrent = currentTrackId != null && mix.tracks.any { it.id == currentTrackId },
                    isPlaying = isPlaying,
                    onPlay = { onPlay(mix.tracks, 0) },
                )
            }
        }

        item(key = "pl-tail") { Spacer(Modifier.height(20.dp)) }
    }
}

private val MIX_COVERS: List<List<Color>> = listOf(
    listOf(Color(0xFF2C3B45), Color(0xFF1B2429)),
    listOf(Color(0xFF3A3327), Color(0xFF241F18)),
    listOf(Color(0xFF332B3C), Color(0xFF201B26)),
    listOf(Color(0xFF2A3A33), Color(0xFF1B2621)),
    listOf(Color(0xFF3B2E2C), Color(0xFF251D1C)),
    listOf(Color(0xFF2A303B), Color(0xFF1C2028)),
)

@Composable
private fun MixCard(
    mix: Mix,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
) {
    val cover = MIX_COVERS[Math.floorMod(index, MIX_COVERS.size)]
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(cover))
                .border(1.dp, Ink.Line, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (mix.tracks.firstOrNull()?.imageUrl != null) {
                Artwork(
                    mix.tracks.first().imageUrl,
                    mix.title,
                    Modifier.size(62.dp),
                    corner = 14.dp,
                    decodeSize = 200,
                )
            } else {
                Text(
                    "%02d".format(index + 1),
                    color = Ink.Brass,
                    fontFamily = Mono,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mix.title,
                color = if (isCurrent) Ink.Brass else Ink.Parchment,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${mix.trackCount} tracks · ${mix.tamilCount} Tamil",
                color = Ink.Muted,
                fontFamily = Sans,
                fontSize = 11.5.sp,
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Ink.Raised)
                .border(1.dp, Ink.Line, CircleShape)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play ${mix.title}",
                tint = Ink.Brass,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
