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
}

/** An editorial mix — one per seed query. */
@Immutable
data class Mix(
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
)

@Immutable
data class Catalog(
    val tracks: List<Track>,
    val mixes: List<Mix>,
    val fetchedAtMs: Long,
)
