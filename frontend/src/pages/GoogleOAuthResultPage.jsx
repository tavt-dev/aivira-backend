import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { exchangeGoogleTicket } from "../api/authApi.js";
import { getProfile } from "../api/userApi.js";
import { hasAdminAccess } from "../utils/authz.js";
import { saveAuth, saveCurrentUser } from "../utils/storage.js";

export default function GoogleOAuthResultPage({ failure = false }) {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const errorCode = searchParams.get("error");
  const ticket = searchParams.get("ticket");
  const nextPath = sanitizeNextPath(searchParams.get("next")) || "/";
  const [message, setMessage] = useState(failure ? t("auth.googleLoginFailed") : t("auth.googleSigningIn"));
  const [busy, setBusy] = useState(!failure);

  useEffect(() => {
    if (failure) {
      setBusy(false);
      setMessage(errorCode || t("auth.googleLoginFailed"));
      return undefined;
    }

    let active = true;

    async function exchangeTicket() {
      if (!ticket) {
        setBusy(false);
        setMessage(t("auth.googleTicketMissing"));
        return;
      }

      try {
        const auth = await exchangeGoogleTicket(ticket);
        const accessToken = auth?.accessToken || auth?.token || auth?.jwt || auth?.access_token;
        saveAuth(auth);

        let profile = null;
        try {
          profile = await getProfile();
          saveCurrentUser(profile);
        } catch {
          profile = null;
        }

        if (!active) return;
        if (nextPath.startsWith("/admin")) {
          navigate(hasAdminAccess(profile, accessToken) ? nextPath : "/admin/forbidden", { replace: true });
          return;
        }
        navigate(nextPath, { replace: true });
      } catch (error) {
        if (!active) return;
        setBusy(false);
        setMessage(error.message || t("auth.googleLoginFailed"));
      }
    }

    exchangeTicket();
    return () => {
      active = false;
    };
  }, [errorCode, failure, navigate, nextPath, ticket, t]);

  return (
    <div className="mx-auto flex min-h-[70vh] w-full max-w-xl items-center justify-center px-4 py-20 text-center">
      <section className="w-full rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-xs font-black uppercase tracking-[0.14em] text-blue-600">{t("common.bookstore")}</p>
        <h1 className="mt-3 font-serif text-3xl font-bold text-slate-950">
          {busy ? t("auth.googleSigningIn") : t("auth.googleLoginFailed")}
        </h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">{message}</p>
        {!busy && (
          <Link
            className="mt-6 inline-flex rounded-full bg-blue-700 px-5 py-3 text-sm font-bold text-white transition hover:bg-blue-600"
            to="/?auth=login"
            replace
          >
            {t("auth.backToLogin")}
          </Link>
        )}
      </section>
    </div>
  );
}

function sanitizeNextPath(value) {
  if (!value || typeof value !== "string") return "";
  if (!value.startsWith("/") || value.startsWith("//")) return "";
  return value;
}
