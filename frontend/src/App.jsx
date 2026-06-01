import { useEffect, useMemo, useRef, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import AuthModal from "./components/AuthModal.jsx";
import Layout from "./components/Layout.jsx";
import AccountPage from "./pages/AccountPage.jsx";
import CartPage from "./pages/CartPage.jsx";
import CategoryPage from "./pages/CategoryPage.jsx";
import CheckoutPage from "./pages/CheckoutPage.jsx";
import HomePage from "./pages/HomePage.jsx";
import OrdersPage from "./pages/OrdersPage.jsx";
import PaymentResultPage from "./pages/PaymentResultPage.jsx";
import ProductPage from "./pages/ProductPage.jsx";
import AdminCategoriesPage from "./pages/admin/AdminCategoriesPage.jsx";
import AdminLayout from "./pages/admin/AdminLayout.jsx";
import AdminOrdersPendingPage from "./pages/admin/AdminOrdersPendingPage.jsx";
import AdminPaymentsPage from "./pages/admin/AdminPaymentsPage.jsx";
import AdminPermissionsPage from "./pages/admin/AdminPermissionsPage.jsx";
import AdminProductsPage from "./pages/admin/AdminProductsPage.jsx";
import { initMotionEffects } from "./utils/motion.js";
import { getCurrentUser } from "./utils/storage.js";

export default function App() {
  const [authOpen, setAuthOpen] = useState(false);
  const [user, setUser] = useState(getCurrentUser());
  const location = useLocation();

  useEffect(() => {
    const sync = () => setUser(getCurrentUser());
    window.addEventListener("aivira-auth", sync);
    return () => window.removeEventListener("aivira-auth", sync);
  }, []);

  useEffect(() => {
    let cleanup = () => {};
    const timer = window.setTimeout(() => {
      cleanup = initMotionEffects(document);
    }, 80);
    return () => {
      window.clearTimeout(timer);
      cleanup();
    };
  }, [location.pathname, location.search]);

  return (
    <>
      <MotionChrome />
      <IntroBook />
      <Routes>
        <Route element={<Layout user={user} onAuth={() => setAuthOpen(true)} />}>
          <Route index element={<HomePage />} />
          <Route path="/category" element={<Navigate to="/category/all" replace />} />
          <Route path="/category/:slug" element={<CategoryPage />} />
          <Route path="/books/:slug" element={<LegacyProductRedirect />} />
          <Route path="/product/:slug" element={<ProductPage onAuth={() => setAuthOpen(true)} />} />
          <Route path="/cart" element={<CartPage onAuth={() => setAuthOpen(true)} />} />
          <Route path="/checkout" element={<CheckoutPage onAuth={() => setAuthOpen(true)} />} />
          <Route path="/orders" element={<OrdersPage onAuth={() => setAuthOpen(true)} />} />
          <Route path="/account" element={<AccountPage onAuth={() => setAuthOpen(true)} />} />
          <Route path="/payment/result" element={<Navigate to="/payment-result" replace />} />
          <Route path="/payment-result" element={<PaymentResultPage />} />
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<Navigate to="/admin/products" replace />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="categories" element={<AdminCategoriesPage />} />
            <Route path="payments" element={<AdminPaymentsPage />} />
            <Route path="permissions" element={<AdminPermissionsPage />} />
            <Route path="orders-pending" element={<AdminOrdersPendingPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} />
    </>
  );
}

function MotionChrome() {
  const [progress, setProgress] = useState(0);
  const [showTop, setShowTop] = useState(false);
  const cursorRef = useRef(null);
  const ringRef = useRef(null);

  useEffect(() => {
    let frame = 0;
    let target = { x: -100, y: -100 };
    let ring = { x: -100, y: -100 };

    const updateScroll = () => {
      const scrollable = document.documentElement.scrollHeight - window.innerHeight;
      const pct = scrollable > 0 ? (window.scrollY / scrollable) * 100 : 0;
      setProgress(pct);
      setShowTop(window.scrollY > 600);
      const heroAtmosphere = document.querySelector(".h-atmo");
      if (heroAtmosphere) heroAtmosphere.style.transform = `translateY(${window.scrollY * 0.06}px)`;
    };

    const updateCursorState = (event) => {
      const interactive = event.target.closest?.("a, button, .book-card, .cat-c, input, textarea, select");
      const text = event.target.closest?.("input, textarea, select");
      cursorRef.current?.classList.toggle("c-hover", Boolean(interactive));
      ringRef.current?.classList.toggle("c-hover", Boolean(interactive));
      cursorRef.current?.classList.toggle("c-text", Boolean(text));
    };

    const move = (event) => {
      target = { x: event.clientX, y: event.clientY };
      updateCursorState(event);
    };

    const tick = () => {
      ring.x += (target.x - ring.x) * 0.16;
      ring.y += (target.y - ring.y) * 0.16;
      if (cursorRef.current) {
        cursorRef.current.style.left = `${target.x}px`;
        cursorRef.current.style.top = `${target.y}px`;
      }
      if (ringRef.current) {
        ringRef.current.style.left = `${ring.x}px`;
        ringRef.current.style.top = `${ring.y}px`;
      }
      frame = requestAnimationFrame(tick);
    };

    updateScroll();
    window.addEventListener("scroll", updateScroll, { passive: true });
    window.addEventListener("mousemove", move, { passive: true });
    window.addEventListener("mouseover", updateCursorState);
    frame = requestAnimationFrame(tick);
    return () => {
      window.removeEventListener("scroll", updateScroll);
      window.removeEventListener("mousemove", move);
      window.removeEventListener("mouseover", updateCursorState);
      cancelAnimationFrame(frame);
    };
  }, []);

  useEffect(() => {
    const click = (event) => {
      const button = event.target.closest?.(".btn-fill, .btn-buy, .n-cta, .auth-submit");
      if (!button) return;
      const rect = button.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "btn-ripple";
      ripple.style.left = `${event.clientX - rect.left}px`;
      ripple.style.top = `${event.clientY - rect.top}px`;
      button.appendChild(ripple);
      window.setTimeout(() => ripple.remove(), 780);
    };
    document.addEventListener("click", click);
    return () => document.removeEventListener("click", click);
  }, []);

  return (
    <>
      <div id="sprog" style={{ width: `${progress}%` }} />
      <div id="cur" ref={cursorRef} />
      <div id="cur-ring" ref={ringRef} />
      <button id="btt" className={showTop ? "show" : ""} type="button" onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}>↑</button>
    </>
  );
}

function LegacyProductRedirect() {
  const slug = window.location.pathname.split("/").pop();
  return <Navigate to={`/product/${slug}`} replace />;
}

function IntroBook() {
  const [visible, setVisible] = useState(() => sessionStorage.getItem("aivira_intro_seen") !== "true");
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!visible) return undefined;
    const timer = setTimeout(() => play(), 1500);
    return () => clearTimeout(timer);
  }, [visible]);

  function play() {
    if (open) return;
    setOpen(true);
    sessionStorage.setItem("aivira_intro_seen", "true");
    setTimeout(() => setVisible(false), 2700);
  }

  if (!visible) return null;

  return (
    <div className={`intro ${open ? "intro-leave" : ""}`} onClick={play}>
      <StarField count={150} />
      <div className="intro-nebula" />
      <div className="intro-scene">
        <div className="ic">
          <div className="is" />
          <div className="ic-pages-edge" />
          <div className={`ic-inner-text ${open ? "show" : ""}`}>
            <div className="ici-logo">AIVIRA</div>
            <div className="ici-div" />
            <div className="ici-quote">Unlock your<br />new chapters</div>
          </div>
          <div className={`ic-cover ${open ? "open" : ""}`}>
            <div className="ic-border" />
            <StarField count={55} small />
            <div className="ic-mark">A</div>
            <div className="ic-logo">AIVIRA</div>
            <div className="ic-sep" />
            <div className="ic-tagline">Unlock your new chapters</div>
          </div>
          <div className={`ic-cover-back ${open ? "open" : ""}`} />
        </div>
      </div>
      {!open && <div className="click-enter">Click anywhere to enter</div>}
    </div>
  );
}

function StarField({ count = 80, small = false }) {
  const stars = useMemo(() => Array.from({ length: count }, (_, index) => ({
    id: index,
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    size: `${(small ? 0.5 : 0.7) + Math.random() * (small ? 1.2 : 2)}px`,
    delay: `${Math.random() * 5}s`,
    duration: `${2 + Math.random() * 4}s`,
    opacity: 0.16 + Math.random() * 0.58
  })), [count, small]);

  return (
    <div className="stars">
      {stars.map((star) => (
        <i key={star.id} style={{ left: star.left, top: star.top, width: star.size, height: star.size, animationDelay: star.delay, animationDuration: star.duration, opacity: star.opacity }} />
      ))}
    </div>
  );
}

function NotFoundPage() {
  return <div className="page-shell"><div className="empty"><h3>Page not found</h3></div></div>;
}
