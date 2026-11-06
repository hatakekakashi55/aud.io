import { create } from 'zustand'
import { getCatalog, getDemoCatalog, type CatalogSource } from '../services/musicService'
import { clearBeatboxCache } from '../services/liveCatalog'
import type { Album, Artist, Playlist, Song } from '../types'

export type CatalogStatus = 'idle' | 'loading' | 'ready' | 'error'

interface CatalogState {
  songs: Song[]
  artists: Artist[]
  albums: Album[]
  playlists: Playlist[]
  status: CatalogStatus
  source: CatalogSource
  error: string | null
  load: () => Promise<void>
  useDemoFallback: () => void
  refreshLiveCatalog: () => Promise<void>
  /** Merge freshly fetched entities (e.g. live search results) into the catalog. */
  mergeIntoCatalog: (partial: { songs?: Song[]; artists?: Artist[]; albums?: Album[] }) => void
}

let inFlight: Promise<void> | null = null

export const useCatalogStore = create<CatalogState>((set, get) => ({
  songs: [],
  artists: [],
  albums: [],
  playlists: [],
  status: 'idle',
  source: 'demo',
  error: null,

  load: async () => {
    if (get().status === 'loading') return inFlight ?? Promise.resolve()
    if (get().status === 'ready') return
    set({ status: 'loading', error: null })
    inFlight = (async () => {
      try {
        const { data, source } = await getCatalog()
        set({ ...data, status: 'ready', source, error: null })
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load the music service'
        set({ status: 'error', error: message })
      } finally {
        inFlight = null
      }
    })()
    return inFlight
  },

  /** Escape hatch when the configured API is down — run on the bundled demo catalog. */
  useDemoFallback: () => {
    const { data, source } = getDemoCatalog()
    set({ ...data, status: 'ready', source, error: null })
  },

  /** Clear the BeatBox session cache and refetch from the network. */
  refreshLiveCatalog: async () => {
    clearBeatboxCache()
    set({ status: 'idle' })
    await get().load()
  },

  mergeIntoCatalog: (partial) => {
    set((state) => {
      const dedupe = <T extends { id: string }>(fresh: T[] | undefined, existing: T[]): T[] => {
        if (!fresh || fresh.length === 0) return existing
        const freshIds = new Set(fresh.map((item) => item.id))
        return [...fresh, ...existing.filter((item) => !freshIds.has(item.id))]
      }
      return {
        songs: dedupe(partial.songs, state.songs),
        artists: dedupe(partial.artists, state.artists),
        albums: dedupe(partial.albums, state.albums),
      }
    })
  },
}))
