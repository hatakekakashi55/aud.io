import { useEffect } from 'react'
import { usePlayerStore } from '../store/playerStore'

/**
 * Global shortcuts:
 *   Space  play/pause        L  like current track
 *   M      mute              E  toggle full-screen player
 *   Shift+→  next track      Shift+←  previous track
 *   Escape  collapse full-screen player
 */
export default function useKeyboardShortcuts() {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      const tag = target?.tagName
      const typing =
        tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || target?.isContentEditable === true

      const player = usePlayerStore.getState()

      if (e.key === 'Escape') {
        if (player.expanded) {
          e.preventDefault()
          player.setExpanded(false)
        }
        return
      }
      if (typing) return

      switch (e.code) {
        case 'Space':
          e.preventDefault()
          player.togglePlay()
          break
        case 'KeyL':
          if (player.currentSongId) player.toggleLike(player.currentSongId)
          break
        case 'KeyM':
          player.toggleMute()
          break
        case 'KeyE':
          e.preventDefault()
          player.setExpanded(!player.expanded)
          break
        case 'ArrowRight':
          if (e.shiftKey) {
            e.preventDefault()
            player.next(false)
          }
          break
        case 'ArrowLeft':
          if (e.shiftKey) {
            e.preventDefault()
            player.prev()
          }
          break
        default:
          break
      }
    }

    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])
}
