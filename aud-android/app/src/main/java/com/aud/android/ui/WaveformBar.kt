package com.aud.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The signature AUD seek bar: a deterministic "tape waveform" —
 * 64 brass levels generated from the track id, drawn as vertical ticks.
 * Tap or drag to scrub. Deliberately not a stock Material slider.
 */
@Composable
fun WaveformBar(
    progress: Float,
    seed: String,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bars = remember(seed) {
        val rnd = Random(seed.hashCode())
        List(64) { i ->
            // A loose envelope so the shape reads like music, not noise.
            val envelope = 0.55 + 0.45 * sin((i / 64.0) * PI * 5.5 + rnd.nextDouble() * 0.6)
            val jitter = 0.35 + rnd.nextDouble() * 0.65
            (envelope * 0.75 + jitter * 0.35).coerceIn(0.12, 1.0).toFloat()
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(seed) {
                detectTapGestures { offset ->
                    onSeekFraction((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(seed) {
                detectHorizontalDragGestures { change, _ ->
                    onSeekFraction((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val count = bars.size
        val slot = size.width / count.toFloat()
        val barWidth = slot * 0.56f
        val midY = size.height / 2f
        val progressX = progress.coerceIn(0f, 1f) * size.width

        for (i in 0 until count) {
            val x = slot * i.toFloat() + slot / 2f
            val h = bars[i] * size.height * 0.88f
            val played = x <= progressX
            drawLine(
                color = if (played) Ink.Brass else Ink.Line,
                start = Offset(x, midY - h / 2f),
                end = Offset(x, midY + h / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
