import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { AlertTriangle, CheckCircle2, PiggyBank } from "lucide-react";
import { budgetsApi } from "../api/budgets";
import { categoriesApi } from "../api/categories";
import { apiFieldErrors, apiMessage } from "../api/client";
import type { BudgetStatus, CategoryBudgetStatus, CategoryDto } from "../api/types";
import EmptyState from "../components/EmptyState";
import { SkeletonCard } from "../components/Skeleton";
import { currentMonth, formatINR, monthLabel, pct } from "../utils/format";

export default function BudgetsPage() {
  const [month, setMonth] = useState(currentMonth());
  const [budget, setBudget] = useState<BudgetStatus | null>(null);
  const [categoryBudgets, setCategoryBudgets] = useState<CategoryBudgetStatus[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("");
  const [rollover, setRollover] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);

  useEffect(() => {
    categoriesApi.list().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([budgetsApi.get(month), budgetsApi.categories(month)])
      .then(([b, cb]) => {
        if (!cancelled) {
          setBudget(b);
          setCategoryBudgets(cb);
          setAmount(b.budget);
        }
      })
      .catch((err) => {
        if (!cancelled) toast.error(apiMessage(err, "Failed to load budget"));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [month]);

  const refresh = async () => {
    try {
      const [b, cb] = await Promise.all([budgetsApi.get(month), budgetsApi.categories(month)]);
      setBudget(b);
      setCategoryBudgets(cb);
    } catch {
      // Toast already shown by caller.
    }
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    setFieldError(null);
    if (!/^\d+(\.\d{1,2})?$/.test(amount.trim()) || Number(amount) <= 0) {
      setFieldError("Enter a valid amount greater than 0 (max 2 decimals).");
      return;
    }
    setSaving(true);
    try {
      await budgetsApi.upsert(month, Number(amount).toFixed(2), category || undefined, category ? rollover : undefined);
      await refresh();
      toast.success(
        category
          ? `Budget for ${category} set to ${formatINR(Number(amount).toFixed(2))} for ${monthLabel(month)}`
          : `Budget set to ${formatINR(Number(amount).toFixed(2))} for ${monthLabel(month)}`,
      );
    } catch (err) {
      const fe = apiFieldErrors(err);
      if (fe.amount || fe.month || fe.category) setFieldError(fe.amount ?? fe.month ?? fe.category);
      else toast.error(apiMessage(err, "Failed to save budget"));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="animate-pulse h-8 w-48 rounded bg-zinc-800" />
        <SkeletonCard />
        <SkeletonCard />
      </div>
    );
  }

  if (!budget) {
    return (
      <EmptyState
        icon={PiggyBank}
        title="Couldn't load the budget"
        text="The backend didn't respond. Make sure it's running and try again."
      />
    );
  }

  const p = pct(budget.spent, budget.budget);
  const barColor = budget.exceeded ? "bg-rose-500" : budget.nearLimit ? "bg-amber-400" : "bg-emerald-400";

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Budgets</h1>
          <p className="text-sm text-zinc-500">One monthly cap, tracked in real time</p>
        </div>
        <input
          type="month"
          value={month}
          max={currentMonth()}
          onChange={(e) => e.target.value && setMonth(e.target.value)}
          aria-label="Select month"
          className="input !w-auto [color-scheme:dark]"
        />
      </div>

      {/* Alert banner */}
      {budget.exceeded ? (
        <div className="flex items-start gap-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-5 py-4">
          <AlertTriangle className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
          <div>
            <div className="font-semibold text-rose-200">Budget exceeded</div>
            <p className="text-sm text-rose-200/70 mt-0.5">
              You've spent {formatINR(budget.spent)} against a {formatINR(budget.budget)} budget in {monthLabel(month)}.
            </p>
          </div>
        </div>
      ) : budget.nearLimit ? (
        <div className="flex items-start gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 px-5 py-4">
          <AlertTriangle className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
          <div>
            <div className="font-semibold text-amber-200">Near budget limit</div>
            <p className="text-sm text-amber-200/70 mt-0.5">
              You've used {p.toFixed(0)}% of your {formatINR(budget.budget)} budget in {monthLabel(month)}.
            </p>
          </div>
        </div>
      ) : (
        <div className="flex items-start gap-3 rounded-2xl border border-emerald-500/25 bg-emerald-500/5 px-5 py-4">
          <CheckCircle2 className="h-5 w-5 text-emerald-300 shrink-0 mt-0.5" />
          <div>
            <div className="font-semibold text-emerald-200">On track</div>
            <p className="text-sm text-emerald-200/70 mt-0.5">
              {formatINR(budget.remaining)} remaining of your {formatINR(budget.budget)} budget.
            </p>
          </div>
        </div>
      )}

      {/* Progress */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-5">
          <div className="h-11 w-11 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center">
            <PiggyBank className="h-5 w-5 text-emerald-300" />
          </div>
          <div>
            <h2 className="font-semibold text-zinc-100">{monthLabel(month)} budget</h2>
            <p className="text-xs text-zinc-500">Updates with every transaction</p>
          </div>
        </div>
        <div className="flex items-baseline justify-between mb-2">
          <span className="text-3xl font-bold text-white">{formatINR(budget.spent)}</span>
          <span className="text-sm text-zinc-500">of {formatINR(budget.budget)}</span>
        </div>
        <div className="h-4 rounded-full bg-zinc-800 overflow-hidden">
          <div className={`h-full rounded-full transition-all duration-500 ${barColor}`} style={{ width: `${Math.min(100, p)}%` }} />
        </div>
        <div className="flex justify-between text-xs text-zinc-500 mt-2">
          <span>{p.toFixed(1)}% used</span>
          <span>{budget.exceeded ? "Over budget" : `${formatINR(budget.remaining)} left`}</span>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {[
          { label: "Budget", value: formatINR(budget.budget), tone: "text-zinc-100" },
          { label: "Spent", value: formatINR(budget.spent), tone: "text-rose-300" },
          { label: "Remaining", value: formatINR(budget.remaining), tone: budget.exceeded ? "text-rose-300" : "text-emerald-300" },
        ].map((s) => (
          <div key={s.label} className="card p-5">
            <div className="text-xs text-zinc-500">{s.label}</div>
            <div className={`mt-1 text-xl font-bold ${s.tone}`}>{s.value}</div>
          </div>
        ))}
      </div>

      {/* Set / update */}
      <div className="card p-6">
        <h2 className="font-semibold text-zinc-100 mb-1">Set budget for {monthLabel(month)}</h2>
        <p className="text-xs text-zinc-500 mb-4">
          Leave the category empty for the overall monthly budget. Pick a category to set a
          per-category budget; saving replaces the existing budget.
        </p>
        <form onSubmit={handleSubmit} className="space-y-3" noValidate>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="budget-amount">Amount (₹)</label>
              <input
                id="budget-amount"
                className={`input ${fieldError ? "input-error" : ""}`}
                inputMode="decimal"
                placeholder="50000.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                aria-label="Budget amount in INR"
              />
            </div>
            <div>
              <label className="label" htmlFor="budget-category">
                Category <span className="text-zinc-600">(optional)</span>
              </label>
              <select
                id="budget-category"
                className="input"
                value={category}
                onChange={(e) => {
                  setCategory(e.target.value);
                  if (!e.target.value) setRollover(false);
                }}
              >
                <option value="">Overall monthly budget</option>
                {categories
                  .filter((c) => c.type === "expense")
                  .map((c) => (
                    <option key={c.id} value={c.name}>{c.name}</option>
                  ))}
              </select>
            </div>
          </div>
          {category && (
            <label className="flex items-center gap-2.5 text-sm text-zinc-300 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={rollover}
                onChange={(e) => setRollover(e.target.checked)}
                className="h-4 w-4 rounded accent-emerald-500"
              />
              Rollover unused budget to next month
            </label>
          )}
          {fieldError && <p className="text-xs text-rose-400">{fieldError}</p>}
          <button type="submit" className="btn-primary sm:w-44" disabled={saving}>
            {saving ? "Saving…" : "Save budget"}
          </button>
        </form>
      </div>

      {/* Per-category budgets */}
      {categoryBudgets.length > 0 && (
        <div className="card p-6">
          <h2 className="font-semibold text-zinc-100 mb-4">Category budgets · {monthLabel(month)}</h2>
          <div className="space-y-5">
            {categoryBudgets.map((cb) => {
              const cp = pct(cb.spent, cb.budget);
              const barColor = cb.exceeded ? "bg-rose-500" : cb.nearLimit ? "bg-amber-400" : "bg-emerald-400";
              return (
                <div key={cb.category}>
                  <div className="flex items-baseline justify-between text-sm mb-1.5">
                    <span className="text-zinc-300 font-medium">
                      {cb.category}
                      {cb.rollover && (
                        <span className="badge bg-sky-500/10 text-sky-300 border border-sky-500/20 ml-2">
                          rollover
                        </span>
                      )}
                    </span>
                    <span className="text-xs text-zinc-500">
                      {formatINR(cb.spent)} of {formatINR(cb.budget)}
                    </span>
                  </div>
                  <div className="h-2.5 rounded-full bg-zinc-800 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${barColor}`}
                      style={{ width: `${Math.min(100, cp)}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-zinc-600 mt-1">
                    <span>{cp.toFixed(0)}% used</span>
                    <span>{cb.exceeded ? "Over budget" : `${formatINR(cb.remaining)} left`}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
