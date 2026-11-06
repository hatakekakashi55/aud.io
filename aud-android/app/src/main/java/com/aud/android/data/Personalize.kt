package com.aud.android.data

import kotlin.math.ln

/**
 * The "For You" engine — ranks the live catalog against this listener's own
 * history (play counts, artist affinity, language affinity, likes).
 *
 * Design goals:
 *  - zero network calls; pure local ranking, instant on every track change;
 *  - heavy artist/language affinity wins, but with diversity guardrails so the
 *    feed never collapses into one artist;
 *  - unplayed tracks get a freshness boost so discovery never dies;
 *  - a listener with no history gets a deterministic interleave of every mix
 *    (worldwide spread) instead of any single region or genre.
 */
object Personalize {

    private const val MAX_PICK_REPEAT = 2 // per-artist diversity cap per pass

    fun forYou(catalog: Catalog, store: Store, limit: Int = 18): List<Track> {
        if (catalog.tracks.isEmpty()) return emptyList()

        val plays = store.playCounts()
        val artistAff = store.artistAffinity()
        val langAff = store.languageAffinity()
        val totalPlays = plays.values.sum()

        // Fresh ears — no history yet: interleave mixes round-robin for a worldwide spread.
        if (totalPlays == 0) return interleaveMixes(catalog, limit)

        val liked = store.likedIds()
        val maxArtist = ((artistAff.values.maxOrNull() ?: 1).coerceAtLeast(1)).toFloat()
        val maxLang = ((langAff.values.maxOrNull() ?: 1).coerceAtLeast(1)).toFloat()

        // Stable jitter per track id — keeps the rail from feeling frozen without randomness.
        fun jitter(id: String): Float = ((kotlin.math.abs(id.hashCode()) % 1000) / 1000.0).toFloat() * 0.5f

        data class Scored(val track: Track, var score: Float, val artistKey: String)

        val scored = catalog.tracks.map { t ->
            val key = store.artistKey(t)
            val playsT = plays[t.id] ?: 0
            val s =
                (artistAff[key] ?: 0).toFloat() / maxArtist * 2.6f +                    // artist lean
                    langAff[t.language.lowercase()]?.let { it.toFloat() / maxLang * 1.3f }.orZero() +
                    (if (t.id in liked) 1.6f else 0f) +                                  // explicitly saved
                    (if (playsT == 0) 1.4f else 0f) +                                    // discovery boost
                    jitter(t.id) -                                                       // gentle shuffle
                    (ln(playsT + 1.0) * 0.85).toFloat()                            // demote overplayed
            Scored(t, s, key)
        }.sortedByDescending { it.score }

        // Greedy pick with artist-diversity guardrails.
        val picked = ArrayList<Track>(limit)
        val artistPicks = HashMap<String, Int>()
        for (candidate in scored) {
            if (picked.size >= limit) break
            val count = artistPicks[candidate.artistKey] ?: 0
            if (count >= MAX_PICK_REPEAT) continue
            picked += candidate.track
            artistPicks[candidate.artistKey] = count + 1
        }
        // Top-up if the diversity cap left us short.
        if (picked.size < limit) {
            for (candidate in scored) {
                if (picked.size >= limit) break
                if (candidate.track.id !in picked.map { it.id }) picked += candidate.track
            }
        }
        return picked
    }

    /** Round-robin across mixes — deterministic worldwide spread for new listeners. */
    private fun interleaveMixes(catalog: Catalog, limit: Int): List<Track> {
        if (catalog.mixes.isEmpty()) return catalog.tracks.take(limit)
        val iterators = catalog.mixes.map { it.tracks.iterator() }
        val out = ArrayList<Track>(limit)
        val seen = HashSet<String>()
        var progressed = true
        while (out.size < limit && progressed) {
            progressed = false
            for (iterator in iterators) {
                while (iterator.hasNext()) {
                    val track = iterator.next()
                    if (seen.add(track.id)) {
                        out += track
                        progressed = true
                        break
                    }
                }
                if (out.size >= limit) break
            }
        }
        return out
    }

    private fun Float?.orZero(): Float = this ?: 0f
}
