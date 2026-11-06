import { Play } from 'lucide-react'
import type { ReactNode } from 'react'

interface MediaCardProps {
  art: ReactNode
  title: string
  subtitle?: string
  onPlay: () => void
  onClick: () => void
  /** Round artwork for artist cards. */
  roundArt?: boolean
}

/** Spotify-style vertical card with a hover play button. */
export default function MediaCard({ art, title, subtitle, onPlay, onClick, roundArt = false }: MediaCardProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onClick()
        }
      }}
      className="media-card group relative cursor-pointer rounded-2xl bg-base-850 p-4 outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
    >
      <div className="relative">
        <div className={`overflow-hidden bg-base-800 shadow-card ${roundArt ? 'rounded-full' : 'rounded-xl'}`}>{art}</div>
        <button
          onClick={(e) => {
            e.stopPropagation()
            onPlay()
          }}
          aria-label={`Play ${title}`}
          className="play-fab absolute bottom-2 right-2 flex h-12 w-12 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow hover:scale-105 hover:bg-brand-400 active:scale-95 transition-transform"
        >
          <Play size={22} fill="currentColor" strokeWidth={0} className="ml-0.5" />
        </button>
      </div>
      <p className="mt-3 clamp-1 text-[15px] font-bold text-text-primary">{title}</p>
      {subtitle && <p className="mt-1 clamp-2 text-[13px] leading-snug text-text-secondary">{subtitle}</p>}
    </div>
  )
}
