import { useState } from 'react'
import { Check, Crown, FlaskConical, Keyboard, RefreshCw, Server, Settings2, SlidersHorizontal, UserRound } from 'lucide-react'
import { config } from '../config'
import { useCatalogStore } from '../store/catalogStore'
import { useUserStore } from '../store/userStore'
import { useToastStore } from '../store/toastStore'

function Switch({ checked, onToggle, label, description }: { checked: boolean; onToggle: () => void; label: string; description: string }) {
  return (
    <button
      role="switch"
      aria-checked={checked}
      onClick={onToggle}
      className="flex w-full items-center justify-between gap-6 rounded-2xl border border-white/5 bg-white/[0.03] px-4 py-3.5 text-left transition-colors hover:bg-white/[0.05]"
    >
      <span className="min-w-0">
        <span className="block text-[13.5px] font-semibold text-text-primary">{label}</span>
        <span className="mt-0.5 block text-[11.5px] leading-snug text-text-muted">{description}</span>
      </span>
      <span className={`relative w-11 shrink-0 rounded-full transition-colors duration-200 ${checked ? 'bg-brand-500' : 'bg-base-600'}`} style={{ height: 26 }}>
        <span className={`absolute left-0.5 top-0.5 h-[22px] w-[22px] rounded-full bg-white shadow transition-transform duration-200 ${checked ? 'translate-x-[18px]' : 'translate-x-0'}`} />
      </span>
    </button>
  )
}

function SectionTitle({ icon: Icon, children }: { icon: typeof UserRound; children: string }) {
  return (
    <h2 className="mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.22em] text-text-muted">
      <Icon size={13} /> {children}
    </h2>
  )
}

const SHORTCUTS: [string, string][] = [
  ['Space', 'Play / pause'],
  ['Shift + →', 'Next track'],
  ['Shift + ←', 'Previous track'],
  ['L', 'Like current track'],
  ['M', 'Mute / unmute'],
  ['E', 'Full-screen player'],
  ['Esc', 'Close overlays'],
]

export default function Settings() {
  const source = useCatalogStore((s) => s.source)
  const refreshLiveCatalog = useCatalogStore((s) => s.refreshLiveCatalog)
  const streamQuality = useUserStore((s) => s.streamQuality)
  const setStreamQuality = useUserStore((s) => s.setStreamQuality)
  const [refreshing, setRefreshing] = useState(false)
  const isLive = source === 'api'

  const doRefresh = async () => {
    setRefreshing(true)
    try {
      await refreshLiveCatalog()
      push({ icon: 'check', title: 'Catalog refreshed', description: 'Latest tracks pulled from BeatBox' })
    } catch {
      push({ icon: 'info', title: 'Refresh failed', description: 'Couldn’t reach BeatBox — showing cached catalog' })
    } finally {
      setRefreshing(false)
    }
  }
  const {
    displayName,
    setDisplayName,
    compactMode,
    reduceMotion,
    showExplicit,
    toggleCompactMode,
    toggleReduceMotion,
    toggleShowExplicit,
    premiumInterest,
    setPremiumInterest,
  } = useUserStore()
  const push = useToastStore((s) => s.push)

  const [nameDraft, setNameDraft] = useState(displayName)
  const [lastDisplayName, setLastDisplayName] = useState(displayName)
  if (lastDisplayName !== displayName) {
    // React-endorsed derived-state sync (no effect needed)
    setLastDisplayName(displayName)
    setNameDraft(displayName)
  }
  const commitName = () => {
    const trimmed = nameDraft.trim()
    if (trimmed && trimmed !== displayName) {
      setDisplayName(trimmed)
      push({ icon: 'check', title: 'Profile updated', description: `We'll call you ${trimmed}` })
    } else {
      setNameDraft(displayName)
    }
  }

  const initials = displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('')

  return (
    <div className="mx-auto w-full max-w-3xl px-5 pb-16 pt-6 sm:px-6">
      <h1 className="flex items-center gap-3 text-2xl font-black tracking-tight text-white sm:text-[28px]">
        <Settings2 size={24} className="text-text-muted" /> Settings
      </h1>

      {/* ── Profile ─────────────────────────────────── */}
      <section className="mt-7">
        <SectionTitle icon={UserRound}>Profile</SectionTitle>
        <div className="flex flex-col gap-5 rounded-3xl border border-white/5 bg-white/[0.03] p-5 sm:flex-row sm:items-center">
          <span className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-emerald-900 text-2xl font-extrabold text-white">
            {initials || 'A'}
          </span>
          <div className="min-w-0 flex-1">
            <label htmlFor="display-name" className="text-[10.5px] font-bold uppercase tracking-[0.18em] text-text-muted">Display name</label>
            <input
              id="display-name"
              value={nameDraft}
              onChange={(e) => setNameDraft(e.target.value)}
              onBlur={commitName}
              onKeyDown={(e) => e.key === 'Enter' && (e.target as HTMLInputElement).blur()}
              className="mt-1 h-11 w-full max-w-xs rounded-xl border border-white/10 bg-base-850 px-3.5 text-[14px] font-semibold text-text-primary outline-none transition-colors focus:border-brand-500/60"
            />
            <div className="mt-2 flex items-center gap-2">
              <span className="flex items-center gap-1.5 rounded-full bg-white/5 px-2.5 py-1 text-[10.5px] font-bold text-text-secondary">
                <Crown size={11} className="text-amber-400" /> AUD Free
              </span>
              {premiumInterest && <span className="text-[10.5px] font-semibold text-brand-400">Premium waitlist joined</span>}
            </div>
          </div>
        </div>
      </section>

      {/* ── Music service ───────────────────────────── */}
      <section className="mt-7">
        <SectionTitle icon={Server}>Music service</SectionTitle>
        <div className="overflow-hidden rounded-3xl border border-white/5 bg-white/[0.03]">
          <div className="flex items-start gap-4 p-5">
            <span className={`mt-0.5 flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${isLive ? 'bg-brand-500/12 text-brand-400' : 'bg-amber-400/10 text-amber-400'}`}>
              {isLive ? <Server size={20} /> : <FlaskConical size={20} />}
            </span>
            <div className="min-w-0 flex-1">
              <p className="flex flex-wrap items-center gap-2 text-[14px] font-bold text-text-primary">
                {isLive ? 'Live — BeatBox Worker (JioSaavn)' : 'Running on the built-in demo catalog'}
                {isLive && (
                  <span className="flex items-center gap-1.5 rounded-full bg-brand-500/10 px-2 py-0.5 text-[9.5px] font-bold uppercase tracking-[0.18em] text-brand-400">
                    <span className="h-1.5 w-1.5 rounded-full bg-brand-500" /> Connected
                  </span>
                )}
              </p>
              <p className="mt-1 text-[12px] leading-relaxed text-text-secondary">
                {isLive
                  ? 'Songs, artwork and streams are fetched in real time from the BeatBox API. Search on the Search tab pulls fresh tracks straight from Saavn.'
                  : 'Point AUD at the BeatBox API and every screen runs on live data — no code changes required.'}
              </p>
              <code className="mt-2 inline-block select-text rounded-lg border border-white/5 bg-black/40 px-2.5 py-1 text-[10.5px] text-emerald-200/70">{config.apiUrl || 'VITE_MUSIC_API_URL is not set'}</code>

              {isLive && (
                <>
                  <div className="mt-3 flex items-center gap-2">
                    <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-text-muted">Stream quality</span>
                    <div className="flex items-center gap-1 rounded-full border border-white/10 bg-base-850 p-1">
                      {(['320', '160', '96'] as const).map((q) => (
                        <button
                          key={q}
                          onClick={() => { setStreamQuality(q); push({ icon: 'check', title: `Stream quality set to ${q} kbps` }) }}
                          aria-pressed={streamQuality === q}
                          className={`rounded-full px-3 py-1.5 text-[11px] font-bold transition-all ${streamQuality === q ? 'bg-brand-500 text-black' : 'text-text-secondary hover:text-white'}`}
                        >
                          {q} kbps
                        </button>
                      ))}
                    </div>
                  </div>
                  <button
                    onClick={doRefresh}
                    disabled={refreshing}
                    className="mt-3 flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-[11.5px] font-bold text-text-secondary transition-all hover:border-brand-500/40 hover:text-white disabled:opacity-50"
                  >
                    <RefreshCw size={13} className={refreshing ? 'animate-spin' : ''} />
                    {refreshing ? 'Refreshing catalog…' : 'Refresh catalog from BeatBox'}
                  </button>
                </>
              )}
            </div>
          </div>
          <div className="border-t border-white/5 bg-black/40 p-5">
            <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-text-muted">Wire-up</p>
            <pre className="mt-2 select-text overflow-x-auto text-[11px] leading-relaxed text-emerald-200/75">{`# .env (then restart the dev server)
VITE_MUSIC_API_URL=https://aud29.thangamprabhu1.workers.dev
VITE_CATALOG_MODE=auto        # auto | beatbox | rest

# BeatBox (JioSaavn-compatible) — used automatically:
GET {BASE}/api/search/songs?query=anirudh   → tracks + 320kbps streams
GET {BASE}/api/search?query=vaanam          → combined search
GET {BASE}/api/playlists?id=<numeric>       → playlist detail

# Or the generic REST contract (VITE_CATALOG_MODE=rest):
GET {BASE}/songs    GET {BASE}/artists    GET {BASE}/albums    GET {BASE}/playlists`}</pre>
            <p className="mt-3 text-[11px] leading-relaxed text-text-muted">
              BeatBox needs CORS enabled for browser access — if the worker ever drops it, the app shows a retry screen instead of fake data, and you can continue on the demo catalog one tap away.
            </p>
          </div>
        </div>
      </section>

      {/* ── Playback & appearance ───────────────────── */}
      <section className="mt-7">
        <SectionTitle icon={SlidersHorizontal}>Playback &amp; appearance</SectionTitle>
        <div className="space-y-2">
          <Switch checked={compactMode} onToggle={toggleCompactMode} label="Compact mode" description="Denser track rows and artwork — more music per screen." />
          <Switch checked={reduceMotion} onToggle={toggleReduceMotion} label="Reduce motion" description="Disables equalizer animations, transitions and parallax." />
          <Switch checked={showExplicit} onToggle={toggleShowExplicit} label="Show explicit badges" description="Mark tracks with explicit lyrics throughout the app." />
        </div>
      </section>

      {/* ── Premium ─────────────────────────────────── */}
      <section className="mt-7">
        <SectionTitle icon={Crown}>AUD Premium</SectionTitle>
        <button
          onClick={() => {
            setPremiumInterest(!premiumInterest)
            push(premiumInterest ? { icon: 'info', title: 'Removed from the Premium waitlist' } : { icon: 'check', title: "You're on the Premium waitlist" })
          }}
          className="group flex w-full items-center gap-4 overflow-hidden rounded-3xl border border-amber-400/15 bg-gradient-to-br from-amber-500/12 via-orange-500/8 to-transparent p-5 text-left transition-all hover:border-amber-400/30"
        >
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-300 to-orange-600 shadow-card transition-transform group-hover:scale-105">
            <Crown size={24} className="text-black" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-[14.5px] font-extrabold text-amber-200">Ad-free. Lossless. Offline.</span>
            <span className="mt-0.5 block text-[11.5px] leading-snug text-amber-100/60">{premiumInterest ? "You're on the launch list — we'll ping you first." : 'Join the waitlist — launching in India soon.'}</span>
          </span>
          {premiumInterest && <Check size={20} className="shrink-0 text-amber-300" strokeWidth={3} />}
        </button>
      </section>

      {/* ── Keyboard ────────────────────────────────── */}
      <section className="mt-7">
        <SectionTitle icon={Keyboard}>Keyboard shortcuts</SectionTitle>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {SHORTCUTS.map(([keys, action]) => (
            <div key={keys} className="flex flex-col items-start gap-2 rounded-2xl border border-white/5 bg-white/[0.03] px-4 py-3">
              <kbd className="rounded-md border border-white/15 bg-black/50 px-2 py-1 font-mono text-[10.5px] font-bold text-text-primary">{keys}</kbd>
              <span className="text-[11px] leading-snug text-text-secondary">{action}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ── About ───────────────────────────────────── */}
      <section className="mt-7 border-t border-white/5 pt-6">
        <p className="text-[11px] leading-relaxed text-text-muted">
          AUD v1.0.0 — “Music for every mood.” Built with React, TypeScript, Tailwind CSS and the HTML Audio API.
          {source === 'demo' && ' Demo catalog streams sample audio courtesy of SoundHelix.'}
        </p>
        <p className="mt-1.5 text-[10px] text-text-muted/60">© 2026 AUD Technologies. Made with care in Chennai.</p>
      </section>
    </div>
  )
}
