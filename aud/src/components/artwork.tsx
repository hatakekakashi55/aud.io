/**
 * Artwork primitives — every image in the app flows through one of these
 * so fallbacks (API data with missing artwork) look intentional.
 */
import {
  Car,
  CloudRain,
  Coffee,
  Drum,
  Flame,
  Guitar,
  Headphones,
  Heart,
  ListMusic,
  Music2,
  Music3,
  Music4,
  Radio,
  Sparkles,
  Star,
  Sun,
  TrendingUp,
  Zap,
} from 'lucide-react'
import type { PlaylistArt } from '../types'

const ART_ICONS: Record<string, typeof Music2> = {
  Heart,
  Sparkles,
  Guitar,
  Flame,
  CloudRain,
  TrendingUp,
  Car,
  Coffee,
  ListMusic,
  Zap,
  Sun,
  Drum,
  Star,
  Headphones,
  Radio,
  Music2,
  Music3,
  Music4,
}

export function PlaylistArtwork({ art, size = '100%', className = '' }: { art: PlaylistArt; size?: number | string; className?: string }) {
  if (art.type === 'image') {
    return (
      <img
        src={art.src}
        alt=""
        loading="lazy"
        draggable={false}
        className={`object-cover bg-base-800 ${className}`}
        style={{ width: size, height: size }}
      />
    )
  }
  const Icon = ART_ICONS[art.icon] ?? Music2
  return (
    <div
      className={`bg-gradient-to-br ${art.from} ${art.to} flex items-center justify-center ${className}`}
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      <Icon className="w-2/5 h-2/5 text-white/90" strokeWidth={1.5} />
    </div>
  )
}

export function CoverImage({ src, alt, className = '' }: { src: string | null | undefined; alt: string; className?: string }) {
  if (!src) {
    return (
      <div className={`bg-gradient-to-br from-base-700 to-base-850 flex items-center justify-center ${className}`} aria-hidden="true">
        <Music2 className="w-1/4 h-1/4 text-text-muted" />
      </div>
    )
  }
  return <img src={src} alt={alt} loading="lazy" draggable={false} className={`object-cover bg-base-800 ${className}`} />
}

const AVATAR_HUES = [262, 336, 24, 174, 210, 96, 0, 288]

export function ArtistAvatar({ name, image, className = '' }: { name: string; image?: string | null; className?: string }) {
  if (image) {
    return <img src={image} alt={name} loading="lazy" draggable={false} className={`object-cover bg-base-800 rounded-full ${className}`} />
  }
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('')
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) | 0
  const hue = AVATAR_HUES[Math.abs(hash) % AVATAR_HUES.length]
  return (
    <div
      className={`rounded-full flex items-center justify-center font-bold text-white/95 select-none ${className}`}
      style={{ background: `linear-gradient(135deg, hsl(${hue} 55% 38%), hsl(${hue} 60% 16%))` }}
      aria-hidden="true"
    >
      <span style={{ fontSize: '0.9em' }}>{initials}</span>
    </div>
  )
}
