import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { ChevronDown, Compass, KeyRound, LogOut, Menu, Moon, Search, Settings, ShoppingBag, Sun, User, UserCircle } from "lucide-react";
import { logout as logoutRequest } from "../api/authApi.js";
import { getCart } from "../api/cartApi.js";
import { getProducts } from "../api/catalogApi.js";
import LanguageSwitcher from "./LanguageSwitcher.jsx";
import { normalizeBook, pageRows } from "../utils/mappers.js";
import { clearAuth, getAccessToken, getRefreshToken } from "../utils/storage.js";
import { getTheme, toggleTheme } from "../utils/theme.js";

export default function Navbar({ solid, user, onAuth }) {
  const { t } = useTranslation();
  const [scrolled, setScrolled] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [theme, setThemeState] = useState(getTheme);
  const userMenuRef = useRef(null);
  const navigate = useNavigate();
  const isSolid = solid || scrolled;

  useEffect(() => {
    function handleScroll() {
      setScrolled(window.scrollY > 20);
    }

    handleScroll();
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    function closeOnOutside(event) {
      if (!userMenuRef.current?.contains(event.target)) setUserMenuOpen(false);
    }

    function closeOnEscape(event) {
      if (event.key === "Escape") setUserMenuOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutside);
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutside);
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, []);

  useEffect(() => {
    const syncTheme = (event) => setThemeState(event.detail?.theme || getTheme());
    window.addEventListener("aivira-theme", syncTheme);
    return () => window.removeEventListener("aivira-theme", syncTheme);
  }, []);

  useEffect(() => {
    const sync = async () => {
      if (!getAccessToken()) {
        setCartCount(0);
        return;
      }

      try {
        const cart = await getCart();
        setCartCount((cart?.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0));
      } catch {
        setCartCount(0);
      }
    };

    sync();
    window.addEventListener("aivira-cart", sync);
    window.addEventListener("aivira-auth", sync);
    return () => {
      window.removeEventListener("aivira-cart", sync);
      window.removeEventListener("aivira-auth", sync);
    };
  }, []);

  useEffect(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      setResults([]);
      return;
    }

    let alive = true;
    getProducts({ keyword: q, page: 1, size: 5 })
      .then((page) => {
        const rows = pageRows(page);
        if (alive) setResults(rows.map((row) => normalizeBook(row)));
      })
      .catch(() => {
        if (alive) setResults([]);
      });

    return () => {
      alive = false;
    };
  }, [query]);

  async function logout(event) {
    event.preventDefault();
    const refreshToken = getRefreshToken();

    try {
      if (refreshToken) await logoutRequest(refreshToken);
    } catch {
      // Local logout still succeeds when the backend is offline.
    } finally {
      clearAuth();
      navigate("/");
      setMobileOpen(false);
      setUserMenuOpen(false);
    }
  }

  function submitSearch(event) {
    event.preventDefault();
    const keyword = query.trim();
    if (!keyword) return;

    navigate(`/category/all?search=${encodeURIComponent(keyword)}`);
    setResults([]);
    setMobileOpen(false);
    setUserMenuOpen(false);
  }

  function closePanels() {
    setResults([]);
    setMobileOpen(false);
  }

  const navTextClass = isSolid ? "text-slate-600" : "text-white/85";
  const activeTextClass = isSolid ? "text-blue-600" : "text-white";
  const displayName = user?.username || user?.email || t("nav.reader");
  const isDark = theme === "dark";

  function switchTheme() {
    setThemeState(toggleTheme());
  }

  return (
    <nav
      className={[
        "fixed left-0 right-0 top-0 z-50 border-b transition-all duration-500 ease-out",
        isSolid
          ? "border-slate-200/70 bg-white/85 py-3 shadow-sm backdrop-blur-xl"
          : "border-transparent bg-transparent py-6"
      ].join(" ")}
    >
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-6 px-4 md:px-8">
        <Link to="/" onClick={closePanels} className="group flex flex-shrink-0 items-center gap-3">
          <img 
             src="/logo.png" 
             alt="Aivira Bookstore" 
             className={[
                "h-10 w-auto rounded-lg object-contain transition-transform duration-500 group-hover:scale-105",
                isSolid ? "shadow-[0_4px_14px_rgba(0,0,0,0.18)]" : "border border-white/10 shadow-[0_4px_14px_rgba(0,0,0,0.4)]"
             ].join(" ")}
          />
          <span className="flex flex-col leading-none">
            <span
              className={[
                "text-[1.35rem] font-black transition-colors duration-500",
                isSolid ? "text-slate-950" : "text-white"
              ].join(" ")}
              style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
            >
              Aivira
            </span>
            <span
              className={[
                "hidden text-[0.68rem] font-bold uppercase transition-colors duration-500 sm:block",
                isSolid ? "text-blue-600" : "text-blue-100"
              ].join(" ")}
            >
              Bookstore
            </span>
          </span>
        </Link>

        <div className="relative mx-auto hidden max-w-lg flex-1 lg:flex">
          <form onSubmit={submitSearch} className="group relative w-full">
            <Search
              className={[
                "absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 transition-colors duration-300",
                isSolid ? "text-slate-400 group-focus-within:text-blue-500" : "text-white/60 group-focus-within:text-white"
              ].join(" ")}
              strokeWidth={2.5}
            />
            <input
              type="text"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t("nav.searchPlaceholder")}
              className={[
                "w-full rounded-full border py-2.5 pl-11 pr-5 text-sm font-medium transition-all duration-300 placeholder:font-normal focus:outline-none focus:ring-2 focus:ring-blue-500/30",
                isSolid
                  ? "border-slate-200 bg-slate-100/50 text-slate-900 placeholder:text-slate-400 focus:bg-white"
                  : "border-white/20 bg-white/10 text-white placeholder:text-white/60 hover:bg-white/20 focus:bg-white/10"
              ].join(" ")}
            />
          </form>

          {results.length > 0 && (
            <div className="absolute top-[calc(100%+12px)] z-50 w-full origin-top overflow-hidden rounded-2xl border border-slate-100/80 bg-white/95 shadow-[0_10px_40px_-10px_rgba(0,0,0,0.18)] backdrop-blur-xl transition-all duration-300">
              <div className="border-b border-slate-100 bg-slate-50/70 px-4 py-3">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">{t("nav.topResults")}</span>
              </div>
              <div className="p-2">
                {results.map((book) => (
                  <Link
                    key={book.id}
                    to={`/product/${book.slug}`}
                    onClick={closePanels}
                    className="group/item flex items-center gap-4 rounded-xl p-3 transition-colors hover:bg-blue-50/70"
                  >
                    <div className="relative h-16 w-12 flex-shrink-0 overflow-hidden rounded shadow-sm">
                      <img src={book.image || book.cover} alt={book.title} className="h-full w-full object-cover" />
                      <div className="absolute inset-0 rounded border border-black/5" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="line-clamp-1 text-sm font-bold text-slate-900 transition-colors group-hover/item:text-blue-600">
                        {book.title}
                      </h4>
                      <p className="mt-0.5 line-clamp-1 text-xs text-slate-500">{book.author}</p>
                      {book.catLabel && (
                        <span className="mt-1.5 inline-block rounded-sm bg-slate-100 px-2 py-0.5 text-[10px] font-bold uppercase text-slate-600">
                          {book.catLabel}
                        </span>
                      )}
                    </div>
                  </Link>
                ))}
              </div>
              <div className="border-t border-slate-100 p-2">
                <button
                  type="button"
                  onClick={submitSearch}
                  className="w-full rounded-lg py-2.5 text-center text-sm font-semibold text-blue-600 transition-colors hover:bg-blue-50"
                >
                  {t("nav.viewAllResults", { query })}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className={["hidden items-center gap-8 transition-colors duration-500 md:flex", navTextClass].join(" ")}>
          <div className="flex items-center gap-6 text-sm font-bold tracking-wide">
            <NavLink to="/" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <span>{t("common.home")}</span>
              <span className="absolute -bottom-1.5 left-0 h-[2px] w-full origin-left scale-x-0 rounded-full bg-blue-500 transition-transform group-hover:scale-x-100" />
            </NavLink>
            <NavLink to="/category/all" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <Compass className="h-4 w-4 opacity-70 transition-opacity group-hover:opacity-100" />
              <span>{t("common.explore")}</span>
              <span className="absolute -bottom-1.5 left-0 h-[2px] w-full origin-left scale-x-0 rounded-full bg-blue-500 transition-transform group-hover:scale-x-100" />
            </NavLink>
            <NavLink to="/orders" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <span>{t("common.orders")}</span>
              <span className="absolute -bottom-1.5 left-0 h-[2px] w-full origin-left scale-x-0 rounded-full bg-blue-500 transition-transform group-hover:scale-x-100" />
            </NavLink>
          </div>

          <div className="flex items-center gap-4">
            <NavLink to="/cart" className={({ isActive }) => ["relative rounded-full p-2 transition-colors hover:bg-slate-500/10", isActive ? activeTextClass : ""].join(" ")}>
              <ShoppingBag className="h-5 w-5" strokeWidth={2} />
              {cartCount > 0 && (
                <span className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-blue-600 px-1 text-[9px] font-bold text-white ring-2 ring-white">
                  {cartCount > 99 ? "99+" : cartCount}
                </span>
              )}
            </NavLink>

            <div className={["h-6 w-px opacity-30", isSolid ? "bg-slate-300" : "bg-white"].join(" ")} />

            <LanguageSwitcher compact inverted={!isSolid} />

            {user ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  type="button"
                  onClick={() => setUserMenuOpen((value) => !value)}
                  className={[
                    "flex items-center gap-2 rounded-full border p-1.5 pl-2 pr-3 transition-all duration-300 hover:shadow-md",
                    isSolid ? "border-slate-200 bg-white text-slate-700 hover:border-slate-300" : "border-white/20 bg-white/10 text-white hover:bg-white/20"
                  ].join(" ")}
                  aria-expanded={userMenuOpen}
                >
                  <div className={["flex h-7 w-7 items-center justify-center rounded-full", isSolid ? "bg-slate-100" : "bg-white/20"].join(" ")}>
                    <User className="h-4 w-4" />
                  </div>
                  <span className="max-w-[140px] truncate text-sm font-bold">
                    {t("nav.greeting", { name: displayName })}
                  </span>
                  <ChevronDown className={["h-4 w-4 transition-transform", userMenuOpen ? "rotate-180" : ""].join(" ")} />
                </button>

                {userMenuOpen && (
                  <div className="absolute right-0 top-[calc(100%+12px)] z-50 w-64 overflow-hidden rounded-2xl border border-slate-200 bg-white p-2 text-slate-700 shadow-2xl shadow-slate-950/15">
                    <div className="mb-1 rounded-xl bg-slate-50 px-3 py-3">
                      <p className="truncate text-sm font-black text-slate-950">{displayName}</p>
                      <p className="mt-0.5 truncate text-xs font-semibold text-slate-500">{user.email || t("account.aiviraAccount")}</p>
                    </div>
                    <UserMenuLink to="/account" icon={UserCircle} label={t("common.account")} onClick={closePanels} />
                    <UserMenuLink to="/account" icon={Settings} label={t("common.settings")} onClick={closePanels} />
                    <UserMenuLink to="/account#password" icon={KeyRound} label={t("common.changePassword")} onClick={closePanels} />
                    <button
                      type="button"
                      onClick={switchTheme}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-bold transition-colors hover:bg-slate-100"
                    >
                      {isDark ? <Sun className="h-4 w-4 text-amber-500" /> : <Moon className="h-4 w-4 text-slate-500" />}
                      <span>{isDark ? t("common.lightMode") : t("common.darkMode")}</span>
                    </button>
                    <div className="my-1 h-px bg-slate-100" />
                    <button
                      type="button"
                      onClick={logout}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-bold text-red-600 transition-colors hover:bg-red-50"
                    >
                      <LogOut className="h-4 w-4" />
                      <span>{t("common.logout")}</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <button
                type="button"
                onClick={onAuth}
                className={[
                  "flex items-center gap-2 rounded-full border p-1.5 pl-2 pr-4 transition-all duration-300 hover:shadow-md",
                  isSolid ? "border-slate-200 bg-white text-slate-700 hover:border-slate-300" : "border-white/20 bg-white/10 text-white hover:bg-white/20"
                ].join(" ")}
              >
                <div className={["flex h-6 w-6 items-center justify-center rounded-full", isSolid ? "bg-slate-100" : "bg-white/20"].join(" ")}>
                  <User className="h-3.5 w-3.5" />
                </div>
                <span className="text-sm font-bold">{t("common.login")}</span>
              </button>
            )}
          </div>
        </div>

        <div className="flex items-center gap-4 md:hidden">
          <NavLink to="/cart" onClick={() => setMobileOpen(false)} className={["relative rounded-full p-2 transition-colors hover:bg-slate-500/10", isSolid ? "text-slate-700" : "text-white"].join(" ")}>
            <ShoppingBag className="h-5 w-5" strokeWidth={2} />
            {cartCount > 0 && (
              <span className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-blue-600 px-1 text-[9px] font-bold text-white ring-2 ring-white">
                {cartCount > 99 ? "99+" : cartCount}
              </span>
            )}
          </NavLink>
          <button
            type="button"
            onClick={() => setMobileOpen((value) => !value)}
            className={["rounded-lg p-2 transition-colors", isSolid ? "text-slate-900 hover:bg-slate-100" : "bg-white/80 text-slate-900 hover:bg-white"].join(" ")}
            aria-label={t("nav.toggleMenu")}
          >
            <Menu className="h-6 w-6" />
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="mx-4 mt-3 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-xl backdrop-blur-xl md:hidden">
          <form onSubmit={submitSearch} className="relative mb-4">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" strokeWidth={2.5} />
            <input
              type="text"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t("nav.searchMobilePlaceholder")}
              className="w-full rounded-full border border-slate-200 bg-slate-50 py-2.5 pl-11 pr-4 text-sm text-slate-900 outline-none focus:ring-2 focus:ring-blue-500/30"
            />
          </form>

          {results.length > 0 && (
            <div className="mb-4 overflow-hidden rounded-xl border border-slate-100">
              {results.map((book) => (
                <Link key={book.id} to={`/product/${book.slug}`} onClick={closePanels} className="flex items-center gap-3 border-b border-slate-100 p-3 last:border-b-0">
                  <img src={book.image || book.cover} alt={book.title} className="h-14 w-10 rounded object-cover" />
                  <span className="min-w-0">
                    <strong className="line-clamp-1 text-sm text-slate-900">{book.title}</strong>
                    <small className="line-clamp-1 text-xs text-slate-500">{book.author}</small>
                  </span>
                </Link>
              ))}
            </div>
          )}

          <div className="grid gap-2 text-sm font-bold text-slate-700">
            <NavLink to="/" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.home")}</NavLink>
            <NavLink to="/category/all" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.categories")}</NavLink>
            <NavLink to="/orders" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.orders")}</NavLink>
            <div className="px-3 py-2">
              <LanguageSwitcher compact />
            </div>
            {user ? (
              <>
                <div className="rounded-xl bg-slate-50 px-3 py-3">
                  <p className="truncate text-sm font-black text-slate-950">{displayName}</p>
                  <p className="mt-0.5 truncate text-xs text-slate-500">{user.email || t("account.aiviraAccount")}</p>
                </div>
                <NavLink to="/account" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.account")}</NavLink>
                <NavLink to="/account" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.settings")}</NavLink>
                <NavLink to="/account#password" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">{t("common.changePassword")}</NavLink>
                <button
                  type="button"
                  onClick={switchTheme}
                  className="rounded-xl px-3 py-2 text-left hover:bg-slate-100"
                >
                  {isDark ? t("common.lightMode") : t("common.darkMode")}
                </button>
                <button type="button" onClick={logout} className="rounded-xl px-3 py-2 text-left text-red-600 hover:bg-red-50">
                  {t("common.logout")}
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setMobileOpen(false);
                  onAuth?.();
                }}
                className="rounded-xl px-3 py-2 text-left text-blue-600 hover:bg-blue-50"
              >
                {t("common.login")}
              </button>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}

function UserMenuLink({ to, icon: Icon, label, onClick }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold transition-colors hover:bg-slate-100"
    >
      <Icon className="h-4 w-4 text-slate-500" />
      <span>{label}</span>
    </Link>
  );
}
