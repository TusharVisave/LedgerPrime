import { api } from "./client";
import type { DashboardDto } from "./types";

export const dashboardApi = {
  get: (month: string) =>
    api.get<DashboardDto>("/dashboard", { params: { month } }).then((r) => r.data),
};
