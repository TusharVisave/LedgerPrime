import type { LucideIcon } from "lucide-react";

interface Props {
  icon: LucideIcon;
  title: string;
  text?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyState({ icon: Icon, title, text, actionLabel, onAction }: Props) {
  return (
    <div className="card p-10 flex flex-col items-center text-center">
      <div className="h-14 w-14 rounded-2xl bg-zinc-800/70 flex items-center justify-center mb-4">
        <Icon className="h-7 w-7 text-zinc-500" />
      </div>
      <h3 className="font-semibold text-zinc-200">{title}</h3>
      {text && <p className="mt-1.5 text-sm text-zinc-500 max-w-sm">{text}</p>}
      {actionLabel && onAction && (
        <button onClick={onAction} className="btn-primary mt-5">
          {actionLabel}
        </button>
      )}
    </div>
  );
}
