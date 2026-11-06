import { useNavigate, useParams } from 'react-router-dom'
import { BadgeCheck, Link2, Mic, Play, Shuffle, UserCheck, UserPlus } from 'lucide-react'
import { useCatalog } from '../hooks/useCatalog'
import { usePlayerStore } from '../store/playerStore'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'
import SongRow from '../components/SongRow'
import MediaCard from '../components/MediaCard'
import EmptyState from '../components/EmptyState'
import { ArtistAvatar, CoverImage } from '../components/artwork'
import { formatCount, randomIndex } from '../utils/format'

export default function ArtistDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { artistById, songsOfArtist, albumsOfArtist, songsOfAlbum } = useCatalog()
  const playQueue = usePlayerStore((s) => s.playQueue)
  const shuffle = usePlayerStore((s) => s.shuffle)
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle)
  const followedArtistIds = useUserStore((s) => s.followedArtistIds)
  const toggleFollowArtist = useUserStore((s) => s.toggleFollowArtist)
  const push = useToastStore((s) => s.push)

  const artist = id ? artistById.get(id) : undefined

  if (!artist) {
    return (
      <EmptyState
        icon={Mic}
        title="Artist not found"
        description="This artist isn't in the catalog."
        actionLabel="Back to search"
        onAction={() => navigate('/search')}
      />
    )
  }

  const artistSongs = songsOfArtist(artist.id)
  const topSongs = [...artistSongs].sort((a, b) => b.plays - a.plays)
  const topFive = topSongs.slice(0, 5)
  const topIds = topSongs.map((s) => s.id)
  const artistAlbums = albumsOfArtist(artist.id).sort((a, b) => b.year - a.year)
  const following = followedArtistIds.includes(artist.id)

  const playAll = () => {
    if (topIds.length > 0) playQueue(topIds, 0)
  }

  const shufflePlay = () => {
    if (topIds.length === 0) return
    if (!shuffle) toggleShuffle()
    playQueue(topIds, randomIndex(topIds.length))
    push({ icon: 'info', title: `Shuffling ${artist.name}` })
  }

  const shareArtist = async () => {
    const url = `${window.location.origin}${window.location.pathname}#/artist/${artist.id}`
    try {
      await navigator.clipboard.writeText(url)
      push({ icon: 'link', title: 'Artist link copied' })
    } catch {
      push({ icon: 'info', title: 'Could not copy link' })
    }
  }

  return (
    <div className="pb-12">
      {/* Header */}
      <div className="relative h-60 overflow-hidden sm:h-80">
        {artist.image ? (
          <img src={artist.image} alt={artist.name} className="absolute inset-0 h-full w-full object-cover object-[50%_22%]" />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-brand-700/40 via-base-850 to-base-950">
            <ArtistAvatar name={artist.name} image={null} className="h-36 w-36 text-5xl ring-1 ring-white/10" />
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-base-950 via-base-950/25 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 px-5 pb-5 sm:px-7">
          {artist.verified && (
            <p className="flex items-center gap-1.5 text-[11.5px] font-bold uppercase tracking-[0.18em] text-sky-400">
              <BadgeCheck size={15} strokeWidth={2.5} /> Verified Artist
            </p>
          )}
          <h1 className="mt-1 text-4xl font-black tracking-tight text-white drop-shadow sm:text-6xl xl:text-7xl">{artist.name}</h1>
          <p className="mt-1.5 text-[12.5px] font-semibold text-white/85">{formatCount(artist.monthlyListeners)} monthly listeners</p>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3 px-5 py-5 sm:px-7">
        <button onClick={playAll} className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-500 text-black shadow-glow transition-all hover:scale-105 hover:bg-brand-400 active:scale-95" aria-label={`Play ${artist.name}`}>
          <Play size={24} fill="currentColor" className="ml-0.5" />
        </button>
        <button onClick={shufflePlay} className="flex h-12 w-12 items-center justify-center rounded-full text-text-secondary transition-all hover:scale-105 hover:text-white" aria-label="Shuffle play" title="Shuffle play">
          <Shuffle size={22} />
        </button>
        <button
          onClick={() => {
            toggleFollowArtist(artist.id)
            push({ icon: 'user', title: following ? `Unfollowed ${artist.name}` : `Following ${artist.name}`, description: following ? undefined : 'New releases will surface in your library' })
          }}
          className={`flex items-center gap-2 rounded-full border px-5 py-2 text-[11.5px] font-extrabold uppercase tracking-[0.16em] transition-all ${following ? 'border-brand-500/50 text-brand-400 hover:border-brand-400' : 'border-white/25 text-white hover:scale-105 hover:border-white'}`}
          aria-pressed={following}
        >
          {following ? <UserCheck size={15} strokeWidth={2.5} /> : <UserPlus size={15} strokeWidth={2.5} />}
          {following ? 'Following' : 'Follow'}
        </button>
        <button onClick={shareArtist} className="ml-auto rounded-full p-2.5 text-text-muted transition-colors hover:bg-white/5 hover:text-white" aria-label="Copy artist link" title="Copy link to artist">
          <Link2 size={19} />
        </button>
      </div>

      {/* Popular */}
      <section className="px-2 sm:px-4">
        <h2 className="mb-2 px-3 text-xl font-extrabold tracking-tight text-white sm:px-3 sm:text-2xl">Popular</h2>
        {topFive.map((song, i) => (
          <SongRow key={song.id} song={song} index={i + 1} contextIds={topFive.map((s) => s.id)} />
        ))}
      </section>

      {/* Albums & singles */}
      {artistAlbums.length > 0 && (
        <section className="px-5 sm:px-7">
          <h2 className="mb-3 mt-8 text-xl font-extrabold tracking-tight text-white sm:text-2xl">Albums &amp; singles</h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
            {artistAlbums.map((album) => (
              <MediaCard
                key={album.id}
                art={<CoverImage src={album.cover} alt={`${album.title} cover`} className="aspect-square w-full" />}
                title={album.title}
                subtitle={`${album.year} · ${album.type === 'album' ? 'Album' : album.type === 'ep' ? 'EP' : 'Single'}`}
                onPlay={() => {
                  const ids = songsOfAlbum(album.id).map((s) => s.id)
                  if (ids.length) playQueue(ids, 0)
                }}
                onClick={() => navigate(`/album/${album.id}`)}
              />
            ))}
          </div>
        </section>
      )}

      {/* About */}
      <section className="px-5 sm:px-7">
        <h2 className="mb-3 mt-8 text-xl font-extrabold tracking-tight text-white sm:text-2xl">About</h2>
        <div className="relative overflow-hidden rounded-3xl border border-white/5">
          <div className="relative h-44 sm:h-56">
            {artist.image ? (
              <img src={artist.image} alt="" aria-hidden="true" className="absolute inset-0 h-full w-full object-cover object-[50%_15%]" />
            ) : (
              <div className="absolute inset-0 bg-gradient-to-br from-brand-700/50 to-base-850" />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-base-850 via-base-850/35 to-transparent" />
            <div className="absolute bottom-4 left-5 sm:left-6">
              <p className="text-3xl font-black text-white sm:text-4xl">{formatCount(artist.monthlyListeners)}</p>
              <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-white/70">Monthly listeners</p>
            </div>
          </div>
          <div className="bg-base-850 p-5 sm:p-6">
            <p className="max-w-3xl text-[13px] leading-relaxed text-text-secondary">{artist.bio}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              {artist.genres.map((genre) => (
                <span key={genre} className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[11px] font-semibold text-text-secondary">
                  {genre}
                </span>
              ))}
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[11px] font-semibold text-text-secondary">
                {formatCount(artist.followers)} followers
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
