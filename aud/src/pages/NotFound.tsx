import { Disc3 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function NotFound() {
  const navigate = useNavigate()
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 px-6 text-center">
      <span className="flex h-24 w-24 items-center justify-center rounded-full border border-white/10 bg-white/[0.03]">
        <Disc3 size={40} className="text-text-muted" strokeWidth={1.4} />
      </span>
      <h1 className="text-6xl font-black tracking-tighter text-white">404</h1>
      <p className="max-w-xs text-[13px] text-text-secondary">This track skipped — the page you're looking for isn't in the catalog.</p>
      <button onClick={() => navigate('/')} className="mt-2 rounded-full bg-brand-500 px-6 py-3 text-sm font-extrabold text-black transition-transform hover:scale-105">
        Back to home
      </button>
    </div>
  )
}
