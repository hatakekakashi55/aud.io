package com.aud.android.data

/**
 * Language normalisation for the live feed.
 *
 * The upstream service reports a free-form `language` per track ("tamil",
 * "Tamil", "hindi", "telugu", "english"…). Everything the app ranks or labels
 * goes through here, so "Tamil" is one bucket no matter how it was spelled.
 */
object Languages {

    /** Aliases → canonical display names. Lowercase keys. */
    private val ALIASES = mapOf(
        "tamil" to "Tamil",
        "tamizh" to "Tamil",
        "tam" to "Tamil",
        "hindi" to "Hindi",
        "hin" to "Hindi",
        "bollywood" to "Hindi",
        "telugu" to "Telugu",
        "tel" to "Telugu",
        "malayalam" to "Malayalam",
        "mal" to "Malayalam",
        "kannada" to "Kannada",
        "kan" to "Kannada",
        "english" to "English",
        "eng" to "English",
        "punjabi" to "Punjabi",
        "marathi" to "Marathi",
        "bengali" to "Bengali",
        "gujarati" to "Gujarati",
        "urdu" to "Urdu",
        "korean" to "Korean",
        "japanese" to "Japanese",
        "spanish" to "Spanish",
        "instrumental" to "Instrumental",
        "music" to "Music",
    )

    fun canonical(raw: String): String {
        val key = raw.trim().lowercase()
        if (key.isEmpty()) return ""
        ALIASES[key]?.let { return it }
        // "tamil (dubbed)" → "Tamil"
        val head = key.substringBefore('(').substringBefore('-').trim()
        ALIASES[head]?.let { return it }
        return raw.trim().replaceFirstChar { it.titlecase() }
    }

    fun isTamil(raw: String): Boolean {
        val key = raw.trim().lowercase()
        if (key.isEmpty()) return false
        return key.contains("tamil") || key.contains("tamizh") || key == "tam"
    }

    /**
     * Last-resort inference when the service omits `language` entirely:
     * classic Tamil film-music credits and Tamil script in the title.
     */
    private val TAMIL_HINTS = listOf(
        "anirudh", "rahman", "ilaiyaraaja", "ilaiyaraja", "yuvan", "harris jayaraj",
        "gv prakash", "santhosh narayanan", "sidu", "vijay antony", "d imman",
        "imman", "siva karthikeyan", "thalapathy", "ajith", "kamal", "rajinikanth",
        "sivakarthikeyan", "dhanush", "vijay", "suriya", "keerthi", "nayanthara",
        "tamil", "kuthu", "kollywood", "chennai",
    )

    fun inferTamil(title: String, artist: String, album: String): Boolean {
        val haystack = "$title $artist $album".lowercase()
        if (haystack.any { it in '\u0B80'..'\u0BFF' }) return true // Tamil script
        return TAMIL_HINTS.any { haystack.contains(it) }
    }
}
