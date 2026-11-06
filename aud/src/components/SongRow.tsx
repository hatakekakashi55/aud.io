import { Clock3, Heart, MoreHorizontal, Pause, Play } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { MouseEvent } from 'react'
import type { Song } from '../types'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useContextMenuStore } from '../store/contextMenuStore'
import { useUserStore } from '../store/userStore'
import { formatDuration } from '../utils/format'
import Equalizer from './Equalizer'
import ExplicitBadge from './ExplicitBadge'
import { CoverImage } from './artwork'

interface SongRowProps {
  song: Song
  /** 1-based position shown in the index column. */
  index?: number
  /** The list this row belongs to — playback will continue through it. */
  contextIds?: string[]
  showAlbum?: boolean
  /** When set, the context menu offers "Remove from this playlist". */
  removeFromPlaylistId?: string
}

export default function SongRow({ song, index, contextIds, showAlbum = true, removeFromPlaylistId }: SongRowProps) {
  const navigate = useNavigate()
  const { artistById, albumById } = useCatalog()
  const showExplicit = useUserStore((s) => s.showExplicit)

  const currentSongId = usePlayerStore((s) => s.currentSongId)
  const isPlaying = usePlayerStore((s) => s.isPlaying)
  const playQueue = usePlayerStore((s) => s.playQueue)
  const togglePlay = usePlayerStore((s) => s.togglePlay)
  const toggleLike = usePlayerStore((s) => s.toggleLike)
  const liked = usePlayerStore((s) => s.likedIds.includes(song.id))
  const openMenu = useContextMenuStore((s) => s.open)

  const artist = artistById.get(song.artistId)
  const album = albumById.get(song.albumId)
  const isCurrent = currentSongId === song.id

  const playFromHere = () => {
    if (isCurrent) {
      togglePlay()
      return
    }
    const ids = contextIds && contextIds.length > 0 ? contextIds : [song.id]
    const start = Math.max(0, ids.indexOf(song.id))
    playQueue(ids, start)
  }

  const openContextMenu = (e: MouseEvent, x?: number, y?: number) => {
    e.preventDefault()
    e.stopPropagation()
    openMenu(song.id, x ?? e.clientX, y ?? e.clientY, removeFromPlaylistId ?? null)
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={playFromHere}
      onContextMenu={openContextMenu}
      onKeyDown={(e) => {
        if (e.key === 'Enter') playFromHere()
      }}
      className="song-row group flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2 text-left outline-none transition-colors hover:bg-white/5 focus-visible:bg-white/5"
      aria-label={`Play ${song.title} by ${artist?.name ?? 'unknown artist'}`}
    >
      {/* Index / state */}
      <div className="flex w-7 shrink-0 items-center justify-center">
        {isCurrent ? (
          <>
            <span className="group-hover:hidden"><Equalizer /></span>
            <button
              className="hidden group-hover:flex h-7 w-7 items-center justify-center rounded-full text-white"
              onClick={(e) => { e.stopPropagation(); togglePlay() }}
              aria-label={isPlaying ? 'Pause' : 'Play'}
            >
              {isPlaying ? <Pause size={15} fill="currentColor" /> : <Play size={15} fill="currentColor" className="ml-0.5" />}
            </button>
          </>
        ) : (
          <>
            <span className="text-sm tabular-nums text-text-muted group-hover:hidden">{index ?? ''}</span>
            <Play size={15} fill="currentColor" className="hidden text-white group-hover:block" />
          </>
        )}
      </div>

      {/* Title cell */}
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <CoverImage src={album?.cover} alt="" className="song-thumb h-10 w-10 shrink-0 rounded-md" />
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <p className={`clamp-1 text-[15px] font-medium ${isCurrent ? 'text-brand-400' : 'text-text-primary'}`}>
              {song.title}
            </p>
            {song.explicit && showExplicit && <ExplicitBadge />}
          </div>
          <button
            onClick={(e) => { e.stopPropagation(); if (artist) navigate(`/artist/${artist.id}`) }}
            className="clamp-1 text-[13px] text-text-secondary transition-colors hover:text-white hover:underline text-left"
          >
            {artist?.name ?? 'Unknown artist'}
          </button>
        </div>
      </div>

      {/* Album */}
      {showAlbum && (
        <button
          onClick={(e) => { e.stopPropagation(); if (album) navigate(`/album/${album.id}`) }}
          className="hidden w-48 shrink-0 truncate text-left text-[13px] text-text-secondary transition-colors hover:text-white hover:underline xl:block"
        >
          {album?.title ?? ''}
        </button>
      )}

      {/* Like, duration, more */}
      <div className="flex shrink-0 items-center gap-2 sm:gap-3">
        <button
          onClick={(e) => { e.stopPropagation(); toggleLike(song.id) }}
          aria-label={liked ? 'Remove from Liked Songs' : 'Save to Liked Songs'}
          aria-pressed={liked}
          className={`rounded-full p-1.5 transition-all ${liked ? 'text-brand-500 opacity-100' : 'text-text-secondary opacity-0 hover:text-white group-hover:opacity-100 focus-visible:opacity-100'}`}
        >
          <Heart size={17} fill={liked ? 'currentColor' : 'none'} />
        </button>
        <span className="w-10 text-right text-[13px] tabular-nums text-text-muted">{formatDuration(song.duration)}</span>
        <button
          onClick={(e) => {
            e.stopPropagation()
            const rect = (e.currentTarget as HTMLButtonElement).getBoundingClientRect()
            openMenu(song.id, rect.right - 272, rect.bottom + 6, removeFromPlaylistId ?? null)
          }}
          onContextMenu={(e) => openContextMenu(e)}
          aria-label="More options"
          className="rounded-full p-1.5 text-text-secondary opacity-0 transition-all hover:text-white group-hover:opacity-100 focus-visible:opacity-100"
        >
          <MoreHorizontal size={18} />
        </button>
      </div>
    </div>
  )
}

export function SongTableHeader({ showAlbum = true }: { showAlbum?: boolean }) {
  return (
    <div className="flex items-center gap-3 border-b border-white/5 px-3 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-text-muted">
      <span className="w-7 text-center">#</span>
      <span className="flex-1">Title</span>
      {showAlbum && <span className="hidden w-48 xl:block">Album</span>}
      <span className="w-10 text-right">
        <Clock3 size={15} className="ml-auto" aria-label="Duration" />
      </span>
      <span className="w-[100px]" aria-hidden="true" />
    </div>
  )
}
