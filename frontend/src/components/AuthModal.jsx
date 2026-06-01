import { useEffect, useState } from "react";
import { forgotPassword, login, register, resendVerification, resetPassword, verifyUser } from "../api/authApi.js";
import { clearPendingVerify, getPendingVerify, saveAuth, savePendingVerify } from "../utils/storage.js";

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

export default function AuthModal({ open, onClose, initialMode = "login" }) {
  const [mode, setMode] = useState(initialMode);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
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
        onClose();
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

          {(mode === "verify" || mode === "forgot" || mode === "reset") && (
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

          {message && <div className={`auth-message ${message.type}`}>{message.text}</div>}

          <button className="auth-submit" disabled={busy} type="submit">
            <span>{busy ? "Working..." : meta.action}</span>
          </button>

          <div className="auth-switch">
            {mode === "verify" && <button type="button" onClick={resendOtp} disabled={busy}>Resend OTP</button>}
            {(mode === "login" || mode === "register") && <button type="button" onClick={() => switchMode("forgot")}>Forgot password</button>}
            {(mode === "verify" || mode === "forgot" || mode === "reset") && <button type="button" onClick={() => switchMode("login")}>Back to login</button>}
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
