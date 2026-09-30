import { useEffect, useState } from "react";
import Modal from "./Modal";
import type { GoalAllocateInput, GoalDto } from "../api/types";
import { formatINR, today } from "../utils/format";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: GoalAllocateInput) => Promise<void>;
  goal: GoalDto | null;
}

const AMOUNT_RE = /^\d+(\.\d{1,2})?$/;

export default function AllocateModal({ open, onClose, onSubmit, goal }: Props) {
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");
  const [date, setDate] = useState(today());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    setAmount("");
    setNote("");
    setDate(today());
    setErrors({});
  }, [open ]);

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!AMOUNT_RE.test(amount.trim()) || Number(amount) <= 0) {
      e.amount = "Enter a valid amount greater than 0 (max 2 decimals).";
    }
    if (!date) e.date = "Pick a date.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setBusy(true);
    try {
      await onSubmit({
        amount: Number(amount).toFixed(2),
        ...(note.trim() ? { note: note.trim() } : {}),
        ...(date ? { date } : {}),
      });
      onClose();
    } catch {
      // Parent shows toast; keep modal open.
    } finally {
      setBusy(false);
    }
  };

  const fieldError = (f: string) =>
    errors[f] ? <p className="mt-1 text-xs text-rose-400">{errors[f]}</p> : null;

  return (
    <Modal open={open} onClose={onClose} title={goal ? `Allocate to ${goal.name}` : "Allocate"}>
      {goal && (
        <p className="text-xs text-zinc-500 mb-4">
          Remaining: <span className="font-semibold text-emerald-300">{formatINR(goal.remaining)}</span>
          {" "}of {formatINR(goal.targetAmount)}
        </p>
      )}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="alloc-amount">Amount (₹)</label>
          <input
            id="alloc-amount"
            className={`input ${errors.amount ? "input-error" : ""}`}
            inputMode="decimal"
            placeholder="1000.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          {fieldError("amount")}
        </div>
        <div>
          <label className="label" htmlFor="alloc-note">
            Note <span className="text-zinc-600">(optional)</span>
          </label>
          <input
            id="alloc-note"
            className="input"
            placeholder="e.g. Monthly SIP"
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </div>
        <div>
          <label className="label" htmlFor="alloc-date">Date</label>
          <input
            id="alloc-date"
            type="date"
            className={`input ${errors.date ? "input-error" : ""} [color-scheme:dark]`}
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
          {fieldError("date")}
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-ghost" disabled={busy}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Allocating…" : "Allocate"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
