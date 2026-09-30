import { useEffect, useState } from "react";
import Modal from "./Modal";
import type { GoalDto, GoalInput } from "../api/types";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: GoalInput) => Promise<void>;
  editing?: GoalDto | null;
}

const AMOUNT_RE = /^\d+(\.\d{1,2})?$/;

export default function GoalModal({ open, onClose, onSubmit, editing }: Props) {
  const [name, setName] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [deadline, setDeadline] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setName(editing.name);
      setTargetAmount(editing.targetAmount);
      setDeadline(editing.deadline ?? "");
    } else {
      setName("");
      setTargetAmount("");
      setDeadline("");
    }
    setErrors({});
  }, [open, editing]);

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = "Give the goal a name.";
    if (!AMOUNT_RE.test(targetAmount.trim()) || Number(targetAmount) <= 0) {
      e.targetAmount = "Enter a valid target greater than 0 (max 2 decimals).";
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;
    setBusy(true);
    try {
      await onSubmit({
        name: name.trim(),
        targetAmount: Number(targetAmount).toFixed(2),
        ...(deadline ? { deadline } : {}),
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
    <Modal open={open} onClose={onClose} title={editing ? "Edit goal" : "New savings goal"}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="goal-name">Goal name</label>
          <input
            id="goal-name"
            className={`input ${errors.name ? "input-error" : ""}`}
            placeholder="e.g. Emergency fund"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          {fieldError("name")}
        </div>
        <div>
          <label className="label" htmlFor="goal-target">Target amount (₹)</label>
          <input
            id="goal-target"
            className={`input ${errors.targetAmount ? "input-error" : ""}`}
            inputMode="decimal"
            placeholder="50000.00"
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
          />
          {fieldError("targetAmount")}
        </div>
        <div>
          <label className="label" htmlFor="goal-deadline">
            Deadline <span className="text-zinc-600">(optional)</span>
          </label>
          <input
            id="goal-deadline"
            type="date"
            className="input [color-scheme:dark]"
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
          />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-ghost" disabled={busy}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Saving…" : editing ? "Save changes" : "Create goal"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
