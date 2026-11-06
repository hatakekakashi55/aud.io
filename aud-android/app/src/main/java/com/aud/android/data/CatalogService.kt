package com.aud.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Catalog service — talks to the live AUD music service over plain HTTPS.
 * Native HTTP has no CORS layer, so the catalog load and the live search work
 * exactly the same from any device with connectivity.
 *
 *   GET {BASE}/api/search/songs?query=<q>  →  {"data":{"results":[ … ]}}
 *
 * The catalog is hydrated from a **Tamil-first shelf of seed queries** — Tamil
 * cinema's composers and eras come first, then a handful of other languages so
 * the home feed still feels like a world of music rather than one region.
 * Every seed doubles as a live editorial mix, and the union is far larger than
 * the old 12-query catalog, which is what made the feed feel "limited".
 *
 * Queries run in parallel on a small pool (same idea as the web client), so a
 * bigger shelf does not mean a slower boot. Partial success is normal: a mix
 * only disappears if its own query came back empty.
 *
 * The catalog is cached on-device for [CACHE_TTL_MS]; a stale cache is only
 * used when the network is unreachable (so the app still opens offline).
 *
 * All functions are blocking — call from Dispatchers.IO.
 */
object CatalogService {

    const val BASE = "https://aud29.thangamprabhu1.workers.dev"

    private const val CACHE_PREFS = "aud_catalog_cache"
    private const val CACHE_KEY = "catalog_v2"
    private const val CACHE_TTL_MS = 15L * 60L * 1000L
    private const val CACHE_VERSION = 2

    /** How many tracks each seed contributes to its mix (and to the union). */
    private const val TRACKS_PER_MIX = 20

    /** Below this the catalog is too thin to feel like a real home feed. */
    private const val MIN_TRACKS = 40
    private const val MIN_MIXES = 3

    private const val PARALLELISM = 8

    /**
     * The shelf. **Tamil first** — roughly two thirds of the seeds — then a
     * short worldwide tail. Order matters: it is the order of the home rails,
     * and the order used to interleave the "fresh ears" feed.
     */
    private val SEEDS = listOf(
        // ── Tamil ────────────────────────────────────────────────────────
        "Tamil Top 40" to "tamil hits",
        "Anirudh" to "anirudh ravichander hits",
        "A. R. Rahman" to "a r rahman tamil hits",
        "Ilaiyaraaja" to "ilaiyaraaja tamil hits",
        "Tamil Love" to "tamil love songs",
        "Kuthu & Mass" to "kuthu songs tamil",
        "Melody Nights" to "tamil melody hits",
        "Yuvan Shankar Raja" to "yuvan shankar raja hits",
        "Harris Jayaraj" to "harris jayaraj hits",
        "Santhosh Narayanan" to "santhosh narayanan songs",
        "G. V. Prakash" to "gv prakash kumar songs",
        "Tamil Sad" to "sad tamil songs",
        "90s Tamil Gold" to "ilaiyaraaja 90s tamil songs",
        "Vijay Hits" to "vijay songs tamil",
        "Ajith Hits" to "ajith kumar songs tamil",
        "Dhanush" to "dhanush songs tamil",
        "Kollywood Fresh" to "latest tamil songs 2024",
        "Tamil Rap" to "tamil rap songs",
        "Tamil Indie" to "tamil indie songs",
        "Tamil Party" to "tamil dance songs",
        // ── The rest of the world (a tail, not the headline) ─────────────
        "Bollywood Hits" to "bollywood hits",
        "Hindi Love" to "hindi love songs",
        "Telugu Hits" to "telugu hits",
        "Malayalam Hits" to "malayalam hits",
        "English Pop" to "english pop hits",
        "K-Pop" to "kpop hits",
        "Global Top 40" to "top 40 hits 2024",
        "Lo-Fi & Chill" to "lofi chill beats",
    )

    class CatalogException(message: String) : Exception(message)

    /* ── Public API ─────────────────────────────────────────────── */

    /** Fresh catalog (or a ≤15-min cached one). Falls back to a stale cache only when offline. */
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
    fun search(query: String, limit: Int = 20): List<Track> {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "empty query" }
        return runCatching { fetchResults(trimmed).take(limit) }
            .getOrElse { throw CatalogException(friendly(it)) }
    }

    /* ── Catalog build ──────────────────────────────────────────── */

    @Throws(CatalogException::class)
    private fun fetchAndBuild(): Catalog {
        val fetched = fetchAllSeeds()
        val mixes = fetched.mapNotNull { (title, tracks) ->
            if (tracks.isEmpty()) null else Mix(title, tracks.size, tracks.take(TRACKS_PER_MIX))
        }
        val merged = mixes.flatMap { it.tracks }.distinctBy { it.id }
        if (merged.size < MIN_TRACKS || mixes.size < MIN_MIXES) {
            throw CatalogException(
                "The service answered, but with too little usable data " +
                    "(${merged.size} tracks from ${mixes.size}/${SEEDS.size} mixes)."
            )
        }
        return Catalog(merged, mixes, System.currentTimeMillis())
    }

    /** Fire every seed at once on a bounded pool; failures are simply skipped. */
    private fun fetchAllSeeds(): List<Pair<String, List<Track>>> {
        val executor = Executors.newFixedThreadPool(PARALLELISM) { runnable ->
            Thread(runnable, "aud-catalog").apply { isDaemon = true }
        }
        return try {
            val tasks: List<Callable<Pair<String, List<Track>>>> = SEEDS.map { (title, query) ->
                Callable {
                    val tracks: List<Track> = runCatching { fetchResults(query) }.getOrDefault(emptyList())
                    title to tracks
                }
            }
            executor.invokeAll(tasks, 75, TimeUnit.SECONDS).map { future ->
                runCatching { future.get() }.getOrElse { Pair("", emptyList<Track>()) }
            }.filter { it.first.isNotEmpty() }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun fetchResults(query: String): List<Track> {
        val body = httpGet("$BASE/api/search/songs?query=${URLEncoder.encode(query, "UTF-8")}")
        return parseSongs(body)
    }

    /* ── HTTP ───────────────────────────────────────────────────── */

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 18_000
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
            val artist = clean(raw.optString("primaryArtists").ifBlank { "Unknown Artist" })
            val album = clean(raw.optString("album").ifBlank { "Singles" })
            tracks += Track(
                id = "ls_${raw.optString("id")}",
                title = title,
                artistName = artist,
                albumName = album,
                imageUrl = imageUrl(raw),
                audioUrl = audioUrl,
                durationSec = raw.optInt("duration", 0).coerceAtLeast(0),
                releaseYear = raw.optString("year").toIntOrNull() ?: 0,
                language = resolveLanguage(raw.optString("language"), title, artist, album),
                sourceUrl = raw.optString("url").ifBlank { null },
            )
        }
        return tracks
    }

    /**
     * Canonical language for a track. When the feed omits it, fall back to a
     * Tamil-credit heuristic so Tamil rows still land in the Tamil rails.
     */
    private fun resolveLanguage(raw: String, title: String, artist: String, album: String): String {
        val canonical = Languages.canonical(raw)
        if (canonical.isNotEmpty() && canonical != "Music") return canonical
        return if (Languages.inferTamil(title, artist, album)) "Tamil" else "Music"
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

            val root = JSONObject()
                .put("v", CACHE_VERSION)
                .put("ts", catalog.fetchedAtMs)
                .put("tracks", tracksJson)
                .put("mixes", mixesJson)

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
                    language = Languages.canonical(o.optString("lang")),
                    sourceUrl = o.optString("link").ifBlank { null },
                )
            }
            if (tracks.size < MIN_TRACKS) return null
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
            Catalog(tracks, mixes, ts)
        }.getOrNull()
    }
}
