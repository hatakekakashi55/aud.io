import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Car,
  CloudRain,
  Coffee,
  Drum,
  Flame,
  Guitar,
  Headphones,
  Play,
  Radio,
  Search as SearchIcon,
  Loader2,
  SearchX,
  Star,
  Sun,
  TrendingUp,
  X,
  Zap,
} from 'lucide-react'
import { GENRE_TILES } from '../data/demoData'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useCatalogStore } from '../store/catalogStore'
import { searchBeatbox } from '../services/liveCatalog'
import MediaCard from '../components/MediaCard'
import SongRow from '../components/SongRow'
import EmptyState from '../components/EmptyState'
import { ArtistAvatar, CoverImage, PlaylistArtwork } from '../components/artwork'
import type { GenreTile } from '../data/demoData'

const TILE_ICONS: Record<string, typeof Star> = {
  Star, Zap, Coffee, Guitar, Drum, Flame, Sun, CloudRain, Headphones, Car, Radio, TrendingUp,
}

/** Keywords used when a browse tile is tapped — all trace back to the demo catalog. */
const TILE_KEYWORDS: Record<string, string[]> = {
  'Bollywood Pop': ['bollywood', 'dance pop'],
  Synthwave: ['synthwave'],
  'Lo-Fi': ['lo-fi'],
  'Indie Rock': ['indie'],
  Carnatic: ['carnatic', 'melodic'],
  'Dance Pop': ['dance', 'bollywood'],
  'Acoustic Folk': ['acoustic', 'folk'],
  Monsoon: ['monsoon', 'mazhai', 'vaanam', 'barish', 'rainy'],
  Focus: ['lo-fi'],
  'Night Drive': ['synthwave', 'neon', 'ultraviolet'],
  Electronic: ['electronic', 'synthwave'],
  'Top Hits': ['*'],
}

export default function Search() {
  const navigate = useNavigate()
  const { songs, artists, albums, playlists, songById } = useCatalog()
  const playQueue = usePlayerStore((s) => s.playQueue)
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const isLive = useCatalogStore((s) => s.source === 'api')
  const mergeIntoCatalog = useCatalogStore((s) => s.mergeIntoCatalog)
  const [remote, setRemote] = useState<{ query: string; status: 'loading' | 'error' } | null>(null)

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  const trimmed = query.trim()

  // Live search — fresh tracks from BeatBox are merged into the catalog,
  // so everything downstream (rows, queue, play) just works.
  useEffect(() => {
    if (!isLive || trimmed.length < 3) return
    const timer = window.setTimeout(() => {
      setRemote({ query: trimmed, status: 'loading' })
      searchBeatbox(trimmed)
        .then(({ songs, artists, albums }) => {
          if (songs.length > 0) mergeIntoCatalog({ songs, artists, albums })
          setRemote(null)
        })
        .catch(() => setRemote({ query: trimmed, status: 'error' }))
    }, 420)
    return () => window.clearTimeout(timer)
  }, [trimmed, isLive, mergeIntoCatalog])

  // Derived live-search states — no synchronous setState inside the effect.
  const remoteActive = isLive && trimmed.length >= 3
  const remoteForQuery = remote?.query === trimmed ? remote : null
  const showSearching = remoteActive && remoteForQuery?.status === 'loading'
  const showError = remoteActive && remoteForQuery?.status === 'error'

  const activeKeywords = useMemo(() => {
    if (!trimmed) return null
    return TILE_KEYWORDS[trimmed] ?? [trimmed.toLowerCase()]
  }, [trimmed])

  const results = useMemo(() => {
    if (!activeKeywords) return null
    const matchesAll = activeKeywords.includes('*')
    const hit = (fields: (string | undefined)[]) =>
      matchesAll || activeKeywords.some((k) => fields.filter(Boolean).join(' ').toLowerCase().includes(k))

    const matchedSongs = matchesAll
      ? [...songs].sort((a, b) => b.plays - a.plays)
      : songs.filter((s) => {
          const artist = artists.find((a) => a.id === s.artistId)
          const album = albums.find((a) => a.id === s.albumId)
          return hit([s.title, s.genre, artist?.name, album?.title])
        })
    const matchedArtists = artists.filter((a) => hitsArtist(a.name, a.genres, activeKeywords))
    const matchedAlbums = albums.filter((a) => hit([a.title]))
    const matchedPlaylists = playlists.filter((p) => hit([p.title, p.description, p.curator]))
    return { matchedSongs, matchedArtists, matchedAlbums, matchedPlaylists }
  }, [activeKeywords, songs, artists, albums, playlists])

  const playIds = (ids: string[]) => {
    const valid = ids.filter((id) => songById.has(id))
    if (valid.length > 0) playQueue(valid, 0)
  }

  const topSong = results?.matchedSongs[0]
  const topSongArtist = topSong ? artists.find((a) => a.id === topSong.artistId) : undefined
  const topSongAlbum = topSong ? albums.find((a) => a.id === topSong.albumId) : undefined

  return (
    <div className="px-5 pb-12 pt-4 sm:px-6 lg:px-7">
      <h1 className="sr-only">Search</h1>

      {/* Search input */}
      <label className="flex h-12 w-full max-w-xl items-center gap-3 rounded-full border border-transparent bg-base-800 px-4 ring-1 ring-white/5 transition-all focus-within:bg-base-750 focus-within:ring-white/20 sm:h-13 sm:px-5">
        <SearchIcon size={19} className="shrink-0 text-text-muted" aria-hidden="true" />
        <input
          ref={inputRef}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Songs, artists, albums, playlists…"
          className="h-full min-w-0 flex-1 bg-transparent text-[15px] text-text-primary outline-none placeholder:text-text-muted"
          aria-label="Search the catalog"
        />
        {query && (
          <button onClick={() => setQuery('')} className="shrink-0 rounded-full p-1.5 text-text-muted transition-colors hover:text-white" aria-label="Clear search">
            <X size={17} />
          </button>
        )}
      </label>

      {/* Live status strip */}
      <div className="mt-1.5 flex h-6 items-center gap-2 px-1 text-[11px] font-semibold" aria-live="polite">
        {showSearching && (
          <span className="flex items-center gap-1.5 text-brand-400">
            <Loader2 size={12} className="animate-spin" /> Searching BeatBox…
          </span>
        )}
        {!showSearching && showError && (
          <span className="text-amber-400/90">Live search unavailable — showing catalog matches</span>
        )}
        {!showSearching && !showError && isLive && trimmed.length >= 3 && (
          <span className="flex items-center gap-1.5 text-text-muted">
            <span className="h-1.5 w-1.5 rounded-full bg-brand-500" /> Live results from BeatBox
          </span>
        )}
      </div>

      {!results ? (
        /* ── Browse mode ─────────────────────────────────── */
        <>
          <h2 className="mb-4 mt-7 text-xl font-extrabold tracking-tight text-white sm:text-2xl">Browse all</h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-4">
            {GENRE_TILES.map((tile: GenreTile) => {
              const Icon = TILE_ICONS[tile.icon] ?? Star
              return (
                <button
                  key={tile.id}
                  onClick={() => setQuery(tile.title)}
                  className={`relative h-24 overflow-hidden rounded-2xl bg-gradient-to-br p-3 text-left ring-1 ring-white/10 transition-transform hover:scale-[1.03] focus-visible:scale-[1.03] sm:h-32 sm:p-4 ${tile.from} ${tile.to}`}
                  aria-label={`Browse ${tile.title}`}
                >
                  <span className="relative z-10 max-w-[70%] text-[15px] font-extrabold leading-tight text-white drop-shadow sm:text-xl">{tile.title}</span>
                  <Icon className="absolute -bottom-2 right-2 h-16 w-16 rotate-[16deg] text-black/25 sm:-bottom-3 sm:h-24 sm:w-24" strokeWidth={1.4} aria-hidden="true" />
                </button>
              )
            })}
          </div>
        </>
      ) : results.matchedSongs.length === 0 && results.matchedArtists.length === 0 && results.matchedAlbums.length === 0 && results.matchedPlaylists.length === 0 ? (
        <EmptyState
          icon={SearchX}
          title={`No results for “${trimmed}”`}
          description="Check the spelling, or try a mood instead — Carnatic, Lo-Fi, Synthwave, Monsoon…"
          actionLabel="Clear search"
          onAction={() => setQuery('')}
        />
      ) : (
        /* ── Results mode ────────────────────────────────── */
        <div className="mt-6">
          <div className="grid gap-6 xl:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
            {/* Top result */}
            {topSong && (
              <section>
                <h2 className="mb-3 text-xl font-extrabold tracking-tight text-white">Top result</h2>
                <div className="group relative flex h-[calc(100%-2.5rem)] min-h-[240px] flex-col justify-end overflow-hidden rounded-2xl border border-white/5 bg-base-850 p-5 transition-colors hover:bg-base-800">
                  <CoverImage src={topSongAlbum?.cover} alt="" className="absolute inset-0 h-full w-full object-cover opacity-25" />
                  <div className="absolute inset-0 bg-gradient-to-t from-base-950 via-base-950/40 to-transparent" />
                  <div className="relative">
                    <CoverImage src={topSongAlbum?.cover} alt={`${topSong.title} cover`} className="h-24 w-24 rounded-xl shadow-card sm:h-28 sm:w-28" />
                    <h3 className="mt-4 clamp-2 text-2xl font-black tracking-tight text-white sm:text-3xl">{topSong.title}</h3>
                    <p className="mt-1.5 text-[12.5px] text-text-secondary">
                      <span className="rounded-full bg-white/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-text-primary">Song</span>
                      <span className="ml-2 align-middle">{topSongArtist?.name}</span>
                    </p>
                  </div>
                  <button
                    onClick={() => playQueue(results.matchedSongs.slice(0, 30).map((s) => s.id), 0)}
                    className="absolute bottom-4 right-4 flex h-12 w-12 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow transition-transform hover:scale-110"
                    aria-label={`Play ${topSong.title}`}
                  >
                    <Play size={20} fill="currentColor" className="ml-0.5" />
                  </button>
                </div>
              </section>
            )}

            {/* Songs */}
            {results.matchedSongs.length > 0 && (
              <section>
                <h2 className="mb-3 text-xl font-extrabold tracking-tight text-white">Songs</h2>
                <div className="-mx-2">
                  {results.matchedSongs.slice(0, 5).map((song, i) => (
                    <SongRow key={song.id} song={song} contextIds={results.matchedSongs.slice(0, 30).map((s) => s.id)} showAlbum={false} index={i + 1} />
                  ))}
                </div>
              </section>
            )}
          </div>

          {/* Artists */}
          {results.matchedArtists.length > 0 && (
            <>
              <h2 className="mb-3 mt-8 text-xl font-extrabold tracking-tight text-white">Artists</h2>
              <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
                {results.matchedArtists.slice(0, 6).map((artist) => (
                  <button key={artist.id} onClick={() => navigate(`/artist/${artist.id}`)} className="flex flex-col items-center gap-2 rounded-2xl p-3 transition-colors hover:bg-white/5">
                    <ArtistAvatar name={artist.name} image={artist.image} className="aspect-square w-full max-w-[104px] text-xl" />
                    <span className="clamp-1 text-[12px] font-bold text-text-primary">{artist.name}</span>
                    <span className="-mt-1.5 text-[10.5px] text-text-muted">Artist</span>
                  </button>
                ))}
              </div>
            </>
          )}

          {/* Albums */}
          {results.matchedAlbums.length > 0 && (
            <>
              <h2 className="mb-3 mt-8 text-xl font-extrabold tracking-tight text-white">Albums</h2>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                {results.matchedAlbums.slice(0, 6).map((album) => (
                  <MediaCard
                    key={album.id}
                    art={<CoverImage src={album.cover} alt={`${album.title} cover`} className="aspect-square w-full" />}
                    title={album.title}
                    subtitle={`${album.year}`}
                    onPlay={() => playIds(songs.filter((s) => s.albumId === album.id).map((s) => s.id))}
                    onClick={() => navigate(`/album/${album.id}`)}
                  />
                ))}
              </div>
            </>
          )}

          {/* Playlists */}
          {results.matchedPlaylists.length > 0 && (
            <>
              <h2 className="mb-3 mt-8 text-xl font-extrabold tracking-tight text-white">Playlists</h2>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                {results.matchedPlaylists.slice(0, 6).map((playlist) => (
                  <MediaCard
                    key={playlist.id}
                    art={<PlaylistArtwork art={playlist.art} size="100%" className="aspect-square w-full" />}
                    title={playlist.title}
                    subtitle={playlist.curator}
                    onPlay={() => playQueue(playlist.songIds.filter((id) => songById.has(id)), 0)}
                    onClick={() => navigate(`/playlist/${playlist.id}`)}
                  />
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  )
}

/** Artist matching supports both plain text and genre overlap. */
function hitsArtist(name: string, genres: string[], keywords: string[] | null): boolean {
  if (!keywords) return false
  if (keywords.includes('*')) return true
  const haystack = [name, ...genres].join(' ').toLowerCase()
  return keywords.some((k) => haystack.includes(k))
}
