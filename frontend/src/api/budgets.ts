import { api } from "./client";
import type { BudgetStatus, CategoryBudgetStatus } from "./types";

export interface BudgetUpsertInput {
  month: string;
  amount: string; // decimal string
  category?: string;
  rollover?: boolean;
}

export const budgetsApi = {
  get: (month: string) => api.get<BudgetStatus>("/budgets", { params: { month } }).then((r) => r.data),
  upsert: (month: string, amount: string, category?: string, rollover?: boolean) => {
    const body: BudgetUpsertInput = { month, amount };
    if (category) body.category = category;
    if (rollover !== undefined) body.rollover = rollover;
    return api.put<BudgetStatus>("/budgets", body).then((r) => r.data);
  },
  /** Per-category budgets for a month. */
  categories: (month: string) =>
    api.get<CategoryBudgetStatus[]>("/budgets/categories", { params: { month } }).then((r) => r.data),
};
