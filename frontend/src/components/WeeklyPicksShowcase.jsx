import React from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ShoppingCart, Star } from "lucide-react";
import { formatVND } from "../utils/formatters.js";

function MagneticButton({ children, onClick, className = "" }) {
  const ref = React.useRef(null);
  const [position, setPosition] = React.useState({ x: 0, y: 0 });

  function handleMouse(event) {
    const rect = ref.current?.getBoundingClientRect();
    if (!rect) return;
    const x = event.clientX - (rect.left + rect.width / 2);
    const y = event.clientY - (rect.top + rect.height / 2);
    setPosition({ x: x * 0.2, y: y * 0.2 });
  }

  return (
    <motion.button
      ref={ref}
      type="button"
      onMouseMove={handleMouse}
      onMouseLeave={() => setPosition({ x: 0, y: 0 })}
      animate={{ x: position.x, y: position.y }}
      transition={{ type: "spring", stiffness: 150, damping: 15, mass: 0.1 }}
      onClick={onClick}
      className={`relative overflow-hidden rounded-full font-bold transition-colors ${className}`}
    >
      {children}
    </motion.button>
  );
}

function SpotlightBook({ book }) {
  const cardRef = React.useRef(null);
  const [mousePos, setMousePos] = React.useState({ x: 50, y: 50 });
  const [isHovered, setIsHovered] = React.useState(false);

  function handleMouseMove(event) {
    const rect = cardRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    setMousePos({ x, y });
  }

  const rotateX = isHovered ? (mousePos.y - 50) * -0.2 : 0;
  const rotateY = isHovered ? (mousePos.x - 50) * 0.2 : 0;
  const currentPrice = Number(book.price || 0);
  const oldPrice = Number(book.priceOld || 0);
  const discountAmount = oldPrice - currentPrice;
  const discountPercent = oldPrice > 0 ? Math.round((discountAmount / oldPrice) * 100) : 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.8, ease: "easeOut" }}
      className="group relative flex flex-col items-center gap-10 lg:flex-row lg:items-stretch"
    >
      <div className="relative w-full max-w-sm flex-shrink-0 lg:max-w-md" style={{ perspective: "1000px" }}>
        <Link
          to={`/product/${book.slug}`}
          ref={cardRef}
          onMouseMove={handleMouseMove}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => {
            setIsHovered(false);
            setMousePos({ x: 50, y: 50 });
          }}
          className="relative block aspect-[2/3] w-full"
          style={{
            transform: `rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(${isHovered ? 1.05 : 1})`,
            transition: "transform 0.4s cubic-bezier(0.22,1,0.36,1)",
            transformStyle: "preserve-3d",
          }}
        >
          <div
            className="absolute -inset-10 opacity-40 blur-3xl transition-opacity duration-500 group-hover:opacity-70"
            style={{
              background: "radial-gradient(circle at 50% 50%, rgba(96,165,250,0.5), transparent 70%)",
              transform: "translateZ(-50px)",
            }}
          />

          <img
            src={book.image || book.cover || "/placeholder-book.jpg"}
            alt={book.title}
            className="absolute inset-0 h-full w-full rounded-l-md rounded-r-3xl object-cover shadow-[inset_4px_0_10px_rgba(255,255,255,0.2)]"
            style={{
              boxShadow: isHovered
                ? "20px 30px 60px rgba(0,0,0,0.6), -5px 5px 20px rgba(0,0,0,0.3)"
                : "10px 15px 30px rgba(0,0,0,0.4)",
              transition: "box-shadow 0.4s ease",
            }}
          />

          <div className="absolute bottom-0 left-0 top-0 w-6 origin-left overflow-hidden rounded-l-md bg-gradient-to-r from-black/60 to-transparent" />
          <div
            className="pointer-events-none absolute inset-0 rounded-l-md rounded-r-3xl opacity-0 mix-blend-overlay transition-opacity duration-300 group-hover:opacity-100"
            style={{
              background: `radial-gradient(circle at ${mousePos.x}% ${mousePos.y}%, rgba(255,255,255,0.8) 0%, transparent 50%)`,
            }}
          />
        </Link>
      </div>

      <div className="flex flex-1 flex-col justify-center">
        <div className="mb-4 inline-flex items-center gap-3">
          <span className="rounded-full border border-blue-500/30 bg-blue-500/20 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-300">
            {"Editor's Choice"}
          </span>
          <div className="flex items-center gap-1 text-amber-400">
            {Array.from({ length: 5 }).map((_, index) => (
              <Star key={index} size={14} fill="currentColor" />
            ))}
          </div>
        </div>

        <Link to={`/product/${book.slug}`} className="group/title block">
          <h3
            className="mb-2 text-4xl font-extrabold leading-tight text-white transition-colors group-hover/title:text-blue-200 lg:text-5xl"
            style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            {book.title}
          </h3>
        </Link>

        <p className="mb-6 text-lg font-medium text-blue-200/60">{book.author}</p>

        <div className="relative mb-8 rounded-2xl border border-white/5 bg-white/5 p-6 backdrop-blur-md">
          <div className="absolute -left-3 -top-3 flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-white shadow-[0_0_15px_rgba(37,99,235,0.5)]">
            <span className="font-serif text-xl leading-none">&ldquo;</span>
          </div>
          <p
            className="text-lg italic leading-relaxed text-blue-100/90"
            style={{ fontFamily: "'Cormorant Garamond', serif" }}
          >
            A standout pick with a sharp point of view and a story that keeps its hold through the final page.
          </p>
          <p className="mt-3 text-sm font-bold uppercase tracking-widest text-blue-400/80">Aivira Curator</p>
        </div>

        <div className="flex items-end gap-6">
          <div>
            {discountPercent > 0 && (
              <div className="mb-1 flex items-center gap-2">
                <span className="text-sm text-slate-500 line-through">{formatVND(oldPrice)}</span>
                <span className="rounded bg-rose-500/20 px-1.5 py-0.5 text-xs font-bold text-rose-300">
                  -{discountPercent}%
                </span>
              </div>
            )}
            <div className="text-3xl font-black text-white">{formatVND(currentPrice)}</div>
          </div>

          <MagneticButton
            className="flex items-center gap-2 bg-blue-600 px-8 py-3.5 text-white shadow-[0_0_20px_rgba(37,99,235,0.4)] hover:bg-blue-500"
            onClick={() => {
              console.log("Add to cart", book.id);
            }}
          >
            <ShoppingCart size={18} />
            Buy now
          </MagneticButton>
        </div>
      </div>
    </motion.div>
  );
}

function RunnerUpBook({ book, index }) {
  const currentPrice = Number(book.price || 0);
  const oldPrice = Number(book.priceOld || 0);
  const discountAmount = oldPrice - currentPrice;
  const discountPercent = oldPrice > 0 ? Math.round((discountAmount / oldPrice) * 100) : 0;

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5, delay: index * 0.15 }}
      className="group relative flex items-center gap-5 rounded-2xl border border-white/5 bg-slate-900/50 p-4 transition-all duration-300 hover:border-blue-500/30 hover:bg-slate-800/80 hover:shadow-[0_8px_30px_rgba(0,0,0,0.3)]"
    >
      <Link
        to={`/product/${book.slug}`}
        className="relative block h-32 w-24 flex-shrink-0 overflow-hidden rounded-lg shadow-md transition-transform duration-300 group-hover:scale-105"
      >
        <img src={book.image || book.cover || "/placeholder-book.jpg"} alt={book.title} className="h-full w-full object-cover" />
        <div className="absolute inset-0 rounded-lg shadow-[inset_2px_0_5px_rgba(255,255,255,0.2)]" />
      </Link>

      <div className="flex min-w-0 flex-1 flex-col">
        <Link
          to={`/product/${book.slug}`}
          className="mb-1 block truncate text-lg font-bold text-white transition-colors group-hover:text-blue-300"
          style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
        >
          {book.title}
        </Link>
        <p className="mb-3 truncate text-sm text-slate-400">{book.author}</p>

        <div className="mt-auto flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-lg font-extrabold text-blue-100">{formatVND(currentPrice)}</span>
            {discountPercent > 0 && <span className="text-xs text-slate-500 line-through">{formatVND(oldPrice)}</span>}
          </div>
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-white/5 text-slate-300 transition-colors hover:bg-blue-600 hover:text-white"
          >
            <ShoppingCart size={16} />
          </button>
        </div>
      </div>
    </motion.div>
  );
}

export default function WeeklyPicksShowcase({ books, loading, emptyMessage }) {
  if (loading) {
    return (
      <div className="flex h-96 w-full items-center justify-center">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-blue-500/30 border-t-blue-500" />
      </div>
    );
  }

  if (!books || books.length === 0) {
    return (
      <div className="flex h-40 w-full items-center justify-center rounded-2xl border border-dashed border-white/20 text-slate-400">
        {emptyMessage}
      </div>
    );
  }

  const spotlight = books[0];
  const runnerUps = books.slice(1, 4);

  return (
    <div className="mt-12 flex flex-col gap-12 lg:flex-row lg:gap-16">
      <div className="lg:w-[60%]">
        <SpotlightBook book={spotlight} />
      </div>

      <div className="flex flex-col justify-center gap-4 lg:w-[40%]">
        <h4 className="mb-4 text-sm font-bold uppercase tracking-widest text-slate-400">More picks this week</h4>
        <div className="flex flex-col gap-4">
          {runnerUps.map((book, index) => (
            <RunnerUpBook key={book.id || book.slug} book={book} index={index} />
          ))}
        </div>
      </div>
    </div>
  );
}
