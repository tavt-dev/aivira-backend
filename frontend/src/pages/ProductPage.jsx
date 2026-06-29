import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  BookOpen, ChevronRight, Home, Layers, Minus, Plus,
  ShoppingBag, Star, TrendingUp, X, ZoomIn, MessageSquare,
  Award, Package, BarChart3,
} from "lucide-react";

import { addCartItem } from "../api/cartApi.js";
import { getProduct } from "../api/catalogApi.js";
import { getProductReviews } from "../api/reviewApi.js";
import { saveCheckoutCartItemIds } from "../utils/checkoutSelection.js";
import { discount, formatSold, formatVND } from "../utils/formatters.js";
import {
  normalizeBook, normalizeCartItem, normalizeReview,
  pageMeta as readPageMeta, pageRows,
} from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";
import { getTheme } from "../utils/theme.js";

/* ── Constants ─────────────────────────────── */
const REVIEW_SIZE = 5;

/* ── Particles ─────────────────────────────── */
const PARTICLES = [
  { left:"6%",  top:"22%", size:3, dur:"4.2s", delay:"0s",   op:0.55 },
  { left:"88%", top:"12%", size:2, dur:"5.6s", delay:"0.8s", op:0.40 },
  { left:"18%", top:"68%", size:2, dur:"3.9s", delay:"1.2s", op:0.45 },
  { left:"76%", top:"54%", size:3, dur:"5.0s", delay:"0.4s", op:0.50 },
  { left:"45%", top:"84%", size:2, dur:"6.2s", delay:"1.9s", op:0.38 },
  { left:"92%", top:"78%", size:2, dur:"5.8s", delay:"1.0s", op:0.38 },
];

/* ── Token system ───────────────────────────── */
function tokens(isDark) {
  if (isDark) return {
    pageBg:    "#07091a",
    surface1:  "rgba(10,15,42,0.95)",
    surface2:  "rgba(16,22,58,0.85)",
    surface3:  "rgba(22,28,70,0.75)",
    heroBg:    "linear-gradient(135deg,rgba(12,17,48,0.98) 0%,rgba(8,12,35,0.99) 60%,rgba(15,10,40,0.98) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
    border:    "rgba(255,255,255,0.075)",
    borderMid: "rgba(255,255,255,0.14)",
    text1:     "#e8eeff",
    text2:     "#8892b0",
    text3:     "#4a5578",
    accent:    "#4f6ef7",
    accentGlow:"rgba(79,110,247,0.35)",
    gold:      "#f0a500",
    emerald:   "#10d98a",
    red:       "#ef4444",
    inputBg:   "rgba(255,255,255,0.05)",
    orb1:      "rgba(79,110,247,0.25)",
    orb2:      "rgba(240,165,0,0.16)",
    orb3:      "rgba(167,139,250,0.15)",
    skA:       "rgba(255,255,255,0.03)",
    skB:       "rgba(79,110,247,0.10)",
    skC:       "rgba(167,139,250,0.08)",
  };
  return {
    pageBg:    "#f0ede8",
    surface1:  "rgba(255,252,246,0.97)",
    surface2:  "rgba(250,247,241,0.92)",
    surface3:  "rgba(244,241,234,0.88)",
    heroBg:    "linear-gradient(135deg,rgba(15,23,42,0.97) 0%,rgba(22,30,58,0.98) 60%,rgba(20,15,50,0.97) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#6d8fff 40%,#c4b5fd 70%,transparent)",
    border:    "rgba(15,23,42,0.10)",
    borderMid: "rgba(15,23,42,0.18)",
    text1:     "#0f172a",
    text2:     "#334155",
    text3:     "#94a3b8",
    accent:    "#1d4ed8",
    accentGlow:"rgba(29,78,216,0.22)",
    gold:      "#b45309",
    emerald:   "#047857",
    red:       "#b91c1c",
    inputBg:   "rgba(15,23,42,0.06)",
    orb1:      "rgba(79,110,247,0.10)",
    orb2:      "rgba(240,165,0,0.08)",
    orb3:      "rgba(167,139,250,0.08)",
    skA:       "rgba(15,23,42,0.05)",
    skB:       "rgba(37,99,235,0.07)",
    skC:       "rgba(139,92,246,0.05)",
  };
}

/* ── useTheme hook ──────────────────────────── */
function useTheme() {
  const [isDark, setIsDark] = useState(() => getTheme() === "dark");
  useEffect(() => {
    const sync = () => setIsDark(getTheme() === "dark");
    window.addEventListener("aivira-theme", sync);
    return () => window.removeEventListener("aivira-theme", sync);
  }, []);
  return isDark;
}

/* ═══════════════════════════════════════════════
   MAIN PAGE
═══════════════════════════════════════════════ */
export default function ProductPage({ onAuth }) {
  const { t } = useTranslation();
  const isDark = useTheme();
  const tk = tokens(isDark);
  const { slug } = useParams();
  const navigate = useNavigate();

  const [book, setBook]                         = useState(null);
  const [selectedImage, setSelectedImage]       = useState("");
  const [selectedVariationId, setVariationId]   = useState("");
  const [message, setMessage]                   = useState("");
  const [quantity, setQuantity]                 = useState(1);
  const [busy, setBusy]                         = useState(false);
  const [cartSuccess, setCartSuccess]           = useState(false);
  const [lightboxOpen, setLightboxOpen]         = useState(false);
  const [loading, setLoading]                   = useState(true);

  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    getProduct(slug, { signal: ctrl.signal })
      .then(data => {
        const b = normalizeBook(data);
        const defVar = pickDefaultVariation(b.variations);
        setBook(b);
        setVariationId(defVar?.id || "");
        setSelectedImage(buildGallery(b)[0]?.url || b.image || "");
        setQuantity(1); setMessage("");
      })
      .catch(err => { if (err.name === "AbortError") return; setBook(null); setMessage(err.message || t("product.notFound")); })
      .finally(() => { if (!ctrl.signal.aborted) setLoading(false); });
    return () => ctrl.abort();
  }, [slug, t]);

  const selectedVariation = useMemo(
    () => book?.variations?.find(v => String(v.id) === String(selectedVariationId)),
    [book?.variations, selectedVariationId]
  );
  const stockQuantity = Number(selectedVariation?.stockQuantity ?? book?.stockQuantity ?? 0);
  const canAdd        = Boolean(selectedVariation?.id) && stockQuantity > 0 && !busy;
  const gallery       = useMemo(() => buildGallery(book), [book]);
  const hasDiscount   = discount(book) > 0;

  useEffect(() => {
    setQuantity(c => Math.min(Math.max(1, c), Math.max(1, stockQuantity || 1)));
  }, [stockQuantity]);

  async function addToCart() {
    if (!getAccessToken()) { onAuth?.(); return; }
    if (!selectedVariation?.id) { setMessage(t("product.noVariation")); return; }
    if (stockQuantity <= 0)     { setMessage(t("product.outOfStock")); return; }
    setBusy(true); setMessage(""); setCartSuccess(false);
    try {
      await addCartItem({ productVariationId: selectedVariation.id, quantity });
      setCartSuccess(true);
      setMessage(t("product.added"));
      window.dispatchEvent(new Event("aivira-cart"));
      setTimeout(() => setCartSuccess(false), 3000);
    } catch (err) {
      setMessage(err.message || t("product.addFailed"));
    } finally {
      setBusy(false);
    }
  }

  async function checkoutNow() {
    if (!getAccessToken()) { onAuth?.(); return; }
    if (!selectedVariation?.id) { setMessage(t("product.noVariation")); return; }
    if (stockQuantity <= 0)     { setMessage(t("product.outOfStock")); return; }
    setBusy(true); setMessage(""); setCartSuccess(false);
    try {
      const cart = await addCartItem({ productVariationId: selectedVariation.id, quantity });
      const cartRows = readCartItems(cart);
      const target = cartRows.find(item => String(item.productVariationId) === String(selectedVariation.id));

      if (!target?.cartItemId) {
        throw new Error(t("checkout.noCartIds"));
      }

      saveCheckoutCartItemIds([target.cartItemId]);
      setCartSuccess(true);
      window.dispatchEvent(new Event("aivira-cart"));
      navigate("/checkout");
    } catch (err) {
      setMessage(err.message || t("checkout.unavailable"));
    } finally {
      setBusy(false);
    }
  }

  /* ── Skeleton state ─────────── */
  if (loading) return <ProductSkeleton tk={tk} isDark={isDark}/>;

  /* ── Error / not-found state ─── */
  if (!book) return (
    <div className="relative w-full overflow-hidden" style={{ background:tk.pageBg, minHeight:"100vh" }}>
      <div className="mx-auto flex min-h-[60vh] max-w-5xl items-center justify-center px-4 pb-20 pt-28 md:px-8">
        <motion.div initial={{opacity:0,scale:0.95}} animate={{opacity:1,scale:1}} transition={{duration:0.5}}
          className="w-full rounded-[28px] px-8 py-20 text-center"
          style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)" }}>
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-[28px]"
            style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 16px 40px rgba(79,110,247,0.4)" }}>
            <BookOpen size={32} color="#fff"/>
          </div>
          <h1 className="text-3xl font-bold" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
            {message || t("product.notFound")}
          </h1>
        </motion.div>
      </div>
    </div>
  );

  const catSlug = book.cat || "all";

  return (
    <div className="relative w-full overflow-hidden" style={{
      background: isDark
        ? "radial-gradient(ellipse at 60% 0%,rgba(30,24,80,0.9) 0%,#07091a 55%)"
        : "radial-gradient(ellipse at 60% 0%,rgba(210,220,255,0.35) 0%,#f0ede8 55%)",
      minHeight:"100vh"
    }}>
      {/* ── Dot-grid ── */}
      <div className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:`radial-gradient(circle,${isDark?"rgba(79,110,247,0.10)":"rgba(37,99,235,0.06)"} 1px,transparent 1px)`,
          backgroundSize:"48px 48px",
          maskImage:"radial-gradient(ellipse at 50% 0%,black 0%,transparent 70%)",
        }}/>

      {/* ── Ambient orbs ── */}
      <div className="pointer-events-none absolute -right-40 -top-40 h-[560px] w-[560px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb1} 0%,transparent 70%)`, filter:"blur(80px)" }}/>
      <div className="pointer-events-none absolute -left-32 top-[35%] h-[420px] w-[420px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb2} 0%,transparent 70%)`, filter:"blur(90px)" }}/>
      <div className="pointer-events-none absolute bottom-[15%] right-[25%] h-[350px] w-[350px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb3} 0%,transparent 70%)`, filter:"blur(80px)" }}/>

      <div className="relative z-10 mx-auto max-w-[1400px] px-4 pb-28 pt-20 md:px-8">
        {/* ── Hero bar ── */}
        <ProductHeroBar tk={tk} isDark={isDark} book={book} catSlug={catSlug} t={t}/>

        {/* ── Toast message ── */}
        <AnimatePresence>
          {message && (
            <motion.div key="toast"
              initial={{opacity:0,y:-12,scale:0.96}} animate={{opacity:1,y:0,scale:1}} exit={{opacity:0,y:-12,scale:0.96}}
              transition={{duration:0.3,ease:[0.22,1,0.36,1]}}
              className="mb-6 flex items-center gap-3 rounded-2xl px-5 py-4"
              style={{
                background: cartSuccess?"rgba(16,217,138,0.12)":"rgba(79,110,247,0.12)",
                border:`1px solid ${cartSuccess?"rgba(16,217,138,0.4)":"rgba(79,110,247,0.4)"}`,
                backdropFilter:"blur(20px)",
              }}>
              <span className="text-sm font-bold" style={{ color:cartSuccess?tk.emerald:tk.accent }}>{message}</span>
              <button type="button" onClick={() => setMessage("")} className="ml-auto opacity-60 hover:opacity-100">
                <X size={14} color={cartSuccess?tk.emerald:tk.accent}/>
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Main panel ── */}
        <motion.div
          initial={{opacity:0,y:32}} animate={{opacity:1,y:0}}
          transition={{duration:0.7,ease:[0.22,1,0.36,1],delay:0.08}}
          className="relative overflow-hidden rounded-[32px]"
          style={{
            background:tk.surface1,
            border:`1px solid ${tk.border}`,
            backdropFilter:"blur(28px)",
            boxShadow: isDark
              ? "0 40px 120px rgba(0,0,0,0.55), inset 0 1px 0 rgba(255,255,255,0.05)"
              : "0 20px 80px rgba(15,23,42,0.13), 0 4px 24px rgba(15,23,42,0.08), inset 0 1px 0 rgba(255,255,255,0.9)",
          }}>
          {/* Top accent line */}
          <div className="absolute left-0 right-0 top-0 h-[1.5px]"
            style={{ background:tk.heroLine }}/>

          <div className="grid gap-8 p-6 pb-0 md:p-10 md:pb-0 lg:grid-cols-[minmax(300px,460px)_1fr] lg:gap-12">
            {/* Gallery — trên (mobile) / trái (desktop) */}
            <motion.div initial={{opacity:0,x:-40}} animate={{opacity:1,x:0}}
              transition={{duration:0.65,delay:0.12,ease:[0.22,1,0.36,1]}}>
              <BookGallery
                gallery={gallery} selectedImage={selectedImage} onSelect={setSelectedImage}
                title={book.title} tk={tk} isDark={isDark}
                onLightbox={() => setLightboxOpen(true)} t={t}/>
            </motion.div>

            {/* Info — dưới (mobile) / phải (desktop) */}
            <motion.div initial={{opacity:0,x:40}} animate={{opacity:1,x:0}}
              transition={{duration:0.65,delay:0.18,ease:[0.22,1,0.36,1]}}
              className="flex min-w-0 flex-col justify-center">
              <BookInfoPanel
                book={book} tk={tk} isDark={isDark}
                hasDiscount={hasDiscount} stockQuantity={stockQuantity}
                selectedVariationId={selectedVariationId} onVariation={setVariationId}
                quantity={quantity} onQuantity={setQuantity}
                busy={busy} canAdd={canAdd} cartSuccess={cartSuccess}
                onAddToCart={addToCart} onCheckoutNow={checkoutNow} t={t}/>
            </motion.div>
          </div>

          {/* ── Metadata full-width ── */}
          <motion.div
            initial={{opacity:0,y:16}} animate={{opacity:1,y:0}}
            transition={{duration:0.5,delay:0.48,ease:[0.22,1,0.36,1]}}
            className="px-6 pb-8 pt-2 md:px-10 md:pb-10">
            <div className="h-px mb-8" style={{ background:tk.border }}/>
            <MetadataTable book={book} tk={tk} isDark={isDark} t={t}/>
          </motion.div>

        </motion.div>

        {/* ── Reviews ── */}
        <ReviewSection slug={slug} tk={tk} isDark={isDark}/>
      </div>

      {/* ── Lightbox — portal thoát khỏi overflow-hidden, đè lên navbar ── */}
      {createPortal(
        <AnimatePresence>
          {lightboxOpen && (
            <motion.div
              initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}
              transition={{duration:0.25}}
              className="fixed inset-0 flex items-center justify-center p-4"
              style={{ zIndex:9999, background:"rgba(0,0,0,0.92)", backdropFilter:"blur(14px)" }}
              onClick={() => setLightboxOpen(false)}>
              <motion.img
                initial={{scale:0.88,opacity:0}} animate={{scale:1,opacity:1}} exit={{scale:0.88,opacity:0}}
                transition={{duration:0.32,ease:[0.22,1,0.36,1]}}
                src={selectedImage} alt={book.title}
                className="max-h-[90vh] max-w-[90vw] rounded-2xl object-contain"
                style={{ boxShadow:"0 40px 120px rgba(0,0,0,0.85)" }}
                onClick={e => e.stopPropagation()}/>
              <motion.button
                initial={{opacity:0,scale:0.8}} animate={{opacity:1,scale:1}} exit={{opacity:0,scale:0.8}}
                transition={{duration:0.2,delay:0.1}}
                whileHover={{scale:1.12}} whileTap={{scale:0.92}}
                className="absolute right-6 top-6 flex h-12 w-12 items-center justify-center rounded-full transition-all"
                style={{ background:"rgba(255,255,255,0.14)", border:"1px solid rgba(255,255,255,0.25)", backdropFilter:"blur(8px)" }}
                onClick={() => setLightboxOpen(false)}>
                <X size={20} color="#fff"/>
              </motion.button>
            </motion.div>
          )}
        </AnimatePresence>,
        document.body
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════
   HERO BAR — Breadcrumb + ambient
═══════════════════════════════════════════════ */
function ProductHeroBar({ tk, book, catSlug, t }) {
  return (
    <motion.div
      initial={{opacity:0,y:-20}} animate={{opacity:1,y:0}}
      transition={{duration:0.55,ease:[0.22,1,0.36,1]}}
      className="relative mb-8 overflow-hidden rounded-[22px] px-6 py-5"
      style={{
        background: tk.heroBg,
        border:"1px solid rgba(255,255,255,0.06)",
        boxShadow:"0 20px 60px rgba(0,0,0,0.4)",
      }}>
      {/* Top line */}
      <div className="absolute left-0 right-0 top-0 h-[1px]"
        style={{ background:tk.heroLine }}/>
      {/* Glow */}
      <div className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full"
        style={{ background:"radial-gradient(circle,rgba(79,110,247,0.2) 0%,transparent 70%)" }}/>
      {/* Particles */}
      {PARTICLES.slice(0,4).map((p,i) => (
        <div key={i} className="catalog-particle-v2"
          style={{ left:p.left, top:p.top, width:p.size, height:p.size, background:"#4f6ef7",
            "--dur":p.dur, "--delay":p.delay, "--op":p.op }}/>
      ))}

      <nav className="relative flex flex-wrap items-center gap-1.5 text-xs">
        <Link to="/" className="flex items-center gap-1 transition-colors hover:text-blue-300"
          style={{ color:"#6374a8" }}>
          <Home size={11}/> <span>{t("common.home")}</span>
        </Link>
        <ChevronRight size={10} style={{ color:"#2d3561" }}/>
        <Link to={`/category/${catSlug}`} className="transition-colors hover:text-blue-300"
          style={{ color:"#6374a8" }}>
          {book.catLabel || t("common.books")}
        </Link>
        <ChevronRight size={10} style={{ color:"#2d3561" }}/>
        <span className="max-w-[300px] truncate font-semibold" style={{ color:"#93a8ff" }}>
          {book.title}
        </span>
      </nav>
    </motion.div>
  );
}

/* ═══════════════════════════════════════════════
   GALLERY
═══════════════════════════════════════════════ */
function BookGallery({ gallery, selectedImage, onSelect, title, tk, onLightbox, t }) {
  const [imgLoaded, setImgLoaded] = useState(false);
  const [hovered, setHovered]     = useState(false);

  return (
    <div className="grid gap-4">
      {/* Main image */}
      <button
        type="button"
        aria-label={t("product.openGallery", "Open image gallery")}
        className="group relative block w-full cursor-zoom-in overflow-hidden border-0 p-0 text-left"
        style={{ borderRadius:"20px", aspectRatio:"2/3" }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onClick={onLightbox}>
        {/* Shimmer while loading */}
        {!imgLoaded && (
          <div className="absolute inset-0 catalog-dynamic-shimmer"
            style={{ "--sa":tk.skA, "--sb":tk.skB, "--sc":tk.skC }}/>
        )}
        <img
          src={selectedImage || gallery[0]?.url} alt={title}
          onLoad={() => setImgLoaded(true)}
          className="h-full w-full object-cover transition-transform duration-700 ease-out"
          style={{ transform: hovered ? "scale(1.06)" : "scale(1)" }}/>
        {/* Spine decoration */}
        <div className="pointer-events-none absolute inset-y-0 left-0 w-4"
          style={{ background:"linear-gradient(to right,rgba(0,0,0,0.35),transparent)" }}/>
        {/* Zoom hint */}
        <motion.div
          animate={{ opacity: hovered ? 1 : 0, scale: hovered ? 1 : 0.85 }}
          transition={{ duration: 0.2 }}
          className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full"
          style={{ background:"rgba(0,0,0,0.55)", backdropFilter:"blur(8px)", border:"1px solid rgba(255,255,255,0.2)" }}>
          <ZoomIn size={18} color="#fff"/>
        </motion.div>
        {/* Bottom gradient overlay */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-24"
          style={{ background:"linear-gradient(to top,rgba(0,0,0,0.4),transparent)" }}/>
      </button>

      {/* Thumbnail strip */}
      {gallery.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-1" aria-label={t("product.gallery")}
          style={{ scrollbarWidth:"none" }}>
          {gallery.map((item,i) => {
            const active = selectedImage === item.url;
            return (
              <motion.button
                key={item.url}
                type="button"
                initial={{opacity:0,y:12}} animate={{opacity:1,y:0}}
                transition={{duration:0.3,delay:i*0.05}}
                onClick={() => onSelect(item.url)}
                whileHover={{ y:-3 }}
                className="relative flex-shrink-0 overflow-hidden"
                style={{
                  width:"56px", aspectRatio:"2/3",
                  borderRadius:"12px",
                  border: active
                    ? "2px solid #4f6ef7"
                    : `2px solid ${tk.border}`,
                  boxShadow: active
                    ? "0 0 0 3px rgba(79,110,247,0.25), 0 4px 16px rgba(79,110,247,0.3)"
                    : "none",
                  transition:"all 0.25s ease",
                }}>
                <img src={item.url} alt={item.alt || ""} className="h-full w-full object-cover"/>
                {active && (
                  <div className="absolute inset-0 rounded-[10px]"
                    style={{ background:"rgba(79,110,247,0.15)" }}/>
                )}
              </motion.button>
            );
          })}
        </div>
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════
   INFO PANEL
═══════════════════════════════════════════════ */
function BookInfoPanel({ book, tk, isDark, hasDiscount, stockQuantity, selectedVariationId, onVariation,
  quantity, onQuantity, busy, canAdd, cartSuccess, onAddToCart, onCheckoutNow, t }) {
  return (
    <div className="flex flex-col gap-7">
      {/* Category badge */}
      <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{duration:0.5,delay:0.2}}>
        <span className="inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-[0.68rem] font-black uppercase tracking-[0.18em]"
          style={{
            background: isDark?"rgba(79,110,247,0.15)":"rgba(37,99,235,0.10)",
            border:`1px solid ${isDark?"rgba(79,110,247,0.4)":"rgba(37,99,235,0.3)"}`,
            color:tk.accent,
          }}>
          <BookOpen size={11}/>
          {book.catLabel || t("common.books")}
        </span>
      </motion.div>

      {/* Title + Author */}
      <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.7,delay:0.22,ease:[0.22,1,0.36,1]}}>
        <h1 className="font-bold leading-[1.08] tracking-tight"
          style={{
            fontFamily:"var(--f-serif)",
            fontSize:"clamp(2rem,4vw,3.25rem)",
            letterSpacing:"-0.03em",
            background: isDark
              ? "linear-gradient(135deg,#e8eeff 0%,#b8c8ff 45%,#d4bcff 80%,#f0f4ff 100%)"
              : "linear-gradient(135deg,#0f172a 0%,#1e3a8a 60%,#1d4ed8 100%)",
            WebkitBackgroundClip:"text",
            WebkitTextFillColor:"transparent",
          }}>
          {book.title}
        </h1>
        <p className="mt-3 text-base font-medium italic" style={{ color:tk.text2 }}>
          {t("product.byAuthor", { author: book.author })}
        </p>
      </motion.div>

      {/* Rating row */}
      <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{duration:0.5,delay:0.28}}
        className="flex flex-wrap items-center gap-3">
        <RatingRow book={book} stockQuantity={stockQuantity} tk={tk} isDark={isDark} t={t}/>
      </motion.div>

      {/* Divider */}
      <div className="h-px" style={{ background:tk.border }}/>

      {/* Price */}
      <motion.div initial={{opacity:0,scale:0.92}} animate={{opacity:1,scale:1}}
        transition={{duration:0.55,delay:0.32,ease:[0.22,1,0.36,1]}}>
        <PriceBlock book={book} hasDiscount={hasDiscount} tk={tk} isDark={isDark} t={t}/>
      </motion.div>

      {/* Variations */}
      <motion.div initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} transition={{duration:0.5,delay:0.36}}>
        <VariationPicker variations={book.variations} selected={selectedVariationId} onSelect={onVariation} tk={tk} isDark={isDark} t={t}/>
      </motion.div>

      {/* Quantity + CTA */}
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{duration:0.55,delay:0.40}}
        className="flex flex-col gap-4">
        <QuantityRow value={quantity} max={Math.max(1,stockQuantity||1)} onChange={onQuantity} tk={tk} isDark={isDark} t={t}/>
        <CTAButtons
          busy={busy}
          canAdd={canAdd}
          cartSuccess={cartSuccess}
          onAdd={onAddToCart}
          onCheckoutNow={onCheckoutNow}
          tk={tk}
          isDark={isDark}
          t={t}
        />
      </motion.div>

      {/* Divider */}
      <div className="h-px" style={{ background:tk.border }}/>

      {/* Description */}
      {book.desc && (
        <motion.div initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} transition={{duration:0.5,delay:0.44}}>
          <DescriptionBlock desc={book.desc} tk={tk}/>
        </motion.div>
      )}

    </div>
  );
}

/* ── Rating row ──────────────────────────────── */
function RatingRow({ book, stockQuantity, tk, isDark, t }) {
  const rating = Number(book.rating || 0);
  const sold   = formatSold(book.sold);

  return (
    <>
      {/* Stars */}
      <div className="flex items-center gap-2 rounded-full px-3.5 py-2"
        style={{ background:isDark?"rgba(240,165,0,0.12)":"rgba(217,119,6,0.09)", border:`1px solid ${isDark?"rgba(240,165,0,0.3)":"rgba(217,119,6,0.22)"}` }}>
        <div className="flex items-center gap-0.5">
          {[1,2,3,4,5].map(n => (
            <Star key={n} size={13} fill={n<=Math.round(rating)?"currentColor":"none"}
              style={{ color:tk.gold }}/>
          ))}
        </div>
        <span className="text-xs font-black" style={{ color:tk.gold }}>{rating.toFixed(1)}</span>
      </div>

      {/* Sold */}
      <div className="flex items-center gap-1.5 rounded-full px-3.5 py-2"
        style={{ background:isDark?"rgba(79,110,247,0.10)":"rgba(37,99,235,0.07)", border:`1px solid ${tk.border}` }}>
        <TrendingUp size={12} style={{ color:tk.accent }}/>
        <span className="text-xs font-bold" style={{ color:tk.text2 }}>
          {t("product.sold", { sold })}
        </span>
      </div>

      {/* Stock */}
      <div className="flex items-center gap-1.5 rounded-full px-3.5 py-2"
        style={{
          background: stockQuantity>0 ? (isDark?"rgba(16,217,138,0.10)":"rgba(5,150,105,0.08)") : (isDark?"rgba(239,68,68,0.10)":"rgba(220,38,38,0.08)"),
          border:`1px solid ${stockQuantity>0?(isDark?"rgba(16,217,138,0.3)":"rgba(5,150,105,0.25)"):(isDark?"rgba(239,68,68,0.3)":"rgba(220,38,38,0.25)")}`,
        }}>
        {stockQuantity > 0
          ? <>
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-60" style={{ background:tk.emerald }}/>
                <span className="relative inline-flex h-2 w-2 rounded-full" style={{ background:tk.emerald }}/>
              </span>
              <span className="text-xs font-bold" style={{ color:tk.emerald }}>
                {t("product.inStock", { count: stockQuantity })}
              </span>
            </>
          : <>
              <span className="h-2 w-2 rounded-full" style={{ background:tk.red }}/>
              <span className="text-xs font-bold" style={{ color:tk.red }}>{t("product.outOfStock")}</span>
            </>}
      </div>
    </>
  );
}

/* ── Price block ─────────────────────────────── */
function PriceBlock({ book, hasDiscount, tk, isDark, t }) {
  const discPct = discount(book);
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex flex-col">
        <motion.span
          initial={{scale:0.85,opacity:0}} animate={{scale:1,opacity:1}}
          transition={{duration:0.5,type:"spring",stiffness:300,damping:20}}
          className="font-black"
          style={{
            fontFamily:"var(--f-serif)",
            fontSize:"clamp(2.4rem,5vw,3.4rem)",
            letterSpacing:"-0.04em",
            background: isDark
              ? "linear-gradient(135deg,#e8eeff,#93a8ff)"
              : "linear-gradient(135deg,#0f172a,#1e3a8a)",
            WebkitBackgroundClip:"text",
            WebkitTextFillColor:"transparent",
          }}>
          {formatVND(book.price)}
        </motion.span>
        {hasDiscount && (
          <span className="text-base font-medium line-through" style={{ color:tk.text3 }}>
            {formatVND(book.priceOld)}
          </span>
        )}
      </div>
      {hasDiscount && (
        <motion.div initial={{scale:0,opacity:0}} animate={{scale:1,opacity:1}}
          transition={{type:"spring",stiffness:400,damping:18,delay:0.1}}
          className="flex flex-col items-center gap-0.5">
          <span className="rounded-full px-3 py-1.5 text-sm font-black"
            style={{ background:"rgba(234,88,12,0.15)", border:"1px solid rgba(234,88,12,0.35)", color:"#fb923c" }}>
            -{discPct}%
          </span>
          <span className="text-[10px] font-bold" style={{ color:tk.text3 }}>
            {t("product.off", { discount: discPct })}
          </span>
        </motion.div>
      )}
    </div>
  );
}

/* ── Description block ───────────────────────── */
function DescriptionBlock({ desc, tk }) {
  const [expanded, setExpanded] = useState(false);
  const SHORT_LEN = 280;
  const isLong = desc.length > SHORT_LEN;
  const shown  = expanded || !isLong ? desc : desc.slice(0, SHORT_LEN) + "…";
  return (
    <div>
      <p className="text-sm leading-7" style={{ color:tk.text2 }}>{shown}</p>
      {isLong && (
        <button type="button" onClick={() => setExpanded(e => !e)}
          className="mt-2 text-xs font-bold transition-colors hover:opacity-80"
          style={{ color:tk.accent }}>
          {expanded ? "Thu gọn ▲" : "Xem thêm ▼"}
        </button>
      )}
    </div>
  );
}

/* ── Metadata table ──────────────────────────── */
function MetadataTable({ book, tk, isDark, t }) {
  const rows = [
    [t("product.metaAuthor"),          book.author],
    [t("product.metaIsbn"),            book.isbn],
    [t("product.metaPublisher"),       book.publisher],
    [t("product.metaPublicationYear"), book.publicationYear],
    [t("product.metaLanguage"),        book.bookLanguage],
    [t("product.metaPageCount"),       book.pageCount],
    [t("product.metaFormat"),          book.bookFormat],
    [t("product.metaDimensions"),      book.dimensions],
    [t("product.metaCategory"),        book.catLabel],
  ].filter(([,v]) => v !== undefined && v !== null && v !== "");
  if (!rows.length) return null;
  const icons = { [t("product.metaAuthor")]: Award, [t("product.metaPageCount")]: Layers, [t("product.metaFormat")]: Package };

  return (
    <div className="rounded-2xl overflow-hidden"
      style={{ border:`1px solid ${tk.border}`, backdropFilter:"blur(20px)" }}>
      {/* Header */}
      <div className="flex items-center gap-3 px-6 py-5"
        style={{ background:isDark?"rgba(18,24,64,0.6)":"rgba(15,23,42,0.04)", borderBottom:`1px solid ${tk.border}` }}>
        <BarChart3 size={18} style={{ color:tk.accent }}/>
        <h2 className="text-base font-black uppercase tracking-wider" style={{ color:tk.text1 }}>
          {t("product.metadata")}
        </h2>
      </div>
      {/* Rows */}
      <div className="grid md:grid-cols-2">
        {rows.map(([label, value], i) => {
          const Icon = icons[label];
          return (
            <div key={label}
              className="flex items-start gap-4 px-6 py-5 transition-colors"
              style={{
                borderBottom: i < rows.length - 1 ? `1px solid ${tk.border}` : "none",
                borderRight: i % 2 === 0 && i < rows.length - 1 ? `1px solid ${tk.border}` : "none",
              }}>
              {Icon && <Icon size={16} style={{ color:tk.accent, marginTop:4, flexShrink:0 }}/>}
              <div>
                <dt className="text-[0.7rem] font-black uppercase tracking-[0.14em]" style={{ color:tk.text3 }}>{label}</dt>
                <dd className="mt-1 text-base font-semibold" style={{ color:tk.text1 }}>{value}</dd>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ── Variation picker ────────────────────────── */
function VariationPicker({ variations, selected, onSelect, tk, isDark, t }) {
  const active = (variations || []).filter(v => v.active !== false);
  if (!active.length) return null;
  return (
    <div>
      <h3 className="mb-3 text-[0.62rem] font-black uppercase tracking-[0.18em]" style={{ color:tk.text3 }}>
        {t("product.variations")}
      </h3>
      <div className="grid gap-2.5 md:grid-cols-2">
        {active.map(v => {
          const isSelected = String(selected) === String(v.id);
          const label = [v.size, v.color].filter(Boolean).join(" / ") || t("product.defaultVariation");
          return (
            <motion.button
              key={v.id}
              type="button"
              whileHover={{ scale:1.02 }}
              whileTap={{ scale:0.98 }}
              onClick={() => onSelect(v.id)}
              className="relative overflow-hidden rounded-2xl p-4 text-left transition-all duration-250"
              style={isSelected ? {
                background: isDark?"rgba(79,110,247,0.15)":"rgba(37,99,235,0.10)",
                border:"1px solid rgba(79,110,247,0.5)",
                boxShadow:"0 0 0 3px rgba(79,110,247,0.12), 0 8px 24px rgba(79,110,247,0.15)",
              } : {
                background:tk.surface2,
                border:`1px solid ${tk.border}`,
              }}>
              {isSelected && (
                <div className="absolute inset-0 rounded-2xl"
                  style={{ background:"linear-gradient(135deg,rgba(79,110,247,0.1),transparent)" }}/>
              )}
              <strong className="relative block text-sm font-bold" style={{ color:tk.text1 }}>{label}</strong>
              {v.sku && <span className="relative mt-0.5 block text-[0.62rem] font-medium uppercase tracking-wider" style={{ color:tk.text3 }}>{v.sku}</span>}
              <span className="relative mt-2 block text-xs font-bold"
                style={{ color: Number(v.stockQuantity)>0 ? tk.emerald : tk.red }}>
                {t("product.variationStock", { count: v.stockQuantity || 0 })}
              </span>
              {Number(v.additionalPrice || 0) > 0 && (
                <span className="relative mt-1 block text-sm font-black" style={{ color:tk.accent }}>
                  +{formatVND(v.additionalPrice)}
                </span>
              )}
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}

/* ── Quantity stepper ────────────────────────── */
function QuantityRow({ value, max, onChange, tk, t }) {
  return (
    <div className="flex items-center gap-4">
      <span className="text-sm font-bold" style={{ color:tk.text2 }}>{t("product.quantity")}</span>
      <div className="flex items-center overflow-hidden rounded-2xl"
        style={{ background:tk.surface2, border:`1px solid ${tk.border}`, backdropFilter:"blur(20px)" }}>
        <motion.button
          type="button" whileHover={{scale:1.1}} whileTap={{scale:0.9}}
          onClick={() => onChange(Math.max(1, value-1))}
          className="flex h-12 w-12 items-center justify-center transition-colors"
          style={{ color:tk.text2 }}
          disabled={value <= 1}>
          <Minus size={16}/>
        </motion.button>
        <div className="flex h-12 w-16 items-center justify-center border-x text-sm font-black"
          style={{ borderColor:tk.border, color:tk.text1 }}>
          {value}
        </div>
        <motion.button
          type="button" whileHover={{scale:1.1}} whileTap={{scale:0.9}}
          onClick={() => onChange(Math.min(max, value+1))}
          className="flex h-12 w-12 items-center justify-center transition-colors"
          style={{ color:tk.text2 }}
          disabled={value >= max}>
          <Plus size={16}/>
        </motion.button>
      </div>
      <span className="text-xs font-medium" style={{ color:tk.text3 }}>/ {max}</span>
    </div>
  );
}

/* ── CTA Buttons ─────────────────────────────── */
function CTAButtons({ busy, canAdd, cartSuccess, onAdd, onCheckoutNow, tk, isDark, t }) {
  return (
    <div className="flex flex-wrap gap-3">
      {/* Primary */}
      <motion.button
        type="button"
        onClick={onAdd}
        disabled={!canAdd}
        whileHover={canAdd?{scale:1.03,y:-2}:{}}
        whileTap={canAdd?{scale:0.97}:{}}
        className="relative flex items-center gap-2.5 overflow-hidden rounded-full px-8 py-4 text-sm font-black uppercase tracking-wider text-white transition-all disabled:cursor-not-allowed disabled:opacity-40"
        style={{
          background: cartSuccess
            ? "linear-gradient(135deg,#059669,#10d98a)"
            : "linear-gradient(135deg,#2a3ecc,#4f6ef7,#818cf8)",
          boxShadow: canAdd
            ? (cartSuccess
              ? "0 8px 28px rgba(16,217,138,0.45)"
              : "0 8px 28px rgba(79,110,247,0.45)")
            : "none",
          transition:"all 0.35s cubic-bezier(0.22,1,0.36,1)",
        }}>
        {/* Shine sweep */}
        {canAdd && (
          <div className="absolute inset-0 -translate-x-full animate-[shimmer_2.5s_ease_infinite]"
            style={{ background:"linear-gradient(105deg,transparent 40%,rgba(255,255,255,0.25) 50%,transparent 60%)" }}/>
        )}
        <ShoppingBag size={16}/>
        <span className="relative">
          {busy ? t("common.working") : cartSuccess ? t("product.added") : t("product.addToCart")}
        </span>
      </motion.button>

      {/* Secondary */}
      <motion.div whileHover={canAdd?{scale:1.02,y:-2}:{}} whileTap={canAdd?{scale:0.97}:{}}>
        <button
          type="button"
          onClick={onCheckoutNow}
          disabled={!canAdd}
          className="flex items-center gap-2 rounded-full px-8 py-4 text-sm font-black uppercase tracking-wider transition-all disabled:cursor-not-allowed disabled:opacity-40"
          style={{
            background:isDark?"rgba(255,255,255,0.05)":"rgba(15,23,42,0.05)",
            border:`1.5px solid ${tk.borderMid}`,
            color:tk.text1,
            backdropFilter:"blur(16px)",
          }}>
          {t("product.checkout")}
        </button>
      </motion.div>
    </div>
  );
}

/* ═══════════════════════════════════════════════
   REVIEW SECTION
═══════════════════════════════════════════════ */
function ReviewSection({ slug, tk, isDark }) {
  const { t } = useTranslation();
  const [reviews, setReviews] = useState([]);
  const [meta, setMeta]       = useState({ currentPage:1, totalPages:0, hasNext:false, hasPrevious:false, totalElements:0 });
  const [rating, setRating]   = useState("");
  const [sort, setSort]       = useState("newest");
  const [page, setPage]       = useState(1);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true); setMessage("");
    getProductReviews(slug, { rating, sort, page, size: REVIEW_SIZE }, { signal: ctrl.signal })
      .then(payload => {
        setReviews(pageRows(payload).map(normalizeReview));
        setMeta(readPageMeta(payload, { page, size: REVIEW_SIZE }));
      })
      .catch(err => { if (err.name === "AbortError") return; setReviews([]); setMessage(err.message || t("product.reviewLoadFailed")); })
      .finally(() => { if (!ctrl.signal.aborted) setLoading(false); });
    return () => ctrl.abort();
  }, [page, rating, slug, sort, t]);

  const selStyle = {
    background:tk.surface2, border:`1px solid ${tk.border}`,
    color:tk.text1, borderRadius:"999px",
    padding:"8px 16px", fontSize:"0.85rem", fontWeight:700,
    outline:"none",
  };

  return (
    <motion.section
      initial={{opacity:0,y:40}} whileInView={{opacity:1,y:0}}
      viewport={{once:true,margin:"0px 0px -80px 0px"}}
      transition={{duration:0.7,ease:[0.22,1,0.36,1]}}
      className="relative mt-8 overflow-hidden rounded-[28px]"
      style={{
        background:tk.surface1,
        border:`1px solid ${tk.border}`,
        backdropFilter:"blur(28px)",
        boxShadow: isDark?"none":"0 8px 40px rgba(15,23,42,0.08)",
      }}>
      {/* Top line */}
      <div className="absolute left-0 right-0 top-0 h-[1px]"
        style={{ background:`linear-gradient(90deg,transparent,${tk.accent}80,transparent)` }}/>
      <div className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full"
        style={{ background:`radial-gradient(circle,${tk.accentGlow} 0%,transparent 70%)` }}/>

      <div className="p-6 md:p-10">
        {/* Header */}
        <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl"
              style={{ background:`${tk.accent}22`, color:tk.accent }}>
              <MessageSquare size={18}/>
            </div>
            <div>
              <p className="text-[0.6rem] font-black uppercase tracking-[0.2em]" style={{ color:tk.accent }}>
                {t("product.publicReviewsCopy")}
              </p>
              <h2 className="text-2xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                {t("product.reviews")}
                {meta.totalElements > 0 && (
                  <span className="ml-2 text-sm font-bold" style={{ color:tk.text3 }}>
                    ({meta.totalElements})
                  </span>
                )}
              </h2>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <select value={rating} onChange={e => { setRating(e.target.value); setPage(1); }} style={selStyle}>
              <option value="">{t("product.allRatings")}</option>
              {[5,4,3,2,1].map(n => <option key={n} value={n}>{t("product.ratingFilter",{rating:n})}</option>)}
            </select>
            <select value={sort} onChange={e => { setSort(e.target.value); setPage(1); }} style={selStyle}>
              <option value="newest">{t("product.reviewNewest")}</option>
              <option value="oldest">{t("product.reviewOldest")}</option>
              <option value="rating_desc">{t("product.reviewRatingDesc")}</option>
              <option value="rating_asc">{t("product.reviewRatingAsc")}</option>
            </select>
          </div>
        </div>

        {message && (
          <div className="mb-6 rounded-2xl px-5 py-4 text-sm font-semibold"
            style={{ background:"rgba(240,165,0,0.12)", border:"1px solid rgba(240,165,0,0.3)", color:tk.gold }}>
            {message}
          </div>
        )}

        {/* Reviews list */}
        {loading ? (
          <div className="grid gap-3">
            {[...Array(3)].map((_,i) => (
              <div key={i} className="catalog-dynamic-shimmer h-28 rounded-2xl"
                style={{ "--sa":tk.skA,"--sb":tk.skB,"--sc":tk.skC,animationDelay:`${i*120}ms` }}/>
            ))}
          </div>
        ) : reviews.length ? (
          <div className="grid gap-4">
            <AnimatePresence>
              {reviews.map((review,i) => (
                <motion.div key={review.id}
                  initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-12}}
                  transition={{duration:0.4,delay:i*0.06,ease:[0.22,1,0.36,1]}}>
                  <ReviewCard review={review} tk={tk} isDark={isDark}/>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        ) : (
          <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:0.4}}
            className="rounded-2xl px-6 py-14 text-center"
            style={{ background:tk.surface2, border:`1px dashed ${tk.border}` }}>
            <MessageSquare size={36} style={{ color:tk.text3, margin:"0 auto 12px" }}/>
            <h3 className="text-xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
              {t("product.noReviews")}
            </h3>
            <p className="mt-2 text-sm" style={{ color:tk.text3 }}>{t("product.noReviewsCopy")}</p>
          </motion.div>
        )}

        {/* Pagination */}
        {meta.totalPages > 1 && (
          <div className="mt-8 flex flex-wrap justify-center gap-2">
            <button
              disabled={!meta.hasPrevious || loading}
              onClick={() => setPage(p => Math.max(1,p-1))}
              className="rounded-full px-5 py-2.5 text-sm font-bold transition-all disabled:opacity-40"
              style={{ background:tk.surface2, border:`1px solid ${tk.border}`, color:tk.text1 }}>
              {t("catalog.previousPage")}
            </button>
            <span className="flex items-center rounded-full px-5 py-2.5 text-sm font-bold"
              style={{ background:isDark?"rgba(79,110,247,0.15)":"rgba(37,99,235,0.10)", color:tk.accent }}>
              {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })}
            </span>
            <button
              disabled={!meta.hasNext || loading}
              onClick={() => setPage(p => p+1)}
              className="rounded-full px-5 py-2.5 text-sm font-bold transition-all disabled:opacity-40"
              style={{ background:tk.surface2, border:`1px solid ${tk.border}`, color:tk.text1 }}>
              {t("catalog.nextPage")}
            </button>
          </div>
        )}
      </div>
    </motion.section>
  );
}

/* ── Review card ─────────────────────────────── */
function ReviewCard({ review, tk, isDark }) {
  const { t, i18n } = useTranslation();
  const [hov, setHov] = useState(false);
  const locale = String(i18n.language || "vi").startsWith("en") ? "en-US" : "vi-VN";

  // Gradient avatar color from username
  const avatarGrad = ["linear-gradient(135deg,#4f6ef7,#a78bfa)", "linear-gradient(135deg,#10d98a,#38bdf8)", "linear-gradient(135deg,#f0a500,#fb923c)", "linear-gradient(135deg,#ec4899,#a78bfa)"];
  const gradIdx = (review.username?.charCodeAt(0) || 0) % avatarGrad.length;

  return (
    <article
      className="relative overflow-hidden rounded-2xl p-5 transition-all duration-300"
      style={{
        background: hov?(isDark?"rgba(18,24,64,0.95)":"rgba(255,251,244,1)"):tk.surface2,
        border:`1px solid ${hov?tk.borderMid:tk.border}`,
        backdropFilter:"blur(20px)",
        boxShadow: hov?(isDark?"0 12px 40px rgba(79,110,247,0.1)":"0 12px 40px rgba(15,23,42,0.08)"):"none",
        transform: hov?"translateY(-2px)":"none",
      }}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}>
      {/* Left accent bar on hover */}
      <div className="absolute inset-y-0 left-0 w-[3px] rounded-l-2xl transition-all duration-300"
        style={{ background: hov?"linear-gradient(to bottom,#4f6ef7,#a78bfa)":"transparent" }}/>

      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div className="flex items-center gap-3">
          {/* Avatar */}
          <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-black text-white"
            style={{ background:avatarGrad[gradIdx] }}>
            {(review.username || "?")[0].toUpperCase()}
          </div>
          <div>
            <strong className="block text-sm font-black" style={{ color:tk.text1 }}>{review.username}</strong>
            <div className="mt-0.5 flex items-center gap-0.5">
              {[1,2,3,4,5].map(n => (
                <Star key={n} size={11} fill={n<=review.rating?"currentColor":"none"}
                  style={{ color:tk.gold }}/>
              ))}
            </div>
          </div>
        </div>
        {review.createdAt && (
          <time className="text-xs font-semibold" style={{ color:tk.text3 }}>
            {new Intl.DateTimeFormat(locale).format(new Date(review.createdAt))}
          </time>
        )}
      </div>

      {review.comment && (
        <p className="mt-4 text-sm leading-7" style={{ color:tk.text2 }}>{review.comment}</p>
      )}

      {review.images?.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {review.images.map(img => (
            <img key={img.id || img.imageUrl} src={img.imageUrl}
              alt={review.comment || review.username}
              className="h-20 w-20 rounded-xl object-cover transition-transform hover:scale-105"
              style={{ border:`1px solid ${tk.border}` }}/>
          ))}
        </div>
      )}

      {review.adminReply && (
        <div className="mt-4 rounded-xl px-4 py-3 text-sm"
          style={{ background:isDark?"rgba(79,110,247,0.10)":"rgba(37,99,235,0.07)", border:`1px solid ${isDark?"rgba(79,110,247,0.25)":"rgba(37,99,235,0.2)"}` }}>
          <strong className="mb-1 block text-xs font-black uppercase tracking-wider" style={{ color:tk.accent }}>
            {t("product.adminReply")}
          </strong>
          <span style={{ color:tk.text2 }}>{review.adminReply}</span>
        </div>
      )}
    </article>
  );
}

/* ═══════════════════════════════════════════════
   SKELETON
═══════════════════════════════════════════════ */
function ProductSkeleton({ tk }) {
  const shimmer = { "--sa":tk.skA, "--sb":tk.skB, "--sc":tk.skC };
  return (
    <div className="relative w-full overflow-hidden" style={{ background:tk.pageBg, minHeight:"100vh" }}>
      <div className="pointer-events-none absolute -right-40 -top-40 h-[560px] w-[560px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb1} 0%,transparent 70%)`, filter:"blur(80px)" }}/>
      <div className="mx-auto max-w-[1400px] px-4 pb-28 pt-20 md:px-8">
        <div className="catalog-dynamic-shimmer mb-8 h-16 rounded-[22px]" style={shimmer}/>
        <div className="overflow-hidden rounded-[32px] p-8 md:p-10"
          style={{ background:tk.surface1, border:`1px solid ${tk.border}` }}>
          <div className="grid gap-12 lg:grid-cols-[460px_1fr]">
            <div className="grid gap-4">
              <div className="catalog-dynamic-shimmer rounded-[20px]" style={{ ...shimmer, aspectRatio:"2/3" }}/>
              <div className="flex gap-2">
                {[...Array(4)].map((_,i) => (
                  <div key={i} className="catalog-dynamic-shimmer h-20 w-14 flex-shrink-0 rounded-xl" style={shimmer}/>
                ))}
              </div>
            </div>
            <div className="flex flex-col gap-6 py-4">
              {[["w-24","h-6"],["w-4/5","h-10"],["w-2/3","h-8"],["w-1/2","h-5"],["w-full","h-12"],["w-full","h-24"],["w-full","h-32"],["w-52","h-12"]].map(([w,h],i) => (
                <div key={i} className={`catalog-dynamic-shimmer ${h} ${w} rounded-xl`} style={{...shimmer,animationDelay:`${i*60}ms`}}/>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Utils ──────────────────────────────────────── */
function buildGallery(book) {
  if (!book) return [];
  const rows = [
    book.thumbnailUrl ? { url:book.thumbnailUrl, alt:book.title } : null,
    ...(book.media||[]).filter(m => m.active!==false && (!m.mediaType||m.mediaType==="IMAGE"))
      .sort((a,b) => Number(a.sortOrder||0)-Number(b.sortOrder||0))
      .map(m => ({ url:m.mediaUrl, alt:m.altText||book.title })),
    ...(book.variations||[]).filter(v => v.imageUrl).map(v => ({ url:v.imageUrl, alt:v.size||book.title })),
    book.image ? { url:book.image, alt:book.title } : null,
  ].filter(item => item?.url);
  const seen = new Set();
  return rows.filter(item => { if(seen.has(item.url)) return false; seen.add(item.url); return true; });
}

function pickDefaultVariation(variations = []) {
  return variations.find(v => v.active!==false && Number(v.stockQuantity||0)>0)
    || variations.find(v => v.active!==false)
    || variations[0];
}

function readCartItems(cart) {
  const rows = Array.isArray(cart?.items)
    ? cart.items
    : (cart?.cartItemId || cart?.id ? [cart] : []);
  return rows.map(normalizeCartItem);
}
