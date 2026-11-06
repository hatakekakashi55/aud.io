import { useEffect, useRef, useState, type CSSProperties } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  AudioLines,
  ChevronUp,
  Heart,
  Laptop,
  ListMusic,
  MonitorSpeaker,
  Pause,
  Play,
  Repeat,
  Repeat1,
  Shuffle,
  SkipBack,
  SkipForward,
  Volume1,
  Volume2,
  VolumeX,
} from 'lucide-react'
import { usePlayerStore } from '../store/playerStore'
import { useCatalog } from '../hooks/useCatalog'
import { useToastStore } from '../store/toastStore'
import SeekBar from './SeekBar'
import { CoverImage } from './artwork'

function VolumeControl() {
  const volume = usePlayerStore((s) => s.volume)
  const muted = usePlayerStore((s) => s.muted)
  const setVolume = usePlayerStore((s) => s.setVolume)
  const toggleMute = usePlayerStore((s) => s.toggleMute)
  const effective = muted ? 0 : volume
  const Icon = effective === 0 ? VolumeX : effective < 0.5 ? Volume1 : Volume2

  return (
    <div className="hidden items-center gap-1.5 md:flex">
      <button onClick={toggleMute} className="rounded-full p-2 text-text-secondary transition-colors hover:text-white" aria-label={muted ? 'Unmute' : 'Mute'} aria-pressed={muted}>
        <Icon size={18} />
      </button>
      <input
        type="range"
        min={0}
        max={100}
        value={Math.round(effective * 100)}
        onChange={(e) => setVolume(Number(e.target.value) / 100)}
        className="aud-range w-24"
        style={{ '--fill': `${effective * 100}%` } as CSSProperties}
        aria-label="Volume"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(effective * 100)}
      />
    </div>
  )
}

function DevicePopover() {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointerDown = (e: PointerEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    window.addEventListener('pointerdown', onPointerDown)
    return () => window.removeEventListener('pointerdown', onPointerDown)
  }, [open])

  return (
    <div className="relative hidden sm:block" ref={ref}>
      <button onClick={() => setOpen((v) => !v)} className="rounded-full p-2 text-text-secondary transition-colors hover:text-white" aria-label="Connect to a device" aria-expanded={open} title="Connect to a device">
        <MonitorSpeaker size={18} />
      </button>
      {open && (
        <div className="absolute bottom-12 right-0 w-72 overflow-hidden rounded-2xl border border-white/10 bg-base-750/95 shadow-card backdrop-blur-2xl animate-fade-up" role="dialog" aria-label="Available devices">
          <p className="border-b border-white/5 px-4 py-3 text-[11px] font-bold uppercase tracking-[0.18em] text-text-muted">Connect to a device</p>
          <div className="p-2">
            <div className="flex items-center gap-3 rounded-xl bg-brand-500/10 px-3 py-2.5">
              <Laptop size={17} className="shrink-0 text-brand-400" />
              <span className="min-w-0 flex-1">
                <span className="block text-[13px] font-semibold text-brand-400">This browser</span>
                <span className="block text-[10.5px] text-text-muted">Web Audio · Active session</span>
              </span>
              <span className="h-2 w-2 rounded-full bg-brand-500" aria-label="Connected" />
            </div>
            <p className="px-3 pb-1.5 pt-2.5 text-[10.5px] leading-snug text-text-muted">AUD Connect speakers and TVs on your network will appear here automatically.</p>
          </div>
        </div>
      )}
    </div>
  )
}

export default function PlayerBar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { songById, artistById, albumById } = useCatalog()

  const currentSongId = usePlayerStore((s) => s.currentSongId)
  const isPlaying = usePlayerStore((s) => s.isPlaying)
  const togglePlay = usePlayerStore((s) => s.togglePlay)
  const next = usePlayerStore((s) => s.next)
  const prev = usePlayerStore((s) => s.prev)
  const shuffle = usePlayerStore((s) => s.shuffle)
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle)
  const repeat = usePlayerStore((s) => s.repeat)
  const cycleRepeat = usePlayerStore((s) => s.cycleRepeat)
  const toggleLike = usePlayerStore((s) => s.toggleLike)
  const liked = usePlayerStore((s) => (currentSongId ? s.likedIds.includes(currentSongId) : false))
  const setExpanded = usePlayerStore((s) => s.setExpanded)
  const push = useToastStore((s) => s.push)

  const song = currentSongId ? songById.get(currentSongId) : undefined
  const artist = song ? artistById.get(song.artistId) : undefined
  const album = song ? albumById.get(song.albumId) : undefined
  const queueActive = location.pathname === '/queue'

  const repeatIcon = repeat === 'one'
    ? <Repeat1 size={17} strokeWidth={2.3} />
    : <Repeat size={17} strokeWidth={2.3} />

  return (
    <footer className="relative z-40 shrink-0 border-t border-white/5 bg-[#0c0c12]/95 px-2 backdrop-blur-xl sm:px-4" aria-label="Now playing bar">
      <div className="grid h-[84px] grid-cols-[minmax(0,1fr)_minmax(0,2.2fr)_minmax(0,1fr)] items-center gap-2 sm:gap-4">
        {/* Left — track meta */}
        <div className="flex min-w-0 items-center gap-2 sm:gap-3">
          {song ? (
            <>
              <button onClick={() => setExpanded(true)} className="group relative h-14 w-14 shrink-0 overflow-hidden rounded-lg shadow-card" aria-label="Open full player">
                <CoverImage src={album?.cover} alt={album ? `${album.title} cover` : ''} className="h-full w-full transition-transform duration-300 group-hover:scale-105" />
                <span className="absolute inset-0 flex items-center justify-center bg-black/35 opacity-0 transition-opacity group-hover:opacity-100">
                  <ChevronUp size={20} className="text-white" />
                </span>
              </button>
              <motion.div key={song.id} className="min-w-0" initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.3, ease: 'easeOut' }}>
                <button onClick={() => setExpanded(true)} className="clamp-1 block max-w-full text-left text-[13.5px] font-semibold leading-tight text-text-primary hover:underline">
                  {song.title}
                </button>
                {artist && (
                  <button onClick={() => navigate(`/artist/${artist.id}`)} className="clamp-1 block max-w-full text-left text-[11px] text-text-secondary transition-colors hover:text-white hover:underline">
                    {artist.name}
                  </button>
                )}
              </motion.div>
              <button
                onClick={() => { toggleLike(song.id); push({ icon: 'heart', title: liked ? 'Removed from Liked Songs' : 'Saved to Liked Songs', description: song.title }) }}
                className={`ml-1 hidden rounded-full p-1.5 transition-all hover:scale-110 sm:block ${liked ? 'text-brand-500' : 'text-text-secondary hover:text-white'}`}
                aria-label={liked ? 'Remove from Liked Songs' : 'Save to Liked Songs'}
                aria-pressed={liked}
              >
                <Heart size={17} fill={liked ? 'currentColor' : 'none'} />
              </button>
            </>
          ) : (
            <Link to="/" className="flex select-none items-center gap-2 text-text-muted transition-colors hover:text-text-secondary" aria-label="AUD home">
              <AudioLines size={22} className="text-brand-600" />
              <span className="hidden text-[12px] font-semibold uppercase tracking-[0.22em] md:block">Your music awaits</span>
            </Link>
          )}
        </div>

        {/* Center — transport + seek */}
        <div className="flex min-w-0 flex-col items-center justify-center gap-1.5">
          <div className="flex items-center gap-3 sm:gap-5">
            <button onClick={toggleShuffle} className={`relative rounded-full p-1 transition-colors ${shuffle ? 'text-brand-400' : 'text-text-secondary hover:text-white'}`} aria-label="Toggle shuffle" aria-pressed={shuffle} title="Shuffle">
              <Shuffle size={16} />
              {shuffle && <span className="absolute -bottom-1.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-brand-400" aria-hidden="true" />}
            </button>
            <button onClick={prev} className="text-text-secondary transition-colors hover:text-white" aria-label="Previous track" title="Previous (Shift+Left)">
              <SkipBack size={19} fill="currentColor" strokeWidth={0} />
            </button>
            <button
              onClick={togglePlay}
              disabled={!song}
              className="flex h-9 w-9 items-center justify-center rounded-full bg-white text-black transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-30 sm:h-10 sm:w-10"
              aria-label={isPlaying ? 'Pause' : 'Play'}
              title="Play/Pause (Space)"
            >
              <AnimatePresence mode="wait" initial={false}>
                <motion.span
                  key={isPlaying ? 'pause' : 'play'}
                  initial={{ scale: 0.5, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  exit={{ scale: 0.5, opacity: 0 }}
                  transition={{ type: 'spring', stiffness: 500, damping: 28 }}
                  className="flex items-center justify-center"
                >
                  {isPlaying ? <Pause size={18} fill="currentColor" strokeWidth={0} /> : <Play size={18} fill="currentColor" strokeWidth={0} className="ml-0.5" />}
                </motion.span>
              </AnimatePresence>
            </button>
            <button onClick={() => next(false)} className="text-text-secondary transition-colors hover:text-white" aria-label="Next track" title="Next (Shift+Right)">
              <SkipForward size={19} fill="currentColor" strokeWidth={0} />
            </button>
            <button onClick={cycleRepeat} className={`relative rounded-full p-1 transition-colors ${repeat !== 'off' ? 'text-brand-400' : 'text-text-secondary hover:text-white'}`} aria-label={`Repeat mode: ${repeat}`} title={`Repeat: ${repeat}`}>
              {repeatIcon}
              {repeat !== 'off' && <span className="absolute -bottom-1.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-brand-400" aria-hidden="true" />}
            </button>
          </div>
          {song && <SeekBar className="w-full max-w-[560px]" />}
        </div>

        {/* Right — queue, devices, volume, expand */}
        <div className="flex items-center justify-end gap-0.5 sm:gap-1">
          <NavLink to="/queue" className={`rounded-full p-2 transition-colors ${queueActive ? 'text-brand-400' : 'text-text-secondary hover:text-white'}`} aria-label="Queue" aria-current={queueActive ? 'page' : undefined} title="Queue">
            <ListMusic size={18} />
          </NavLink>
          <DevicePopover />
          <VolumeControl />
          <button onClick={() => setExpanded(true)} className="rounded-full p-2 text-text-secondary transition-colors hover:text-white" aria-label="Full-screen player" title="Full-screen player (E)">
            <ChevronUp size={19} />
          </button>
        </div>
      </div>
    </footer>
  )
}
