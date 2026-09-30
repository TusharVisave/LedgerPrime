import { useEffect, useRef, useState } from "react";
import toast from "react-hot-toast";
import { Paperclip, ReceiptText, Sparkles } from "lucide-react";
import Modal from "./Modal";
import { apiMessage } from "../api/client";
import type { CategoryDto, TransactionDto } from "../api/types";
import { receiptBlob, transactionsApi, type TransactionInput } from "../api/transactions";
import { today } from "../utils/format";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (input: TransactionInput) => Promise<void>;
  categories: CategoryDto[];
  editing?: TransactionDto | null;
  /** Pre-fill draft from quick-add parse or receipt scan (only when not editing). */
  draft?: TransactionInput | null;
  /** Where the draft came from ("Quick-add" / "Receipt scan"), shown as a banner. */
  draftSource?: string | null;
  /** Called after a receipt upload completes (only when editing). */
  onReceiptUpload?: (id: number) => void | Promise<void>;
}

const AMOUNT_RE = /^\d+(\.\d{1,2})?$/;

export default function TransactionModal({ open, onClose, onSubmit, categories, editing, draft, draftSource, onReceiptUpload }: Props) {
  const [type, setType] = useState<"income" | "expense">("expense");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("");
  const [tags, setTags] = useState("");
  const [note, setNote] = useState("");
  const [date, setDate] = useState(today());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [uploadingReceipt, setUploadingReceipt] = useState(false);
  const [viewingReceipt, setViewingReceipt] = useState(false);
  const receiptInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setType(editing.type);
      setAmount(editing.amount);
      setCategory(editing.category);
      setTags(editing.tags.join(", "));
      setNote(editing.note);
      setDate(editing.date);
    } else if (draft) {
      setType(draft.type);
      setAmount(draft.amount ?? "");
      setCategory(draft.category ?? "");
      setTags((draft.tags ?? []).join(", "));
      setNote(draft.note ?? "");
      setDate(draft.date ?? today());
    } else {
      setType("expense");
      setAmount("");
      setCategory("");
      setTags("");
      setNote("");
      setDate(today());
    }
    setErrors({});
    setReceiptFile(null);
  }, [open, editing, draft]);

  const filteredCategories = categories.filter((c) => c.type === type);

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!AMOUNT_RE.test(amount.trim()) || Number(amount) <= 0) {
      e.amount = "Enter a valid amount greater than 0 (max 2 decimals).";
    }
    if (!category) e.category = "Choose a category.";
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
        type,
        amount: Number(amount).toFixed(2),
        category,
        tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
        note: note.trim(),
        date,
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

  const handleReceiptUpload = async () => {
    if (!editing || !receiptFile) return;
    setUploadingReceipt(true);
    try {
      await transactionsApi.attachReceipt(editing.id, receiptFile);
      toast.success("Receipt attached");
      setReceiptFile(null);
      if (receiptInputRef.current) receiptInputRef.current.value = "";
      await onReceiptUpload?.(editing.id);
    } catch (err) {
      toast.error(apiMessage(err, "Failed to attach receipt"));
    } finally {
      setUploadingReceipt(false);
    }
  };

  const handleViewReceipt = async () => {
    if (!editing) return;
    setViewingReceipt(true);
    try {
      const blob = await receiptBlob(editing.id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
    } catch (err) {
      toast.error(apiMessage(err, "Couldn't load the receipt"));
    } finally {
      setViewingReceipt(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={editing ? "Edit transaction" : "Add transaction"}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {!editing && draft && draftSource && (
          <div className="flex items-start gap-2.5 rounded-xl border border-emerald-500/25 bg-emerald-500/5 px-4 py-3 text-xs text-emerald-200/90">
            <Sparkles className="h-4 w-4 text-emerald-300 shrink-0 mt-0.5" />
            <span>Prefilled from {draftSource} — review the details before saving.</span>
          </div>
        )}
        <div className="grid grid-cols-2 gap-2 p-1 bg-zinc-950/60 border border-zinc-800 rounded-xl">
          {(["expense", "income"] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => {
                setType(t);
                setCategory("");
              }}
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

        <div>
          <label className="label" htmlFor="tx-amount">Amount (₹)</label>
          <input
            id="tx-amount"
            className={`input ${errors.amount ? "input-error" : ""}`}
            inputMode="decimal"
            placeholder="1250.50"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          {fieldError("amount")}
        </div>

        <div>
          <label className="label" htmlFor="tx-category">Category</label>
          <select
            id="tx-category"
            className={`input ${errors.category ? "input-error" : ""}`}
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            <option value="">Select a category</option>
            {filteredCategories.map((c) => (
              <option key={c.id} value={c.name}>{c.name}</option>
            ))}
          </select>
          {fieldError("category")}
        </div>

        <div>
          <label className="label" htmlFor="tx-date">Date</label>
          <input
            id="tx-date"
            type="date"
            className={`input ${errors.date ? "input-error" : ""} [color-scheme:dark]`}
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
          {fieldError("date")}
        </div>

        <div>
          <label className="label" htmlFor="tx-note">Note</label>
          <input
            id="tx-note"
            className="input"
            placeholder="e.g. Weekly groceries"
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </div>

        <div>
          <label className="label" htmlFor="tx-tags">Tags <span className="text-zinc-600">(comma separated)</span></label>
          <input
            id="tx-tags"
            className="input"
            placeholder="food, essentials"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
          />
        </div>

        {editing && (
          <div className="rounded-xl border border-zinc-800 bg-zinc-950/60 p-4">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-sm font-medium text-zinc-300">
                <ReceiptText className="h-4 w-4 text-zinc-500" />
                Receipt
              </div>
              <button
                type="button"
                onClick={handleViewReceipt}
                disabled={viewingReceipt}
                className="text-xs font-medium text-emerald-400 hover:text-emerald-300 disabled:opacity-50"
              >
                {viewingReceipt ? "Loading…" : "View receipt"}
              </button>
            </div>
            <div className="mt-3 flex items-center gap-2">
              <input
                ref={receiptInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => setReceiptFile(e.target.files?.[0] ?? null)}
              />
              <button
                type="button"
                onClick={() => receiptInputRef.current?.click()}
                className="btn-ghost !py-2 text-xs"
              >
                <Paperclip className="h-4 w-4" />
                {receiptFile ? receiptFile.name : "Choose image"}
              </button>
              <button
                type="button"
                onClick={handleReceiptUpload}
                disabled={!receiptFile || uploadingReceipt}
                className="btn-primary !py-2 text-xs"
              >
                {uploadingReceipt ? "Uploading…" : "Upload"}
              </button>
            </div>
            <p className="mt-2 text-[11px] text-zinc-600">
              Attaching a receipt replaces any previously attached one.
            </p>
          </div>
        )}

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose} className="btn-ghost" disabled={busy}>
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Saving…" : editing ? "Save changes" : "Add transaction"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
