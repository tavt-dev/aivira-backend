import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { logout as logoutRequest } from "../api/authApi.js";
import { getCart } from "../api/cartApi.js";
import { getProducts } from "../api/catalogApi.js";
import { normalizeBook, pageRows } from "../utils/mappers.js";
import { clearAuth, getAccessToken, getRefreshToken } from "../utils/storage.js";

export default function Navbar({ solid, user, onAuth }) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const navigate = useNavigate();

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
  }

  function submitSearch(event) {
    event.preventDefault();
    navigate(`/category/all?search=${encodeURIComponent(query.trim())}`);
    setResults([]);
  }

  return (
    <nav className={solid ? "site-nav nav-solid" : "site-nav"}>
      <Link to="/" className="n-logo">
        <span className="n-pip" />
        AIVIRA
      </Link>
      <form className="n-search" onSubmit={submitSearch}>
        <span className="search-icon">Search</span>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search books by title or author..."
        />
        {results.length > 0 && (
          <div className="search-res show">
            {results.map((book) => (
              <Link key={book.id} to={`/product/${book.slug}`} className="search-res-item" onClick={() => setResults([])}>
                <img src={book.image || book.cover} alt={book.title} className="s-thumb" />
                <span>
                  <strong>{book.title}</strong>
                  <small>{book.author}</small>
                </span>
              </Link>
            ))}
          </div>
        )}
      </form>
      <ul className="n-links">
        <li><NavLink to="/">Home</NavLink></li>
        <li><NavLink to="/category/all">Categories</NavLink></li>
        <li><NavLink to="/cart">Cart {cartCount ? `(${cartCount})` : ""}</NavLink></li>
        <li><NavLink to="/orders">Orders</NavLink></li>
        <li><NavLink to="/admin/products">Admin</NavLink></li>
        <li>
          {user ? (
            <a href="/" className="n-cta nav-login-btn" onClick={logout}>
              Hi, {user.username || user.email || "Reader"} / Logout
            </a>
          ) : (
            <button className="n-cta nav-login-btn" type="button" onClick={onAuth}>Login</button>
          )}
        </li>
      </ul>
    </nav>
  );
}
