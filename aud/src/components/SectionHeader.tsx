import { Link } from 'react-router-dom'

export default function SectionHeader({ title, to, actionLabel }: { title: string; to?: string; actionLabel?: string }) {
  return (
    <div className="mb-3 mt-8 flex items-end justify-between gap-4 first:mt-2">
      <h2 className="text-[19px] font-extrabold tracking-tight text-text-primary sm:text-[22px]">{title}</h2>
      {to && (
        <Link to={to} className="shrink-0 text-[11px] font-bold uppercase tracking-[0.14em] text-text-muted transition-colors hover:text-white">
          {actionLabel ?? 'Show all'}
        </Link>
      )}
    </div>
  )
}
