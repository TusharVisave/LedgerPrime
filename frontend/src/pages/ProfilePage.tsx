import { useState } from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { KeyRound, LogOut, MonitorSmartphone, UserRound } from "lucide-react";
import { authApi } from "../api/auth";
import { apiFieldErrors, apiMessage } from "../api/client";
import ConfirmDialog from "../components/ConfirmDialog";
import { useAuth } from "../context/AuthContext";

export const PASSWORD_HINT = "Minimum 8 characters, with at least one letter and one digit.";

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [changing, setChanging] = useState(false);
  const [logoutAllConfirm, setLogoutAllConfirm] = useState(false);
  const [loggingOutAll, setLoggingOutAll] = useState(false);

  const fieldErr = (f: string) =>
    fieldErrors[f] ? <p className="mt-1 text-xs text-rose-400">{fieldErrors[f]}</p> : null;

  const handleChangePassword = async (ev: React.FormEvent) => {
    ev.preventDefault();
    const fe: Record<string, string> = {};
    if (!currentPassword) fe.currentPassword = "Enter your current password.";
    if (newPassword !== confirmPassword) fe.confirmPassword = "Passwords don't match.";
    setFieldErrors(fe);
    if (Object.keys(fe).length > 0) return;

    setChanging(true);
    try {
      await authApi.changePassword(currentPassword, newPassword);
      // Cookies are cleared by the backend — the session is over.
      toast.success("Password changed. Please sign in again.");
      await logout();
      navigate("/login", { replace: true });
    } catch (err) {
      const serverFe = apiFieldErrors(err);
      if (Object.keys(serverFe).length > 0) {
        setFieldErrors(serverFe);
      } else {
        toast.error(apiMessage(err, "Failed to change password"));
      }
    } finally {
      setChanging(false);
    }
  };

  const handleLogoutAll = async () => {
    setLoggingOutAll(true);
    try {
      await authApi.logoutAll();
      toast.success("Signed out from all devices.");
      await logout();
      navigate("/login", { replace: true });
    } catch (err) {
      toast.error(apiMessage(err, "Failed to sign out everywhere"));
    } finally {
      setLoggingOutAll(false);
      setLogoutAllConfirm(false);
    }
  };

  return (
    <div className="space-y-6 max-w-2xl">
      <div className="pt-12 md:pt-0">
        <h1 className="text-2xl font-bold text-white">Profile & security</h1>
        <p className="text-sm text-zinc-500">Manage your account and sessions</p>
      </div>

      {/* Account */}
      <div className="card p-6">
        <div className="flex items-center gap-4">
          <div className="h-12 w-12 rounded-full bg-zinc-800 flex items-center justify-center text-lg font-bold text-emerald-300 shrink-0">
            {user?.name?.charAt(0).toUpperCase() ?? "?"}
          </div>
          <div className="min-w-0">
            <div className="font-semibold text-zinc-100 truncate">{user?.name}</div>
            <div className="text-sm text-zinc-500 truncate">{user?.email}</div>
          </div>
        </div>
      </div>

      {/* Change password */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-5">
          <div className="h-10 w-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center">
            <KeyRound className="h-5 w-5 text-emerald-300" />
          </div>
          <div>
            <h2 className="font-semibold text-zinc-100">Change password</h2>
            <p className="text-xs text-zinc-500">You'll be signed out and asked to sign in again.</p>
          </div>
        </div>
        <form onSubmit={handleChangePassword} className="space-y-4" noValidate>
          <div>
            <label className="label" htmlFor="pw-current">Current password</label>
            <input
              id="pw-current"
              type="password"
              className={`input ${fieldErrors.currentPassword ? "input-error" : ""}`}
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
            />
            {fieldErr("currentPassword")}
          </div>
          <div>
            <label className="label" htmlFor="pw-new">New password</label>
            <input
              id="pw-new"
              type="password"
              className={`input ${fieldErrors.newPassword ? "input-error" : ""}`}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
            />
            <p className="mt-1.5 text-xs text-zinc-500">{PASSWORD_HINT}</p>
            {fieldErr("newPassword")}
          </div>
          <div>
            <label className="label" htmlFor="pw-confirm">Confirm new password</label>
            <input
              id="pw-confirm"
              type="password"
              className={`input ${fieldErrors.confirmPassword ? "input-error" : ""}`}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
            />
            {fieldErr("confirmPassword")}
          </div>
          <button type="submit" className="btn-primary" disabled={changing}>
            <UserRound className="h-4 w-4" />
            {changing ? "Changing…" : "Change password"}
          </button>
        </form>
      </div>

      {/* Sessions */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="h-10 w-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
            <MonitorSmartphone className="h-5 w-5 text-amber-300" />
          </div>
          <div>
            <h2 className="font-semibold text-zinc-100">Sessions</h2>
            <p className="text-xs text-zinc-500">Revoke access on every device at once.</p>
          </div>
        </div>
        <button onClick={() => setLogoutAllConfirm(true)} className="btn-ghost !border-rose-900/60 !text-rose-300 hover:!bg-rose-500/10">
          <LogOut className="h-4 w-4" /> Log out all devices
        </button>
      </div>

      <ConfirmDialog
        open={logoutAllConfirm}
        onClose={() => setLogoutAllConfirm(false)}
        onConfirm={handleLogoutAll}
        title="Log out all devices?"
        message="This signs you out everywhere, including this device. You'll need to sign in again."
        confirmLabel="Log out everywhere"
        busy={loggingOutAll}
      />
    </div>
  );
}
