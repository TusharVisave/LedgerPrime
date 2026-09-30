import { api } from "./client";
import type { CategoryDto } from "./types";

export interface CategoryInput {
  name: string;
  type: "income" | "expense";
  color?: string;
  icon?: string;
}

export const categoriesApi = {
  list: () => api.get<CategoryDto[]>("/categories").then((r) => r.data),
  create: (input: CategoryInput) => api.post<CategoryDto>("/categories", input).then((r) => r.data),
  update: (id: number, input: CategoryInput) =>
    api.put<CategoryDto>(`/categories/${id}`, input).then((r) => r.data),
  remove: (id: number) => api.delete<{ message: string }>(`/categories/${id}`).then((r) => r.data),
};
