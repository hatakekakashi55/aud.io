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

    /** Normalised language tag for row metadata: TAMIL · HINDI · ENGLISH · MUSIC. */
    val languageTag: String
        get() = Languages.canonical(language).ifBlank { "Music" }.uppercase()

    /** True for Tamil tracks — the feed's home language. */
    val isTamil: Boolean
        get() = Languages.isTamil(language)

    /** What the row prints under the title: `ARJIT SINGH  ·  TAMIL`. */
    val creditLine: String
        get() = shortArtists.uppercase() + "  ·  " + languageTag
}

/** An editorial mix — one per seed query, filled entirely from the live service. */
@Immutable
data class Mix(
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
)

/** A home-page artist tile — the catalog's artists, grouped from their tracks. */
@Immutable
data class ArtistShelfEntry(
    val name: String,
    val imageUrl: String?,
    val language: String,
    val trackCount: Int,
    val tracks: List<Track>,
)

@Immutable
data class Catalog(
    val tracks: List<Track>,
    val mixes: List<Mix>,
    val fetchedAtMs: Long,
) {
    /** Tamil tracks, in catalog order (Tamil mixes are seeded first). */
    val tamilTracks: List<Track> get() = tracks.filter { it.isTamil }

    /**
     * The front-page chart: Tamil leads, the world follows — a 3:1 blend so the
     * chart feels Tamil-first without ever collapsing into one language.
     */
    val trending: List<Track>
        get() {
            val tamil = tamilTracks
            val world = tracks.filterNot { it.isTamil }
            val out = ArrayList<Track>(tamil.size + world.size)
            var w = 0
            tamil.forEachIndexed { i, t ->
                out += t
                if ((i + 1) % 3 == 0 && w < world.size) out += world[w++]
            }
            while (w < world.size) out += world[w++]
            return out
        }

    val trendingTamil: List<Track> get() = tamilTracks

    val trendingWorld: List<Track> get() = tracks.filterNot { it.isTamil }

    /** Artists with the deepest shelf presence — Tamil names lead. */
    val artistShelf: List<ArtistShelfEntry>
        get() {
            val grouped = LinkedHashMap<String, MutableList<Track>>()
            tracks.forEach { track ->
                val name = track.artistName.split(",").firstOrNull()?.trim().orEmpty()
                if (name.isEmpty()) return@forEach
                grouped.getOrPut(name) { ArrayList() }.add(track)
            }
            return grouped.map { (name, list) ->
                ArtistShelfEntry(
                    name = name,
                    imageUrl = list.firstNotNullOfOrNull { it.imageUrl },
                    language = list.first().language,
                    trackCount = list.size,
                    tracks = list,
                )
            }.sortedWith(
                compareByDescending<ArtistShelfEntry> { it.tracks.any { t -> t.isTamil } }
                    .thenByDescending { it.trackCount }
                    .thenBy { it.name.lowercase() }
            )
        }
}
