import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Playlist } from '../types'

const USER_GRADIENTS = [
  { from: 'from-indigo-600', to: 'to-slate-800' },
  { from: 'from-rose-600', to: 'to-purple-900' },
  { from: 'from-cyan-600', to: 'to-blue-900' },
  { from: 'from-lime-600', to: 'to-teal-900' },
]

interface UserState {
  displayName: string
  userPlaylists: Playlist[]
  playlistCounter: number
  followedArtistIds: string[]
  compactMode: boolean
  reduceMotion: boolean
  showExplicit: boolean
  premiumInterest: boolean
  streamQuality: '320' | '160' | '96'

  setDisplayName: (name: string) => void
  setPremiumInterest: (value: boolean) => void
  setStreamQuality: (quality: '320' | '160' | '96') => void
  createPlaylist: () => Playlist
  addSongToPlaylist: (playlistId: string, songId: string) => boolean
  removeSongFromPlaylist: (playlistId: string, songId: string) => void
  deletePlaylist: (playlistId: string) => void
  toggleFollowArtist: (artistId: string) => void
  toggleCompactMode: () => void
  toggleReduceMotion: () => void
  toggleShowExplicit: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set, get) => ({
      displayName: 'Karthik',
      userPlaylists: [],
      playlistCounter: 0,
      followedArtistIds: ['karthik-rao'],
      compactMode: false,
      reduceMotion: false,
      showExplicit: true,
      premiumInterest: false,
      streamQuality: '320',

      setDisplayName: (name) => set({ displayName: name.trim() || 'Karthik' }),
      setPremiumInterest: (value) => set({ premiumInterest: value }),
      setStreamQuality: (quality) => set({ streamQuality: quality }),

      createPlaylist: () => {
        const counter = get().playlistCounter + 1
        const gradient = USER_GRADIENTS[(counter - 1) % USER_GRADIENTS.length]
        const playlist: Playlist = {
          id: `user-${Date.now().toString(36)}-${counter}`,
          title: `My Playlist #${counter}`,
          description: 'Your personal collection — add songs with the ⋯ menu on any track.',
          curator: get().displayName,
          followers: 0,
          songIds: [],
          art: { type: 'gradient', from: gradient.from, to: gradient.to, icon: 'ListMusic' },
          userCreated: true,
        }
        set({ userPlaylists: [playlist, ...get().userPlaylists], playlistCounter: counter })
        return playlist
      },

      addSongToPlaylist: (playlistId, songId) => {
        const playlists = get().userPlaylists
        const target = playlists.find((p) => p.id === playlistId)
        if (!target) return false
        if (target.songIds.includes(songId)) return false
        set({
          userPlaylists: playlists.map((p) =>
            p.id === playlistId ? { ...p, songIds: [...p.songIds, songId] } : p,
          ),
        })
        return true
      },

      removeSongFromPlaylist: (playlistId, songId) =>
        set({
          userPlaylists: get().userPlaylists.map((p) =>
            p.id === playlistId ? { ...p, songIds: p.songIds.filter((id) => id !== songId) } : p,
          ),
        }),

      deletePlaylist: (playlistId) =>
        set({ userPlaylists: get().userPlaylists.filter((p) => p.id !== playlistId) }),

      toggleFollowArtist: (artistId) => {
        const ids = get().followedArtistIds
        set({
          followedArtistIds: ids.includes(artistId) ? ids.filter((id) => id !== artistId) : [...ids, artistId],
        })
      },

      toggleCompactMode: () => set({ compactMode: !get().compactMode }),
      toggleReduceMotion: () => set({ reduceMotion: !get().reduceMotion }),
      toggleShowExplicit: () => set({ showExplicit: !get().showExplicit }),
    }),
    { name: 'aud-user' },
  ),
)
