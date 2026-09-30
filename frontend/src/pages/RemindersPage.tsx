import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Bell, CalendarClock, Mail, Pencil, Plus, Repeat, Trash2 } from "lucide-react";
import { apiMessage } from "../api/client";
import { remindersApi, type ReminderInput } from "../api/reminders";
import type { ReminderDto } from "../api/types";
import ConfirmDialog from "../components/ConfirmDialog";
import EmptyState from "../components/EmptyState";
import ReminderModal from "../components/ReminderModal";
import { SkeletonCard } from "../components/Skeleton";
import { dateLabel, daysUntil, formatINR } from "../utils/format";

type Status = "overdue" | "dueSoon" | "upcoming";

function statusOf(dueDate: string): Status {
  const d = daysUntil(dueDate);
  if (d < 0) return "overdue";
  if (d <= 3) return "dueSoon";
  return "upcoming";
}

const STATUS_META: Record<Status, { label: string; card: string; pill: string }> = {
  overdue: {
    label: "Overdue",
    card: "border-rose-500/30",
    pill: "bg-rose-500/10 text-rose-300 border border-rose-500/25",
  },
  dueSoon: {
    label: "Due soon",
    card: "border-amber-500/30",
    pill: "bg-amber-500/10 text-amber-300 border border-amber-500/25",
  },
  upcoming: {
    label: "Upcoming",
    card: "border-zinc-800",
    pill: "bg-zinc-800 text-zinc-400 border border-zinc-700",
  },
};

export default function RemindersPage() {
  const [reminders, setReminders] = useState<ReminderDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ReminderDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ReminderDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      setReminders(await remindersApi.list());
    } catch (err) {
      toast.error(apiMessage(err, "Failed to load reminders"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const sorted = useMemo(
    () => [...reminders].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    [reminders],
  );

  const counts = useMemo(() => {
    const c: Record<Status, number> = { overdue: 0, dueSoon: 0, upcoming: 0 };
    for (const r of reminders) c[statusOf(r.dueDate)]++;
    return c;
  }, [reminders]);

  const handleCreate = async (input: ReminderInput) => {
    try {
      await remindersApi.create(input);
      toast.success("Reminder added");
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to add reminder"));
      throw err;
    }
  };

  const handleUpdate = async (input: ReminderInput) => {
    if (!editing) return;
    try {
      await remindersApi.update(editing.id, input);
      toast.success("Reminder updated");
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to update reminder"));
      throw err;
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await remindersApi.remove(deleteTarget.id);
      toast.success("Reminder deleted");
      setDeleteTarget(null);
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to delete reminder"));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Reminders</h1>
          <p className="text-sm text-zinc-500">
            {reminders.length === 0
              ? "Never miss a due date"
              : `${counts.overdue} overdue · ${counts.dueSoon} due soon`}
          </p>
        </div>
        <button
          onClick={() => {
            setEditing(null);
            setModalOpen(true);
          }}
          className="btn-primary !py-2 text-sm"
        >
          <Plus className="h-4 w-4" /> Add reminder
        </button>
      </div>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : sorted.length === 0 ? (
        <EmptyState
          icon={Bell}
          title="No reminders yet"
          text="Set reminders for bills and dues — recurring ones generate transactions automatically."
          actionLabel="Add reminder"
          onAction={() => {
            setEditing(null);
            setModalOpen(true);
          }}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {sorted.map((r) => {
            const status = statusOf(r.dueDate);
            const meta = STATUS_META[status];
            const d = daysUntil(r.dueDate);
            return (
              <div key={r.id} className={`card p-5 ${meta.card}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="font-semibold text-zinc-100 truncate">{r.title}</div>
                    <div className="text-xs text-zinc-500 mt-0.5">
                      {r.category}
                      {r.isRecurring && ` · ${r.recurrence}`}
                    </div>
                  </div>
                  <span className={`badge shrink-0 ${meta.pill}`}>
                    {status === "overdue"
                      ? `${Math.abs(d)}d overdue`
                      : status === "dueSoon"
                        ? d === 0 ? "Due today" : `Due in ${d}d`
                        : dateLabel(r.dueDate)}
                  </span>
                </div>

                <div className="flex items-center gap-4 mt-4 text-sm">
                  <span className="font-bold text-zinc-100">{formatINR(r.amount)}</span>
                  <span className="flex items-center gap-1.5 text-xs text-zinc-500">
                    <CalendarClock className="h-3.5 w-3.5" /> {dateLabel(r.dueDate)}
                  </span>
                  {r.isRecurring && (
                    <span className="flex items-center gap-1.5 text-xs text-zinc-500">
                      <Repeat className="h-3.5 w-3.5" /> {r.recurrence}
                    </span>
                  )}
                  {r.emailNotify && (
                    <span className="flex items-center gap-1.5 text-xs text-emerald-300/80">
                      <Mail className="h-3.5 w-3.5" /> email
                    </span>
                  )}
                </div>

                <div className="flex justify-end gap-1 mt-3">
                  <button
                    aria-label={`Edit ${r.title}`}
                    onClick={() => {
                      setEditing(r);
                      setModalOpen(true);
                    }}
                    className="p-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition"
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                  <button
                    aria-label={`Delete ${r.title}`}
                    onClick={() => setDeleteTarget(r)}
                    className="p-2 rounded-lg text-zinc-500 hover:text-rose-300 hover:bg-rose-500/10 transition"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <ReminderModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
        }}
        onSubmit={editing ? handleUpdate : handleCreate}
        editing={editing}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete reminder"
        message={`Delete the "${deleteTarget?.title}" reminder?`}
        busy={deleting}
      />
    </div>
  );
}
