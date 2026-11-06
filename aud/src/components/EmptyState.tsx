import type { LucideIcon } from 'lucide-react'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
}

export default function EmptyState({ icon: Icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
      <span className="flex h-20 w-20 items-center justify-center rounded-3xl bg-white/[0.05] ring-1 ring-white/5">
        <Icon size={30} className="text-text-muted" strokeWidth={1.6} />
      </span>
      <h2 className="text-lg font-extrabold text-text-primary">{title}</h2>
      {description && <p className="max-w-sm text-[13px] leading-relaxed text-text-secondary">{description}</p>}
      {actionLabel && onAction && (
        <button onClick={onAction} className="mt-2 rounded-full bg-brand-500 px-5 py-2.5 text-[13px] font-extrabold text-black transition-all hover:scale-105 hover:bg-brand-400">
          {actionLabel}
        </button>
      )}
    </div>
  )
}
