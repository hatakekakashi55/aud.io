import { useMemo } from 'react'
import { useCatalogStore } from '../store/catalogStore'
import { useUserStore } from '../store/userStore'
import type { Album, Artist, Playlist, Song } from '../types'

/** Catalog lookups + combined (curated + user) playlists, memoized per render. */
export function useCatalog() {
  const songs = useCatalogStore((s) => s.songs)
  const artists = useCatalogStore((s) => s.artists)
  const albums = useCatalogStore((s) => s.albums)
  const curatedPlaylists = useCatalogStore((s) => s.playlists)
  const userPlaylists = useUserStore((s) => s.userPlaylists)

  return useMemo(() => {
    const songById = new Map(songs.map((s) => [s.id, s]))
    const artistById = new Map(artists.map((a) => [a.id, a]))
    const albumById = new Map(albums.map((a) => [a.id, a]))
    const playlists: Playlist[] = [...userPlaylists, ...curatedPlaylists]
    const playlistById = new Map(playlists.map((p) => [p.id, p]))

    const songsOfAlbum = (albumId: string): Song[] =>
      songs.filter((s) => s.albumId === albumId)
    const songsByIds = (ids: string[]): Song[] =>
      ids.map((id) => songById.get(id)).filter((s): s is Song => Boolean(s))
    const albumsOfArtist = (artistId: string): Album[] =>
      albums.filter((a) => a.artistId === artistId)
    const songsOfArtist = (artistId: string): Song[] =>
      songs.filter((s) => s.artistId === artistId)
    const artistOf = (song: Song): Artist | undefined => artistById.get(song.artistId)
    const albumOf = (song: Song): Album | undefined => albumById.get(song.albumId)

    return {
      songs,
      artists,
      albums,
      playlists,
      curatedPlaylists,
      userPlaylists,
      songById,
      artistById,
      albumById,
      playlistById,
      songsOfAlbum,
      songsByIds,
      albumsOfArtist,
      songsOfArtist,
      artistOf,
      albumOf,
    }
  }, [songs, artists, albums, curatedPlaylists, userPlaylists])
}
