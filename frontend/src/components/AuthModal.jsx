import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { forgotPassword, login, register, resendVerification, resetPassword, verifyUser } from "../api/authApi.js";
import { getProfile } from "../api/userApi.js";
import { hasAdminAccess } from "../utils/authz.js";
import { clearPendingVerify, getPendingVerify, saveAuth, saveCurrentUser, savePendingVerify } from "../utils/storage.js";

const initialForm = {
  username: "",
  password: "",
  confirmPassword: "",
  email: "",
  firstName: "",
  lastName: "",
  otpCode: "",
  newPassword: ""
};

const modeMeta = {
  login: {
    title: "Welcome back",
    kicker: "Backend login",
    copy: "Use your verified Aivira account. Demo login is disabled.",
    action: "Login"
  },
  register: {
    title: "Create account",
    kicker: "Email verification",
    copy: "Register first. If verification is required, Aivira will open the OTP form.",
    action: "Create account"
  },
  verify: {
    title: "Verify email",
    kicker: "Controlled OTP",
    copy: "Enter the OTP sent by Aivira. This form only opens after register or a backend verification response.",
    action: "Verify OTP"
  },
  forgot: {
    title: "Reset access",
    kicker: "Forgot password",
    copy: "Request a password reset OTP for a verified email.",
    action: "Send OTP"
  },
  reset: {
    title: "Set new password",
    kicker: "Password OTP",
    copy: "Use the reset OTP and choose a new password.",
    action: "Reset password"
  }
};

export default function AuthModal({ open, onClose, initialMode = "login", nextPath = "" }) {
  const [mode, setMode] = useState(initialMode);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const pendingVerify = mode === "verify" ? getPendingVerify() : null;

  const meta = modeMeta[mode];
  const step = mode === "verify" || mode === "reset" ? 2 : 1;

  useEffect(() => {
    if (!open) return undefined;
    setMode(initialMode);
    const onKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, initialMode, onClose]);

  if (!open) return null;

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function switchMode(nextMode) {
    setMode(nextMode);
    setMessage(null);
  }

  async function handleLoginRedirect(accessToken) {
    let profile = null;
    try {
      profile = await getProfile();
      saveCurrentUser(profile);
    } catch {
      profile = null;
    }

    if (nextPath.startsWith("/admin")) {
      onClose();
      navigate(hasAdminAccess(profile, accessToken) ? nextPath : "/admin/forbidden", { replace: true });
      return;
    }

    onClose();
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);

    try {
      validateForm(mode, form);

      if (mode === "login") {
        const auth = await login({ username: form.username.trim(), password: form.password });
        if (shouldVerifyOtp(auth)) {
          openVerifyFlow({
            email: auth?.email || (form.username.includes("@") ? form.username.trim() : ""),
            username: form.username.trim(),
            source: "login"
          });
          return;
        }

        const accessToken = auth?.accessToken || auth?.token || auth?.jwt || auth?.access_token;
        if (!accessToken) throw new Error("Backend did not return an access token.");
        saveAuth(auth, { username: form.username.trim() });
        await handleLoginRedirect(accessToken);
        return;
      }

      if (mode === "register") {
        const response = await register({
          username: form.username.trim(),
          password: form.password,
          email: form.email.trim(),
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim()
        });
        if (shouldVerifyOtp(response, true)) {
          openVerifyFlow({ email: form.email.trim(), username: form.username.trim(), source: "register" });
          return;
        }
        setMessage({ type: "success", text: "Account created. You can login now." });
        setMode("login");
      }

      if (mode === "forgot") {
        await forgotPassword({ email: form.email.trim() });
        setMessage({ type: "success", text: "Password reset OTP sent. Enter it with your new password." });
        setMode("reset");
      }

      if (mode === "reset") {
        await resetPassword({
          email: form.email.trim(),
          otpCode: form.otpCode.trim(),
          newPassword: form.newPassword
        });
        setMessage({ type: "success", text: "Password reset successful. Login with your new password." });
        setMode("login");
        setForm((current) => ({ ...current, password: "", newPassword: "", otpCode: "" }));
      }

      if (mode === "verify") {
        const pending = getPendingVerify();
        if (!pending) {
          setMode("login");
          throw new Error("Verification session expired. Please register or login again.");
        }
        const email = pending.email || form.email.trim();
        if (!email) throw new Error("Email is required for this pending verification.");
        await verifyUser({ email, otpCode: form.otpCode.trim() });
        clearPendingVerify();
        setMessage({ type: "success", text: "Email verified. You can login now." });
        setForm((current) => ({ ...current, email, password: "", otpCode: "" }));
        setMode("login");
      }
    } catch (error) {
      if (mode === "login" && isVerifyRequiredError(error)) {
        openVerifyFlow({
          email: form.username.includes("@") ? form.username.trim() : "",
          username: form.username.trim(),
          source: "login"
        });
        return;
      }
      setMessage({ type: "error", text: error.message || "Action failed. Please check backend/API." });
    } finally {
      setBusy(false);
    }
  }

  function openVerifyFlow(context) {
    savePendingVerify(context);
    setForm((current) => ({
      ...current,
      email: context?.email || current.email,
      username: context?.username || current.username,
      otpCode: ""
    }));
    setMessage({
      type: "success",
      text: context?.source === "register"
        ? "Account created. Enter the OTP sent to your email."
        : "This account needs email verification. Enter the OTP to continue."
    });
    setMode("verify");
  }

  async function resendOtp() {
    setBusy(true);
    setMessage(null);
    try {
      const pending = getPendingVerify();
      if (!pending) {
        setMode("login");
        throw new Error("Verification session expired. Please register or login again.");
      }
      const email = pending.email || form.email.trim();
      if (!email) throw new Error("Email is required for this pending verification.");
      await resendVerification({ email });
      setMessage({ type: "success", text: "Verification OTP resent. Check your email." });
    } catch (error) {
      setMessage({ type: "error", text: error.message || "Could not resend OTP." });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[5000] flex items-center justify-center bg-slate-950/80 px-4 py-8 backdrop-blur-md"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div
        className="relative grid max-h-[92vh] w-[min(900px,94vw)] overflow-hidden rounded-[20px] border border-white/10 bg-[radial-gradient(circle_at_85%_12%,rgba(72,139,255,0.18),transparent_26%),#020a16] shadow-[0_36px_110px_rgba(5,9,15,0.4)] md:grid-cols-[0.95fr_1.05fr]"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        <button
          className="absolute right-[18px] top-[15px] z-20 h-[38px] w-[38px] rounded-full border-0 bg-white/10 text-xl text-white/75 transition hover:rotate-90 hover:bg-white/15 hover:text-white"
          type="button"
          onClick={onClose}
          aria-label="Close auth modal"
        >
          x
        </button>

        <aside className="relative grid min-h-[190px] place-content-center overflow-hidden bg-[radial-gradient(ellipse_at_center,rgba(45,107,240,0.42),transparent_58%),linear-gradient(140deg,rgba(13,32,72,0.9),rgba(2,10,22,0.98))] p-10 text-center text-white before:absolute before:left-1/2 before:top-1/2 before:h-[360px] before:w-[360px] before:-translate-x-1/2 before:-translate-y-1/2 before:animate-spin before:rounded-full before:border before:border-white/10 before:content-[''] after:absolute after:-bottom-[70px] after:-right-20 after:h-[220px] after:w-[220px] after:rounded-full after:border after:border-white/10 after:bg-blue-500/15 after:blur-[2px] after:content-[''] md:min-h-[610px]">
          <div className="absolute left-[calc(50%_-_6px)] top-[calc(50%_-_186px)] h-3 w-3 origin-[6px_186px] animate-spin rounded-full bg-[#c8d9ff] shadow-[0_0_24px_rgba(111,191,255,0.9)]" />
          <div className="relative font-display text-6xl tracking-[0.18em] text-white [text-shadow:0_20px_50px_rgba(0,0,0,0.35)]">
            AIVIRA
          </div>
          <p className="relative mt-2 font-serif text-lg italic text-white/45">
            Unlock your new chapters
          </p>
          <div className="relative mt-7 flex flex-wrap justify-center gap-2">
            {["Real backend only", "JWT session", "OTP verification"].map((item) => (
              <span
                key={item}
                className="rounded-full border border-white/15 bg-white/10 px-3 py-2 text-[0.74rem] font-extrabold uppercase tracking-[0.08em] text-white/70"
              >
                {item}
              </span>
            ))}
          </div>
        </aside>

        <form className="grid max-h-[92vh] content-center gap-4 overflow-y-auto p-7 md:p-[42px]" onSubmit={submit}>
          <div className="grid grid-cols-2 gap-1.5 rounded-full border border-white/10 bg-white/5 p-1.5" aria-label="Authentication modes">
            <button
              type="button"
              className={[
                "rounded-full px-3 py-2.5 font-black transition",
                mode === "login"
                  ? "bg-white text-slate-950 shadow-[0_12px_30px_rgba(0,0,0,0.22)]"
                  : "text-white/60 hover:text-white",
              ].join(" ")}
              onClick={() => switchMode("login")}
            >
              Login
            </button>
            <button
              type="button"
              className={[
                "rounded-full px-3 py-2.5 font-black transition",
                mode === "register"
                  ? "bg-white text-slate-950 shadow-[0_12px_30px_rgba(0,0,0,0.22)]"
                  : "text-white/60 hover:text-white",
              ].join(" ")}
              onClick={() => switchMode("register")}
            >
              Register
            </button>
          </div>

          <div className="text-center">
            <div className="mx-auto inline-flex w-fit rounded-full bg-blue-500/10 px-2.5 py-1.5 text-[0.72rem] font-black uppercase tracking-[0.12em] text-blue-300">
              {meta.kicker}
            </div>
            <h2
              id="auth-title"
              className="my-2 font-serif text-[clamp(2rem,4vw,2.8rem)] italic text-white"
            >
              {meta.title}
            </h2>
            <p className="mx-auto max-w-[390px] text-sm leading-6 text-white/50">
              {meta.copy}
            </p>
          </div>

          {(mode === "verify" || mode === "forgot" || mode === "reset") && (
            <div className="grid grid-cols-[34px_1fr_34px] items-center gap-2.5" aria-label="Auth progress">
              <span
                className={[
                  "grid h-[34px] w-[34px] place-items-center rounded-full border font-black",
                  step >= 1
                    ? "border-blue-600 bg-blue-600 text-white shadow-[0_0_28px_rgba(45,107,240,0.45)]"
                    : "border-white/15 text-white/50",
                ].join(" ")}
              >
                1
              </span>
              <i className="h-px bg-gradient-to-r from-blue-500 to-white/15" />
              <span
                className={[
                  "grid h-[34px] w-[34px] place-items-center rounded-full border font-black",
                  step >= 2
                    ? "border-blue-600 bg-blue-600 text-white shadow-[0_0_28px_rgba(45,107,240,0.45)]"
                    : "border-white/15 text-white/50",
                ].join(" ")}
              >
                2
              </span>
            </div>
          )}

          <div className="grid gap-3" key={mode}>
            {(mode === "login" || mode === "register") && (
              <>
                <Field label="Username" value={form.username} onChange={(value) => update("username", value)} autoComplete="username" minLength={4} />
                <Field label="Password" type="password" value={form.password} onChange={(value) => update("password", value)} autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={6} />
              </>
            )}

            {mode === "register" && (
              <>
                <Field label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} autoComplete="email" />
                <div className="grid gap-2.5 md:grid-cols-2">
                  <Field label="First name" value={form.firstName} onChange={(value) => update("firstName", value)} autoComplete="given-name" required={false} />
                  <Field label="Last name" value={form.lastName} onChange={(value) => update("lastName", value)} autoComplete="family-name" required={false} />
                </div>
                <Field label="Confirm password" type="password" value={form.confirmPassword} onChange={(value) => update("confirmPassword", value)} autoComplete="new-password" minLength={6} />
              </>
            )}

            {(mode === "verify" || mode === "forgot" || mode === "reset") && (
              <Field
                label="Email"
                type="email"
                value={mode === "verify" && pendingVerify?.email ? pendingVerify.email : form.email}
                onChange={(value) => update("email", value)}
                autoComplete="email"
                disabled={mode === "verify" && Boolean(pendingVerify?.email)}
              />
            )}

            {mode === "verify" && (
              <Field
                label="OTP code"
                value={form.otpCode}
                onChange={(value) => update("otpCode", value.replace(/\D/g, "").slice(0, 6))}
                inputMode="numeric"
                maxLength={6}
              />
            )}

            {mode === "reset" && (
              <>
                <Field label="OTP code" value={form.otpCode} onChange={(value) => update("otpCode", value.replace(/\D/g, "").slice(0, 6))} inputMode="numeric" maxLength={6} />
                <Field label="New password" type="password" value={form.newPassword} onChange={(value) => update("newPassword", value)} autoComplete="new-password" minLength={6} />
              </>
            )}
          </div>

          {message && (
            <div
              className={[
                "rounded-xl border px-3.5 py-3 text-sm leading-6",
                message.type === "success"
                  ? "border-emerald-300/30 bg-emerald-500/15 text-emerald-100"
                  : "border-red-300/30 bg-red-500/15 text-red-100",
              ].join(" ")}
            >
              {message.text}
            </div>
          )}

          <button
            className="relative overflow-hidden rounded-[10px] border-0 bg-blue-700 px-5 py-3.5 font-extrabold text-white shadow-[0_10px_30px_rgba(24,83,227,0.28)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_38px_rgba(24,83,227,0.36)] disabled:cursor-wait disabled:opacity-70 disabled:hover:translate-y-0"
            disabled={busy}
            type="submit"
          >
            <span>{busy ? "Working..." : meta.action}</span>
          </button>

          <div className="flex flex-wrap justify-center gap-2.5">
            {mode === "verify" && <button className="px-1.5 py-1 font-extrabold text-[#c8d9ff] hover:text-white disabled:cursor-wait disabled:opacity-55" type="button" onClick={resendOtp} disabled={busy}>Resend OTP</button>}
            {(mode === "login" || mode === "register") && <button className="px-1.5 py-1 font-extrabold text-[#c8d9ff] hover:text-white" type="button" onClick={() => switchMode("forgot")}>Forgot password</button>}
            {(mode === "verify" || mode === "forgot" || mode === "reset") && <button className="px-1.5 py-1 font-extrabold text-[#c8d9ff] hover:text-white" type="button" onClick={() => switchMode("login")}>Back to login</button>}
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, type = "text", value, onChange, required = true, ...props }) {
  return (
    <label className="grid gap-2">
      <span className="text-[0.78rem] font-black uppercase tracking-[0.08em] text-white/65">
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={label}
        required={required}
        className="w-full rounded-[10px] border border-white/10 bg-white/95 px-3.5 py-3 text-slate-950 outline-none transition focus:-translate-y-px focus:border-sky-300 focus:shadow-[0_0_0_4px_rgba(45,107,240,0.16)] disabled:cursor-not-allowed disabled:bg-white/15 disabled:text-white/70"
        {...props}
      />
    </label>
  );
}

function validateForm(mode, form) {
  if ((mode === "login" || mode === "register") && form.username.trim().length < 4) {
    throw new Error("Username must be at least 4 characters.");
  }
  if ((mode === "login" || mode === "register") && form.password.length < 6) {
    throw new Error("Password must be at least 6 characters.");
  }
  if (mode === "register" && form.password !== form.confirmPassword) {
    throw new Error("Password confirmation does not match.");
  }
  if ((mode === "register" || mode === "verify" || mode === "forgot" || mode === "reset") && !form.email.trim()) {
    throw new Error("Email is required.");
  }
  if ((mode === "verify" || mode === "reset") && form.otpCode.trim().length < 6) {
    throw new Error("OTP code must be 6 digits.");
  }
  if (mode === "reset" && form.newPassword.length < 6) {
    throw new Error("New password must be at least 6 characters.");
  }
}

function shouldVerifyOtp(response, registerSuccess = false) {
  const nextStep = response?.nextStep || response?.status || response?.authStep;
  if (String(nextStep || "").toUpperCase() === "VERIFY_OTP") return true;
  const message = String(response?.message || "").toLowerCase();
  if (message.includes("verify") || message.includes("otp") || normalizeText(message).includes("xac minh")) return true;
  return registerSuccess && !(response?.accessToken || response?.token || response?.jwt || response?.access_token);
}

function isVerifyRequiredError(error) {
  const text = normalizeText(`${error?.message || ""} ${error?.errorCode || ""}`);
  return error?.errorCode === "E2202"
    || error?.errorCode === "E3106"
    || text.includes("verify")
    || text.includes("verified")
    || text.includes("active")
    || text.includes("otp")
    || text.includes("xac minh")
    || text.includes("kich hoat");
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}
