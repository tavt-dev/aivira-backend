import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { logout as logoutRequest } from "../../api/authApi.js";
import LanguageSwitcher from "../../components/LanguageSwitcher.jsx";
import { clearAuth, getCurrentUser, getRefreshToken } from "../../utils/storage.js";

const links = [
  ["/admin/dashboard", "admin.dashboard"],
  ["/admin/products", "admin.products"],
  ["/admin/categories", "admin.categories"],
  ["/admin/orders", "admin.orders"],
  ["/admin/discounts", "admin.discounts"],
  ["/admin/payments", "admin.payments"],
  ["/admin/users", "admin.users"],
  ["/admin/permissions", "admin.permissions"]
];

export default function AdminLayout() {
  const { t } = useTranslation();
  const [user, setUser] = useState(getCurrentUser());
  const navigate = useNavigate();

  useEffect(() => {
    const sync = () => setUser(getCurrentUser());
    window.addEventListener("aivira-auth", sync);
    return () => window.removeEventListener("aivira-auth", sync);
  }, []);

  async function logout() {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) await logoutRequest(refreshToken);
    } catch {
      // Local logout still succeeds when the backend is offline.
    } finally {
      clearAuth();
      navigate("/?auth=login&next=/admin/dashboard", { replace: true });
    }
  }

  return (
    <div className="min-h-screen bg-slate-100 lg:grid lg:grid-cols-[260px_1fr]">
      <aside className="sticky top-0 z-30 flex min-h-0 flex-col gap-2 border-b border-white/10 bg-slate-950 p-5 text-white lg:min-h-screen lg:border-b-0">
        <Link to="/admin/dashboard" className="mb-4 flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 font-serif text-xl font-bold">A</span>
          <span className="font-serif text-2xl font-bold tracking-wider">{t("admin.brand")}</span>
        </Link>

        <nav className="flex gap-2 overflow-x-auto lg:grid lg:overflow-visible">
          {links.map(([to, label]) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                [
                  "whitespace-nowrap rounded-xl px-4 py-3 text-sm font-bold text-white/65 transition-colors hover:bg-white/10 hover:text-white",
                  isActive ? "bg-blue-600 text-white shadow-lg shadow-blue-600/25" : ""
                ].join(" ")
              }
            >
              {t(label)}
            </NavLink>
          ))}
        </nav>

        <Link className="mt-auto hidden rounded-xl px-4 py-3 text-sm font-bold text-white/55 transition-colors hover:bg-white/10 hover:text-white lg:block" to="/">
          {t("common.backToBookstore")}
        </Link>
      </aside>

      <section className="min-w-0 p-5 md:p-8">
        <header className="mb-6 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <div>
            <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{t("admin.dashboard")}</span>
            <h1 className="mt-2 font-serif text-3xl font-bold text-slate-950">{t("admin.workspace")}</h1>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-600">
              {user?.username || user?.email || t("admin.admin")}
            </span>
            <LanguageSwitcher compact />
            <Link className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-50" to="/">
              {t("admin.bookstore")}
            </Link>
            <button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white transition-colors hover:bg-blue-600" type="button" onClick={logout}>
              {t("admin.logout")}
            </button>
          </div>
        </header>
        <Outlet />
      </section>
    </div>
  );
}
