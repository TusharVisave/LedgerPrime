import { useEffect, useRef, useState } from "react";
import toast from "react-hot-toast";
import { FileDown, FileSpreadsheet, FileUp, Pencil, Plus, ReceiptText, ScanText, Sparkles, Trash2, X } from "lucide-react";
import { categoriesApi } from "../api/categories";
import { apiMessage } from "../api/client";
import { exportsApi } from "../api/exports";
import { transactionsApi, type TransactionInput } from "../api/transactions";
import type { CategoryDto, ImportResult, Page, ParsedTransaction, ReceiptScanResult, TransactionDto } from "../api/types";
import ConfirmDialog from "../components/ConfirmDialog";
import EmptyState from "../components/EmptyState";
import Modal from "../components/Modal";
import Pagination from "../components/Pagination";
import { SkeletonTable } from "../components/Skeleton";
import TransactionModal from "../components/TransactionModal";
import { currentMonth, dateLabel, formatINR, today } from "../utils/format";

const PAGE_SIZE = 25;

interface Filters {
  type: "" | "income" | "expense";
  category: string;
  keyword: string;
  minAmount: string;
  maxAmount: string;
  startDate: string;
  endDate: string;
}

const EMPTY_FILTERS: Filters = {
  type: "",
  category: "",
  keyword: "",
  minAmount: "",
  maxAmount: "",
  startDate: "",
  endDate: "",
};

export default function TransactionsPage() {
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<TransactionDto> | null>(null);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TransactionDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TransactionDto | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [exporting, setExporting] = useState<"pdf" | "xlsx" | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const firstLoad = useRef(true);
  // Quick-add / receipt scan draft pre-fill for the modal
  const [draft, setDraft] = useState<TransactionInput | null>(null);
  const [draftSource, setDraftSource] = useState<string | null>(null);
  const [quickAddText, setQuickAddText] = useState("");
  const [parsing, setParsing] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const scanInputRef = useRef<HTMLInputElement>(null);
  const csvInputRef = useRef<HTMLInputElement>(null);

  // Debounce keyword search
  useEffect(() => {
    const t = setTimeout(() => setDebouncedKeyword(filters.keyword), 450);
    return () => clearTimeout(t);
  }, [filters.keyword]);

  useEffect(() => {
    categoriesApi.list().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    transactionsApi
      .list({
        type: filters.type,
        category: filters.category,
        keyword: debouncedKeyword,
        minAmount: filters.minAmount,
        maxAmount: filters.maxAmount,
        startDate: filters.startDate,
        endDate: filters.endDate,
        page,
        size: PAGE_SIZE,
        sort: "date,desc",
      })
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) toast.error(apiMessage(err, "Failed to load transactions"));
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
          firstLoad.current = false;
        }
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, debouncedKeyword, filters.type, filters.category, filters.minAmount, filters.maxAmount, filters.startDate, filters.endDate, reloadKey]);

  const setFilter = <K extends keyof Filters>(key: K, value: Filters[K]) => {
    setFilters((f) => ({ ...f, [key]: value }));
    setPage(0);
  };

  const clearFilters = () => {
    setFilters(EMPTY_FILTERS);
    setDebouncedKeyword("");
    setPage(0);
  };

  const hasFilters =
    filters.type || filters.category || filters.keyword || filters.minAmount ||
    filters.maxAmount || filters.startDate || filters.endDate;

  const handleCreate = async (input: TransactionInput) => {
    try {
      const res = await transactionsApi.create(input);
      toast.success("Transaction added");
      if (res.budgetStatus.exceeded) toast.error("Budget exceeded for this month");
      else if (res.budgetStatus.nearLimit) toast("Near your budget limit", { icon: "⚠️" });
      setPage(0);
      setReloadKey((k) => k + 1);
    } catch (err) {
      toast.error(apiMessage(err, "Failed to add transaction"));
      throw err;
    }
  };

  const handleUpdate = async (input: TransactionInput) => {
    if (!editing) return;
    try {
      await transactionsApi.update(editing.id, input);
      toast.success("Transaction updated");
      setReloadKey((k) => k + 1);
    } catch (err) {
      toast.error(apiMessage(err, "Failed to update transaction"));
      throw err;
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await transactionsApi.remove(deleteTarget.id);
      toast.success("Transaction deleted");
      setDeleteTarget(null);
      // If we deleted the last row on this page, step back a page.
      if (data && data.content.length === 1 && page > 0) setPage(page - 1);
      else setReloadKey((k) => k + 1);
    } catch (err) {
      toast.error(apiMessage(err, "Failed to delete transaction"));
    } finally {
      setDeleting(false);
    }
  };

  const handleExport = async (kind: "pdf" | "xlsx") => {
    setExporting(kind);
    try {
      if (kind === "pdf") await exportsApi.transactionsPdf(currentMonth());
      else await exportsApi.transactionsXlsx(currentMonth());
      toast.success(`Exported ${kind.toUpperCase()} for this month`);
    } catch (err) {
      toast.error(apiMessage(err, "Export failed"));
    } finally {
      setExporting(null);
    }
  };

  const openDraftModal = (d: TransactionInput, source: string) => {
    setEditing(null);
    setDraft(d);
    setDraftSource(source);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditing(null);
    setDraft(null);
    setDraftSource(null);
  };

  /** NL quick-add: parse free text, then open the modal pre-filled for confirmation. */
  const handleQuickAdd = async (ev: React.FormEvent) => {
    ev.preventDefault();
    const text = quickAddText.trim();
    if (!text) return;
    setParsing(true);
    try {
      const p: ParsedTransaction = await transactionsApi.parse(text);
      const d: TransactionInput = {
        type: p.type,
        amount: p.amount ?? "",
        category: p.category ?? "",
        note: p.note ?? "",
        date: p.date ?? today(),
        tags: [],
      };
      setQuickAddText("");
      openDraftModal(d, "Quick-add");
      if (!p.amount) toast("No amount detected — please fill it in.", { icon: "⚠️" });
    } catch (err) {
      toast.error(apiMessage(err, "Couldn't parse that text"));
    } finally {
      setParsing(false);
    }
  };

  /** Receipt scan: upload image → scan → open modal pre-filled. */
  const handleScanFile = async (file: File) => {
    setScanning(true);
    try {
      const r: ReceiptScanResult = await transactionsApi.scanReceipt(file);
      const d: TransactionInput = {
        type: "expense",
        amount: r.amount ?? "",
        category: r.category ?? "",
        note: r.merchant ? `Receipt — ${r.merchant}` : "Receipt scan",
        date: r.date ?? today(),
        tags: [],
      };
      if (!r.ocrAvailable) {
        toast("OCR unavailable — please fill in manually.", { icon: "⚠️" });
      } else {
        toast.success("Receipt scanned — review and confirm");
      }
      openDraftModal(d, "Receipt scan");
    } catch (err) {
      toast.error(apiMessage(err, "Receipt scan failed"));
    } finally {
      setScanning(false);
    }
  };

  /** CSV import: upload file → toast summary → show per-row errors. */
  const handleCsvFile = async (file: File) => {
    setImporting(true);
    try {
      const r = await transactionsApi.importCsv(file);
      if (r.failedCount > 0) {
        toast(`Imported ${r.importedCount}, ${r.failedCount} failed`, { icon: "⚠️" });
      } else {
        toast.success(`Imported ${r.importedCount} transaction${r.importedCount === 1 ? "" : "s"}`);
      }
      setImportResult(r);
      setReloadKey((k) => k + 1);
    } catch (err) {
      toast.error(apiMessage(err, "CSV import failed"));
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Transactions</h1>
          <p className="text-sm text-zinc-500">Search, filter and manage every entry</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => scanInputRef.current?.click()}
            disabled={scanning}
            className="btn-ghost !py-2 text-xs"
          >
            <ScanText className="h-4 w-4" />
            {scanning ? "Scanning…" : "Scan receipt"}
          </button>
          <button
            onClick={() => csvInputRef.current?.click()}
            disabled={importing}
            className="btn-ghost !py-2 text-xs"
          >
            <FileUp className="h-4 w-4" />
            {importing ? "Importing…" : "Import CSV"}
          </button>
          <button
            onClick={() => handleExport("pdf")}
            disabled={exporting !== null}
            className="btn-ghost !py-2 text-xs"
          >
            <FileDown className="h-4 w-4" />
            {exporting === "pdf" ? "Exporting…" : "PDF"}
          </button>
          <button
            onClick={() => handleExport("xlsx")}
            disabled={exporting !== null}
            className="btn-ghost !py-2 text-xs"
          >
            <FileSpreadsheet className="h-4 w-4" />
            {exporting === "xlsx" ? "Exporting…" : "Excel"}
          </button>
          <button
            onClick={() => {
              setEditing(null);
              setDraft(null);
              setModalOpen(true);
            }}
            className="btn-primary !py-2 text-sm"
          >
            <Plus className="h-4 w-4" /> Add
          </button>
        </div>
      </div>

      {/* Quick-add bar */}
      <form onSubmit={handleQuickAdd} className="card p-3 sm:p-4">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Sparkles className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-emerald-400 pointer-events-none" />
            <input
              className="input !pl-10"
              placeholder="Quick-add — e.g. spent 500 on lunch today"
              value={quickAddText}
              onChange={(e) => setQuickAddText(e.target.value)}
              disabled={parsing}
              aria-label="Quick-add transaction from natural language"
            />
          </div>
          <button type="submit" className="btn-primary shrink-0" disabled={parsing || !quickAddText.trim()}>
            {parsing ? "Parsing…" : "Parse"}
          </button>
        </div>
        <p className="text-[11px] text-zinc-600 mt-2 px-1">
          Type naturally — the draft opens for confirmation before anything is saved.
        </p>
      </form>

      <input
        ref={scanInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) handleScanFile(f);
          e.target.value = "";
        }}
      />
      <input
        ref={csvInputRef}
        type="file"
        accept=".csv,text/csv"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) handleCsvFile(f);
          e.target.value = "";
        }}
      />

      {/* Filters */}
      <div className="card p-4 sm:p-5">
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-3">
          <div>
            <label className="label">Type</label>
            <select className="input" value={filters.type} onChange={(e) => setFilter("type", e.target.value as Filters["type"])}>
              <option value="">All</option>
              <option value="income">Income</option>
              <option value="expense">Expense</option>
            </select>
          </div>
          <div>
            <label className="label">Category</label>
            <select className="input" value={filters.category} onChange={(e) => setFilter("category", e.target.value)}>
              <option value="">All</option>
              {categories.map((c) => (
                <option key={c.id} value={c.name}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="col-span-2 sm:col-span-1">
            <label className="label">Search</label>
            <input
              className="input"
              placeholder="Note, tag…"
              value={filters.keyword}
              onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
            />
          </div>
          <div>
            <label className="label">Min ₹</label>
            <input className="input" inputMode="decimal" placeholder="10.50" value={filters.minAmount}
              onChange={(e) => setFilter("minAmount", e.target.value)} />
          </div>
          <div>
            <label className="label">Max ₹</label>
            <input className="input" inputMode="decimal" value={filters.maxAmount}
              onChange={(e) => setFilter("maxAmount", e.target.value)} />
          </div>
          <div>
            <label className="label">From</label>
            <input type="date" className="input [color-scheme:dark]" value={filters.startDate}
              max={filters.endDate || today()} onChange={(e) => setFilter("startDate", e.target.value)} />
          </div>
          <div>
            <label className="label">To</label>
            <input type="date" className="input [color-scheme:dark]" value={filters.endDate}
              min={filters.startDate} max={today()} onChange={(e) => setFilter("endDate", e.target.value)} />
          </div>
          <div className="flex items-end">
            {hasFilters ? (
              <button onClick={clearFilters} className="btn-ghost !py-2.5 text-xs w-full">
                <X className="h-4 w-4" /> Clear
              </button>
            ) : null}
          </div>
        </div>
      </div>

      {/* Table */}
      {loading && firstLoad.current ? (
        <SkeletonTable rows={8} />
      ) : data && data.content.length > 0 ? (
        <div>
          {/* Desktop table */}
          <div className="card overflow-hidden hidden md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wide text-zinc-500 border-b border-zinc-800">
                  <th className="px-5 py-3.5 font-medium">Date</th>
                  <th className="px-5 py-3.5 font-medium">Type</th>
                  <th className="px-5 py-3.5 font-medium">Category</th>
                  <th className="px-5 py-3.5 font-medium">Note / Tags</th>
                  <th className="px-5 py-3.5 font-medium text-right">Amount</th>
                  <th className="px-5 py-3.5 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60">
                {data.content.map((t) => (
                  <tr key={t.id} className="hover:bg-zinc-800/30 transition">
                    <td className="px-5 py-3.5 text-zinc-300 whitespace-nowrap">{dateLabel(t.date)}</td>
                    <td className="px-5 py-3.5">
                      <span className={`badge ${t.type === "income" ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20" : "bg-rose-500/10 text-rose-300 border border-rose-500/20"}`}>
                        {t.type}
                      </span>
                      {t.recurringGenerated && (
                        <span className="badge bg-zinc-800 text-zinc-400 ml-1.5" title="Auto-generated from a recurring reminder">auto</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-zinc-300 whitespace-nowrap">{t.category}</td>
                    <td className="px-5 py-3.5 max-w-xs">
                      <div className="truncate text-zinc-200">{t.note || <span className="text-zinc-600">—</span>}</div>
                      {t.tags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1">
                          {t.tags.map((tag) => (
                            <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-400">#{tag}</span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td className={`px-5 py-3.5 text-right font-semibold whitespace-nowrap ${t.type === "income" ? "text-emerald-300" : "text-rose-300"}`}>
                      {t.type === "income" ? "+" : "−"}{formatINR(t.amount)}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex justify-end gap-1">
                        <button
                          aria-label="Edit transaction"
                          onClick={() => {
                            setEditing(t);
                            setModalOpen(true);
                          }}
                          className="p-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          aria-label="Delete transaction"
                          onClick={() => setDeleteTarget(t)}
                          className="p-2 rounded-lg text-zinc-500 hover:text-rose-300 hover:bg-rose-500/10 transition"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="md:hidden space-y-3">
            {data.content.map((t) => (
              <div key={t.id} className="card p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-zinc-100">{t.note || t.category}</div>
                    <div className="text-xs text-zinc-500 mt-0.5">
                      {t.category} · {dateLabel(t.date)}
                    </div>
                  </div>
                  <div className={`text-sm font-semibold whitespace-nowrap ${t.type === "income" ? "text-emerald-300" : "text-rose-300"}`}>
                    {t.type === "income" ? "+" : "−"}{formatINR(t.amount)}
                  </div>
                </div>
                <div className="flex items-center justify-between mt-3">
                  <span className={`badge ${t.type === "income" ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20" : "bg-rose-500/10 text-rose-300 border border-rose-500/20"}`}>
                    {t.type}
                  </span>
                  <div className="flex gap-1">
                    <button
                      aria-label="Edit transaction"
                      onClick={() => {
                        setEditing(t);
                        setModalOpen(true);
                      }}
                      className="p-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      aria-label="Delete transaction"
                      onClick={() => setDeleteTarget(t)}
                      className="p-2 rounded-lg text-zinc-500 hover:text-rose-300 hover:bg-rose-500/10 transition"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            size={data.size}
            onPage={setPage}
          />
        </div>
      ) : (
        <EmptyState
          icon={ReceiptText}
          title={hasFilters ? "No transactions match" : "No transactions yet"}
          text={hasFilters ? "Try widening your filters or clearing them." : "Add your first income or expense to get started."}
          actionLabel={hasFilters ? "Clear filters" : "Add transaction"}
          onAction={hasFilters ? clearFilters : () => {
            setEditing(null);
            setModalOpen(true);
          }}
        />
      )}

      <TransactionModal
        open={modalOpen}
        onClose={closeModal}
        onSubmit={editing ? handleUpdate : handleCreate}
        categories={categories}
        editing={editing}
        draft={draft}
        draftSource={draftSource}
        onReceiptUpload={() => setReloadKey((k) => k + 1)}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete transaction"
        message={`Delete this ${deleteTarget?.type} of ${deleteTarget ? formatINR(deleteTarget.amount) : ""}? This can't be undone.`}
        busy={deleting}
      />

      {/* CSV import errors */}
      <Modal
        open={!!importResult && importResult.errors.length > 0}
        onClose={() => setImportResult(null)}
        title={`Import result — ${importResult?.importedCount ?? 0} imported, ${importResult?.failedCount ?? 0} failed`}
      >
        <div className="space-y-2 max-h-[50vh] overflow-y-auto">
          {importResult?.errors.map((e, i) => (
            <div key={i} className="rounded-xl border border-rose-500/25 bg-rose-500/5 px-4 py-2.5 text-sm">
              <span className="font-semibold text-rose-300">Row {e.row}:</span>{" "}
              <span className="text-zinc-400">{e.message}</span>
            </div>
          ))}
        </div>
        <div className="flex justify-end mt-5">
          <button onClick={() => setImportResult(null)} className="btn-primary">
            Done
          </button>
        </div>
      </Modal>
    </div>
  );
}
