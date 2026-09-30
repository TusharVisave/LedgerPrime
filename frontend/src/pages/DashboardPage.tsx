import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import {
  AlertTriangle,
  ArrowDownLeft,
  ArrowUpRight,
  Bot,
  Info,
  Lightbulb,
  PiggyBank,
  ReceiptText,
  Sparkles,
  TrendingUp,
  Wallet,
  X,
} from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { dashboardApi } from "../api/dashboard";
import { insightsApi } from "../api/insights";
import { apiMessage } from "../api/client";
import type { AiReportDto, DashboardDto, InsightsDto } from "../api/types";
import EmptyState from "../components/EmptyState";
import Modal from "../components/Modal";
import { SkeletonCard, SkeletonChart } from "../components/Skeleton";
import { currentMonth, dateLabel, formatINR, monthLabel, pct, shortMonth } from "../utils/format";

function BudgetBar({ spent, budget, exceeded, nearLimit }: { spent: string; budget: string; exceeded: boolean; nearLimit: boolean }) {
  const p = pct(spent, budget);
  const barColor = exceeded ? "bg-rose-500" : nearLimit ? "bg-amber-400" : "bg-emerald-400";
  return (
    <div>
      {(exceeded || nearLimit) && (
        <div
          className={`mb-4 flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${
            exceeded
              ? "border-rose-500/30 bg-rose-500/10 text-rose-200"
              : "border-amber-500/30 bg-amber-500/10 text-amber-200"
          }`}
        >
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
          <div>
            <span className="font-semibold">{exceeded ? "Budget exceeded" : "Near budget limit"}</span>
            <span className="block text-xs opacity-80 mt-0.5">
              {exceeded
                ? `You've spent ${formatINR(spent)} against a ${formatINR(budget)} budget this month.`
                : `You've used ${p.toFixed(0)}% of your ${formatINR(budget)} budget.`}
            </span>
          </div>
        </div>
      )}
      <div className="flex items-baseline justify-between text-sm mb-2">
        <span className="text-zinc-400">
          Spent <span className="font-semibold text-zinc-100">{formatINR(spent)}</span>
        </span>
        <span className="text-zinc-500">of {formatINR(budget)}</span>
      </div>
      <div className="h-3 rounded-full bg-zinc-800 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-500 ${barColor}`}
          style={{ width: `${Math.min(100, p)}%` }}
        />
      </div>
      <div className="text-right text-xs text-zinc-500 mt-1.5">{p.toFixed(1)}% used</div>
    </div>
  );
}

const chartTooltipStyle = {
  backgroundColor: "#18181b",
  border: "1px solid #27272a",
  borderRadius: "12px",
  color: "#f4f4f5",
  fontSize: "12px",
};

export default function DashboardPage() {
  const [month, setMonth] = useState(currentMonth());
  const [data, setData] = useState<DashboardDto | null>(null);
  const [insights, setInsights] = useState<InsightsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiReport, setAiReport] = useState<AiReportDto | null>(null);
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([dashboardApi.get(month), insightsApi.get(month)])
      .then(([d, i]) => {
        if (!cancelled) {
          setData(d);
          setInsights(i);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          // 401 is handled globally (clears user + redirect). Other errors:
          console.error(apiMessage(err, "Failed to load dashboard"));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [month]);

  const handleAiReport = async () => {
    setAiLoading(true);
    try {
      const r = await insightsApi.aiReport(month);
      setAiReport(r);
      setAiModalOpen(true);
    } catch (err) {
      // 501 when the AI report isn't configured — show the backend's message.
      toast.error(apiMessage(err, "AI report isn't available right now"));
    } finally {
      setAiLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="animate-pulse h-8 w-56 rounded bg-zinc-800" />
        <div className="grid gap-4 md:grid-cols-3">
          <SkeletonCard /> <SkeletonCard /> <SkeletonCard />
        </div>
        <div className="grid gap-4 lg:grid-cols-2">
          <SkeletonChart /> <SkeletonChart />
        </div>
        <SkeletonCard />
      </div>
    );
  }

  if (!data) {
    return (
      <EmptyState
        icon={ReceiptText}
        title="Couldn't load the dashboard"
        text="The backend didn't respond. Make sure it's running and try again."
        actionLabel="Retry"
        onAction={() => setMonth((m) => m)}
      />
    );
  }

  const { summary, budget, categoryBudgets, categorySpending, monthlyTrend, dailyTrend, recentTransactions } = data;

  const donutData = categorySpending.map((c) => ({
    name: c.name,
    value: Number(c.value),
    color: c.color || "#34d399",
  }));

  const trendData = monthlyTrend.map((t) => ({
    month: shortMonth(t.month),
    income: Number(t.income),
    expense: Number(t.expense),
  }));

  const dailyData = dailyTrend.map((d) => ({ day: d.day, amount: Number(d.amount) }));

  const summaryCards = [
    {
      label: "Income",
      value: formatINR(summary.income),
      icon: ArrowDownLeft,
      gradient: "from-emerald-500/15 to-emerald-500/5",
      iconBg: "bg-emerald-500/15 text-emerald-300",
    },
    {
      label: "Expenses",
      value: formatINR(summary.expense),
      icon: ArrowUpRight,
      gradient: "from-rose-500/15 to-rose-500/5",
      iconBg: "bg-rose-500/15 text-rose-300",
    },
    {
      label: "Balance",
      value: formatINR(summary.balance),
      icon: Wallet,
      gradient: "from-sky-500/15 to-sky-500/5",
      iconBg: "bg-sky-500/15 text-sky-300",
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-sm text-zinc-500">Your money at a glance</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleAiReport}
            disabled={aiLoading}
            className="btn-ghost !py-2 text-xs"
          >
            <Bot className="h-4 w-4" />
            {aiLoading ? "Generating…" : "AI monthly report"}
          </button>
          <input
            type="month"
            value={month}
            max={currentMonth()}
            onChange={(e) => e.target.value && setMonth(e.target.value)}
            aria-label="Select month"
            className="input !w-auto [color-scheme:dark]"
          />
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid gap-4 md:grid-cols-3">
        {summaryCards.map(({ label, value, icon: Icon, gradient, iconBg }) => (
          <div key={label} className={`card p-5 bg-gradient-to-br ${gradient}`}>
            <div className="flex items-center justify-between">
              <span className="text-sm text-zinc-400">{label} · {monthLabel(month)}</span>
              <div className={`h-10 w-10 rounded-xl flex items-center justify-center ${iconBg}`}>
                <Icon className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-2 text-2xl font-bold text-white">{value}</div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-5">
          <h3 className="font-semibold text-zinc-100 mb-1">Spending by category</h3>
          <p className="text-xs text-zinc-500 mb-4">{monthLabel(month)}</p>
          {donutData.length === 0 ? (
            <div className="h-56 flex items-center justify-center text-sm text-zinc-500">
              No expenses this month
            </div>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={donutData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={3}
                    strokeWidth={0}
                  >
                    {donutData.map((d, i) => (
                      <Cell key={i} fill={d.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={chartTooltipStyle}
                    formatter={(value) => [formatINR(Number(value)), "Spent"]}
                  />
                  <Legend
                    layout="vertical"
                    align="right"
                    verticalAlign="middle"
                    formatter={(v) => <span className="text-xs text-zinc-400">{v}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="card p-5">
          <h3 className="font-semibold text-zinc-100 mb-1">6-month trend</h3>
          <p className="text-xs text-zinc-500 mb-4">Income vs expenses</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trendData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="gradIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#34d399" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#34d399" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="gradExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#fb7185" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#fb7185" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                <XAxis dataKey="month" tick={{ fill: "#71717a", fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: "#71717a", fontSize: 12 }} axisLine={false} tickLine={false} width={60}
                  tickFormatter={(v: number) => `₹${v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v}`} />
                <Tooltip
                  contentStyle={chartTooltipStyle}
                  formatter={(value, name) => [formatINR(Number(value)), name === "income" ? "Income" : "Expense"]}
                />
                <Legend formatter={(v) => <span className="text-xs text-zinc-400 capitalize">{v}</span>} />
                <Area type="monotone" dataKey="income" stroke="#34d399" strokeWidth={2} fill="url(#gradIncome)" />
                <Area type="monotone" dataKey="expense" stroke="#fb7185" strokeWidth={2} fill="url(#gradExpense)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Budget + daily trend */}
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <PiggyBank className="h-5 w-5 text-emerald-300" />
            <h3 className="font-semibold text-zinc-100">Budget · {monthLabel(month)}</h3>
          </div>
          <BudgetBar spent={budget.spent} budget={budget.budget} exceeded={budget.exceeded} nearLimit={budget.nearLimit} />
          <div className="grid grid-cols-2 gap-3 mt-5">
            <div className="rounded-xl bg-zinc-950/60 border border-zinc-800 p-3.5">
              <div className="text-xs text-zinc-500">Spent</div>
              <div className="text-lg font-bold text-rose-300">{formatINR(budget.spent)}</div>
            </div>
            <div className="rounded-xl bg-zinc-950/60 border border-zinc-800 p-3.5">
              <div className="text-xs text-zinc-500">Remaining</div>
              <div className="text-lg font-bold text-emerald-300">{formatINR(budget.remaining)}</div>
            </div>
          </div>
        </div>

        <div className="card p-5">
          <h3 className="font-semibold text-zinc-100 mb-1">Daily spending</h3>
          <p className="text-xs text-zinc-500 mb-4">{monthLabel(month)}</p>
          {dailyData.every((d) => d.amount === 0) ? (
            <div className="h-48 flex items-center justify-center text-sm text-zinc-500">
              No spending recorded yet
            </div>
          ) : (
            <div className="h-48">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={dailyData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="gradDaily" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#34d399" stopOpacity={0.35} />
                      <stop offset="100%" stopColor="#34d399" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                  <XAxis dataKey="day" tick={{ fill: "#71717a", fontSize: 11 }} axisLine={false} tickLine={false} interval={4} />
                  <YAxis hide />
                  <Tooltip contentStyle={chartTooltipStyle} formatter={(v) => [formatINR(Number(v)), "Spent"]} labelFormatter={(d) => `Day ${d}`} />
                  <Area type="monotone" dataKey="amount" stroke="#34d399" strokeWidth={2} fill="url(#gradDaily)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Category budgets */}
      {categoryBudgets && categoryBudgets.length > 0 && (
        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <PiggyBank className="h-5 w-5 text-emerald-300" />
            <h3 className="font-semibold text-zinc-100">Category budgets · {monthLabel(month)}</h3>
          </div>
          <div className="grid gap-x-8 gap-y-5 md:grid-cols-2">
            {categoryBudgets.map((cb) => {
              const p = pct(cb.spent, cb.budget);
              const barColor = cb.exceeded ? "bg-rose-500" : cb.nearLimit ? "bg-amber-400" : "bg-emerald-400";
              return (
                <div key={cb.category}>
                  <div className="flex items-baseline justify-between text-sm mb-1.5">
                    <span className="text-zinc-300 font-medium">
                      {cb.category}
                      {cb.rollover && (
                        <span className="badge bg-sky-500/10 text-sky-300 border border-sky-500/20 ml-2" title="Unused budget rolls over to next month">
                          rollover
                        </span>
                      )}
                    </span>
                    <span className="text-xs text-zinc-500">
                      <span className={cb.exceeded ? "text-rose-300 font-semibold" : "text-zinc-300"}>{formatINR(cb.spent)}</span>
                      {" "}of {formatINR(cb.budget)}
                    </span>
                  </div>
                  <div className="h-2.5 rounded-full bg-zinc-800 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${barColor}`}
                      style={{ width: `${Math.min(100, p)}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-zinc-600 mt-1">
                    <span>{p.toFixed(0)}% used</span>
                    <span>{cb.exceeded ? "Over budget" : `${formatINR(cb.remaining)} left`}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Spending anomalies */}
      {insights && insights.anomalies && insights.anomalies.length > 0 && (
        <div className="grid gap-3 md:grid-cols-2">
          {insights.anomalies.map((a, i) => (
            <div
              key={i}
              className="flex items-start gap-3 rounded-2xl border border-rose-500/25 bg-rose-500/5 px-5 py-4"
            >
              <TrendingUp className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
              <div className="text-sm">
                <span className="font-semibold text-rose-200">
                  {a.category} spending is {a.multiplier} its 8-week average
                </span>
                <span className="block text-xs text-rose-200/60 mt-1">
                  Recent spend {formatINR(a.recentSpend)} vs baseline average {formatINR(a.baselineAverage)}.
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Insights */}
      {insights && insights.insights.length > 0 && (
        <div className="card p-5">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb className="h-5 w-5 text-amber-300" />
            <h3 className="font-semibold text-zinc-100">Insights</h3>
          </div>
          <div className="space-y-3">
            {insights.insights.map((ins, i) => (
              <div
                key={i}
                className={`flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${
                  ins.severity === "warning"
                    ? "border-amber-500/25 bg-amber-500/5"
                    : ins.severity === "success"
                      ? "border-emerald-500/25 bg-emerald-500/5"
                      : "border-zinc-800 bg-zinc-950/40"
                }`}
              >
                {ins.severity === "warning" ? (
                  <AlertTriangle className="h-5 w-5 text-amber-300 shrink-0 mt-0.5" />
                ) : ins.severity === "success" ? (
                  <Sparkles className="h-5 w-5 text-emerald-300 shrink-0 mt-0.5" />
                ) : (
                  <Info className="h-5 w-5 text-sky-300 shrink-0 mt-0.5" />
                )}
                <div>
                  <div className="font-medium text-zinc-100">{ins.title}</div>
                  <div className="text-xs text-zinc-400 mt-0.5">{ins.message}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent transactions */}
      <div className="card p-5">
        <h3 className="font-semibold text-zinc-100 mb-2">Recent transactions</h3>
        {recentTransactions.length === 0 ? (
          <p className="text-sm text-zinc-500 py-6 text-center">No transactions yet this month.</p>
        ) : (
          <div className="divide-y divide-zinc-800/60">
            {recentTransactions.slice(0, 7).map((t) => (
              <div key={t.id} className="flex items-center gap-4 py-3">
                <div
                  className={`h-10 w-10 rounded-xl flex items-center justify-center text-lg font-bold shrink-0 ${
                    t.type === "income" ? "bg-emerald-500/10 text-emerald-300" : "bg-rose-500/10 text-rose-300"
                  }`}
                >
                  {t.type === "income" ? "+" : "−"}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="truncate text-sm font-medium text-zinc-200">
                    {t.note || t.category}
                  </div>
                  <div className="text-xs text-zinc-500">
                    {t.category} · {dateLabel(t.date)}
                  </div>
                </div>
                <div className={`text-sm font-semibold ${t.type === "income" ? "text-emerald-300" : "text-rose-300"}`}>
                  {t.type === "income" ? "+" : "−"}{formatINR(t.amount)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* AI monthly report */}
      <Modal
        open={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        title={`AI report · ${aiReport ? monthLabel(aiReport.month) : monthLabel(month)}`}
        maxWidth="max-w-2xl"
      >
        {aiReport && (
          <div className="flex items-start gap-3">
            <div className="h-10 w-10 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center shrink-0">
              <Bot className="h-5 w-5 text-violet-300" />
            </div>
            <div className="text-sm text-zinc-300 leading-relaxed whitespace-pre-wrap">
              {aiReport.report}
            </div>
          </div>
        )}
        <div className="flex justify-end mt-6">
          <button onClick={() => setAiModalOpen(false)} className="btn-ghost">
            <X className="h-4 w-4" /> Close
          </button>
        </div>
      </Modal>
    </div>
  );
}
