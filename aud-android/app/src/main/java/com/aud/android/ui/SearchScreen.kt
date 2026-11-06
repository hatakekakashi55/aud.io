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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aud.android.data.CatalogService
import com.aud.android.data.Catalog
import com.aud.android.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface WireState {
    data object Idle : WireState
    data object Loading : WireState
    data class Ready(val tracks: List<Track>) : WireState
    data object Error : WireState
}

private val QUICK = listOf("Anirudh", "A. R. Rahman", "Ilaiyaraaja", "Kuthu", "Sad", "Bollywood", "Love")

/** The archive — instant local matches, plus a live "From the Wire" search. */
@Composable
fun SearchScreen(
    catalog: Catalog,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var wire by remember { mutableStateOf<WireState>(WireState.Idle) }
    val trimmed = query.trim()

    /* Live search — debounced, always fresh from the wire. */
    LaunchedEffect(trimmed) {
        wire = WireState.Idle
        if (trimmed.length < 3) return@LaunchedEffect
        delay(400)
        wire = WireState.Loading
        wire = withContext(Dispatchers.IO) {
            runCatching { CatalogService.search(trimmed, limit = 18) }
                .fold(onSuccess = { WireState.Ready(it) }, onFailure = { WireState.Error })
        }
    }

    val local = remember(trimmed, catalog) {
        if (trimmed.isEmpty()) emptyList()
        else {
            val needle = trimmed.lowercase()
            catalog.tracks.filter {
                it.title.lowercase().contains(needle) ||
                    it.artistName.lowercase().contains(needle) ||
                    it.albumName.lowercase().contains(needle)
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomPadding + 12.dp),
    ) {
        /* Heading */
        item(key = "heading") {
            Spacer(Modifier.height(16.dp))
            SectionHeader(kicker = "The Archive", title = "What shall we play?")
        }

        /* Underline search field */
        item(key = "field") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    "SEARCH THE STACK AND THE WIRE",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth()) {
                    if (trimmed.isEmpty()) {
                        Text(
                            "Song, artist, album…",
                            color = Ink.Faint,
                            fontFamily = Serif,
                            fontSize = 20.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Ink.Parchment,
                            fontFamily = Serif,
                            fontSize = 20.sp,
                        ),
                        cursorBrush = SolidColor(Ink.Brass),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Ink.Brass)
                )
            }
        }

        if (trimmed.isEmpty()) {
            /* Quick searches */
            item(key = "quick-kicker") {
                Spacer(Modifier.height(18.dp))
                SectionHeader(kicker = "Shortcuts", title = "Quick searches")
            }
            item(key = "quick-chips") {
                FlowChips(values = QUICK, onPick = { query = it })
            }
            item(key = "quick-note") {
                Spacer(Modifier.height(22.dp))
                Text(
                    "TYPE THREE CHARACTERS — THE WIRE ANSWERS FRESH.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 8.sp,
                    letterSpacing = 1.6.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            return@LazyColumn
        }

        /* Local matches */
        item(key = "local-header") {
            Spacer(Modifier.height(16.dp))
            SectionHeader(kicker = "In your stack", title = "${local.size} match${if (local.size == 1) "" else "es"}")
        }
        if (local.isEmpty()) {
            item(key = "local-empty") {
                Text(
                    "NOTHING IN THE SHELF MATCHES “${trimmed.uppercase()}”.",
                    color = Ink.Faint,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        } else {
            itemsIndexed(local.take(24), key = { _, t -> "local-${t.id}" }) { index, track ->
                LinerRow(
                    index = index,
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onPlay(local, index) },
                )
            }
        }

        /* From the wire — live results */
        if (trimmed.length >= 3) {
            item(key = "wire-header") {
                Spacer(Modifier.height(14.dp))
                SectionHeader(kicker = "From the wire", title = "Fresh this minute")
            }
            when (val w = wire) {
                is WireState.Loading -> item(key = "wire-loading") { WireStatus("ASKING THE WIRE…") }
                is WireState.Error -> item(key = "wire-error") { WireStatus("THE WIRE DIDN'T ANSWER — STACK RESULTS ABOVE.") }
                is WireState.Ready -> {
                    if (w.tracks.isEmpty()) {
                        item(key = "wire-empty") { WireStatus("THE WIRE FOUND NOTHING FOR “${trimmed.uppercase()}”.") }
                    } else {
                        itemsIndexed(w.tracks, key = { _, t -> "wire-${t.id}" }) { index, track ->
                            LinerRow(
                                index = index,
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isPlaying = isPlaying,
                                onClick = { onPlay(w.tracks, index) },
                            )
                        }
                    }
                }
                is WireState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun WireStatus(text: String) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        VuBars(active = true, color = Ink.Signal)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink.Muted, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.2.sp)
    }
}

/** Editorial outlined chips, wrapped with a manual flow layout. */
@Composable
private fun FlowChips(values: List<String>, onPick: (String) -> Unit) {
    // Simple two-row layout — enough for the fixed shortcut list.
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        val rows = listOf(values.take(4), values.drop(4))
        rows.filter { it.isNotEmpty() }.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                row.forEach { value ->
                    Text(
                        value.uppercase(),
                        color = Ink.Parchment,
                        fontFamily = Mono,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.3.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Ink.Line, RoundedCornerShape(6.dp))
                            .clickable { onPick(value) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
