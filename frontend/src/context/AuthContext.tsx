import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { authApi } from "../api/auth";
import { onUnauthorized } from "../api/client";
import type { UserDto } from "../api/types";

interface AuthState {
  user: UserDto | null;
  loading: boolean; // boot-time /auth/me probe
  busy: boolean; // login/register/logout in flight
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const userRef = useRef(user);
  userRef.current = user;

  // Any 401 from a non-/me call → session is dead → clear user.
  // App redirects via ProtectedRoute.
  useEffect(() => {
    onUnauthorized(() => {
      setUser(null);
    });
  }, []);

  useEffect(() => {
    let cancelled = false;
    authApi
      .me()
      .then((u) => {
        if (!cancelled) setUser(u);
      })
      .catch(() => {
        // Not signed in — stay on the logged-out state; no redirect from here.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setBusy(true);
    try {
      const u = await authApi.login(email, password);
      setUser(u);
    } finally {
      setBusy(false);
    }
  }, []);

  const register = useCallback(async (name: string, email: string, password: string) => {
    setBusy(true);
    try {
      const u = await authApi.register(name, email, password);
      setUser(u);
    } finally {
      setBusy(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setBusy(true);
    try {
      await authApi.logout();
    } catch {
      // Even if the call fails, the session is over client-side.
    } finally {
      setUser(null);
      setBusy(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, busy, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
