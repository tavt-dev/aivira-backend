import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { ArrowUp } from "lucide-react";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import AuthModal from "./components/AuthModal.jsx";
import PageLoader from "./components/PageLoader.jsx";
import IntroBook, { hasSeenIntro } from "./components/IntroBook.jsx";
import Layout from "./components/Layout.jsx";
import RequireAuth from "./components/RequireAuth.jsx";
import RequireAdmin from "./components/RequireAdmin.jsx";
import { ConfirmDialogProvider, ToastProvider } from "./components/ui/index.jsx";
import AccountPage from "./pages/AccountPage.jsx";
import CartPage from "./pages/CartPage.jsx";
import CategoryPage from "./pages/CategoryPage.jsx";
import CheckoutPage from "./pages/CheckoutPage.jsx";
import GoogleOAuthResultPage from "./pages/GoogleOAuthResultPage.jsx";
import HomePage from "./pages/HomePage.jsx";
import OrdersPage from "./pages/OrdersPage.jsx";
import PaymentResultPage from "./pages/PaymentResultPage.jsx";
import ProductPage from "./pages/ProductPage.jsx";
import AdminCategoriesPage from "./pages/admin/AdminCategoriesPage.jsx";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage.jsx";
import AdminDiscountsPage from "./pages/admin/AdminDiscountsPage.jsx";
import AdminForbiddenPage from "./pages/admin/AdminForbiddenPage.jsx";
import AdminLayout from "./pages/admin/AdminLayout.jsx";
import AdminOrdersPage from "./pages/admin/AdminOrdersPage.jsx";
import AdminPaymentsPage from "./pages/admin/AdminPaymentsPage.jsx";
import AdminPermissionsPage from "./pages/admin/AdminPermissionsPage.jsx";
import AdminProductsPage from "./pages/admin/AdminProductsPage.jsx";
import AdminReviewsPage from "./pages/admin/AdminReviewsPage.jsx";
import AdminUsersPage from "./pages/admin/AdminUsersPage.jsx";
import { initMotionEffects } from "./utils/motion.js";
import { getCurrentUser } from "./utils/storage.js";

export default function App() {
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState("login");
  const [user, setUser] = useState(getCurrentUser());
  const [introDone, setIntroDone] = useState(hasSeenIntro);
  const [pageLoaderDone, setPageLoaderDone] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const isAdminRoute = location.pathname.startsWith("/admin");
  const searchParams = new URLSearchParams(location.search);
  const authParam = searchParams.get("auth");
  const isAuthRequest = location.pathname === "/login"
    || location.pathname === "/register"
    || authParam === "login"
    || authParam === "register";
  const authNextPath = sanitizeNextPath(searchParams.get("next"));

  useEffect(() => {
    const sync = () => setUser(getCurrentUser());
    window.addEventListener("aivira-auth", sync);
    return () => window.removeEventListener("aivira-auth", sync);
  }, []);

  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
      setAuthMode("login");
      const next = `${location.pathname}${location.search}${location.hash}`;
      if (location.pathname.startsWith("/admin")) {
        navigate(`/?auth=login&next=${encodeURIComponent(next)}`, { replace: true });
        return;
      }
      setAuthOpen(true);
    };

    window.addEventListener("aivira-auth-expired", handleExpired);
    return () => window.removeEventListener("aivira-auth-expired", handleExpired);
  }, [location.hash, location.pathname, location.search, navigate]);

  useEffect(() => {
    document.body.classList.toggle("admin-route", isAdminRoute);
    return () => document.body.classList.remove("admin-route");
  }, [isAdminRoute]);

  useEffect(() => {
    if (!introDone || isAdminRoute) return undefined;
    let cleanup = () => {};
    const timer = window.setTimeout(() => {
      cleanup = initMotionEffects(document);
    }, 80);
    return () => {
      window.clearTimeout(timer);
      cleanup();
    };
  }, [introDone, isAdminRoute, location.pathname, location.search]);

  useEffect(() => {
    if ((!introDone && !isAuthRequest) || isAdminRoute) return;
    if (location.pathname === "/login" || authParam === "login") {
      openAuth("login");
    }
    if (location.pathname === "/register" || authParam === "register") {
      openAuth("register");
    }
  }, [authParam, introDone, isAuthRequest, isAdminRoute, location.pathname]);

  // PageLoader fallback dismiss (PageLoader handles its own onDone)
  useEffect(() => {
    const timer = setTimeout(() => setPageLoaderDone(true), 3000);
    return () => clearTimeout(timer);
  }, []);

  function openAuth(mode = "login") {
    setAuthMode(mode);
    setAuthOpen(true);
  }

  if (!introDone && !isAdminRoute && !isAuthRequest) {
    return <IntroBook onFinish={() => setIntroDone(true)} />;
  }

  return (
    <ToastProvider>
      <ConfirmDialogProvider>
        {/* Brand splash — shown on every page load, fades out in ~880ms */}
        {!pageLoaderDone && !isAdminRoute && (
          <PageLoader onDone={() => setPageLoaderDone(true)} />
        )}
        {!isAdminRoute && <MotionChrome />}
        <Routes>
          <Route path="/auth/google/success" element={<GoogleOAuthResultPage />} />
          <Route path="/auth/google/failure" element={<GoogleOAuthResultPage failure />} />
          <Route path="/admin/login" element={<Navigate to="/?auth=login&next=/admin/dashboard" replace />} />
          <Route path="/admin/forbidden" element={<AdminForbiddenPage />} />
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <AdminLayout />
              </RequireAdmin>
            }
          >
            <Route index element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboardPage />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="categories" element={<AdminCategoriesPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="orders-pending" element={<Navigate to="/admin/orders" replace />} />
            <Route path="discounts" element={<AdminDiscountsPage />} />
            <Route path="coupons" element={<Navigate to="/admin/discounts" replace />} />
            <Route path="promotions" element={<Navigate to="/admin/discounts" replace />} />
            <Route path="payments" element={<AdminPaymentsPage />} />
            <Route path="reviews" element={<AdminReviewsPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="permissions" element={<AdminPermissionsPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
          <Route element={<Layout user={user} onAuth={() => openAuth("login")} />}>
            <Route index element={<HomePage />} />
            <Route path="/login" element={<Navigate to="/" replace />} />
            <Route path="/register" element={<Navigate to="/" replace />} />
            <Route path="/verify-otp" element={<Navigate to="/login" replace />} />
            <Route path="/category" element={<Navigate to="/category/all" replace />} />
            <Route path="/category/:slug" element={<CategoryPage />} />
            <Route path="/books/:slug" element={<LegacyProductRedirect />} />
            <Route path="/product/:slug" element={<ProductPage onAuth={() => openAuth("login")} />} />
            <Route path="/cart" element={<RequireAuth><CartPage onAuth={() => openAuth("login")} /></RequireAuth>} />
            <Route path="/checkout" element={<RequireAuth><CheckoutPage onAuth={() => openAuth("login")} /></RequireAuth>} />
            <Route path="/orders" element={<RequireAuth><OrdersPage onAuth={() => openAuth("login")} /></RequireAuth>} />
            <Route path="/account" element={<RequireAuth><AccountPage onAuth={() => openAuth("login")} /></RequireAuth>} />
            <Route path="/payment/result" element={<Navigate to="/payment-result" replace />} />
            <Route path="/payment-result" element={<PaymentResultPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
        {!isAdminRoute && (
          <AuthModal
            open={authOpen}
            initialMode={authMode}
            nextPath={authNextPath}
            onClose={() => setAuthOpen(false)}
          />
        )}
      </ConfirmDialogProvider>
    </ToastProvider>
  );
}

function sanitizeNextPath(value) {
  if (!value || typeof value !== "string") return "";
  if (!value.startsWith("/") || value.startsWith("//")) return "";
  return value;
}

function MotionChrome() {
  const { t } = useTranslation();
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
    };

    const updateCursorState = (event) => {
      const interactive = event.target.closest?.("a, button, input, textarea, select");
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

  return (
    <>
      <div id="sprog" style={{ width: `${progress}%` }} />
      <div id="cur" ref={cursorRef} />
      <div id="cur-ring" ref={ringRef} />
      <button
        id="btt"
        className={showTop ? "show" : ""}
        type="button"
        onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        aria-label={t("common.top")}
        title={t("common.top")}
      >
        <ArrowUp aria-hidden="true" />
      </button>
    </>
  );
}

function LegacyProductRedirect() {
  const slug = window.location.pathname.split("/").pop();
  return <Navigate to={`/product/${slug}`} replace />;
}

function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <div className="mx-auto w-full max-w-4xl px-4 pb-20 pt-28 text-center md:px-8">
      <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16">
        <h1 className="font-serif text-4xl font-bold text-slate-950">{t("notFound.title")}</h1>
      </div>
    </div>
  );
}
