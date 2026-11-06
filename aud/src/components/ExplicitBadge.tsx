export default function ExplicitBadge({ className = '' }: { className?: string }) {
  return (
    <span
      title="Explicit lyrics"
      className={`inline-flex items-center justify-center rounded-[4px] bg-base-600/80 text-[9px] font-bold text-text-secondary leading-none px-1.5 py-[3px] tracking-wide ${className}`}
    >
      E
    </span>
  )
}
