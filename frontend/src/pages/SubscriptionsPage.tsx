import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { CalendarClock, RefreshCw } from "lucide-react";
import { subscriptionsApi } from "../api/subscriptions";
import { apiMessage } from "../api/client";
import type { SubscriptionsDto } from "../api/types";
import EmptyState from "../components/EmptyState";
import { SkeletonRow } from "../components/Skeleton";
import { dateLabel, formatINR } from "../utils/format";

export default function SubscriptionsPage() {
  const [data, setData] = useState<SubscriptionsDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    subscriptionsApi
      .list()
      .then((d) => {
        if (!cancelled) setData(d);
      })
      .catch((err) => {
        if (!cancelled) toast.error(apiMessage(err, "Failed to load subscriptions"));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="animate-pulse h-8 w-56 rounded bg-zinc-800" />
        <div className="card p-5">
          <SkeletonRow /> <SkeletonRow /> <SkeletonRow />
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <EmptyState
        icon={RefreshCw}
        title="Couldn't load subscriptions"
        text="The backend didn't respond. Make sure it's running and try again."
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Subscriptions</h1>
          <p className="text-sm text-zinc-500">Recurring reminders, summed monthly</p>
        </div>
      </div>

      {/* Total monthly cost */}
      <div className="card p-6 bg-gradient-to-br from-violet-500/10 to-violet-500/5">
        <div className="flex items-center gap-3">
          <div className="h-11 w-11 rounded-xl bg-violet-500/15 border border-violet-500/25 flex items-center justify-center">
            <RefreshCw className="h-5 w-5 text-violet-300" />
          </div>
          <div>
            <div className="text-xs text-zinc-500">Total monthly cost</div>
            <div className="text-2xl font-bold text-white">{formatINR(data.totalMonthlyCost)}</div>
          </div>
          <div className="ml-auto text-xs text-zinc-500">
            {data.count} recurring {data.count === 1 ? "reminder" : "reminders"}
          </div>
        </div>
      </div>

      {data.subscriptions.length === 0 ? (
        <EmptyState
          icon={CalendarClock}
          title="No subscriptions"
          text="No subscriptions — mark a reminder as recurring to see it here."
        />
      ) : (
        <div className="card overflow-hidden">
          <div className="divide-y divide-zinc-800/60">
            {data.subscriptions.map((s) => (
              <div key={s.id} className="flex items-center gap-4 px-5 py-4">
                <div className="h-10 w-10 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center shrink-0">
                  <RefreshCw className="h-4 w-4 text-violet-300" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="truncate text-sm font-medium text-zinc-100">{s.title}</div>
                  <div className="text-xs text-zinc-500 mt-0.5">
                    {s.category} · next due {dateLabel(s.dueDate)} · {s.recurrence}
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <div className="text-sm font-semibold text-zinc-100">
                    {formatINR(s.monthlyCost)}
                    <span className="text-xs font-normal text-zinc-500">/mo</span>
                  </div>
                  <div className="text-[11px] text-zinc-600">{formatINR(s.amount)} per {s.recurrence}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
