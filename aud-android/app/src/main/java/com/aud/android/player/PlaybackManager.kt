package com.aud.android.player

import android.media.MediaPlayer
import com.aud.android.data.Store
import com.aud.android.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

data class PlaybackState(
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val positionSec: Int = 0,
    val durationSec: Int = 0,
    val shuffle: Boolean = false,
    val repeatOne: Boolean = false,
) {
    val current: Track? get() = queue.getOrNull(index)
    val hasTrack: Boolean get() = current != null
}

/**
 * Playback engine — android.media.MediaPlayer + StateFlow.
 * Recently-played ids are pushed into [Store] on every track start.
 */
class PlaybackManager(private val store: Store) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerRef = AtomicReference<MediaPlayer?>(null)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var ticker: Job? = null
    private var consecutiveLoadFailures = 0

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val clamped = startIndex.coerceIn(0, tracks.lastIndex)
        _state.update { it.copy(queue = tracks.toList(), index = clamped, positionSec = 0) }
        loadAndPlay(clamped)
    }

    fun toggle() {
        val player = playerRef.get() ?: return
        if (_state.value.isPlaying) {
            runCatching { player.pause() }
            _state.update { it.copy(isPlaying = false) }
            stopTicker()
        } else {
            runCatching { player.start() }
            _state.update { it.copy(isPlaying = true) }
            startTicker()
        }
    }

    fun next() {
        noteManualSkip()
        advance(auto = false)
    }

    fun previous() {
        val s = _state.value
        if (s.positionSec > 3) {
            seekTo(0)
            return
        }
        noteManualSkip()
        if (s.queue.isEmpty()) return
        val target = if (s.index <= 0) s.queue.lastIndex else s.index - 1
        _state.update { it.copy(index = target, positionSec = 0) }
        loadAndPlay(target)
    }

    fun seekTo(seconds: Int) {
        val clamped = seconds.coerceAtLeast(0)
        playerRef.get()?.let { runCatching { it.seekTo(clamped * 1000) } }
        _state.update { it.copy(positionSec = clamped) }
    }

    fun toggleShuffle() = _state.update { it.copy(shuffle = !it.shuffle) }
    fun toggleRepeat() = _state.update { it.copy(repeatOne = !it.repeatOne) }

    fun release() {
        stopTicker()
        playerRef.getAndSet(null)?.let { runCatching { it.release() } }
        _state.update { it.copy(isPlaying = false, isPreparing = false) }
    }

    /* ── internals ─────────────────────────────────────────────── */

    /**
     * A manual "next" in the first 40% of a track (and under 90 s) means the
     * listener rejected it — that feeds the For You demotion. Auto-advance on
     * completion, or skipping a dead stream, is not a skip.
     */
    private fun noteManualSkip() {
        val state = _state.value
        val track = state.current ?: return
        val duration = state.durationSec.coerceAtLeast(track.durationSec)
        if (duration <= 0) return
        val earlyEnough = state.positionSec < 90 && state.positionSec < (duration * 0.4f).toInt()
        if (earlyEnough) store.recordSkip(track)
    }

    private fun advance(auto: Boolean) {
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (auto && s.repeatOne) {
            seekTo(0)
            runCatching { playerRef.get()?.start() }
            _state.update { it.copy(isPlaying = true) }
            startTicker()
            return
        }
        val target = when {
            s.shuffle && s.queue.size > 1 -> {
                var pick = s.index
                while (pick == s.index) pick = (0 until s.queue.size).random()
                pick
            }
            else -> (s.index + 1).mod(s.queue.size)
        }
        _state.update { it.copy(index = target, positionSec = 0) }
        loadAndPlay(target)
    }

    private fun loadAndPlay(index: Int) {
        val track = _state.value.queue.getOrNull(index) ?: return
        stopTicker()
        playerRef.getAndSet(null)?.let { runCatching { it.release() } }
        store.pushRecent(track.id)
        store.recordPlay(track)

        _state.update {
            it.copy(isPreparing = true, isPlaying = false, positionSec = 0, durationSec = track.durationSec)
        }

        val player = try {
            MediaPlayer().apply {
            setDataSource(track.audioUrl)
            setOnPreparedListener { mp ->
                consecutiveLoadFailures = 0
                mp.start()
                _state.update {
                    it.copy(
                        isPreparing = false,
                        isPlaying = true,
                        durationSec = if (mp.duration > 0) mp.duration / 1000 else track.durationSec,
                    )
                }
                startTicker()
            }
            setOnCompletionListener { advance(auto = true) }
            setOnErrorListener { _, what, extra ->
                // Skip the broken stream and move on — errorCode surfaced in the logcat tag AUD.
                android.util.Log.w("AUD", "MediaPlayer error what=$what extra=$extra url=${track.audioUrl}")
                _state.update { it.copy(isPreparing = false, isPlaying = false) }
                consecutiveLoadFailures += 1
                if (consecutiveLoadFailures <= _state.value.queue.size) {
                    advance(auto = true)
                }
                true
            }
            prepareAsync()
            }
        } catch (e: Exception) {
            android.util.Log.w("AUD", "load failed for ${track.audioUrl}: ${e.message}")
            _state.update { it.copy(isPreparing = false, isPlaying = false) }
            // Auto-skip dead streams, but never loop the whole queue forever.
            consecutiveLoadFailures += 1
            if (consecutiveLoadFailures <= _state.value.queue.size) {
                advance(auto = true)
            }
            return
        }
        playerRef.set(player)
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (true) {
                val player = playerRef.get() ?: break
                _state.update { it.copy(positionSec = (player.currentPosition / 1000).coerceAtLeast(0)) }
                delay(700)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }
}
