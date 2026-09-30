import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { BarChart3, BellRing, PiggyBank } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { apiFieldErrors, apiMessage } from "../api/client";
import { PASSWORD_HINT } from "./ProfilePage";

type Mode = "login" | "register";

const FEATURES = [
  {
    icon: PiggyBank,
    title: "Budgets that warn you",
    text: "Set monthly budgets and get amber/red alerts before you overshoot.",
  },
  {
    icon: BarChart3,
    title: "Charts that make sense",
    text: "Category donuts, 6-month trends and daily spending — all INR formatted.",
  },
  {
    icon: BellRing,
    title: "Never miss a bill",
    text: "Recurring reminders with email notifications keep dues on track.",
  },
];

export default function AuthPage() {
  const { user, login, register, busy } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const [mode, setMode] = useState<Mode>("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  if (user) return <Navigate to={from} replace />;

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    setFieldErrors({});
    try {
      if (mode === "login") {
        await login(email.trim(), password);
        toast.success("Welcome back!");
      } else {
        await register(name.trim(), email.trim(), password);
        toast.success("Account created — welcome!");
      }
      navigate(from, { replace: true });
    } catch (err) {
      const fe = apiFieldErrors(err);
      if (Object.keys(fe).length > 0) {
        setFieldErrors(fe);
      } else {
        toast.error(apiMessage(err, mode === "login" ? "Login failed" : "Registration failed"));
      }
    }
  };

  const switchMode = (m: Mode) => {
    setMode(m);
    setFieldErrors({});
  };

  const fieldErr = (f: string) =>
    fieldErrors[f] ? <p className="mt-1 text-xs text-rose-400">{fieldErrors[f]}</p> : null;

  return (
    <div className="min-h-screen bg-zinc-950 flex">
      {/* Left: brand panel */}
      <div className="hidden lg:flex w-[45%] relative overflow-hidden flex-col justify-between p-12 bg-gradient-to-br from-emerald-950 via-zinc-950 to-zinc-950 border-r border-zinc-800/60">
        <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-emerald-500/15 blur-3xl" />
        <div className="absolute -bottom-40 right-0 h-96 w-96 rounded-full bg-emerald-600/10 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="h-11 w-11 rounded-xl bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center">
            <span className="text-2xl font-extrabold text-zinc-950">₹</span>
          </div>
          <div className="leading-tight">
            <div className="text-lg font-bold text-white">LedgerPrime</div>
            <div className="text-xs text-zinc-400">Your money, decoded</div>
          </div>
        </div>

        <div className="relative">
          <h1 className="text-4xl font-extrabold text-white leading-tight">
            Know where every
            <br />
            rupee goes<span className="text-emerald-400">.</span>
          </h1>
          <p className="mt-4 text-zinc-400 max-w-md">
            A premium fintech-grade expense tracker: budgets, insights, reminders and
            beautiful INR charts — all in one dark, focused workspace.
          </p>
          <div className="mt-10 space-y-6">
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <div key={title} className="flex gap-4">
                <div className="h-11 w-11 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center shrink-0">
                  <Icon className="h-5 w-5 text-emerald-300" />
                </div>
                <div>
                  <div className="font-semibold text-zinc-100">{title}</div>
                  <div className="text-sm text-zinc-500 mt-0.5 max-w-sm">{text}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <p className="relative text-xs text-zinc-600">Cookie-secured sessions · No tokens in storage</p>
      </div>

      {/* Right: auth card */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center gap-3 mb-8 justify-center">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center">
              <span className="text-xl font-extrabold text-zinc-950">₹</span>
            </div>
            <div className="text-lg font-bold text-white">LedgerPrime</div>
          </div>

          <div className="card p-8">
            <div className="grid grid-cols-2 gap-1 p-1 bg-zinc-950/60 border border-zinc-800 rounded-xl mb-8">
              {(["login", "register"] as Mode[]).map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => switchMode(m)}
                  className={`py-2 rounded-lg text-sm font-semibold capitalize transition ${
                    mode === m ? "bg-emerald-500 text-zinc-950" : "text-zinc-500 hover:text-zinc-200"
                  }`}
                >
                  {m === "login" ? "Sign in" : "Sign up"}
                </button>
              ))}
            </div>

            <h2 className="text-xl font-bold text-white">
              {mode === "login" ? "Welcome back" : "Create your account"}
            </h2>
            <p className="text-sm text-zinc-500 mt-1 mb-6">
              {mode === "login"
                ? "Sign in to continue tracking your expenses."
                : "Start tracking in under a minute."}
            </p>

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              {mode === "register" && (
                <div>
                  <label className="label" htmlFor="auth-name">Name</label>
                  <input
                    id="auth-name"
                    className={`input ${fieldErrors.name ? "input-error" : ""}`}
                    placeholder="Tushar Visave"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    autoComplete="name"
                  />
                  {fieldErr("name")}
                </div>
              )}
              <div>
                <label className="label" htmlFor="auth-email">Email</label>
                <input
                  id="auth-email"
                  type="email"
                  className={`input ${fieldErrors.email ? "input-error" : ""}`}
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                />
                {fieldErr("email")}
              </div>
              <div>
                <label className="label" htmlFor="auth-password">Password</label>
                <input
                  id="auth-password"
                  type="password"
                  className={`input ${fieldErrors.password ? "input-error" : ""}`}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete={mode === "login" ? "current-password" : "new-password"}
                />
                {mode === "register" && !fieldErrors.password && (
                  <p className="mt-1.5 text-xs text-zinc-500">{PASSWORD_HINT}</p>
                )}
                {fieldErr("password")}
              </div>
              <button type="submit" className="btn-primary w-full !py-3" disabled={busy}>
                {busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"}
              </button>
            </form>

            <p className="text-center text-sm text-zinc-500 mt-6">
              {mode === "login" ? "New here? " : "Already have an account? "}
              <button
                type="button"
                onClick={() => switchMode(mode === "login" ? "register" : "login")}
                className="text-emerald-400 font-medium hover:text-emerald-300"
              >
                {mode === "login" ? "Create an account" : "Sign in"}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
