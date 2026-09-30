import { api } from "./client";
import type { ReminderDto } from "./types";

export interface ReminderInput {
  title: string;
  amount?: string; // decimal string, default "0"
  category?: string; // default "Bills"
  dueDate: string; // YYYY-MM-DD
  isRecurring?: boolean;
  recurrence?: string; // default "monthly"
  emailNotify?: boolean;
}

export const remindersApi = {
  list: () => api.get<ReminderDto[]>("/reminders").then((r) => r.data),
  upcoming: (days = 7) =>
    api.get<ReminderDto[]>("/reminders/upcoming", { params: { days } }).then((r) => r.data),
  create: (input: ReminderInput) => api.post<ReminderDto>("/reminders", input).then((r) => r.data),
  update: (id: number, input: ReminderInput) =>
    api.put<ReminderDto>(`/reminders/${id}`, input).then((r) => r.data),
  remove: (id: number) => api.delete<{ message: string }>(`/reminders/${id}`).then((r) => r.data),
};
