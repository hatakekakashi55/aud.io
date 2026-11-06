/**
 * Music service — single access point for all catalog data.
 *
 * When VITE_MUSIC_API_URL is set, AUD hydrates from the live service:
 *   1. "beatbox" adapter — the JioSaavn-compatible worker (default, auto-detected).
 *   2. "rest" adapter — plain GET /songs, /artists, /albums, /playlists (see README).
 *   Force one with VITE_CATALOG_MODE=beatbox|rest.
 *
 * When unset, the bundled demo catalog runs. Failures surface to the UI, which offers
 * retry or an explicit "continue with demo catalog" — never silent fake data.
 */
import { ALBUMS, ARTISTS, PLAYLISTS, SONGS } from '../data/demoData'
import { isApiConfigured } from '../config'
import { apiGet } from './apiClient'
import { fetchBeatboxCatalog } from './liveCatalog'
import type { Album, Artist, CatalogData, Playlist, Song } from '../types'

export type CatalogSource = 'api' | 'demo'

export interface CatalogResult {
  data: CatalogData
  source: CatalogSource
}

export type CatalogMode = 'auto' | 'beatbox' | 'rest'
export const catalogMode: CatalogMode = ((import.meta.env.VITE_CATALOG_MODE as string) || 'auto') as CatalogMode

async function fetchFromApi(signal?: AbortSignal): Promise<CatalogData> {
  const [songs, artists, albums, playlists] = await Promise.all([
    apiGet<unknown>('/songs', signal),
    apiGet<unknown>('/artists', signal),
    apiGet<unknown>('/albums', signal),
    apiGet<unknown>('/playlists', signal),
  ])
  return {
    songs: asArray<Song>(songs, 'songs'),
    artists: asArray<Artist>(artists, 'artists'),
    albums: asArray<Album>(albums, 'albums'),
    playlists: asArray<Playlist>(playlists, 'playlists'),
  }
}

function asArray<T>(value: unknown, name: string): T[] {
  if (!Array.isArray(value)) {
    throw new Error(`Invalid /${name} response — expected an array, got ${value === null ? 'null' : typeof value}`)
  }
  return value as T[]
}

function demoCatalog(): CatalogData {
  return {
    songs: SONGS.map((s) => ({ ...s })),
    artists: ARTISTS.map((a) => ({ ...a, genres: [...a.genres] })),
    albums: ALBUMS.map((a) => ({ ...a })),
    playlists: PLAYLISTS.map((p) => ({ ...p, songIds: [...p.songIds], art: { ...p.art } })),
  }
}

const errMessage = (err: unknown) => (err instanceof Error ? err.message : String(err))

/** Load the catalog. Live service when configured, otherwise demo data. */
export async function getCatalog(signal?: AbortSignal): Promise<CatalogResult> {
  if (!isApiConfigured()) {
    return { data: demoCatalog(), source: 'demo' }
  }

  if (catalogMode === 'rest') {
    const data = await fetchFromApi(signal)
    return { data, source: 'api' }
  }

  // BeatBox (default) — auto mode falls back to the REST contract if the worker doesn't answer.
  try {
    const { data } = await fetchBeatboxCatalog({ signal })
    return { data, source: 'api' }
  } catch (beatboxErr) {
    if (catalogMode === 'beatbox') throw beatboxErr
    try {
      const data = await fetchFromApi(signal)
      return { data, source: 'api' }
    } catch (restErr) {
      throw new Error(`BeatBox: ${errMessage(beatboxErr)} · REST contract: ${errMessage(restErr)}`)
    }
  }
}

/** Explicitly load the bundled demo catalog (in-app fallback). */
export function getDemoCatalog(): CatalogResult {
  return { data: demoCatalog(), source: 'demo' }
}
