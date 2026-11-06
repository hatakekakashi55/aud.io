/**
 * Audio engine — owns the single HTMLAudioElement for the whole app.
 * Bridges the player store to actual audio playback:
 *   currentSongId → load + play     isPlaying → play/pause
 *   seekRequest   → audio.currentTime
 *   volume/muted  → audio element
 *   timeupdate/metadata/ended → back into the store
 */
import { usePlayerStore } from '../store/playerStore'
import { useCatalogStore } from '../store/catalogStore'
import { useToastStore } from '../store/toastStore'
import { useUserStore } from '../store/userStore'

export const audio = new Audio()
audio.preload = 'metadata'

let initialized = false
let lastSongId: string | null = null
let lastSeekNonce = 0

export function initAudioEngine() {
  if (initialized) return
  initialized = true

  audio.addEventListener('timeupdate', () => {
    usePlayerStore.getState().setPosition(audio.currentTime)
  })
  audio.addEventListener('loadedmetadata', () => {
    if (Number.isFinite(audio.duration)) usePlayerStore.getState().setDuration(audio.duration)
  })
  audio.addEventListener('ended', () => {
    usePlayerStore.getState().next(true)
  })
  audio.addEventListener('error', () => {
    // A dead stream must not wedge the queue — or loop forever on repeat-one.
    const state = usePlayerStore.getState()
    if (!state.currentSongId) return
    if (state.repeat === 'one') usePlayerStore.setState({ repeat: 'off' })
    useToastStore.getState().push({ icon: 'info', title: 'Couldn’t play that track', description: 'Skipping ahead' })
    state.next(true)
  })

  // Reflect persisted volume immediately.
  const s = usePlayerStore.getState()
  audio.volume = s.volume
  audio.muted = s.muted

  usePlayerStore.subscribe((state) => {
    // Track change → load source and play.
    if (state.currentSongId !== lastSongId) {
      lastSongId = state.currentSongId
      lastSeekNonce = state.seekRequest?.nonce ?? lastSeekNonce
      const songId = state.currentSongId
      const song = songId ? useCatalogStore.getState().songs.find((s) => s.id === songId) : undefined

      if (song) {
        // Seed with catalog metadata until the stream reports the real duration.
        usePlayerStore.setState({ duration: song.duration, position: 0 })
        audio.src = resolveStreamUrl(song)
        audio.currentTime = 0
        if (state.isPlaying) {
          void audio.play().catch(() => usePlayerStore.setState({ isPlaying: false }))
        }
      } else {
        audio.removeAttribute('src')
      }
      return
    }

    // Play/pause.
    if (state.isPlaying && audio.paused) {
      void audio.play().catch(() => usePlayerStore.setState({ isPlaying: false }))
    } else if (!state.isPlaying && !audio.paused) {
      audio.pause()
    }

    // Explicit seeks.
    const nonce = state.seekRequest?.nonce ?? 0
    if (nonce !== lastSeekNonce && state.seekRequest) {
      lastSeekNonce = nonce
      if (Number.isFinite(audio.duration) || audio.src) {
        audio.currentTime = state.seekRequest.seconds
      }
    }

    // Volume / mute.
    if (audio.volume !== state.volume) audio.volume = state.volume
    if (audio.muted !== state.muted) audio.muted = state.muted
  })
}

/** Prefer the user's stream quality; fall back gracefully to whatever exists. */
function resolveStreamUrl(song: { audioUrl: string } & { streams?: Record<string, string | undefined> }): string {
  const quality = useUserStore.getState().streamQuality
  const streams = (song as { streams?: Record<string, string> }).streams
  if (streams) {
    return streams[quality] ?? streams['160'] ?? streams['96'] ?? streams['48'] ?? streams['320'] ?? song.audioUrl
  }
  return song.audioUrl
}

/** Direct DOM seek — kept for scrubber UIs. */
export function seekAudio(seconds: number) {
  audio.currentTime = Math.max(0, seconds)
}
