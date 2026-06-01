import { useState } from "react";
import { forgotPassword, login, register, resendVerification, resetPassword, verifyUser } from "../api/authApi.js";
import { saveAuth } from "../utils/storage.js";

export default function AuthModal({ open, onClose }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username: "", password: "", email: "", firstName: "", lastName: "", otpCode: "", newPassword: "" });
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  if (!open) return null;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      if (mode === "login") {
        const auth = await login({ username: form.username, password: form.password });
        const accessToken = auth?.accessToken || auth?.token || auth?.jwt || auth?.access_token;
        if (!accessToken) throw new Error("Backend did not return an access token.");
        saveAuth(auth, { username: form.username });
        onClose();
      } else if (mode === "register") {
        await register({
          username: form.username,
          password: form.password,
          email: form.email,
          firstName: form.firstName,
          lastName: form.lastName
        });
        setMessage("Registered. Check email OTP, then verify account.");
        setMode("verify");
      } else if (mode === "verify") {
        await verifyUser({ email: form.email, otpCode: form.otpCode });
        setMessage("Verified. You can log in now.");
        setMode("login");
      } else if (mode === "forgot") {
        await forgotPassword({ email: form.email });
        setMessage("Password reset OTP sent. Enter OTP and new password.");
        setMode("reset");
      } else if (mode === "reset") {
        await resetPassword({ email: form.email, otpCode: form.otpCode, newPassword: form.newPassword });
        setMessage("Password reset successful. You can log in now.");
        setMode("login");
      }
    } catch (error) {
      setMessage(error.message || "Action failed. Please check backend/API.");
    } finally {
      setBusy(false);
    }
  }

  async function resendOtp() {
    setMessage("");
    try {
      await resendVerification({ email: form.email });
      setMessage("Verification OTP resent.");
    } catch (error) {
      setMessage(error.message || "Could not resend OTP.");
    }
  }

  return (
    <div className="modal-bg on">
      <div className="modal-box auth-box">
        <button className="modal-x" onClick={onClose}>x</button>
        <div className="auth-left">
          <div className="auth-brand">AIVIRA</div>
          <p>Unlock your new chapters</p>
        </div>
        <form className="auth-right" onSubmit={submit}>
          <h2>{mode === "login" ? "Welcome to Aivira" : mode === "register" ? "Create Aivira Account" : mode === "verify" ? "Verify Email" : mode === "forgot" ? "Forgot Password" : "Reset Password"}</h2>
          {(mode === "login" || mode === "register") && (
            <>
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="Username" required />
              <input value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Password" type="password" required />
            </>
          )}
          {mode !== "login" && (
            <>
              <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="Email" type="email" required />
              {mode === "register" && (
                <div className="auth-grid">
                  <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} placeholder="First name" />
                  <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} placeholder="Last name" />
                </div>
              )}
              {mode === "verify" && (
                <input value={form.otpCode} onChange={(e) => setForm({ ...form, otpCode: e.target.value })} placeholder="OTP code" required />
              )}
              {mode === "reset" && (
                <>
                  <input value={form.otpCode} onChange={(e) => setForm({ ...form, otpCode: e.target.value })} placeholder="Password OTP" required />
                  <input value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} placeholder="New password" type="password" required />
                </>
              )}
            </>
          )}
          {message && <div className="notice">{message}</div>}
          <button className="auth-submit" disabled={busy}>{busy ? "Working..." : mode === "login" ? "Login" : mode === "register" ? "Register" : mode === "verify" ? "Verify" : mode === "forgot" ? "Send OTP" : "Reset password"}</button>
          <div className="auth-switch">
            <button type="button" onClick={() => setMode(mode === "login" ? "register" : "login")}>
              {mode === "login" ? "Create account" : "Back to login"}
            </button>
            <button type="button" onClick={() => setMode("verify")}>Verify OTP</button>
            {mode === "verify" && <button type="button" onClick={resendOtp}>Resend OTP</button>}
            <button type="button" onClick={() => setMode("forgot")}>Forgot password</button>
          </div>
        </form>
      </div>
    </div>
  );
}
