import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { RepeatMode } from '../types'

interface PlayerState {
  queue: string[]
  queueIndex: number
  currentSongId: string | null
  isPlaying: boolean
  position: number
  duration: number
  volume: number
  muted: boolean
  shuffle: boolean
  repeat: RepeatMode
  expanded: boolean
  likedIds: string[]
  recentIds: string[]
  /** Nonce-based seek requests consumed by the audio engine. */
  seekRequest: { seconds: number; nonce: number } | null

  playQueue: (songIds: string[], startIndex: number) => void
  togglePlay: () => void
  next: (auto?: boolean) => void
  prev: () => void
  seekTo: (seconds: number) => void
  setPosition: (seconds: number) => void
  setDuration: (seconds: number) => void
  setVolume: (volume: number) => void
  toggleMute: () => void
  toggleShuffle: () => void
  cycleRepeat: () => void
  toggleLike: (songId: string) => void
  isLiked: (songId: string) => boolean
  addToQueue: (songId: string) => void
  playNext: (songId: string) => void
  removeFromQueue: (index: number) => void
  clearUpcoming: () => void
  moveQueueItem: (from: number, to: number) => void
  setExpanded: (expanded: boolean) => void
}

function pushRecent(recent: string[], id: string): string[] {
  return [id, ...recent.filter((r) => r !== id)].slice(0, 24)
}

export const usePlayerStore = create<PlayerState>()(
  persist(
    (set, get) => ({
      queue: [],
      queueIndex: -1,
      currentSongId: null,
      isPlaying: false,
      position: 0,
      duration: 0,
      volume: 0.8,
      muted: false,
      shuffle: false,
      repeat: 'off',
      expanded: false,
      likedIds: [],
      recentIds: [],
      seekRequest: null,

      playQueue: (songIds, startIndex) => {
        const idx = Math.min(Math.max(startIndex, 0), songIds.length - 1)
        const currentSongId = songIds[idx] ?? null
        set({
          queue: [...songIds],
          queueIndex: idx,
          currentSongId,
          isPlaying: currentSongId !== null,
          position: 0,
          recentIds: currentSongId ? pushRecent(get().recentIds, currentSongId) : get().recentIds,
        })
      },

      togglePlay: () => {
        const { currentSongId, isPlaying } = get()
        if (!currentSongId) return
        set({ isPlaying: !isPlaying })
      },

      next: (auto = false) => {
        const { queue, queueIndex, shuffle, repeat, currentSongId } = get()
        if (queue.length === 0 || queueIndex < 0) return

        if (auto && repeat === 'one' && currentSongId) {
          set((s) => ({ seekRequest: { seconds: 0, nonce: (s.seekRequest?.nonce ?? 0) + 1 }, position: 0 }))
          return
        }

        let nextIndex: number
        if (shuffle && queue.length > 1) {
          do {
            nextIndex = Math.floor(Math.random() * queue.length)
          } while (nextIndex === queueIndex)
        } else {
          nextIndex = queueIndex + 1
        }

        if (nextIndex >= queue.length) {
          if (repeat === 'all') {
            nextIndex = 0
          } else if (auto) {
            // End of queue with repeat off — stop gracefully.
            set({ isPlaying: false, position: 0 })
            return
          } else {
            nextIndex = 0 // manual skip wraps around
          }
        }

        const songId = queue[nextIndex]
        set({
          queueIndex: nextIndex,
          currentSongId: songId,
          isPlaying: true,
          position: 0,
          recentIds: pushRecent(get().recentIds, songId),
        })
      },

      prev: () => {
        const { queue, queueIndex, position } = get()
        if (queue.length === 0 || queueIndex < 0) return
        // Restart current track when we're a few seconds in — standard player behaviour.
        if (position > 3) {
          set((s) => ({ seekRequest: { seconds: 0, nonce: (s.seekRequest?.nonce ?? 0) + 1 }, position: 0 }))
          return
        }
        const prevIndex = queueIndex <= 0 ? queue.length - 1 : queueIndex - 1
        const songId = queue[prevIndex]
        set({
          queueIndex: prevIndex,
          currentSongId: songId,
          isPlaying: true,
          position: 0,
          recentIds: pushRecent(get().recentIds, songId),
        })
      },

      seekTo: (seconds) =>
        set((s) => ({
          seekRequest: { seconds: Math.max(0, seconds), nonce: (s.seekRequest?.nonce ?? 0) + 1 },
          position: Math.max(0, seconds),
        })),

      setPosition: (seconds) => set({ position: seconds }),
      setDuration: (seconds) => set({ duration: seconds }),

      setVolume: (volume) => set({ volume: Math.min(1, Math.max(0, volume)), muted: volume === 0 ? get().muted : false }),
      toggleMute: () => set({ muted: !get().muted }),

      toggleShuffle: () => set({ shuffle: !get().shuffle }),

      cycleRepeat: () =>
        set({
          repeat: get().repeat === 'off' ? 'all' : get().repeat === 'all' ? 'one' : 'off',
        }),

      toggleLike: (songId) => {
        const liked = get().likedIds
        set({
          likedIds: liked.includes(songId) ? liked.filter((id) => id !== songId) : [songId, ...liked],
        })
      },
      isLiked: (songId) => get().likedIds.includes(songId),

      addToQueue: (songId) => set({ queue: [...get().queue, songId] }),

      playNext: (songId) => {
        const { queue, queueIndex } = get()
        if (queueIndex < 0) {
          set({ queue: [songId], queueIndex: 0, currentSongId: songId, isPlaying: true, position: 0 })
          return
        }
        const nextQueue = [...queue]
        nextQueue.splice(queueIndex + 1, 0, songId)
        set({ queue: nextQueue })
      },

      removeFromQueue: (index) => {
        const { queue, queueIndex } = get()
        if (index < 0 || index >= queue.length) return
        const nextQueue = queue.filter((_, i) => i !== index)
        let nextIndex = queueIndex
        if (index < queueIndex) nextIndex = queueIndex - 1
        else if (index === queueIndex) nextIndex = Math.min(queueIndex, nextQueue.length - 1)
        set({ queue: nextQueue, queueIndex: nextIndex })
      },

      clearUpcoming: () => {
        const { queue, queueIndex } = get()
        set({ queue: queue.slice(0, queueIndex + 1) })
      },

      moveQueueItem: (from, to) => {
        const { queue, queueIndex } = get()
        if (from === to || from < 0 || to < 0 || from >= queue.length || to >= queue.length) return
        const nextQueue = [...queue]
        const [moved] = nextQueue.splice(from, 1)
        nextQueue.splice(to, 0, moved)
        let nextIndex = queueIndex
        if (queueIndex === from) nextIndex = to
        else if (from < queueIndex && to >= queueIndex) nextIndex = queueIndex - 1
        else if (from > queueIndex && to <= queueIndex) nextIndex = queueIndex + 1
        set({ queue: nextQueue, queueIndex: nextIndex })
      },

      setExpanded: (expanded) => set({ expanded }),
    }),
    {
      name: 'aud-player',
      partialize: (state) => ({
        likedIds: state.likedIds,
        recentIds: state.recentIds,
        volume: state.volume,
        muted: state.muted,
        shuffle: state.shuffle,
        repeat: state.repeat,
      }),
    },
  ),
)
