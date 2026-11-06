package com.aud.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
 * The catalog is cached on-device for [CACHE_TTL_MS]; a stale cache is only
 * used when the network is unreachable (so the app still opens offline).
 *
 * All functions are blocking — call from Dispatchers.IO.
 */
object CatalogService {

    const val BASE = "https://aud29.thangamprabhu1.workers.dev"

    private const val CACHE_PREFS = "aud_catalog_cache"
    private const val CACHE_KEY = "catalog_v1"
    private const val CACHE_TTL_MS = 15L * 60L * 1000L
    private const val CACHE_VERSION = 1

    /** Editorial seed mixes — worldwide spread with a local shelf. */
    /** Worldwide shelf — global genres first, home turf last. */
    private val SEEDS = listOf(
        "Global Top 40" to "top 40 hits",
        "Pop Essentials" to "pop hits 2024",
        "Hip-Hop Heat" to "hip hop hits",
        "Electronic Pulse" to "edm hits",
        "R&B Slow Jams" to "r&b hits",
        "Latin Fuego" to "latin hits",
        "K-Pop Wave" to "kpop hits",
        "Bollywood Brass" to "bollywood hits",
        "Rahman Files" to "a r rahman",
        "Ilaiyaraaja: Maestro" to "ilaiyaraaja",
        "Love on Loop (Tamil)" to "tamil love songs",
        "Fresh Ink" to "latest tamil",
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
        val mixes = SEEDS.mapNotNull { (title, query) ->
            val tracks = runCatching { fetchResults(query) }.getOrNull().orEmpty()
            if (tracks.isEmpty()) null else Mix(title, tracks.size, tracks.take(12))
        }
        val merged = mixes.flatMap { it.tracks }.distinctBy { it.id }
        if (merged.size < 24 || mixes.size < 3) {
            throw CatalogException(
                "The service answered, but with too little usable data " +
                    "(${merged.size} tracks from ${mixes.size}/${SEEDS.size} mixes)."
            )
        }
        val catalog = Catalog(merged, mixes, System.currentTimeMillis())
        return catalog
    }

    private fun fetchResults(query: String): List<Track> {
        val body = httpGet("$BASE/api/search/songs?query=${URLEncoder.encode(query, "UTF-8")}")
        return parseSongs(body)
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
            setRequestProperty("User-Agent", "AUD-Android/1.0 (+https://aud29.thangamprabhu1.workers.dev)")
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
            Catalog(tracks, mixes, ts)
        }.getOrNull()
    }
}
