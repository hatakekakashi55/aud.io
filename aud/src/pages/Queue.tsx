import { useNavigate } from 'react-router-dom'
import { ListMusic, Trash2, X } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import EmptyState from '../components/EmptyState'
import { CoverImage } from '../components/artwork'
import { formatDuration } from '../utils/format'

export default function Queue() {
  const navigate = useNavigate()
  const { songById, artistById, albumById } = useCatalog()
  const currentSongId = usePlayerStore((s) => s.currentSongId)
  const queue = usePlayerStore((s) => s.queue)
  const queueIndex = usePlayerStore((s) => s.queueIndex)
  const removeFromQueue = usePlayerStore((s) => s.removeFromQueue)
  const clearUpcoming = usePlayerStore((s) => s.clearUpcoming)
  const playQueue = usePlayerStore((s) => s.playQueue)

  const current = currentSongId ? songById.get(currentSongId) : undefined
  const currentArtist = current ? artistById.get(current.artistId) : undefined
  const currentAlbum = current ? albumById.get(current.albumId) : undefined
  const upNext = queue.slice(queueIndex + 1)

  if (!current) {
    return (
      <EmptyState
        icon={ListMusic}
        title="Your queue is empty"
        description="Press play on anything and what comes next will appear here — like a thoughtful DJ."
        actionLabel="Browse music"
        onAction={() => navigate('/')}
      />
    )
  }

  return (
    <div className="mx-auto w-full max-w-5xl px-5 pb-12 pt-5 sm:px-6">
      <h1 className="text-2xl font-black tracking-tight text-white sm:text-[28px]">Queue</h1>

      {/* Now playing */}
      <h2 className="mt-6 text-[11px] font-bold uppercase tracking-[0.22em] text-text-muted">Now playing</h2>
      <div className="mt-2 flex items-center gap-4 rounded-2xl border border-brand-500/15 bg-brand-500/[0.06] p-3">
        <CoverImage src={currentAlbum?.cover} alt="" className="h-16 w-16 shrink-0 rounded-xl shadow-card" />
        <div className="min-w-0 flex-1">
          <p className="clamp-1 text-[15px] font-bold text-brand-400">{current.title}</p>
          <button onClick={() => currentArtist && navigate(`/artist/${currentArtist.id}`)} className="clamp-1 text-[12.5px] text-text-secondary hover:text-white hover:underline">
            {currentArtist?.name}
          </button>
        </div>
        <span className="shrink-0 text-[12px] tabular-nums text-text-muted">{formatDuration(current.duration)}</span>
      </div>

      {/* Up next */}
      <div className="mt-6 flex items-center justify-between">
        <h2 className="text-[11px] font-bold uppercase tracking-[0.22em] text-text-muted">Next in queue · {upNext.length}</h2>
        {upNext.length > 0 && (
          <button onClick={clearUpcoming} className="flex items-center gap-1.5 text-[11.5px] font-semibold text-text-muted transition-colors hover:text-white">
            <Trash2 size={13} /> Clear queue
          </button>
        )}
      </div>

      {upNext.length === 0 ? (
        <div className="mt-8 flex flex-col items-center gap-2 rounded-2xl border border-dashed border-white/10 py-12 text-center">
          <ListMusic size={26} className="text-text-muted" strokeWidth={1.5} />
          <p className="text-[13.5px] font-bold text-text-primary">That's the end of the line</p>
          <p className="max-w-[260px] text-[11.5px] leading-relaxed text-text-secondary">Add more songs with the ⋯ menu — or Play next to hear them immediately.</p>
        </div>
      ) : (
        <ul className="mt-2 divide-y divide-white/5">
          {upNext.map((id, i) => {
            const absoluteIndex = queueIndex + 1 + i
            const song = songById.get(id)
            if (!song) return null
            const artist = artistById.get(song.artistId)
            const album = albumById.get(song.albumId)
            return (
              <li key={`${id}-${absoluteIndex}`} className="group flex items-center gap-3 py-2">
                <button onClick={() => playQueue(queue, absoluteIndex)} className="flex min-w-0 flex-1 items-center gap-3 rounded-xl p-1.5 text-left transition-colors hover:bg-white/5" aria-label={`Jump to ${song.title}`}>
                  <CoverImage src={album?.cover} alt="" className="h-12 w-12 shrink-0 rounded-lg" />
                  <span className="min-w-0 flex-1">
                    <span className="block clamp-1 text-[13.5px] font-semibold text-text-primary">{song.title}</span>
                    <span className="block clamp-1 text-[11.5px] text-text-muted">{artist?.name}</span>
                  </span>
                  <span className="shrink-0 text-[11.5px] tabular-nums text-text-muted">{formatDuration(song.duration)}</span>
                </button>
                <button
                  onClick={() => removeFromQueue(absoluteIndex)}
                  className="shrink-0 rounded-full p-2 text-text-muted opacity-0 transition-all hover:bg-white/5 hover:text-white focus-visible:opacity-100 group-hover:opacity-100"
                  aria-label={`Remove ${song.title} from queue`}
                  title="Remove from queue"
                >
                  <X size={17} />
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {/* Tip */}
      <p className="mt-8 rounded-2xl border border-white/5 bg-white/[0.03] px-4 py-3 text-[11.5px] leading-relaxed text-text-muted">
        Tip — right-click any track anywhere in AUD for <span className="text-text-secondary">Play next</span>, <span className="text-text-secondary">Add to queue</span> and <span className="text-text-secondary">Add to playlist</span>.
      </p>
    </div>
  )
}
