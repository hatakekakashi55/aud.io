package com.aud.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Catalog service — talks to the live AUD music service over plain HTTPS.
 * Native HTTP has no CORS layer, so the catalog load and the live search work
 * exactly the same from any device with connectivity.
 *
 *   GET {BASE}/api/search/songs?query=<q>  →  {"data":{"results":[ … ]}}
 *
 * The catalog is now **Tamil-first and much wider**: ~46 seed buckets instead of
 * 12, split into three tiers —
 *
 *  1. **Tamil core** (majority): composers, singers, eras, moods, film songs,
 *     indie/Tamil-Rap, melody/kuthu/devotional.
 *  2. **Indian neighbours**: Bollywood, Telugu, Malayalam, Kannada, Punjabi,
 *     Hindi indie — the natural second ring for a Tamil listener.
 *  3. **Worldwide**: Pop, Hip-Hop, R&B, Rock, Lo-fi, K-pop, Latin, Afrobeats…
 *
 * Because the worker's search is fuzzy, every seed result is filtered by its
 * *reported* language before it is allowed into a shelf, so a "Tamil melody"
 * query never quietly returns Hindi rows. Tamil buckets are the only ones that
 * accept blank-language rows (verified by Tamil script in the title).
 *
 * Cached on-device for [CACHE_TTL_MS]; a stale cache is only used when the
 * network is unreachable (so the app still opens offline).
 *
 * All functions are blocking — call from Dispatchers.IO.
 */
object CatalogService {

    const val BASE = "https://aud29.thangamprabhu1.workers.dev"

    private const val CACHE_PREFS = "aud_catalog_cache"
    private const val CACHE_KEY = "catalog_v3"
    private const val CACHE_TTL_MS = 20L * 60L * 1000L
    private const val CACHE_VERSION = 3

    /** Per-bucket per-language row cap — keeps one huge result set from swamping the rest. */
    private const val MAX_PER_LANGUAGE = 8

    /** Buckets are fetched this many at a time: fast, but never a thundering herd. */
    private const val FETCH_PARALLELISM = 8

    class CatalogException(message: String) : Exception(message)

    /* ── Seeds ──────────────────────────────────────────────────── */

    enum class Tier { TAMIL, INDIA, WORLD, ANY }

    /**
     * @param languages  accepted `language` values (lowercase). Empty = accept all.
     * @param shelf      home shelf this bucket feeds, or null for chart/only buckets.
     * @param weight     ordering within a shelf (higher first).
     */
    data class Seed(
        val title: String,
        val query: String,
        val tier: Tier,
        val languages: Set<String> = emptySet(),
        val shelf: String? = null,
        val kicker: String = "",
        val weight: Int = 0,
    )

    object ShelfKey {
        const val TAMIL_FRESH = "tamil_fresh"
        const val TAMIL_GOLD = "tamil_gold"
        const val TAMIL_LOVE = "tamil_love"
        const val TAMIL_ENERGY = "tamil_energy"
        const val TAMIL_VOICES = "tamil_voices"
        const val TAMIL_COMPOSERS = "tamil_composers"
        const val TAMIL_INDIE = "tamil_indie"
        const val TAMIL_DEVOTIONAL = "tamil_devotional"
        const val INDIA = "india"
        const val WORLD = "world"
        const val CHARTS = "charts"
    }

    private val TAMIL_LANGS = setOf("tamil")
    private val INDIAN_LANGS = setOf("hindi", "telugu", "malayalam", "kannada", "punjabi", "bengali", "marathi")

    /** Every seed, in fetch order. Tamil first — if the network is slow, Kollywood lands first. */
    private val SEEDS: List<Seed> = buildList {
        /* 1 ── Tamil: fresh releases & chartbusters */
        add(Seed("Fresh Tamil", "latest tamil hits 2025", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_FRESH, "New this week", 30))
        add(Seed("Tamil Trending", "trending tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_FRESH, "New this week", 28))
        add(Seed("Kollywood 2024", "tamil songs 2024", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_FRESH, "New this week", 24))
        add(Seed("New Kollywood", "tamil new releases", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_FRESH, "New this week", 22))
        add(Seed("Anirudh Ravichander", "anirudh ravichander hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_FRESH, "New this week", 20))

        /* 2 ── Tamil: the canon */
        add(Seed("Ilaiyaraaja", "ilaiyaraaja hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 30))
        add(Seed("A. R. Rahman", "a r rahman tamil hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 29))
        add(Seed("M. S. Viswanathan", "ms viswanathan hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 24))
        add(Seed("80s & 90s Tamil", "90s tamil songs hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 26))
        add(Seed("S. P. Balasubrahmanyam", "spb tamil hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 22))
        add(Seed("Evergreen Tamil Melody", "tamil melody evergreen", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_GOLD, "The canon", 20))

        /* 3 ── Tamil: love */
        add(Seed("Tamil Love", "tamil love songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_LOVE, "Feel something", 30))
        add(Seed("Romantic Duets", "tamil romantic duet songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_LOVE, "Feel something", 26))
        add(Seed("Tamil Sad", "tamil sad songs breakup", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_LOVE, "Feel something", 25))
        add(Seed("Midnight Tamil", "tamil night love melody", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_LOVE, "Feel something", 18))

        /* 4 ── Tamil: energy */
        add(Seed("Kuthu Anthems", "kuthu tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_ENERGY, "Full throttle", 30))
        add(Seed("Mass BGM", "tamil mass bgm", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_ENERGY, "Full throttle", 27))
        add(Seed("Vijay Hits", "vijay songs tamil", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_ENERGY, "Full throttle", 24))
        add(Seed("Ajith Hits", "ajith kumar tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_ENERGY, "Full throttle", 20))
        add(Seed("Dance Tamil", "tamil dance songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_ENERGY, "Full throttle", 18))

        /* 5 ── Tamil: voices */
        add(Seed("Sid Sriram", "sid sriram tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_VOICES, "Voices", 30))
        add(Seed("A. R. Ameen & Pradeep", "ar ameen tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_VOICES, "Voices", 18))
        add(Seed("Chinmayi & Shreya", "chinmayi shreya ghoshal tamil", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_VOICES, "Voices", 24))
        add(Seed("Hariharan & Sadhana", "hariharan tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_VOICES, "Voices", 20))
        add(Seed("Karthik & Saindhavi", "karthik raja tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_VOICES, "Voices", 16))

        /* 6 ── Tamil: composers beyond the big two */
        add(Seed("G. V. Prakash", "g v prakash kumar hits tamil", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 30))
        add(Seed("Harris Jayaraj", "harris jayaraj hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 29))
        add(Seed("Yuvan Shankar Raja", "yuvan shankar raja hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 28))
        add(Seed("D. Imman", "d imman tamil songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 21))
        add(Seed("Vidyasagar", "vidyasagar tamil hits", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 19))
        add(Seed("Santhosh Narayanan", "santhosh narayanan tamil", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_COMPOSERS, "Composers", 18))

        /* 7 ── Tamil: indie & rap */
        add(Seed("Tamil Indie", "tamil indie songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_INDIE, "New voices", 30))
        add(Seed("Tamil Rap", "tamil rap hip hop", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_INDIE, "New voices", 27))
        add(Seed("Independent Tamil", "tamil independent music", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_INDIE, "New voices", 22))

        /* 8 ── Tamil: devotional & folk */
        add(Seed("Bhakti Tamil", "tamil devotional songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_DEVOTIONAL, "Roots", 30))
        add(Seed("A. R. Rahman Devotional", "a r rahman devotional tamil", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_DEVOTIONAL, "Roots", 20))
        add(Seed("Tamil Folk", "tamil folk songs", Tier.TAMIL, TAMIL_LANGS, ShelfKey.TAMIL_DEVOTIONAL, "Roots", 18))

        /* 9 ── India, the second ring */
        add(Seed("Bollywood Hits", "bollywood hits", Tier.INDIA, INDIAN_LANGS, ShelfKey.INDIA, "Across India", 30))
        add(Seed("Hindi Fresh", "hindi new songs 2025", Tier.INDIA, INDIAN_LANGS, ShelfKey.INDIA, "Across India", 26))
        add(Seed("Telugu Hits", "telugu hit songs", Tier.INDIA, INDIAN_LANGS, ShelfKey.INDIA, "Across India", 24))
        add(Seed("Malayalam Hits", "malayalam hit songs", Tier.INDIA, INDIAN_LANGS, ShelfKey.INDIA, "Across India", 20))
        add(Seed("Punjabi Hits", "punjabi hits", Tier.INDIA, INDIAN_LANGS, ShelfKey.INDIA, "Across India", 18))

        /* 10 ── The world */
        add(Seed("Global Top 40", "top 40 hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 30))
        add(Seed("Pop Essentials", "pop hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 28))
        add(Seed("Hip-Hop Heat", "hip hop hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 26))
        add(Seed("R&B Slow Jams", "r&b hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 22))
        add(Seed("Electronic Pulse", "edm hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 20))
        add(Seed("Rock Classics", "rock classics", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 18))
        add(Seed("Lo-fi Focus", "lofi chill beats", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 16))
        add(Seed("K-Pop Wave", "kpop hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 14))
        add(Seed("Afrobeats", "afrobeats hits", Tier.WORLD, emptySet(), ShelfKey.WORLD, "Around the world", 12))

        /* 11 ── Chart-only buckets (feed the trending list, never their own shelf) */
        add(Seed("Tamil Top 50", "tamil top 50 songs", Tier.TAMIL, TAMIL_LANGS, null, weight = 40))
        add(Seed("India Top 50", "india top 50 songs", Tier.INDIA, INDIAN_LANGS, null, weight = 20))
        add(Seed("World Top 50", "global top 50 songs", Tier.WORLD, emptySet(), null, weight = 10))
    }

    /* ── Public API ─────────────────────────────────────────────── */

    /** Fresh catalog (or a ≤20-min cached one). Falls back to a stale cache only when offline. */
    @Throws(CatalogException::class)
    fun loadCatalog(context: Context, forceRefresh: Boolean = false): Catalog {
        if (!forceRefresh) {
            readCache(context)?.let { if (System.currentTimeMillis() - it.fetchedAtMs < CACHE_TTL_MS) return it }
        }
        val fresh = runCatching { fetchAndBuild() }
        return fresh.getOrElse { networkError ->
            readCache(context)?.let { return it }   // stale-but-real data beats an error screen
            throw CatalogException(friendly(networkError))
        }.also { writeCache(context, it) }
    }

    /** Live search — powers the Find tab's "From the Wire" section. */
    @Throws(CatalogException::class)
    fun search(query: String, limit: Int = 24): List<Track> {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "empty query" }
        return runCatching { fetchResults(trimmed, Seed("search", trimmed, Tier.ANY)).take(limit) }
            .getOrElse { throw CatalogException(friendly(it)) }
    }

    /**
     * "More like this" — one extra live query for an artist, used by the track
     * sheet and the artist shelves. Tamil-first: a Kollywood artist is searched
     * with a Tamil qualifier so the worker doesn't drift into Hindi results.
     */
    @Throws(CatalogException::class)
    fun moreLikeThis(artist: String, tamil: Boolean, limit: Int = 16): List<Track> {
        val trimmed = artist.trim()
        require(trimmed.isNotEmpty()) { "empty artist" }
        val query = if (tamil && !trimmed.lowercase().contains("tamil")) "$trimmed tamil" else trimmed
        val seed = Seed("more", query, if (tamil) Tier.TAMIL else Tier.WORLD, if (tamil) TAMIL_LANGS else emptySet())
        return runCatching { fetchResults(query, seed).take(limit) }
            .getOrElse { throw CatalogException(friendly(it)) }
    }

    /** Number of buckets the catalog is built from — surfaced in the UI. */
    val seedCount: Int get() = SEEDS.size

    /* ── Catalog build ──────────────────────────────────────────── */

    @Throws(CatalogException::class)
    private fun fetchAndBuild(): Catalog {
        val now = System.currentTimeMillis()

        // Bounded parallel fetch — 54 buckets, FETCH_PARALLELISM requests in flight at a time.
        val bucketRows: List<Pair<Seed, List<Track>>> = runBlocking {
            SEEDS.chunked(FETCH_PARALLELISM).flatMap { batch ->
                coroutineScope {
                    batch.map { seed ->
                        async(Dispatchers.IO) {
                            seed to runCatching { fetchResults(seed.query, seed) }.getOrNull().orEmpty()
                        }
                    }.awaitAll()
                }
            }
        }

        val mixes = ArrayList<Mix>(SEEDS.size)
        val chartPool = LinkedHashMap<String, Track>()
        for ((seed, rows) in bucketRows) {
            if (rows.isEmpty()) continue
            if (seed.shelf == null) {
                rows.forEach { chartPool.putIfAbsent(it.id, it) }
            } else {
                mixes += Mix(seed.title, rows.size, rows.take(14))
            }
        }

        val merged = LinkedHashMap<String, Track>()
        mixes.flatMap { it.tracks }.forEach { merged.putIfAbsent(it.id, it) }
        chartPool.values.forEach { merged.putIfAbsent(it.id, it) }
        val allTracks = merged.values.toList()

        if (allTracks.count { it.isTamil } < 24 || mixes.size < 6) {
            throw CatalogException(
                "The service answered, but with too little usable Tamil data " +
                    "(${allTracks.count { it.isTamil }} Tamil of ${allTracks.size} tracks " +
                    "from ${mixes.size}/${SEEDS.size} buckets)."
            )
        }

        val shelves = buildShelves(mixes, chartPool.values.toList())
        return Catalog(allTracks, mixes, shelves, now)
    }

    /** Groups mixes into home-page shelves, Tamil shelves first. */
    private fun buildShelves(mixes: List<Mix>, chartTracks: List<Track>): List<Shelf> {
        val byShelf = LinkedHashMap<String, MutableList<Pair<Seed, Mix>>>()
        val seedByTitle = SEEDS.associateBy { it.title }
        for (mix in mixes) {
            val seed = seedByTitle[mix.title] ?: continue
            val key = seed.shelf ?: continue
            byShelf.getOrPut(key) { mutableListOf() } += seed to mix
        }

        fun shelfFor(key: String, kicker: String, title: String, subtitle: String, cap: Int = 20): Shelf? {
            val entries = byShelf[key] ?: return null
            val ordered = entries.sortedByDescending { it.first.weight }
            val tracks = ordered.flatMap { it.second.tracks }.distinctBy { it.id }.take(cap)
            if (tracks.isEmpty()) return null
            return Shelf(key, kicker, title, subtitle, tracks)
        }

        val shelves = mutableListOf<Shelf>()
        shelfFor(ShelfKey.TAMIL_FRESH, "Tamil · fresh", "Straight off the Kollywood press", "Released in the last few months, live from the wire")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_LOVE, "Tamil · romance", "Love, in Tamil", "Duets, melodies and the soft stuff")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_ENERGY, "Tamil · energy", "Kuthu, mass & full throttle", "For the gym, the bus and the wedding")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_GOLD, "Tamil · the canon", "Evergreen Tamil gold", "Ilaiyaraaja, Rahman, MSV — the long library")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_COMPOSERS, "Tamil · composers", "Beyond the big two", "Harris, Yuvan, GV, Imman, Santhosh")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_VOICES, "Tamil · voices", "Voices you know by heart", "Sid Sriram, Chinmayi, Shreya, Hariharan")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_INDIE, "Tamil · new voices", "Tamil indie & rap", "Independent Kollywood, without a label")?.let(shelves::add)
        shelfFor(ShelfKey.TAMIL_DEVOTIONAL, "Tamil · roots", "Bhakti & folk", "Devotional and the older folk line")?.let(shelves::add)
        shelfFor(ShelfKey.INDIA, "Across India", "The neighbours", "Telugu, Malayalam, Punjabi, Bollywood")?.let(shelves::add)
        shelfFor(ShelfKey.WORLD, "Around the world", "The rest of the map", "Pop, hip-hop, lo-fi, K-pop, Afrobeats")?.let(shelves::add)

        if (chartTracks.isNotEmpty()) {
            shelves += Shelf(
                ShelfKey.CHARTS,
                "The chart",
                "Trending right now",
                "Tamil, India and the world — one list",
                chartTracks.take(30),
            )
        }
        return shelves
    }

    /**
     * Fetches one seed and applies its language policy:
     *  - Tamil buckets accept `tamil` rows plus blank-language rows proven Tamil by script;
     *  - India buckets accept the listed Indian languages;
     *  - world buckets accept anything that isn't already Tamil (no duplicates).
     */
    private fun fetchResults(query: String, seed: Seed): List<Track> {
        val body = httpGet("$BASE/api/search/songs?query=${URLEncoder.encode(query, "UTF-8")}")
        val parsed = parseSongs(body)
        val filtered = parsed.filter { track ->
            when (seed.tier) {
                Tier.TAMIL -> track.isTamil
                Tier.INDIA -> !track.isTamil && (
                    seed.languages.isEmpty() ||
                        seed.languages.contains(track.language.trim().lowercase()) ||
                        track.language.isBlank() || track.language.equals("music", true)
                    )
                Tier.WORLD -> !track.isTamil &&
                    (seed.languages.isEmpty() || seed.languages.contains(track.language.trim().lowercase()))
                Tier.ANY -> true
            }
        }
        // Keep each bucket from collapsing into a single language when the worker is generous.
        return filtered.groupBy { it.language.lowercase() }
            .flatMap { (_, rows) -> rows.take(MAX_PER_LANGUAGE) }
    }

    /* ── HTTP ───────────────────────────────────────────────────── */

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", "AUD-Android/1.1 (+https://aud29.thangamprabhu1.workers.dev)")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw CatalogException("Service HTTP $code — ${body.take(200)}")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    /* ── Parsing (org.json — no dependencies) ───────────────────── */

    private fun parseSongs(body: String): List<Track> {
        if (body.isBlank()) return emptyList()
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val results = root.optJSONObject("data")?.optJSONArray("results") ?: return emptyList()
        val tracks = ArrayList<Track>(results.length())
        for (i in 0 until results.length()) {
            val raw = results.optJSONObject(i) ?: continue
            val audioUrl = bestAudio(raw) ?: continue
            val title = clean(raw.optString("name").ifBlank { raw.optString("title") })
            if (title.isEmpty()) continue
            tracks += Track(
                id = "ls_${raw.optString("id")}",
                title = title,
                artistName = clean(raw.optString("primaryArtists").ifBlank { "Unknown Artist" }),
                albumName = clean(raw.optString("album").ifBlank { "Singles" }),
                imageUrl = imageUrl(raw),
                audioUrl = audioUrl,
                durationSec = raw.optInt("duration", 0).coerceAtLeast(0),
                releaseYear = raw.optString("year").toIntOrNull() ?: 0,
                language = clean(raw.optString("language")).replaceFirstChar { it.titlecase() },
                sourceUrl = raw.optString("url").ifBlank { null },
            )
        }
        return tracks
    }

    /** Prefer 320 → 160 → 96 → 48 kbps. Worker reports quality as e.g. "320kbps". */
    private fun bestAudio(raw: JSONObject): String? {
        val downloads = raw.optJSONArray("downloadUrl") ?: return null
        val byKbps = HashMap<String, String>()
        for (i in 0 until downloads.length()) {
            val entry = downloads.optJSONObject(i) ?: continue
            val kbps = entry.optString("quality").filter { it.isDigit() }
            val url = entry.optString("url")
            if (kbps.isNotEmpty() && url.isNotBlank()) byKbps[kbps] = url
        }
        return listOf("320", "160", "96", "48").firstNotNullOfOrNull { byKbps[it] }
    }

    private fun imageUrl(raw: JSONObject): String? {
        val images = raw.optJSONArray("image") ?: return null
        var best: String? = null
        var fallback: String? = null
        for (i in 0 until images.length()) {
            val entry = images.optJSONObject(i) ?: continue
            val link = entry.optString("link")
            if (link.isBlank()) continue
            when (entry.optString("quality")) {
                "500x500" -> best = link
                "150x150" -> if (best == null) best = link
                else -> fallback = link
            }
        }
        return best ?: fallback
    }

    /** The upstream feed embeds HTML entities (and raw quotes) in names. */
    private fun clean(value: String): String =
        value.replace("&quot;", "\"").replace("&#34;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'").replace("&#x27;", "'")
            .replace("&nbsp;", " ")
            .trim()

    private fun friendly(error: Throwable): String = when {
        error is CatalogException -> error.message ?: "Service error"
        error is java.net.UnknownHostException -> "No DNS route to the music service — check the connection."
        error is java.net.SocketTimeoutException -> "The service timed out. Check the connection and retry."
        else -> "Service unreachable: ${error.message ?: error.javaClass.simpleName}"
    }

    /* ── On-device cache (JSON in SharedPreferences) ────────────── */

    private fun writeCache(context: Context, catalog: Catalog) {
        runCatching {
            fun trackJson(t: Track): JSONObject = JSONObject()
                .put("id", t.id).put("title", t.title)
                .put("artist", t.artistName).put("album", t.albumName)
                .put("image", t.imageUrl ?: "").put("audio", t.audioUrl)
                .put("dur", t.durationSec).put("year", t.releaseYear)
                .put("lang", t.language).put("link", t.sourceUrl ?: "")

            val tracksJson = JSONArray()
            catalog.tracks.forEach { tracksJson.put(trackJson(it)) }

            val mixesJson = JSONArray()
            catalog.mixes.forEach { mix ->
                val ids = JSONArray()
                mix.tracks.forEach { ids.put(it.id) }
                mixesJson.put(JSONObject().put("title", mix.title).put("trackIds", ids))
            }

            val shelvesJson = JSONArray()
            catalog.shelves.forEach { shelf ->
                val ids = JSONArray()
                shelf.tracks.forEach { ids.put(it.id) }
                shelvesJson.put(
                    JSONObject()
                        .put("key", shelf.key)
                        .put("kicker", shelf.kicker)
                        .put("title", shelf.title)
                        .put("sub", shelf.subtitle)
                        .put("trackIds", ids)
                )
            }

            val root = JSONObject()
                .put("v", CACHE_VERSION)
                .put("ts", catalog.fetchedAtMs)
                .put("tracks", tracksJson)
                .put("mixes", mixesJson)
                .put("shelves", shelvesJson)

            context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(CACHE_KEY, root.toString())
                .apply()
        }
    }

    private fun readCache(context: Context): Catalog? {
        return runCatching {
            val raw = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
                .getString(CACHE_KEY, null) ?: return null
            val root = JSONObject(raw)
            if (root.optInt("v", 0) != CACHE_VERSION) return null
            val ts = root.optLong("ts", 0L)

            val trackObjects = root.getJSONArray("tracks")
            val tracks = ArrayList<Track>(trackObjects.length())
            for (i in 0 until trackObjects.length()) {
                val o = trackObjects.getJSONObject(i)
                tracks += Track(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    artistName = o.optString("artist"),
                    albumName = o.optString("album", "Singles"),
                    imageUrl = o.optString("image").ifBlank { null },
                    audioUrl = o.getString("audio"),
                    durationSec = o.optInt("dur", 0),
                    releaseYear = o.optInt("year", 0),
                    language = o.optString("lang"),
                    sourceUrl = o.optString("link").ifBlank { null },
                )
            }
            if (tracks.size < 24) return null
            val byId = tracks.associateBy { it.id }

            val mixes = ArrayList<Mix>()
            val mixObjects = root.optJSONArray("mixes") ?: JSONArray()
            for (i in 0 until mixObjects.length()) {
                val o = mixObjects.getJSONObject(i)
                val ids = o.optJSONArray("trackIds") ?: continue
                val mixTracks = ArrayList<Track>()
                for (j in 0 until ids.length()) byId[ids.getString(j)]?.let { mixTracks += it }
                if (mixTracks.isNotEmpty()) mixes += Mix(o.getString("title"), mixTracks.size, mixTracks)
            }

            val shelves = ArrayList<Shelf>()
            val shelfObjects = root.optJSONArray("shelves") ?: JSONArray()
            for (i in 0 until shelfObjects.length()) {
                val o = shelfObjects.getJSONObject(i)
                val ids = o.optJSONArray("trackIds") ?: continue
                val shelfTracks = ArrayList<Track>()
                for (j in 0 until ids.length()) byId[ids.getString(j)]?.let { shelfTracks += it }
                if (shelfTracks.isEmpty()) continue
                shelves += Shelf(
                    key = o.optString("key"),
                    kicker = o.optString("kicker"),
                    title = o.optString("title"),
                    subtitle = o.optString("sub"),
                    tracks = shelfTracks,
                )
            }
            Catalog(tracks, mixes, shelves, ts)
        }.getOrNull()
    }
}
