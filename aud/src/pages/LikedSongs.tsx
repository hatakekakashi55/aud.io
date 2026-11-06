import { useNavigate } from 'react-router-dom'
import { Heart, Play } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import SongRow, { SongTableHeader } from '../components/SongRow'
import EmptyState from '../components/EmptyState'
import { useUserStore } from '../store/userStore'
import { formatLongDuration } from '../utils/format'

export default function LikedSongs() {
  const navigate = useNavigate()
  const { songById } = useCatalog()
  const likedIds = usePlayerStore((s) => s.likedIds)
  const playQueue = usePlayerStore((s) => s.playQueue)
  const displayName = useUserStore((s) => s.displayName)

  const likedSongs = likedIds.map((id) => songById.get(id)).filter((s): s is NonNullable<typeof s> => Boolean(s))
  const songIds = likedSongs.map((s) => s.id)
  const totalSeconds = likedSongs.reduce((acc, s) => acc + s.duration, 0)

  return (
    <div className="pb-10">
      {/* Banner */}
      <div className="relative overflow-hidden bg-gradient-to-b from-indigo-800/70 via-purple-900/25 to-base-950">
        <div className="flex flex-col gap-5 px-5 pb-6 pt-14 sm:flex-row sm:items-end sm:px-7">
          <div className="flex h-44 w-44 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-400 via-purple-600 to-purple-900 shadow-card ring-1 ring-white/15 sm:h-52 sm:w-52">
            <Heart size={80} className="text-white drop-shadow-lg" fill="currentColor" strokeWidth={0} />
          </div>
          <div className="min-w-0">
            <p className="text-[10.5px] font-bold uppercase tracking-[0.26em] text-white/70">Playlist</p>
            <h1 className="mt-1.5 text-5xl font-black tracking-tight text-white sm:text-6xl">Liked Songs</h1>
            <p className="mt-3 text-[12.5px] text-white/90">
              <span className="font-bold text-white">{displayName}</span>
              {likedSongs.length > 0 && <> · {likedSongs.length} songs, {formatLongDuration(totalSeconds)}</>}
            </p>
          </div>
        </div>
      </div>

      {likedSongs.length > 0 ? (
        <>
          <div className="flex items-center gap-3 px-5 py-4 sm:px-7">
            <button onClick={() => playQueue(songIds, 0)} className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow transition-all hover:scale-105 hover:bg-brand-400 active:scale-95" aria-label="Play liked songs">
              <Play size={24} fill="currentColor" className="ml-0.5" />
            </button>
          </div>
          <div className="px-3 sm:px-5">
            <SongTableHeader />
            <div className="mt-1.5">
              {likedSongs.map((song, i) => (
                <SongRow key={song.id} song={song} index={i + 1} contextIds={songIds} />
              ))}
            </div>
          </div>
        </>
      ) : (
        <EmptyState
          icon={Heart}
          title="Likes live here"
          description="Tap the heart on any track and it'll be waiting for you in this corner."
          actionLabel="Find something to love"
          onAction={() => navigate('/search')}
        />
      )}
    </div>
  )
}
