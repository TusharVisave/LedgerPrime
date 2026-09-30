import axios, { type AxiosError } from "axios";
import type { ApiError } from "./types";

// Cookie-based JWT: the backend sets httpOnly cookies, so we MUST send
// credentials on every request and MUST NOT touch localStorage tokens.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080/api",
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

export function onUnauthorized(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler;
}

api.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      // Don't redirect for the boot-time /auth/me probe — AuthContext
      // handles that case itself.
      const url = error.config?.url ?? "";
      if (!url.endsWith("/auth/me")) {
        unauthorizedHandler?.();
      }
    }
    return Promise.reject(error);
  },
);

/** Extract a human-readable message from an Axios error. */
export function apiMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(err)) {
    const data = err.response?.data;
    if (data?.message) return data.message;
    if (err.response?.status === 0 || !err.response) return "Network error — is the backend running?";
  }
  return fallback;
}

/** Extract backend field errors: { field: message }. */
export function apiFieldErrors(err: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(err)) {
    return err.response?.data?.fieldErrors ?? {};
  }
  return {};
}

/** Download a blob response as a file (PDF / XLSX exports). */
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
