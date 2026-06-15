import React, { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ArrowRight, BookOpen, CheckCircle2, Clock3, PackageCheck, Sparkles } from "lucide-react";

import { getCategories, getProducts } from "../api/catalogApi.js";
import { getStorefrontHome } from "../api/storefrontApi.js";
import WeeklyPicksShowcase from "../components/WeeklyPicksShowcase.jsx";
import { formatVND } from "../utils/formatters.js";
import { normalizeBook, normalizeCategoryHighlight, pageRows } from "../utils/mappers.js";

const CATEGORY_FALLBACK_IMAGES = [
  "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1519682337058-a94d519337bc?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1499750310107-5fef28a66643?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=800&auto=format&fit=crop",
];

// Accent color per category index — harmonizes with the blue brand palette
const CAT_ACCENTS = [
  { color: "#f59e0b", glow: "rgba(245,158,11,0.35)" },   // amber
  { color: "#3b82f6", glow: "rgba(59,130,246,0.35)" },   // blue (brand)
  { color: "#ec4899", glow: "rgba(236,72,153,0.35)" },   // pink
  { color: "#10b981", glow: "rgba(16,185,129,0.35)" },   // emerald
  { color: "#8b5cf6", glow: "rgba(139,92,246,0.35)" },   // violet
  { color: "#38bdf8", glow: "rgba(56,189,248,0.35)" },   // sky
];

export default function HomePage() {
  const { t } = useTranslation();
  const { featured, newArrivals, bestselling, categoryHighlights, books, loading, message } = useStorefrontHome();
  const orbitBooks = featured.length ? featured : newArrivals.length ? newArrivals : bestselling;
  const [activeOrbit, setActiveOrbit] = useState(0);

  useEffect(() => {
    if (orbitBooks.length === 0) return undefined;

    const timer = setInterval(() => {
      setActiveOrbit((current) => (current + 1) % orbitBooks.length);
    }, 2600);

    return () => clearInterval(timer);
  }, [orbitBooks.length]);

  return (
    <div className="tw-home w-full overflow-hidden" style={{ background: "var(--cream-50)" }}>
      <section className="relative flex min-h-screen items-center overflow-hidden bg-slate-950 px-4 pb-16 pt-28 text-white md:px-8">
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          <div className="absolute -right-1/4 top-1/4 h-[800px] w-[800px] rounded-full bg-blue-600/20 blur-[120px] mix-blend-screen" />
          <div className="absolute -bottom-1/4 -left-1/4 h-[600px] w-[600px] rounded-full bg-sky-400/10 blur-[100px] mix-blend-screen" />
          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay" />
        </div>

        <div className="relative z-10 mx-auto grid w-full max-w-7xl grid-cols-1 items-center gap-16 lg:grid-cols-2">
          <motion.div
            className="z-20 flex flex-col items-start gap-8 pt-10"
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7 }}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.8, ease: "backOut" }}
              className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md"
            >
              <span className="h-2 w-2 animate-pulse rounded-full bg-blue-400" />
              <span className="text-sm font-medium tracking-wide text-slate-300">
                {t("home.heroKicker")}
              </span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="text-5xl font-bold leading-[1.08] tracking-tight md:text-7xl"
              style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.02em" }}
            >
              {t("home.title1")} <br />
              {t("home.title2")}{" "}
              <span className="bg-gradient-to-r from-blue-400 to-sky-200 bg-clip-text text-transparent">
                {t("home.titleWorld")}
              </span>
              <br />
              <em className="mt-2 block text-3xl font-light italic text-slate-400 md:text-5xl" style={{ fontFamily: "var(--f-serif)" }}>
                {t("home.subtitle")}
              </em>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.3 }}
              className="max-w-lg text-lg font-light leading-relaxed text-slate-400 md:text-xl"
            >
              {t("home.heroCopy")}
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.4 }}
              className="flex flex-wrap items-center gap-4"
            >
              <Link
                to="/category/all"
                className="group relative overflow-hidden px-8 py-4 font-bold tracking-wide text-white transition-all duration-300 hover:scale-105"
                style={{
                  borderRadius: "var(--r-pill)",
                  background: "linear-gradient(135deg, #1d4ed8, #2563eb, #3b82f6)",
                  boxShadow: "0 0 40px rgba(37,99,235,0.4), 0 4px 16px rgba(0,0,0,0.25)",
                }}
                onMouseEnter={(e) => e.currentTarget.style.boxShadow="0 0 60px rgba(56,189,248,0.6), 0 8px 24px rgba(0,0,0,0.3)"}
                onMouseLeave={(e) => e.currentTarget.style.boxShadow="0 0 40px rgba(37,99,235,0.4), 0 4px 16px rgba(0,0,0,0.25)"}
              >
                {t("home.exploreLibrary")}
              </Link>
              <Link
                to="/cart"
                className="px-8 py-4 font-bold tracking-wide text-white backdrop-blur transition-all duration-300 hover:bg-white/15"
                style={{
                  borderRadius: "var(--r-pill)",
                  border: "1.5px solid rgba(255,255,255,0.2)",
                  background: "rgba(255,255,255,0.06)",
                }}
              >
                {t("home.viewCart")}
              </Link>
            </motion.div>

            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 1, delay: 0.8 }}
              className="mt-4 grid w-full grid-cols-3 gap-8 border-t border-white/10 pt-8"
            >
              <div>
                <strong className="block font-display text-4xl text-blue-400">{books.length}+</strong>
                <span className="mt-1 block text-xs uppercase tracking-widest text-slate-400">
                  {t("home.titles")}
                </span>
              </div>
              <div>
                <strong className="block font-display text-4xl text-white">COD</strong>
                <span className="mt-1 block text-xs uppercase tracking-widest text-slate-400">
                  {t("home.supported")}
                </span>
              </div>
              <div>
                <strong className="block font-display text-4xl text-white">VNPay</strong>
                <span className="mt-1 block text-xs uppercase tracking-widest text-slate-400">
                  {t("home.momo")}
                </span>
              </div>
            </motion.div>
          </motion.div>

          <HeroBookOrbit books={orbitBooks} activeOrbit={activeOrbit} />
          <MobileHeroBooks books={orbitBooks.slice(0, 3)} loading={loading} />
        </div>
      </section>

      <Ticker />

      <CategoryShowcase categories={categoryHighlights} loading={loading} />

      {/* ── Weekly Picks — Dark Editorial Mode ── */}
      <section className="relative overflow-hidden bg-[#0a1128] px-4 py-24 md:px-8">
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay" />
        <div className="mx-auto max-w-7xl relative z-10">
          <SectionHead chip={t("home.weeklyPicksChip")} title={t("home.weeklyPicks")} link="/category/all" dark={true} />
          {message && <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{message}</div>}
          <WeeklyPicksShowcase books={featured} loading={loading} emptyMessage={t("home.noFeaturedBooks")} />
        </div>
      </section>

      {/* ── New Arrivals — warm cream bg, high contrast with dark above ── */}
      <section className="relative overflow-hidden bg-[#f8f6f1] px-4 py-24 md:px-8">
        {/* Subtle top wave separator */}
        <div className="absolute left-0 right-0 top-0 h-1 bg-gradient-to-r from-transparent via-blue-400/30 to-transparent" />
        {/* Corner decorations */}
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-blue-500/5 blur-3xl" />
        <div className="pointer-events-none absolute -left-20 bottom-0 h-48 w-48 rounded-full bg-amber-400/8 blur-3xl" />
        <div className="pointer-events-none absolute inset-0 new-arrivals-paper-texture" />
        <NewArrivalsEditorial books={newArrivals} loading={loading} emptyMessage={t("home.noNewArrivals")} />
      </section>

      {/* ── Bestselling — light ── */}
      <BestsellingRanking books={bestselling} loading={loading} t={t} />

      {/* ── Quote — dark blue ── */}
      <QuoteSection />

      {/* ── About — light ── */}
      <AboutSection booksCount={books.length} />

      {/* ── How It Works — dark navy ── */}
      <HowItWorks />

      {/* ── Latest News — light cream ── */}
      <LatestNews />
    </div>
  );
}

function useStorefrontHome() {
  const { t } = useTranslation();
  const [state, setState] = useState({
    featured: [],
    newArrivals: [],
    bestselling: [],
    categoryHighlights: [],
    books: []
  });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setMessage("");

    async function loadHome() {
      try {
        const payload = await getStorefrontHome({ signal: controller.signal });
        setState(toHomeState(payload));
      } catch (error) {
        if (error.name === "AbortError") return;

        try {
          const fallbackPayload = await getStorefrontFallback(controller.signal);
          setState(toHomeState(fallbackPayload));
        } catch (fallbackError) {
          if (fallbackError.name === "AbortError") return;
          setState({
            featured: [],
            newArrivals: [],
            bestselling: [],
            categoryHighlights: [],
            books: []
          });
          setMessage(fallbackError.message || error.message || t("home.storefrontFailed"));
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadHome();

    return () => {
      controller.abort();
    };
  }, [t]);

  return { ...state, loading, message };
}

function toHomeState(payload) {
  const featured = (payload?.featuredBooks || []).map((row) => normalizeBook(row));
  const newArrivals = (payload?.newArrivals || []).map((row) => normalizeBook(row));
  const bestselling = (payload?.bestsellingBooks || []).map((row) => normalizeBook(row));
  const categoryHighlights = (payload?.categoryHighlights || [])
    .map((row) => normalizeCategoryHighlight(row))
    .filter(Boolean);

  return {
    featured,
    newArrivals,
    bestselling,
    categoryHighlights,
    books: uniqueBooks([...featured, ...newArrivals, ...bestselling])
  };
}

async function getStorefrontFallback(signal) {
  const [newestResult, bestsellingResult, categoriesResult] = await Promise.allSettled([
    getProducts({ page: 1, size: 8, sort: "newest" }, { signal }),
    getProducts({ page: 1, size: 8, sort: "best_selling" }, { signal }),
    getCategories({ signal })
  ]);

  throwIfAborted(signal);

  const newestRows = newestResult.status === "fulfilled" ? pageRows(newestResult.value) : [];
  const bestsellingRows = bestsellingResult.status === "fulfilled" ? pageRows(bestsellingResult.value) : [];
  const categoryRows = categoriesResult.status === "fulfilled" ? categoriesResult.value || [] : [];
  const featuredBooks = newestRows.filter((row) => row?.featured);
  const fallbackFeatured = featuredBooks.length ? featuredBooks : newestRows.slice(0, 4);

  if (!newestRows.length && !bestsellingRows.length && !categoryRows.length) {
    throw (
      newestResult.reason ||
      bestsellingResult.reason ||
      categoriesResult.reason ||
      new Error("Could not load storefront data")
    );
  }

  return {
    featuredBooks: fallbackFeatured,
    newArrivals: newestRows,
    bestsellingBooks: bestsellingRows.length ? bestsellingRows : newestRows,
    categoryHighlights: categoryRows.map((category) => ({
      ...category,
      categoryId: category.categoryId ?? category.id
    }))
  };
}

function throwIfAborted(signal) {
  if (signal?.aborted) {
    throw new DOMException("The operation was aborted.", "AbortError");
  }
}

function uniqueBooks(items) {
  const seen = new Set();
  return items.filter((book) => {
    const key = book.productId || book.id || book.slug;
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function HeroBookOrbit({ books, activeOrbit }) {
  const positions = [
    { x: -260, y: 18, rotate: -11, scale: 0.82, z: 10, opacity: 0.7 },
    { x: -132, y: -14, rotate: -5, scale: 0.94, z: 20, opacity: 0.9 },
    { x: 0, y: -34, rotate: 0, scale: 1.12, z: 40, opacity: 1 },
    { x: 138, y: -12, rotate: 5, scale: 0.95, z: 22, opacity: 0.9 },
    { x: 268, y: 18, rotate: 11, scale: 0.82, z: 10, opacity: 0.72 },
  ];

  const visibleBooks = useMemo(() => {
    if (!books.length) return [];

    const items = [];
    const max = Math.min(5, books.length);

    for (let index = 0; index < max; index += 1) {
      items.push(books[(activeOrbit + index) % books.length]);
    }

    return items;
  }, [books, activeOrbit]);

  if (!visibleBooks.length) {
    return (
      <div className="relative hidden h-[600px] w-full lg:block">
        <div className="absolute left-1/2 top-1/2 h-[380px] w-[260px] -translate-x-1/2 -translate-y-1/2 rounded-3xl border border-white/10 bg-white/5 shadow-2xl backdrop-blur" />
      </div>
    );
  }

  return (
    <div className="relative hidden h-[600px] w-full overflow-visible lg:block">
      <div className="pointer-events-none absolute left-1/2 top-1/2 h-[520px] w-[620px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-blue-600/15 blur-3xl" />
      <div className="pointer-events-none absolute left-1/2 top-[54%] h-[320px] w-[560px] -translate-x-1/2 -translate-y-1/2 rounded-full border border-white/10 bg-white/[0.025] shadow-[inset_0_0_80px_rgba(37,99,235,0.08)]" />
      <div className="pointer-events-none absolute bottom-20 left-1/2 h-14 w-[500px] -translate-x-1/2 rounded-full bg-black/40 blur-2xl" />

      {visibleBooks.map((book, index) => {
        const position = positions[index] || positions[positions.length - 1];

        return (
          <motion.div
            key={book.id}
            initial={{
              opacity: 0,
              x: position.x,
              y: position.y + 28,
              rotate: position.rotate,
              scale: position.scale * 0.92,
            }}
            animate={{
              opacity: position.opacity,
              x: position.x,
              y: position.y,
              rotate: position.rotate,
              scale: position.scale,
            }}
            transition={{
              opacity: { duration: 0.5 },
              x: { type: "spring", stiffness: 44, damping: 18, mass: 1.1 },
              y: { type: "spring", stiffness: 44, damping: 18, mass: 1.1 },
              rotate: { type: "spring", stiffness: 48, damping: 19, mass: 1 },
              scale: { type: "spring", stiffness: 48, damping: 19, mass: 1 },
            }}
            className="absolute left-1/2 top-1/2 aspect-[2/3] w-[190px] cursor-pointer rounded-xl will-change-transform"
            style={{
              zIndex: position.z,
            }}
          >
            <motion.div
              animate={{ y: [0, -10, 0] }}
              transition={{
                duration: 4.8 + index * 0.25,
                ease: "easeInOut",
                repeat: Infinity,
                repeatType: "mirror",
              }}
              className="absolute left-0 top-0 h-full w-full"
            >
              <Link
                to={`/product/${book.slug}`}
                className="group block h-full w-full -translate-x-1/2 -translate-y-1/2 origin-center overflow-hidden rounded-[22px] border border-white/15 bg-slate-900 shadow-[0_34px_90px_rgba(0,0,0,0.58)] ring-1 ring-white/5 transition duration-500 hover:scale-[1.045]"
              >
                <img
                  src={book.image || book.cover}
                  alt={book.title}
                  className="h-full w-full object-cover transition duration-700 group-hover:scale-105"
                />

                <div className="absolute inset-0 bg-gradient-to-tr from-slate-950/60 via-transparent to-white/25 opacity-80" />
                <div className="absolute inset-x-0 top-0 h-1/2 bg-gradient-to-b from-white/10 to-transparent opacity-70" />
                <div className="absolute bottom-0 left-0 right-0 translate-y-3 bg-gradient-to-t from-slate-950/95 via-slate-950/70 to-transparent p-4 opacity-0 transition duration-300 group-hover:translate-y-0 group-hover:opacity-100">
                  <p className="line-clamp-2 font-serif text-lg font-bold leading-tight text-white">
                    {book.title}
                  </p>
                  <p className="mt-1 line-clamp-1 text-xs font-medium text-slate-300">
                    {book.author}
                  </p>
                </div>
                <div className="pointer-events-none absolute inset-0 rounded-[22px] shadow-[inset_0_0_0_1px_rgba(255,255,255,0.08),inset_18px_0_28px_rgba(2,6,23,0.28)]" />
              </Link>
            </motion.div>
          </motion.div>
        );
      })}
    </div>
  );
}

function MobileHeroBooks({ books, loading }) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="grid gap-3 lg:hidden">
        {Array.from({ length: 3 }).map((_, index) => (
          <div key={index} className="h-24 animate-pulse rounded-2xl border border-white/10 bg-white/10" />
        ))}
      </div>
    );
  }

  if (!books.length) {
    return (
      <div className="rounded-2xl border border-white/10 bg-white/5 p-5 text-sm text-slate-300 lg:hidden">
        {t("home.noFeaturedBooks")}
      </div>
    );
  }

  return (
    <div className="grid gap-3 lg:hidden">
      {books.map((book) => (
        <Link
          key={book.id}
          to={`/product/${book.slug}`}
          className="flex items-center gap-4 rounded-2xl border border-white/10 bg-white/5 p-3 backdrop-blur"
        >
          <img src={book.image || book.cover} alt={book.title} className="h-24 w-16 rounded-xl object-cover" />
          <span className="min-w-0">
            <strong className="line-clamp-2 font-serif text-lg text-white">{book.title}</strong>
            <small className="mt-1 line-clamp-1 block text-slate-400">{book.author}</small>
          </span>
        </Link>
      ))}
    </div>
  );
}

function Ticker() {
  const { t } = useTranslation();
  const text = t("home.ticker");

  return (
    <div
      className="relative flex overflow-hidden whitespace-nowrap border-y border-blue-900/50 bg-[#0a1128] py-4"
    >
      <motion.div
        animate={{ x: ["0%", "-50%"] }}
        transition={{ ease: "linear", duration: 35, repeat: Infinity }}
        className="flex items-center text-sm font-semibold uppercase tracking-[0.25em] text-blue-200/80"
      >
        {Array.from({ length: 8 }).map((_, index) => (
          <span key={index} className="flex items-center gap-12 px-12">
            <span>{text}</span>
            <span className="h-1 w-1 rotate-45 bg-blue-400/50" />
          </span>
        ))}
      </motion.div>
    </div>
  );
}

// Individual card component so we can use hooks per card
function CategoryCard({ category, index, getGridClass, getAspect, t }) {
  const accent = CAT_ACCENTS[index % CAT_ACCENTS.length];
  const cardRef = React.useRef(null);
  const [mousePos, setMousePos] = React.useState({ x: 50, y: 50 });
  const [isHovered, setIsHovered] = React.useState(false);

  function handleMouseMove(e) {
    const rect = cardRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    setMousePos({ x, y });
  }

  // Parallax transform values
  const offsetX = isHovered ? (mousePos.x - 50) * 0.018 : 0;
  const offsetY = isHovered ? (mousePos.y - 50) * 0.018 : 0;
  const rotateX = isHovered ? (mousePos.y - 50) * -0.12 : 0;
  const rotateY = isHovered ? (mousePos.x - 50) * 0.12 : 0;

  return (
    <motion.div
      className={getGridClass(index)}
      initial={{ opacity: 0, y: 40, filter: "blur(12px)" }}
      whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
      viewport={{ once: true, margin: "-30px" }}
      transition={{
        duration: 0.7,
        delay: index * 0.1,
        ease: [0.22, 1, 0.36, 1],
      }}
      style={{ perspective: "1000px" }}
    >
      <Link
        ref={cardRef}
        to={`/category/${category.slug}`}
        className={[
          "group relative block w-full overflow-hidden rounded-2xl bg-slate-900",
          getAspect(index),
        ].join(" ")}
        style={{
          boxShadow: isHovered
            ? `0 32px 80px rgba(0,0,0,0.5), 0 0 0 1px ${accent.color}40, 0 0 60px ${accent.glow}`
            : "0 8px 32px rgba(0,0,0,0.2)",
          transform: `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(${isHovered ? 1.012 : 1})`,
          transition: "box-shadow 0.5s ease, transform 0.35s cubic-bezier(0.22,1,0.36,1)",
        }}
        onMouseMove={handleMouseMove}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => {
          setIsHovered(false);
          setMousePos({ x: 50, y: 50 });
        }}
      >
        {/* ── Background image with parallax ── */}
        <div
          className="absolute inset-0 overflow-hidden"
          style={{ borderRadius: "inherit" }}
        >
          <img
            src={category.imageUrl || CATEGORY_FALLBACK_IMAGES[index % CATEGORY_FALLBACK_IMAGES.length]}
            alt={category.categoryName}
            className="absolute inset-[-6%] h-[112%] w-[112%] object-cover"
            style={{
              transform: `translate(${-offsetX * 3}%, ${-offsetY * 3}%) scale(1)`,
              transition: isHovered ? "transform 0.1s linear" : "transform 0.6s cubic-bezier(0.22,1,0.36,1)",
            }}
          />
        </div>

        {/* ── Mouse spotlight layer ── */}
        <div
          className="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
          style={{
            background: `radial-gradient(320px circle at ${mousePos.x}% ${mousePos.y}%, ${accent.color}22 0%, transparent 70%)`,
            pointerEvents: "none",
          }}
        />

        {/* ── Gradient overlays ── */}
        <div className="absolute inset-0 bg-gradient-to-b from-black/5 via-black/20 to-black/90" />
        <div
          className="absolute inset-0 opacity-0 transition-opacity duration-700 group-hover:opacity-100"
          style={{
            background: `linear-gradient(135deg, ${accent.color}10 0%, transparent 50%, ${accent.glow} 100%)`,
          }}
        />

        {/* ── Noise texture overlay ── */}
        <div
          className="absolute inset-0 opacity-[0.04] mix-blend-overlay"
          style={{
            backgroundImage: "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E\")",
          }}
        />

        {/* ── Watermark number ── */}
        <div
          className="pointer-events-none absolute -right-4 -top-6 select-none leading-none text-white/[0.07] transition-all duration-700 group-hover:text-white/[0.13] group-hover:-right-2 group-hover:-top-4"
          style={{
            fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
            fontSize: "clamp(7rem, 14vw, 11rem)",
            letterSpacing: "-0.02em",
          }}
        >
          {String(index + 1).padStart(2, "0")}
        </div>

        {/* ── Accent color stripe top ── */}
        <div
          className="absolute left-0 right-0 top-0 h-[2px] opacity-0 transition-opacity duration-500 group-hover:opacity-100"
          style={{ background: `linear-gradient(to right, transparent, ${accent.color}, transparent)` }}
        />

        {/* ── Small accent badge top-left ── */}
        <motion.div
          className="absolute left-4 top-4 z-20 flex h-7 items-center rounded-full px-2.5"
          animate={{ y: isHovered ? -2 : 0 }}
          transition={{ type: "spring", stiffness: 300, damping: 20 }}
          style={{
            background: `${accent.color}dd`,
            backdropFilter: "blur(10px)",
            fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
            fontSize: "0.8rem",
            letterSpacing: "0.1em",
            color: "#fff",
            boxShadow: `0 0 20px ${accent.glow}, inset 0 1px 0 rgba(255,255,255,0.25)`,
          }}
        >
          {String(index + 1).padStart(2, "0")}
        </motion.div>

        {/* ── Bottom content: slide up on hover ── */}
        <div className="absolute bottom-0 left-0 right-0 z-10 p-4">
          {/* Content panel */}
          <motion.div
            animate={{ y: isHovered ? -4 : 0 }}
            transition={{ type: "spring", stiffness: 260, damping: 22 }}
            className="overflow-hidden rounded-xl"
            style={{
              background: "rgba(2,6,23,0.72)",
              backdropFilter: "blur(20px)",
              borderTop: `1.5px solid ${accent.color}55`,
              borderLeft: "1px solid rgba(255,255,255,0.05)",
              borderRight: "1px solid rgba(255,255,255,0.05)",
              boxShadow: `inset 0 1px 0 rgba(255,255,255,0.07), 0 -8px 32px ${accent.glow}`,
            }}
          >
            {/* Category name row */}
            <div className="p-4 pb-0">
              <h3 
                className="text-xl font-bold leading-tight tracking-tight text-white/95 md:text-2xl"
                style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
              >
                {category.categoryName}
              </h3>
            </div>

            {/* Detail row */}
            <div className="flex items-center justify-between gap-3 p-3 pt-2">
              {/* Book count */}
              <motion.div
                animate={{ x: isHovered ? 2 : 0, opacity: isHovered ? 1 : 0.8 }}
                transition={{ duration: 0.25 }}
              >
                {category.bookCount ? (
                  <span
                    className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[0.65rem] font-bold tracking-wide"
                    style={{
                      background: "rgba(255,255,255,0.08)",
                      border: "1px solid rgba(255,255,255,0.14)",
                      color: "rgba(255,255,255,0.8)",
                    }}
                  >
                    <span className="h-1.5 w-1.5 rounded-full flex-shrink-0" style={{ background: accent.color }} />
                    {t("home.categoryBookCount", { count: category.bookCount })}
                  </span>
                ) : (
                  <span className="text-xs text-white/40">{category.description}</span>
                )}
              </motion.div>

              {/* Magnetic arrow button */}
              <motion.div
                className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full"
                animate={{
                  scale: isHovered ? 1.15 : 1,
                  x: isHovered ? 1 : 0,
                }}
                transition={{ type: "spring", stiffness: 400, damping: 20 }}
                style={{
                  background: `linear-gradient(135deg, ${accent.color}, ${accent.color}bb)`,
                  boxShadow: isHovered ? `0 0 20px ${accent.glow}, 0 4px 12px rgba(0,0,0,0.4)` : `0 2px 8px ${accent.glow}`,
                }}
              >
                <ArrowRight size={13} color="#fff" strokeWidth={2.5} />
              </motion.div>
            </div>

            {/* Progress bar */}
            <motion.div
              className="h-[2px] origin-left"
              initial={{ scaleX: 0 }}
              animate={{ scaleX: isHovered ? 1 : 0 }}
              transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
              style={{ background: `linear-gradient(to right, ${accent.color}, transparent)` }}
            />
          </motion.div>
        </div>

        {/* ── Corner shimmer effect ── */}
        <div
          className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-500 group-hover:opacity-100"
          style={{
            background: `linear-gradient(135deg, rgba(255,255,255,0.06) 0%, transparent 50%)`,
          }}
        />
      </Link>
    </motion.div>
  );
}

function CategoryShowcase({ categories, loading }) {
  const { t } = useTranslation();
  const cats = categories.slice(0, 6);

  // Bento grid: items 0,5 span 2 cols; 1–4 span 1 col
  // On desktop: row1 = [wide][small][small] (4cols), row2 = [small][small][wide] (4cols)
  function getGridClass(index) {
    if (index === 0) return "lg:col-span-2 lg:row-span-1";
    if (index === 5) return "lg:col-span-2 lg:row-span-1";
    return "lg:col-span-1";
  }

  function getAspect(index) {
    if (index === 0 || index === 5) return "aspect-[16/9] lg:aspect-[16/10]";
    return "aspect-[4/5]";
  }

  return (
    <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
      <SectionHead chip={t("home.explore")} title={t("home.categoryHighlights")} link="/category/all" />
      {loading ? (
        <CategorySkeleton />
      ) : cats.length ? (
        <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {cats.map((category, index) => (
            <CategoryCard
              key={category.id || category.slug}
              category={category}
              index={index}
              getGridClass={getGridClass}
              getAspect={getAspect}
              t={t}
            />
          ))}
        </div>
      ) : (
        <HomeEmptyState title={t("home.noCategoryHighlights")} />
      )}
    </section>
  );
}

function QuoteSection() {
  const { t } = useTranslation();
  const quote = t("home.quote");
  const words = quote.split(" ");

  return (
    <section className="relative flex items-center justify-center overflow-hidden px-4 py-32 text-white" style={{ background: "linear-gradient(180deg, #050818 0%, #040d24 100%)" }}>
      {/* Animated star particles */}
      {[...Array(12)].map((_, i) => (
        <motion.div
          key={i}
          className="pointer-events-none absolute h-0.5 w-0.5 rounded-full bg-blue-300/40"
          style={{
            left: `${8 + (i * 7.7) % 84}%`,
            top: `${10 + (i * 13) % 80}%`,
          }}
          animate={{ opacity: [0.2, 0.8, 0.2], scale: [1, 1.8, 1] }}
          transition={{ duration: 2.5 + (i % 4) * 0.7, repeat: Infinity, delay: i * 0.3 }}
        />
      ))}
      {/* Soft dark vignette glow */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(37,99,235,0.07)_0%,transparent_70%)]" />

      <div className="relative z-10 mx-auto max-w-3xl text-center">
        {/* Decorative quote mark */}
        <motion.div
          initial={{ opacity: 0, y: -20, scale: 0.6 }}
          whileInView={{ opacity: 1, y: 0, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 1, ease: [0.22, 1, 0.36, 1] }}
          className="mb-2 inline-block select-none text-[7rem] leading-none text-blue-500/25"
          style={{ fontFamily: "Georgia, serif", lineHeight: 0.8 }}
        >
          &ldquo;
        </motion.div>

        {/* Word-by-word stagger reveal */}
        <p
          className="mb-10 text-3xl font-light italic leading-relaxed text-blue-50/95 md:text-4xl lg:text-[2.8rem]"
          style={{ fontFamily: "var(--f-serif)", lineHeight: 1.45 }}
        >
          {words.map((word, i) => (
            <motion.span
              key={i}
              className="inline-block"
              style={{ marginRight: "0.28em" }}
              initial={{ opacity: 0, y: 20, filter: "blur(4px)" }}
              whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.1 + i * 0.04, ease: [0.22, 1, 0.36, 1] }}
            >
              {word}
            </motion.span>
          ))}
        </p>

        <motion.div
          className="inline-flex items-center gap-4 text-xs font-bold uppercase tracking-[0.3em] text-blue-400/70"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, delay: 0.4 + words.length * 0.04 }}
        >
          <span className="h-px w-12 bg-gradient-to-r from-transparent to-blue-500/50" />
          {t("home.philosophy")}
          <span className="h-px w-12 bg-gradient-to-l from-transparent to-blue-500/50" />
        </motion.div>
      </div>
    </section>
  );
}

function NewArrivalsEditorial({ books, loading, emptyMessage }) {
  const { t } = useTranslation();
  const items = books.slice(0, 8);
  const spotlight = items[0];
  const shelf = items.slice(1);

  return (
    <div className="relative z-10 mx-auto max-w-7xl">
      <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "0px 0px -80px 0px" }}
          transition={{ duration: 0.65, ease: [0.22, 1, 0.36, 1] }}
          className="max-w-3xl"
        >
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-blue-200/80 bg-white/75 px-4 py-2 text-[0.68rem] font-black uppercase tracking-[0.18em] text-blue-700 shadow-sm backdrop-blur">
            <Sparkles size={13} className="text-amber-500" />
            {t("home.collection")}
          </div>
          <h2
            className="text-4xl font-bold leading-tight text-slate-950 md:text-6xl"
            style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
          >
            {t("home.newArrivals")}
          </h2>
          <p className="mt-4 max-w-2xl text-base leading-relaxed text-slate-500 md:text-lg">
            {t("home.newArrivalsSubtitle")}
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.15 }}
        >
          <Link
            to="/category/all?sort=newest"
            className="group inline-flex items-center gap-3 rounded-full border border-slate-200 bg-slate-950 px-6 py-3 text-xs font-black uppercase tracking-[0.16em] text-white shadow-[0_16px_40px_rgba(15,23,42,0.18)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-blue-700 hover:shadow-[0_20px_46px_rgba(37,99,235,0.26)]"
          >
            {t("home.viewNewBooks")}
            <ArrowRight size={14} className="transition-transform duration-300 group-hover:translate-x-1" />
          </Link>
        </motion.div>
      </div>

      {loading ? (
        <NewArrivalsSkeleton />
      ) : items.length ? (
        <div className="mt-12 space-y-8">
          <NewArrivalSpotlight book={spotlight} />

          <div className="flex min-w-0 flex-col">
            <motion.div
              initial={{ opacity: 0, x: 18 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.1 }}
              className="mb-4 flex items-center justify-between gap-4 border-b border-slate-200/80 pb-4"
            >
              <div className="inline-flex items-center gap-2 text-[0.68rem] font-black uppercase tracking-[0.2em] text-slate-500">
                <BookOpen size={14} className="text-blue-600" />
                {t("home.releaseShelf")}
              </div>
              <span className="hidden text-xs font-semibold text-slate-400 sm:inline">
                {items.length} {t("home.allBooks").toLowerCase()}
              </span>
            </motion.div>

            {shelf.length ? (
              <div className="grid flex-1 grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                {shelf.map((book, index) => (
                  <NewArrivalShelfCard key={book.id || book.slug} book={book} index={index} />
                ))}
              </div>
            ) : (
              <motion.div
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.55, delay: 0.15 }}
                className="flex min-h-[260px] items-center justify-center rounded-2xl border border-dashed border-blue-200 bg-white/70 p-8 text-center shadow-sm backdrop-blur"
              >
                <p className="max-w-sm text-sm font-semibold leading-6 text-slate-500">
                  {t("home.newArrivalsSubtitle")}
                </p>
              </motion.div>
            )}
          </div>
        </div>
      ) : (
        <HomeEmptyState title={emptyMessage} />
      )}
    </div>
  );
}

function NewArrivalSpotlight({ book }) {
  const { t } = useTranslation();
  const [isHovered, setIsHovered] = React.useState(false);
  const [mousePos, setMousePos] = React.useState({ x: 50, y: 50 });
  const discountPct = getDiscountPercent(book);
  const stockQuantity = Number(book.stockQuantity || 0);
  const rotateX = isHovered ? (mousePos.y - 50) * -0.12 : 0;
  const rotateY = isHovered ? (mousePos.x - 50) * 0.12 : 0;

  function handleMouseMove(event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    setMousePos({ x, y });
  }

  return (
    <motion.article
      initial={{ opacity: 0, y: 34, filter: "blur(10px)" }}
      whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
      viewport={{ once: true, margin: "0px 0px -80px 0px" }}
      transition={{ duration: 0.78, ease: [0.22, 1, 0.36, 1] }}
      className="relative self-start overflow-hidden rounded-[28px] border border-white/80 bg-white/[0.78] p-5 shadow-[0_28px_90px_rgba(15,23,42,0.13)] backdrop-blur-xl md:p-7"
    >
      <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-blue-500/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-20 left-8 h-52 w-52 rounded-full bg-amber-400/[0.14] blur-3xl" />
      <div className="pointer-events-none absolute inset-x-6 top-0 h-px bg-gradient-to-r from-transparent via-amber-300/70 to-transparent" />

      <div className="relative grid items-center gap-8 md:grid-cols-[minmax(210px,0.62fr)_minmax(0,1.38fr)]">
        <Link
          to={`/product/${book.slug}`}
          className="group relative mx-auto block w-full max-w-[240px] md:max-w-[260px]"
          style={{ perspective: "1200px" }}
          onMouseMove={handleMouseMove}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => {
            setIsHovered(false);
            setMousePos({ x: 50, y: 50 });
          }}
        >
          <motion.div
            className="relative aspect-[2/3] overflow-hidden rounded-l-lg rounded-r-[28px] bg-slate-900 shadow-[24px_34px_70px_rgba(15,23,42,0.36)]"
            animate={{
              y: isHovered ? -8 : 0,
              scale: isHovered ? 1.025 : 1,
              rotateX,
              rotateY,
            }}
            transition={{ type: "spring", stiffness: 190, damping: 20 }}
            style={{
              transformStyle: "preserve-3d",
            }}
          >
            <img
              src={book.image || book.cover}
              alt={book.title}
              className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
            />
            <div className="absolute inset-y-0 left-0 w-8 bg-gradient-to-r from-black/[0.65] via-black/[0.26] to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-tr from-slate-950/[0.38] via-transparent to-white/[0.28]" />
            <div
              className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
              style={{
                background: `radial-gradient(circle at ${mousePos.x}% ${mousePos.y}%, rgba(255,255,255,0.6), rgba(255,255,255,0.14) 34%, transparent 66%)`,
                mixBlendMode: "overlay",
              }}
            />
            <div className="absolute inset-0 rounded-l-lg rounded-r-[28px] shadow-[inset_0_0_0_1px_rgba(255,255,255,0.14),inset_18px_0_26px_rgba(2,6,23,0.24)]" />
          </motion.div>

          <div className="pointer-events-none absolute -bottom-7 left-1/2 h-10 w-[78%] -translate-x-1/2 rounded-full bg-slate-950/[0.22] blur-xl transition-opacity duration-300 group-hover:opacity-80" />

          {discountPct > 0 && (
            <motion.span
              initial={{ opacity: 0, scale: 0.7, rotate: -10 }}
              whileInView={{ opacity: 1, scale: 1, rotate: 0 }}
              viewport={{ once: true }}
              transition={{ type: "spring", stiffness: 330, damping: 18, delay: 0.2 }}
              className="absolute -right-3 top-4 z-10 rounded-full bg-rose-500 px-3 py-1.5 text-[0.68rem] font-black uppercase tracking-wider text-white shadow-[0_10px_28px_rgba(244,63,94,0.28)]"
            >
              -{discountPct}%
            </motion.span>
          )}
        </Link>

        <div className="flex min-w-0 flex-col justify-center">
          <motion.div
            initial={{ opacity: 0, x: -12 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.52, delay: 0.15 }}
            className="mb-4 flex flex-wrap items-center gap-2"
          >
            <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-600 px-3 py-1.5 text-[0.66rem] font-black uppercase tracking-[0.16em] text-white shadow-[0_10px_24px_rgba(37,99,235,0.22)]">
              <Clock3 size={12} />
              {t("home.newArrivalSpotlight")}
            </span>
            {book.publicationYear && (
              <span className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1.5 text-[0.66rem] font-black uppercase tracking-[0.14em] text-amber-700">
                {book.publicationYear}
              </span>
            )}
          </motion.div>

          <Link to={`/product/${book.slug}`} className="group/title block">
            <h3
              className="line-clamp-3 text-3xl font-bold leading-[1.05] text-slate-950 transition-colors duration-300 group-hover/title:text-blue-700 md:text-4xl"
              style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
            >
              {book.title}
            </h3>
          </Link>
          <p className="mt-3 line-clamp-1 text-sm font-bold uppercase tracking-[0.16em] text-slate-400">
            {book.author}
          </p>
          <p className="mt-5 line-clamp-3 text-sm leading-6 text-slate-500">
            {book.desc || t("home.newArrivalsSubtitle")}
          </p>

          <div className="mt-6 flex flex-wrap gap-2">
            <NewArrivalMetaPill icon={<BookOpen size={13} />} label={book.catLabel || book.categoryName || t("common.books")} />
            <NewArrivalMetaPill
              icon={stockQuantity > 0 ? <CheckCircle2 size={13} /> : <PackageCheck size={13} />}
              label={stockQuantity > 0 ? t("home.inStock", { count: stockQuantity }) : t("home.outOfStock")}
              tone={stockQuantity > 0 ? "green" : "red"}
            />
          </div>

          <div className="mt-8 flex flex-wrap items-end justify-between gap-5 border-t border-slate-200 pt-6">
            <div>
              {discountPct > 0 && (
                <div className="mb-1 text-sm font-semibold text-slate-400 line-through">
                  {formatVND(book.priceOld)}
                </div>
              )}
              <div className="text-3xl font-black text-slate-950" style={{ letterSpacing: "-0.02em" }}>
                {formatVND(book.price)}
              </div>
            </div>

            <Link
              to={`/product/${book.slug}`}
              className="group inline-flex items-center gap-2.5 rounded-full bg-gradient-to-r from-slate-950 to-blue-800 px-6 py-3 text-xs font-black uppercase tracking-[0.14em] text-white shadow-[0_16px_34px_rgba(15,23,42,0.2)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_20px_44px_rgba(37,99,235,0.28)]"
            >
              {t("common.viewDetails")}
              <ArrowRight size={14} className="transition-transform duration-300 group-hover:translate-x-1" />
            </Link>
          </div>
        </div>
      </div>
    </motion.article>
  );
}

function NewArrivalShelfCard({ book, index }) {
  const { t } = useTranslation();
  const [isHovered, setIsHovered] = React.useState(false);
  const discountPct = getDiscountPercent(book);
  const stockQuantity = Number(book.stockQuantity || 0);

  return (
    <motion.article
      initial={{ opacity: 0, y: 28, filter: "blur(8px)" }}
      whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
      viewport={{ once: true, margin: "0px 0px -60px 0px" }}
      transition={{ duration: 0.56, delay: index * 0.07, ease: [0.22, 1, 0.36, 1] }}
      onHoverStart={() => setIsHovered(true)}
      onHoverEnd={() => setIsHovered(false)}
    >
      <Link
        to={`/product/${book.slug}`}
        className="group relative flex h-full min-h-[214px] overflow-hidden rounded-2xl border border-white/80 bg-white/[0.82] p-3 shadow-[0_12px_44px_rgba(15,23,42,0.08)] backdrop-blur transition-all duration-300 hover:-translate-y-1 hover:border-blue-200 hover:shadow-[0_24px_58px_rgba(37,99,235,0.13)]"
      >
        <div className="relative h-[188px] w-[126px] flex-shrink-0 overflow-hidden rounded-l-md rounded-r-2xl bg-slate-100 shadow-[10px_14px_28px_rgba(15,23,42,0.18)]">
          <img
            src={book.image || book.cover}
            alt={book.title}
            className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
          />
          <div className="absolute inset-y-0 left-0 w-5 bg-gradient-to-r from-black/45 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-tr from-slate-950/[0.28] via-transparent to-white/20 opacity-80" />
          <div className="absolute inset-0 translate-x-[-120%] bg-gradient-to-r from-transparent via-white/30 to-transparent transition-transform duration-700 group-hover:translate-x-[120%]" />
          {discountPct > 0 && (
            <span className="absolute left-2 top-2 rounded-full bg-rose-500 px-2 py-1 text-[0.62rem] font-black leading-none text-white">
              -{discountPct}%
            </span>
          )}
        </div>

        <div className="flex min-w-0 flex-1 flex-col px-4 py-2">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-blue-50 px-2.5 py-1 text-[0.62rem] font-black uppercase tracking-[0.14em] text-blue-700">
              {t("home.justArrived")}
            </span>
            {book.publicationYear && (
              <span className="text-[0.68rem] font-bold text-slate-400">{book.publicationYear}</span>
            )}
          </div>

          <h3
            className="line-clamp-2 text-lg font-bold leading-snug text-slate-950 transition-colors duration-300 group-hover:text-blue-700"
            style={{ fontFamily: "var(--f-body)" }}
          >
            {book.title}
          </h3>
          <p className="mt-1 line-clamp-1 text-xs font-semibold text-slate-400">{book.author}</p>
          <p className="mt-3 line-clamp-2 text-xs leading-5 text-slate-500">
            {book.catLabel || book.categoryName || t("common.books")}
          </p>

          <div className="mt-auto flex items-end justify-between gap-3 pt-4">
            <div className="min-w-0">
              {discountPct > 0 && (
                <div className="mb-0.5 text-[0.72rem] font-semibold text-slate-400 line-through">
                  {formatVND(book.priceOld)}
                </div>
              )}
              <div className="text-lg font-black text-slate-950">{formatVND(book.price)}</div>
              <div className={`mt-1 text-[0.68rem] font-bold ${stockQuantity > 0 ? "text-emerald-600" : "text-rose-500"}`}>
                {stockQuantity > 0 ? t("home.inStock", { count: stockQuantity }) : t("home.outOfStock")}
              </div>
            </div>
            <motion.span
              animate={{ x: isHovered ? 0 : -4, opacity: isHovered ? 1 : 0.55 }}
              transition={{ duration: 0.25 }}
              className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-slate-950 text-white shadow-[0_10px_24px_rgba(15,23,42,0.18)] transition-colors duration-300 group-hover:bg-blue-700"
            >
              <ArrowRight size={15} />
            </motion.span>
          </div>
        </div>

        <div className="absolute bottom-0 left-0 h-[2px] w-0 bg-gradient-to-r from-blue-600 via-amber-400 to-transparent transition-all duration-500 group-hover:w-full" />
      </Link>
    </motion.article>
  );
}

function NewArrivalMetaPill({ icon, label, tone = "blue" }) {
  const toneClass = {
    blue: "border-blue-100 bg-blue-50 text-blue-700",
    green: "border-emerald-100 bg-emerald-50 text-emerald-700",
    red: "border-rose-100 bg-rose-50 text-rose-600",
  }[tone];

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-bold ${toneClass}`}>
      {icon}
      <span className="line-clamp-1">{label}</span>
    </span>
  );
}

function NewArrivalsSkeleton() {
  return (
    <div className="mt-12 grid gap-7 lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.25fr)]">
      <div className="rounded-[28px] border border-white/80 bg-white/70 p-6 shadow-sm">
        <div className="grid items-center gap-8 md:grid-cols-[minmax(210px,0.62fr)_minmax(0,1.38fr)]">
          <div className="new-arrivals-skeleton aspect-[2/3] w-full max-w-[240px] rounded-l-lg rounded-r-[28px]" />
          <div className="flex flex-col justify-center">
            <div className="new-arrivals-skeleton h-8 w-32 rounded-full" />
            <div className="new-arrivals-skeleton mt-6 h-10 w-4/5" />
            <div className="new-arrivals-skeleton mt-3 h-10 w-3/5" />
            <div className="new-arrivals-skeleton mt-6 h-4 w-full" />
            <div className="new-arrivals-skeleton mt-2 h-4 w-5/6" />
            <div className="new-arrivals-skeleton mt-8 h-9 w-40 rounded-full" />
          </div>
        </div>
      </div>
      <div>
        <div className="new-arrivals-skeleton mb-4 h-6 w-44 rounded-full" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="flex min-h-[214px] rounded-2xl border border-white/80 bg-white/70 p-3">
              <div className="new-arrivals-skeleton h-[188px] w-[126px] flex-shrink-0 rounded-l-md rounded-r-2xl" />
              <div className="flex flex-1 flex-col px-4 py-2">
                <div className="new-arrivals-skeleton h-6 w-24 rounded-full" />
                <div className="new-arrivals-skeleton mt-4 h-5 w-full" />
                <div className="new-arrivals-skeleton mt-2 h-5 w-3/4" />
                <div className="new-arrivals-skeleton mt-4 h-3 w-28" />
                <div className="new-arrivals-skeleton mt-auto h-6 w-32" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function getDiscountPercent(book) {
  const currentPrice = Number(book?.price || 0);
  const oldPrice = Number(book?.priceOld || 0);

  if (oldPrice > currentPrice && currentPrice > 0) {
    return Math.round((1 - currentPrice / oldPrice) * 100);
  }

  return Number(book?.discountPercentage || 0);
}

function CategorySkeleton() {
  return (
    <>
      <style>{`
        @keyframes cat-shimmer {
          0% { background-position: -600px 0; }
          100% { background-position: 600px 0; }
        }
        .cat-skel {
          background: linear-gradient(105deg, #e2e8f0 30%, #eff3fb 50%, #e2e8f0 70%);
          background-size: 1200px 100%;
          animation: cat-shimmer 1.8s ease-in-out infinite;
        }
      `}</style>
      <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <div className="cat-skel aspect-[16/10] rounded-2xl lg:col-span-2" />
        <div className="cat-skel aspect-[4/5] rounded-2xl" style={{ animationDelay: "120ms" }} />
        <div className="cat-skel aspect-[4/5] rounded-2xl" style={{ animationDelay: "240ms" }} />
        <div className="cat-skel aspect-[4/5] rounded-2xl" style={{ animationDelay: "80ms" }} />
        <div className="cat-skel aspect-[4/5] rounded-2xl" style={{ animationDelay: "160ms" }} />
        <div className="cat-skel aspect-[16/10] rounded-2xl lg:col-span-2" style={{ animationDelay: "200ms" }} />
      </div>
    </>
  );
}

function HomeEmptyState({ title }) {
  return (
    <div className="mt-12 rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h3 className="font-serif text-2xl font-bold text-slate-900">{title}</h3>
    </div>
  );
}

function BestsellingRanking({ books, loading, t }) {
  const items = books.slice(0, 5);
  // Gold, Silver, Bronze, then muted
  const RANK_META = [
    { color: "#f59e0b", label: "🥇", glow: "rgba(245,158,11,0.3)" },
    { color: "#94a3b8", label: "🥈", glow: "rgba(148,163,184,0.2)" },
    { color: "#cd7f32", label: "🥉", glow: "rgba(205,127,50,0.2)" },
    { color: "#475569", label: "04", glow: "rgba(71,85,105,0.1)" },
    { color: "#475569", label: "05", glow: "rgba(71,85,105,0.1)" },
  ];

  return (
    <section className="relative overflow-hidden bg-white px-4 py-28 text-slate-900 md:px-8">
      {/* Light blue accents intertwining */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute -left-1/4 top-0 h-[800px] w-[800px] rounded-full bg-blue-50/80 blur-[120px]" />
        <div className="absolute -right-1/4 bottom-0 h-[600px] w-[600px] rounded-full bg-indigo-50/60 blur-[100px]" />
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.03] mix-blend-overlay" />
      </div>
      {/* Top separator line */}
      <div className="absolute left-0 right-0 top-0 h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent" />

      <div className="relative mx-auto max-w-7xl">
        <SectionHead
          chip={t("home.featuredBooks")}
          title={t("home.bestsellingBooks")}
          link="/category/all?sort=popular"
        />

        {loading ? (
          <div className="mt-10 space-y-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex items-center gap-6 rounded-2xl border border-slate-100 bg-slate-50 p-5">
                <div className="h-12 w-12 animate-pulse rounded-full bg-slate-200" />
                <div className="h-20 w-14 animate-pulse rounded-lg bg-slate-200" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/2 animate-pulse rounded bg-slate-200" />
                  <div className="h-3 w-1/4 animate-pulse rounded bg-slate-200" />
                </div>
                <div className="h-6 w-20 animate-pulse rounded bg-slate-200" />
              </div>
            ))}
          </div>
        ) : (
          <div className="mx-auto mt-12 max-w-4xl space-y-4">
            {items.map((book, index) => {
              const meta = RANK_META[index];
              // Compute discount: API first, then priceOld diff
              const bPrice = Number(book.price || 0);
              const bOldPrice = Number(book.priceOld || 0);
              const bDiscount = bOldPrice > bPrice && bOldPrice > 0
                ? Math.round(((bOldPrice - bPrice) / bOldPrice) * 100)
                : 0;
              return (
                <motion.div
                  key={book.id}
                  initial={{ opacity: 0, x: -30 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.55, delay: index * 0.1, ease: [0.22, 1, 0.36, 1] }}
                >
                  <Link
                    to={`/product/${book.slug}`}
                    className="group relative flex items-center gap-5 overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition-all duration-400 hover:-translate-y-1 hover:border-blue-200 hover:shadow-[0_12px_40px_rgba(37,99,235,0.08)] md:p-5"
                  >
                    {/* Hover glow sweep */}
                    <div
                      className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-500 group-hover:opacity-100"
                      style={{ background: `radial-gradient(600px circle at 0% 50%, rgba(37,99,235,0.03), transparent 60%)` }}
                    />

                    {/* Rank badge */}
                    <div
                      className="relative flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-2xl text-2xl font-black transition-all duration-300 group-hover:scale-110 group-hover:shadow-md"
                      style={{
                        background: `linear-gradient(135deg, ${meta.color}15, white)`,
                        border: `1.5px solid ${meta.color}30`,
                        color: meta.color,
                        fontFamily: "'Plus Jakarta Sans', sans-serif",
                      }}
                    >
                      {index < 3 ? meta.label : <span className="text-xl">{meta.label}</span>}
                    </div>

                    {/* Book cover */}
                    <div className="relative h-28 w-20 flex-shrink-0 overflow-hidden rounded-xl shadow-md transition-shadow duration-500 group-hover:shadow-lg">
                      <img
                        src={book.image || book.cover}
                        alt={book.title}
                        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
                      />
                      <div className="absolute inset-y-0 left-0 w-2 bg-gradient-to-r from-black/20 to-transparent" />
                      {/* Discount badge on cover */}
                      {bDiscount > 0 && (
                        <div className="absolute right-1 top-1 rounded-full bg-rose-500 px-1.5 py-0.5 text-[9px] font-black leading-none text-white shadow">
                          -{bDiscount}%
                        </div>
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex min-w-0 flex-1 flex-col">
                      <span className="mb-1.5 text-[10px] font-bold uppercase tracking-[0.15em] text-blue-600">
                        {book.catLabel || "Best Seller"}
                      </span>
                      <h3
                        className="line-clamp-2 text-lg font-bold leading-snug text-slate-900 transition-colors group-hover:text-blue-700 md:text-xl"
                        style={{ fontFamily: "var(--f-body)" }}
                      >
                        {book.title}
                      </h3>
                      <p className="mt-1 line-clamp-1 text-sm text-slate-500">{book.author}</p>
                      {/* mini accent bar */}
                      <div className="mt-2 h-0.5 w-0 rounded-full bg-blue-500 transition-all duration-500 group-hover:w-16" />
                    </div>

                    {/* Price + arrow */}
                    <div className="flex flex-shrink-0 items-center gap-4">
                      <div className="flex flex-col items-end">
                        <span className="text-xl font-extrabold text-blue-900" style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
                          {bPrice ? `${Number(bPrice).toLocaleString("vi-VN")}đ` : "—"}
                        </span>
                        {bDiscount > 0 && bOldPrice > 0 && (
                          <span className="text-xs text-slate-400 line-through">
                            {Number(bOldPrice).toLocaleString("vi-VN")}đ
                          </span>
                        )}
                      </div>
                      <div
                        className="flex h-9 w-9 items-center justify-center rounded-full border border-blue-100 bg-blue-50 transition-all duration-300 group-hover:border-blue-600 group-hover:bg-blue-600 group-hover:shadow-[0_4px_12px_rgba(37,99,235,0.3)]"
                      >
                        <ArrowRight size={15} className="text-blue-600 transition-all duration-300 group-hover:translate-x-0.5 group-hover:text-white" />
                      </div>
                    </div>
                  </Link>
                </motion.div>
              );
            })}
          </div>
        )}

        {/* CTA */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="mt-12 text-center"
        >
          <Link
            to="/category/all?sort=popular"
            className="group inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-8 py-3.5 text-sm font-bold uppercase tracking-widest text-slate-600 shadow-sm transition-all duration-300 hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 hover:shadow-md"
          >
            {t("home.viewAll")}
            <ArrowRight size={14} className="transition-transform group-hover:translate-x-1" />
          </Link>
        </motion.div>
      </div>
    </section>
  );
}



function HowItWorks() {
  const { t } = useTranslation();
  const STEP_ICONS = [
    // Compass / discover
    <svg key="discover" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} className="h-8 w-8">
      <circle cx="12" cy="12" r="10" />
      <path d="m16.24 7.76-2.12 6.36-6.36 2.12 2.12-6.36 6.36-2.12z" />
    </svg>,
    // Book open / choose
    <svg key="choose" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} className="h-8 w-8">
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
    </svg>,
    // Check badge / order
    <svg key="track" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} className="h-8 w-8">
      <path d="M9 12l2 2 4-4" />
      <path d="M7.86 2.07A2 2 0 0 1 9 2h6a2 2 0 0 1 1.14.07L21 4.93A2 2 0 0 1 22 6.7V12c0 5.5-4.6 8.3-10 10-5.4-1.7-10-4.5-10-10V6.7a2 2 0 0 1 1-1.77z" />
    </svg>,
  ];
  const STEP_ACCENTS = ["#3b82f6", "#8b5cf6", "#10b981"];

  const steps = [
    { num: "01", title: t("home.steps.discoverTitle"), desc: t("home.steps.discoverDesc") },
    { num: "02", title: t("home.steps.chooseTitle"), desc: t("home.steps.chooseDesc") },
    { num: "03", title: t("home.steps.trackTitle"), desc: t("home.steps.trackDesc") },
  ];

  return (
    <section className="relative overflow-hidden px-4 py-28 text-white md:px-8" style={{ background: "linear-gradient(180deg, #040d24 0%, #071035 50%, #040d24 100%)" }}>
      {/* Top border from QuoteSection */}
      <div className="absolute left-0 right-0 top-0 h-px bg-gradient-to-r from-transparent via-blue-500/40 to-transparent" />
      {/* Mesh radial glows */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-1/6 top-1/4 h-72 w-72 rounded-full bg-blue-600/10 blur-[80px]" />
        <div className="absolute right-1/6 bottom-1/4 h-72 w-72 rounded-full bg-violet-600/8 blur-[80px]" />
        <div className="absolute left-1/2 top-0 h-40 w-40 -translate-x-1/2 rounded-full bg-blue-400/8 blur-[60px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        <div className="mx-auto mb-20 max-w-2xl text-center">
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            className="mb-4 inline-block rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-400"
          >
            {t("home.process")}
          </motion.div>
          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="mb-4 text-4xl font-bold leading-tight text-white md:text-5xl"
            style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
          >
            {t("home.howItWorks")}
          </motion.h2>
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-lg font-light text-slate-400"
          >
            {t("home.processCopy")}
          </motion.p>
        </div>

        <div className="relative grid grid-cols-1 gap-6 md:grid-cols-3">
          {/* Animated connector line */}
          <div className="absolute left-[16.5%] right-[16.5%] top-[56px] hidden md:flex">
            <motion.div
              initial={{ scaleX: 0, opacity: 0 }}
              whileInView={{ scaleX: 1, opacity: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 1.2, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
              className="h-px flex-1 origin-left bg-gradient-to-r from-blue-600/60 via-violet-500/40 to-emerald-500/60"
            />
          </div>

          {steps.map((item, index) => (
            <motion.div
              key={item.num}
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.7, delay: index * 0.18, ease: [0.22, 1, 0.36, 1] }}
              className="group relative overflow-hidden rounded-3xl border border-white/[0.07] p-10 backdrop-blur-md transition-all duration-500 hover:-translate-y-3 hover:border-white/15 hover:shadow-[0_32px_64px_rgba(0,0,0,0.5)]"
              style={{ background: "linear-gradient(145deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.02) 100%)" }}
            >
              {/* Ambient glow on hover */}
              <div
                className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-700 group-hover:opacity-100"
                style={{ background: `radial-gradient(circle at 40% 40%, ${STEP_ACCENTS[index]}18, transparent 65%)` }}
              />
              {/* Top accent border */}
              <div className="absolute left-0 right-0 top-0 h-[2px] rounded-t-3xl opacity-0 transition-opacity duration-500 group-hover:opacity-100" style={{ background: `linear-gradient(to right, transparent, ${STEP_ACCENTS[index]}, transparent)` }} />

              {/* Floating icon */}
              <motion.div
                animate={{ y: [0, -8, 0] }}
                transition={{ duration: 3.5 + index * 0.4, ease: "easeInOut", repeat: Infinity }}
                className="mb-8 inline-flex"
              >
                <div
                  className="flex h-16 w-16 items-center justify-center rounded-2xl transition-all duration-500 group-hover:scale-110 group-hover:shadow-[0_0_30px_currentColor]"
                  style={{
                    background: `linear-gradient(135deg, ${STEP_ACCENTS[index]}25, ${STEP_ACCENTS[index]}10)`,
                    border: `1.5px solid ${STEP_ACCENTS[index]}50`,
                    color: STEP_ACCENTS[index],
                    boxShadow: `0 8px 24px ${STEP_ACCENTS[index]}25`,
                  }}
                >
                  {STEP_ICONS[index]}
                </div>
              </motion.div>

              {/* Number watermark */}
              <div
                className="pointer-events-none absolute -right-3 -top-4 select-none text-[6rem] font-black leading-none opacity-[0.06] transition-opacity duration-500 group-hover:opacity-[0.12]"
                style={{ color: STEP_ACCENTS[index], fontFamily: "'Plus Jakarta Sans', sans-serif" }}
              >
                {item.num}
              </div>

              <h3
                className="mb-3 text-xl font-bold text-white md:text-2xl"
                style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
              >
                {item.title}
              </h3>
              <p className="font-light leading-relaxed text-slate-400">{item.desc}</p>
            </motion.div>
          ))}
        </div>

        {/* CTA */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.7 }}
          className="mt-16 text-center"
        >
          <Link
            to="/category/all"
            className="group inline-flex items-center gap-3 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 px-10 py-4 font-bold tracking-wide text-white shadow-[0_0_40px_rgba(37,99,235,0.35)] transition-all duration-300 hover:scale-105 hover:shadow-[0_0_60px_rgba(37,99,235,0.55)]"
          >
            {t("home.exploreLibrary")}
            <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
          </Link>
        </motion.div>
      </div>
    </section>
  );
}

function AboutSection({ booksCount }) {
  const { t } = useTranslation();

  const collageImages = [
    { src: "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=800&auto=format&fit=crop", cls: "absolute -top-4 left-0 h-64 w-48 rotate-[-3deg]", zIndex: 3 },
    { src: "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=800&auto=format&fit=crop", cls: "absolute top-24 right-0 h-72 w-52 rotate-[4deg]", zIndex: 2 },
    { src: "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?q=80&w=800&auto=format&fit=crop", cls: "absolute bottom-0 left-12 h-60 w-44 rotate-[1.5deg]", zIndex: 4 },
  ];

  const bullets = t("home.aboutBullets", { returnObjects: true });

  return (
    <section className="relative overflow-hidden bg-white px-4 py-28 md:px-8">
      {/* Top border */}
      <div className="absolute left-0 right-0 top-0 h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent" />
      {/* Decorative corner glows */}
      <div className="pointer-events-none absolute -right-40 -top-40 h-[600px] w-[600px] rounded-full bg-blue-50/60 blur-[120px]" />
      <div className="pointer-events-none absolute -bottom-20 -left-20 h-80 w-80 rounded-full bg-indigo-50/80 blur-[80px]" />

      <div className="mx-auto grid max-w-7xl grid-cols-1 items-center gap-20 lg:grid-cols-2">
        {/* Left — Text */}
        <div className="relative z-10">
          <motion.div
            initial={{ opacity: 0, x: -8 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            className="mb-6 inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50 px-3.5 py-1.5 text-[10px] font-black uppercase tracking-[0.18em] text-blue-600"
          >
            <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
            {t("home.whyUs")}
          </motion.div>

          {/* Staggered title lines */}
          {t("home.aboutTitle").split("\n").length > 0 && (
            <div className="mb-8 overflow-hidden">
              {t("home.aboutTitle").split(" ").reduce((acc, word, i) => {
                // Group into lines of 3 words
                const lineIdx = Math.floor(i / 3);
                if (!acc[lineIdx]) acc[lineIdx] = [];
                acc[lineIdx].push(word);
                return acc;
              }, []).map((lineWords, lineIdx) => (
                <motion.div
                  key={lineIdx}
                  className="block overflow-hidden"
                  initial={{ opacity: 0, y: 40 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.7, delay: lineIdx * 0.12, ease: [0.22, 1, 0.36, 1] }}
                >
                  <span
                    className="block text-4xl font-bold leading-tight text-slate-900 md:text-5xl"
                    style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
                  >
                    {lineWords.join(" ")}
                  </span>
                </motion.div>
              ))}
            </div>
          )}

          <motion.p
            className="mb-8 text-lg font-light leading-relaxed text-slate-500"
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.3 }}
          >
            {t("home.aboutCopy")}
          </motion.p>

          <ul className="mb-10 space-y-3">
            {(Array.isArray(bullets) ? bullets : []).map((item, i) => (
              <motion.li
                key={item}
                className="flex items-center gap-3 text-slate-700"
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: 0.35 + i * 0.1, ease: [0.22, 1, 0.36, 1] }}
              >
                <motion.div
                  className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-blue-600"
                  whileInView={{ boxShadow: ["0 0 0px rgba(37,99,235,0)", "0 0 16px rgba(37,99,235,0.5)", "0 0 8px rgba(37,99,235,0.3)"] }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.8, delay: 0.5 + i * 0.1 }}
                >
                  <svg viewBox="0 0 12 10" fill="none" className="h-3 w-3">
                    <path d="M1 5l3 3 7-7" stroke="#fff" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </motion.div>
                <span className="font-medium">{item}</span>
              </motion.li>
            ))}
          </ul>

          {/* Animated stat counters */}
          <motion.div
            className="mb-10 grid grid-cols-3 gap-6 border-t border-slate-100 pt-8"
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.5 }}
          >
            {[
              { num: "12K+", label: t("home.readers") },
              { num: `${booksCount || 500}+`, label: t("home.titles") },
              { num: "99%", label: "Hài Lòng" },
            ].map(({ num, label }) => (
              <div key={label} className="text-center">
                <div
                  className="text-3xl font-black text-blue-700"
                  style={{ fontFamily: "'Plus Jakarta Sans', sans-serif", letterSpacing: "-0.03em" }}
                >
                  {num}
                </div>
                <div className="mt-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">{label}</div>
              </div>
            ))}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.6 }}
          >
            <Link
              to="/about"
              className="group inline-flex items-center gap-3 rounded-full bg-slate-900 px-8 py-4 font-bold tracking-wide text-white shadow-xl transition-all duration-300 hover:bg-blue-700 hover:shadow-[0_0_40px_rgba(37,99,235,0.4)]"
            >
              {t("home.learnMore")}
              <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
            </Link>
          </motion.div>
        </div>

        {/* Right — Photo collage */}
        <motion.div
          className="relative hidden h-[540px] lg:block"
          initial={{ opacity: 0, x: 40 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.9, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
        >
          {collageImages.map((img, i) => (
            <motion.div
              key={i}
              className={`overflow-hidden rounded-2xl shadow-[0_16px_48px_rgba(0,0,0,0.18)] ${img.cls}`}
              style={{ zIndex: img.zIndex }}
              initial={{ opacity: 0, y: 30 + i * 10, rotate: img.cls.includes("-3") ? -5 : img.cls.includes("4") ? 6 : 2 }}
              whileInView={{ opacity: 1, y: 0, rotate: img.cls.includes("-3") ? -3 : img.cls.includes("4") ? 4 : 1.5 }}
              viewport={{ once: true }}
              transition={{ duration: 0.9, delay: 0.3 + i * 0.18, ease: [0.22, 1, 0.36, 1] }}
              whileHover={{ scale: 1.04, zIndex: 10, boxShadow: "0 24px 60px rgba(0,0,0,0.25)" }}
            >
              <img
                src={img.src}
                alt="Aivira"
                className="h-full w-full object-cover transition-transform duration-700 hover:scale-105"
              />
              <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent" />
            </motion.div>
          ))}

          {/* Floating badge */}
          <motion.div
            className="absolute bottom-8 right-4 z-20 rounded-2xl border border-blue-200 bg-white/90 px-5 py-3 shadow-xl backdrop-blur-md"
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            whileInView={{ opacity: 1, scale: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.7, delay: 0.7, type: "spring", stiffness: 300, damping: 20 }}
            animate={{ y: [0, -6, 0] }}
          >
            <div className="text-center">
              <div className="text-2xl font-black text-blue-700" style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}>✦ Aivira</div>
              <div className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Knowledge On Every Page</div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}


function LatestNews() {
  const { t } = useTranslation();
  const [cursorPos, setCursorPos] = React.useState({ x: 0, y: 0 });
  const [cursorVisible, setCursorVisible] = React.useState(false);
  const featuredRef = React.useRef(null);

  function handleFeaturedMouseMove(e) {
    const rect = featuredRef.current?.getBoundingClientRect();
    if (!rect) return;
    setCursorPos({ x: e.clientX - rect.left, y: e.clientY - rect.top });
  }

  const posts = [
    {
      title: t("home.posts.one"),
      category: t("home.categories.business"),
      date: "02 Jun, 2026",
      image: "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?q=80&w=900&auto=format&fit=crop",
      featured: true,
    },
    {
      title: t("home.posts.two"),
      category: t("home.categories.wellness"),
      date: "28 May, 2026",
      image: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600&auto=format&fit=crop",
    },
    {
      title: t("home.posts.three"),
      category: t("home.categories.literature"),
      date: "20 May, 2026",
      image: "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=600&auto=format&fit=crop",
    },
  ];

  const featured = posts[0];
  const secondary = posts.slice(1);

  return (
    <section className="relative overflow-hidden bg-[#fafaf8] px-4 py-24 md:px-8">
      {/* Top separator */}
      <div className="absolute left-0 right-0 top-0 h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent" />
      <div className="pointer-events-none absolute right-0 top-0 h-96 w-96 rounded-full bg-blue-50/80 blur-3xl" />
      <div className="pointer-events-none absolute -left-20 bottom-0 h-64 w-64 rounded-full bg-indigo-50/60 blur-3xl" />

      <div className="mx-auto max-w-7xl">
        <SectionHead chip={t("home.insights")} title={t("home.blog")} link="/blog" />

        {/* Editorial layout: 60/40 */}
        <div className="mt-12 grid grid-cols-1 gap-6 lg:grid-cols-5">

          {/* Featured article — custom cursor */}
          <motion.article
            ref={featuredRef}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
            className="group relative col-span-1 cursor-none overflow-hidden rounded-3xl shadow-[0_8px_40px_rgba(0,0,0,0.1)] transition-shadow duration-500 hover:shadow-[0_24px_64px_rgba(0,0,0,0.2)] lg:col-span-3"
            onMouseMove={handleFeaturedMouseMove}
            onMouseEnter={() => setCursorVisible(true)}
            onMouseLeave={() => setCursorVisible(false)}
          >
            {/* Custom cursor orb */}
            <motion.div
              className="pointer-events-none absolute z-30 flex items-center justify-center rounded-full bg-white/90 text-[11px] font-black uppercase tracking-widest text-slate-900 shadow-xl"
              style={{ width: 72, height: 72, left: cursorPos.x - 36, top: cursorPos.y - 36, backdropFilter: "blur(8px)" }}
              animate={{ opacity: cursorVisible ? 1 : 0, scale: cursorVisible ? 1 : 0.3 }}
              transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            >
              Đọc
            </motion.div>

            {/* Image with inner-parallax zoom */}
            <div className="aspect-[4/3] overflow-hidden lg:aspect-[16/11]">
              <motion.img
                src={featured.image}
                alt={featured.title}
                className="h-full w-full object-cover"
                style={{ transformOrigin: "center" }}
                whileHover={{ scale: 1.08 }}
                transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
              />
            </div>

            {/* Gradient overlays */}
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/95 via-slate-950/30 to-transparent" />
            <div className="absolute inset-0 bg-blue-900/0 transition-colors duration-500 group-hover:bg-blue-900/15" />

            {/* Content */}
            <div className="absolute bottom-0 left-0 right-0 p-8">
              <div className="mb-3 flex items-center gap-3">
                <span className="rounded-full bg-blue-600 px-3 py-1 text-[0.65rem] font-bold uppercase tracking-widest text-white shadow-[0_0_12px_rgba(37,99,235,0.5)]">
                  {featured.category}
                </span>
                <span className="text-xs text-white/50">{featured.date}</span>
              </div>
              <h3
                className="text-2xl font-bold leading-snug text-white transition-colors group-hover:text-blue-200 md:text-3xl"
                style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
              >
                {featured.title}
              </h3>
              <motion.div
                className="mt-5 flex items-center gap-2 text-sm font-bold text-blue-400"
                initial={{ opacity: 0, y: 8 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: 0.5 }}
              >
                <span className="h-px w-8 bg-blue-400/60" />
                {t("home.readMore") || "Đọc thêm"}
              </motion.div>
            </div>
          </motion.article>

          {/* Secondary articles */}
          <div className="col-span-1 flex flex-col gap-4 lg:col-span-2">
            {secondary.map((post, index) => (
              <motion.article
                key={post.title}
                initial={{ opacity: 0, x: 24 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: 0.1 + index * 0.14, ease: [0.22, 1, 0.36, 1] }}
                className="group flex flex-1 cursor-pointer gap-5 overflow-hidden rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition-all duration-350 hover:-translate-y-1 hover:border-blue-200 hover:shadow-[0_12px_40px_rgba(37,99,235,0.1)]"
                whileHover={{ x: 2 }}
              >
                {/* Thumbnail with inner zoom */}
                <div className="relative h-28 w-24 flex-shrink-0 overflow-hidden rounded-xl shadow-md">
                  <motion.img
                    src={post.image}
                    alt={post.title}
                    className="h-full w-full object-cover"
                    whileHover={{ scale: 1.12 }}
                    transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                  />
                  {/* Category color stripe on image */}
                  <div className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-600 to-indigo-500 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
                </div>

                {/* Text */}
                <div className="flex min-w-0 flex-col justify-center gap-2">
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-[0.6rem] font-bold uppercase tracking-wider text-blue-700 transition-colors duration-300 group-hover:bg-blue-100">
                      {post.category}
                    </span>
                    <span className="text-[0.65rem] text-slate-400">{post.date}</span>
                  </div>
                  <h3
                    className="line-clamp-2 text-sm font-bold leading-snug text-slate-900 transition-colors duration-300 group-hover:text-blue-700 md:text-base"
                    style={{ fontFamily: "'Plus Jakarta Sans', sans-serif" }}
                  >
                    {post.title}
                  </h3>
                  <motion.span
                    className="inline-flex w-fit items-center gap-1 text-xs font-bold text-blue-500"
                    initial={{ opacity: 0, x: -8 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: 0.4 + index * 0.1 }}
                  >
                    <span className="opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                      Đọc thêm <ArrowRight size={11} className="inline" />
                    </span>
                  </motion.span>
                </div>
              </motion.article>
            ))}

            {/* View all blog link card */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: 0.4 }}
              className="group flex flex-1 cursor-pointer items-center justify-between rounded-2xl border border-dashed border-blue-200 bg-blue-50/50 p-5 transition-all duration-300 hover:border-blue-300 hover:bg-blue-50"
            >
              <span className="text-sm font-bold text-blue-700">Xem tất cả bài viết</span>
              <ArrowRight size={16} className="text-blue-500 transition-transform group-hover:translate-x-1" />
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  );
}


function SectionHead({ chip, title, link, dark = false }) {
  const { t } = useTranslation();

  return (
    <div className="mb-12 flex flex-col justify-between gap-4 md:flex-row md:items-end">
      <div>
        {/* Chip */}
        <div
          className="mb-3 inline-flex items-center gap-1.5"
          style={{
            fontSize: "0.65rem",
            fontWeight: 800,
            letterSpacing: "0.18em",
            textTransform: "uppercase",
            color: dark ? "rgba(147,197,253,0.9)" : "#2563eb",
          }}
        >
          <span
            className="inline-block h-[6px] w-[6px] rounded-full"
            style={{ background: dark ? "#60a5fa" : "#2563eb" }}
          />
          {chip}
        </div>
        {/* Title */}
        <h2
          className={`text-4xl font-bold leading-tight md:text-5xl ${dark ? "text-white" : "text-slate-900"}`}
          style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
        >
          {title}
        </h2>
      </div>

      {link && (
        <Link
          to={link}
          className={`group flex flex-shrink-0 items-center gap-2 text-xs font-bold uppercase tracking-[0.12em] transition-colors ${
            dark ? "text-white/40 hover:text-blue-300" : "text-slate-400 hover:text-blue-600"
          }`}
        >
          <span className="relative">
            {t("home.viewAll")}
            <span className="absolute -bottom-px left-0 h-px w-0 bg-blue-500 transition-all duration-300 group-hover:w-full" />
          </span>
          <ArrowRight size={13} className="transition-transform group-hover:translate-x-0.5" />
        </Link>
      )}
    </div>
  );
}
