import React from "react";
import { Link } from "react-router-dom";
import { motion, useMotionValue, useSpring } from "motion/react";
import { ShoppingCart, Star, ArrowRight, Bookmark } from "lucide-react";
import { formatVND } from "../utils/formatters.js";

// ── Magnetic button: pointer follows the cursor ──────────────────────────────
function MagneticButton({ children, onClick, className = "" }) {
  const ref = React.useRef(null);
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const sx = useSpring(x, { stiffness: 180, damping: 14, mass: 0.08 });
  const sy = useSpring(y, { stiffness: 180, damping: 14, mass: 0.08 });

  function handleMouse(e) {
    const rect = ref.current?.getBoundingClientRect();
    if (!rect) return;
    x.set((e.clientX - (rect.left + rect.width / 2)) * 0.28);
    y.set((e.clientY - (rect.top + rect.height / 2)) * 0.28);
  }

  return (
    <motion.button
      ref={ref}
      type="button"
      onMouseMove={handleMouse}
      onMouseLeave={() => { x.set(0); y.set(0); }}
      style={{ x: sx, y: sy }}
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      className={`relative overflow-hidden rounded-full font-bold ${className}`}
    >
      <motion.span
        className="pointer-events-none absolute inset-0 rounded-full"
        style={{
          background: "radial-gradient(circle at 50% 0%, rgba(255,255,255,0.25) 0%, transparent 60%)",
        }}
      />
      {children}
    </motion.button>
  );
}

// ── Glare layer that tracks mouse inside the book cover ───────────────────────
function BookGlare({ mousePos, isHovered }) {
  return (
    <motion.div
      className="pointer-events-none absolute inset-0 rounded-l-md rounded-r-3xl"
      animate={{ opacity: isHovered ? 1 : 0 }}
      transition={{ duration: 0.3 }}
      style={{
        background: `radial-gradient(circle at ${mousePos.x}% ${mousePos.y}%, rgba(255,255,255,0.65) 0%, rgba(255,255,255,0.15) 35%, transparent 65%)`,
        mixBlendMode: "overlay",
      }}
    />
  );
}

// ── Main spotlight book ───────────────────────────────────────────────────────
function SpotlightBook({ book }) {
  const cardRef = React.useRef(null);
  const [mousePos, setMousePos] = React.useState({ x: 50, y: 50 });
  const [isHovered, setIsHovered] = React.useState(false);

  const rotateX = useMotionValue(0);
  const rotateY = useMotionValue(0);
  const sRotX = useSpring(rotateX, { stiffness: 140, damping: 18 });
  const sRotY = useSpring(rotateY, { stiffness: 140, damping: 18 });

  function handleMouseMove(e) {
    const rect = cardRef.current?.getBoundingClientRect();
    if (!rect) return;
    const px = (e.clientX - rect.left) / rect.width;
    const py = (e.clientY - rect.top) / rect.height;
    setMousePos({ x: px * 100, y: py * 100 });
    rotateX.set((py - 0.5) * -14);
    rotateY.set((px - 0.5) * 14);
  }

  function handleLeave() {
    setIsHovered(false);
    setMousePos({ x: 50, y: 50 });
    rotateX.set(0);
    rotateY.set(0);
  }

  const currentPrice = Number(book.price || 0);
  const oldPrice = Number(book.priceOld || 0);
  // Tính % giảm từ 2 giá gốc/bán
  const discountPercent = oldPrice > currentPrice
    ? Math.round(((oldPrice - currentPrice) / oldPrice) * 100)
    : 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 40 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1] }}
      className="group relative flex flex-col items-center gap-12 lg:flex-row lg:items-stretch"
    >
      {/* ── Book Cover ── */}
      <div className="relative w-full max-w-xs flex-shrink-0 lg:max-w-sm" style={{ perspective: "1200px" }}>
        {/* Cinematic ambient glow behind book */}
        <motion.div
          className="absolute -inset-12 rounded-full opacity-50"
          animate={{
            background: isHovered
              ? "radial-gradient(circle, rgba(96,165,250,0.55) 0%, transparent 70%)"
              : "radial-gradient(circle, rgba(96,165,250,0.25) 0%, transparent 70%)",
            scale: isHovered ? 1.15 : 1,
          }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          style={{ filter: "blur(30px)", pointerEvents: "none" }}
        />

        {/* Floating particles */}
        {[...Array(5)].map((_, i) => (
          <motion.div
            key={i}
            className="pointer-events-none absolute h-1 w-1 rounded-full bg-blue-300/60"
            style={{
              left: `${20 + i * 15}%`,
              top: `${10 + i * 18}%`,
            }}
            animate={{
              y: [0, -14 - i * 4, 0],
              opacity: [0.3, 0.9, 0.3],
              scale: [1, 1.6, 1],
            }}
            transition={{
              duration: 2.5 + i * 0.5,
              repeat: Infinity,
              delay: i * 0.4,
              ease: "easeInOut",
            }}
          />
        ))}

        <motion.div
          ref={cardRef}
          className="relative block aspect-[2/3] w-full cursor-pointer"
          style={{
            rotateX: sRotX,
            rotateY: sRotY,
            transformStyle: "preserve-3d",
          }}
          animate={{ scale: isHovered ? 1.05 : 1 }}
          transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
          onMouseMove={handleMouseMove}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={handleLeave}
        >
          <Link to={`/product/${book.slug}`} className="absolute inset-0">
            {/* Book image */}
            <img
              src={book.image || book.cover || "/placeholder-book.jpg"}
              alt={book.title}
              className="absolute inset-0 h-full w-full rounded-l-md rounded-r-3xl object-cover"
              style={{
                boxShadow: isHovered
                  ? "22px 36px 80px rgba(0,0,0,0.7), -6px 6px 24px rgba(0,0,0,0.35)"
                  : "12px 20px 40px rgba(0,0,0,0.5)",
                transition: "box-shadow 0.5s ease",
              }}
            />
            {/* Book spine */}
            <div className="absolute bottom-0 left-0 top-0 w-7 overflow-hidden rounded-l-md bg-gradient-to-r from-black/70 via-black/30 to-transparent" />
            {/* Glare */}
            <BookGlare mousePos={mousePos} isHovered={isHovered} />
          </Link>

          {/* Discount badge */}
          {discountPercent > 0 && (
            <motion.div
              className="absolute -right-3 -top-3 z-20 flex h-14 w-14 flex-col items-center justify-center rounded-full bg-rose-500 text-white shadow-[0_0_20px_rgba(239,68,68,0.5)]"
              initial={{ scale: 0, rotate: -20 }}
              animate={{ scale: 1, rotate: 0 }}
              transition={{ type: "spring", stiffness: 400, damping: 15, delay: 0.4 }}
            >
              <span className="text-[9px] font-bold uppercase tracking-wider">SAVE</span>
              <span className="text-lg font-black leading-none">{discountPercent}%</span>
            </motion.div>
          )}
        </motion.div>
      </div>

      {/* ── Info panel ── */}
      <div className="flex flex-1 flex-col justify-center">
        {/* Editor badge + stars */}
        <motion.div
          className="mb-5 inline-flex items-center gap-3"
          initial={{ opacity: 0, x: -20 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <span className="rounded-full border border-blue-500/40 bg-blue-500/15 px-3.5 py-1.5 text-[10px] font-black uppercase tracking-[0.15em] text-blue-300">
            ✦ Editor&apos;s Choice
          </span>
          <div className="flex items-center gap-0.5">
            {Array.from({ length: 5 }).map((_, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, scale: 0 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: 0.4 + i * 0.07, type: "spring", stiffness: 500, damping: 15 }}
              >
                <Star size={13} className="text-amber-400" fill="currentColor" />
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Title */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, delay: 0.25 }}
        >
          <Link to={`/product/${book.slug}`} className="group/title block">
            <h3
              className="mb-2 text-4xl font-extrabold leading-[1.05] text-white transition-colors duration-300 group-hover/title:text-blue-200 lg:text-5xl"
              style={{ fontFamily: "'Plus Jakarta Sans', sans-serif", letterSpacing: "-0.02em" }}
            >
              {book.title}
            </h3>
          </Link>
          <p className="mt-1 text-base font-medium text-blue-200/50">{book.author}</p>
        </motion.div>

        {/* Quote card */}
        <motion.div
          className="relative my-8 overflow-hidden rounded-2xl border border-white/[0.07] p-6"
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, delay: 0.35 }}
          style={{ background: "rgba(255,255,255,0.04)", backdropFilter: "blur(16px)" }}
        >
          <div className="absolute -left-2 -top-2 flex h-9 w-9 items-center justify-center rounded-full bg-blue-600 text-white shadow-[0_0_20px_rgba(37,99,235,0.5)]">
            <span style={{ fontFamily: "Georgia, serif", fontSize: "1.4rem", lineHeight: 1, paddingTop: 2 }}>&ldquo;</span>
          </div>
          <p
            className="text-base italic leading-relaxed text-blue-100/80"
            style={{ fontFamily: "'Cormorant Garamond', serif", fontSize: "1.1rem" }}
          >
            A standout pick with a sharp point of view and a story that keeps its hold through the final page.
          </p>
          <div className="mt-4 flex items-center gap-2">
            <div className="h-px flex-1 bg-white/10" />
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-blue-400/80">Aivira Curator</p>
          </div>
        </motion.div>

        {/* Price + CTA */}
        <motion.div
          className="flex flex-wrap items-center gap-5"
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, delay: 0.45 }}
        >
          <div>
            {discountPercent > 0 && (
              <div className="mb-0.5 flex items-center gap-2">
                <span className="text-sm text-slate-500 line-through">{formatVND(oldPrice)}</span>
                <span className="rounded-full bg-rose-500/15 px-2 py-0.5 text-xs font-black text-rose-400">
                  -{discountPercent}%
                </span>
              </div>
            )}
            <div className="text-3xl font-black text-white" style={{ letterSpacing: "-0.02em" }}>
              {formatVND(currentPrice)}
            </div>
          </div>

          <MagneticButton
            className="group flex items-center gap-2.5 bg-gradient-to-r from-blue-600 to-blue-500 px-8 py-4 text-sm text-white shadow-[0_0_30px_rgba(37,99,235,0.45)] transition-shadow hover:shadow-[0_0_50px_rgba(37,99,235,0.65)]"
            onClick={() => console.log("Add to cart", book.id)}
          >
            <ShoppingCart size={16} className="transition-transform group-hover:scale-110" />
            Mua ngay
            <motion.span
              className="ml-1"
              animate={{ x: [0, 4, 0] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
            >
              <ArrowRight size={14} />
            </motion.span>
          </MagneticButton>

          <button
            type="button"
            className="flex h-12 w-12 items-center justify-center rounded-full border border-white/10 bg-white/5 text-white/50 transition-all duration-300 hover:border-white/20 hover:bg-white/10 hover:text-white"
          >
            <Bookmark size={16} />
          </button>
        </motion.div>
      </div>
    </motion.div>
  );
}

// ── Runner-up card ────────────────────────────────────────────────────────────
function RunnerUpBook({ book, index }) {
  const [hovered, setHovered] = React.useState(false);
  const currentPrice = Number(book.price || 0);
  const oldPrice = Number(book.priceOld || 0);
  // Tính % giảm từ 2 giá gốc/bán
  const discountPercent = oldPrice > currentPrice
    ? Math.round(((oldPrice - currentPrice) / oldPrice) * 100)
    : 0;

  return (
    <motion.div
      initial={{ opacity: 0, x: 30 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.55, delay: 0.1 + index * 0.12, ease: [0.22, 1, 0.36, 1] }}
      onHoverStart={() => setHovered(true)}
      onHoverEnd={() => setHovered(false)}
    >
      <Link
        to={`/product/${book.slug}`}
        className="group relative flex items-center gap-4 overflow-hidden rounded-2xl border border-white/[0.06] p-4 backdrop-blur-sm"
        style={{
          background: hovered
            ? "linear-gradient(135deg, rgba(37,99,235,0.12), rgba(255,255,255,0.05))"
            : "rgba(255,255,255,0.03)",
          borderColor: hovered ? "rgba(96,165,250,0.3)" : "rgba(255,255,255,0.06)",
          boxShadow: hovered ? "0 8px 32px rgba(0,0,0,0.35)" : "none",
          transform: hovered ? "translateY(-2px)" : "translateY(0)",
          transition: "all 0.35s cubic-bezier(0.22,1,0.36,1)",
        }}
      >
        {/* Rank watermark */}
        <div
          className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 select-none text-[3.5rem] font-black leading-none"
          style={{
            color: "rgba(255,255,255,0.04)",
            fontFamily: "'Plus Jakarta Sans', sans-serif",
            opacity: hovered ? 0.08 : 0.04,
            transition: "opacity 0.3s",
          }}
        >
          {String(index + 2).padStart(2, "0")}
        </div>

        {/* Book cover + discount badge */}
        <div className="relative h-28 w-20 flex-shrink-0 overflow-hidden rounded-xl shadow-lg">
          <img
            src={book.image || book.cover || "/placeholder-book.jpg"}
            alt={book.title}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
          />
          <div className="absolute inset-y-0 left-0 w-1.5 bg-gradient-to-r from-black/50 to-transparent" />
          {/* Discount badge on cover */}
          {discountPercent > 0 && (
            <motion.div
              className="absolute left-0 top-0 rounded-br-xl bg-rose-500 px-2 py-1 text-[10px] font-black leading-none text-white shadow-lg"
              initial={{ opacity: 0, scale: 0.7 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 + index * 0.1, type: "spring", stiffness: 400, damping: 18 }}
            >
              -{discountPercent}%
            </motion.div>
          )}
        </div>

        {/* Info */}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span className="text-[10px] font-black uppercase tracking-[0.15em] text-blue-400">
            {book.catLabel || "Weekly Pick"}
          </span>
          <p
            className="line-clamp-2 text-base font-bold leading-snug text-white/90 transition-colors duration-300 group-hover:text-blue-200"
            style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            {book.title}
          </p>
          <p className="line-clamp-1 text-xs text-slate-500">{book.author}</p>

          {/* Price row */}
          <div className="mt-2 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              {/* Current price */}
              <span className="text-base font-extrabold text-white">{formatVND(currentPrice)}</span>
              {/* Old price strikethrough */}
              {discountPercent > 0 && (
                <span className="text-xs text-slate-500 line-through">{formatVND(oldPrice)}</span>
              )}
            </div>
            {/* Cart button */}
            <motion.div
              animate={{ x: hovered ? 0 : -4, opacity: hovered ? 1 : 0 }}
              transition={{ duration: 0.25 }}
              className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-blue-600/80 text-white shadow-[0_0_12px_rgba(37,99,235,0.4)]"
            >
              <ShoppingCart size={13} />
            </motion.div>
          </div>
        </div>
      </Link>
    </motion.div>
  );
}

// ── Main export ───────────────────────────────────────────────────────────────
export default function WeeklyPicksShowcase({ books, loading, emptyMessage }) {
  if (loading) {
    return (
      <div className="mt-12 flex flex-col gap-12 lg:flex-row lg:gap-16">
        {/* Spotlight skeleton */}
        <div className="lg:w-[58%]">
          <div className="flex flex-col gap-10 lg:flex-row">
            <div className="aspect-[2/3] w-full max-w-xs animate-pulse rounded-3xl bg-white/5" />
            <div className="flex flex-1 flex-col gap-4 pt-4">
              <div className="h-4 w-28 animate-pulse rounded bg-white/5" />
              <div className="h-10 w-4/5 animate-pulse rounded bg-white/5" />
              <div className="h-4 w-32 animate-pulse rounded bg-white/5" />
              <div className="mt-4 h-28 animate-pulse rounded-2xl bg-white/5" />
              <div className="mt-4 h-12 w-48 animate-pulse rounded-full bg-white/5" />
            </div>
          </div>
        </div>
        {/* Runner-ups skeleton */}
        <div className="flex flex-col gap-4 lg:w-[42%]">
          {[0, 1, 2].map((i) => (
            <div key={i} className="flex gap-4 rounded-2xl bg-white/5 p-4">
              <div className="h-28 w-20 animate-pulse rounded-xl bg-white/5" style={{ animationDelay: `${i * 100}ms` }} />
              <div className="flex flex-1 flex-col gap-2 py-1">
                <div className="h-3 w-20 animate-pulse rounded bg-white/5" />
                <div className="h-5 w-full animate-pulse rounded bg-white/5" />
                <div className="h-4 w-3/4 animate-pulse rounded bg-white/5" />
                <div className="mt-auto h-5 w-24 animate-pulse rounded bg-white/5" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (!books || books.length === 0) {
    return (
      <div className="mt-12 flex h-48 items-center justify-center rounded-2xl border border-dashed border-white/15 text-slate-400">
        {emptyMessage}
      </div>
    );
  }

  const spotlight = books[0];
  const runnerUps = books.slice(1, 4);

  return (
    <div className="mt-14 flex flex-col gap-12 lg:flex-row lg:gap-20">
      {/* Spotlight — 58% */}
      <div className="lg:w-[58%]">
        <SpotlightBook book={spotlight} />
      </div>

      {/* Runner-ups — 42% */}
      <div className="flex flex-col lg:w-[42%]">
        <motion.h4
          className="mb-6 text-[10px] font-black uppercase tracking-[0.25em] text-white/30"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          ✦ More picks this week
        </motion.h4>
        <div className="flex flex-col gap-3">
          {runnerUps.map((book, index) => (
            <RunnerUpBook key={book.id || book.slug} book={book} index={index} />
          ))}
        </div>

        {/* Divider + link */}
        <motion.div
          className="mt-8 flex items-center gap-4"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.6 }}
        >
          <div className="h-px flex-1 bg-white/[0.06]" />
          <Link
            to="/category/all"
            className="group flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] text-white/25 transition-colors hover:text-blue-400"
          >
            Xem tất cả
            <ArrowRight size={11} className="transition-transform group-hover:translate-x-1" />
          </Link>
        </motion.div>
      </div>
    </div>
  );
}
