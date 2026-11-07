package com.aud.android.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Everything in "Settings → Look & feel": the accent, text size and a
 * high-contrast text switch. Stored in SharedPreferences so it survives restarts.
 */
@Immutable
data class Appearance(
    val accent: Accent = Accent.SeaGlass,
    /** 0.9 – 1.3, applied on top of the system font scale. */
    val textScale: Float = 1f,
    val highContrast: Boolean = false,
) {
    val textScaleLabel: String
        get() = when {
            textScale <= 0.92f -> "Compact"
            textScale <= 1.02f -> "Default"
            textScale <= 1.14f -> "Large"
            else -> "Extra large"
        }

    companion object {
        fun load(context: Context): Appearance {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Appearance(
                accent = Accent.fromKey(prefs.getString(KEY_ACCENT, null)),
                textScale = prefs.getFloat(KEY_TEXT_SCALE, 1f).coerceIn(0.9f, 1.3f),
                highContrast = prefs.getBoolean(KEY_CONTRAST, false),
            )
        }

        fun save(context: Context, appearance: Appearance) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCENT, appearance.accent.key)
                .putFloat(KEY_TEXT_SCALE, appearance.textScale)
                .putBoolean(KEY_CONTRAST, appearance.highContrast)
                .apply()
        }

        private const val PREFS = "aud_appearance"
        private const val KEY_ACCENT = "accent"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_CONTRAST = "high_contrast"
    }
}

/**
 * Accent options, chosen for comfort rather than punch: every one is desaturated,
 * mid-luminance (so it never blooms on a dark page) and distinguishable from the
 * amber status colour.
 */
enum class Accent(
    val key: String,
    val label: String,
    val color: Color,
    val deep: Color,
    val on: Color,
) {
    SeaGlass("seaglass", "Sea glass", Color(0xFF6FC2AE), Color(0xFF3F8A7C), Color(0xFF0E2320)),
    Sage("sage", "Sage", Color(0xFFA3C08A), Color(0xFF6C8A55), Color(0xFF15200E)),
    Periwinkle("periwinkle", "Periwinkle", Color(0xFF9DB4E0), Color(0xFF5F7AAE), Color(0xFF101A2A)),
    Sand("sand", "Warm sand", Color(0xFFD3B98F), Color(0xFF9C825B), Color(0xFF241A0C)),
    Rose("rose", "Dusty rose", Color(0xFFD9A0A6), Color(0xFFA26B72), Color(0xFF28100F));

    companion object {
        fun fromKey(key: String?): Accent = entries.firstOrNull { it.key == key } ?: SeaGlass
    }
}
