import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowDownWideNarrow, ChevronRight, Plus, Search as SearchIcon, X } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'
import { ArtistAvatar, CoverImage, PlaylistArtwork } from '../components/artwork'
import EmptyState from '../components/EmptyState'

type Tab = 'all' | 'playlists' | 'artists' | 'albums'
type Sort = 'recent' | 'name'

const TABS: { id: Tab; label: string }[] = [
  { id: 'all', label: 'All' },
  { id: 'playlists', label: 'Playlists' },
  { id: 'artists', label: 'Artists' },
  { id: 'albums', label: 'Albums' },
]

export default function Library() {
  const navigate = useNavigate()
  const { playlists, artists, albums } = useCatalog()
  const createPlaylist = useUserStore((s) => s.createPlaylist)
  const push = useToastStore((s) => s.push)
  const [tab, setTab] = useState<Tab>('all')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<Sort>('recent')

  const q = query.trim().toLowerCase()
  const matches = (text: string) => !q || text.toLowerCase().includes(q)

  const filteredPlaylists = useMemo(
    () => playlists.filter((p) => matches(`${p.title} ${p.curator}`)).sort((a, b) => (sort === 'name' ? a.title.localeCompare(b.title) : Number(b.userCreated ?? false) - Number(a.userCreated ?? false))),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [playlists, q, sort],
  )
  const filteredArtists = useMemo(
    () => artists.filter((a) => matches(a.name)).sort((a, b) => (sort === 'name' ? a.name.localeCompare(b.name) : b.monthlyListeners - a.monthlyListeners)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [artists, q, sort],
  )
  const filteredAlbums = useMemo(
    () => albums.filter((a) => matches(a.title)).sort((a, b) => (sort === 'name' ? a.title.localeCompare(b.title) : b.year - a.year)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [albums, q, sort],
  )

  const showPlaylists = tab === 'all' || tab === 'playlists'
  const showArtists = tab === 'all' || tab === 'artists'
  const showAlbums = tab === 'all' || tab === 'albums'
  const totalCount = (showPlaylists ? filteredPlaylists.length : 0) + (showArtists ? filteredArtists.length : 0) + (showAlbums ? filteredAlbums.length : 0)

  const handleCreate = () => {
    const playlist = createPlaylist()
    push({ icon: 'plus', title: `Created ${playlist.title}`, description: 'Find it in Your Library' })
    navigate(`/playlist/${playlist.id}`)
  }

  return (
    <div className="px-5 pb-12 pt-5 sm:px-6 lg:px-7">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-black tracking-tight text-white sm:text-[28px]">Your Library</h1>
        <button onClick={handleCreate} className="flex h-10 w-10 items-center justify-center rounded-full text-text-secondary transition-all hover:scale-110 hover:bg-white/5 hover:text-white" aria-label="Create playlist" title="Create playlist">
          <Plus size={22} />
        </button>
      </div>

      {/* Controls */}
      <div className="mt-4 flex flex-wrap items-center gap-2">
        <label className="flex h-9 w-48 items-center gap-2 rounded-full bg-base-800 px-3 ring-1 ring-white/5 focus-within:ring-white/20">
          <SearchIcon size={15} className="shrink-0 text-text-muted" />
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Filter library" className="h-full min-w-0 flex-1 bg-transparent text-[12.5px] outline-none placeholder:text-text-muted" aria-label="Filter library" />
          {query && (
            <button onClick={() => setQuery('')} aria-label="Clear filter" className="text-text-muted hover:text-white">
              <X size={14} />
            </button>
          )}
        </label>
        <label className="flex h-9 items-center gap-2 rounded-full bg-base-800 px-3 text-text-secondary ring-1 ring-white/5">
          <ArrowDownWideNarrow size={15} aria-hidden="true" />
          <select value={sort} onChange={(e) => setSort(e.target.value as Sort)} className="h-full bg-transparent text-[12.5px] font-medium outline-none [&>option]:bg-base-800" aria-label="Sort library">
            <option value="recent">Recent</option>
            <option value="name">Name A–Z</option>
          </select>
        </label>
        <div className="flex items-center gap-1.5">
          {TABS.map(({ id, label }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              aria-pressed={tab === id}
              className={`h-9 rounded-full px-3.5 text-[12px] font-bold transition-all ${tab === id ? 'bg-white text-black' : 'bg-base-800 text-text-secondary hover:bg-base-750 hover:text-white'}`}
            >
              {label}
            </button>
          ))}
        </div>
        <span className="ml-auto text-[11px] font-medium tabular-nums text-text-muted">{totalCount} {totalCount === 1 ? 'item' : 'items'}</span>
      </div>

      {/* List */}
      <div className="mt-4">
        {showPlaylists && filteredPlaylists.length > 0 && (
          <>
            {tab === 'all' && <h2 className="px-3 pb-1 pt-2 text-[11px] font-bold uppercase tracking-[0.2em] text-text-muted">Playlists</h2>}
            {filteredPlaylists.map((playlist) => (
              <button key={playlist.id} onClick={() => navigate(`/playlist/${playlist.id}`)} className="group flex w-full items-center gap-4 rounded-2xl px-3 py-2.5 text-left transition-colors hover:bg-white/5">
                <PlaylistArtwork art={playlist.art} size={64} className="shrink-0 rounded-lg shadow-card" />
                <span className="min-w-0 flex-1">
                  <span className="block clamp-1 text-[15px] font-bold text-text-primary">{playlist.title}</span>
                  <span className="mt-0.5 block text-[12px] text-text-muted">Playlist · {playlist.userCreated ? `By you · ${playlist.songIds.length} songs` : playlist.curator}</span>
                </span>
                <ChevronRight size={17} className="shrink-0 text-text-muted opacity-0 transition-opacity group-hover:opacity-100" />
              </button>
            ))}
          </>
        )}

        {showArtists && filteredArtists.length > 0 && (
          <>
            {tab === 'all' && <h2 className="px-3 pb-1 pt-4 text-[11px] font-bold uppercase tracking-[0.2em] text-text-muted">Artists</h2>}
            {filteredArtists.map((artist) => (
              <button key={artist.id} onClick={() => navigate(`/artist/${artist.id}`)} className="group flex w-full items-center gap-4 rounded-2xl px-3 py-2.5 text-left transition-colors hover:bg-white/5">
                <ArtistAvatar name={artist.name} image={artist.image} className="h-16 w-16 shrink-0 shadow-card" />
                <span className="min-w-0 flex-1">
                  <span className="block clamp-1 text-[15px] font-bold text-text-primary">{artist.name}</span>
                  <span className="mt-0.5 block text-[12px] text-text-muted">Artist · {artist.genres[0] ?? 'Music'}</span>
                </span>
                <ChevronRight size={17} className="shrink-0 text-text-muted opacity-0 transition-opacity group-hover:opacity-100" />
              </button>
            ))}
          </>
        )}

        {showAlbums && filteredAlbums.length > 0 && (
          <>
            {tab === 'all' && <h2 className="px-3 pb-1 pt-4 text-[11px] font-bold uppercase tracking-[0.2em] text-text-muted">Albums</h2>}
            {filteredAlbums.map((album) => {
              const artist = artists.find((a) => a.id === album.artistId)
              return (
                <button key={album.id} onClick={() => navigate(`/album/${album.id}`)} className="group flex w-full items-center gap-4 rounded-2xl px-3 py-2.5 text-left transition-colors hover:bg-white/5">
                  <CoverImage src={album.cover} alt="" className="h-16 w-16 shrink-0 rounded-lg shadow-card" />
                  <span className="min-w-0 flex-1">
                    <span className="block clamp-1 text-[15px] font-bold text-text-primary">{album.title}</span>
                    <span className="mt-0.5 block text-[12px] text-text-muted">Album · {album.year} · {artist?.name}</span>
                  </span>
                  <ChevronRight size={17} className="shrink-0 text-text-muted opacity-0 transition-opacity group-hover:opacity-100" />
                </button>
              )
            })}
          </>
        )}

        {totalCount === 0 && (
          <EmptyState icon={SearchIcon} title="Nothing matches that filter" description={`No library entries match “${query}”. Try a different search.`} actionLabel="Clear filter" onAction={() => setQuery('')} />
        )}
      </div>
    </div>
  )
}
