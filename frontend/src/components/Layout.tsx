import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  Bell,
  LayoutDashboard,
  LogOut,
  PiggyBank,
  ReceiptText,
  RefreshCw,
  Settings,
  Tags,
  Target,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

const NAV = [
  { to: "/", label: "Dashboard", short: "Home", icon: LayoutDashboard, end: true },
  { to: "/transactions", label: "Transactions", short: "Trans.", icon: ReceiptText, end: false },
  { to: "/goals", label: "Goals", short: "Goals", icon: Target, end: false },
  { to: "/subscriptions", label: "Subscriptions", short: "Subs.", icon: RefreshCw, end: false },
  { to: "/budgets", label: "Budgets", short: "Budget", icon: PiggyBank, end: false },
  { to: "/categories", label: "Categories", short: "Categ.", icon: Tags, end: false },
  { to: "/reminders", label: "Reminders", short: "Remind.", icon: Bell, end: false },
];

function Brand() {
  return (
    <div className="flex items-center gap-3 px-2">
      <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center shrink-0">
        <span className="text-xl font-extrabold text-zinc-950">₹</span>
      </div>
      <div className="leading-tight">
        <div className="font-bold text-zinc-100">LedgerPrime</div>
        <div className="text-[11px] text-zinc-500">Track every rupee</div>
      </div>
    </div>
  );
}

function NavItems({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="flex flex-col gap-1">
      {NAV.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition ${
              isActive
                ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20"
                : "text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/60 border border-transparent"
            }`
          }
        >
          <Icon className="h-[18px] w-[18px]" />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex fixed inset-y-0 left-0 w-64 flex-col border-r border-zinc-800/80 bg-zinc-950/95 px-4 py-6 z-20">
        <Brand />
        <div className="mt-8 flex-1">
          <NavItems />
        </div>
        <div className="border-t border-zinc-800/80 pt-4">
          <div className="flex items-center gap-3 px-2">
            <div className="h-9 w-9 rounded-full bg-zinc-800 flex items-center justify-center text-sm font-bold text-emerald-300 shrink-0">
              {user?.name?.charAt(0).toUpperCase() ?? "?"}
            </div>
            <div className="min-w-0 flex-1 leading-tight">
              <div className="truncate text-sm font-medium text-zinc-200">{user?.name}</div>
              <div className="truncate text-xs text-zinc-500">{user?.email}</div>
            </div>
          </div>
          <button
            onClick={() => navigate("/profile")}
            className="mt-3 w-full flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/60 transition"
          >
            <Settings className="h-[18px] w-[18px]" />
            Profile & security
          </button>
          <button
            onClick={handleLogout}
            className="mt-1 w-full flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium text-zinc-400 hover:text-rose-300 hover:bg-rose-500/10 transition"
          >
            <LogOut className="h-[18px] w-[18px]" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="md:pl-64 pb-24 md:pb-10">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6 md:py-8">
          <Outlet />
        </div>
      </main>

      {/* Mobile top bar */}
      <header className="md:hidden fixed top-0 inset-x-0 z-20 bg-zinc-950/95 backdrop-blur border-b border-zinc-800/80">
        <div className="flex items-center justify-between px-4 py-3">
          <Brand />
          <div className="flex items-center gap-1">
            <button
              onClick={() => navigate("/profile")}
              aria-label="Profile and security"
              className="p-2 rounded-xl text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/60 transition"
            >
              <Settings className="h-5 w-5" />
            </button>
            <button
              onClick={handleLogout}
              aria-label="Sign out"
              className="p-2 rounded-xl text-zinc-400 hover:text-rose-300 hover:bg-rose-500/10 transition"
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Mobile bottom tab bar */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-20 bg-zinc-950/95 backdrop-blur border-t border-zinc-800/80">
        <div className="grid grid-cols-7 px-1 pb-[env(safe-area-inset-bottom)]">
          {NAV.map(({ to, short, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `flex flex-col items-center gap-1 py-2.5 text-[9px] font-medium transition ${
                  isActive ? "text-emerald-300" : "text-zinc-500"
                }`
              }
            >
              <Icon className="h-5 w-5" />
              {short}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  );
}
