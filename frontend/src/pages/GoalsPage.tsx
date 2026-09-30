import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Pencil, PiggyBank, Plus, Target, Trash2 } from "lucide-react";
import { goalsApi } from "../api/goals";
import { apiMessage } from "../api/client";
import type { GoalAllocateInput, GoalDto, GoalInput } from "../api/types";
import AllocateModal from "../components/AllocateModal";
import ConfirmDialog from "../components/ConfirmDialog";
import EmptyState from "../components/EmptyState";
import GoalModal from "../components/GoalModal";
import { SkeletonCard } from "../components/Skeleton";
import { dateLabel, formatINR } from "../utils/format";

export default function GoalsPage() {
  const [goals, setGoals] = useState<GoalDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<GoalDto | null>(null);
  const [allocating, setAllocating] = useState<GoalDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<GoalDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = () => {
    setLoading(true);
    goalsApi
      .list()
      .then(setGoals)
      .catch((err) => toast.error(apiMessage(err, "Failed to load goals")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleCreate = async (input: GoalInput) => {
    try {
      await goalsApi.create(input);
      toast.success("Goal created");
      load();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to create goal"));
      throw err;
    }
  };

  const handleUpdate = async (input: GoalInput) => {
    if (!editing) return;
    try {
      await goalsApi.update(editing.id, input);
      toast.success("Goal updated");
      load();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to update goal"));
      throw err;
    }
  };

  const handleAllocate = async (input: GoalAllocateInput) => {
    if (!allocating) return;
    try {
      await goalsApi.allocate(allocating.id, input);
      toast.success(`Allocated ${formatINR(Number(input.amount).toFixed(2))} to ${allocating.name}`);
      load();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to allocate"));
      throw err;
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await goalsApi.remove(deleteTarget.id);
      toast.success("Goal deleted");
      setDeleteTarget(null);
      load();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to delete goal"));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Goals</h1>
          <p className="text-sm text-zinc-500">Save toward what matters, one allocation at a time</p>
        </div>
        <button
          onClick={() => {
            setEditing(null);
            setModalOpen(true);
          }}
          className="btn-primary !py-2 text-sm"
        >
          <Plus className="h-4 w-4" /> New goal
        </button>
      </div>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <SkeletonCard /> <SkeletonCard /> <SkeletonCard />
        </div>
      ) : goals.length === 0 ? (
        <EmptyState
          icon={Target}
          title="No goals yet"
          text="Set a savings goal — a vacation, an emergency fund, a new phone — and watch it fill up."
          actionLabel="Create your first goal"
          onAction={() => {
            setEditing(null);
            setModalOpen(true);
          }}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {goals.map((g) => (
            <div key={g.id} className="card p-5 flex flex-col">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <h3 className="font-semibold text-zinc-100 truncate">{g.name}</h3>
                  {g.deadline && (
                    <p className="text-xs text-zinc-500 mt-0.5">Due {dateLabel(g.deadline)}</p>
                  )}
                </div>
                <div className="flex gap-1 shrink-0">
                  <button
                    aria-label="Edit goal"
                    onClick={() => {
                      setEditing(g);
                      setModalOpen(true);
                    }}
                    className="p-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition"
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                  <button
                    aria-label="Delete goal"
                    onClick={() => setDeleteTarget(g)}
                    className="p-2 rounded-lg text-zinc-500 hover:text-rose-300 hover:bg-rose-500/10 transition"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>

              <div className="mt-4">
                <div className="flex items-baseline justify-between text-sm mb-2">
                  <span className="font-bold text-white">{formatINR(g.allocated)}</span>
                  <span className="text-zinc-500">of {formatINR(g.targetAmount)}</span>
                </div>
                <div className="h-3 rounded-full bg-zinc-800 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-emerald-400 transition-all duration-500"
                    style={{ width: `${Math.min(100, Math.max(0, g.progressPercent))}%` }}
                  />
                </div>
                <div className="flex items-center justify-between mt-1.5 text-xs">
                  <span className="text-emerald-300 font-medium">
                    {g.progressPercent.toFixed(1)}%
                  </span>
                  <span className="text-zinc-500">{formatINR(g.remaining)} left</span>
                </div>
              </div>

              <button
                onClick={() => setAllocating(g)}
                className="btn-ghost w-full mt-4 !py-2 text-sm"
              >
                <PiggyBank className="h-4 w-4" /> Allocate money
              </button>
            </div>
          ))}
        </div>
      )}

      <GoalModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
        }}
        onSubmit={editing ? handleUpdate : handleCreate}
        editing={editing}
      />

      <AllocateModal
        open={!!allocating}
        onClose={() => setAllocating(null)}
        onSubmit={handleAllocate}
        goal={allocating}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete goal"
        message={`Delete "${deleteTarget?.name}" and its allocation history? This can't be undone.`}
        busy={deleting}
      />
    </div>
  );
}
