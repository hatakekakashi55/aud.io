export default function Equalizer({ className = '' }: { className?: string }) {
  return (
    <span className={`inline-flex items-end gap-[2.5px] h-[14px] ${className}`} aria-hidden="true">
      <span className="eq-bar w-[3px] h-full bg-brand-500 animate-eq1" />
      <span className="eq-bar w-[3px] h-full bg-brand-500 animate-eq2" style={{ animationDelay: '0.15s' }} />
      <span className="eq-bar w-[3px] h-full bg-brand-500 animate-eq3" style={{ animationDelay: '0.3s' }} />
    </span>
  )
}
