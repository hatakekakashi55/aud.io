package com.aud.android.data

import kotlin.math.ceil
import kotlin.math.ln

/**
 * The "For You" engine — ranks the live catalog against this listener's own
 * history (play counts, artist affinity, language affinity, likes).
 *
 * Design goals:
 *  - **Tamil-first by default.** A listener with no history gets a Tamil shelf;
 *    a listener with history still gets a Tamil majority (≥ 2 of every 3 picks)
 *    because Tamil is the app's home language — personal taste shapes *which*
 *    Tamil songs, and the remaining third opens the rest of the world;
 *  - zero network calls; pure local ranking, instant on every track change;
 *  - heavy artist/language affinity wins, but with diversity guardrails so the
 *    feed never collapses into one artist;
 *  - unplayed tracks get a freshness boost so discovery never dies;
 *  - [resetTaste] (in [Store]) clears all signals, so the next feed is the
 *    Tamil shelf again — with a small rotation so it isn't a carbon copy.
 */
object Personalize {

    private const val MAX_PICK_REPEAT = 2   // per-artist diversity cap per pass
    private const val TAMIL_SHARE = 0.66f   // Tamil quota of the personalized feed
    private const val TAMIL_NUDGE = 0.9f    // tie-breaker lean toward Tamil
    private const val WORLD_SHELF_CAP = 4   // world tracks in a fresh Tamil shelf

    fun forYou(catalog: Catalog, store: Store, limit: Int = 18): List<Track> {
        if (catalog.tracks.isEmpty()) return emptyList()

        val resetCount = store.feedResetCount()
        val plays = store.playCounts()
        val tamil = catalog.tamilTracks.ifEmpty { catalog.tracks }

        // Fresh ears — no history (never played, or just reset): the Tamil shelf.
        if (plays.isEmpty()) return tamilShelf(catalog, tamil, limit, rotation = resetCount)

        val liked = store.likedIds()
        val skips = store.skipCounts()
        val artistAff = store.artistAffinity()
        val artistSkips = store.skippedArtists()
        val langAff = store.languageAffinity()
        val maxArtist = ((artistAff.values.maxOrNull() ?: 1).coerceAtLeast(1)).toFloat()
        val maxArtistSkips = ((artistSkips.values.maxOrNull() ?: 1).coerceAtLeast(1)).toFloat()
        val maxLang = ((langAff.values.maxOrNull() ?: 1).coerceAtLeast(1)).toFloat()

        // Salt keeps the rail from feeling frozen, yet stable within a day/session.
        val salt = resetCount * 31 + (catalog.fetchedAtMs / 86_400_000L).toInt()
        fun jitter(id: String): Float =
            ((kotlin.math.abs((id.hashCode() * 31 + salt) % 1000)) / 1000.0).toFloat() * 0.6f

        fun score(t: Track): Float {
            val key = store.artistKey(t)
            val playsT = plays[t.id] ?: 0
            return (artistAff[key] ?: 0).toFloat() / maxArtist * 2.6f +          // artist lean
                langAff[Languages.canonical(t.language).lowercase()]
                    ?.let { it.toFloat() / maxLang * 1.3f }.orZero() +
                (if (t.id in liked) 1.6f else 0f) +                              // explicitly saved
                (if (playsT == 0) 1.4f else 0f) +                                // discovery boost
                (if (t.isTamil) TAMIL_NUDGE else 0f) +                           // home language
                jitter(t.id) -                                                   // gentle shuffle
                (ln(playsT + 1.0) * 0.85).toFloat() -                            // demote overplayed
                (ln((skips[t.id] ?: 0) + 1.0) * 0.7).toFloat() -                 // demote rejected
                (artistSkips[key] ?: 0).toFloat() / maxArtistSkips * 0.9f        // artists you keep skipping
        }

        val tamilRanked = tamil.map { it to score(it) }.sortedByDescending { it.second }
        val worldRanked = catalog.tracks.filterNot { it.isTamil }
            .map { it to score(it) }.sortedByDescending { it.second }

        val picked = ArrayList<Track>(limit)
        val artistPicks = HashMap<String, Int>()
        val seen = HashSet<String>()

        fun take(candidate: Track): Boolean {
            if (!seen.add(candidate.id)) return false
            val key = store.artistKey(candidate)
            val count = artistPicks[key] ?: 0
            if (count >= MAX_PICK_REPEAT) return false
            artistPicks[key] = count + 1
            picked += candidate
            return true
        }

        // Pass 1 — the Tamil quota.
        val quota = minOf(ceil(limit * TAMIL_SHARE).toInt(), tamilRanked.size)
        for ((track, _) in tamilRanked) {
            if (picked.size >= quota) break
            take(track)
        }

        // Pass 2 — everything else by score, Tamil first on ties.
        for ((track, _) in (tamilRanked + worldRanked)) {
            if (picked.size >= limit) break
            take(track)
        }

        // Top-up if the diversity cap left us short.
        if (picked.size < limit) {
            for (track in catalog.tracks) {
                if (picked.size >= limit) break
                if (track.id !in seen) picked += track
            }
        }
        return picked
    }

    /**
     * The reset / brand-new shelf: Tamil mixes interleaved round-robin, then a
     * short world tail so the page still breathes. Deterministic per rotation.
     */
    private fun tamilShelf(catalog: Catalog, tamil: List<Track>, limit: Int, rotation: Int): List<Track> {
        val out = ArrayList<Track>(limit)
        val seen = HashSet<String>()
        val tamilTarget = (limit - WORLD_SHELF_CAP).coerceAtLeast(1)

        val tamilMixes = catalog.mixes.filter { mix -> mix.tracks.any { it.isTamil } }
        if (tamilMixes.isNotEmpty()) {
            val starts = tamilMixes.indices.map { (it + rotation) % tamilMixes.size }
            val cursors = HashMap<Int, Int>()
            var progressed = true
            while (out.size < tamilTarget && progressed) {
                progressed = false
                for (index in starts) {
                    if (out.size >= tamilTarget) break
                    val tracks = tamilMixes[index].tracks
                    var cursor = cursors[index] ?: 0
                    while (cursor < tracks.size) {
                        val track = tracks[cursor++]
                        if (track.isTamil && seen.add(track.id)) {
                            out += track
                            progressed = true
                            break
                        }
                    }
                    cursors[index] = cursor
                }
            }
        }
        // Whatever the mixes could not supply comes straight from the Tamil pool.
        for (track in tamil) {
            if (out.size >= tamilTarget) break
            if (seen.add(track.id)) out += track
        }

        // World tail — rotate the start so resets feel different.
        val world = catalog.tracks.filterNot { it.isTamil }
        if (world.isNotEmpty() && out.size < limit) {
            val start = (rotation * 3) % world.size
            for (i in world.indices) {
                if (out.size >= limit) break
                val track = world[(start + i) % world.size]
                if (seen.add(track.id)) out += track
            }
        }
        // If the shelf still ran dry (a Tamil-less catalog), fill honestly.
        for (track in catalog.tracks) {
            if (out.size >= limit) break
            if (seen.add(track.id)) out += track
        }
        return out
    }

    /** True when the feed is currently the un-personalized Tamil shelf. */
    fun isFreshShelf(store: Store): Boolean = store.totalPlays() == 0

    private fun Float?.orZero(): Float = this ?: 0f
}
