import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Pencil, Plus, Tags, Trash2 } from "lucide-react";
import { categoriesApi, type CategoryInput } from "../api/categories";
import { apiMessage } from "../api/client";
import type { CategoryDto } from "../api/types";
import CategoryModal from "../components/CategoryModal";
import ConfirmDialog from "../components/ConfirmDialog";
import EmptyState from "../components/EmptyState";
import { SkeletonCard } from "../components/Skeleton";

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CategoryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CategoryDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      setCategories(await categoriesApi.list());
    } catch (err) {
      toast.error(apiMessage(err, "Failed to load categories"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleCreate = async (input: CategoryInput) => {
    try {
      await categoriesApi.create(input);
      toast.success(`Category "${input.name}" added`);
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to add category"));
      throw err;
    }
  };

  const handleUpdate = async (input: CategoryInput) => {
    if (!editing) return;
    try {
      await categoriesApi.update(editing.id, input);
      toast.success("Category updated");
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to update category"));
      throw err;
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await categoriesApi.remove(deleteTarget.id);
      toast.success(`Category "${deleteTarget.name}" deleted`);
      setDeleteTarget(null);
      fetchAll();
    } catch (err) {
      toast.error(apiMessage(err, "Failed to delete category"));
    } finally {
      setDeleting(false);
    }
  };

  const incomeCats = categories.filter((c) => c.type === "income");
  const expenseCats = categories.filter((c) => c.type === "expense");

  const grid = (list: CategoryDto[]) => (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {list.map((c) => (
        <div key={c.id} className="card p-5 hover:border-zinc-700 transition group">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3 min-w-0">
              <span
                className="h-11 w-11 rounded-xl flex items-center justify-center shrink-0"
                style={{ backgroundColor: `${c.color}1f`, border: `1px solid ${c.color}55` }}
              >
                <span className="h-3.5 w-3.5 rounded-full" style={{ backgroundColor: c.color }} />
              </span>
              <div className="min-w-0">
                <div className="font-semibold text-zinc-100 truncate">{c.name}</div>
                <div className="text-xs text-zinc-500 truncate">icon: {c.icon || "tag"}</div>
              </div>
            </div>
            <div className="flex gap-0.5 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition shrink-0">
              <button
                aria-label={`Edit ${c.name}`}
                onClick={() => {
                  setEditing(c);
                  setModalOpen(true);
                }}
                className="p-2 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-800 transition"
              >
                <Pencil className="h-4 w-4" />
              </button>
              <button
                aria-label={`Delete ${c.name}`}
                disabled={c.isDefault}
                title={c.isDefault ? "Default categories can't be deleted" : `Delete ${c.name}`}
                onClick={() => setDeleteTarget(c)}
                className="p-2 rounded-lg text-zinc-500 hover:text-rose-300 hover:bg-rose-500/10 transition disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-zinc-500"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <span className={`badge ${c.type === "income" ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20" : "bg-rose-500/10 text-rose-300 border border-rose-500/20"}`}>
              {c.type}
            </span>
            {c.isDefault && (
              <span className="badge bg-zinc-800 text-zinc-400 border border-zinc-700">default</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-3 pt-12 md:pt-0">
        <div>
          <h1 className="text-2xl font-bold text-white">Categories</h1>
          <p className="text-sm text-zinc-500">Organise income and expenses your way</p>
        </div>
        <button
          onClick={() => {
            setEditing(null);
            setModalOpen(true);
          }}
          className="btn-primary !py-2 text-sm"
        >
          <Plus className="h-4 w-4" /> Add category
        </button>
      </div>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : categories.length === 0 ? (
        <EmptyState
          icon={Tags}
          title="No categories yet"
          text="Create your first category to start organising transactions."
          actionLabel="Add category"
          onAction={() => {
            setEditing(null);
            setModalOpen(true);
          }}
        />
      ) : (
        <>
          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500 mb-4">
              Expenses · {expenseCats.length}
            </h2>
            {expenseCats.length > 0 ? (
              grid(expenseCats)
            ) : (
              <p className="text-sm text-zinc-600">No expense categories.</p>
            )}
          </section>
          <section>
            <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500 mb-4">
              Income · {incomeCats.length}
            </h2>
            {incomeCats.length > 0 ? (
              grid(incomeCats)
            ) : (
              <p className="text-sm text-zinc-600">No income categories.</p>
            )}
          </section>
        </>
      )}

      <CategoryModal
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
        title="Delete category"
        message={`Delete the "${deleteTarget?.name}" category? Transactions using it will keep their label.`}
        busy={deleting}
      />
    </div>
  );
}
