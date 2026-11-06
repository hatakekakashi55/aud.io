/**
 * BeatBox adapter — talks to the JioSaavn-compatible worker at
 * https://aud29.thangamprabhu1.workers.dev ("BeatBox Worker").
 *
 * Endpoints used:
 *   GET /api/search/songs?query=<q>   rich track objects (duration, downloadUrl, album…)
 *
 * The worker has no "get everything" route, so the catalog is hydrated from a set of
 * curated seed queries (each result set doubles as a live playlist). Results are cached
 * in sessionStorage — fresh enough, without hammering the worker on every reload.
 */
import { config } from '../config'
import type { Album, Artist, CatalogData, Playlist, Song } from '../types'

interface SaavnImage {
  quality: string
  link: string
}
interface SaavnDownload {
  quality: string
  url: string
}
interface SaavnSong {
  id: string
  name?: string
  title?: string
  duration?: number | string
  year?: string | number
  language?: string
  image?: SaavnImage[]
  primaryArtists?: string
  album?: string
  downloadUrl?: SaavnDownload[]
  url?: string
}

export interface StreamMap {
  '320'?: string
  '160'?: string
  '96'?: string
  '48'?: string
}
export type LiveSong = Song & {
  streams?: StreamMap
  saavnUrl?: string
  artistName: string
  albumName: string
  albumCover: string | null
}

const CACHE_KEY = 'aud-beatbox-catalog-v1'
const CACHE_TTL_MS = 15 * 60 * 1000

export interface SeedBucket {
  slug: string
  title: string
  query: string
  from: string
  to: string
  icon: string
  description: string
}

export const SEED_BUCKETS: SeedBucket[] = [
  { slug: 'tamil-love', title: 'Tamil Love', query: 'tamil love songs', from: 'from-pink-600', to: 'to-rose-900', icon: 'Heart', description: 'Live from BeatBox · romantic Tamil melodies.' },
  { slug: 'anirudh', title: 'Anirudh', query: 'anirudh ravichander', from: 'from-orange-500', to: 'to-red-800', icon: 'Flame', description: 'Live from BeatBox · the maestro of mass beats.' },
  { slug: 'ar-rahman', title: 'A.R. Rahman', query: 'a r rahman', from: 'from-amber-500', to: 'to-yellow-900', icon: 'Star', description: 'Live from BeatBox · legendary melodies.' },
  { slug: 'vijay-hits', title: 'Vijay Hits', query: 'vijay songs tamil', from: 'from-red-600', to: 'to-rose-900', icon: 'Zap', description: 'Live from BeatBox · Thalapathy chartbusters.' },
  { slug: 'sad-tamil', title: 'Breakup & Sad', query: 'sad songs tamil', from: 'from-sky-700', to: 'to-slate-900', icon: 'CloudRain', description: 'Live from BeatBox · feel-the-tear favourites.' },
  { slug: 'bollywood', title: 'Bollywood Hits', query: 'bollywood hits', from: 'from-fuchsia-600', to: 'to-purple-900', icon: 'Sparkles', description: 'Live from BeatBox · all-India anthems.' },
  { slug: 'tamil-melody', title: 'Tamil Melody', query: 'tamil melody', from: 'from-emerald-600', to: 'to-teal-900', icon: 'Music2', description: 'Live from BeatBox · timeless melodic gems.' },
  { slug: 'kuthu', title: 'Kuthu Anthems', query: 'kuthu tamil', from: 'from-orange-600', to: 'to-amber-900', icon: 'Drum', description: 'Live from BeatBox · full-throttle kuthu.' },
  { slug: 'ilaiyaraaja', title: 'Ilaiyaraaja', query: 'ilaiyaraaja hits', from: 'from-lime-600', to: 'to-green-950', icon: 'Guitar', description: 'Live from BeatBox · the Maestro, pure bliss.' },
  { slug: 'english', title: 'English Hits', query: 'english hits 2024', from: 'from-violet-600', to: 'to-indigo-950', icon: 'Radio', description: 'Live from BeatBox · the biggest English tracks.' },
]

/* ── helpers ─────────────────────────────────────────────── */

const slugify = (value: string) =>
  value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '').slice(0, 48) || 'x'

const pickImage = (images: SaavnImage[] | undefined, preferred = '500x500'): string | null => {
  if (!images || images.length === 0) return null
  return (
    images.find((img) => img.quality === preferred)?.link ??
    images.find((img) => img.quality === '150x150')?.link ??
    images[images.length - 1]?.link ??
    null
  )
}

const pickStreams = (urls: SaavnDownload[] | undefined): StreamMap => {
  const map: StreamMap = {}
  for (const entry of urls ?? []) {
    const kbps = String(entry.quality).replace(/[^0-9]/g, '')
    if (kbps === '320' || kbps === '160' || kbps === '96' || kbps === '48') {
      map[kbps] = entry.url
    }
  }
  return map
}

const bestStream = (map: StreamMap): string | null => map['320'] ?? map['160'] ?? map['96'] ?? map['48'] ?? null

const titleCase = (value: string) => value.charAt(0).toUpperCase() + value.slice(1).toLowerCase()

/** Decode the occasional HTML entity the worker returns inside names. */
const clean = (value: string) =>
  String(value)
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&#039;/g, "'")
    .replace(/&#x27;/g, "'")
    .trim()

const firstArtist = (primaryArtists: string | undefined): string =>
  clean((primaryArtists ?? '').split(',').map((a) => a.trim()).filter(Boolean)[0] ?? 'Unknown Artist') || 'Unknown Artist'

export function mapSaavnSong(raw: SaavnSong): LiveSong | null {
  const streams = pickStreams(raw.downloadUrl)
  const audioUrl = bestStream(streams)
  if (!raw.id || !audioUrl) return null

  const name = clean(raw.name ?? raw.title ?? 'Untitled') || 'Untitled'
  const artist = firstArtist(raw.primaryArtists)
  const year = Number.parseInt(String(raw.year ?? ''), 10)
  const language = raw.language ? titleCase(clean(raw.language)) : 'Music'
  const albumName = raw.album ? clean(String(raw.album)) : 'Singles'
  const cover = pickImage(raw.image, '500x500')

  return {
    id: `ls_${raw.id}`,
    title: name,
    artistId: `la_${slugify(artist)}`,
    albumId: `lm_${slugify(albumName)}`,
    duration: Number(raw.duration) || 0,
    audioUrl,
    plays: 0,
    explicit: false,
    genre: language,
    releaseDate: Number.isFinite(year) ? `${year}-01-01` : new Date().toISOString().slice(0, 10),
    streams,
    saavnUrl: raw.url ?? undefined,
    artistName: artist,
    albumName,
    albumCover: cover,
  }
}

async function fetchBucket(bucket: SeedBucket, signal?: AbortSignal): Promise<{ bucket: SeedBucket; songs: LiveSong[] }> {
  const url = `${config.apiUrl}/api/search/songs?query=${encodeURIComponent(bucket.query)}`
  const response = await fetch(url, { headers: { Accept: 'application/json' }, signal })
  if (!response.ok) throw new Error(`BeatBox /api/search/songs → HTTP ${response.status}`)
  const body = (await response.json()) as { data?: { results?: SaavnSong[] } }
  const results = body?.data?.results
  if (!Array.isArray(results)) throw new Error('BeatBox returned an unexpected payload (no data.results)')
  return { bucket, songs: results.map(mapSaavnSong).filter((s): s is LiveSong => s !== null) }
}

/** Assemble the full catalog: songs deduped, artists/albums/playlists synthesized. */
export function buildCatalog(bucketResults: { bucket: SeedBucket; songs: LiveSong[] }[]): CatalogData & { songs: LiveSong[] } {
  const songMap = new Map<string, LiveSong>()
  const bucketSongIds = new Map<string, string[]>()
  for (const { bucket, songs } of bucketResults) {
    const ids: string[] = []
    for (const song of songs) {
      if (!songMap.has(song.id)) songMap.set(song.id, song)
      ids.push(song.id)
    }
    bucketSongIds.set(bucket.slug, [...new Set(ids)])
  }
  const songs = [...songMap.values()]

  // Artists — display name taken straight from the credits, not the slug.
  const artistMeta = new Map<string, { name: string; lang: string; count: number }>()
  for (const song of songs) {
    const meta = artistMeta.get(song.artistId)
    if (meta) meta.count += 1
    else artistMeta.set(song.artistId, { name: song.artistName, lang: song.genre, count: 1 })
  }
  const artists: Artist[] = [...artistMeta.entries()]
    .map(([id, meta]) => ({
      id,
      name: meta.name,
      image: null,
      genres: [meta.lang],
      monthlyListeners: 0,
      followers: 0,
      bio: `${meta.name} — streamed live from JioSaavn through the BeatBox API. ${meta.count} track${meta.count === 1 ? '' : 's'} in this catalog.`,
      verified: false,
    }))
    .sort((a, b) => b.monthlyListeners - a.monthlyListeners || a.name.localeCompare(b.name))

  // Albums — one record per unique album name, cover from any contained track.
  const albumMeta = new Map<string, { title: string; cover: string | null; year: number; artistId: string }>()
  for (const song of songs) {
    const entry = albumMeta.get(song.albumId)
    if (entry) {
      if (!entry.cover && song.albumCover) entry.cover = song.albumCover
      continue
    }
    albumMeta.set(song.albumId, {
      title: song.albumName,
      cover: song.albumCover,
      year: Number.parseInt(song.releaseDate.slice(0, 4), 10) || new Date().getFullYear(),
      artistId: song.artistId,
    })
  }
  const albums: Album[] = [...albumMeta.entries()].map(([id, a]) => ({ id, ...a, type: 'album' as const }))

  // Playlists — every seed bucket becomes a live mix.
  const playlists: Playlist[] = SEED_BUCKETS
    .map((bucket): Playlist | null => {
      const ids = bucketSongIds.get(bucket.slug) ?? []
      if (ids.length === 0) return null
      return {
        id: `lp_${bucket.slug}`,
        title: bucket.title,
        description: bucket.description,
        curator: 'BeatBox Live',
        followers: 0,
        songIds: ids,
        art: { type: 'gradient', from: bucket.from, to: bucket.to, icon: bucket.icon },
      }
    })
    .filter((p): p is Playlist => p !== null)

  return { songs, artists, albums, playlists }
}

export interface BeatboxResult {
  data: CatalogData & { songs: LiveSong[] }
  fromCache: boolean
}

/** Load (or return cached) live catalog from the BeatBox worker. */
export async function fetchBeatboxCatalog(options?: { forceRefresh?: boolean; signal?: AbortSignal }): Promise<BeatboxResult> {
  if (!options?.forceRefresh) {
    try {
      const raw = sessionStorage.getItem(CACHE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw) as { ts: number; data: CatalogData & { songs: LiveSong[] } }
        if (Date.now() - parsed.ts < CACHE_TTL_MS && parsed.data?.songs?.length) {
          return { data: parsed.data, fromCache: true }
        }
      }
    } catch {
      // Corrupt cache — fall through to network.
    }
  }

  const settled = await Promise.allSettled(SEED_BUCKETS.map((bucket) => fetchBucket(bucket, options?.signal)))
  const successes = settled
    .filter((r): r is PromiseFulfilledResult<{ bucket: SeedBucket; songs: LiveSong[] }> => r.status === 'fulfilled')
    .map((r) => r.value)
    .filter(({ songs }) => songs.length > 0)

  if (successes.length < 3) {
    const firstError = settled.find((r): r is PromiseRejectedResult => r.status === 'rejected')?.reason
    throw firstError instanceof Error ? firstError : new Error('BeatBox API did not return enough data')
  }

  const data = buildCatalog(successes)
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch {
    // Quota exceeded — non-fatal.
  }
  return { data, fromCache: false }
}

export function clearBeatboxCache() {
  try {
    sessionStorage.removeItem(CACHE_KEY)
  } catch {
    /* ignore */
  }
}

/** Live search — used by the Search page to merge fresh tracks into the catalog. */
export async function searchBeatbox(query: string, signal?: AbortSignal): Promise<{ songs: LiveSong[]; artists: Artist[]; albums: Album[] }> {
  const trimmed = query.trim()
  if (trimmed.length < 3) return { songs: [], artists: [], albums: [] }
  const bucket: SeedBucket = { slug: 'search', title: 'Search', query: trimmed, from: '', to: '', icon: '', description: '' }
  const { songs } = await fetchBucket(bucket, signal)

  const artistMeta = new Map<string, { name: string; lang: string }>()
  for (const song of songs) {
    if (!artistMeta.has(song.artistId)) artistMeta.set(song.artistId, { name: song.artistName, lang: song.genre })
  }
  const artists: Artist[] = [...artistMeta.entries()].map(([id, meta]) => ({
    id,
    name: meta.name,
    image: null,
    genres: [meta.lang],
    monthlyListeners: 0,
    followers: 0,
    bio: `${meta.name} — streamed live from JioSaavn.`,
    verified: false,
  }))
  const albumMeta = new Map<string, Album>()
  for (const song of songs) {
    if (!albumMeta.has(song.albumId)) {
      albumMeta.set(song.albumId, {
        id: song.albumId,
        title: song.albumName,
        artistId: song.artistId,
        year: Number.parseInt(song.releaseDate.slice(0, 4), 10) || new Date().getFullYear(),
        cover: song.albumCover,
        type: 'album',
      })
    }
  }
  return { songs, artists, albums: [...albumMeta.values()] }
}
