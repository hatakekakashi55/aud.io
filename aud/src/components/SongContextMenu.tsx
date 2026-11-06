import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Check,
  ChevronRight,
  Disc3,
  Heart,
  Link2,
  ListEnd,
  ListMusic,
  ListPlus,
  Mic,
  Plus,
  Trash2,
} from 'lucide-react'
import { useContextMenuStore } from '../store/contextMenuStore'
import { usePlayerStore } from '../store/playerStore'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'
import { useCatalog } from '../hooks/useCatalog'
import { PlaylistArtwork } from './artwork'

export default function SongContextMenu() {
  const { songId, x, y, removeFromPlaylistId, close } = useContextMenuStore()
  const navigate = useNavigate()
  const { songById, artistById, albumById } = useCatalog()
  const playNext = usePlayerStore((s) => s.playNext)
  const addToQueue = usePlayerStore((s) => s.addToQueue)
  const toggleLike = usePlayerStore((s) => s.toggleLike)
  const liked = usePlayerStore((s) => (songId ? s.likedIds.includes(songId) : false))
  const userPlaylists = useUserStore((s) => s.userPlaylists)
  const addSongToPlaylist = useUserStore((s) => s.addSongToPlaylist)
  const createPlaylist = useUserStore((s) => s.createPlaylist)
  const removeSongFromPlaylist = useUserStore((s) => s.removeSongFromPlaylist)
  const push = useToastStore((s) => s.push)
  const menuRef = useRef<HTMLDivElement>(null)
  const [subOpenFor, setSubOpenFor] = useState<string | null>(null)
  const subOpen = subOpenFor === songId
  const toggleSub = () => setSubOpenFor((cur) => (cur === songId ? null : songId))

  const song = songId ? songById.get(songId) : undefined

  useEffect(() => {
    if (!songId) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close()
    }
    const onScroll = () => close()
    window.addEventListener('keydown', onKey)
    window.addEventListener('scroll', onScroll, true)
    window.addEventListener('resize', onScroll)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('resize', onScroll)
    }
  }, [songId, close])

  if (!songId || !song) return null

  const artist = artistById.get(song.artistId)
  const album = albumById.get(song.albumId)
  const MENU_W = 272
  const MENU_H = 430
  const left = Math.min(Math.max(8, x), window.innerWidth - MENU_W - 8)
  const top = Math.min(Math.max(8, y), window.innerHeight - MENU_H - 8)
  const submenuSide = left > window.innerWidth - MENU_W - 240 ? 'right' : 'left'

  const item =
    'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-[13px] font-medium text-text-primary transition-colors hover:bg-white/10'
  const icon = 'h-4 w-4 shrink-0 text-text-secondary'

  const copyLink = async () => {
    const url = `${window.location.origin}${window.location.pathname}#/artist/${song.artistId}`
    try {
      await navigator.clipboard.writeText(url)
      push({ icon: 'link', title: 'Link copied to clipboard', description: url })
    } catch {
      push({ icon: 'info', title: 'Could not copy link', description: 'Clipboard access was denied' })
    }
    close()
  }

  const removableFrom = removeFromPlaylistId && userPlaylists.some((p) => p.id === removeFromPlaylistId) ? removeFromPlaylistId : null

  return (
    <div className="fixed inset-0 z-[60]" onPointerDown={close} onContextMenu={(e) => { e.preventDefault(); close() }}>
      <div
        ref={menuRef}
        role="menu"
        aria-label={`Options for ${song.title}`}
        className="absolute w-[272px] rounded-2xl border border-white/10 bg-base-750/95 p-1.5 shadow-card backdrop-blur-2xl animate-fade-up"
        style={{ left, top }}
        onPointerDown={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="border-b border-white/5 px-3 py-2.5">
          <p className="clamp-1 text-[13px] font-bold text-text-primary">{song.title}</p>
          <p className="clamp-1 text-[11px] text-text-secondary">{artist?.name}</p>
        </div>

        <div className="py-1.5">
          <button className={item} onClick={() => { playNext(song.id); push({ icon: 'queue', title: 'Will play next', description: song.title }); close() }}>
            <ListEnd className={icon} /> Play next
          </button>
          <button className={item} onClick={() => { addToQueue(song.id); push({ icon: 'queue', title: 'Added to queue', description: song.title }); close() }}>
            <ListPlus className={icon} /> Add to queue
          </button>
          <button className={item} onClick={() => { toggleLike(song.id); push({ icon: 'heart', title: liked ? 'Removed from Liked Songs' : 'Saved to Liked Songs', description: song.title }); close() }}>
            <Heart className={icon} fill={liked ? 'currentColor' : 'none'} /> {liked ? 'Remove from Liked Songs' : 'Save to Liked Songs'}
          </button>
        </div>

        <div className="border-t border-white/5 py-1.5">
          {/* Add to playlist — hover submenu */}
          <div className="relative group/sub">
            <button className={`${item} justify-between`} aria-haspopup="true" aria-expanded={subOpen} onClick={toggleSub}>
              <span className="flex items-center gap-3"><ListMusic className={icon} /> Add to playlist</span>
              <ChevronRight className={`h-4 w-4 text-text-muted transition-transform ${subOpen ? 'rotate-90' : ''}`} />
            </button>
            <div
              className={`absolute top-0 w-64 rounded-2xl border border-white/10 bg-base-750/95 p-1.5 shadow-card backdrop-blur-2xl group-hover/sub:block ${subOpen ? 'block' : 'hidden'} ${submenuSide === 'left' ? 'left-full ml-1' : 'right-full mr-1'}`}
              role="menu"
            >
              <button
                className={item}
                onClick={() => {
                  const playlist = createPlaylist()
                  addSongToPlaylist(playlist.id, song.id)
                  push({ icon: 'plus', title: `Added to ${playlist.title}`, description: song.title })
                  close()
                }}
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-md border border-dashed border-white/25"><Plus size={15} /></span>
                New playlist
              </button>
              {userPlaylists.length > 0 && <div className="mx-3 my-1.5 border-t border-white/5" />}
              <div className="max-h-44 overflow-y-auto nice-scroll">
                {userPlaylists.map((playlist) => {
                  const already = playlist.songIds.includes(song.id)
                  return (
                    <button
                      key={playlist.id}
                      className={item}
                      onClick={() => {
                        if (already) {
                          push({ icon: 'info', title: 'Already in that playlist', description: playlist.title })
                        } else {
                          addSongToPlaylist(playlist.id, song.id)
                          push({ icon: 'plus', title: `Added to ${playlist.title}`, description: song.title })
                        }
                        close()
                      }}
                    >
                      <PlaylistArtwork art={playlist.art} size={28} className="rounded-md" />
                      <span className="min-w-0 flex-1 truncate text-left">{playlist.title}</span>
                      {already && <Check size={15} className="shrink-0 text-brand-500" />}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>

          <button className={item} onClick={copyLink}>
            <Link2 className={icon} /> Copy link
          </button>
          <button className={item} onClick={() => { if (artist) navigate(`/artist/${artist.id}`); close() }}>
            <Mic className={icon} /> Go to artist
          </button>
          <button className={item} onClick={() => { if (album) navigate(`/album/${album.id}`); close() }}>
            <Disc3 className={icon} /> Go to album
          </button>
        </div>

        {removableFrom && (
          <div className="border-t border-white/5 py-1.5">
            <button
              className={`${item} !text-red-400`}
              onClick={() => {
                removeSongFromPlaylist(removableFrom, song.id)
                push({ icon: 'trash', title: 'Removed from playlist', description: song.title })
                close()
              }}
            >
              <Trash2 className="h-4 w-4 shrink-0 text-red-400" /> Remove from this playlist
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
