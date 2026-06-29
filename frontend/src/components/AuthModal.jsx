import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Eye, EyeOff, Lock, Mail, User, X, CheckCircle2, XCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { forgotPassword, login, register, resendVerification, resetPassword, verifyUser } from "../api/authApi.js";
import { getProfile } from "../api/userApi.js";
import { hasAdminAccess } from "../utils/authz.js";
import {
  clearPendingVerify,
  getPendingVerify,
  saveAuth,
  saveCurrentUser,
  savePendingVerify
} from "../utils/storage.js";

const GOOGLE_AUTHORIZE_URL = import.meta.env.VITE_GOOGLE_OAUTH_AUTHORIZE_URL || "/api/v1/auth/google/authorize";

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
    title: "auth.loginTitle",
    kicker: "common.bookstore",
    copy: "auth.loginCopy",
    action: "auth.loginAction"
  },
  register: {
    title: "auth.registerTitle",
    kicker: "common.bookstore",
    copy: "auth.registerCopy",
    action: "auth.registerAction"
  },
  verify: {
    title: "auth.verifyTitle",
    kicker: "auth.verifyKicker",
    copy: "auth.verifyCopy",
    action: "auth.verifyAction"
  },
  forgot: {
    title: "auth.forgotTitle",
    kicker: "auth.forgotKicker",
    copy: "auth.forgotCopy",
    action: "auth.forgotAction"
  },
  reset: {
    title: "auth.resetTitle",
    kicker: "auth.resetKicker",
    copy: "auth.resetCopy",
    action: "auth.resetAction"
  }
};

export default function AuthModal({ open, onClose, initialMode = "login", nextPath = "" }) {
  const { t } = useTranslation();
  const [mode, setMode] = useState(initialMode);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
  const [visiblePasswords, setVisiblePasswords] = useState({});
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

  function togglePassword(field) {
    setVisiblePasswords((current) => ({ ...current, [field]: !current[field] }));
  }

  function startGoogleLogin() {
    window.location.assign(buildGoogleAuthorizeUrl(nextPath || "/"));
  }

  async function handleLoginRedirect(accessToken) {
    let profile = null;
    try {
      profile = await getProfile();
      saveCurrentUser(profile);
    } catch {
      profile = null;
    }

    const canAccessAdmin = hasAdminAccess(profile, accessToken);

    if (nextPath.startsWith("/admin")) {
      onClose();
      navigate(canAccessAdmin ? nextPath : "/admin/forbidden", { replace: true });
      return;
    }

    if (canAccessAdmin) {
      onClose();
      navigate("/admin/dashboard", { replace: true });
      return;
    }

    onClose();
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);

    try {
      validateForm(mode, form, t);

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
        if (!accessToken) throw new Error(t("auth.accessTokenMissing"));
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
        setMessage({ type: "success", text: t("auth.accountCreatedLogin") });
        setMode("login");
      }

      if (mode === "forgot") {
        await forgotPassword({ email: form.email.trim() });
        setMessage({ type: "success", text: t("auth.otpSent") });
        setMode("reset");
      }

      if (mode === "reset") {
        await resetPassword({
          email: form.email.trim(),
          otpCode: form.otpCode.trim(),
          newPassword: form.newPassword
        });
        setMessage({ type: "success", text: t("auth.resetSuccess") });
        setMode("login");
        setForm((current) => ({ ...current, password: "", newPassword: "", otpCode: "" }));
      }

      if (mode === "verify") {
        const pending = getPendingVerify();
        if (!pending) {
          setMode("login");
          throw new Error(t("auth.verifyExpired"));
        }
        const email = pending.email || form.email.trim();
        if (!email) throw new Error(t("auth.emailRequiredPending"));
        await verifyUser({ email, otpCode: form.otpCode.trim() });
        clearPendingVerify();
        setMessage({ type: "success", text: t("auth.emailVerified") });
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
      setMessage({ type: "error", text: error.message || t("auth.actionFailed") });
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
      text: context?.source === "register" ? t("auth.registerOtp") : t("auth.loginOtp")
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
        throw new Error(t("auth.verifyExpired"));
      }
      const email = pending.email || form.email.trim();
      if (!email) throw new Error(t("auth.emailRequiredPending"));
      await resendVerification({ email });
      setMessage({ type: "success", text: t("auth.otpResent") });
    } catch (error) {
      setMessage({ type: "error", text: error.message || t("auth.resendFailed") });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[5000] flex items-center justify-center px-3 py-4 sm:px-4 sm:py-8"
      style={{ background: "rgba(2,6,23,0.82)", backdropFilter: "blur(18px)" }}
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      {/* Ambient glow behind modal */}
      <div
        aria-hidden="true"
        style={{
          position: "fixed",
          top: "50%",
          left: "50%",
          width: 700,
          height: 700,
          transform: "translate(-50%,-50%)",
          background: "radial-gradient(circle, rgba(37,99,235,0.13) 0%, transparent 70%)",
          pointerEvents: "none",
          zIndex: 0
        }}
      />

      <div
        className="relative z-10 grid max-h-[min(92vh,100dvh)] w-[min(920px,96vw)] sm:w-[min(920px,94vw)] overflow-hidden md:grid-cols-[1fr_1.1fr]"
        style={{
          borderRadius: 20,
          border: "1px solid rgba(255,255,255,0.09)",
          background: "radial-gradient(circle at 80% 10%, rgba(56,120,255,0.14) 0%, transparent 50%), #030d1e",
          boxShadow: "0 40px 120px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04) inset"
        }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        {/* ── Close button ── */}
        <button
          style={{
            position: "absolute",
            right: 14,
            top: 12,
            zIndex: 30,
            width: 34,
            height: 34,
            borderRadius: "50%",
            border: "1px solid rgba(255,255,255,0.12)",
            background: "rgba(255,255,255,0.06)",
            color: "rgba(255,255,255,0.65)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            cursor: "pointer",
            transition: "all 0.2s ease"
          }}
          type="button"
          onClick={onClose}
          aria-label={t("auth.close")}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = "rgba(255,255,255,0.13)";
            e.currentTarget.style.color = "#fff";
            e.currentTarget.style.transform = "rotate(90deg)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = "rgba(255,255,255,0.06)";
            e.currentTarget.style.color = "rgba(255,255,255,0.65)";
            e.currentTarget.style.transform = "rotate(0deg)";
          }}
        >
          <X size={16} strokeWidth={2.5} />
        </button>

        {/* ── Aside panel (desktop only) ── */}
        <aside
          className="hidden md:flex"
          style={{
            position: "relative",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            minHeight: 620,
            padding: "48px 36px",
            textAlign: "center",
            overflow: "hidden",
            background:
              "radial-gradient(ellipse at 50% 40%, rgba(37,99,235,0.38) 0%, rgba(17,45,120,0.22) 45%, transparent 72%), linear-gradient(155deg, rgba(10,28,70,0.95) 0%, rgba(2,10,28,1) 100%)"
          }}
        >
          {/* Floating orb 1 */}
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              top: "18%",
              left: "50%",
              transform: "translateX(-50%)",
              width: 200,
              height: 200,
              borderRadius: "50%",
              background: "radial-gradient(circle, rgba(56,132,255,0.22) 0%, transparent 65%)",
              animation: "auth-float 6s ease-in-out infinite",
              filter: "blur(1px)"
            }}
          />
          {/* Floating orb 2 */}
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              bottom: "14%",
              right: "10%",
              width: 140,
              height: 140,
              borderRadius: "50%",
              background: "radial-gradient(circle, rgba(99,179,255,0.15) 0%, transparent 65%)",
              animation: "auth-float 8s ease-in-out infinite 1.5s",
              filter: "blur(2px)"
            }}
          />
          {/* Decorative ring */}
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%,-50%)",
              width: 380,
              height: 380,
              borderRadius: "50%",
              border: "1px solid rgba(255,255,255,0.06)",
              pointerEvents: "none"
            }}
          />
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%,-50%)",
              width: 280,
              height: 280,
              borderRadius: "50%",
              border: "1px solid rgba(56,132,255,0.12)",
              pointerEvents: "none"
            }}
          />

          {/* Brand */}
          <div style={{ position: "relative", zIndex: 1 }}>
            {/* Glowing dot */}
            <div
              aria-hidden="true"
              style={{
                width: 10,
                height: 10,
                borderRadius: "50%",
                background: "#93c5fd",
                boxShadow: "0 0 22px 6px rgba(111,191,255,0.7)",
                margin: "0 auto 22px"
              }}
            />
            <div
              style={{
                fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
                fontSize: "clamp(3rem, 5vw, 4.2rem)",
                letterSpacing: "0.22em",
                color: "#fff",
                textShadow: "0 0 40px rgba(56,132,255,0.55), 0 24px 60px rgba(0,0,0,0.4)",
                lineHeight: 1
              }}
            >
              AIVIRA
            </div>
            <p
              style={{
                marginTop: 10,
                fontFamily: "var(--f-serif, 'Cormorant Garamond', serif)",
                fontSize: "1.12rem",
                fontStyle: "italic",
                color: "rgba(255,255,255,0.38)",
                letterSpacing: "0.04em"
              }}
            >
              {t("auth.sideSubtitle")}
            </p>

            {/* Tags */}
            <div style={{ marginTop: 28, display: "flex", flexWrap: "wrap", justifyContent: "center", gap: 8 }}>
              {t("auth.tags", { returnObjects: true }).map((item) => (
                <span
                  key={item}
                  style={{
                    padding: "7px 14px",
                    borderRadius: 999,
                    border: "1px solid rgba(255,255,255,0.12)",
                    background: "rgba(255,255,255,0.07)",
                    fontFamily: "var(--f-body, 'Outfit', sans-serif)",
                    fontSize: "0.68rem",
                    fontWeight: 800,
                    letterSpacing: "0.1em",
                    textTransform: "uppercase",
                    color: "rgba(255,255,255,0.6)"
                  }}
                >
                  {item}
                </span>
              ))}
            </div>
          </div>

          {/* Bottom decorative line */}
          <div
            aria-hidden="true"
            style={{
              position: "absolute",
              bottom: 0,
              left: "10%",
              right: "10%",
              height: 1,
              background: "linear-gradient(90deg, transparent, rgba(56,132,255,0.4), transparent)"
            }}
          />
        </aside>

        {/* ── Form panel ── */}
        <form
          style={{
            display: "grid",
            alignContent: "center",
            gap: 18,
            overflowY: "auto",
            padding: "36px 32px",
            scrollBehavior: "smooth"
          }}
          onSubmit={submit}
        >
          {/* Tab switcher */}
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 6,
              padding: 5,
              borderRadius: 999,
              border: "1px solid rgba(255,255,255,0.09)",
              background: "rgba(255,255,255,0.04)"
            }}
            aria-label={t("auth.modes")}
          >
            {["login", "register"].map((m) => (
              <button
                key={m}
                type="button"
                style={{
                  borderRadius: 999,
                  padding: "9px 12px",
                  fontSize: "0.82rem",
                  fontWeight: 800,
                  fontFamily: "var(--f-body, 'Outfit', sans-serif)",
                  letterSpacing: "0.04em",
                  transition: "all 0.22s ease",
                  border: "none",
                  cursor: "pointer",
                  ...(mode === m
                    ? {
                        background: "linear-gradient(135deg, #2563eb 0%, #3b82f6 100%)",
                        color: "#fff",
                        boxShadow: "0 8px 24px rgba(37,99,235,0.38)"
                      }
                    : {
                        background: "transparent",
                        color: "rgba(255,255,255,0.5)"
                      })
                }}
                onClick={() => switchMode(m)}
              >
                {t(`common.${m}`)}
              </button>
            ))}
          </div>

          {/* Heading block */}
          <div style={{ textAlign: "center" }}>
            <div
              style={{
                display: "inline-block",
                padding: "5px 14px",
                borderRadius: 999,
                background: "rgba(37,99,235,0.14)",
                border: "1px solid rgba(37,99,235,0.28)",
                fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
                fontSize: "0.78rem",
                letterSpacing: "0.14em",
                color: "#93c5fd",
                marginBottom: 10
              }}
            >
              {t(meta.kicker)}
            </div>
            <h2
              id="auth-title"
              style={{
                margin: "0 0 8px",
                fontFamily: "var(--f-serif, 'Cormorant Garamond', serif)",
                fontSize: "clamp(1.6rem, 3.5vw, 2.6rem)",
                fontStyle: "italic",
                fontWeight: 700,
                color: "#fff",
                lineHeight: 1.15
              }}
            >
              {t(meta.title)}
            </h2>
            <p
              style={{
                margin: "0 auto",
                maxWidth: 380,
                fontSize: "0.83rem",
                lineHeight: 1.65,
                color: "rgba(255,255,255,0.52)"
              }}
            >
              {t(meta.copy)}
            </p>
          </div>

          {/* Progress steps */}
          {(mode === "verify" || mode === "forgot" || mode === "reset") && (
            <div
              style={{ display: "grid", gridTemplateColumns: "34px 1fr 34px", alignItems: "center", gap: 10 }}
              aria-label="Auth progress"
            >
              {/* Step 1 */}
              <span
                style={{
                  width: 34, height: 34, borderRadius: "50%", display: "grid", placeItems: "center",
                  fontSize: "0.85rem", fontWeight: 800, transition: "all 0.25s ease",
                  background: "linear-gradient(135deg, #2563eb, #60a5fa)",
                  border: "none", color: "#fff", boxShadow: "0 0 22px rgba(37,99,235,0.5)"
                }}
              >1</span>
              {/* Connecting line */}
              <i style={{ height: 1, background: "linear-gradient(90deg, #3b82f6, rgba(255,255,255,0.12))", display: "block" }} />
              {/* Step 2 */}
              <span
                style={{
                  width: 34, height: 34, borderRadius: "50%", display: "grid", placeItems: "center",
                  fontSize: "0.85rem", fontWeight: 800, transition: "all 0.25s ease",
                  ...(step >= 2
                    ? { background: "linear-gradient(135deg, #2563eb, #60a5fa)", border: "none", color: "#fff", boxShadow: "0 0 22px rgba(37,99,235,0.5)" }
                    : { background: "transparent", border: "1px solid rgba(255,255,255,0.14)", color: "rgba(255,255,255,0.4)" })
                }}
              >2</span>
            </div>
          )}

          {/* Fields */}
          <div style={{ display: "grid", gap: 12 }} key={mode}>
            {(mode === "login" || mode === "register") && (
              <>
                <Field
                  label={mode === "login" ? t("auth.email") : t("auth.username")}
                  value={form.username}
                  onChange={(value) => update("username", value)}
                  autoComplete="username"
                  minLength={mode === "register" ? 4 : undefined}
                  icon={mode === "login" ? Mail : User}
                  placeholder={mode === "login" ? "you@example.com" : t("auth.username")}
                  variant="bright"
                />
                <Field
                  label={t("auth.password")}
                  type={visiblePasswords.password ? "text" : "password"}
                  value={form.password}
                  onChange={(value) => update("password", value)}
                  autoComplete={mode === "login" ? "current-password" : "new-password"}
                  minLength={mode === "register" ? 6 : undefined}
                  passwordToggle={{
                    visible: Boolean(visiblePasswords.password),
                    onToggle: () => togglePassword("password")
                  }}
                  icon={Lock}
                  placeholder={t("auth.passwordPlaceholder")}
                  variant="bright"
                />
              </>
            )}

            {mode === "register" && (
              <>
                <Field
                  label={t("auth.email")}
                  type="email"
                  value={form.email}
                  onChange={(value) => update("email", value)}
                  autoComplete="email"
                />
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                  <Field
                    label={t("auth.firstName")}
                    value={form.firstName}
                    onChange={(value) => update("firstName", value)}
                    autoComplete="given-name"
                    required={false}
                  />
                  <Field
                    label={t("auth.lastName")}
                    value={form.lastName}
                    onChange={(value) => update("lastName", value)}
                    autoComplete="family-name"
                    required={false}
                  />
                </div>
                <Field
                  label={t("auth.confirmPassword")}
                  type={visiblePasswords.confirmPassword ? "text" : "password"}
                  value={form.confirmPassword}
                  onChange={(value) => update("confirmPassword", value)}
                  autoComplete="new-password"
                  minLength={6}
                  passwordToggle={{
                    visible: Boolean(visiblePasswords.confirmPassword),
                    onToggle: () => togglePassword("confirmPassword")
                  }}
                />
              </>
            )}

            {(mode === "verify" || mode === "forgot" || mode === "reset") && (
              <Field
                label={t("auth.email")}
                type="email"
                value={mode === "verify" && pendingVerify?.email ? pendingVerify.email : form.email}
                onChange={(value) => update("email", value)}
                autoComplete="email"
                disabled={mode === "verify" && Boolean(pendingVerify?.email)}
              />
            )}

            {mode === "verify" && (
              <Field
                label={t("auth.otpCode")}
                value={form.otpCode}
                onChange={(value) => update("otpCode", value.replace(/\D/g, "").slice(0, 6))}
                inputMode="numeric"
                maxLength={6}
              />
            )}

            {mode === "reset" && (
              <>
                <Field
                  label={t("auth.otpCode")}
                  value={form.otpCode}
                  onChange={(value) => update("otpCode", value.replace(/\D/g, "").slice(0, 6))}
                  inputMode="numeric"
                  maxLength={6}
                />
                <Field
                  label={t("auth.newPassword")}
                  type={visiblePasswords.newPassword ? "text" : "password"}
                  value={form.newPassword}
                  onChange={(value) => update("newPassword", value)}
                  autoComplete="new-password"
                  minLength={6}
                  passwordToggle={{
                    visible: Boolean(visiblePasswords.newPassword),
                    onToggle: () => togglePassword("newPassword")
                  }}
                />
              </>
            )}
          </div>

          {/* Alert message */}
          {message && (
            <div
              style={{
                display: "flex",
                alignItems: "flex-start",
                gap: 10,
                padding: "12px 14px",
                borderRadius: 14,
                fontSize: "0.82rem",
                lineHeight: 1.6,
                ...(message.type === "success"
                  ? {
                      border: "1px solid rgba(52,211,153,0.28)",
                      background: "rgba(16,185,129,0.1)",
                      color: "#a7f3d0"
                    }
                  : {
                      border: "1px solid rgba(248,113,113,0.28)",
                      background: "rgba(239,68,68,0.1)",
                      color: "#fca5a5"
                    })
              }}
            >
              {message.type === "success" ? (
                <CheckCircle2 size={16} style={{ marginTop: 1, flexShrink: 0, color: "#34d399" }} />
              ) : (
                <XCircle size={16} style={{ marginTop: 1, flexShrink: 0, color: "#f87171" }} />
              )}
              <span>{message.text}</span>
            </div>
          )}

          {/* CTA button */}
          <button
            disabled={busy}
            type="submit"
            style={{
              position: "relative",
              overflow: "hidden",
              borderRadius: 12,
              border: "none",
              padding: "13px 20px",
              fontFamily: "var(--f-body, 'Outfit', sans-serif)",
              fontWeight: 800,
              fontSize: "0.9rem",
              letterSpacing: "0.04em",
              color: "#fff",
              cursor: busy ? "wait" : "pointer",
              opacity: busy ? 0.7 : 1,
              background: "linear-gradient(135deg, #1d4ed8 0%, #3b82f6 60%, #60a5fa 100%)",
              boxShadow: busy ? "none" : "0 10px 32px rgba(37,99,235,0.38)",
              transition: "all 0.22s ease"
            }}
            onMouseEnter={(e) => {
              if (!busy) {
                e.currentTarget.style.transform = "translateY(-2px)";
                e.currentTarget.style.boxShadow = "0 18px 44px rgba(37,99,235,0.48)";
              }
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = "translateY(0)";
              e.currentTarget.style.boxShadow = "0 10px 32px rgba(37,99,235,0.38)";
            }}
          >
            {/* Shimmer overlay */}
            <span
              aria-hidden="true"
              style={{
                position: "absolute",
                inset: 0,
                background: "linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.18) 50%, transparent 65%)",
                backgroundSize: "200% 100%",
                animation: busy ? "none" : "auth-shimmer 2.2s infinite"
              }}
            />
            <span style={{ position: "relative" }}>
              {busy ? t("common.working") : t(meta.action)}
            </span>
          </button>

          {/* Google login section */}
          {(mode === "login" || mode === "register") && (
            <>
              {/* OR divider */}
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr auto 1fr",
                  alignItems: "center",
                  gap: 12,
                  color: "rgba(255,255,255,0.38)",
                  fontSize: "0.7rem",
                  fontWeight: 800,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase"
                }}
              >
                <span style={{ height: 1, background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.14))" }} />
                {t("auth.or")}
                <span style={{ height: 1, background: "linear-gradient(90deg, rgba(255,255,255,0.14), transparent)" }} />
              </div>

              {/* Google button */}
              <button
                type="button"
                onClick={startGoogleLogin}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 12,
                  padding: "11px 18px",
                  borderRadius: 12,
                  border: "1px solid rgba(255,255,255,0.16)",
                  background: "rgba(255,255,255,0.96)",
                  color: "#374151",
                  fontFamily: "var(--f-body, 'Outfit', sans-serif)",
                  fontWeight: 700,
                  fontSize: "0.87rem",
                  letterSpacing: "0.01em",
                  cursor: "pointer",
                  boxShadow: "0 4px 18px rgba(0,0,0,0.18)",
                  transition: "all 0.2s ease"
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-2px)";
                  e.currentTarget.style.boxShadow = "0 10px 28px rgba(0,0,0,0.24)";
                  e.currentTarget.style.background = "#fff";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "translateY(0)";
                  e.currentTarget.style.boxShadow = "0 4px 18px rgba(0,0,0,0.18)";
                  e.currentTarget.style.background = "rgba(255,255,255,0.96)";
                }}
              >
                <GoogleMark />
                {t("auth.continueWithGoogle")}
              </button>
            </>
          )}

          {/* Secondary links */}
          <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "center", gap: 6 }}>
            {mode === "verify" && (
              <button
                style={{
                  padding: "6px 10px",
                  fontSize: "0.8rem",
                  fontWeight: 700,
                  color: "#93c5fd",
                  background: "none",
                  border: "none",
                  cursor: busy ? "wait" : "pointer",
                  opacity: busy ? 0.55 : 1,
                  transition: "color 0.15s"
                }}
                type="button"
                onClick={resendOtp}
                disabled={busy}
                onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
              >
                {t("auth.resendOtp")}
              </button>
            )}
            {(mode === "login" || mode === "register") && (
              <button
                style={{
                  padding: "6px 10px",
                  fontSize: "0.8rem",
                  fontWeight: 700,
                  color: "#93c5fd",
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  transition: "color 0.15s"
                }}
                type="button"
                onClick={() => switchMode("forgot")}
                onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
              >
                {t("auth.forgotPassword")}
              </button>
            )}
            {(mode === "verify" || mode === "forgot" || mode === "reset") && (
              <button
                style={{
                  padding: "6px 10px",
                  fontSize: "0.8rem",
                  fontWeight: 700,
                  color: "#93c5fd",
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  transition: "color 0.15s"
                }}
                type="button"
                onClick={() => switchMode("login")}
                onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
              >
                {t("auth.backToLogin")}
              </button>
            )}
          </div>

          {/* No account / Has account + Terms */}
          {(mode === "login" || mode === "register") && (
            <div style={{ textAlign: "center", fontSize: "0.75rem", color: "rgba(255,255,255,0.45)", display: "grid", gap: 8 }}>
              <p style={{ margin: 0 }}>
                {mode === "login" ? t("auth.noAccount") : t("auth.hasAccount")}{" "}
                <button
                  style={{ fontWeight: 800, color: "#93c5fd", background: "none", border: "none", cursor: "pointer" }}
                  type="button"
                  onClick={() => switchMode(mode === "login" ? "register" : "login")}
                  onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
                >
                  {mode === "login" ? t("auth.createNow") : t("common.login")}
                </button>
              </p>
              <p style={{ margin: 0, lineHeight: 1.7 }}>
                {t("auth.termsPrefix")}{" "}
                <button
                  style={{ fontWeight: 700, color: "#93c5fd", background: "none", border: "none", cursor: "pointer" }}
                  type="button"
                  onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
                >
                  {t("footer.privacy")}
                </button>{" "}
                {t("auth.and")}{" "}
                <button
                  style={{ fontWeight: 700, color: "#93c5fd", background: "none", border: "none", cursor: "pointer" }}
                  type="button"
                  onMouseEnter={(e) => { e.currentTarget.style.color = "#fff"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "#93c5fd"; }}
                >
                  {t("footer.terms")}
                </button>
                .
              </p>
            </div>
          )}
        </form>
      </div>

      {/* Keyframes injected via style tag */}
      <style>{`
        @keyframes auth-float {
          0%, 100% { transform: translateX(-50%) translateY(0px); }
          50% { transform: translateX(-50%) translateY(-18px); }
        }
        @keyframes auth-shimmer {
          0% { background-position: -200% 0; }
          100% { background-position: 200% 0; }
        }
      `}</style>
    </div>
  );
}

// ── Field component ──
function Field({
  label,
  type = "text",
  value,
  onChange,
  required = true,
  passwordToggle,
  icon: Icon,
  placeholder,
  variant = "default",
  ...props
}) {
  const { t } = useTranslation();
  const [focused, setFocused] = useState(false);
  const bright = variant === "bright";

  const inputBase = {
    width: "100%",
    borderRadius: 10,
    padding: Icon ? "11px 12px 11px 40px" : "11px 14px",
    paddingRight: passwordToggle ? 50 : undefined,
    fontSize: "0.88rem",
    fontFamily: "var(--f-body, 'Outfit', sans-serif)",
    outline: "none",
    transition: "all 0.18s ease",
    ...(bright
      ? {
          border: focused
            ? "1.5px solid rgba(96,165,250,0.8)"
            : "1.5px solid rgba(255,255,255,0.72)",
          background: "#fff",
          color: "#111827",
          boxShadow: focused
            ? "0 0 0 4px rgba(37,99,235,0.16), 0 6px 20px rgba(0,0,0,0.08)"
            : "0 4px 14px rgba(0,0,0,0.06)"
        }
      : {
          border: focused
            ? "1.5px solid rgba(96,165,250,0.7)"
            : "1.5px solid rgba(255,255,255,0.14)",
          background: "rgba(255,255,255,0.06)",
          color: "#f8fafc",
          boxShadow: focused ? "0 0 0 4px rgba(37,99,235,0.14)" : "none"
        })
  };

  return (
    <label style={{ display: "grid", gap: 7 }}>
      <span
        style={{
          fontSize: "0.7rem",
          fontWeight: 800,
          fontFamily: "var(--f-body, 'Outfit', sans-serif)",
          textTransform: "uppercase",
          letterSpacing: "0.1em",
          color: "rgba(255,255,255,0.65)"
        }}
      >
        {label}
      </span>
      <span style={{ position: "relative", display: "block" }}>
        {Icon && (
          <Icon
            style={{
              position: "absolute",
              left: 13,
              top: "50%",
              transform: "translateY(-50%)",
              width: 16,
              height: 16,
              pointerEvents: "none",
              color: bright ? "#2563eb" : "rgba(148,163,184,0.8)"
            }}
            strokeWidth={2.4}
            aria-hidden="true"
          />
        )}
        <input
          type={type}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder || label}
          required={required}
          style={inputBase}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          {...props}
        />
        {passwordToggle && (
          <button
            type="button"
            onClick={passwordToggle.onToggle}
            style={{
              position: "absolute",
              right: 8,
              top: "50%",
              transform: "translateY(-50%)",
              width: 34,
              height: 34,
              borderRadius: "50%",
              border: "none",
              background: "transparent",
              cursor: "pointer",
              display: "grid",
              placeItems: "center",
              color: bright ? "#6b7280" : "rgba(148,163,184,0.7)",
              transition: "all 0.15s"
            }}
            aria-label={passwordToggle.visible ? t("auth.hidePassword", { label }) : t("auth.showPassword", { label })}
            onMouseEnter={(e) => { e.currentTarget.style.background = bright ? "rgba(0,0,0,0.07)" : "rgba(255,255,255,0.08)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = "transparent"; }}
          >
            {passwordToggle.visible ? (
              <EyeOff style={{ width: 15, height: 15 }} strokeWidth={2.4} />
            ) : (
              <Eye style={{ width: 15, height: 15 }} strokeWidth={2.4} />
            )}
          </button>
        )}
      </span>
    </label>
  );
}

// ── Google SVG logo (official 4-color) ──
function GoogleMark() {
  return (
    <svg
      aria-hidden="true"
      style={{ width: 20, height: 20, flexShrink: 0 }}
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
        fill="#4285F4"
      />
      <path
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
        fill="#34A853"
      />
      <path
        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
        fill="#FBBC05"
      />
      <path
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84C6.71 7.31 9.14 5.38 12 5.38z"
        fill="#EA4335"
      />
    </svg>
  );
}

function validateForm(mode, form, t) {
  if (mode === "login" && !form.username.trim()) {
    throw new Error(t("auth.validation.loginUsername"));
  }
  if (mode === "login" && !form.password) {
    throw new Error(t("auth.validation.loginPassword"));
  }
  if (mode === "register" && form.username.trim().length < 4) {
    throw new Error(t("auth.validation.username"));
  }
  if (mode === "register" && form.password.length < 6) {
    throw new Error(t("auth.validation.password"));
  }
  if (mode === "register" && form.password !== form.confirmPassword) {
    throw new Error(t("auth.validation.confirm"));
  }
  if ((mode === "register" || mode === "verify" || mode === "forgot" || mode === "reset") && !form.email.trim()) {
    throw new Error(t("auth.validation.email"));
  }
  if ((mode === "verify" || mode === "reset") && form.otpCode.trim().length < 6) {
    throw new Error(t("auth.validation.otp"));
  }
  if (mode === "reset" && form.newPassword.length < 6) {
    throw new Error(t("auth.validation.newPassword"));
  }
}

export function buildGoogleAuthorizeUrl(nextPath = "/") {
  const url = new URL(GOOGLE_AUTHORIZE_URL, window.location.origin);
  const safeNext =
    typeof nextPath === "string" && nextPath.startsWith("/") && !nextPath.startsWith("//") ? nextPath : "/";
  url.searchParams.set("next", safeNext);
  return url.toString();
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
  return (
    error?.errorCode === "E2202" ||
    error?.errorCode === "E3106" ||
    text.includes("verify") ||
    text.includes("verified") ||
    text.includes("active") ||
    text.includes("otp") ||
    text.includes("xac minh") ||
    text.includes("kich hoat")
  );
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}
