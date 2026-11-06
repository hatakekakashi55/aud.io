import { useCallback, useRef, useState } from 'react'
import { usePlayerStore } from '../store/playerStore'
import { formatDuration } from '../utils/format'

/** Custom scrubber — drag to seek, arrow keys nudge ±5s. */
export default function SeekBar({ className = '', thick = false }: { className?: string; thick?: boolean }) {
  const position = usePlayerStore((s) => s.position)
  const duration = usePlayerStore((s) => s.duration)
  const seekTo = usePlayerStore((s) => s.seekTo)

  const trackRef = useRef<HTMLDivElement>(null)
  const [scrubRatio, setScrubRatio] = useState<number | null>(null)

  const total = Number.isFinite(duration) && duration > 0 ? duration : 0
  const shownPosition = scrubRatio !== null ? scrubRatio * total : Math.min(position, total || position)
  const ratio = total > 0 ? Math.min(1, Math.max(0, shownPosition / total)) : 0

  const ratioFromClientX = useCallback((clientX: number) => {
    const el = trackRef.current
    if (!el) return 0
    const rect = el.getBoundingClientRect()
    return Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
  }, [])

  const commit = useCallback(
    (r: number) => {
      if (total > 0) seekTo(r * total)
      setScrubRatio(null)
    },
    [seekTo, total],
  )

  const height = thick ? 'h-1.5' : 'h-1'

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <span className="w-9 text-right text-[11px] tabular-nums leading-none text-text-muted">
        {formatDuration(shownPosition)}
      </span>
      <div
        ref={trackRef}
        role="slider"
        tabIndex={0}
        aria-label="Seek"
        aria-valuemin={0}
        aria-valuemax={Math.round(total)}
        aria-valuenow={Math.round(shownPosition)}
        aria-valuetext={formatDuration(shownPosition)}
        className="group relative h-4 flex-1 cursor-pointer touch-none flex items-center outline-none"
        onPointerDown={(e) => {
          e.preventDefault()
          const r = ratioFromClientX(e.clientX)
          setScrubRatio(r)
          e.currentTarget.setPointerCapture(e.pointerId)
        }}
        onPointerMove={(e) => {
          if (scrubRatio !== null) setScrubRatio(ratioFromClientX(e.clientX))
        }}
        onPointerUp={(e) => {
          if (scrubRatio !== null) commit(ratioFromClientX(e.clientX))
        }}
        onPointerCancel={() => setScrubRatio(null)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowLeft') seekTo(Math.max(0, shownPosition - 5))
          else if (e.key === 'ArrowRight') seekTo(Math.min(total, shownPosition + 5))
        }}
      >
        <div className={`relative w-full ${height} rounded-full bg-base-600 overflow-visible`}>
          <div className="absolute inset-y-0 left-0 rounded-full bg-brand-500 group-hover:bg-brand-400 transition-colors" style={{ width: `${ratio * 100}%` }} />
          <div
            className={`absolute top-1/2 -translate-y-1/2 -translate-x-1/2 h-3 w-3 rounded-full bg-white shadow transition-opacity ${scrubRatio !== null ? 'opacity-100 scale-110' : 'opacity-0 group-hover:opacity-100'}`}
            style={{ left: `${ratio * 100}%` }}
          />
        </div>
      </div>
      <span className="w-9 text-[11px] tabular-nums leading-none text-text-muted">{formatDuration(total)}</span>
    </div>
  )
}
