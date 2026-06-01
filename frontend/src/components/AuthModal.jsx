import { useEffect, useMemo, useState } from "react";
import { forgotPassword, login, register, resendVerification, resetPassword, verifyUser } from "../api/authApi.js";
import { saveAuth } from "../utils/storage.js";

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
    copy: "Register first, then verify the OTP sent by the backend.",
    action: "Create account"
  },
  verify: {
    title: "Verify email",
    kicker: "OTP required",
    copy: "Enter the 6-digit registration OTP from your email.",
    action: "Verify account"
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

export default function AuthModal({ open, onClose }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  const meta = modeMeta[mode];
  const step = useMemo(() => (mode === "login" ? 1 : mode === "register" ? 1 : mode === "verify" ? 2 : mode === "forgot" ? 1 : 2), [mode]);

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function switchMode(nextMode) {
    setMode(nextMode);
    setMessage(null);
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);

    try {
      validateForm(mode, form);

      if (mode === "login") {
        const auth = await login({ username: form.username.trim(), password: form.password });
        const accessToken = auth?.accessToken || auth?.token || auth?.jwt || auth?.access_token;
        if (!accessToken) throw new Error("Backend did not return an access token.");
        saveAuth(auth, { username: form.username.trim() });
        onClose();
      }

      if (mode === "register") {
        await register({
          username: form.username.trim(),
          password: form.password,
          email: form.email.trim(),
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim()
        });
        setMessage({ type: "success", text: "Account created. Check your email and enter the OTP to activate it." });
        setMode("verify");
      }

      if (mode === "verify") {
        await verifyUser({ email: form.email.trim(), otpCode: form.otpCode.trim() });
        setMessage({ type: "success", text: "Email verified. You can login now." });
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
    } catch (error) {
      setMessage({ type: "error", text: error.message || "Action failed. Please check backend/API." });
    } finally {
      setBusy(false);
    }
  }

  async function resendOtp() {
    setMessage(null);
    try {
      if (!form.email.trim()) throw new Error("Enter your email before resending OTP.");
      await resendVerification({ email: form.email.trim() });
      setMessage({ type: "success", text: "Verification OTP resent." });
    } catch (error) {
      setMessage({ type: "error", text: error.message || "Could not resend OTP." });
    }
  }

  return (
    <div className="modal-bg on auth-overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal-box auth-box" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="modal-x auth-close" type="button" onClick={onClose} aria-label="Close auth modal">x</button>
        <aside className="auth-left">
          <div className="auth-orbit" />
          <div className="auth-brand">AIVIRA</div>
          <p>Unlock your new chapters</p>
          <div className="auth-proof">
            <span>Real backend only</span>
            <span>JWT session</span>
            <span>OTP verification</span>
          </div>
        </aside>

        <form className="auth-right" onSubmit={submit}>
          <div className="auth-tabs" aria-label="Authentication modes">
            <button type="button" className={mode === "login" ? "active" : ""} onClick={() => switchMode("login")}>Login</button>
            <button type="button" className={mode === "register" ? "active" : ""} onClick={() => switchMode("register")}>Register</button>
          </div>

          <div className="auth-head">
            <div className="sec-chip">{meta.kicker}</div>
            <h2 id="auth-title">{meta.title}</h2>
            <p>{meta.copy}</p>
          </div>

          {mode !== "login" && (
            <div className="auth-steps" aria-label="Auth progress">
              <span className={step >= 1 ? "active" : ""}>1</span>
              <i />
              <span className={step >= 2 ? "active" : ""}>2</span>
            </div>
          )}

          <div className="auth-fields" key={mode}>
            {(mode === "login" || mode === "register") && (
              <>
                <Field label="Username" value={form.username} onChange={(value) => update("username", value)} autoComplete="username" minLength={4} />
                <Field label="Password" type="password" value={form.password} onChange={(value) => update("password", value)} autoComplete={mode === "login" ? "current-password" : "new-password"} minLength={6} />
              </>
            )}

            {mode === "register" && (
              <>
                <Field label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} autoComplete="email" />
                <div className="auth-grid">
                  <Field label="First name" value={form.firstName} onChange={(value) => update("firstName", value)} autoComplete="given-name" required={false} />
                  <Field label="Last name" value={form.lastName} onChange={(value) => update("lastName", value)} autoComplete="family-name" required={false} />
                </div>
                <Field label="Confirm password" type="password" value={form.confirmPassword} onChange={(value) => update("confirmPassword", value)} autoComplete="new-password" minLength={6} />
              </>
            )}

            {(mode === "verify" || mode === "forgot" || mode === "reset") && (
              <Field label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} autoComplete="email" />
            )}

            {(mode === "verify" || mode === "reset") && (
              <Field label="OTP code" value={form.otpCode} onChange={(value) => update("otpCode", value.replace(/\D/g, "").slice(0, 6))} inputMode="numeric" maxLength={6} />
            )}

            {mode === "reset" && (
              <Field label="New password" type="password" value={form.newPassword} onChange={(value) => update("newPassword", value)} autoComplete="new-password" minLength={6} />
            )}
          </div>

          {message && <div className={`auth-message ${message.type}`}>{message.text}</div>}

          <button className="auth-submit" disabled={busy} type="submit">
            <span>{busy ? "Working..." : meta.action}</span>
          </button>

          <div className="auth-switch">
            {mode !== "verify" && <button type="button" onClick={() => switchMode("verify")}>Verify OTP</button>}
            {mode === "verify" && <button type="button" onClick={resendOtp}>Resend OTP</button>}
            {mode !== "forgot" && mode !== "reset" && <button type="button" onClick={() => switchMode("forgot")}>Forgot password</button>}
            {(mode === "forgot" || mode === "reset" || mode === "verify") && <button type="button" onClick={() => switchMode("login")}>Back to login</button>}
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, type = "text", value, onChange, required = true, ...props }) {
  return (
    <label className="auth-field">
      <span>{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={label}
        required={required}
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
