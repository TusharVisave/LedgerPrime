/** Loading skeleton blocks (animate-pulse). */
export function SkeletonCard({ className = "" }: { className?: string }) {
  return <div className={`card p-5 ${className}`}>
    <div className="animate-pulse space-y-3">
      <div className="h-4 w-1/3 rounded bg-zinc-800" />
      <div className="h-8 w-2/3 rounded bg-zinc-800" />
    </div>
  </div>;
}

export function SkeletonRow({ className = "" }: { className?: string }) {
  return (
    <div className={`animate-pulse flex items-center gap-4 py-3.5 ${className}`}>
      <div className="h-10 w-10 rounded-xl bg-zinc-800 shrink-0" />
      <div className="flex-1 space-y-2">
        <div className="h-3.5 w-2/5 rounded bg-zinc-800" />
        <div className="h-3 w-1/4 rounded bg-zinc-800/70" />
      </div>
      <div className="h-4 w-20 rounded bg-zinc-800" />
    </div>
  );
}

export function SkeletonTable({ rows = 6 }: { rows?: number }) {
  return (
    <div className="card overflow-hidden">
      <div className="animate-pulse divide-y divide-zinc-800/60 px-5">
        <div className="py-3.5">
          <div className="h-4 w-1/2 rounded bg-zinc-800" />
        </div>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="flex items-center gap-4 py-3.5">
            <div className="h-3.5 w-20 rounded bg-zinc-800" />
            <div className="h-3.5 w-24 rounded bg-zinc-800" />
            <div className="h-3.5 flex-1 rounded bg-zinc-800/60" />
            <div className="h-3.5 w-24 rounded bg-zinc-800" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function SkeletonChart({ className = "" }: { className?: string }) {
  return (
    <div className={`card p-5 ${className}`}>
      <div className="animate-pulse">
        <div className="h-4 w-1/3 rounded bg-zinc-800 mb-6" />
        <div className="h-56 rounded-xl bg-zinc-800/50" />
      </div>
    </div>
  );
}
