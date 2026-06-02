import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { Compass, Menu, Search, ShoppingBag, User } from "lucide-react";
import { logout as logoutRequest } from "../api/authApi.js";
import { getCart } from "../api/cartApi.js";
import { getProducts } from "../api/catalogApi.js";
import { normalizeBook, pageRows } from "../utils/mappers.js";
import { clearAuth, getAccessToken, getRefreshToken } from "../utils/storage.js";

export default function Navbar({ solid, user, onAuth }) {
  const [scrolled, setScrolled] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const [mobileOpen, setMobileOpen] = useState(false);
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
    }

    clearAuth();
    navigate("/");
    setMobileOpen(false);
  }

  function submitSearch(event) {
    event.preventDefault();
    const keyword = query.trim();
    if (!keyword) return;

    navigate(`/category/all?search=${encodeURIComponent(keyword)}`);
    setResults([]);
    setMobileOpen(false);
  }

  function closePanels() {
    setResults([]);
    setMobileOpen(false);
  }

  const navTextClass = isSolid ? "text-slate-600" : "text-white/85";
  const activeTextClass = isSolid ? "text-blue-600" : "text-white";

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
        <Link to="/" onClick={closePanels} className="group flex flex-shrink-0 items-center gap-2">
          <div
            className={[
              "flex h-9 w-9 items-center justify-center rounded-xl shadow-lg transition-all duration-500 group-hover:scale-105",
              isSolid ? "bg-slate-900 shadow-slate-900/20" : "bg-white shadow-white/20"
            ].join(" ")}
          >
            <span className={["font-serif text-xl font-bold leading-none", isSolid ? "text-white" : "text-slate-900"].join(" ")}>
              A
            </span>
          </div>
          <span className={["font-serif text-xl font-bold tracking-wider transition-colors duration-500", isSolid ? "text-slate-900" : "text-white"].join(" ")}>
            AIVIRA
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
              placeholder="Search books by title or author..."
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
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Top Results</span>
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
                  View all results for "{query}"
                </button>
              </div>
            </div>
          )}
        </div>

        <div className={["hidden items-center gap-8 transition-colors duration-500 md:flex", navTextClass].join(" ")}>
          <div className="flex items-center gap-6 text-sm font-bold tracking-wide">
            <NavLink to="/" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <span>Home</span>
              <span className="absolute -bottom-1.5 left-0 h-[2px] w-full origin-left scale-x-0 rounded-full bg-blue-500 transition-transform group-hover:scale-x-100" />
            </NavLink>
            <NavLink to="/category/all" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <Compass className="h-4 w-4 opacity-70 transition-opacity group-hover:opacity-100" />
              <span>Explore</span>
              <span className="absolute -bottom-1.5 left-0 h-[2px] w-full origin-left scale-x-0 rounded-full bg-blue-500 transition-transform group-hover:scale-x-100" />
            </NavLink>
            <NavLink to="/orders" className={({ isActive }) => ["group relative flex items-center gap-2 transition-colors hover:text-blue-500", isActive ? activeTextClass : ""].join(" ")}>
              <span>Orders</span>
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

            {user ? (
              <a
                href="/"
                onClick={logout}
                className={[
                  "flex items-center gap-2 rounded-full border p-1.5 pl-2 pr-4 transition-all duration-300 hover:shadow-md",
                  isSolid ? "border-slate-200 bg-white text-slate-700 hover:border-slate-300" : "border-white/20 bg-white/10 text-white hover:bg-white/20"
                ].join(" ")}
              >
                <div className={["flex h-6 w-6 items-center justify-center rounded-full", isSolid ? "bg-slate-100" : "bg-white/20"].join(" ")}>
                  <User className="h-3.5 w-3.5" />
                </div>
                <span className="max-w-[150px] truncate text-sm font-bold">
                  {user.username || user.email || "Reader"} / Logout
                </span>
              </a>
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
                <span className="text-sm font-bold">Login</span>
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
            aria-label="Toggle menu"
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
              placeholder="Search books..."
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
            <NavLink to="/" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">Home</NavLink>
            <NavLink to="/category/all" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">Categories</NavLink>
            <NavLink to="/orders" onClick={closePanels} className="rounded-xl px-3 py-2 hover:bg-slate-100">Orders</NavLink>
            {user ? (
              <a href="/" onClick={logout} className="rounded-xl px-3 py-2 text-blue-600 hover:bg-blue-50">
                Hi, {user.username || user.email || "Reader"} / Logout
              </a>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setMobileOpen(false);
                  onAuth?.();
                }}
                className="rounded-xl px-3 py-2 text-left text-blue-600 hover:bg-blue-50"
              >
                Login
              </button>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
