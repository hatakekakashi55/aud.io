package com.aud.android.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.Catalog
import com.aud.android.data.Track

/** Your shelf — liked tracks and the last spins, kept on this device. */
@Composable
fun CollectionScreen(
    catalog: Catalog,
    likedIds: Set<String>,
    recentIds: List<String>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val byId = catalog.tracks.associateBy { it.id }
    val liked = catalog.tracks.filter { it.id in likedIds }
    val recents = recentIds.mapNotNull { byId[it] }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomPadding + 12.dp),
    ) {
        item(key = "heading") {
            Spacer(Modifier.height(16.dp))
            SectionHeader(kicker = "Your shelf", title = "Collection")
            Spacer(Modifier.height(4.dp))
        }

        if (liked.isEmpty() && recents.isEmpty()) {
            item(key = "empty") { EmptyShelf() }
            return@LazyColumn
        }

        /* ── Recently spun ── */
        if (recents.isNotEmpty()) {
            item(key = "recent-kicker") {
                Text(
                    "RECENTLY SPUN",
                    color = Ink.Brass,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            item(key = "recent-rail") {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recents, key = { "recent-${it.id}" }) { track ->
                        val queue = recents
                        Column(
                            Modifier
                                .width(112.dp)
                                .clickable { onPlay(queue, queue.indexOf(track)) },
                        ) {
                            Artwork(track.imageUrl, track.title, Modifier.size(112.dp), corner = 12.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                track.title,
                                color = Ink.Parchment,
                                fontFamily = Serif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 2,
                                lineHeight = 13.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            item(key = "recent-rule") { Hairline(Modifier.padding(horizontal = 20.dp)) }
        }

        /* ── Liked ── */
        item(key = "liked-kicker") {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SAVED",
                    color = Ink.Brass,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "%02d".format(liked.size),
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (liked.isEmpty()) {
            item(key = "liked-empty") {
                Text(
                    "NOTHING SAVED YET — TAP THE HEART ON ANY TRACK.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        } else {
            itemsIndexed(liked, key = { _, t -> "liked-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(liked, index) },
                )
            }
        }
    }
}

@Composable
private fun EmptyShelf() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Ink.Line, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = Ink.Brass,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Nothing shelved yet.",
            color = Ink.Parchment,
            fontFamily = Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "TAP THE HEART ON ANY TRACK — IT STAYS ON THIS DEVICE, ALWAYS.",
            color = Ink.Muted,
            fontFamily = Mono,
            fontSize = 8.5.sp,
            letterSpacing = 1.2.sp,
        )
    }
}
