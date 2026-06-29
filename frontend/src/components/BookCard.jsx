import React from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { useTranslation } from "react-i18next";
import { formatVND } from "../utils/formatters.js";
import { ShoppingBag, Star } from "lucide-react";

export default function BookCard({ book, dark = false }) {
  const { t } = useTranslation();
  const [hovered, setHovered] = React.useState(false);
  const hasOldPrice = Number(book.priceOld || 0) > Number(book.price || 0);
  const rating = Number(book.rating || 0);
  const stockQuantity = Number(book.stockQuantity || 0);
  const discountPct = hasOldPrice
    ? Math.round((1 - Number(book.price) / Number(book.priceOld)) * 100)
    : 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 28 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "0px 0px -40px 0px" }}
      transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      onHoverStart={() => setHovered(true)}
      onHoverEnd={() => setHovered(false)}
      style={{
        transform: hovered ? "translateY(-6px)" : "translateY(0)",
        transition: "transform 0.4s cubic-bezier(0.22,1,0.36,1)",
      }}
    >
      <Link
        to={`/product/${book.slug}`}
        className="group relative flex h-full min-w-0 flex-col overflow-hidden"
        style={dark ? {
          borderRadius: "var(--r-lg)",
          background: hovered ? "rgba(18,24,64,0.95)" : "rgba(12,17,48,0.88)",
          boxShadow: hovered
            ? "0 20px 50px rgba(79,110,247,0.2), 0 6px 20px rgba(0,0,0,0.3)"
            : "0 4px 20px rgba(0,0,0,0.25)",
          border: hovered ? "1px solid rgba(79,110,247,0.4)" : "1px solid rgba(255,255,255,0.065)",
          transition: "all 0.4s cubic-bezier(0.22,1,0.36,1)",
        } : {
          borderRadius: "var(--r-lg)",
          background: "#fff",
          boxShadow: hovered
            ? "0 20px 50px rgba(37,99,235,0.12), 0 6px 20px rgba(0,0,0,0.08)"
            : "0 2px 12px rgba(0,0,0,0.06)",
          border: hovered ? "1px solid rgba(147,197,253,0.6)" : "1px solid rgba(226,232,240,0.8)",
          transition: "box-shadow 0.4s ease, border-color 0.4s ease",
        }}
      >
        {/* Discount badge */}
        {discountPct > 0 && (
          <motion.div
            className="absolute left-3 top-3 z-20 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-white"
            style={{ borderRadius: "var(--r-sm)", background: "linear-gradient(135deg, #ef4444, #dc2626)" }}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: "spring", stiffness: 500, damping: 20 }}
          >
            -{discountPct}%
          </motion.div>
        )}

        {/* Category badge */}
        {book.badge && (
          <div
            className="absolute right-3 top-3 z-20 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white"
            style={{ borderRadius: "var(--r-sm)", background: "linear-gradient(135deg, #2563eb, #3b82f6)" }}
          >
            {book.badge}
          </div>
        )}

        {/* Cover image */}
        <div
          className="relative w-full overflow-hidden bg-slate-100"
          style={{ aspectRatio: "2/3", flexShrink: 0 }}
        >
          <img
            src={book.image || book.cover}
            alt={book.title}
            className="h-full w-full object-cover"
            style={{
              transform: hovered ? "scale(1.08)" : "scale(1)",
              transition: "transform 0.7s cubic-bezier(0.22,1,0.36,1)",
            }}
          />
          {/* Spine shadow */}
          <div className="pointer-events-none absolute inset-y-0 left-0 w-4 bg-gradient-to-r from-black/25 to-transparent" />

          {/* Hover overlay: slide-up CTA */}
          <div
            className="absolute inset-0 flex flex-col items-center justify-end"
            style={{
              background: "linear-gradient(to top, rgba(10,17,40,0.85) 0%, rgba(10,17,40,0.3) 50%, transparent 100%)",
              opacity: hovered ? 1 : 0,
              transition: "opacity 0.35s ease",
            }}
          >
            <motion.div
              animate={{ y: hovered ? 0 : 12, opacity: hovered ? 1 : 0 }}
              transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
              className="mb-4 flex items-center gap-2 px-5 py-2.5 text-xs font-bold uppercase tracking-wider text-white"
              style={{
                borderRadius: "var(--r-pill)",
                background: "rgba(37,99,235,0.7)",
                border: "1px solid rgba(96,165,250,0.4)",
                backdropFilter: "blur(12px)",
                boxShadow: "0 4px 20px rgba(37,99,235,0.4)",
              }}
            >
              <ShoppingBag size={13} />
              {t("common.viewDetails")}
            </motion.div>
          </div>
        </div>

        {/* Info */}
        <div className="flex flex-grow flex-col p-4">
          {/* Category label */}
          {book.catLabel && (
            <span
              className="mb-1.5 text-[10px] font-bold uppercase tracking-[0.15em]"
              style={{ color: dark ? "#4f6ef7" : "#3b82f6", fontFamily:"var(--f-body)" }}
            >
              {book.catLabel}
            </span>
          )}

          {/* Title */}
          <h3
            className="mb-1 line-clamp-2 text-[15px] font-bold leading-snug transition-colors duration-300"
            style={{ fontFamily:"var(--f-body)", color: dark ? (hovered ? "#93a8ff" : "#e8eeff") : (hovered ? "#1d4ed8" : "#0f172a") }}
          >
            {book.title}
          </h3>

          {/* Author */}
          <p className="mb-3 line-clamp-1 text-xs font-medium" style={{ color: dark ? "#4a5578" : "#94a3b8" }}>
            {book.author}
          </p>

          {/* Stock indicator */}
          <div className="mb-3 flex items-center gap-1.5">
            <span className="relative flex h-1.5 w-1.5 flex-shrink-0">
              {stockQuantity > 0 && (
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
              )}
              <span
                className={`relative inline-flex h-1.5 w-1.5 rounded-full ${stockQuantity > 0 ? "bg-emerald-500" : "bg-red-400"}`}
              />
            </span>
            <span
              className="text-[10px] font-semibold"
              style={{ color: dark ? "#4a5578" : "#64748b" }}
            >
              {stockQuantity > 0 ? t("home.inStock", { count: stockQuantity }) : t("home.outOfStock")}
            </span>
          </div>

          {/* Price row */}
          <div className="mt-auto flex items-end justify-between gap-2 pt-1">
            <div className="flex flex-col">
              <span
                className="text-xl font-black"
                style={{ fontFamily:"var(--f-body)", letterSpacing:"-0.02em", color: dark ? "#e8eeff" : "#0f172a" }}
              >
                {formatVND(book.price)}
              </span>
              {hasOldPrice && (
                <span className="text-[11px] font-medium line-through" style={{ color: dark ? "#4a5578" : "#94a3b8" }}>
                  {formatVND(book.priceOld)}
                </span>
              )}
            </div>

            {rating > 0 && (
              <div
                className="flex flex-shrink-0 items-center gap-1 px-2 py-0.5"
                style={dark ? {
                  borderRadius:"var(--r-sm)",
                  background:"rgba(240,165,0,0.14)",
                  border:"1px solid rgba(240,165,0,0.35)",
                } : {
                  borderRadius:"var(--r-sm)",
                  background:"#fffbeb",
                  border:"1px solid #fde68a",
                }}
              >
                <Star size={9} className="text-amber-500" fill="currentColor" />
                <span className="text-[10px] font-bold" style={{ color: dark ? "#fbbf24" : "#92400e" }}>{rating.toFixed(1)}</span>
              </div>
            )}
          </div>
        </div>

        {/* Bottom hover accent line */}
        <div
          className="absolute bottom-0 left-0 h-[2px] rounded-b"
          style={{
            background: dark
              ? "linear-gradient(to right, #4f6ef7, #a78bfa)"
              : "linear-gradient(to right, #2563eb, #60a5fa)",
            width: hovered ? "100%" : "0%",
            transition: "width 0.4s cubic-bezier(0.22,1,0.36,1)",
          }}
        />
      </Link>
    </motion.div>
  );
}
