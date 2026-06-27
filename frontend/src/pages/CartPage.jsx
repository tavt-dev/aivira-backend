import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  AlertCircle, ArrowRight, BookOpen, CheckCircle2,
  Minus, Plus, ShoppingBag, ShoppingCart, Trash2,
  X, Zap, Tag, Sparkles,
} from "lucide-react";

import { clearCart, getCart, removeCartItem, updateCartItem } from "../api/cartApi.js";
import {
  getCheckoutCartItemIds, getStoredCheckoutCartItemIds,
  isCartItemCheckoutAvailable, removeCheckoutCartItemIds, saveCheckoutCartItemIds,
} from "../utils/checkoutSelection.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";
import { getTheme } from "../utils/theme.js";

/* ── Token system ────────────────────────────── */
function tokens(isDark) {
  if (isDark) return {
    pageBg:    "radial-gradient(ellipse at 65% 0%,rgba(30,24,80,0.9) 0%,#07091a 55%)",
    surface1:  "rgba(10,15,42,0.95)",
    surface2:  "rgba(16,22,58,0.88)",
    surface3:  "rgba(22,28,70,0.75)",
    heroBg:    "linear-gradient(135deg,rgba(12,17,48,0.98) 0%,rgba(8,12,35,0.99) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
    border:    "rgba(255,255,255,0.075)",
    borderMid: "rgba(255,255,255,0.14)",
    text1:     "#e8eeff",
    text2:     "#8892b0",
    text3:     "#4a5578",
    accent:    "#4f6ef7",
    gold:      "#f0a500",
    emerald:   "#10d98a",
    red:       "#ef4444",
    orb1:      "rgba(79,110,247,0.25)",
    orb2:      "rgba(240,165,0,0.16)",
    orb3:      "rgba(167,139,250,0.15)",
    skA:       "rgba(255,255,255,0.03)",
    skB:       "rgba(79,110,247,0.10)",
    skC:       "rgba(167,139,250,0.08)",
    inputBg:   "rgba(255,255,255,0.05)",
    unavailBg: "rgba(239,68,68,0.08)",
    unavailBorder:"rgba(239,68,68,0.25)",
    checkboxBg:"rgba(79,110,247,0.15)",
  };
  return {
    pageBg:    "radial-gradient(ellipse at 65% 0%,rgba(210,220,255,0.35) 0%,#f0ede8 55%)",
    surface1:  "rgba(255,252,246,0.97)",
    surface2:  "rgba(250,247,241,0.92)",
    surface3:  "rgba(244,241,234,0.88)",
    heroBg:    "linear-gradient(135deg,rgba(15,23,42,0.97) 0%,rgba(22,30,58,0.98) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#6d8fff 40%,#c4b5fd 70%,transparent)",
    border:    "rgba(15,23,42,0.10)",
    borderMid: "rgba(15,23,42,0.18)",
    text1:     "#0f172a",
    text2:     "#334155",
    text3:     "#94a3b8",
    accent:    "#1d4ed8",
    gold:      "#b45309",
    emerald:   "#047857",
    red:       "#b91c1c",
    orb1:      "rgba(79,110,247,0.10)",
    orb2:      "rgba(240,165,0,0.08)",
    orb3:      "rgba(167,139,250,0.08)",
    skA:       "rgba(15,23,42,0.05)",
    skB:       "rgba(37,99,235,0.07)",
    skC:       "rgba(139,92,246,0.05)",
    inputBg:   "rgba(15,23,42,0.06)",
    unavailBg: "rgba(185,28,28,0.05)",
    unavailBorder:"rgba(185,28,28,0.18)",
    checkboxBg:"rgba(29,78,216,0.10)",
  };
}

function useTheme() {
  const [isDark, setIsDark] = useState(() => getTheme() === "dark");
  useEffect(() => {
    const sync = () => setIsDark(getTheme() === "dark");
    window.addEventListener("aivira-theme", sync);
    return () => window.removeEventListener("aivira-theme", sync);
  }, []);
  return isDark;
}

/* ══════════════════════════════════════════════
   CART PAGE
══════════════════════════════════════════════ */
export default function CartPage({ onAuth }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isDark = useTheme();
  const tk = tokens(isDark);

  const [items, setItems]         = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [message, setMessage]     = useState("");
  const [msgOk, setMsgOk]         = useState(false);
  const [loading, setLoading]     = useState(true);
  const [removingId, setRemovingId] = useState(null);

  useEffect(() => {
    if (!getAccessToken()) { setLoading(false); return; }
    setLoading(true);
    getCart()
      .then(cart => {
        const norm = (cart?.items || []).map(normalizeCartItem);
        const stored = getStoredCheckoutCartItemIds();
        const next = stored.length
          ? getCheckoutCartItemIds(norm)
          : norm.filter(isCartItemCheckoutAvailable).map(i => Number(i.cartItemId)).filter(Boolean);
        setItems(norm); setSelectedIds(next); saveCheckoutCartItemIds(next);
      })
      .catch(err => { setMsgOk(false); setMessage(err.message || t("cart.loadFailed")); })
      .finally(() => setLoading(false));
  }, [t]);

  function syncSelection(nextIds) { setSelectedIds(nextIds); saveCheckoutCartItemIds(nextIds); }

  function toggleItem(item) {
    if (!isCartItemCheckoutAvailable(item)) return;
    const id = Number(item.cartItemId);
    syncSelection(selectedIds.includes(id) ? selectedIds.filter(x => x !== id) : [...selectedIds, id]);
  }

  function toggleAll() {
    const avail = items.filter(isCartItemCheckoutAvailable).map(i => Number(i.cartItemId)).filter(Boolean);
    const allSel = avail.length > 0 && avail.every(id => selectedIds.includes(id));
    syncSelection(allSel ? [] : avail);
  }

  function updateQuantity(item, qty) {
    if (!isCartItemCheckoutAvailable(item)) return;
    const max = item.stockQuantity == null ? Number.MAX_SAFE_INTEGER : Math.max(1, Number(item.stockQuantity));
    const next = Math.min(max, Math.max(1, Number(qty) || 1));
    setItems(cur => cur.map(c => c.cartItemId === item.cartItemId ? { ...c, quantity: next } : c));
    updateCartItem(item.cartItemId, { quantity: next })
      .then(cart => {
        const norm = (cart?.items || []).map(normalizeCartItem);
        setItems(norm); syncSelection(getCheckoutCartItemIds(norm));
        window.dispatchEvent(new Event("aivira-cart"));
      })
      .catch(err => { setMsgOk(false); setMessage(err.message || t("cart.updateFailed")); });
  }

  function removeItem(item) {
    setRemovingId(item.cartItemId);
    const id = Number(item.cartItemId);
    setTimeout(() => {
      setItems(cur => cur.filter(c => c.cartItemId !== item.cartItemId));
      syncSelection(removeCheckoutCartItemIds([id]));
      setRemovingId(null);
    }, 350);
    removeCartItem(item.cartItemId)
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch(err => { setMsgOk(false); setMessage(err.message || t("cart.removeFailed")); });
  }

  function clearAll() {
    setItems([]); syncSelection([]);
    clearCart()
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch(err => { setMsgOk(false); setMessage(err.message || t("cart.clearFailed")); });
  }

  function goToCheckout() {
    if (selectedIds.length === 0) {
      setMsgOk(false); setMessage(t("cart.selectAtLeastOne")); return;
    }
    saveCheckoutCartItemIds(selectedIds);
    navigate("/checkout");
  }

  const loggedIn      = Boolean(getAccessToken());
  const availableItems = items.filter(isCartItemCheckoutAvailable);
  const allSelected    = availableItems.length > 0 && availableItems.every(i => selectedIds.includes(Number(i.cartItemId)));
  const selectedItems  = useMemo(() => items.filter(i => selectedIds.includes(Number(i.cartItemId))), [items, selectedIds]);
  const total          = cartTotal(selectedItems);

  return (
    <div className="relative w-full overflow-hidden" style={{ background: tk.pageBg, minHeight:"100vh" }}>
      {/* Dot grid */}
      <div className="pointer-events-none absolute inset-0" style={{
        backgroundImage:`radial-gradient(circle,${isDark?"rgba(79,110,247,0.09)":"rgba(37,99,235,0.05)"} 1px,transparent 1px)`,
        backgroundSize:"48px 48px",
        maskImage:"radial-gradient(ellipse at 50% 0%,black 0%,transparent 65%)",
      }}/>
      {/* Orbs */}
      <div className="pointer-events-none absolute -right-40 -top-40 h-[500px] w-[500px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb1} 0%,transparent 70%)`, filter:"blur(80px)" }}/>
      <div className="pointer-events-none absolute -left-32 top-[40%] h-[400px] w-[400px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb2} 0%,transparent 70%)`, filter:"blur(90px)" }}/>
      <div className="pointer-events-none absolute bottom-[15%] right-[20%] h-[320px] w-[320px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb3} 0%,transparent 70%)`, filter:"blur(80px)" }}/>

      <div className="relative z-10 mx-auto max-w-7xl px-4 pb-28 pt-20 md:px-8">

        {/* Hero bar */}
        <CartHeroBar tk={tk} isDark={isDark} itemCount={items.length} t={t}/>

        {/* Toast */}
        <AnimatePresence>
          {message && (
            <motion.div key="toast"
              initial={{opacity:0,y:-12,scale:0.96}} animate={{opacity:1,y:0,scale:1}} exit={{opacity:0,y:-12}}
              transition={{duration:0.3,ease:[0.22,1,0.36,1]}}
              className="mb-6 flex items-center gap-3 rounded-2xl px-5 py-4"
              style={{
                background: msgOk?"rgba(16,217,138,0.12)":"rgba(239,68,68,0.10)",
                border:`1px solid ${msgOk?"rgba(16,217,138,0.4)":"rgba(239,68,68,0.4)"}`,
                backdropFilter:"blur(20px)",
              }}>
              {msgOk
                ? <CheckCircle2 size={15} color={tk.emerald}/>
                : <AlertCircle size={15} color={tk.red}/>}
              <span className="text-sm font-bold" style={{ color: msgOk?tk.emerald:tk.red }}>{message}</span>
              <button type="button" onClick={() => setMessage("")} className="ml-auto opacity-60 hover:opacity-100">
                <X size={13} color={msgOk?tk.emerald:tk.red}/>
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Not logged in */}
        {!loggedIn && (
          <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.5}}
            className="flex flex-col items-center justify-center gap-6 rounded-[32px] px-8 py-24 text-center"
            style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)" }}>
            <div className="flex h-20 w-20 items-center justify-center rounded-[22px]"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 16px 40px rgba(79,110,247,0.45)" }}>
              <ShoppingCart size={32} color="#fff"/>
            </div>
            <div>
              <h2 className="text-2xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                {t("cart.loginRequired")}
              </h2>
              <p className="mt-2 text-sm" style={{ color:tk.text2 }}>
                {t("cart.loginToContinue","Đăng nhập để xem và quản lý giỏ hàng")}
              </p>
            </div>
            <motion.button whileHover={{scale:1.04,y:-2}} whileTap={{scale:0.97}}
              type="button" onClick={onAuth}
              className="rounded-full px-8 py-4 text-sm font-black uppercase tracking-wider text-white"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 28px rgba(79,110,247,0.45)" }}>
              {t("common.login")}
            </motion.button>
          </motion.div>
        )}

        {/* Loading */}
        {loggedIn && loading && <CartSkeleton tk={tk}/>}

        {/* Empty */}
        {loggedIn && !loading && items.length === 0 && (
          <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.5}}
            className="flex flex-col items-center justify-center gap-6 rounded-[32px] px-8 py-24 text-center"
            style={{ background:tk.surface1, border:`1px dashed ${tk.border}`, backdropFilter:"blur(20px)" }}>
            <div className="flex h-20 w-20 items-center justify-center rounded-[22px]"
              style={{ background:isDark?"rgba(79,110,247,0.12)":"rgba(29,78,216,0.08)" }}>
              <ShoppingBag size={32} style={{ color:tk.accent }}/>
            </div>
            <div>
              <h2 className="text-2xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                {t("cart.empty")}
              </h2>
              <p className="mt-2 text-sm" style={{ color:tk.text3 }}>
                {t("cart.emptySub","Hãy khám phá kho sách tuyển chọn của chúng tôi")}
              </p>
            </div>
            <motion.button whileHover={{scale:1.04,y:-2}} whileTap={{scale:0.97}}
              type="button" onClick={() => navigate("/category/all")}
              className="flex items-center gap-2 rounded-full px-8 py-4 text-sm font-black uppercase tracking-wider text-white"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 28px rgba(79,110,247,0.45)" }}>
              <BookOpen size={16}/> {t("cart.browse")} <ArrowRight size={14}/>
            </motion.button>
          </motion.div>
        )}

        {/* Cart content */}
        {loggedIn && !loading && items.length > 0 && (
          <div className="grid gap-6 lg:grid-cols-[1fr_380px] lg:items-start">

            {/* ── Left: items list ── */}
            <div className="grid gap-4">
              {/* Select all bar */}
              <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}}
                transition={{duration:0.45,ease:[0.22,1,0.36,1]}}
                className="flex items-center justify-between overflow-hidden rounded-[18px] px-5 py-4"
                style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(20px)" }}>
                <label className="flex cursor-pointer items-center gap-3">
                  <PremiumCheckbox checked={allSelected} onChange={toggleAll} tk={tk} isDark={isDark}/>
                  <span className="text-sm font-bold" style={{ color:tk.text1 }}>{t("cart.selectAll")}</span>
                  <span className="rounded-full px-2.5 py-1 text-[0.65rem] font-black uppercase tracking-wider"
                    style={{ background:isDark?"rgba(79,110,247,0.15)":"rgba(29,78,216,0.10)", color:tk.accent }}>
                    {selectedIds.length}/{availableItems.length}
                  </span>
                </label>
                {items.length > 0 && (
                  <motion.button whileHover={{scale:1.03}} whileTap={{scale:0.97}}
                    type="button" onClick={clearAll}
                    className="flex items-center gap-1.5 rounded-full px-4 py-2 text-xs font-bold"
                    style={{ background:"rgba(239,68,68,0.10)", border:"1px solid rgba(239,68,68,0.25)", color:tk.red }}>
                    <Trash2 size={12}/> {t("cart.clear")}
                  </motion.button>
                )}
              </motion.div>

              {/* Items */}
              <AnimatePresence>
                {items.map((item, i) => {
                  const itemId  = Number(item.cartItemId);
                  const avail   = isCartItemCheckoutAvailable(item);
                  const maxQty  = item.stockQuantity == null ? null : Math.max(0, Number(item.stockQuantity));
                  const checked = selectedIds.includes(itemId);
                  const removing = removingId === item.cartItemId;

                  return (
                    <motion.div key={item.cartItemId}
                      layout
                      initial={{opacity:0,y:20}} animate={{opacity: removing?0:1, x: removing?60:0, y:0}}
                      exit={{opacity:0,x:60,scale:0.96}}
                      transition={{duration:0.38,delay:i*0.04,ease:[0.22,1,0.36,1]}}
                      className="relative overflow-hidden rounded-[22px]"
                      style={{
                        background: avail?tk.surface1:tk.unavailBg,
                        border:`1px solid ${avail?(checked?tk.borderMid:tk.border):tk.unavailBorder}`,
                        backdropFilter:"blur(24px)",
                        boxShadow: checked?(isDark?"0 8px 32px rgba(79,110,247,0.18)":"0 8px 28px rgba(29,78,216,0.10)"):"none",
                        transition:"border-color 0.3s, box-shadow 0.3s",
                      }}>
                      {/* Selected glow top line */}
                      <div className="absolute left-0 right-0 top-0 h-[1.5px] transition-all duration-300"
                        style={{ background: checked?"linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)":"transparent" }}/>
                      {/* Left accent */}
                      <div className="absolute inset-y-0 left-0 w-[3px] rounded-l-[22px] transition-all duration-300"
                        style={{ background: checked?"linear-gradient(to bottom,#4f6ef7,#a78bfa80)":avail?"transparent":"rgba(239,68,68,0.5)" }}/>

                      <div className="grid gap-4 p-5 md:grid-cols-[28px_88px_1fr_auto_auto] md:items-center">
                        {/* Checkbox */}
                        <PremiumCheckbox
                          checked={checked} disabled={!avail} onChange={() => toggleItem(item)}
                          tk={tk} isDark={isDark} aria-label={t("cart.selectItem")}/>

                        {/* Book cover */}
                        <Link to={`/product/${item.slug}`} className="block flex-shrink-0">
                          <motion.div whileHover={{scale:1.04}} transition={{duration:0.25}}
                            className="overflow-hidden rounded-xl"
                            style={{ aspectRatio:"2/3", width:"80px", boxShadow:isDark?"0 8px 24px rgba(0,0,0,0.45)":"0 6px 20px rgba(15,23,42,0.15)" }}>
                            {item.image
                              ? <img src={item.image} alt={item.title} className="h-full w-full object-cover"/>
                              : <div className="flex h-full w-full items-center justify-center"
                                  style={{ background:isDark?"rgba(79,110,247,0.12)":"rgba(29,78,216,0.08)" }}>
                                  <BookOpen size={24} style={{ color:tk.accent }}/>
                                </div>
                            }
                          </motion.div>
                        </Link>

                        {/* Info */}
                        <div className="min-w-0">
                          <Link to={`/product/${item.slug}`}
                            className="block truncate text-base font-black leading-tight transition-colors hover:opacity-80"
                            style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                            {item.title}
                          </Link>
                          {item.author && (
                            <p className="mt-1 text-xs font-medium italic" style={{ color:tk.text3 }}>{item.author}</p>
                          )}
                          <div className="mt-2 flex flex-wrap gap-1.5">
                            {[item.size, item.color, item.sku].filter(Boolean).map((v,j) => (
                              <span key={j} className="rounded-full px-2.5 py-1 text-[0.6rem] font-bold uppercase tracking-wider"
                                style={{ background:tk.surface2, color:tk.text3 }}>{v}</span>
                            ))}
                          </div>
                          <p className="mt-2 text-[0.7rem] font-black uppercase tracking-wider"
                            style={{ color: avail?tk.emerald:tk.red }}>
                            {avail
                              ? maxQty == null ? t("cart.available") : t("cart.stockLeft",{count:maxQty})
                              : t("cart.unavailable")}
                          </p>
                        </div>

                        {/* Price + Qty */}
                        <div className="flex flex-col gap-3">
                          <p className="text-sm font-bold" style={{ color:tk.text2 }}>{formatVND(item.price)}</p>
                          <QuantityStepper
                            value={item.quantity} max={maxQty}
                            disabled={!avail}
                            onDecrease={() => updateQuantity(item, item.quantity-1)}
                            onIncrease={() => updateQuantity(item, item.quantity+1)}
                            tk={tk} isDark={isDark}/>
                        </div>

                        {/* Line total + remove */}
                        <div className="flex flex-col items-end gap-3">
                          <motion.strong
                            key={item.quantity}
                            initial={{scale:1.15,color:"#4f6ef7"}} animate={{scale:1,color:tk.text1}}
                            transition={{duration:0.35}}
                            className="text-lg font-black"
                            style={{ fontFamily:"var(--f-serif)" }}>
                            {formatVND(item.price * item.quantity)}
                          </motion.strong>
                          <motion.button whileHover={{scale:1.08}} whileTap={{scale:0.92}}
                            type="button" onClick={() => removeItem(item)}
                            className="flex h-9 w-9 items-center justify-center rounded-full transition-all"
                            style={{ background:"rgba(239,68,68,0.10)", border:"1px solid rgba(239,68,68,0.25)", color:tk.red }}>
                            <Trash2 size={14}/>
                          </motion.button>
                        </div>
                      </div>
                    </motion.div>
                  );
                })}
              </AnimatePresence>
            </div>

            {/* ── Right: Summary sidebar ── */}
            <motion.aside
              initial={{opacity:0,x:32}} animate={{opacity:1,x:0}}
              transition={{duration:0.55,delay:0.15,ease:[0.22,1,0.36,1]}}
              className="sticky top-24 overflow-hidden rounded-[28px]"
              style={{
                background:tk.surface1, border:`1px solid ${tk.border}`,
                backdropFilter:"blur(28px)",
                boxShadow: isDark
                  ? "0 40px 100px rgba(0,0,0,0.55), inset 0 1px 0 rgba(255,255,255,0.05)"
                  : "0 20px 80px rgba(15,23,42,0.12), inset 0 1px 0 rgba(255,255,255,0.9)",
              }}>
              {/* Sidebar top accent */}
              <div className="absolute left-0 right-0 top-0 h-[1.5px]"
                style={{ background:tk.heroLine }}/>

              <div className="p-7">
                {/* Title */}
                <div className="mb-6 flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl"
                    style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 6px 20px rgba(79,110,247,0.4)" }}>
                    <ShoppingCart size={18} color="#fff"/>
                  </div>
                  <h2 className="text-2xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                    {t("cart.summary")}
                  </h2>
                </div>

                {/* Summary rows */}
                <div className="grid gap-4">
                  <SummaryRow icon={Tag} label={t("cart.selectedItems")} value={selectedIds.length} tk={tk}/>
                  <SummaryRow icon={ShoppingBag} label={t("cart.availableItems")} value={availableItems.length} tk={tk}/>
                </div>

                {/* Divider */}
                <div className="my-6 h-px" style={{ background:tk.border }}/>

                {/* Total */}
                <div className="flex items-end justify-between gap-4">
                  <span className="text-sm font-bold" style={{ color:tk.text2 }}>{t("common.total")}</span>
                  <motion.strong
                    key={total}
                    initial={{scale:1.1}} animate={{scale:1}}
                    transition={{type:"spring",stiffness:400,damping:20}}
                    className="text-3xl font-black"
                    style={{
                      fontFamily:"var(--f-serif)",
                      background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)",
                      WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent",
                    }}>
                    {formatVND(total)}
                  </motion.strong>
                </div>

                {/* Promo hint */}
                <div className="mt-4 flex items-center gap-2 rounded-xl px-4 py-3"
                  style={{ background:isDark?"rgba(240,165,0,0.10)":"rgba(180,83,9,0.07)", border:`1px solid ${isDark?"rgba(240,165,0,0.25)":"rgba(180,83,9,0.18)"}` }}>
                  <Sparkles size={13} style={{ color:tk.gold, flexShrink:0 }}/>
                  <p className="text-xs font-semibold" style={{ color:tk.gold }}>
                    {t("cart.freeShippingHint","Miễn phí vận chuyển cho đơn từ 300.000₫")}
                  </p>
                </div>

                {/* CTA checkout */}
                <motion.button
                  whileHover={{ scale:1.02, y:-2 }} whileTap={{ scale:0.97 }}
                  type="button" onClick={goToCheckout}
                  disabled={selectedIds.length === 0}
                  className="relative mt-6 w-full overflow-hidden rounded-full py-4 text-sm font-black uppercase tracking-widest text-white disabled:cursor-not-allowed disabled:opacity-50"
                  style={{
                    background:"linear-gradient(135deg,#2a3ecc 0%,#4f6ef7 50%,#7c3aed 100%)",
                    boxShadow: selectedIds.length>0 ? "0 10px 40px rgba(79,110,247,0.5)" : "none",
                  }}>
                  {/* Shine sweep */}
                  <span className="pointer-events-none absolute inset-0 cart-checkout-shine"/>
                  <span className="relative flex items-center justify-center gap-2">
                    <Zap size={16}/> {t("cart.checkout")} <ArrowRight size={15}/>
                  </span>
                </motion.button>

                {/* Browse link */}
                <Link to="/category/all"
                  className="mt-4 flex items-center justify-center gap-2 rounded-full py-3 text-sm font-bold transition-all hover:opacity-70"
                  style={{ color:tk.text2 }}>
                  <BookOpen size={14}/> {t("cart.browse")}
                </Link>
              </div>
            </motion.aside>
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Hero bar ────────────────────────────────── */
function CartHeroBar({ tk, itemCount, t }) {
  return (
    <motion.div
      initial={{opacity:0,y:-20}} animate={{opacity:1,y:0}}
      transition={{duration:0.55,ease:[0.22,1,0.36,1]}}
      className="relative mb-8 overflow-hidden rounded-[22px] px-8 py-7"
      style={{
        background:tk.heroBg, border:"1px solid rgba(255,255,255,0.07)",
        boxShadow:"0 24px 60px rgba(0,0,0,0.4)",
      }}>
      <div className="absolute left-0 right-0 top-0 h-[1.5px]" style={{ background:tk.heroLine }}/>
      <div className="pointer-events-none absolute -right-8 -top-8 h-36 w-36 rounded-full"
        style={{ background:"radial-gradient(circle,rgba(79,110,247,0.18) 0%,transparent 70%)" }}/>
      <div className="relative flex items-center gap-4">
        <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl"
          style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 24px rgba(79,110,247,0.5)" }}>
          <ShoppingCart size={22} color="#fff"/>
        </div>
        <div className="flex-1">
          <p className="text-[0.6rem] font-black uppercase tracking-[0.2em]" style={{ color:"#4a5578" }}>
            {t("cart.eyebrow","Aivira Bookstore")}
          </p>
          <h1 className="text-2xl font-black md:text-3xl" style={{ color:"#e8eeff", fontFamily:"var(--f-serif)" }}>
            {t("cart.title","Giỏ hàng")}
          </h1>
        </div>
        {itemCount > 0 && (
          <motion.div
            initial={{scale:0}} animate={{scale:1}}
            transition={{type:"spring",stiffness:400,damping:18,delay:0.3}}
            className="flex h-10 w-10 items-center justify-center rounded-full text-sm font-black text-white"
            style={{ background:"linear-gradient(135deg,#4f6ef7,#7c3aed)", boxShadow:"0 4px 16px rgba(79,110,247,0.5)" }}>
            {itemCount}
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}

/* ── Premium checkbox ────────────────────────── */
function PremiumCheckbox({ checked, onChange, disabled, tk, isDark }) {
  return (
    <motion.button
      type="button" role="checkbox" aria-checked={checked}
      onClick={!disabled ? onChange : undefined}
      whileHover={!disabled?{scale:1.12}:{}}
      whileTap={!disabled?{scale:0.9}:{}}
      className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-md transition-all"
      style={{
        background: checked
          ? "linear-gradient(135deg,#2a3ecc,#4f6ef7)"
          : isDark?"rgba(255,255,255,0.06)":"rgba(15,23,42,0.06)",
        border:`1.5px solid ${checked?"transparent":tk.border}`,
        boxShadow: checked?"0 2px 12px rgba(79,110,247,0.45)":"none",
        opacity: disabled ? 0.35 : 1,
        cursor: disabled ? "not-allowed" : "pointer",
      }}>
      <AnimatePresence>
        {checked && (
          <motion.div initial={{scale:0,opacity:0}} animate={{scale:1,opacity:1}} exit={{scale:0,opacity:0}}
            transition={{duration:0.15,ease:[0.22,1,0.36,1]}}>
            <CheckCircle2 size={13} color="#fff" strokeWidth={3}/>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.button>
  );
}

/* ── Quantity stepper ────────────────────────── */
function QuantityStepper({ value, max, disabled, onDecrease, onIncrease, tk, isDark }) {
  return (
    <div className="flex items-center overflow-hidden rounded-full"
      style={{ background:isDark?"rgba(255,255,255,0.05)":"rgba(15,23,42,0.06)", border:`1px solid ${tk.border}` }}>
      <motion.button type="button"
        whileHover={!disabled&&value>1?{backgroundColor:isDark?"rgba(255,255,255,0.08)":"rgba(15,23,42,0.09)"}:{}}
        whileTap={!disabled&&value>1?{scale:0.85}:{}}
        disabled={disabled||value<=1}
        onClick={onDecrease}
        className="px-3 py-2 text-sm font-black transition-colors disabled:opacity-30"
        style={{ color:tk.text1 }}>
        <Minus size={12}/>
      </motion.button>
      <motion.span key={value} initial={{scale:1.2}} animate={{scale:1}} transition={{duration:0.2}}
        className="min-w-[2.5rem] text-center text-sm font-black tabular-nums"
        style={{ color:tk.text1 }}>
        {value}
      </motion.span>
      <motion.button type="button"
        whileHover={!disabled&&(max==null||value<max)?{backgroundColor:isDark?"rgba(255,255,255,0.08)":"rgba(15,23,42,0.09)"}:{}}
        whileTap={!disabled&&(max==null||value<max)?{scale:0.85}:{}}
        disabled={disabled||(max!=null&&value>=max)}
        onClick={onIncrease}
        className="px-3 py-2 text-sm font-black transition-colors disabled:opacity-30"
        style={{ color:tk.text1 }}>
        <Plus size={12}/>
      </motion.button>
    </div>
  );
}

/* ── Summary row ─────────────────────────────── */
function SummaryRow({ icon:Icon, label, value, tk }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex items-center gap-2">
        <Icon size={13} style={{ color:tk.accent }}/>
        <span className="text-sm" style={{ color:tk.text2 }}>{label}</span>
      </div>
      <span className="text-sm font-bold" style={{ color:tk.text1 }}>{value}</span>
    </div>
  );
}

/* ── Skeleton ────────────────────────────────── */
function CartSkeleton({ tk }) {
  const sh = { "--sa":tk.skA,"--sb":tk.skB,"--sc":tk.skC };
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
      <div className="grid gap-4">
        {[...Array(3)].map((_,i) => (
          <div key={i} className="overflow-hidden rounded-[22px] p-5"
            style={{ background:tk.surface1, border:`1px solid ${tk.border}` }}>
            <div className="grid grid-cols-[28px_88px_1fr_auto_auto] items-center gap-4">
              <div className="catalog-dynamic-shimmer h-5 w-5 rounded-md" style={{...sh,animationDelay:`${i*80}ms`}}/>
              <div className="catalog-dynamic-shimmer rounded-xl" style={{...sh,height:"120px",animationDelay:`${i*80+30}ms`}}/>
              <div className="grid gap-2">
                <div className="catalog-dynamic-shimmer h-5 rounded-lg" style={{...sh,width:"70%",animationDelay:`${i*80+50}ms`}}/>
                <div className="catalog-dynamic-shimmer h-3 rounded-lg" style={{...sh,width:"40%",animationDelay:`${i*80+70}ms`}}/>
                <div className="catalog-dynamic-shimmer h-3 rounded-full" style={{...sh,width:"25%",animationDelay:`${i*80+90}ms`}}/>
              </div>
              <div className="catalog-dynamic-shimmer h-8 w-24 rounded-full" style={{...sh,animationDelay:`${i*80+110}ms`}}/>
              <div className="catalog-dynamic-shimmer h-8 w-16 rounded-full" style={{...sh,animationDelay:`${i*80+130}ms`}}/>
            </div>
          </div>
        ))}
      </div>
      <div className="overflow-hidden rounded-[28px] p-7" style={{ background:tk.surface1, border:`1px solid ${tk.border}` }}>
        {[...Array(5)].map((_,i) => (
          <div key={i} className="catalog-dynamic-shimmer mb-4 rounded-xl" style={{...sh,height:i===0?48:i===4?52:28,animationDelay:`${i*60}ms`}}/>
        ))}
      </div>
    </div>
  );
}
