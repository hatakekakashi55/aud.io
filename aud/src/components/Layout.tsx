import { useEffect, useState } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { AlertTriangle, AudioLines, RefreshCw, FlaskConical } from 'lucide-react'
import { initAudioEngine } from '../services/audioEngine'
import { useCatalogStore } from '../store/catalogStore'
import useKeyboardShortcuts from '../hooks/useKeyboardShortcuts'
import Sidebar from './Sidebar'
import TopBar from './TopBar'
import PlayerBar from './PlayerBar'
import ExpandedPlayer from './ExpandedPlayer'
import SongContextMenu from './SongContextMenu'
import Toasts from './Toasts'
import Footer from './Footer'
import { useUserStore } from '../store/userStore'
import Home from '../pages/Home'
import Search from '../pages/Search'
import Library from '../pages/Library'
import PlaylistDetail from '../pages/PlaylistDetail'
import ArtistDetail from '../pages/ArtistDetail'
import AlbumDetail from '../pages/AlbumDetail'
import LikedSongs from '../pages/LikedSongs'
import Queue from '../pages/Queue'
import Settings from '../pages/Settings'
import NotFound from '../pages/NotFound'

function CatalogLoading() {
  return (
    <div className="px-5 py-6 lg:px-7" aria-label="Loading your music">
      <div className="flex items-center gap-3 text-text-muted">
        <AudioLines size={18} className="animate-pulse text-brand-500" />
        <span className="text-sm font-semibold tracking-wide">Tuning in…</span>
      </div>
      <div className="skeleton mt-5 h-40 w-full max-w-3xl rounded-3xl" />
      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-6">
        {Array.from({ length: 6 }, (_, i) => <div key={i} className="skeleton aspect-square rounded-2xl" />)}
      </div>
      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
        {Array.from({ length: 6 }, (_, i) => <div key={i} className="skeleton aspect-[3/4] rounded-2xl" />)}
      </div>
    </div>
  )
}

function CatalogError({ message, onRetry, onDemo }: { message: string | null; onRetry: () => void; onDemo: () => void }) {
  return (
    <div className="flex h-full items-center justify-center px-6">
      <div className="w-full max-w-md rounded-3xl border border-white/10 bg-base-850 p-6 text-center shadow-card">
        <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-400/10">
          <AlertTriangle size={28} className="text-amber-400" />
        </span>
        <h1 className="mt-4 text-xl font-black text-white">Can't reach the music service</h1>
        <p className="mt-1.5 text-[13px] text-text-secondary">Your API is configured but didn't respond. Check the URL and that the service is running.</p>
        {message && (
          <code className="mt-3 block select-text truncate rounded-xl border border-white/5 bg-black/40 px-3 py-2 text-left text-[11px] text-red-300">
            {message}
          </code>
        )}
        <button onClick={onRetry} className="mt-4 flex w-full items-center justify-center gap-2 rounded-full bg-brand-500 py-3 text-sm font-extrabold text-black transition-colors hover:bg-brand-400">
          <RefreshCw size={16} strokeWidth={2.5} /> Try again
        </button>
        <button onClick={onDemo} className="mt-2 flex w-full items-center justify-center gap-2 rounded-full border border-white/10 py-3 text-sm font-bold text-text-secondary transition-colors hover:bg-white/5 hover:text-white">
          <FlaskConical size={16} /> Continue with demo catalog
        </button>
      </div>
    </div>
  )
}

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const reduceMotion = useUserStore((s) => s.reduceMotion)
  const compactMode = useUserStore((s) => s.compactMode)
  const status = useCatalogStore((s) => s.status)
  const error = useCatalogStore((s) => s.error)
  const load = useCatalogStore((s) => s.load)
  const useDemoFallback = useCatalogStore((s) => s.useDemoFallback)

  useKeyboardShortcuts()

  useEffect(() => {
    initAudioEngine()
    void load()
  }, [load])

  useEffect(() => {
    document.getElementById('content-scroll')?.scrollTo({ top: 0 })
  }, [location.pathname])

  return (
    <div className={`${compactMode ? 'compact ' : ''}${reduceMotion ? 'reduce-motion ' : ''}flex h-screen flex-col overflow-hidden bg-base-950 text-text-primary`}>
      <div className="flex min-h-0 flex-1">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="relative flex min-w-0 flex-1 flex-col bg-base-950">
          <TopBar onMenu={() => setSidebarOpen(true)} />
          <div id="content-scroll" className="hover-scroll relative min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
            {status === 'ready' ? (
              <motion.div
                key={location.pathname}
                initial={{ opacity: 0, y: 16, filter: 'blur(6px)' }}
                animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
                transition={{ duration: 0.38, ease: [0.22, 1, 0.36, 1] }}
                className="min-h-full"
              >
              <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/search" element={<Search />} />
                <Route path="/library" element={<Library />} />
                <Route path="/playlist/:id" element={<PlaylistDetail />} />
                <Route path="/artist/:id" element={<ArtistDetail />} />
                <Route path="/album/:id" element={<AlbumDetail />} />
                <Route path="/liked" element={<LikedSongs />} />
                <Route path="/queue" element={<Queue />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="*" element={<NotFound />} />
              </Routes>
              </motion.div>
            ) : status === 'error' ? (
              <CatalogError message={error} onRetry={() => void load()} onDemo={useDemoFallback} />
            ) : (
              <CatalogLoading />
            )}
            {status === 'ready' && <Footer />}
          </div>
        </main>
      </div>

      <PlayerBar />
      <ExpandedPlayer />
      <SongContextMenu />
      <Toasts />
    </div>
  )
}
