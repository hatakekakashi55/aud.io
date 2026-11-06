import { useEffect } from 'react'
import { AudioLines, ChevronRight, Crown, Heart, Home, Library, Plus, Search, Settings, X } from 'lucide-react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useCatalog } from '../hooks/useCatalog'
import { useCatalogStore } from '../store/catalogStore'
import { usePlayerStore } from '../store/playerStore'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'
import { PlaylistArtwork } from './artwork'
import { useLocation } from 'react-router-dom'

function navItemClass(isActive: boolean) {
  return `flex items-center gap-4 rounded-xl px-3 py-2.5 text-[13.5px] font-semibold transition-colors ${
    isActive ? 'bg-white/[0.07] text-white' : 'text-text-secondary hover:text-white'
  }`
}

function SidebarContent({ onNavigate }: { onNavigate: () => void }) {
  const { playlists } = useCatalog()
  const navigate = useNavigate()
  const location = useLocation()
  const likedCount = usePlayerStore((s) => s.likedIds.length)
  const displayName = useUserStore((s) => s.displayName)
  const source = useCatalogStore((s) => s.source)
  const live = source === 'api'
  const premiumInterest = useUserStore((s) => s.premiumInterest)
  const createPlaylist = useUserStore((s) => s.createPlaylist)
  const push = useToastStore((s) => s.push)

  const initials = displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('')

  const handleCreate = () => {
    const playlist = createPlaylist()
    push({ icon: 'plus', title: `Created ${playlist.title}`, description: 'Find it in Your Library' })
    onNavigate()
    navigate(`/playlist/${playlist.id}`)
  }

  return (
    <div className="flex h-full flex-col bg-[#0c0c12]">
      {/* Wordmark */}
      <div className="flex items-center justify-between px-5 pb-1 pt-5">
        <Link to="/" className="flex items-center gap-2.5" aria-label="AUD home">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-500 shadow-glow">
            <AudioLines size={20} strokeWidth={2.4} className="text-[#0a0a0f]" />
          </span>
          <span className="text-[21px] font-black tracking-[0.24em] text-white">AUD</span>
        </Link>
        <button className="rounded-full p-2 text-text-muted hover:text-white lg:hidden" onClick={onNavigate} aria-label="Close sidebar">
          <X size={18} />
        </button>
      </div>

      {/* Primary nav */}
      <nav className="mt-2 space-y-1 px-3" aria-label="Primary">
        <NavLink to="/" end className={({ isActive }) => navItemClass(isActive)} onClick={onNavigate}>
          <Home size={20} strokeWidth={2.1} /> Home
        </NavLink>
        <NavLink to="/search" className={({ isActive }) => navItemClass(isActive)} onClick={onNavigate}>
          <Search size={20} strokeWidth={2.1} /> Search
        </NavLink>
      </nav>

      {/* Library header */}
      <div className="mt-5 flex items-center px-3">
        <NavLink to="/library" className={({ isActive }) => `${navItemClass(isActive)} flex-1`} onClick={onNavigate}>
          <Library size={20} strokeWidth={2.1} /> Your Library
        </NavLink>
        <button
          onClick={handleCreate}
          className="ml-1 rounded-full p-2 text-text-secondary transition-all hover:scale-110 hover:bg-white/5 hover:text-white"
          aria-label="Create playlist"
          title="Create playlist"
        >
          <Plus size={19} />
        </button>
        <button
          onClick={() => { onNavigate(); navigate('/library') }}
          className="rounded-full p-2 text-text-secondary transition-all hover:scale-110 hover:bg-white/5 hover:text-white"
          aria-label="Open library"
          title="Open library"
        >
          <ChevronRight size={19} />
        </button>
      </div>

      {/* Liked songs shortcut */}
      <button onClick={() => { onNavigate(); navigate('/liked') }} className="mx-3 mt-2 flex items-center gap-3 rounded-xl px-3 py-2 text-left transition-colors hover:bg-white/5">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-400 via-purple-600 to-purple-900 shadow-card">
          <Heart size={18} className="text-white" fill="currentColor" />
        </span>
        <span className="min-w-0">
          <span className="block clamp-1 text-[13.5px] font-semibold text-text-primary">Liked Songs</span>
          <span className="block text-[11px] text-text-muted">Playlist · {likedCount} songs</span>
        </span>
      </button>

      {/* Playlist list */}
      <div className="mx-5 mt-4 border-t border-white/5" aria-hidden="true" />
      <div className="flex items-center justify-between px-6 pb-1 pt-3">
        <span className="text-[10.5px] font-bold uppercase tracking-[0.2em] text-text-muted">Playlists</span>
        <span className="rounded-full bg-white/5 px-2 py-0.5 text-[10.5px] font-semibold tabular-nums text-text-muted">{playlists.length}</span>
      </div>
      <div className="nice-scroll min-h-0 flex-1 overflow-y-auto px-3 pb-3">
        {playlists.map((playlist) => {
          const active = location.pathname === `/playlist/${playlist.id}`
          return (
            <button
              key={playlist.id}
              onClick={() => { onNavigate(); navigate(`/playlist/${playlist.id}`) }}
              className={`flex w-full items-center gap-3 rounded-xl px-2 py-[7px] text-left transition-colors ${active ? 'bg-white/[0.07]' : 'hover:bg-white/5'}`}
              aria-current={active ? 'page' : undefined}
            >
              <PlaylistArtwork art={playlist.art} size={44} className="shrink-0 rounded-md" />
              <span className="min-w-0">
                <span className={`block clamp-1 text-[13px] font-semibold ${active ? 'text-brand-400' : 'text-text-primary'}`}>{playlist.title}</span>
                <span className="block clamp-1 text-[11px] text-text-muted">
                  {playlist.userCreated ? `By you · ${playlist.songIds.length} songs` : `Playlist · ${playlist.curator}`}
                </span>
              </span>
            </button>
          )
        })}
      </div>

      {/* Live source badge */}
      {live && (
        <div className="mx-3 mb-2 flex items-center gap-2 rounded-xl border border-brand-500/20 bg-brand-500/[0.07] px-3 py-2" title="Catalog is streaming live from the BeatBox API">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-500 opacity-60" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-500" />
          </span>
          <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-brand-400">Live · BeatBox API</span>
        </div>
      )}

      {/* User card */}
      <div className="border-t border-white/5 p-3">
        <div className="flex items-center gap-3 rounded-xl px-2 py-2">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-emerald-900 text-[12px] font-extrabold text-white">
            {initials || 'A'}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block clamp-1 text-[13px] font-bold text-text-primary">{displayName}</span>
            {premiumInterest ? (
              <span className="flex items-center gap-1 text-[10.5px] font-semibold text-amber-400"><Crown size={10} strokeWidth={2.5} /> Premium waitlist</span>
            ) : (
              <span className="text-[10.5px] text-text-muted">Free plan</span>
            )}
          </span>
          <button onClick={() => { onNavigate(); navigate('/settings') }} className="rounded-full p-2 text-text-muted transition-colors hover:bg-white/5 hover:text-white" aria-label="Settings" title="Settings">
            <Settings size={18} />
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  return (
    <>
      {/* Desktop rail */}
      <aside className="hidden w-[300px] shrink-0 border-r border-white/5 lg:block" aria-label="Sidebar">
        <SidebarContent onNavigate={() => undefined} />
      </aside>

      {/* Mobile drawer */}
      <div className={`fixed inset-0 z-40 lg:hidden ${open ? '' : 'pointer-events-none'}`} aria-hidden={!open}>
        <button
          className={`absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity duration-300 ${open ? 'opacity-100' : 'opacity-0'}`}
          onClick={onClose}
          aria-label="Close sidebar"
          tabIndex={open ? 0 : -1}
        />
        <aside className={`absolute inset-y-0 left-0 w-[300px] max-w-[85vw] border-r border-white/10 shadow-card transition-transform duration-300 ease-out ${open ? 'translate-x-0' : '-translate-x-full'}`}>
          <SidebarContent onNavigate={onClose} />
        </aside>
      </div>
    </>
  )
}
