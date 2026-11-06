import { create } from 'zustand'

interface ContextMenuState {
  songId: string | null
  x: number
  y: number
  /** When set, the menu offers "Remove from this playlist". */
  removeFromPlaylistId: string | null
  open: (songId: string, x: number, y: number, removeFromPlaylistId?: string | null) => void
  close: () => void
}

export const useContextMenuStore = create<ContextMenuState>((set) => ({
  songId: null,
  x: 0,
  y: 0,
  removeFromPlaylistId: null,
  open: (songId, x, y, removeFromPlaylistId = null) =>
    set({ songId, x: Math.max(8, x), y: Math.max(8, y), removeFromPlaylistId }),
  close: () => set({ songId: null }),
}))
