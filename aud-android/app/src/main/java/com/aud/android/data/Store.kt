package com.aud.android.data

import android.content.Context
import org.json.JSONObject

/**
 * Tiny persistent store — liked track ids, recently played ids, and the play
 * telemetry that powers the personalized "For You" feed, in SharedPreferences.
 * No database needed at this scale.
 *
 * Also owns the **feed reset** described in the UI: [resetFeed] wipes the
 * learning signal (play counts, artist + language affinity) but deliberately
 * keeps likes and recents, so a reset re-opens the Tamil-first feed without
 * destroying the listener's shelf.
 */
class Store(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("aud_prefs", Context.MODE_PRIVATE)

    /* ── Profile ── */

    /**
     * A display name for the masthead. Android has no "account" here, so this is
     * whatever the listener typed in Settings — "Listener" until they do.
     */
    fun displayName(): String =
        prefs.getString(KEY_DISPLAY_NAME, "").orEmpty().trim().ifBlank { "Listener" }

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim().take(24)).apply()
    }

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
        val language = languageKey(track)
        if (language.isNotBlank()) bump(KEY_LANGUAGE_COUNTS, language)
    }

    fun playCounts(): Map<String, Int> = readMap(KEY_PLAY_COUNTS)
    fun artistAffinity(): Map<String, Int> = readMap(KEY_ARTIST_COUNTS)
    fun languageAffinity(): Map<String, Int> = readMap(KEY_LANGUAGE_COUNTS)

    fun artistKey(track: Track): String =
        track.artistName.lowercase().split(",").firstOrNull()?.trim()?.take(48).orEmpty()

    /**
     * The language bucket a track counts towards. Rows the service left blank are
     * still attributed to Tamil when the script proves it, so affinity is never
     * silently lost on a mis-tagged Kollywood row.
     */
    fun languageKey(track: Track): String {
        val declared = track.language.trim().lowercase()
        if (declared.isNotBlank() && declared != "music" && declared != "unknown") return declared
        return if (track.isTamil) "tamil" else declared
    }

    /* ── Feed reset ─────────────────────────────────────────────── */

    /**
     * Erases everything the feed has learned — play counts, artist affinity,
     * language affinity — and drops the cached catalog so the next load is a
     * clean, live, Tamil-first fetch. Likes and recents are intentionally kept.
     *
     * @return the millisecond timestamp the reset happened at.
     */
    fun resetFeed(): Long {
        val now = System.currentTimeMillis()
        prefs.edit()
            .remove(KEY_PLAY_COUNTS)
            .remove(KEY_ARTIST_COUNTS)
            .remove(KEY_LANGUAGE_COUNTS)
            .putLong(KEY_LAST_RESET, now)
            .apply()
        return now
    }

    /** When the feed was last reset, or 0 if never. */
    fun lastResetMs(): Long = prefs.getLong(KEY_LAST_RESET, 0L)

    /** True when there is no listening history at all (fresh install or after a reset). */
    fun isFreshProfile(): Boolean = playCounts().values.sum() == 0

    private fun bump(key: String, field: String) {
        if (field.isBlank()) return
        val map = readMap(key).toMutableMap()
        map[field] = (map[field] ?: 0) + 1
        // Keep the map bounded — drop entries beyond 400 by lowest count.
        val trimmed =
            if (map.size <= 400) map
            else map.entries.sortedByDescending { it.value }.take(400).associate { it.key to it.value }
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
        private const val KEY_LAST_RESET = "feed_reset_at"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
