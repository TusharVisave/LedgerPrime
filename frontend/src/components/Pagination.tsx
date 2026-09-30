import { ChevronLeft, ChevronRight } from "lucide-react";

interface Props {
  page: number; // 0-based
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
}

export default function Pagination({ page, totalPages, totalElements, size, onPage }: Props) {
  if (totalPages <= 1) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex items-center justify-between py-4">
      <p className="text-xs text-zinc-500">
        Showing <span className="text-zinc-300 font-medium">{from}–{to}</span> of{" "}
        <span className="text-zinc-300 font-medium">{totalElements}</span>
      </p>
      <div className="flex items-center gap-2">
        <button
          onClick={() => onPage(page - 1)}
          disabled={page === 0}
          aria-label="Previous page"
          className="p-2 rounded-lg border border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800 disabled:opacity-40 disabled:cursor-not-allowed transition"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        {Array.from({ length: totalPages }).map((_, i) => {
          // Compact page window: first, last, current ± 1
          if (i !== 0 && i !== totalPages - 1 && Math.abs(i - page) > 1) {
            if (i === 1 && page > 2) return <span key={i} className="text-zinc-600 text-xs px-1">…</span>;
            if (i === totalPages - 2 && page < totalPages - 3)
              return <span key={i} className="text-zinc-600 text-xs px-1">…</span>;
            if (Math.abs(i - page) > 1) return null;
          }
          return (
            <button
              key={i}
              onClick={() => onPage(i)}
              className={`min-w-9 h-9 px-2 rounded-lg text-sm font-medium transition ${
                i === page
                  ? "bg-emerald-500 text-zinc-950"
                  : "border border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800"
              }`}
            >
              {i + 1}
            </button>
          );
        })}
        <button
          onClick={() => onPage(page + 1)}
          disabled={page >= totalPages - 1}
          aria-label="Next page"
          className="p-2 rounded-lg border border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800 disabled:opacity-40 disabled:cursor-not-allowed transition"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
