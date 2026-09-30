import { api } from "./client";
import type { UserDto } from "./types";

export const authApi = {
  me: () => api.get<UserDto>("/auth/me").then((r) => r.data),
  login: (email: string, password: string) =>
    api.post<UserDto>("/auth/login", { email, password }).then((r) => r.data),
  register: (name: string, email: string, password: string) =>
    api.post<UserDto>("/auth/register", { name, email, password }).then((r) => r.data),
  refresh: () => api.post<UserDto>("/auth/refresh").then((r) => r.data),
  logout: () => api.post<{ message: string }>("/auth/logout").then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<{ message: string }>("/auth/change-password", { currentPassword, newPassword }).then((r) => r.data),
  logoutAll: () => api.post<{ message: string }>("/auth/logout-all").then((r) => r.data),
};
