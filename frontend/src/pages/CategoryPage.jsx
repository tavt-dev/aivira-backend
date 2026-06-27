import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  BookOpen, ChevronRight, Filter, Grid3X3, Home,
  Layers, LayoutList, Search, SlidersHorizontal, Sparkles, TrendingUp, X,
} from "lucide-react";

import { getCategories, getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import { Button, Drawer, Notice, Pagination } from "../components/ui/index.jsx";
import { formatVND } from "../utils/formatters.js";
import { getTheme } from "../utils/theme.js";
import { normalizeBook, normalizeCategory, pageMeta as readPageMeta, pageRows } from "../utils/mappers.js";

/* ── Constants ───────────────────────────────────────── */
const DEFAULT_PAGE = 1;
const DEFAULT_SIZE = 12;
const DEFAULT_SORT = "newest";
const PAGE_SIZES   = [12, 24, 48];

/* ── Accent palette per category index ──────────────── */
const CAT_ACCENTS = [
  { color:"#4f6ef7", glow:"rgba(79,110,247,0.4)"   },
  { color:"#f0a500", glow:"rgba(240,165,0,0.4)"    },
  { color:"#ec4899", glow:"rgba(236,72,153,0.4)"   },
  { color:"#10d98a", glow:"rgba(16,217,138,0.4)"   },
  { color:"#a78bfa", glow:"rgba(167,139,250,0.4)"  },
  { color:"#38bdf8", glow:"rgba(56,189,248,0.4)"   },
];

/* ── Ambient particles ───────────────────────────────── */
const PARTICLES = [
  { left:"7%",  top:"18%", size:3, dur:"4.2s", delay:"0s",   op:0.55 },
  { left:"90%", top:"14%", size:2, dur:"5.6s", delay:"0.8s", op:0.40 },
  { left:"20%", top:"70%", size:2, dur:"3.9s", delay:"1.2s", op:0.45 },
  { left:"78%", top:"58%", size:3, dur:"5.0s", delay:"0.4s", op:0.50 },
  { left:"48%", top:"88%", size:2, dur:"6.2s", delay:"1.9s", op:0.38 },
  { left:"33%", top:"8%",  size:2, dur:"4.5s", delay:"2.4s", op:0.42 },
  { left:"93%", top:"82%", size:2, dur:"5.8s", delay:"1.0s", op:0.38 },
  { left:"60%", top:"32%", size:3, dur:"3.7s", delay:"0.6s", op:0.48 },
];

/* ── Design token helpers ─────────────────────────────── */
function tokens(isDark) {
  if (isDark) return {
    pageBg:     "#07091a",
    surface1:   "rgba(12,17,48,0.92)",
    surface2:   "rgba(18,24,64,0.75)",
    heroBg:     "linear-gradient(135deg,rgba(12,17,48,0.98) 0%,rgba(8,12,35,0.99) 60%,rgba(15,10,40,0.98) 100%)",
    heroTopLine:"linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
    border:     "rgba(255,255,255,0.065)",
    borderMid:  "rgba(255,255,255,0.1)",
    text1:      "#e8eeff",
    text2:      "#8892b0",
    text3:      "#4a5578",
    accent:     "#4f6ef7",
    accentGlow: "rgba(79,110,247,0.35)",
    particle:   "#4f6ef7",
    inputBg:    "rgba(255,255,255,0.05)",
    skeletonA:  "rgba(255,255,255,0.03)",
    skeletonB:  "rgba(79,110,247,0.09)",
    skeletonC:  "rgba(167,139,250,0.07)",
    sortBg:     "rgba(18,24,64,0.75)",
    orb1:       "rgba(79,110,247,0.22)",
    orb2:       "rgba(240,165,0,0.14)",
    orb3:       "rgba(167,139,250,0.13)",
  };
  return {
    pageBg:     "#f5f3ef",
    surface1:   "rgba(255,253,248,0.92)",
    surface2:   "rgba(255,253,248,0.80)",
    heroBg:     "linear-gradient(135deg,rgba(15,23,42,0.97) 0%,rgba(22,30,58,0.98) 60%,rgba(20,15,50,0.97) 100%)",
    heroTopLine:"linear-gradient(90deg,transparent,#6d8fff 40%,#c4b5fd 70%,transparent)",
    border:     "rgba(15,23,42,0.08)",
    borderMid:  "rgba(15,23,42,0.14)",
    text1:      "#0f172a",
    text2:      "#475569",
    text3:      "#94a3b8",
    accent:     "#2563eb",
    accentGlow: "rgba(37,99,235,0.28)",
    particle:   "#7fa0ff",
    inputBg:    "rgba(15,23,42,0.045)",
    skeletonA:  "rgba(15,23,42,0.05)",
    skeletonB:  "rgba(37,99,235,0.07)",
    skeletonC:  "rgba(139,92,246,0.05)",
    sortBg:     "rgba(255,253,248,0.8)",
    orb1:       "rgba(79,110,247,0.12)",
    orb2:       "rgba(240,165,0,0.10)",
    orb3:       "rgba(167,139,250,0.09)",
  };
}

/* ─────────────────────────────────────────────────────
   useTheme — subscribes to aivira-theme + html.dark
───────────────────────────────────────────────────── */
function useTheme() {
  const [isDark, setIsDark] = useState(() => getTheme() === "dark");
  useEffect(() => {
    const sync = () => setIsDark(getTheme() === "dark");
    window.addEventListener("aivira-theme", sync);
    return () => window.removeEventListener("aivira-theme", sync);
  }, []);
  return isDark;
}

/* ═══════════════════════════════════════════════════════
   MAIN PAGE
═══════════════════════════════════════════════════════ */
export default function CategoryPage() {
  const { t } = useTranslation();
  const isDark = useTheme();
  const tk = tokens(isDark);
  const { slug = "all" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const searchKey = searchParams.toString();
  const filters   = useMemo(() => readFilters(new URLSearchParams(searchKey)), [searchKey]);
  const [form, setForm]             = useState(filters);
  const [books, setBooks]           = useState([]);
  const [categories, setCategories] = useState([{ id:"all", slug:"all", label:t("catalog.titleAll") }]);
  const [pageMeta, setPageMeta]     = useState(emptyPageMeta(filters));
  const [loading, setLoading]       = useState(true);
  const [message, setMessage]       = useState("");
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const [viewMode, setViewMode]     = useState("grid");

  const activeCategory = categories.find(c => c.slug === slug);
  const title = filters.keyword
    ? t("catalog.searchTitle", { search: filters.keyword })
    : activeCategory?.label || activeCategory?.categoryName || t("catalog.booksFallback");
  const hasActiveFilters = hasFilters(filters);

  useEffect(() => { setForm(filters); }, [filters]);

  useEffect(() => {
    getCategories()
      .then(rows => {
        const list = pageRows(rows).map(normalizeCategory).filter(Boolean);
        setCategories([{ id:"all", slug:"all", label:t("catalog.titleAll") }, ...list]);
      })
      .catch(err => setMessage(err.message || t("catalog.categoriesFailed")));
  }, [t]);

  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true); setMessage("");
    getProducts({
      keyword: filters.keyword, categorySlug: slug !== "all" ? slug : "",
      author: filters.author, publisher: filters.publisher, isbn: filters.isbn,
      minPrice: filters.minPrice, maxPrice: filters.maxPrice,
      available: filters.available === "" ? "" : filters.available === "true",
      sort: filters.sort, page: Math.max(filters.page - 1, 0), size: filters.size,
    }, { signal: ctrl.signal })
      .then(page => {
        const rows = pageRows(page);
        if (page?.totalPages > 0 && filters.page > page.totalPages && rows.length === 0) {
          setSearchParams(buildSearchParams(filters, { page: page.totalPages }), { replace:true });
          return;
        }
        setBooks(rows.map(r => normalizeBook(r)));
        setPageMeta(readPageMeta(page, { page:filters.page, size:filters.size }));
      })
      .catch(err => {
        if (err.name === "AbortError") return;
        setBooks([]); setPageMeta(emptyPageMeta(filters));
        setMessage(err.message || t("catalog.productsFailed"));
      })
      .finally(() => { if (!ctrl.signal.aborted) setLoading(false); });
    return () => ctrl.abort();
  }, [filters, setSearchParams, slug, t]);

  function applyFilters(e) { e?.preventDefault(); setSearchParams(buildSearchParams(form, { page:DEFAULT_PAGE }), { replace:false }); setMobileFiltersOpen(false); }
  function clearFilters()  { const next = { ...emptyFilters(), sort:DEFAULT_SORT, page:DEFAULT_PAGE, size:DEFAULT_SIZE }; setForm(next); setSearchParams(new URLSearchParams(), { replace:false }); setMobileFiltersOpen(false); }
  function goToPage(page)  { if (loading) return; const total = pageMeta.totalPages||1; setSearchParams(buildSearchParams(filters,{page:Math.min(Math.max(page,1),total)}),{replace:false}); }
  function categoryLink(catSlug) { const q = buildSearchParams(filters,{page:DEFAULT_PAGE}).toString(); return `/category/${catSlug}${q?`?${q}`:""}` ; }
  function removeChip(key) { const reset={keyword:"",author:"",publisher:"",isbn:"",minPrice:"",maxPrice:"",available:"",sort:DEFAULT_SORT,size:DEFAULT_SIZE}; if(reset[key]!==undefined) setSearchParams(buildSearchParams({...filters,[key]:reset[key]},{page:DEFAULT_PAGE}),{replace:false}); }

  return (
    /* ── Page wrapper — overflow-hidden stops orbs from entering footer ── */
    <div className="relative w-full overflow-hidden" style={{ background: tk.pageBg, minHeight:"100vh" }}>
      {/* ── Dot-grid overlay — contained within page ── */}
      <div className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:`radial-gradient(circle, ${isDark?"rgba(79,110,247,0.11)":"rgba(37,99,235,0.07)"} 1px, transparent 1px)`,
          backgroundSize:"48px 48px",
          maskImage:"radial-gradient(ellipse at 50% 0%, black 0%, transparent 65%)",
        }}
      />

      {/* ── Ambient orbs — absolute so they stay within page, not in footer ── */}
      <div className="pointer-events-none absolute -right-40 -top-40 h-[600px] w-[600px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb1} 0%,transparent 70%)`, filter:"blur(80px)" }}/>
      <div className="pointer-events-none absolute -left-32 top-[40%] h-[450px] w-[450px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb2} 0%,transparent 70%)`, filter:"blur(90px)" }}/>
      <div className="pointer-events-none absolute bottom-[10%] right-[20%] h-[380px] w-[380px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb3} 0%,transparent 70%)`, filter:"blur(80px)" }}/>

      {/* ── Content ── */}
      <div className="relative z-10 mx-auto grid w-full max-w-[1440px] grid-cols-1 gap-0 px-4 pb-28 pt-20 md:px-8 lg:grid-cols-[280px_1fr] lg:gap-8">
        <div className="lg:col-span-2 mb-8">
          <CatalogHero tk={tk} isDark={isDark} title={title} loading={loading} total={pageMeta.totalElements}
            activeCategory={activeCategory} filters={filters} hasActiveFilters={hasActiveFilters}
            onClear={clearFilters} onRemoveChip={removeChip} slug={slug} t={t}/>
        </div>

        <aside className="hidden lg:block lg:sticky lg:top-24 lg:self-start">
          <CatalogSidebar tk={tk} isDark={isDark} categories={categories} slug={slug} categoryLink={categoryLink}
            form={form} setForm={setForm} onApply={applyFilters} onClear={clearFilters} hasActiveFilters={hasActiveFilters} t={t}/>
        </aside>

        <main className="min-w-0">
          <CatalogToolbar tk={tk} isDark={isDark} filters={filters} pageMeta={pageMeta} loading={loading}
            viewMode={viewMode} onViewMode={setViewMode} onOpenFilters={() => setMobileFiltersOpen(true)}
            onSortChange={changes => setSearchParams(buildSearchParams(filters,{...changes,page:DEFAULT_PAGE}),{replace:false})} t={t}/>

          {message && <Notice className="mb-6">{message}</Notice>}

          <AnimatePresence mode="wait">
            {loading
              ? <motion.div key="sk" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:0.25}}>
                  <CatalogSkeleton tk={tk} count={filters.size} viewMode={viewMode}/>
                </motion.div>
              : books.length
                ? <motion.div key="books" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:0.3}}>
                    <CatalogBookGrid books={books} viewMode={viewMode} isDark={isDark} tk={tk}/>
                  </motion.div>
                : <motion.div key="empty" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:0.3}}>
                    <CatalogEmptyState tk={tk} isDark={isDark} hasActiveFilters={hasActiveFilters} onClear={clearFilters} t={t}/>
                  </motion.div>}
          </AnimatePresence>

          <Pagination meta={pageMeta} loading={loading} onPage={goToPage} t={t}/>
        </main>

        <Drawer open={mobileFiltersOpen} title={t("catalog.filters")} onClose={() => setMobileFiltersOpen(false)}>
          <CatalogSidebar tk={tk} isDark={isDark} categories={categories} slug={slug} categoryLink={categoryLink}
            form={form} setForm={setForm} onApply={applyFilters} onClear={clearFilters} hasActiveFilters={hasActiveFilters} t={t} compact/>
        </Drawer>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   HERO — same dark bg regardless of theme (editorial)
═══════════════════════════════════════════════════════ */
function CatalogHero({ tk, title, loading, total, activeCategory, filters, hasActiveFilters, onClear, onRemoveChip, slug, t }) {
  const chips = getActiveFilterChips(filters, t);
  const categoryLabel = activeCategory?.label || activeCategory?.categoryName || t("catalog.booksFallback");
  return (
    <motion.section
      initial={{ opacity:0, y:30 }} animate={{ opacity:1, y:0 }}
      transition={{ duration:0.8, ease:[0.22,1,0.36,1] }}
      className="relative overflow-hidden rounded-[28px]"
      style={{ background:tk.heroBg, border:`1px solid rgba(255,255,255,0.05)`, boxShadow:"0 40px 120px rgba(0,0,0,0.55), inset 0 1px 0 rgba(255,255,255,0.05)" }}
    >
      {/* Top accent line */}
      <div className="absolute left-0 right-0 top-0 h-[1px]" style={{ background:tk.heroTopLine }}/>
      {/* Noise */}
      <div className="pointer-events-none absolute inset-0 opacity-[0.03] mix-blend-screen"
        style={{ backgroundImage:"url('https://grainy-gradients.vercel.app/noise.svg')" }}/>
      {/* Inner glows */}
      <div className="pointer-events-none absolute -right-20 -top-20 h-80 w-80 rounded-full"
        style={{ background:"radial-gradient(circle,rgba(79,110,247,0.2) 0%,transparent 70%)" }}/>
      <div className="pointer-events-none absolute -left-10 bottom-0 h-60 w-60 rounded-full"
        style={{ background:"radial-gradient(circle,rgba(167,139,250,0.12) 0%,transparent 70%)" }}/>
      {/* Particles */}
      {PARTICLES.map((p, i) => (
        <div key={i} className="catalog-particle-v2"
          style={{ left:p.left, top:p.top, width:p.size, height:p.size, opacity:p.op, background:"#4f6ef7", "--dur":p.dur, "--delay":p.delay }}/>
      ))}

      <div className="relative px-8 py-10 md:px-12 md:py-12">
        <HeroBreadcrumb slug={slug} categoryLabel={categoryLabel} t={t}/>

        <div className="mt-7 grid gap-10 lg:grid-cols-[1fr_320px] lg:items-center">
          {/* Left */}
          <div>
            {/* Eyebrow */}
            <motion.div initial={{opacity:0,x:-16}} animate={{opacity:1,x:0}} transition={{duration:0.6,delay:0.1}}
              className="mb-5 inline-flex items-center gap-2.5">
              <span className="flex items-center gap-2 rounded-full px-4 py-1.5"
                style={{ background:"rgba(79,110,247,0.12)", border:"1px solid rgba(79,110,247,0.3)" }}>
                <span className="h-1.5 w-1.5 animate-pulse rounded-full" style={{ background:"#4f6ef7" }}/>
                <span className="text-[0.68rem] font-black uppercase tracking-[0.22em]" style={{ color:"#93a8ff" }}>
                  {t("catalog.heroEyebrow")}
                </span>
                <Sparkles size={11} style={{ color:"#4f6ef7" }}/>
              </span>
            </motion.div>

            {/* Title */}
            <motion.h1 initial={{opacity:0,y:24}} animate={{opacity:1,y:0}}
              transition={{duration:0.85,delay:0.18,ease:[0.22,1,0.36,1]}}
              className="mb-5 text-5xl font-bold leading-[1.06] tracking-tight md:text-6xl"
              style={{ fontFamily:"var(--f-serif)", letterSpacing:"-0.025em" }}>
              {filters.keyword ? (
                <>
                  <span className="block text-xl font-normal tracking-normal md:text-2xl" style={{ color:"#4a5578" }}>
                    {t("catalog.keyword")}:
                  </span>
                  <span style={{ background:"linear-gradient(135deg,#93a8ff,#c4b5fd,#7dd3fc)", WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent" }}>
                    &ldquo;{filters.keyword}&rdquo;
                  </span>
                </>
              ) : (
                <span style={{ background:"linear-gradient(135deg,#e8eeff 0%,#b8c8ff 40%,#d4bcff 70%,#f0f4ff 100%)", WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent" }}>
                  {title}
                </span>
              )}
            </motion.h1>

            {/* Copy */}
            <motion.p initial={{opacity:0,y:14}} animate={{opacity:1,y:0}} transition={{duration:0.7,delay:0.28}}
              className="max-w-lg text-base font-light leading-relaxed" style={{ color:"#8892b0" }}>
              {t("catalog.heroCopy")}
            </motion.p>

            <ActiveFilterChips chips={chips} hasActiveFilters={hasActiveFilters} onClear={onClear} onRemove={onRemoveChip} t={t}/>
          </div>

          {/* Right — stat strip */}
          <HeroStatStrip loading={loading} total={total} categoryLabel={categoryLabel} sort={filters.sort} t={t}/>
        </div>
      </div>
    </motion.section>
  );
}

/* ── Stat strip ── */
function HeroStatStrip({ loading, total, categoryLabel, sort, t }) {
  const stats = [
    { icon:<BookOpen size={16}/>,    label:t("catalog.catalog"),         value:categoryLabel,                                    accent:"#4f6ef7" },
    { icon:<TrendingUp size={16}/>,  label:t("catalog.resultsSummary"),   value:loading?"…":String(total??0),                    accent:"#10d98a", isNum:true },
    { icon:<Layers size={16}/>,      label:t("catalog.sort"),             value:getSortLabel(sort,t),                             accent:"#f0a500" },
  ];
  return (
    <motion.div initial={{opacity:0,scale:0.94}} animate={{opacity:1,scale:1}} transition={{duration:0.6,delay:0.38}}
      className="grid gap-3">
      {stats.map((s,i) => (
        <motion.div key={i} initial={{opacity:0,x:20}} animate={{opacity:1,x:0}} transition={{duration:0.5,delay:0.4+i*0.1}}
          className="group flex items-center gap-4 rounded-2xl px-5 py-4 transition-all duration-300"
          style={{ background:"rgba(18,24,64,0.75)", border:"1px solid rgba(255,255,255,0.065)", backdropFilter:"blur(20px)" }}
          whileHover={{ borderColor:s.accent+"55", y:-2 }}>
          <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl"
            style={{ background:s.accent+"18", color:s.accent }}>{s.icon}</div>
          <div className="min-w-0">
            <div className="text-[0.6rem] font-black uppercase tracking-[0.2em]" style={{ color:"#4a5578" }}>{s.label}</div>
            <div className="mt-0.5 truncate text-sm font-bold" style={{ color:"#e8eeff" }}>
              {s.isNum && !loading ? <AnimatedNumber value={Number(s.value)||0}/> : s.value}
            </div>
          </div>
          <div className="ml-auto h-2 w-2 flex-shrink-0 rounded-full animate-pulse"
            style={{ background:s.accent, boxShadow:`0 0 8px ${s.accent}` }}/>
        </motion.div>
      ))}
    </motion.div>
  );
}

/* ── Animated count-up ── */
function AnimatedNumber({ value }) {
  const [n, setN] = useState(0);
  useEffect(() => {
    if (!value) { setN(0); return; }
    let cur = 0;
    const step = Math.ceil(value / (700/16));
    const id = setInterval(() => { cur = Math.min(cur+step, value); setN(cur); if(cur>=value) clearInterval(id); }, 16);
    return () => clearInterval(id);
  }, [value]);
  return <>{n}</>;
}

/* ── Breadcrumb ── */
function HeroBreadcrumb({ slug, categoryLabel, t }) {
  return (
    <motion.nav initial={{opacity:0,y:-8}} animate={{opacity:1,y:0}} transition={{duration:0.5}}
      className="flex items-center gap-1.5 text-xs" style={{ color:"#4a5578" }}>
      <Link to="/" className="flex items-center gap-1 transition-colors hover:text-blue-400">
        <Home size={11}/> <span style={{ color:"#6374a8" }}>{t("common.home")}</span>
      </Link>
      <ChevronRight size={10} style={{ color:"#2d3561" }}/>
      <Link to="/category/all" className={`transition-colors hover:text-blue-400`}
        style={{ color: slug==="all" ? "#93a8ff" : "#6374a8", fontWeight: slug==="all"?"700":"400" }}>
        {t("catalog.titleAll")}
      </Link>
      {slug!=="all" && <>
        <ChevronRight size={10} style={{ color:"#2d3561" }}/>
        <span className="font-semibold" style={{ color:"#93a8ff" }}>{categoryLabel}</span>
      </>}
    </motion.nav>
  );
}

/* ── Active filter chips ── */
const CHIP_COLORS = {
  keyword:   { bg:"rgba(79,110,247,0.15)",  border:"rgba(79,110,247,0.4)",  text:"#93a8ff" },
  author:    { bg:"rgba(167,139,250,0.15)", border:"rgba(167,139,250,0.4)", text:"#c4b5fd" },
  publisher: { bg:"rgba(16,217,138,0.14)",  border:"rgba(16,217,138,0.4)",  text:"#6ee7b7" },
  isbn:      { bg:"rgba(240,165,0,0.14)",   border:"rgba(240,165,0,0.4)",   text:"#fbbf24" },
  minPrice:  { bg:"rgba(236,72,153,0.14)",  border:"rgba(236,72,153,0.4)",  text:"#f9a8d4" },
  maxPrice:  { bg:"rgba(236,72,153,0.14)",  border:"rgba(236,72,153,0.4)",  text:"#f9a8d4" },
  available: { bg:"rgba(56,189,248,0.14)",  border:"rgba(56,189,248,0.4)",  text:"#7dd3fc" },
  sort:      { bg:"rgba(100,116,139,0.14)", border:"rgba(100,116,139,0.35)",text:"#94a3b8" },
  size:      { bg:"rgba(100,116,139,0.14)", border:"rgba(100,116,139,0.35)",text:"#94a3b8" },
};
function ActiveFilterChips({ chips, hasActiveFilters, onClear, onRemove, t }) {
  if (!chips.length) return null;
  return (
    <motion.div initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} transition={{delay:0.35}}
      className="mt-7 flex flex-wrap items-center gap-2">
      <span className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[0.65rem] font-black uppercase tracking-[0.16em]"
        style={{ background:"rgba(79,110,247,0.15)", border:"1px solid rgba(79,110,247,0.35)", color:"#93a8ff" }}>
        <Filter size={10}/> {t("catalog.activeFilters",{count:chips.length})}
      </span>
      <AnimatePresence>
        {chips.map(chip => {
          const c = CHIP_COLORS[chip.key] || CHIP_COLORS.sort;
          return (
            <motion.span key={chip.key}
              initial={{opacity:0,scale:0.75,y:6}} animate={{opacity:1,scale:1,y:0}} exit={{opacity:0,scale:0.7,y:-4}}
              transition={{duration:0.25,ease:[0.22,1,0.36,1]}}
              className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold"
              style={{ background:c.bg, border:`1px solid ${c.border}`, color:c.text }}>
              {chip.label}
              <button type="button" onClick={() => onRemove(chip.key)}
                className="flex h-3.5 w-3.5 items-center justify-center rounded-full opacity-60 transition-opacity hover:opacity-100"
                style={{ background:c.border }}><X size={8}/></button>
            </motion.span>
          );
        })}
      </AnimatePresence>
      {hasActiveFilters && (
        <button type="button" onClick={onClear}
          className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold transition-all hover:opacity-90"
          style={{ background:"rgba(239,68,68,0.12)", border:"1px solid rgba(239,68,68,0.3)", color:"#fca5a5" }}>
          <X size={10}/> {t("catalog.clearAll")}
        </button>
      )}
    </motion.div>
  );
}

/* ═══════════════════════════════════════════════════════
   TOOLBAR
═══════════════════════════════════════════════════════ */
function CatalogToolbar({ tk, isDark, filters, pageMeta, loading, viewMode, onViewMode, onOpenFilters, onSortChange, t }) {
  const totalPages  = Math.max(Number(pageMeta.totalPages)||0,1);
  const currentPage = Math.min(Math.max(Number(pageMeta.currentPage||filters.page)||1,1),totalPages);
  return (
    <motion.div initial={{opacity:0,y:14}} animate={{opacity:1,y:0}} transition={{duration:0.5,delay:0.12}}
      className="mb-6 flex flex-col gap-4 rounded-2xl p-4 md:flex-row md:items-center md:justify-between"
      style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)", boxShadow: isDark?"none":"0 4px 24px rgba(15,23,42,0.06)" }}>
      {/* Left */}
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl"
          style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 24px rgba(79,110,247,0.3)" }}>
          <BookOpen size={17} color="#fff"/>
        </div>
        <div>
          <div className="text-sm font-bold" style={{ color:tk.text1 }}>
            {loading ? t("catalog.loadingBooks") : t("catalog.booksFound",{count:pageMeta.totalElements})}
          </div>
          <div className="mt-0.5 text-xs font-medium" style={{ color:tk.text3 }}>
            {t("catalog.pageIndicator",{page:currentPage,total:totalPages})}
          </div>
        </div>
      </div>

      {/* Right */}
      <div className="flex flex-wrap items-center gap-2">
        {/* View toggle */}
        <div className="flex items-center rounded-xl p-1" style={{ background:isDark?"rgba(255,255,255,0.04)":"rgba(15,23,42,0.05)", border:`1px solid ${tk.border}` }}>
          {[{mode:"grid",Icon:Grid3X3},{mode:"list",Icon:LayoutList}].map(({mode,Icon}) => (
            <button key={mode} type="button" onClick={() => onViewMode(mode)}
              className="flex h-8 w-8 items-center justify-center rounded-lg transition-all duration-200"
              style={viewMode===mode
                ?{background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)",color:"#fff",boxShadow:"0 4px 12px rgba(79,110,247,0.35)"}
                :{color:tk.text3}}>
              <Icon size={15}/>
            </button>
          ))}
        </div>

        <Button type="button" variant="secondary" className="lg:hidden" onClick={onOpenFilters}
          style={{ background:isDark?"rgba(255,255,255,0.05)":"rgba(15,23,42,0.05)", border:`1px solid ${tk.border}`, color:tk.text2 }}>
          <SlidersHorizontal className="h-4 w-4"/> {t("catalog.filters")}
        </Button>

        <SortSelect tk={tk} isDark={isDark} value={filters.sort} size={filters.size} onChange={onSortChange} t={t}/>
      </div>
    </motion.div>
  );
}

function SortSelect({ tk, isDark, value, size, onChange, t }) {
  const sel = {
    background:tk.sortBg, border:`1px solid ${tk.border}`, color:tk.text1,
    borderRadius:"12px", padding:"8px 14px", fontSize:"0.85rem", fontWeight:700, minWidth:"140px",
  };
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl p-2"
      style={{ background:isDark?"rgba(18,24,64,0.75)":"rgba(15,23,42,0.06)", border:`1px solid ${tk.border}`, backdropFilter:"blur(20px)" }}>
      <div className="hidden items-center gap-1.5 px-2 text-[0.62rem] font-black uppercase tracking-[0.16em] sm:flex"
        style={{ color:tk.text3 }}>
        <SlidersHorizontal size={12}/> {t("catalog.sort")}
      </div>
      <select value={value} onChange={e=>onChange({sort:e.target.value})} style={sel}>
        <option value="newest">{t("catalog.sortNewest")}</option>
        <option value="price_asc">{t("catalog.sortPriceAsc")}</option>
        <option value="price_desc">{t("catalog.sortPriceDesc")}</option>
        <option value="best_selling">{t("catalog.sortBestSelling")}</option>
        <option value="name_asc">{t("catalog.sortNameAsc")}</option>
      </select>
      <select value={size} onChange={e=>onChange({size:Number(e.target.value)})} style={sel}>
        {PAGE_SIZES.map(n=><option key={n} value={n}>{t("catalog.perPage",{count:n})}</option>)}
      </select>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   SIDEBAR
═══════════════════════════════════════════════════════ */
function CatalogSidebar({ tk, isDark, categories, slug, categoryLink, form, setForm, onApply, onClear, hasActiveFilters, t, compact=false }) {
  const activeCount = getActiveFilterChips(form,t).length;
  return (
    <div className="grid gap-4">
      <DarkPanel tk={tk} isDark={isDark} icon={<BookOpen size={14}/>} eyebrow={t("catalog.catalog")} title={t("common.categories")}>
        <div className="grid gap-0.5 pt-1">
          {categories.filter(Boolean).map((cat,i) => {
            const accent = CAT_ACCENTS[i%CAT_ACCENTS.length];
            return (
              <motion.div key={cat.id||cat.slug} initial={{opacity:0,x:-12}} animate={{opacity:1,x:0}} transition={{duration:0.35,delay:i*0.04}}>
                <CategoryItem tk={tk} cat={cat} isActive={slug===cat.slug} to={categoryLink(cat.slug)} accent={accent}/>
              </motion.div>
            );
          })}
        </div>
      </DarkPanel>

      <DarkPanel tk={tk} isDark={isDark} icon={<SlidersHorizontal size={14}/>}
        eyebrow={activeCount?t("catalog.activeFilters",{count:activeCount}):t("catalog.filters")} title={t("catalog.filters")}>
        <form className="grid gap-3 pt-1" onSubmit={onApply}>
          <DarkInput tk={tk} label={t("catalog.keyword")}   value={form.keyword}    onChange={v=>setForm({...form,keyword:v})}   placeholder={t("catalog.keywordPlaceholder")}/>
          <DarkInput tk={tk} label={t("catalog.author")}    value={form.author}     onChange={v=>setForm({...form,author:v})}    placeholder={t("catalog.authorPlaceholder")}/>
          <DarkInput tk={tk} label={t("catalog.publisher")} value={form.publisher}  onChange={v=>setForm({...form,publisher:v})} placeholder={t("catalog.publisherPlaceholder")}/>
          <DarkInput tk={tk} label={t("catalog.isbn")}      value={form.isbn}       onChange={v=>setForm({...form,isbn:v})}      placeholder={t("catalog.isbnPlaceholder")}/>
          <div className="grid grid-cols-2 gap-2">
            <DarkInput tk={tk} label={t("catalog.minPrice")} type="number" min="0" value={form.minPrice} onChange={v=>setForm({...form,minPrice:v})}/>
            <DarkInput tk={tk} label={t("catalog.maxPrice")} type="number" min="0" value={form.maxPrice} onChange={v=>setForm({...form,maxPrice:v})}/>
          </div>
          <label className="grid gap-1.5">
            <span className="text-[0.62rem] font-black uppercase tracking-wider" style={{ color:tk.text3 }}>{t("catalog.availability")}</span>
            <select value={form.available} onChange={e=>setForm({...form,available:e.target.value})}
              className="rounded-xl px-3 py-2.5 text-sm font-semibold outline-none"
              style={{ background:tk.inputBg, border:`1px solid ${tk.border}`, color:tk.text1 }}>
              <option value="">{t("catalog.anyAvailability")}</option>
              <option value="true">{t("catalog.availableOnly")}</option>
              <option value="false">{t("catalog.outOfStockOnly")}</option>
            </select>
          </label>
          <div className={`grid gap-2 pt-1 ${compact?"sticky bottom-0 pb-3":""}`}
            style={compact?{background:isDark?"rgba(7,9,26,0.95)":"rgba(245,243,239,0.95)",backdropFilter:"blur(20px)"}:{}}>
            <button type="submit"
              className="flex w-full items-center justify-center gap-2 rounded-xl py-3 text-sm font-black uppercase tracking-wider text-white transition-all duration-300 hover:opacity-90 active:scale-[0.98]"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7,#818cf8)", boxShadow:"0 8px 24px rgba(79,110,247,0.35)" }}>
              <Filter size={15}/> {t("catalog.applyFilters")}
            </button>
            <button type="button" onClick={onClear} disabled={!hasActiveFilters}
              className="w-full rounded-xl py-2.5 text-sm font-bold transition-all hover:opacity-80 disabled:opacity-30"
              style={{ background:isDark?"rgba(255,255,255,0.04)":"rgba(15,23,42,0.05)", border:`1px solid ${tk.border}`, color:tk.text2 }}>
              {t("catalog.clearFilters")}
            </button>
          </div>
        </form>
      </DarkPanel>
    </div>
  );
}

/* ── Category item ── */
function CategoryItem({ tk, cat, isActive, to, accent }) {
  const [hov, setHov] = useState(false);
  const [pos, setPos] = useState({x:50,y:50});
  const ref = useRef(null);
  function onMove(e) { const r=ref.current?.getBoundingClientRect(); if(r) setPos({x:((e.clientX-r.left)/r.width)*100,y:((e.clientY-r.top)/r.height)*100}); }
  return (
    <Link ref={ref} to={to}
      className="relative flex items-center justify-between gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-all duration-250"
      style={isActive
        ?{background:`linear-gradient(135deg,${accent.color}22,${accent.color}18)`,border:`1px solid ${accent.color}45`,color:tk.text1,boxShadow:`0 4px 20px ${accent.glow}`}
        :{border:"1px solid transparent",color:tk.text2}}
      onMouseEnter={()=>setHov(true)} onMouseLeave={()=>{setHov(false);setPos({x:50,y:50});}} onMouseMove={onMove}>
      {!isActive && hov && (
        <span className="pointer-events-none absolute inset-0 rounded-xl"
          style={{ background:`radial-gradient(140px at ${pos.x}% ${pos.y}%,${accent.glow} 0%,transparent 75%)` }}/>
      )}
      <span className="relative z-10 truncate">{cat.label}</span>
      <span className="relative z-10 flex-shrink-0">
        {isActive
          ?<span className="h-2 w-2 rounded-full" style={{ background:accent.color, boxShadow:`0 0 8px ${accent.color}` }}/>
          :<span className="h-1.5 w-1.5 rounded-full transition-colors" style={{ background:hov?accent.color:tk.border }}/>}
      </span>
    </Link>
  );
}

/* ── Dark/Light panel wrapper ── */
function DarkPanel({ tk, isDark, icon, eyebrow, title, children }) {
  return (
    <motion.section initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{duration:0.5}}
      className="relative overflow-hidden rounded-2xl p-5"
      style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)", boxShadow:isDark?"none":"0 4px 24px rgba(15,23,42,0.06)" }}>
      <div className="absolute left-0 right-0 top-0 h-[1px]"
        style={{ background:`linear-gradient(90deg,transparent,${tk.accent}80,transparent)` }}/>
      <div className="pointer-events-none absolute -right-6 -top-6 h-20 w-20 rounded-full"
        style={{ background:`radial-gradient(circle,${tk.accent}18 0%,transparent 70%)` }}/>
      <div className="mb-4 flex items-center gap-3">
        {icon && (
          <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg"
            style={{ background:`${tk.accent}22`, color:tk.accent }}>{icon}</div>
        )}
        <div>
          {eyebrow && <div className="text-[0.58rem] font-black uppercase tracking-[0.22em]" style={{ color:tk.accent }}>{eyebrow}</div>}
          <h2 className="text-sm font-black" style={{ color:tk.text1 }}>{title}</h2>
        </div>
      </div>
      {children}
    </motion.section>
  );
}

/* ── Input ── */
function DarkInput({ tk, label, value, onChange, type="text", ...props }) {
  const [focused, setFocused] = useState(false);
  return (
    <label className="grid gap-1.5">
      <span className="text-[0.6rem] font-black uppercase tracking-wider" style={{ color:tk.text3 }}>{label}</span>
      <input type={type} value={value} onChange={e=>onChange(e.target.value)}
        onFocus={()=>setFocused(true)} onBlur={()=>setFocused(false)}
        className="w-full rounded-xl px-3.5 py-2.5 text-sm font-medium outline-none transition-all"
        style={{
          background:tk.inputBg, color:tk.text1,
          border:`1px solid ${focused?"rgba(79,110,247,0.55)":tk.border}`,
          boxShadow:focused?"0 0 0 3px rgba(79,110,247,0.12)":"none",
        }}
        {...props}/>
    </label>
  );
}

/* ═══════════════════════════════════════════════════════
   BOOK GRID
═══════════════════════════════════════════════════════ */
function CatalogBookGrid({ books, viewMode, isDark, tk }) {
  if (viewMode==="list") return (
    <div className="flex flex-col gap-3">
      {books.map((book,i) => (
        <motion.div key={book.id}
          initial={{opacity:0,x:-20,filter:"blur(6px)"}}
          whileInView={{opacity:1,x:0,filter:"blur(0px)"}}
          viewport={{once:true,margin:"0px 0px -40px 0px"}}
          transition={{duration:0.4,delay:i*0.04,ease:[0.22,1,0.36,1]}}>
          <BookListRow book={book} tk={tk} isDark={isDark}/>
        </motion.div>
      ))}
    </div>
  );
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
      {books.map((book,i) => (
        <motion.div key={book.id}
          initial={{opacity:0,y:28,filter:"blur(8px)"}}
          whileInView={{opacity:1,y:0,filter:"blur(0px)"}}
          viewport={{once:true,margin:"0px 0px -50px 0px"}}
          transition={{duration:0.5,delay:(i%4)*0.07,ease:[0.22,1,0.36,1]}}>
          <BookCard book={book} dark={isDark}/>
        </motion.div>
      ))}
    </div>
  );
}

/* ── List row ── */
function BookListRow({ book, tk, isDark }) {
  const { t } = useTranslation();
  const [hov, setHov] = useState(false);
  return (
    <Link to={`/product/${book.slug}`}
      className="group flex items-center gap-4 overflow-hidden rounded-2xl p-4 transition-all duration-300"
      style={{
        background: hov?(isDark?"rgba(18,24,64,0.95)":"rgba(255,253,248,1)"):tk.surface1,
        border:`1px solid ${hov?(isDark?"rgba(79,110,247,0.3)":"rgba(37,99,235,0.25)"):tk.border}`,
        backdropFilter:"blur(20px)",
        transform:hov?"translateX(5px)":"none",
        boxShadow:hov?(isDark?"0 12px 40px rgba(79,110,247,0.12)":"0 12px 40px rgba(37,99,235,0.1)"):(isDark?"none":"0 2px 16px rgba(15,23,42,0.06)"),
      }}
      onMouseEnter={()=>setHov(true)} onMouseLeave={()=>setHov(false)}>
      <div className="h-[100px] w-[66px] flex-shrink-0 overflow-hidden rounded-xl"
        style={{ boxShadow:"0 8px 24px rgba(0,0,0,0.25)" }}>
        <img src={book.image||book.cover} alt={book.title} className="h-full w-full object-cover"
          style={{ transform:hov?"scale(1.08)":"scale(1)", transition:"transform 0.5s cubic-bezier(0.22,1,0.36,1)" }}/>
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        {book.catLabel && <span className="text-[10px] font-black uppercase tracking-[0.15em]" style={{ color:tk.accent }}>{book.catLabel}</span>}
        <h3 className="line-clamp-2 text-sm font-bold leading-snug transition-colors"
          style={{ color:hov?(isDark?"#93a8ff":"#1d4ed8"):tk.text1 }}>{book.title}</h3>
        {book.author && <p className="text-xs" style={{ color:tk.text3 }}>{book.author}</p>}
        <div className="flex items-center gap-3">
          <span className="text-base font-black" style={{ color:tk.text1, letterSpacing:"-0.02em" }}>
            {book.price?formatVND(book.price):"—"}
          </span>
          {Number(book.stockQuantity)>0
            ?<span className="flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold"
                style={{ background:"rgba(16,217,138,0.12)", border:"1px solid rgba(16,217,138,0.3)", color:"#10d98a" }}>
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400"/>
                {t("home.inStock",{count:book.stockQuantity})}
              </span>
            :<span className="rounded-full px-2 py-0.5 text-[10px] font-bold"
                style={{ background:"rgba(239,68,68,0.12)", border:"1px solid rgba(239,68,68,0.25)", color:"#ef4444" }}>
                {t("home.outOfStock")}
              </span>}
        </div>
      </div>
      <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full transition-all duration-300"
        style={hov
          ?{background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)",boxShadow:"0 4px 16px rgba(79,110,247,0.4)"}
          :{background:isDark?"rgba(255,255,255,0.05)":"rgba(15,23,42,0.06)",border:`1px solid ${tk.border}`}}>
        <ChevronRight size={16} color={hov?"#fff":tk.text3}/>
      </div>
    </Link>
  );
}

/* ═══════════════════════════════════════════════════════
   EMPTY STATE
═══════════════════════════════════════════════════════ */
function CatalogEmptyState({ tk, isDark, hasActiveFilters, onClear, t }) {
  return (
    <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.6}}
      className="relative overflow-hidden rounded-[28px] px-8 py-20 text-center"
      style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)", boxShadow:isDark?"none":"0 4px 24px rgba(15,23,42,0.06)" }}>
      <div className="pointer-events-none absolute left-1/2 top-1/3 h-48 w-48 -translate-x-1/2 -translate-y-1/2 rounded-full"
        style={{ background:`radial-gradient(circle,${tk.accentGlow} 0%,transparent 70%)` }}/>
      <div className="relative mx-auto mb-8 flex h-20 w-20 items-center justify-center">
        <div className={isDark?"catalog-ripple-ring-dark":"catalog-ripple-ring-light"}/>
        <div className={isDark?"catalog-ripple-ring-dark":"catalog-ripple-ring-light"} style={{ animationDelay:"0.8s" }}/>
        <motion.div animate={{y:[0,-10,0]}} transition={{duration:3.5,ease:"easeInOut",repeat:Infinity}}
          className="relative z-10 flex h-20 w-20 items-center justify-center rounded-[28px]"
          style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 16px 40px rgba(79,110,247,0.4)" }}>
          <Search size={28} color="#fff"/>
        </motion.div>
      </div>
      <h2 className="text-3xl font-black"
        style={{ background:`linear-gradient(135deg,${tk.text1},${tk.accent})`, WebkitBackgroundClip:"text", WebkitTextFillColor:"transparent", fontFamily:"var(--f-serif)", letterSpacing:"-0.01em" }}>
        {t("catalog.noBooks")}
      </h2>
      <p className="mx-auto mt-3 max-w-md text-sm leading-6" style={{ color:tk.text3 }}>{t("catalog.emptyCopy")}</p>
      {hasActiveFilters && (
        <motion.div initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{delay:0.3}} className="mt-8">
          <button type="button" onClick={onClear}
            className="inline-flex items-center gap-2 rounded-xl px-6 py-3 text-sm font-black uppercase tracking-wider text-white transition-all hover:opacity-90"
            style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 28px rgba(79,110,247,0.4)" }}>
            <X size={15}/> {t("catalog.clearFilters")}
          </button>
        </motion.div>
      )}
    </motion.div>
  );
}

/* ═══════════════════════════════════════════════════════
   SKELETON
═══════════════════════════════════════════════════════ */
function CatalogSkeleton({ tk, count, viewMode }) {
  const n = Math.min(Number(count)||DEFAULT_SIZE,24);
  const shimmerClass = "catalog-dynamic-shimmer";
  if (viewMode==="list") return (
    <div className="flex flex-col gap-3">
      {Array.from({length:Math.min(n,8)}).map((_,i) => (
        <div key={i} className="flex items-center gap-4 overflow-hidden rounded-2xl p-4"
          style={{ background:tk.surface1, border:`1px solid ${tk.border}` }}>
          <div className={`${shimmerClass} h-[100px] w-[66px] flex-shrink-0 rounded-xl`} style={{ "--sa":tk.skeletonA,"--sb":tk.skeletonB,"--sc":tk.skeletonC,animationDelay:`${i*55}ms` }}/>
          <div className="flex flex-1 flex-col gap-3">
            {[["w-1/4","h-2.5"],["w-3/4","h-4"],["w-1/2","h-3"],["w-24","h-5"]].map(([w,h],j)=>(
              <div key={j} className={`${shimmerClass} ${h} ${w} rounded-full`} style={{ "--sa":tk.skeletonA,"--sb":tk.skeletonB,"--sc":tk.skeletonC,animationDelay:`${i*55+j*45}ms` }}/>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
      {Array.from({length:n}).map((_,i) => (
        <div key={i} className="overflow-hidden rounded-[22px] p-3"
          style={{ background:tk.surface1, border:`1px solid ${tk.border}` }}>
          <div className={`${shimmerClass} aspect-[2/3] rounded-2xl`} style={{ "--sa":tk.skeletonA,"--sb":tk.skeletonB,"--sc":tk.skeletonC,animationDelay:`${i*50}ms` }}/>
          {[["w-14","h-2.5","mt-4"],["w-full","h-4","mt-3"],["w-2/3","h-3","mt-2"],["w-20","h-5","mt-4"]].map(([w,h,mt],j)=>(
            <div key={j} className={`${shimmerClass} ${h} ${w} ${mt} rounded-full`} style={{ "--sa":tk.skeletonA,"--sb":tk.skeletonB,"--sc":tk.skeletonC,animationDelay:`${i*50+j*40}ms` }}/>
          ))}
        </div>
      ))}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   UTILS
═══════════════════════════════════════════════════════ */
function getActiveFilterChips(filters, t) {
  const chips = [];
  if (filters.keyword)               chips.push({key:"keyword",   label:`${t("catalog.keyword")}: ${filters.keyword}`});
  if (filters.author)                chips.push({key:"author",    label:`${t("catalog.author")}: ${filters.author}`});
  if (filters.publisher)             chips.push({key:"publisher", label:`${t("catalog.publisher")}: ${filters.publisher}`});
  if (filters.isbn)                  chips.push({key:"isbn",      label:`${t("catalog.isbn")}: ${filters.isbn}`});
  if (filters.minPrice)              chips.push({key:"minPrice",  label:`${t("catalog.minPrice")}: ${filters.minPrice}`});
  if (filters.maxPrice)              chips.push({key:"maxPrice",  label:`${t("catalog.maxPrice")}: ${filters.maxPrice}`});
  if (filters.available)             chips.push({key:"available", label:getAvailabilityLabel(filters.available,t)});
  if (filters.sort!==DEFAULT_SORT)   chips.push({key:"sort",      label:getSortLabel(filters.sort,t)});
  if (filters.size!==DEFAULT_SIZE)   chips.push({key:"size",      label:t("catalog.perPage",{count:filters.size})});
  return chips;
}
const getAvailabilityLabel = (v,t) => ({true:t("catalog.availableOnly"),false:t("catalog.outOfStockOnly")})[v]||t("catalog.anyAvailability");
const getSortLabel = (v,t) => ({newest:t("catalog.sortNewest"),price_asc:t("catalog.sortPriceAsc"),price_desc:t("catalog.sortPriceDesc"),best_selling:t("catalog.sortBestSelling"),name_asc:t("catalog.sortNameAsc")})[v]||t("catalog.sortNewest");
function readFilters(p) {
  return { keyword:p.get("keyword")||p.get("search")||"", author:p.get("author")||"", publisher:p.get("publisher")||"", isbn:p.get("isbn")||"", minPrice:p.get("minPrice")||"", maxPrice:p.get("maxPrice")||"", available:parseAvailable(p.get("available")), sort:p.get("sort")||DEFAULT_SORT, page:positiveInt(p.get("page"),DEFAULT_PAGE), size:PAGE_SIZES.includes(Number(p.get("size")))?Number(p.get("size")):DEFAULT_SIZE };
}
function emptyFilters() { return {keyword:"",author:"",publisher:"",isbn:"",minPrice:"",maxPrice:"",available:"",sort:DEFAULT_SORT,page:DEFAULT_PAGE,size:DEFAULT_SIZE}; }
function emptyPageMeta(f) { return readPageMeta([],{page:f.page,size:f.size,totalPages:0}); }
function buildSearchParams(f,o={}) {
  const n={...f,...o}; const p=new URLSearchParams();
  const ap=(k,v)=>{ const s=String(v??"").trim(); if(s) p.set(k,s); };
  ap("keyword",n.keyword); ap("author",n.author); ap("publisher",n.publisher); ap("isbn",n.isbn); ap("minPrice",n.minPrice); ap("maxPrice",n.maxPrice); ap("available",n.available);
  if(n.sort&&n.sort!==DEFAULT_SORT) p.set("sort",n.sort);
  if(Number(n.page)>DEFAULT_PAGE) p.set("page",String(n.page));
  if(Number(n.size)!==DEFAULT_SIZE) p.set("size",String(n.size));
  return p;
}
const parseAvailable = v => v==="true"||v==="false"?v:"";
const positiveInt    = (v,fb) => { const n=Number(v); return Number.isInteger(n)&&n>0?n:fb; };
const hasFilters     = f => Boolean(f.keyword||f.author||f.publisher||f.isbn||f.minPrice||f.maxPrice||f.available||f.sort!==DEFAULT_SORT||f.page!==DEFAULT_PAGE||f.size!==DEFAULT_SIZE);
