import { AudioLines } from 'lucide-react'
import { useCatalogStore } from '../store/catalogStore'

export default function Footer() {
  const live = useCatalogStore((s) => s.source === 'api')
  return (
    <footer className="border-t border-white/5 px-5 py-6 sm:px-7">
      <div className="flex flex-col items-center justify-between gap-3 text-center sm:flex-row sm:text-left">
        <div className="flex items-center gap-2">
          <AudioLines size={15} className="text-brand-600" />
          <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-text-muted">AUD</span>
        </div>
        <p className="text-[10.5px] text-text-muted">© 2026 AUD Technologies · Music for every mood</p>
        <p className="text-[10.5px] text-text-muted/70">{live ? 'Streaming live from JioSaavn · BeatBox Worker' : 'Demo catalog · streams via SoundHelix'}</p>
      </div>
    </footer>
  )
}
