import { api } from "./client";
import type { AiReportDto, InsightsDto } from "./types";

export const insightsApi = {
  get: (month?: string) =>
    api.get<InsightsDto>("/insights", { params: month ? { month } : {} }).then((r) => r.data),
  /** AI monthly report — returns 501 when the backend has no API key configured. */
  aiReport: (month?: string) =>
    api.post<AiReportDto>("/insights/ai-report", month ? { month } : {}).then((r) => r.data),
};
