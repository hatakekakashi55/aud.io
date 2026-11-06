package com.aud.android.data

import android.content.Context
import org.json.JSONObject

/**
 * Tiny persistent store — liked track ids, recently played ids and the play
 * telemetry that tunes "For You", all in SharedPreferences.
 *
 * **Resetting the feed**: [resetTaste] throws away every personalization signal
 * (play counts, artist affinity, language affinity) and bumps a reset counter so
 * the next feed is a fresh Tamil-first spread rather than a replay of the old
 * one. Likes and recents are deliberately kept — they are the user's library,
 * not a ranking signal.
 */
class Store(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("aud_prefs", Context.MODE_PRIVATE)

    /* ── Library ─────────────────────────────────────────────────── */

    fun likedIds(): Set<String> = prefs.getStringSet(KEY_LIKED, emptySet())?.toSet() ?: emptySet()

    /** Returns the new liked state of [trackId]. */
    fun toggleLike(trackId: String): Boolean {
        val current = likedIds().toMutableSet()
        val nowLiked = !current.remove(trackId)
        if (nowLiked) current.add(trackId)
        prefs.edit().putStringSet(KEY_LIKED, current).apply()
        return nowLiked
    }

    fun recentIds(): List<String> =
        prefs.getString(KEY_RECENT, "").orEmpty()
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun pushRecent(trackId: String) {
        val next = (listOf(trackId) + recentIds().filter { it != trackId }).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, next.joinToString(",")).apply()
    }

    /* ── Play telemetry — powers the personalized "For You" feed ── */

    /** Records one spin of [track]. Call once per track start. */
    fun recordPlay(track: Track) {
        bump(KEY_PLAY_COUNTS, track.id)
        bump(KEY_ARTIST_COUNTS, artistKey(track))
        val language = Languages.canonical(track.language)
        if (language.isNotBlank()) bump(KEY_LANGUAGE_COUNTS, language.lowercase())
    }

    /** Records a manual skip (user hit next early) — a negative taste signal. */
    fun recordSkip(track: Track) {
        bump(KEY_SKIP_COUNTS, track.id)
        bump(KEY_SKIP_ARTISTS, artistKey(track))
    }

    fun playCounts(): Map<String, Int> = readMap(KEY_PLAY_COUNTS)
    fun skipCounts(): Map<String, Int> = readMap(KEY_SKIP_COUNTS)
    fun skippedArtists(): Map<String, Int> = readMap(KEY_SKIP_ARTISTS)
    fun artistAffinity(): Map<String, Int> = readMap(KEY_ARTIST_COUNTS)
    fun languageAffinity(): Map<String, Int> = readMap(KEY_LANGUAGE_COUNTS)

    /** Total spins recorded — the "has this listener started yet?" test. */
    fun totalPlays(): Int = playCounts().values.sum()

    /** How many times the feed has been reset; rotates the fresh Tamil shelf. */
    fun feedResetCount(): Int = prefs.getInt(KEY_RESET_COUNT, 0)

    /**
     * Wipes every signal that tunes the feed and returns the new reset count.
     * Likes + recents are untouched.
     */
    fun resetTaste(): Int {
        val next = feedResetCount() + 1
        prefs.edit()
            .remove(KEY_PLAY_COUNTS)
            .remove(KEY_ARTIST_COUNTS)
            .remove(KEY_LANGUAGE_COUNTS)
            .remove(KEY_SKIP_COUNTS)
            .remove(KEY_SKIP_ARTISTS)
            .putInt(KEY_RESET_COUNT, next)
            .apply()
        return next
    }

    fun artistKey(track: Track): String =
        track.artistName.lowercase().split(",").firstOrNull()?.trim()?.take(48).orEmpty()

    private fun bump(key: String, field: String) {
        if (field.isBlank()) return
        val map = readMap(key).toMutableMap()
        map[field] = (map[field] ?: 0) + 1
        // Keep the map bounded — drop entries beyond 400 by lowest count.
        val trimmed = if (map.size <= 400) map else map.entries.sortedByDescending { it.value }.take(400).associate { it.key to it.value }
        val json = JSONObject()
        trimmed.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun readMap(key: String): Map<String, Int> = runCatching {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        val json = JSONObject(raw)
        val out = LinkedHashMap<String, Int>()
        json.keys().forEach { k -> out[k] = json.optInt(k, 0) }
        out
    }.getOrDefault(emptyMap())

    companion object {
        private const val KEY_LIKED = "liked_ids"
        private const val KEY_RECENT = "recent_ids"
        private const val MAX_RECENT = 14
        private const val KEY_PLAY_COUNTS = "play_counts_v1"
        private const val KEY_ARTIST_COUNTS = "artist_counts_v1"
        private const val KEY_LANGUAGE_COUNTS = "language_counts_v1"
        private const val KEY_SKIP_COUNTS = "skip_counts_v1"
        private const val KEY_SKIP_ARTISTS = "skip_artists_v1"
        private const val KEY_RESET_COUNT = "feed_reset_count"
    }
}
