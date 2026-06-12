import React, { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ArrowRight } from "lucide-react";

import { getStorefrontHome } from "../api/storefrontApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, normalizeCategoryHighlight } from "../utils/mappers.js";

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
    <div className="tw-home w-full overflow-hidden bg-slate-50">
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
              className="font-serif text-5xl font-bold leading-[1.1] tracking-tight md:text-7xl"
            >
              {t("home.title1")} <br />
              {t("home.title2")}{" "}
              <span className="bg-gradient-to-r from-blue-400 to-sky-200 bg-clip-text text-transparent">
                {t("home.titleWorld")}
              </span>
              <br />
              <em className="mt-2 block font-serif text-3xl font-light italic text-slate-400 md:text-5xl">
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
                className="group relative overflow-hidden rounded-full px-8 py-4 font-bold tracking-wide shadow-[0_0_40px_rgba(37,99,235,0.4)] transition-all duration-300 hover:scale-105 hover:shadow-[0_0_60px_rgba(56,189,248,0.6)]"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-blue-400 transition-transform duration-500 group-hover:scale-110" />
                <span className="relative text-white">{t("home.exploreLibrary")}</span>
              </Link>
              <Link
                to="/cart"
                className="rounded-full border border-white/20 bg-white/5 px-8 py-4 font-bold tracking-wide text-white backdrop-blur transition-all duration-300 hover:bg-white/10"
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

      <section className="bg-white px-4 py-24 md:px-8">
        <div className="mx-auto max-w-7xl">
          <SectionHead chip={t("home.backendCatalog")} title={t("home.weeklyPicks")} link="/category/all" />
          {message && <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{message}</div>}
          {loading ? (
            <BookGridSkeleton count={4} />
          ) : (
            <BookGrid books={featured.slice(0, 4)} emptyMessage={t("home.noFeaturedBooks")} />
          )}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
        <SectionHead chip={t("home.collection")} title={t("home.newArrivals")} link="/category/all?sort=newest" />
        {loading ? (
          <BookGridSkeleton />
        ) : (
          <BookGrid books={newArrivals.slice(0, 8)} emptyMessage={t("home.noNewArrivals")} />
        )}
      </section>

      <section className="bg-white px-4 py-24 md:px-8">
        <div className="mx-auto max-w-7xl">
          <SectionHead chip={t("home.featuredBooks")} title={t("home.bestsellingBooks")} link="/category/all?sort=popular" />
          {loading ? (
            <BookGridSkeleton />
          ) : (
            <BookGrid
              books={bestselling.slice(0, 8).map((book) => ({ ...book, badge: t("home.bestsellerBadge") }))}
              emptyMessage={t("home.noBestsellers")}
            />
          )}
        </div>
      </section>

      <QuoteSection />

      <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
        <SectionHead chip={t("home.collection")} title={t("home.allBooks")} link="/category/all" />
        {loading ? (
          <BookGridSkeleton />
        ) : (
          <BookGrid books={books.slice(0, 12)} emptyMessage={t("home.noBooks")} />
        )}
      </section>

      <HowItWorks />

      <AboutSection booksCount={books.length} />

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

    getStorefrontHome({ signal: controller.signal })
      .then((payload) => {
        const featured = (payload?.featuredBooks || []).map((row) => normalizeBook(row));
        const newArrivals = (payload?.newArrivals || []).map((row) => normalizeBook(row));
        const bestselling = (payload?.bestsellingBooks || []).map((row) => normalizeBook(row));
        const categoryHighlights = (payload?.categoryHighlights || [])
          .map((row) => normalizeCategoryHighlight(row))
          .filter(Boolean);

        setState({
          featured,
          newArrivals,
          bestselling,
          categoryHighlights,
          books: uniqueBooks([...featured, ...newArrivals, ...bestselling])
        });
      })
      .catch((error) => {
        if (error.name === "AbortError") return;
        setState({
          featured: [],
          newArrivals: [],
          bestselling: [],
          categoryHighlights: [],
          books: []
        });
        setMessage(error.message || t("home.storefrontFailed"));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => {
      controller.abort();
    };
  }, [t]);

  return { ...state, loading, message };
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
      className="relative flex overflow-hidden whitespace-nowrap border-y border-white/5 bg-slate-950 py-4"
      style={{
        maskImage: "linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)",
        WebkitMaskImage: "linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)",
      }}
    >
      <motion.div
        animate={{ x: ["0%", "-50%"] }}
        transition={{ ease: "linear", duration: 28, repeat: Infinity }}
        className="flex items-center text-sm font-bold uppercase tracking-[0.3em] text-blue-500/70"
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
              <h3 className="font-serif text-base font-bold leading-tight text-white/95 md:text-lg">
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
  return (
    <section className="relative flex items-center justify-center overflow-hidden bg-slate-950 px-4 py-32 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-900/40 via-slate-950 to-slate-950" />
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 1 }}
        className="relative z-10 mx-auto max-w-4xl text-center"
      >
        <div className="mb-4 inline-block font-serif text-8xl leading-none text-blue-500/30">&quot;</div>
        <p className="mb-10 font-serif text-4xl font-light italic leading-relaxed md:text-5xl">
          {t("home.quote")}
        </p>
        <div className="inline-flex items-center gap-4 text-xs font-bold uppercase tracking-[0.3em] text-blue-400">
          <span className="h-px w-8 bg-blue-400/50" />
          {t("home.philosophy")}
          <span className="h-px w-8 bg-blue-400/50" />
        </div>
      </motion.div>
    </section>
  );
}

function BookGrid({ books, emptyMessage }) {
  if (!books.length) {
    return <HomeEmptyState title={emptyMessage} />;
  }

  return (
    <div className="mt-12 grid grid-cols-2 gap-x-6 gap-y-12 md:grid-cols-3 lg:grid-cols-4">
      {books.map((book, index) => (
        <motion.div
          key={book.id}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "0px 0px -50px 0px" }}
          transition={{ duration: 0.5, delay: (index % 4) * 0.1 }}
        >
          <BookCard book={book} />
        </motion.div>
      ))}
    </div>
  );
}

function BookGridSkeleton({ count = 8 }) {
  return (
    <div className="mt-12 grid grid-cols-2 gap-x-6 gap-y-12 md:grid-cols-3 lg:grid-cols-4">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
          <div className="aspect-[2/3] animate-pulse rounded-xl bg-slate-100" />
          <div className="mt-5 h-3 w-20 animate-pulse rounded bg-slate-100" />
          <div className="mt-3 h-5 w-full animate-pulse rounded bg-slate-100" />
          <div className="mt-2 h-4 w-2/3 animate-pulse rounded bg-slate-100" />
        </div>
      ))}
    </div>
  );
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

function HowItWorks() {
  const { t } = useTranslation();
  const steps = [
    {
      num: "01",
      title: t("home.steps.discoverTitle"),
      desc: t("home.steps.discoverDesc"),
    },
    {
      num: "02",
      title: t("home.steps.chooseTitle"),
      desc: t("home.steps.chooseDesc"),
    },
    {
      num: "03",
      title: t("home.steps.trackTitle"),
      desc: t("home.steps.trackDesc"),
    },
  ];

  return (
    <section className="mt-12 rounded-t-[3rem] bg-slate-950 px-4 py-24 text-white shadow-[0_-20px_40px_rgba(0,0,0,0.1)] md:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto mb-20 max-w-2xl text-center">
          <div className="mb-4 inline-block rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-400">
            {t("home.process")}
          </div>
          <h2 className="mb-4 font-serif text-4xl font-bold md:text-5xl">{t("home.howItWorks")}</h2>
          <p className="text-lg font-light text-slate-400">{t("home.processCopy")}</p>
        </div>

        <div className="relative grid grid-cols-1 gap-8 md:grid-cols-3">
          <div className="absolute left-[15%] right-[15%] top-[48px] hidden h-px bg-gradient-to-r from-transparent via-blue-500/30 to-transparent md:block" />

          {steps.map((item, index) => (
            <motion.div
              key={item.num}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.2 }}
              className="group relative overflow-hidden rounded-3xl border border-white/5 bg-slate-900/50 p-10 backdrop-blur transition-all duration-500 hover:-translate-y-2 hover:border-blue-500/30 hover:bg-slate-800/80 hover:shadow-[0_20px_40px_rgba(37,99,235,0.1)]"
            >
              <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-blue-500/5 via-transparent to-transparent opacity-0 transition-opacity duration-500 group-hover:opacity-100" />
              
              <div className="relative z-10 mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full border border-white/10 bg-slate-950 font-display text-4xl text-white shadow-inner transition-all duration-500 group-hover:border-blue-500/50 group-hover:text-blue-400 group-hover:shadow-[0_0_30px_rgba(37,99,235,0.2)] md:mx-0">
                {item.num}
              </div>
              <h3 className="mb-4 text-center font-serif text-2xl font-bold md:text-left">{item.title}</h3>
              <p className="text-center font-light leading-relaxed text-slate-400 md:text-left">{item.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

function AboutSection({ booksCount }) {
  const { t } = useTranslation();
  return (
    <section className="relative overflow-hidden border-t border-slate-800 bg-slate-900 px-4 py-24 text-white md:px-8">
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-10 mix-blend-overlay" />
      <div className="pointer-events-none absolute right-0 top-0 h-full w-1/2 bg-gradient-to-l from-blue-900/20 to-transparent" />

      <div className="mx-auto grid max-w-7xl grid-cols-1 items-center gap-16 md:grid-cols-2">
        <motion.div
          initial={{ opacity: 0, x: -40 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="relative z-10"
        >
          <div className="mb-6 inline-block rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-white">
            {t("home.whyUs")}
          </div>
          <h2 className="mb-6 font-serif text-4xl font-bold md:text-5xl">{t("home.aboutTitle")}</h2>
          <p className="mb-8 text-lg font-light leading-relaxed text-slate-300">
            {t("home.aboutCopy")}
          </p>

          <ul className="mb-8 space-y-4">
            {t("home.aboutBullets", { returnObjects: true }).map((item) => (
              <li key={item} className="flex items-center gap-3 font-medium text-slate-300">
                <div className="flex h-6 w-6 items-center justify-center rounded-full border border-blue-500/30 bg-blue-500/20 text-blue-400">
                  <span className="text-xs font-bold leading-none">OK</span>
                </div>
                {item}
              </li>
            ))}
          </ul>

          <Link
            to="/about"
            className="inline-block rounded-full bg-blue-600 px-8 py-4 font-bold tracking-wide text-white shadow-lg shadow-blue-500/25 transition-colors hover:bg-blue-500"
          >
            {t("home.learnMore")}
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="relative"
        >
          <div className="relative aspect-square overflow-hidden rounded-[2.5rem] shadow-2xl md:aspect-[4/5]">
            <img
              src="https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=800&auto=format&fit=crop"
              alt="Aivira Library"
              className="h-full w-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-transparent to-transparent" />
            <div className="absolute bottom-8 left-8 right-8 flex items-center justify-around rounded-2xl border border-white/20 bg-white/10 p-6 text-center shadow-[0_8px_32px_rgba(0,0,0,0.3)] backdrop-blur-xl">
              <div>
                <strong className="mb-1 block font-display text-4xl tracking-wide text-white">12K+</strong>
                <span className="text-[10px] font-bold uppercase tracking-widest text-slate-300">{t("home.readers")}</span>
              </div>
              <div className="h-16 w-px bg-gradient-to-b from-transparent via-white/30 to-transparent" />
              <div>
                <strong className="mb-1 block font-display text-4xl tracking-wide text-white">{booksCount}+</strong>
                <span className="text-[10px] font-bold uppercase tracking-widest text-slate-300">{t("home.titles")}</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

function LatestNews() {
  const { t } = useTranslation();
  const posts = [
    {
      title: t("home.posts.one"),
      category: t("home.categories.business"),
      date: "02 Jun, 2026",
      image: "https://images.unsplash.com/photo-1542361345-89e58247f2d5?q=80&w=600&auto=format&fit=crop",
    },
    {
      title: t("home.posts.two"),
      category: t("home.categories.wellness"),
      date: "28 May, 2026",
      image: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?q=80&w=600&auto=format&fit=crop",
    },
    {
      title: t("home.posts.three"),
      category: t("home.categories.literature"),
      date: "20 May, 2026",
      image: "https://images.unsplash.com/photo-1474932430478-367d16b99031?q=80&w=600&auto=format&fit=crop",
    },
  ];

  return (
    <section className="border-t border-slate-200 bg-slate-50 px-4 py-24 md:px-8">
      <div className="mx-auto max-w-7xl">
        <SectionHead chip={t("home.insights")} title={t("home.blog")} link="/blog" />
        <div className="mt-12 grid grid-cols-1 gap-8 md:grid-cols-3">
          {posts.map((post, index) => (
            <motion.article
              key={post.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.15 }}
              className="group flex h-full cursor-pointer flex-col rounded-2xl border border-slate-100 bg-white p-4 transition-all duration-300 hover:shadow-xl"
            >
              <div className="mb-5 aspect-[16/10] w-full overflow-hidden rounded-xl">
                <img
                  src={post.image}
                  alt={post.title}
                  className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
                />
              </div>
              <div className="mb-3 flex items-center gap-3 text-xs font-medium text-slate-500">
                <span className="rounded bg-blue-50 px-2 py-1 font-bold uppercase tracking-wider text-blue-600">
                  {post.category}
                </span>
                <span className="h-1 w-1 rounded-full bg-slate-300" />
                <span>{post.date}</span>
              </div>
              <h3 className="line-clamp-2 font-serif text-lg font-bold leading-snug text-slate-900 transition-colors group-hover:text-blue-600">
                {post.title}
              </h3>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHead({ chip, title, link }) {
  const { t } = useTranslation();
  return (
    <div className="mb-10 flex flex-col justify-between gap-4 md:flex-row md:items-end">
      <div className="flex items-start gap-4">
        {/* Accent bar */}
        <div
          className="mt-1 w-0.5 flex-shrink-0 self-stretch rounded-full"
          style={{
            background: "linear-gradient(180deg, #3b82f6 0%, #93c5fd 100%)",
            minHeight: 36
          }}
        />
        <div>
          <div
            className="mb-2 inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-3 py-1"
            style={{
              fontSize: "0.68rem",
              fontWeight: 800,
              letterSpacing: "0.12em",
              textTransform: "uppercase",
              color: "#2563eb"
            }}
          >
            {chip}
          </div>
          <h2 className="font-serif text-3xl font-bold text-slate-900 md:text-4xl">{title}</h2>
        </div>
      </div>

      {link && (
        <Link
          to={link}
          className="group flex flex-shrink-0 items-center gap-1.5 text-sm font-bold uppercase tracking-wider text-slate-400 transition-colors hover:text-blue-600"
          style={{ letterSpacing: "0.1em" }}
        >
          <span className="relative">
            {t("home.viewAll")}
            <span className="absolute -bottom-px left-0 h-px w-0 bg-blue-500 transition-all duration-300 group-hover:w-full" />
          </span>
          <ArrowRight size={14} className="transition-transform group-hover:translate-x-0.5" />
        </Link>
      )}
    </div>
  );
}
