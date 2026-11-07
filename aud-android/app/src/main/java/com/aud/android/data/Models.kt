package com.aud.android.data

import androidx.compose.runtime.Immutable

/** One playable track, mapped from the live catalog feed. */
@Immutable
data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val imageUrl: String?,
    val audioUrl: String,
    val durationSec: Int,
    val releaseYear: Int,
    val language: String,
    val sourceUrl: String?,
) {
    /** First two credited artists — cleaner rows. */
    val shortArtists: String
        get() {
            val parts = artistName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return when {
                parts.size <= 2 -> parts.joinToString(", ")
                else -> parts.take(2).joinToString(", ") + " +${parts.size - 2}"
            }
        }

    /** The lead credit — used for affinity keys and "more like this" shelves. */
    val leadArtist: String
        get() = artistName.split(",").firstOrNull()?.trim().orEmpty().ifBlank { "Unknown Artist" }

    /**
     * Tamil detection. The live service reports `language: "tamil"` for Kollywood
     * catalogue, but a few rows come back blank or mis-tagged, so the title and
     * album are also checked for Tamil script / known Tamil-only markers.
     */
    val isTamil: Boolean
        get() {
            val lang = language.trim().lowercase()
            if (lang.startsWith("tamil") || lang == "ta") return true
            if (lang.isNotBlank() && lang != "music" && lang != "unknown") return false
            return containsTamilScript(title) || containsTamilScript(artistName) ||
                containsTamilScript(albumName)
        }
}

/** True when [value] holds at least one Tamil-script code point (U+0B80–U+0BFF). */
fun containsTamilScript(value: String): Boolean =
    value.any { it.code in 0x0B80..0x0BFF }

/** An editorial mix — one per seed query. */
@Immutable
data class Mix(
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
) {
    /** Share of this mix that is Tamil — drives where it sits on the home page. */
    val tamilCount: Int
        get() = tracks.count { it.isTamil }
}

/** A home-page shelf: kicker + title + the tracks behind it. */
@Immutable
data class Shelf(
    val key: String,
    val kicker: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
)

@Immutable
data class Catalog(
    val tracks: List<Track>,
    val mixes: List<Mix>,
    val shelves: List<Shelf>,
    val fetchedAtMs: Long,
    val moodList: List<Mood> = emptyList(),
    val tasteMixes: List<TasteMix> = emptyList(),
) {
    val tamilTracks: List<Track> get() = tracks.filter { it.isTamil }
    val tamilShare: Float
        get() = if (tracks.isEmpty()) 0f else tracks.count { it.isTamil }.toFloat() / tracks.size

    companion object {
        val Empty = Catalog(emptyList(), emptyList(), emptyList(), 0L)
    }
}

/* ── Home page building blocks (mirrors the SonicWave-style layout) ── */

/**
 * A mood tile in "Your Sound Universe". [query] is fired live against the
 * service, Tamil-first, so every tile is real catalogue — never decoration.
 */
@Immutable
data class Mood(
    val key: String,
    val label: String,
    val query: String,
    val tamilFirst: Boolean = true,
    val tracks: List<Track> = emptyList(),
) {
    val isEmpty: Boolean get() = tracks.isEmpty()
    val hasContent: Boolean get() = !isEmpty
}

/**
 * A "Made For Your Taste" cover-flow card (Daily Mix, Tamil Vibes, …).
 * [accentIndex] picks a muted two-tone gradient so no artwork is needed.
 */
@Immutable
data class TasteMix(
    val key: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val accentIndex: Int,
) {
    val trackCount: Int get() = tracks.size
}

/** Short "Good Evening" style greeting for the masthead. */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Still Up"
}
