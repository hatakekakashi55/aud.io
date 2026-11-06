import { Check, Heart, Info, Link2, ListPlus, Plus, Trash2, User, X } from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'
import { useToastStore, type ToastIcon } from '../store/toastStore'

const ICONS: Record<ToastIcon, typeof Check> = {
  check: Check,
  heart: Heart,
  queue: ListPlus,
  link: Link2,
  trash: Trash2,
  info: Info,
  plus: Plus,
  user: User,
}

export default function Toasts() {
  const toasts = useToastStore((s) => s.toasts)
  const dismiss = useToastStore((s) => s.dismiss)

  if (toasts.length === 0) return null

  return (
    <div className="fixed bottom-28 left-1/2 -translate-x-1/2 z-[70] flex flex-col items-center gap-2 w-full max-w-md px-4 pointer-events-none">
      <AnimatePresence>
      {toasts.map((toast) => {
        const Icon = ICONS[toast.icon]
        return (
          <motion.div
            key={toast.id}
            layout
            role="status"
            initial={{ opacity: 0, y: 22, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 12, scale: 0.96 }}
            transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            className="pointer-events-auto w-full flex items-center gap-3 rounded-xl border border-white/10 bg-base-750/95 backdrop-blur-xl shadow-card px-4 py-3"
          >
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-500/15 text-brand-400">
              <Icon size={16} strokeWidth={2.2} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-text-primary clamp-1">{toast.title}</p>
              {toast.description && <p className="text-xs text-text-secondary clamp-1">{toast.description}</p>}
            </div>
            <button
              onClick={() => dismiss(toast.id)}
              className="shrink-0 rounded-full p-1.5 text-text-muted hover:text-text-primary hover:bg-white/5 transition-colors"
              aria-label="Dismiss notification"
            >
              <X size={14} />
            </button>
          </motion.div>
        )
      })}
      </AnimatePresence>
    </div>
  )
}
