package com.aud.android.data

import kotlin.math.ln
import kotlin.math.max

/**
 * The "For You" engine — ranks the live catalog against this listener's own
 * history (play counts, artist affinity, language affinity, likes).
 *
 * Re-tuned for a Tamil-first listener:
 *  - **Tamil leads by default.** A brand-new (or freshly reset) listener gets a
 *    feed that is overwhelmingly Tamil — roughly 4 of every 5 rows — with the rest
 *    of India and the world sprinkled in so discovery never dies.
 *  - Once real listening history exists, taste takes over: Tamil is no longer
 *    forced, but it still carries a small head start (the app's "home" bias).
 *  - Heavy artist/language affinity wins, with diversity guardrails so the feed
 *    never collapses into one artist.
 *  - Unplayed tracks get a freshness boost so discovery never dies.
 *
 * Everything is local and deterministic — zero network calls, instant re-rank.
 */
object Personalize {

    private const val MAX_PICK_REPEAT = 2          // per-artist diversity cap per pass
    private const val TAMIL_QUOTA_FRESH = 0.80f    // Tamil share for a listener with no history
    private const val TAMIL_BIAS_EXPERIENCED = 0.6f // gentle Tamil head start once taste exists
    private const val MAX_LANGUAGE_SHARE = 0.85f     // never let one language own the whole rail

    /* ── Fresh / reset feed ─────────────────────────────────────── */

    /**
     * The default feed: **Tamil-first**, ~80 % Tamil. Deterministic (seeded by
     * nothing but the catalog and the rotation key) so the page is stable while
     * scrolling but refreshes when the catalog or the listener changes.
     */
    fun defaultFeed(catalog: Catalog, limit: Int = 24, rotation: Int = 0): List<Track> {
        if (catalog.tracks.isEmpty()) return emptyList()
        val tamil = catalog.tamilTracks
        val others = catalog.tracks.filterNot { it.isTamil }
        if (tamil.isEmpty()) return interleaveMixes(catalog, limit)

        val tamilTarget = (limit * TAMIL_QUOTA_FRESH).toInt().coerceAtLeast(1)
        // Tamil first; if the catalogue is short on Tamil rows the remainder is
        // topped up from elsewhere rather than returning a half-empty rail.
        val tamilPicked = spread(tamil, tamilTarget, rotation)
        val rest = spread(others, limit - tamilPicked.size, rotation)
        val woven = weave(tamilPicked, rest, limit)
        if (woven.size >= limit) return woven
        val have = woven.mapTo(HashSet()) { it.id }
        val filler = spread(catalog.tracks, limit, rotation).filter { have.add(it.id) }
        return (woven + filler).take(limit)
    }

    /**
     * Spreads [tracks] across their mixes so a shelf never returns twelve songs
     * from one seed, rotating by [rotation] to keep repeat visits fresh.
     */
    private fun spread(tracks: List<Track>, limit: Int, rotation: Int): List<Track> {
        if (tracks.isEmpty() || limit <= 0) return emptyList()
        val byArtist = tracks.groupBy { it.leadArtist.lowercase() }
        val artists = byArtist.keys.sorted()
        val out = ArrayList<Track>(limit)
        val used = HashSet<String>()
        val seenArtist = HashMap<String, Int>()
        var round = 0
        while (out.size < limit && round < 40) {
            var progressed = false
            for (a in artists) {
                if (out.size >= limit) break
                val rows = byArtist[a] ?: continue
                val take = seenArtist[a] ?: 0
                if (take >= rows.size) continue
                val idx = (take + rotation) % rows.size
                val track = rows[idx]
                if (!used.add(track.id)) continue
                out += track
                seenArtist[a] = take + 1
                progressed = true
            }
            if (!progressed) break
            round++
        }
        return out
    }

    /** Interleaves the Tamil majority with the wider picks so the mix never reads as two blocks. */
    private fun weave(tamil: List<Track>, others: List<Track>, limit: Int): List<Track> {
        if (others.isEmpty()) return tamil.take(limit)
        if (tamil.isEmpty()) return others.take(limit)
        val out = ArrayList<Track>(limit)
        var t = 0
        var o = 0
        var sinceOther = 0
        while (out.size < limit && (t < tamil.size || o < others.size)) {
            val wantOther = sinceOther >= 4 && o < others.size
            if ((wantOther || t >= tamil.size) && o < others.size) {
                out += others[o++]
                sinceOther = 0
            } else if (t < tamil.size) {
                out += tamil[t++]
                sinceOther++
            } else break
        }
        return out
    }

    /* ── Personalized feed ──────────────────────────────────────── */

    fun forYou(catalog: Catalog, store: Store, limit: Int = 24): List<Track> {
        if (catalog.tracks.isEmpty()) return emptyList()

        val plays = store.playCounts()
        val artistAff = store.artistAffinity()
        val langAff = store.languageAffinity()
        val totalPlays = plays.values.sum()

        // Fresh ears (or a freshly reset profile) — Tamil-first spread, no history to honour.
        if (totalPlays == 0) return defaultFeed(catalog, limit)

        val liked = store.likedIds()
        val maxArtist = (artistAff.values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val maxLang = (langAff.values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()

        // Stable jitter per track id — keeps the rail from feeling frozen without randomness.
        fun jitter(id: String): Float = ((kotlin.math.abs(id.hashCode()) % 1000) / 1000.0).toFloat() * 0.5f

        data class Scored(val track: Track, val score: Float, val artistKey: String)

        val scored = catalog.tracks.map { t ->
            val key = store.artistKey(t)
            val playsT = plays[t.id] ?: 0
            val s =
                (artistAff[key] ?: 0).toFloat() / maxArtist * 2.7f +                  // artist lean
                    (langAff[store.languageKey(t)]?.toFloat() ?: 0f) / maxLang * 1.4f +
                    (if (t.isTamil) TAMIL_BIAS_EXPERIENCED else 0f) +                    // home bias, small
                    (if (t.id in liked) 1.7f else 0f) +                                  // explicitly saved
                    (if (playsT == 0) 1.3f else 0f) +                                    // discovery boost
                    jitter(t.id) -                                                       // gentle shuffle
                    (ln(playsT + 1.0) * 0.9).toFloat()                                   // demote overplayed
            Scored(t, s, key)
        }.sortedByDescending { it.score }

        // Greedy pick with artist- and language-diversity guardrails.
        val picked = ArrayList<Track>(limit)
        val artistPicks = HashMap<String, Int>()
        val languagePicks = HashMap<String, Int>()
        val languageCap = max(3, (limit * MAX_LANGUAGE_SHARE).toInt())
        for (candidate in scored) {
            if (picked.size >= limit) break
            val artistCount = artistPicks[candidate.artistKey] ?: 0
            if (artistCount >= MAX_PICK_REPEAT) continue
            val lang = store.languageKey(candidate.track)
            if ((languagePicks[lang] ?: 0) >= languageCap) continue
            picked += candidate.track
            artistPicks[candidate.artistKey] = artistCount + 1
            languagePicks[lang] = (languagePicks[lang] ?: 0) + 1
        }
        // Top-up if the guardrails left us short.
        if (picked.size < limit) {
            val have = picked.mapTo(HashSet()) { it.id }
            for (candidate in scored) {
                if (picked.size >= limit) break
                if (have.add(candidate.track.id)) picked += candidate.track
            }
        }
        return picked
    }

    /** Round-robin across mixes — deterministic spread when there is nothing else to go on. */
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

    /* ── "Because you played …" ─────────────────────────────────── */

    /**
     * The listener's strongest artist (by affinity), with their catalogue tracks —
     * the seed for a "Because you played …" rail. Null when there is no history or
     * fewer than three tracks to fill a rail.
     */
    fun becauseYou(catalog: Catalog, store: Store, limit: Int = 12): Pair<String, List<Track>>? {
        val top = store.artistAffinity().entries.maxByOrNull { it.value }?.key ?: return null
        if (top.isBlank()) return null
        val needle = top.split(" ").firstOrNull().orEmpty()
        if (needle.length < 3) return null
        val tracks = catalog.tracks
            .filter { it.artistName.lowercase().contains(needle) }
            .sortedByDescending { it.releaseYear }
            .take(limit)
        if (tracks.size < 3) return null
        val display = tracks.first().leadArtist
        return display to tracks
    }

    /* ── Taste summary (shown in Settings) ──────────────────────── */

    /**
     * A human-readable read of the current profile, e.g.
     * `"Tamil · Anirudh Ravichander, Sid Sriram"`. Empty when there is no history.
     */
    fun tasteSummary(store: Store): String {
        val languages = store.languageAffinity().entries.sortedByDescending { it.value }.take(2)
        val artists = store.artistAffinity().entries.sortedByDescending { it.value }
            .map { it.key.substringBefore(",").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(2)
        if (languages.isEmpty() && artists.isEmpty()) return ""
        val lang = languages.joinToString(", ") { it.key.replaceFirstChar { c -> c.titlecase() } }
        return buildString {
            if (lang.isNotBlank()) append(lang.uppercase())
            if (artists.isNotEmpty()) {
                if (lang.isNotBlank()) append(" · ")
                append(artists.joinToString(", ") { a -> a.split(" ").joinToString(" ") { w -> w.replaceFirstChar { c -> c.titlecase() } } })
            }
        }
    }

    /** How many spins the profile is built from — surfaced as a stat. */
    fun totalSpins(store: Store): Int = store.playCounts().values.sum()
}
