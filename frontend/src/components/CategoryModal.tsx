import { useEffect, useState } from "react";
import Modal from "./Modal";
import type { CategoryDto } from "../api/types";
import type { CategoryInput } from "../api/categories";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: CategoryInput) => Promise<void>;
  editing?: CategoryDto | null;
}

const ICON_CHOICES = ["tag", "wallet", "cart", "home", "car", "food", "health", "briefcase", "entertainment", "education"];
const COLOR_CHOICES = ["#34d399", "#f472b6", "#60a5fa", "#fbbf24", "#a78bfa", "#f87171", "#22d3ee", "#fb923c"];

export default function CategoryModal({ open, onClose, onSubmit, editing }: Props) {
  const [name, setName] = useState("");
  const [type, setType] = useState<"income" | "expense">("expense");
  const [color, setColor] = useState(COLOR_CHOICES[0]);
  const [icon, setIcon] = useState(ICON_CHOICES[0]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setName(editing.name);
      setType(editing.type);
      setColor(editing.color || COLOR_CHOICES[0]);
      setIcon(editing.icon || ICON_CHOICES[0]);
    } else {
      setName("");
      setType("expense");
      setColor(COLOR_CHOICES[0]);
      setIcon(ICON_CHOICES[0]);
    }
    setErrors({});
  }, [open, editing]);

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = "Category name is required.";
    setErrors(e);
    if (Object.keys(e).length > 0) return;
    setBusy(true);
    try {
      await onSubmit({ name: name.trim(), type, color, icon });
      onClose();
    } catch {
      // Parent toasts the error; keep modal open.
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={editing ? "Edit category" : "Add category"}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="cat-name">Name</label>
          <input
            id="cat-name"
            className={`input ${errors.name ? "input-error" : ""}`}
            placeholder="e.g. Groceries"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          {errors.name && <p className="mt-1 text-xs text-rose-400">{errors.name}</p>}
        </div>

        <div>
          <label className="label">Type</label>
          <div className="grid grid-cols-2 gap-2 p-1 bg-zinc-950/60 border border-zinc-800 rounded-xl">
            {(["expense", "income"] as const).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={`py-2 rounded-lg text-sm font-semibold capitalize transition ${
                  type === t
                    ? t === "income"
                      ? "bg-emerald-500 text-zinc-950"
                      : "bg-rose-500 text-white"
                    : "text-zinc-500 hover:text-zinc-200"
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="label">Color</label>
          <div className="flex flex-wrap gap-2">
            {COLOR_CHOICES.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setColor(c)}
                aria-label={`Color ${c}`}
                className={`h-9 w-9 rounded-full transition ring-2 ring-offset-2 ring-offset-zinc-900 ${
                  color === c ? "ring-white" : "ring-transparent hover:ring-zinc-600"
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>

        <div>
          <label className="label" htmlFor="cat-icon">Icon</label>
          <select id="cat-icon" className="input" value={icon} onChange={(e) => setIcon(e.target.value)}>
            {ICON_CHOICES.map((i) => (
              <option key={i} value={i}>{i}</option>
            ))}
          </select>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-ghost" disabled={busy}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Saving…" : editing ? "Save changes" : "Add category"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
