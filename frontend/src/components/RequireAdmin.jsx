import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation } from "react-router-dom";
import { getProfile } from "../api/userApi.js";
import { hasAdminAccess } from "../utils/authz.js";
import { getAccessToken, hasAccessToken, saveCurrentUser } from "../utils/storage.js";

export default function RequireAdmin({ children }) {
  const { t } = useTranslation();
  const [status, setStatus] = useState(hasAccessToken() ? "checking" : "guest");
  const location = useLocation();

  useEffect(() => {
    let alive = true;

    async function verifyAdmin() {
      if (!hasAccessToken()) {
        setStatus("guest");
        return;
      }

      setStatus("checking");
      try {
        const profile = await getProfile();
        if (!alive) return;
        saveCurrentUser(profile);
        const accessToken = getAccessToken();
        setStatus(hasAdminAccess(profile, accessToken) ? "allowed" : "forbidden");
      } catch {
        if (!alive) return;
        setStatus("guest");
      }
    }

    verifyAdmin();
    return () => {
      alive = false;
    };
  }, [location.pathname, location.search]);

  if (status === "guest") {
    const next = encodeURIComponent(`${location.pathname}${location.search}`);
    return <Navigate to={`/?auth=login&next=${next}`} replace />;
  }

  if (status === "forbidden") {
    return <Navigate to="/admin/forbidden" replace />;
  }

  if (status === "checking") {
    return (
      <div className="grid min-h-screen place-items-center bg-slate-950 px-4 py-12">
        <div className="w-full max-w-md rounded-3xl border border-white/10 bg-white p-8 text-center shadow-2xl">
          <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{t("admin.security")}</span>
          <h1 className="mt-4 font-serif text-3xl font-bold text-slate-950">{t("admin.checking")}</h1>
          <p className="mt-2 text-slate-500">{t("admin.verifying")}</p>
        </div>
      </div>
    );
  }

  return children;
}
