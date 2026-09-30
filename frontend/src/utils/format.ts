// INR formatting + small display helpers.
// Decimal-string amounts from the API are parsed with Number() ONLY for
// display; nothing is rounded or written back from these values.

const inr = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

export function formatINR(amount: string | number): string {
  const n = typeof amount === "string" ? Number(amount) : amount;
  if (!Number.isFinite(n)) return inr.format(0);
  return inr.format(n);
}

export function formatNumber(amount: string | number): string {
  const n = typeof amount === "string" ? Number(amount) : amount;
  if (!Number.isFinite(n)) return "0";
  return n.toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

/** "2026-09" → "Sep 2026" */
export function monthLabel(month: string): string {
  const [y, m] = month.split("-").map(Number);
  if (!y || !m) return month;
  return new Date(y, m - 1, 1).toLocaleString("en-IN", { month: "short", year: "numeric" });
}

/** "2026-04" → "Apr" (short month name for charts) */
export function shortMonth(month: string): string {
  const [y, m] = month.split("-").map(Number);
  if (!y || !m) return month;
  return new Date(y, m - 1, 1).toLocaleString("en-IN", { month: "short" });
}

/** "2026-09-28" → "28 Sep 2026" */
export function dateLabel(isoDate: string): string {
  const d = new Date(isoDate + "T00:00:00");
  if (Number.isNaN(d.getTime())) return isoDate;
  return d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

/** Current month as "YYYY-MM". */
export function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

/** Today as "YYYY-MM-DD". */
export function today(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}`;
}

/** Days from today to a YYYY-MM-DD date (negative = overdue). */
export function daysUntil(isoDate: string): number {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const target = new Date(isoDate + "T00:00:00");
  return Math.round((target.getTime() - now.getTime()) / 86_400_000);
}

export function pct(spent: string, budget: string): number {
  const s = Number(spent);
  const b = Number(budget);
  if (!Number.isFinite(s) || !Number.isFinite(b) || b <= 0) return 0;
  return Math.min(100, (s / b) * 100);
}
