import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell, Check, ChevronLeft, ChevronRight, Crown, Menu, Rocket, Sparkles, X, Keyboard } from 'lucide-react'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'

const NOTIFICATIONS = [
  { id: 'n1', icon: Rocket, title: 'AUD 1.0 is live', body: 'Queue, likes, user playlists and this new interface shipped today.' },
  { id: 'n2', icon: Keyboard, title: 'Speed through AUD', body: 'Space plays, L likes, M mutes, E expands the now-playing view.' },
  { id: 'n3', icon: Sparkles, title: 'Fresh drops weekly', body: 'Daily Discovery and Top Hits 2026 refresh automatically.' },
]

function PremiumModal({ onClose }: { onClose: () => void }) {
  const premiumInterest = useUserStore((s) => s.premiumInterest)
  const setPremiumInterest = useUserStore((s) => s.setPremiumInterest)
  const push = useToastStore((s) => s.push)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const perks = [
    'Ad-free, uninterrupted listening',
    'Lossless audio — 24-bit / 192 kHz',
    'Offline downloads for flights and metros',
    'Early access to new releases and sessions',
    'Cancel anytime, keep everything you liked',
  ]

  return (
    <div className="fixed inset-0 z-[65] flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-label="AUD Premium">
      <button className="absolute inset-0 bg-black/75 backdrop-blur-sm" onClick={onClose} aria-label="Close" />
      <div className="relative w-full max-w-md overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-b from-base-800 to-base-900 shadow-card animate-fade-up">
        <button onClick={onClose} className="absolute right-4 top-4 rounded-full p-1.5 text-text-muted hover:bg-white/5 hover:text-white" aria-label="Close">
          <X size={18} />
        </button>
        <div className="p-6 sm:p-7">
          <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-300 via-amber-500 to-orange-600 shadow-card">
            <Crown size={26} className="text-black" strokeWidth={2} />
          </span>
          <h2 className="mt-5 text-2xl font-black tracking-tight text-white">AUD Premium</h2>
          <p className="mt-1 text-sm text-text-secondary">Every song. Every skip. No compromises.</p>
          <ul className="mt-5 space-y-2.5">
            {perks.map((perk) => (
              <li key={perk} className="flex items-start gap-2.5 text-[13px] text-text-primary">
                <Check size={15} className="mt-0.5 shrink-0 text-brand-500" strokeWidth={3} />
                {perk}
              </li>
            ))}
          </ul>
          <button
            onClick={() => {
              setPremiumInterest(!premiumInterest)
              push(premiumInterest
                ? { icon: 'info', title: 'Removed from Premium launch list' }
                : { icon: 'check', title: "You're on the Premium launch list", description: "We'll ping you at launch" })
            }}
            className={`mt-6 flex w-full items-center justify-center gap-2 rounded-full py-3 text-sm font-extrabold transition-all ${
              premiumInterest
                ? 'border border-brand-500/40 bg-brand-500/10 text-brand-400'
                : 'bg-brand-500 text-black hover:bg-brand-400 hover:scale-[1.02]'
            }`}
          >
            {premiumInterest ? (<><Check size={16} strokeWidth={3} /> You're on the launch list</>) : 'Notify me at launch'}
          </button>
          <button onClick={onClose} className="mt-2 w-full rounded-full py-2.5 text-[13px] font-semibold text-text-muted transition-colors hover:text-white">
            Continue with Free
          </button>
        </div>
      </div>
    </div>
  )
}

export default function TopBar({ onMenu }: { onMenu: () => void }) {
  const navigate = useNavigate()
  const [premiumOpen, setPremiumOpen] = useState(false)
  const [bellOpen, setBellOpen] = useState(false)
  const bellRef = useRef<HTMLDivElement>(null)
  const push = useToastStore((s) => s.push)
  const displayName = useUserStore((s) => s.displayName)

  const initials = displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('')

  useEffect(() => {
    if (!bellOpen) return
    const onPointerDown = (e: PointerEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) setBellOpen(false)
    }
    window.addEventListener('pointerdown', onPointerDown)
    return () => window.removeEventListener('pointerdown', onPointerDown)
  }, [bellOpen])

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-2 bg-gradient-to-b from-base-950/95 via-base-950/75 to-transparent px-3 backdrop-blur-md sm:gap-3 sm:px-4 lg:px-6">
      <button onClick={onMenu} className="rounded-full p-2 text-text-secondary hover:text-white lg:hidden" aria-label="Open sidebar">
        <Menu size={20} />
      </button>

      <button onClick={() => navigate(-1)} className="hidden h-9 w-9 items-center justify-center rounded-full bg-black/60 text-text-secondary transition-colors hover:text-white sm:flex" aria-label="Go back">
        <ChevronLeft size={20} />
      </button>
      <button onClick={() => navigate(1)} className="hidden h-9 w-9 items-center justify-center rounded-full bg-black/60 text-text-secondary transition-colors hover:text-white sm:flex" aria-label="Go forward">
        <ChevronRight size={20} />
      </button>

      <div className="ml-auto flex items-center gap-2">
        <button
          onClick={() => setPremiumOpen(true)}
          className="hidden items-center gap-1.5 rounded-full bg-white px-4 py-2 text-[12.5px] font-extrabold text-black transition-transform hover:scale-[1.04] sm:flex"
        >
          <Crown size={14} strokeWidth={2.5} /> Explore Premium
        </button>

        {/* Notifications */}
        <div className="relative" ref={bellRef}>
          <button
            onClick={() => setBellOpen((v) => !v)}
            className="relative flex h-9 w-9 items-center justify-center rounded-full bg-black/60 text-text-secondary transition-colors hover:text-white"
            aria-label="Notifications"
            aria-expanded={bellOpen}
          >
            <Bell size={18} />
            <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-brand-500 ring-2 ring-base-950" aria-hidden="true" />
          </button>
          {bellOpen && (
            <div className="absolute right-0 top-11 w-[340px] overflow-hidden rounded-2xl border border-white/10 bg-base-750/95 shadow-card backdrop-blur-2xl animate-fade-up" role="dialog" aria-label="Notifications">
              <div className="flex items-center justify-between border-b border-white/5 px-4 py-3">
                <span className="text-[13px] font-bold text-white">What's new</span>
                <span className="rounded-full bg-brand-500/15 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-brand-400">3 new</span>
              </div>
              <div className="p-1.5">
                {NOTIFICATIONS.map(({ id, icon: Icon, title, body }) => (
                  <button
                    key={id}
                    onClick={() => { setBellOpen(false); push({ icon: 'info', title }) }}
                    className="flex w-full items-start gap-3 rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-white/5"
                  >
                    <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-500/12 text-brand-400">
                      <Icon size={16} />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-[13px] font-semibold text-text-primary">{title}</span>
                      <span className="mt-0.5 block text-[11.5px] leading-snug text-text-secondary">{body}</span>
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        <button onClick={() => navigate('/settings')} className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-emerald-900 text-[12px] font-extrabold text-white ring-1 ring-white/10 transition-transform hover:scale-105" aria-label="Your profile and settings" title={displayName}>
          {initials || 'A'}
        </button>
      </div>

      {premiumOpen && <PremiumModal onClose={() => setPremiumOpen(false)} />}
    </header>
  )
}
