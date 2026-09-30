import { api } from "./client";
import type { GoalAllocateInput, GoalDto, GoalInput } from "./types";

export const goalsApi = {
  list: () => api.get<GoalDto[]>("/goals").then((r) => r.data),
  get: (id: number) => api.get<GoalDto>(`/goals/${id}`).then((r) => r.data),
  create: (input: GoalInput) => api.post<GoalDto>("/goals", input).then((r) => r.data),
  update: (id: number, input: GoalInput) =>
    api.put<GoalDto>(`/goals/${id}`, input).then((r) => r.data),
  remove: (id: number) => api.delete(`/goals/${id}`).then((r) => r.data),
  allocate: (id: number, input: GoalAllocateInput) =>
    api.post<GoalDto>(`/goals/${id}/allocate`, input).then((r) => r.data),
};
