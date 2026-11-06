import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ListMusic, Music4, Play, Shuffle, Trash2 } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'
import { PlaylistArtwork } from '../components/artwork'
import SongRow, { SongTableHeader } from '../components/SongRow'
import EmptyState from '../components/EmptyState'
import { formatCount, formatLongDuration, randomIndex } from '../utils/format'

export default function PlaylistDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { playlistById, songsByIds } = useCatalog()
  const playQueue = usePlayerStore((s) => s.playQueue)
  const shuffle = usePlayerStore((s) => s.shuffle)
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle)
  const deletePlaylist = useUserStore((s) => s.deletePlaylist)
  const push = useToastStore((s) => s.push)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const playlist = id ? playlistById.get(id) : undefined
  const songs = playlist ? songsByIds(playlist.songIds) : []
  const totalSeconds = songs.reduce((acc, s) => acc + s.duration, 0)
  const songIds = songs.map((s) => s.id)
  const isUserPlaylist = Boolean(playlist?.userCreated)

  useEffect(() => {
    if (!confirmDelete) return
    const t = window.setTimeout(() => setConfirmDelete(false), 3000)
    return () => window.clearTimeout(t)
  }, [confirmDelete])

  if (!playlist) {
    return (
      <EmptyState
        icon={ListMusic}
        title="Playlist not found"
        description="It may have been deleted, or the link is wrong."
        actionLabel="Back to library"
        onAction={() => navigate('/library')}
      />
    )
  }

  const playAll = () => {
    if (songIds.length === 0) {
      push({ icon: 'info', title: 'This playlist is empty', description: 'Add songs from any track’s ⋯ menu' })
      return
    }
    playQueue(songIds, 0)
  }

  const shufflePlay = () => {
    if (songIds.length === 0) return
    if (!shuffle) toggleShuffle()
    playQueue(songIds, randomIndex(songIds.length))
    push({ icon: 'info', title: `Shuffling ${playlist.title}` })
  }

  const handleDelete = () => {
    if (!confirmDelete) {
      setConfirmDelete(true)
      return
    }
    deletePlaylist(playlist.id)
    push({ icon: 'trash', title: `Deleted ${playlist.title}` })
    navigate('/library')
  }

  return (
    <div className="pb-10">
      {/* Banner */}
      <div className="relative overflow-hidden">
        {playlist.art.type === 'image' ? (
          <img src={playlist.art.src} alt="" aria-hidden="true" className="absolute inset-0 h-full w-full scale-125 object-cover opacity-30 blur-2xl" />
        ) : (
          <div className={`absolute inset-0 scale-110 bg-gradient-to-br ${playlist.art.from} ${playlist.art.to} opacity-40 blur-2xl`} aria-hidden="true" />
        )}
        <div className="absolute inset-0 bg-gradient-to-b from-white/[0.06] via-base-950/30 to-base-950" aria-hidden="true" />
        <div className="relative flex flex-col gap-5 px-5 pb-6 pt-14 sm:flex-row sm:items-end sm:px-7">
          <PlaylistArtwork art={playlist.art} size={176} className="shrink-0 rounded-2xl shadow-card ring-1 ring-white/15" />
          <div className="min-w-0">
            <p className="text-[10.5px] font-bold uppercase tracking-[0.26em] text-white/70">Playlist{isUserPlaylist ? ' · Private' : ''}</p>
            <h1 className="mt-1.5 clamp-2 text-4xl font-black leading-[1.02] tracking-tight text-white sm:text-5xl xl:text-6xl">{playlist.title}</h1>
            <p className="mt-3 clamp-2 max-w-2xl text-[13px] leading-snug text-white/80">{playlist.description}</p>
            <p className="mt-2 text-[12px] text-white/90">
              <span className="font-bold text-white">{playlist.curator}</span>
              {playlist.followers > 0 && <> · {formatCount(playlist.followers)} followers</>}
              {songs.length > 0 && <> · {songs.length} songs, {formatLongDuration(totalSeconds)}</>}
            </p>
          </div>
        </div>
      </div>

      {/* Action bar */}
      <div className="flex items-center gap-3 px-5 py-4 sm:px-7">
        <button onClick={playAll} className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow transition-all hover:scale-105 hover:bg-brand-400 active:scale-95" aria-label="Play playlist">
          <Play size={24} fill="currentColor" className="ml-0.5" />
        </button>
        <button onClick={shufflePlay} className="flex h-12 w-12 items-center justify-center rounded-full text-text-secondary transition-all hover:scale-105 hover:text-white" aria-label="Shuffle play" title="Shuffle play">
          <Shuffle size={22} />
        </button>
        {isUserPlaylist && (
          <button
            onClick={handleDelete}
            className={`ml-auto flex items-center gap-2 rounded-full px-4 py-2 text-[12px] font-bold transition-all ${confirmDelete ? 'bg-red-500 text-white' : 'text-red-400 hover:bg-red-500/10'}`}
            aria-label={confirmDelete ? 'Confirm delete playlist' : 'Delete playlist'}
          >
            <Trash2 size={15} />
            {confirmDelete ? 'Tap again to delete' : 'Delete playlist'}
          </button>
        )}
      </div>

      {/* Track list */}
      {songs.length > 0 ? (
        <div className="px-3 sm:px-5">
          <SongTableHeader />
          <div className="mt-1.5">
            {songs.map((song, i) => (
              <SongRow
                key={`${song.id}-${i}`}
                song={song}
                index={i + 1}
                contextIds={songIds}
                removeFromPlaylistId={isUserPlaylist ? playlist.id : undefined}
              />
            ))}
          </div>
        </div>
      ) : (
        <EmptyState
          icon={Music4}
          title="This playlist is empty"
          description="Open any track’s ⋯ menu and choose Add to playlist to build it out."
          actionLabel="Browse music"
          onAction={() => navigate('/')}
        />
      )}
    </div>
  )
}
