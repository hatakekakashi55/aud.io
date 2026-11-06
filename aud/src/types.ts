export type PlaylistArt =
  | { type: 'image'; src: string }
  | { type: 'gradient'; from: string; to: string; icon: string }

export interface Artist {
  id: string
  name: string
  image: string | null
  genres: string[]
  monthlyListeners: number
  followers: number
  bio: string
  verified: boolean
}

export interface Album {
  id: string
  title: string
  artistId: string
  year: number
  cover: string | null
  type: 'album' | 'single' | 'ep'
}

export interface Song {
  id: string
  title: string
  artistId: string
  albumId: string
  duration: number // seconds
  audioUrl: string
  plays: number
  explicit: boolean
  genre: string
  releaseDate: string
}

export interface Playlist {
  id: string
  title: string
  description: string
  curator: string
  followers: number
  songIds: string[]
  art: PlaylistArt
  userCreated?: boolean
}

export type RepeatMode = 'off' | 'all' | 'one'

export interface CatalogData {
  artists: Artist[]
  albums: Album[]
  songs: Song[]
  playlists: Playlist[]
}
