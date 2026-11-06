import { Play } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useToastStore } from '../store/toastStore'
import { useUserStore } from '../store/userStore'
import MediaCard from '../components/MediaCard'
import SectionHeader from '../components/SectionHeader'
import SongRow from '../components/SongRow'
import { ArtistAvatar, CoverImage, PlaylistArtwork } from '../components/artwork'
import { greeting } from '../utils/format'
import type { Playlist } from '../types'

export default function Home() {
  const navigate = useNavigate()
  const { curatedPlaylists, songs, artists, albums, songById, songsOfAlbum } = useCatalog()
  const playQueue = usePlayerStore((s) => s.playQueue)
  const recentIds = usePlayerStore((s) => s.recentIds)
  const push = useToastStore((s) => s.push)
  const displayName = useUserStore((s) => s.displayName)

  const playPlaylist = (playlist: Playlist, shuffled = false) => {
    const ids = playlist.songIds.filter((id) => songById.has(id))
    if (ids.length === 0) {
      push({ icon: 'info', title: 'This playlist is empty', description: 'Add songs from any track’s ⋯ menu' })
      return
    }
    const start = shuffled ? Math.floor(Math.random() * ids.length) : 0
    playQueue(ids, start)
  }

  const quickPicks = curatedPlaylists.slice(0, 6)
  const recentSongs = recentIds.map((id) => songById.get(id)).filter((s): s is NonNullable<typeof s> => Boolean(s)).slice(0, 6)
  const recentContext = recentSongs.map((s) => s.id)
  const trending = [...songs].sort((a, b) => b.plays - a.plays || Date.parse(b.releaseDate) - Date.parse(a.releaseDate)).slice(0, 6)
  const trendingContext = trending.map((s) => s.id)
  const topArtists = [...artists].sort((a, b) => b.monthlyListeners - a.monthlyListeners)
  const freshAlbums = [...albums].sort((a, b) => b.year - a.year).slice(0, 6)

  return (
    <div className="px-5 pb-12 pt-4 sm:px-6 lg:px-7">
      <h1 className="text-2xl font-black tracking-tight text-white sm:text-[28px]">{greeting()}, <span className="text-brand-400">{displayName.split(' ')[0]}</span></h1>

      {/* Quick picks */}
      <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2 sm:gap-2.5 xl:grid-cols-3">
        {quickPicks.map((playlist) => (
          <div
            key={playlist.id}
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/playlist/${playlist.id}`)}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/playlist/${playlist.id}`) } }}
            className="group relative flex h-16 cursor-pointer items-stretch overflow-hidden rounded-lg bg-base-800/70 text-left ring-1 ring-white/5 outline-none transition-colors hover:bg-base-750 focus-visible:bg-base-750 sm:h-[72px]"
          >
            <PlaylistArtwork art={playlist.art} size="100%" className="h-full w-16 shrink-0 rounded-l-lg sm:w-[72px]" />
            <span className="flex min-w-0 flex-1 items-center px-3 sm:px-4">
              <span className="clamp-2 text-[12px] font-bold leading-snug text-text-primary">{playlist.title}</span>
            </span>
            <button
              onClick={(e) => { e.stopPropagation(); playPlaylist(playlist) }}
              aria-label={`Play ${playlist.title}`}
              className="absolute right-3 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-brand-500 text-black opacity-0 shadow-glow transition-all hover:scale-110 group-hover:opacity-100 focus-visible:opacity-100"
            >
              <Play size={18} fill="currentColor" className="ml-0.5" />
            </button>
          </div>
        ))}
      </div>

      {/* Jump back in */}
      {recentSongs.length > 0 && (
        <>
          <SectionHeader title="Jump back in" />
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-6">
            {recentSongs.map((song, i) => {
              const album = albums.find((a) => a.id === song.albumId)
              const artist = artists.find((a) => a.id === song.artistId)
              return (
                <MediaCard
                  key={`${song.id}-${i}`}
                  art={<CoverImage src={album?.cover} alt="" className="aspect-square w-full" />}
                  title={song.title}
                  subtitle={artist?.name}
                  onPlay={() => playQueue(recentContext, i)}
                  onClick={() => { if (album) navigate(`/album/${album.id}`) }}
                />
              )
            })}
          </div>
        </>
      )}

      {/* Made for you */}
      <SectionHeader title="Made for you" to="/library" actionLabel="Your library" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
        {curatedPlaylists.slice(0, 6).map((playlist) => (
          <MediaCard
            key={playlist.id}
            art={<PlaylistArtwork art={playlist.art} size="100%" className="aspect-square w-full" />}
            title={playlist.title}
            subtitle={playlist.description}
            onPlay={() => playPlaylist(playlist)}
            onClick={() => navigate(`/playlist/${playlist.id}`)}
          />
        ))}
      </div>

      {/* Trending now */}
      <SectionHeader title="Trending now" to="/queue" actionLabel="Open queue" />
      <div className="rounded-2xl border border-white/5 bg-white/[0.03] p-1.5 sm:p-2">
        {trending.map((song, i) => (
          <SongRow key={song.id} song={song} index={i + 1} contextIds={trendingContext} showAlbum={false} />
        ))}
      </div>

      {/* Artists */}
      <SectionHeader title="Artists you’ll love" to="/search" actionLabel="Find more" />
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {topArtists.map((artist) => (
          <button key={artist.id} onClick={() => navigate(`/artist/${artist.id}`)} className="group flex flex-col items-center gap-2 rounded-2xl p-3 outline-none transition-colors hover:bg-white/5 focus-visible:bg-white/5">
            <ArtistAvatar name={artist.name} image={artist.image} className="w-full max-w-[120px] aspect-square text-2xl ring-1 ring-white/10 transition-transform duration-300 group-hover:scale-[1.04]" />
            <span className="clamp-1 max-w-full text-[12px] font-bold text-text-primary">{artist.name}</span>
            <span className="-mt-1.5 text-[10.5px] text-text-muted">Artist</span>
          </button>
        ))}
      </div>

      {/* New releases */}
      <SectionHeader title="Fresh albums" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
        {freshAlbums.map((album) => {
          const artist = artists.find((a) => a.id === album.artistId)
          return (
            <MediaCard
              key={album.id}
              art={<CoverImage src={album.cover} alt={`${album.title} cover`} className="aspect-square w-full" />}
              title={album.title}
              subtitle={`${album.year} · ${artist?.name ?? ''}`}
              onPlay={() => {
                const ids = songsOfAlbum(album.id).map((s) => s.id)
                if (ids.length > 0) playQueue(ids, 0)
              }}
              onClick={() => navigate(`/album/${album.id}`)}
            />
          )
        })}
      </div>
    </div>
  )
}
