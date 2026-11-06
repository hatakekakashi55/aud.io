import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  ChevronDown,
  ChevronRight,
  Heart,
  ListMusic,
  MonitorSpeaker,
  MoreHorizontal,
  Music2,
  Pause,
  Play,
  Repeat,
  Repeat1,
  Share2,
  Shuffle,
  SkipBack,
  SkipForward,
  X,
} from 'lucide-react'
import { usePlayerStore } from '../store/playerStore'
import { useCatalog } from '../hooks/useCatalog'
import { useContextMenuStore } from '../store/contextMenuStore'
import { useToastStore } from '../store/toastStore'
import { ArtistAvatar, CoverImage } from './artwork'
import SeekBar from './SeekBar'
import { formatDuration } from '../utils/format'

export default function ExpandedPlayer() {
  const navigate = useNavigate()
  const { songById, artistById, albumById } = useCatalog()

  const expanded = usePlayerStore((s) => s.expanded)
  const setExpanded = usePlayerStore((s) => s.setExpanded)
  const currentSongId = usePlayerStore((s) => s.currentSongId)
  const isPlaying = usePlayerStore((s) => s.isPlaying)
  const togglePlay = usePlayerStore((s) => s.togglePlay)
  const next = usePlayerStore((s) => s.next)
  const prev = usePlayerStore((s) => s.prev)
  const shuffle = usePlayerStore((s) => s.shuffle)
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle)
  const repeat = usePlayerStore((s) => s.repeat)
  const cycleRepeat = usePlayerStore((s) => s.cycleRepeat)
  const queue = usePlayerStore((s) => s.queue)
  const queueIndex = usePlayerStore((s) => s.queueIndex)
  const playQueue = usePlayerStore((s) => s.playQueue)
  const clearUpcoming = usePlayerStore((s) => s.clearUpcoming)
  const liked = usePlayerStore((s) => (currentSongId ? s.likedIds.includes(currentSongId) : false))
  const toggleLike = usePlayerStore((s) => s.toggleLike)
  const openMenu = useContextMenuStore((s) => s.open)
  const push = useToastStore((s) => s.push)

  if (!expanded) return null

  const song = currentSongId ? songById.get(currentSongId) : undefined
  const artist = song ? artistById.get(song.artistId) : undefined
  const album = song ? albumById.get(song.albumId) : undefined
  const upNext = queue.slice(queueIndex + 1)

  const share = async () => {
    if (!song) return
    const url = `${window.location.origin}${window.location.pathname}#/artist/${song.artistId}`
    try {
      await navigator.clipboard.writeText(url)
      push({ icon: 'link', title: 'Link copied to clipboard' })
    } catch {
      push({ icon: 'info', title: 'Could not copy link' })
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 72 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', stiffness: 260, damping: 30 }}
      className="fixed inset-0 z-[55] flex flex-col overflow-hidden bg-base-950"
      role="dialog"
      aria-modal="true"
      aria-label="Full-screen player"
    >
      {/* Ambient artwork backdrop */}
      {album?.cover && (
        <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
          <img src={album.cover} alt="" className="h-full w-full scale-125 object-cover opacity-35 blur-[110px] saturate-150" />
          <div className="absolute inset-0 bg-gradient-to-b from-black/50 via-base-950/55 to-base-950" />
        </div>
      )}

      {/* Top bar */}
      <div className="relative flex h-16 shrink-0 items-center justify-between px-4 sm:px-6">
        <button onClick={() => setExpanded(false)} className="rounded-full p-2.5 text-text-secondary transition-colors hover:bg-white/10 hover:text-white" aria-label="Collapse player">
          <ChevronDown size={22} />
        </button>
        <div className="text-center">
          <p className="text-[10px] font-bold uppercase tracking-[0.24em] text-text-muted">Playing from {album?.type ?? 'queue'}</p>
          <p className="clamp-1 max-w-[45vw] text-[12px] font-semibold text-text-secondary">{album?.title ?? 'Your queue'}</p>
        </div>
        <button
          onClick={(e) => song && openMenu(song.id, Math.min(e.clientX - 260, window.innerWidth - 280), e.clientY + 8)}
          className="rounded-full p-2.5 text-text-secondary transition-colors hover:bg-white/10 hover:text-white"
          aria-label="More options for current track"
        >
          <MoreHorizontal size={20} />
        </button>
      </div>

      {/* Body */}
      <div className="nice-scroll relative grid min-h-0 flex-1 items-center gap-6 overflow-y-auto px-4 pb-6 sm:px-8 lg:grid-cols-[minmax(0,1fr)_minmax(340px,400px)] lg:gap-10 lg:overflow-hidden">
        {/* Left — artwork + controls */}
        <div className="mx-auto flex w-full max-w-[560px] flex-col items-center justify-center pt-2 lg:pt-0">
          {song ? (
            <>
              <div className="w-full max-w-[min(440px,52vh)]">
                <CoverImage
                  src={album?.cover}
                  alt={album ? `${album.title} cover art` : 'Cover art'}
                  className="aspect-square w-full rounded-2xl shadow-card ring-1 ring-white/10"
                />
              </div>

              <div className="mt-6 flex w-full max-w-[min(520px,100%)] items-center gap-4">
                <div className="min-w-0 flex-1">
                  <h1 className="clamp-2 text-xl font-black leading-tight tracking-tight text-white sm:text-2xl lg:text-3xl">{song.title}</h1>
                  {artist && (
                    <button onClick={() => { setExpanded(false); navigate(`/artist/${artist.id}`) }} className="mt-1.5 flex items-center gap-2 text-text-secondary transition-colors hover:text-white">
                      <ArtistAvatar name={artist.name} image={artist.image} className="h-6 w-6 text-[9px]" />
                      <span className="text-sm font-medium hover:underline">{artist.name}</span>
                    </button>
                  )}
                </div>
                <button
                  onClick={() => toggleLike(song.id)}
                  className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full border transition-all hover:scale-105 ${liked ? 'border-brand-500/40 bg-brand-500/15 text-brand-400' : 'border-white/15 text-text-secondary hover:text-white'}`}
                  aria-label={liked ? 'Remove from Liked Songs' : 'Save to Liked Songs'}
                  aria-pressed={liked}
                >
                  <Heart size={20} fill={liked ? 'currentColor' : 'none'} />
                </button>
              </div>

              <div className="mt-4 w-full max-w-[min(520px,100%)]">
                <SeekBar thick />
              </div>

              <div className="mt-2 flex w-full max-w-[min(520px,100%)] items-center justify-between">
                <button onClick={toggleShuffle} className={`relative rounded-full p-2 transition-colors ${shuffle ? 'text-brand-400' : 'text-text-secondary hover:text-white'}`} aria-label="Toggle shuffle" aria-pressed={shuffle}>
                  <Shuffle size={19} />
                  {shuffle && <span className="absolute bottom-0.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-brand-400" />}
                </button>
                <button onClick={prev} className="rounded-full p-2 text-text-primary transition-colors hover:text-white" aria-label="Previous track">
                  <SkipBack size={26} fill="currentColor" strokeWidth={0} />
                </button>
                <button onClick={togglePlay} className="flex h-16 w-16 items-center justify-center rounded-full bg-white text-black shadow-glow transition-all hover:scale-105 active:scale-95" aria-label={isPlaying ? 'Pause' : 'Play'}>
                  <AnimatePresence mode="wait" initial={false}>
                    <motion.span
                      key={isPlaying ? 'pause' : 'play'}
                      initial={{ scale: 0.5, opacity: 0 }}
                      animate={{ scale: 1, opacity: 1 }}
                      exit={{ scale: 0.5, opacity: 0 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 26 }}
                      className="flex items-center justify-center"
                    >
                      {isPlaying ? <Pause size={28} fill="currentColor" strokeWidth={0} /> : <Play size={28} fill="currentColor" strokeWidth={0} className="ml-1" />}
                    </motion.span>
                  </AnimatePresence>
                </button>
                <button onClick={() => next(false)} className="rounded-full p-2 text-text-primary transition-colors hover:text-white" aria-label="Next track">
                  <SkipForward size={26} fill="currentColor" strokeWidth={0} />
                </button>
                <button onClick={cycleRepeat} className={`relative rounded-full p-2 transition-colors ${repeat !== 'off' ? 'text-brand-400' : 'text-text-secondary hover:text-white'}`} aria-label={`Repeat mode: ${repeat}`}>
                  {repeat === 'one' ? <Repeat1 size={19} /> : <Repeat size={19} />}
                  {repeat !== 'off' && <span className="absolute bottom-0.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-brand-400" />}
                </button>
              </div>

              <div className="mt-4 flex w-full max-w-[min(520px,100%)] items-center justify-between text-text-muted">
                <span className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em]">
                  <MonitorSpeaker size={15} /> Web Audio
                </span>
                <div className="flex items-center gap-1">
                  <button onClick={share} className="rounded-full p-2 transition-colors hover:bg-white/10 hover:text-white" aria-label="Share track" title="Copy listening link">
                    <Share2 size={17} />
                  </button>
                  <button onClick={() => { setExpanded(false); navigate('/queue') }} className="flex items-center gap-1.5 rounded-full py-2 pl-2 pr-1 transition-colors hover:bg-white/10 hover:text-white" aria-label="Open queue">
                    <ListMusic size={17} />
                    <span className="hidden text-[11px] font-semibold uppercase tracking-[0.14em] sm:block">Queue · {upNext.length}</span>
                    <ChevronRight size={15} />
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center gap-4 py-24 text-center">
              <span className="flex h-24 w-24 items-center justify-center rounded-3xl bg-white/5"><Music2 size={40} className="text-text-muted" /></span>
              <h1 className="text-2xl font-black text-white">Nothing playing yet</h1>
              <p className="max-w-xs text-sm text-text-secondary">Pick a song from anywhere in AUD — your session will start here.</p>
              <button onClick={() => { setExpanded(false); navigate('/') }} className="mt-2 rounded-full bg-brand-500 px-6 py-3 text-sm font-extrabold text-black transition-transform hover:scale-105">Browse music</button>
            </div>
          )}
        </div>

        {/* Right — up next */}
        <aside className="flex max-h-[46vh] w-full flex-col overflow-hidden rounded-2xl border border-white/[0.08] bg-white/[0.045] backdrop-blur-xl lg:max-h-[74vh]" aria-label="Up next">
          <div className="flex shrink-0 items-center justify-between border-b border-white/5 px-5 py-4">
            <span className="text-[12px] font-bold uppercase tracking-[0.2em] text-text-secondary">Up next · {upNext.length}</span>
            {upNext.length > 0 && (
              <button onClick={clearUpcoming} className="flex items-center gap-1 text-[11px] font-semibold text-text-muted transition-colors hover:text-white">
                <X size={13} /> Clear
              </button>
            )}
          </div>
          <div className="nice-scroll min-h-0 flex-1 overflow-y-auto p-2">
            {upNext.length === 0 ? (
              <div className="flex h-full min-h-[120px] flex-col items-center justify-center gap-2 text-center">
                <ListMusic size={22} className="text-text-muted" />
                <p className="text-[13px] font-semibold text-text-secondary">End of the queue</p>
                <p className="max-w-[220px] text-[11px] leading-snug text-text-muted">Add songs with the ⋯ menu on any track.</p>
              </div>
            ) : (
              upNext.map((id, i) => {
                const upNextSong = songById.get(id)
                if (!upNextSong) return null
                const upNextArtist = artistById.get(upNextSong.artistId)
                const upNextAlbum = albumById.get(upNextSong.albumId)
                const absoluteIndex = queueIndex + 1 + i
                return (
                  <button
                    key={`${id}-${absoluteIndex}`}
                    onClick={() => playQueue(queue, absoluteIndex)}
                    className="flex w-full items-center gap-3 rounded-xl p-2 text-left transition-colors hover:bg-white/5"
                  >
                    <CoverImage src={upNextAlbum?.cover} alt="" className="h-11 w-11 shrink-0 rounded-md" />
                    <span className="min-w-0 flex-1">
                      <span className="block clamp-1 text-[13px] font-semibold text-text-primary">{upNextSong.title}</span>
                      <span className="block clamp-1 text-[11px] text-text-muted">{upNextArtist?.name}</span>
                    </span>
                    <span className="shrink-0 text-[11px] tabular-nums text-text-muted">{formatDuration(upNextSong.duration)}</span>
                  </button>
                )
              })
            )}
          </div>
        </aside>
      </div>
    </motion.div>
  )
}
