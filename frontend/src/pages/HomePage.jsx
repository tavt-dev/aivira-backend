import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { motion } from "motion/react";

import { getStorefrontHome } from "../api/storefrontApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, normalizeCategoryHighlight } from "../utils/mappers.js";

const CATEGORY_FALLBACK_IMAGES = [
  "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1519682337058-a94d519337bc?q=80&w=600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1499750310107-5fef28a66643?q=80&w=600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=600&auto=format&fit=crop",
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
                className="rounded-full bg-white px-8 py-4 font-bold tracking-wide text-slate-900 shadow-[0_0_40px_rgba(255,255,255,0.2)] transition-all duration-300 hover:scale-105 hover:bg-slate-200"
              >
                {t("home.exploreLibrary")}
              </Link>
              <Link
                to="/cart"
                className="rounded-full border border-white/20 bg-white/5 px-8 py-4 font-bold tracking-wide backdrop-blur transition-all duration-300 hover:bg-white/10"
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
                <strong className="block font-serif text-2xl">{books.length}+</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
                  {t("home.titles")}
                </span>
              </div>
              <div>
                <strong className="block font-serif text-2xl">COD</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
                  {t("home.supported")}
                </span>
              </div>
              <div>
                <strong className="block font-serif text-2xl">VNPay</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
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
    <div className="flex overflow-hidden whitespace-nowrap border-y border-white/5 bg-slate-900 py-4">
      <motion.div
        animate={{ x: ["0%", "-50%"] }}
        transition={{ ease: "linear", duration: 25, repeat: Infinity }}
        className="flex items-center space-x-16 text-sm font-bold uppercase tracking-[0.3em] text-slate-500"
      >
        {Array.from({ length: 4 }).map((_, index) => (
          <span key={index}>{text}</span>
        ))}
      </motion.div>
    </div>
  );
}

function CategoryShowcase({ categories, loading }) {
  const { t } = useTranslation();

  return (
    <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
      <SectionHead chip={t("home.explore")} title={t("home.categoryHighlights")} link="/category/all" />
      {loading ? (
        <CategorySkeleton />
      ) : categories.length ? (
      <div className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        {categories.slice(0, 6).map((category, index) => (
          <motion.div
            key={category.id || category.slug}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-50px" }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
          >
            <Link
              to={`/category/${category.slug}`}
              className="group relative block aspect-[4/5] w-full overflow-hidden rounded-2xl bg-slate-200 shadow-sm transition-all duration-500 hover:shadow-2xl"
            >
              <img
                src={category.imageUrl || CATEGORY_FALLBACK_IMAGES[index % CATEGORY_FALLBACK_IMAGES.length]}
                alt={category.categoryName}
                className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
              />
              <div className="absolute inset-0 bg-gradient-to-b from-black/20 via-black/10 to-black/80" />
              <div className="absolute left-6 top-6 z-10 flex h-10 w-10 items-center justify-center rounded-full border border-white/30 font-serif font-bold italic text-white backdrop-blur-md transition-transform duration-500 group-hover:scale-110">
                0{index + 1}
              </div>
              <div className="absolute bottom-6 left-6 right-6 z-10 transition-transform duration-500 group-hover:-translate-y-2">
                <h3 className="mb-1 font-serif text-2xl font-bold text-white">{category.categoryName}</h3>
                <div className="flex items-center justify-between">
                  <p className="line-clamp-2 text-sm tracking-wide text-white/70">
                    {category.bookCount ? t("home.categoryBookCount", { count: category.bookCount }) : category.description}
                  </p>
                  <span className="flex h-8 w-8 -translate-x-4 items-center justify-center rounded-full bg-white text-slate-900 opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100">
                    <span className="text-lg leading-none">-&gt;</span>
                  </span>
                </div>
              </div>
            </Link>
          </motion.div>
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
    <section className="relative flex items-center justify-center overflow-hidden bg-blue-900 px-4 py-32 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_white_1px,_transparent_1px)] opacity-10 [background-size:24px_24px]" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 1 }}
        className="relative z-10 mx-auto max-w-4xl text-center"
      >
        <div className="mb-8 inline-block h-10 overflow-hidden font-serif text-8xl leading-none text-blue-400/30">"</div>
        <p className="mb-8 font-serif text-3xl font-medium leading-tight md:text-5xl">
          {t("home.quote")}
        </p>
        <div className="text-sm font-bold uppercase tracking-[0.3em] text-blue-300">
          {t("home.philosophy")}
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
    <div className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="aspect-[4/5] animate-pulse rounded-2xl bg-slate-200" />
      ))}
    </div>
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
    <section className="mt-12 rounded-t-[3rem] bg-slate-900 px-4 py-24 text-white md:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto mb-16 max-w-2xl text-center">
          <div className="mb-4 inline-block rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-400">
            {t("home.process")}
          </div>
          <h2 className="mb-4 font-serif text-4xl font-bold md:text-5xl">{t("home.howItWorks")}</h2>
          <p className="text-slate-400">{t("home.processCopy")}</p>
        </div>

        <div className="relative grid grid-cols-1 gap-8 md:grid-cols-3">
          <div className="absolute left-[15%] right-[15%] top-[40px] hidden h-px bg-gradient-to-r from-transparent via-blue-500/30 to-transparent md:block" />

          {steps.map((item, index) => (
            <motion.div
              key={item.num}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.2 }}
              className="group relative rounded-2xl border border-white/5 bg-slate-800/50 p-8 backdrop-blur transition-colors hover:border-blue-500/30"
            >
              <div className="relative z-10 mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full border border-white/10 bg-slate-900 font-serif text-3xl font-bold text-white transition-all duration-500 group-hover:scale-110 group-hover:border-blue-500/50 md:mx-0">
                {item.num}
              </div>
              <h3 className="mb-3 text-center text-2xl font-bold md:text-left">{item.title}</h3>
              <p className="text-center leading-relaxed text-slate-400 md:text-left">{item.desc}</p>
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
          <div className="relative aspect-square overflow-hidden rounded-3xl shadow-2xl md:aspect-[4/5]">
            <img
              src="https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=800&auto=format&fit=crop"
              alt="Aivira Library"
              className="h-full w-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-tr from-slate-900/80 to-transparent" />
            <div className="absolute bottom-8 left-8 right-8 flex items-center justify-around rounded-2xl border border-white/20 bg-white/10 p-6 text-center shadow-xl backdrop-blur-md">
              <div>
                <strong className="mb-1 block font-serif text-3xl text-white">12K+</strong>
                <span className="text-xs font-bold uppercase tracking-widest text-slate-300">{t("home.readers")}</span>
              </div>
              <div className="h-12 w-px bg-white/20" />
              <div>
                <strong className="mb-1 block font-serif text-3xl text-white">{booksCount}+</strong>
                <span className="text-xs font-bold uppercase tracking-widest text-slate-300">{t("home.titles")}</span>
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
    <div className="flex flex-col justify-between gap-4 border-b border-slate-200 pb-6 md:flex-row md:items-end">
      <div>
        <div className="mb-2 inline-block rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
          {chip}
        </div>
        <h2 className="font-serif text-3xl font-bold text-slate-900 md:text-4xl">{title}</h2>
      </div>

      {link && (
        <Link
          to={link}
          className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-slate-500 transition-colors hover:text-blue-600"
        >
          {t("home.viewAll")} <span className="text-lg">-&gt;</span>
        </Link>
      )}
    </div>
  );
}
