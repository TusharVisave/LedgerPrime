import { useEffect, useState } from "react";
import Modal from "./Modal";
import type { ReminderDto } from "../api/types";
import type { ReminderInput } from "../api/reminders";
import { today } from "../utils/format";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: ReminderInput) => Promise<void>;
  editing?: ReminderDto | null;
}

export default function ReminderModal({ open, onClose, onSubmit, editing }: Props) {
  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState("0");
  const [category, setCategory] = useState("Bills");
  const [dueDate, setDueDate] = useState(today());
  const [isRecurring, setIsRecurring] = useState(false);
  const [recurrence, setRecurrence] = useState("monthly");
  const [emailNotify, setEmailNotify] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setTitle(editing.title);
      setAmount(editing.amount);
      setCategory(editing.category);
      setDueDate(editing.dueDate);
      setIsRecurring(editing.isRecurring);
      setRecurrence(editing.recurrence || "monthly");
      setEmailNotify(editing.emailNotify);
    } else {
      setTitle("");
      setAmount("0");
      setCategory("Bills");
      setDueDate(today());
      setIsRecurring(false);
      setRecurrence("monthly");
      setEmailNotify(false);
    }
    setErrors({});
  }, [open, editing]);

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    const e: Record<string, string> = {};
    if (!title.trim()) e.title = "Title is required.";
    if (!/^\d+(\.\d{1,2})?$/.test(amount.trim())) e.amount = "Enter a valid amount (max 2 decimals).";
    if (!dueDate) e.dueDate = "Pick a due date.";
    setErrors(e);
    if (Object.keys(e).length > 0) return;
    setBusy(true);
    try {
      await onSubmit({
        title: title.trim(),
        amount: Number(amount).toFixed(2),
        category: category.trim() || "Bills",
        dueDate,
        isRecurring,
        recurrence,
        emailNotify,
      });
      onClose();
    } catch {
      // Parent toasts the error; keep modal open.
    } finally {
      setBusy(false);
    }
  };

  const fieldError = (f: string) =>
    errors[f] ? <p className="mt-1 text-xs text-rose-400">{errors[f]}</p> : null;

  const toggleRow = (
    checked: boolean,
    onChange: (v: boolean) => void,
    label: string,
    hint: string,
  ) => (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="w-full flex items-center justify-between py-2.5"
    >
      <span className="text-left">
        <span className="block text-sm font-medium text-zinc-200">{label}</span>
        <span className="block text-xs text-zinc-500">{hint}</span>
      </span>
      <span
        className={`relative h-6 w-11 rounded-full transition shrink-0 ${
          checked ? "bg-emerald-500" : "bg-zinc-700"
        }`}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-all ${
            checked ? "left-[22px]" : "left-0.5"
          }`}
        />
      </span>
    </button>
  );

  return (
    <Modal open={open} onClose={onClose} title={editing ? "Edit reminder" : "Add reminder"}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="rem-title">Title</label>
          <input
            id="rem-title"
            className={`input ${errors.title ? "input-error" : ""}`}
            placeholder="e.g. Electricity bill"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          {fieldError("title")}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label" htmlFor="rem-amount">Amount (₹)</label>
            <input
              id="rem-amount"
              className={`input ${errors.amount ? "input-error" : ""}`}
              inputMode="decimal"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
            {fieldError("amount")}
          </div>
          <div>
            <label className="label" htmlFor="rem-date">Due date</label>
            <input
              id="rem-date"
              type="date"
              className={`input ${errors.dueDate ? "input-error" : ""} [color-scheme:dark]`}
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
            />
            {fieldError("dueDate")}
          </div>
        </div>

        <div>
          <label className="label" htmlFor="rem-category">Category</label>
          <input
            id="rem-category"
            className="input"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
        </div>

        <div className="divide-y divide-zinc-800/60">
          {toggleRow(isRecurring, setIsRecurring, "Recurring", "Repeat this reminder automatically")}
          {isRecurring && (
            <div className="py-3">
              <label className="label" htmlFor="rem-recurrence">Recurrence</label>
              <select
                id="rem-recurrence"
                className="input"
                value={recurrence}
                onChange={(e) => setRecurrence(e.target.value)}
              >
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
                <option value="quarterly">Quarterly</option>
                <option value="yearly">Yearly</option>
              </select>
            </div>
          )}
          {toggleRow(emailNotify, setEmailNotify, "Email notification", "Get an email before it's due")}
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-ghost" disabled={busy}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Saving…" : editing ? "Save changes" : "Add reminder"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
