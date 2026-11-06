import { useNavigate, useParams } from 'react-router-dom'
import { Disc3, Play, Shuffle } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useToastStore } from '../store/toastStore'
import SongRow, { SongTableHeader } from '../components/SongRow'
import EmptyState from '../components/EmptyState'
import { ArtistAvatar, CoverImage } from '../components/artwork'
import { formatLongDuration, randomIndex } from '../utils/format'

export default function AlbumDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { albumById, artistById, songsOfAlbum } = useCatalog()
  const playQueue = usePlayerStore((s) => s.playQueue)
  const shuffle = usePlayerStore((s) => s.shuffle)
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle)
  const push = useToastStore((s) => s.push)

  const album = id ? albumById.get(id) : undefined

  if (!album) {
    return (
      <EmptyState
        icon={Disc3}
        title="Album not found"
        description="This album isn't in the catalog."
        actionLabel="Back to home"
        onAction={() => navigate('/')}
      />
    )
  }

  const artist = artistById.get(album.artistId)
  const songs = songsOfAlbum(album.id)
  const songIds = songs.map((s) => s.id)
  const totalSeconds = songs.reduce((acc, s) => acc + s.duration, 0)
  const typeLabel = album.type === 'album' ? 'Album' : album.type === 'ep' ? 'EP' : 'Single'

  const playAll = () => {
    if (songIds.length > 0) playQueue(songIds, 0)
  }

  const shufflePlay = () => {
    if (songIds.length === 0) return
    if (!shuffle) toggleShuffle()
    playQueue(songIds, randomIndex(songIds.length))
    push({ icon: 'info', title: `Shuffling ${album.title}` })
  }

  return (
    <div className="pb-10">
      {/* Banner */}
      <div className="relative overflow-hidden">
        {album.cover && <img src={album.cover} alt="" aria-hidden="true" className="absolute inset-0 h-full w-full scale-125 object-cover opacity-30 blur-2xl" />}
        <div className="absolute inset-0 bg-gradient-to-b from-white/[0.06] via-base-950/30 to-base-950" aria-hidden="true" />
        <div className="relative flex flex-col gap-5 px-5 pb-6 pt-14 sm:flex-row sm:items-end sm:px-7">
          <CoverImage src={album.cover} alt={`${album.title} cover`} className="h-44 w-44 shrink-0 rounded-2xl shadow-card ring-1 ring-white/15 sm:h-52 sm:w-52" />
          <div className="min-w-0">
            <p className="text-[10.5px] font-bold uppercase tracking-[0.26em] text-white/70">{typeLabel}</p>
            <h1 className="mt-1.5 clamp-2 text-4xl font-black leading-[1.02] tracking-tight text-white sm:text-5xl">{album.title}</h1>
            <div className="mt-3 flex items-center gap-2">
              {artist && (
                <button onClick={() => navigate(`/artist/${artist.id}`)} className="flex items-center gap-2 transition-opacity hover:opacity-85">
                  <ArtistAvatar name={artist.name} image={artist.image} className="h-6 w-6 text-[9px]" />
                  <span className="text-[13px] font-bold text-white hover:underline">{artist.name}</span>
                </button>
              )}
              <span className="text-[13px] text-white/80">· {album.year} · {songs.length} songs, {formatLongDuration(totalSeconds)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3 px-5 py-4 sm:px-7">
        <button onClick={playAll} className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow transition-all hover:scale-105 hover:bg-brand-400 active:scale-95" aria-label={`Play ${album.title}`}>
          <Play size={24} fill="currentColor" className="ml-0.5" />
        </button>
        <button onClick={shufflePlay} className="flex h-12 w-12 items-center justify-center rounded-full text-text-secondary transition-all hover:scale-105 hover:text-white" aria-label="Shuffle play" title="Shuffle play">
          <Shuffle size={22} />
        </button>
      </div>

      {/* Tracks */}
      <div className="px-3 sm:px-5">
        <SongTableHeader showAlbum={false} />
        <div className="mt-1.5">
          {songs.map((song, i) => (
            <SongRow key={song.id} song={song} index={i + 1} contextIds={songIds} showAlbum={false} />
          ))}
        </div>
      </div>

      {/* Release footer */}
      <div className="mt-6 border-t border-white/5 px-5 pt-4 sm:px-7">
        <p className="text-[12px] text-text-muted">Released {new Date(songs[0]?.releaseDate ?? `${album.year}-01-01`).toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })}</p>
        <p className="mt-1 text-[11px] text-text-muted">℗ {album.year} AUD Records · All rights reserved</p>
      </div>
    </div>
  )
}
