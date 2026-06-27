import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  BookOpen, ChevronRight, ExternalLink,
  LayoutDashboard, CreditCard, LogOut, Moon,
  Percent, Shield, ShoppingBag, Star, Sun, Tag, Users,
  Menu, X,
} from "lucide-react";
import { getTheme, toggleTheme } from "../../utils/theme.js";

import { logout as logoutRequest } from "../../api/authApi.js";
import LanguageSwitcher from "../../components/LanguageSwitcher.jsx";
import { clearAuth, getCurrentUser, getRefreshToken } from "../../utils/storage.js";

/* ── Nav items ──────────────────────────────── */
const NAV_ITEMS = [
  { to:"/admin/dashboard",   label:"admin.dashboard",   icon:LayoutDashboard },
  { to:"/admin/products",    label:"admin.products",    icon:BookOpen },
  { to:"/admin/categories",  label:"admin.categories",  icon:Tag },
  { to:"/admin/orders",      label:"admin.orders",      icon:ShoppingBag },
  { to:"/admin/discounts",   label:"admin.discounts",   icon:Percent },
  { to:"/admin/payments",    label:"admin.payments",    icon:CreditCard },
  { to:"/admin/reviews",     label:"admin.reviews",     icon:Star },
  { to:"/admin/users",       label:"admin.users",       icon:Users },
  { to:"/admin/permissions", label:"admin.permissions", icon:Shield },
];

/* ── Tokens ─────────────────────────────────── */
const tk = {
  sidebar:  "var(--admin-sidebar)",
  sidebarBorder: "var(--admin-border)",
  pageBg:   "var(--admin-page)",
  topBg:    "var(--admin-header)",
  text1:    "var(--admin-text)",
  text2:    "var(--admin-text-muted)",
  text3:    "var(--admin-text-faint)",
  accent:   "#4f6ef7",
  heroLine: "linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
  border:   "var(--admin-border)",
};

export default function AdminLayout() {
  const { t } = useTranslation();
  const [user, setUser] = useState(getCurrentUser());
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isDark, setIsDark] = useState(() => getTheme() === "dark");
  const navigate = useNavigate();

  useEffect(() => {
    const sync = () => setIsDark(getTheme() === "dark");
    window.addEventListener("aivira-theme", sync);
    return () => window.removeEventListener("aivira-theme", sync);
  }, []);

  function handleToggleTheme() {
    toggleTheme();
  }

  useEffect(() => {
    const sync = () => setUser(getCurrentUser());
    window.addEventListener("aivira-auth", sync);
    return () => window.removeEventListener("aivira-auth", sync);
  }, []);

  async function logout() {
    const rt = getRefreshToken();
    try { if (rt) await logoutRequest(rt); } catch { /* Local auth still has to be cleared. */ }
    finally { clearAuth(); navigate("/?auth=login&next=/admin/dashboard",{replace:true}); }
  }

  return (
    <div className="admin-shell flex min-h-screen" style={{ background: tk.pageBg }}>

      {/* ── Mobile overlay ── */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}
            transition={{duration:0.2}}
            className="fixed inset-0 z-40 lg:hidden"
            style={{ background:"rgba(0,0,0,0.6)", backdropFilter:"blur(4px)" }}
            onClick={() => setMobileOpen(false)}/>
        )}
      </AnimatePresence>

      {/* ── Sidebar ── */}
      <motion.aside
        initial={false}
        className={`fixed inset-y-0 left-0 z-50 flex w-[260px] flex-col transform transition-transform duration-300 lg:static lg:translate-x-0 ${mobileOpen ? "translate-x-0" : "-translate-x-full"}`}
        style={{
          background: tk.sidebar,
          borderRight:`1px solid ${tk.sidebarBorder}`,
        }}>
        {/* Top accent */}
        <div className="absolute left-0 right-0 top-0 h-[1.5px]" style={{ background:tk.heroLine }}/>

        {/* Brand */}
        <div className="flex items-center gap-3 px-6 py-6">
          <motion.div whileHover={{scale:1.08}} whileTap={{scale:0.94}}
            className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl"
            style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 6px 20px rgba(79,110,247,0.45)" }}>
            <BookOpen size={18} color="#fff"/>
          </motion.div>
          <Link to="/admin/dashboard" className="min-w-0">
            <p className="text-[0.55rem] font-black uppercase tracking-[0.2em]" style={{ color:tk.text3 }}>Admin Panel</p>
            <p className="truncate text-base font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
              {t("admin.brand","Aivira")}
            </p>
          </Link>
          <button type="button" className="ml-auto lg:hidden" onClick={() => setMobileOpen(false)}
            style={{ color:tk.text2 }}>
            <X size={18}/>
          </button>
        </div>

        {/* Divider */}
        <div className="mx-4 h-px" style={{ background:tk.sidebarBorder }}/>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto px-3 py-4" style={{ scrollbarWidth:"none" }}>
          <p className="mb-2 ml-3 text-[0.55rem] font-black uppercase tracking-[0.2em]" style={{ color:tk.text3 }}>
            Navigation
          </p>
          <div className="grid gap-0.5">
            {NAV_ITEMS.map(({ to, label, icon:Icon }, i) => (
              <NavLink key={to} to={to} onClick={() => setMobileOpen(false)}
                className={({ isActive }) => isActive ? "admin-nav-active" : "admin-nav-item"}>
                {({ isActive }) => (
                  <motion.div
                    initial={{opacity:0,x:-12}} animate={{opacity:1,x:0}}
                    transition={{duration:0.3,delay:i*0.04}}
                    className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-bold transition-all"
                    style={{
                      background: isActive?"rgba(79,110,247,0.12)":"transparent",
                      color: isActive?tk.text1:tk.text2,
                      border: isActive?"1px solid rgba(79,110,247,0.18)":"1px solid transparent",
                    }}>
                    <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg transition-all"
                      style={{ background:isActive?"rgba(79,110,247,0.16)":"var(--admin-surface-soft)", color:isActive?tk.accent:tk.text3 }}>
                      <Icon size={14}/>
                    </div>
                    {t(label)}
                    {isActive && (
                      <motion.div layoutId="sidebar-indicator" className="ml-auto"
                        initial={false} transition={{type:"spring",stiffness:500,damping:35}}>
                        <ChevronRight size={13} style={{ color:tk.accent }}/>
                      </motion.div>
                    )}
                  </motion.div>
                )}
              </NavLink>
            ))}
          </div>
        </nav>

        {/* Bottom actions */}
        <div className="border-t px-3 pb-6 pt-4" style={{ borderColor:tk.sidebarBorder }}>
          <Link to="/" target="_blank"
            className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-bold transition-all hover:bg-white/5"
            style={{ color:tk.text3 }}>
            <ExternalLink size={14}/> {t("common.backToBookstore","Về Bookstore")}
          </Link>
          <button type="button" onClick={logout}
            className="mt-1 flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-bold transition-all hover:bg-red-500/10"
            style={{ color:"rgba(239,68,68,0.75)" }}>
            <LogOut size={14}/> {t("admin.logout","Đăng xuất")}
          </button>
        </div>
      </motion.aside>

      {/* ── Main area ── */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Top bar */}
        <header className="sticky top-0 z-30 flex items-center gap-4 border-b px-6 py-4"
          style={{ background:tk.topBg, borderColor:tk.border, backdropFilter:"blur(20px)" }}>
          <button type="button" className="flex-shrink-0 lg:hidden" onClick={() => setMobileOpen(true)}
            style={{ color:tk.text2 }}>
            <Menu size={20}/>
          </button>

          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-bold" style={{ color:tk.text3 }}>
              {t("admin.workspace","Admin Workspace")}
            </p>
          </div>

          <div className="flex flex-shrink-0 items-center gap-3">
            <LanguageSwitcher compact/>
            {/* Theme toggle */}
            <motion.button
              whileHover={{ scale:1.08 }} whileTap={{ scale:0.9 }}
              type="button" onClick={handleToggleTheme}
              title={isDark ? "Chuyển sang sáng" : "Chuyển sang tối"}
              className="flex h-9 w-9 items-center justify-center rounded-full transition-all"
              style={{
                background: isDark ? "rgba(240,165,0,0.12)" : "rgba(79,110,247,0.12)",
                border: `1px solid ${isDark ? "rgba(240,165,0,0.35)" : "rgba(79,110,247,0.3)"}`,
                color: isDark ? "#f0a500" : "#4f6ef7",
              }}>
              <AnimatePresence mode="wait" initial={false}>
                <motion.div key={isDark?"moon":"sun"}
                  initial={{scale:0,rotate:-90,opacity:0}}
                  animate={{scale:1,rotate:0,opacity:1}}
                  exit={{scale:0,rotate:90,opacity:0}}
                  transition={{duration:0.2,ease:[0.22,1,0.36,1]}}>
                  {isDark ? <Sun size={15}/> : <Moon size={15}/>}
                </motion.div>
              </AnimatePresence>
            </motion.button>
            {/* User chip */}
            <div className="flex items-center gap-2 overflow-hidden rounded-full px-4 py-2"
              style={{ background:"rgba(79,110,247,0.10)", border:"1px solid rgba(79,110,247,0.18)" }}>
              <div className="flex h-6 w-6 items-center justify-center rounded-full text-xs font-black text-white"
                style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)" }}>
                {(user?.username||user?.email||"A")[0].toUpperCase()}
              </div>
              <span className="hidden text-sm font-bold sm:block" style={{ color:tk.text1 }}>
                {user?.username||user?.email||t("admin.admin","Admin")}
              </span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto p-4 sm:p-6 lg:p-8">
          <Outlet/>
        </main>
      </div>
    </div>
  );
}
