import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { motion } from "motion/react";
import { BookOpen, Filter, Search, SlidersHorizontal, Sparkles, X } from "lucide-react";

import { getCategories, getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import {
  Button,
  Drawer,
  Input,
  Notice,
  Pagination,
  Select,
} from "../components/ui/index.jsx";
import { normalizeBook, normalizeCategory, pageMeta as readPageMeta, pageRows } from "../utils/mappers.js";

const DEFAULT_PAGE = 1;
const DEFAULT_SIZE = 12;
const DEFAULT_SORT = "newest";
const PAGE_SIZES = [12, 24, 48];

export default function CategoryPage() {
  const { t } = useTranslation();
  const { slug = "all" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const searchKey = searchParams.toString();
  const filters = useMemo(() => readFilters(new URLSearchParams(searchKey)), [searchKey]);
  const [form, setForm] = useState(filters);
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([
    { id: "all", slug: "all", label: t("catalog.titleAll") },
  ]);
  const [pageMeta, setPageMeta] = useState(emptyPageMeta(filters));
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const activeCategory = categories.find((category) => category.slug === slug);
  const title = filters.keyword
    ? t("catalog.searchTitle", { search: filters.keyword })
    : activeCategory?.label || activeCategory?.categoryName || t("catalog.booksFallback");
  const hasActiveFilters = hasFilters(filters);

  useEffect(() => {
    setForm(filters);
  }, [filters]);

  useEffect(() => {
    getCategories()
      .then((rows) => {
        const list = pageRows(rows).map(normalizeCategory).filter(Boolean);
        setCategories([{ id: "all", slug: "all", label: t("catalog.titleAll") }, ...list]);
      })
      .catch((error) => setMessage(error.message || t("catalog.categoriesFailed")));
  }, [t]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setMessage("");

    getProducts(
      {
        keyword: filters.keyword,
        categorySlug: slug !== "all" ? slug : "",
        author: filters.author,
        publisher: filters.publisher,
        isbn: filters.isbn,
        minPrice: filters.minPrice,
        maxPrice: filters.maxPrice,
        available: filters.available === "" ? "" : filters.available === "true",
        sort: filters.sort,
        page: filters.page,
        size: filters.size,
      },
      { signal: controller.signal }
    )
      .then((page) => {
        if (page?.totalPages > 0 && filters.page > page.totalPages) {
          setSearchParams(buildSearchParams(filters, { page: page.totalPages }), { replace: true });
          return;
        }

        setBooks(pageRows(page).map((row) => normalizeBook(row)));
        setPageMeta(readPageMeta(page, { page: filters.page, size: filters.size }));
      })
      .catch((error) => {
        if (error.name === "AbortError") return;
        setBooks([]);
        setPageMeta(emptyPageMeta(filters));
        setMessage(error.message || t("catalog.productsFailed"));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [filters, setSearchParams, slug, t]);

  function applyFilters(event) {
    event?.preventDefault();
    setSearchParams(buildSearchParams(form, { page: DEFAULT_PAGE }), { replace: false });
    setMobileFiltersOpen(false);
  }

  function clearFilters() {
    const next = {
      ...emptyFilters(),
      sort: DEFAULT_SORT,
      page: DEFAULT_PAGE,
      size: DEFAULT_SIZE,
    };
    setForm(next);
    setSearchParams(new URLSearchParams(), { replace: false });
    setMobileFiltersOpen(false);
  }

  function goToPage(page) {
    if (loading) return;
    const total = pageMeta.totalPages || 1;
    const nextPage = Math.min(Math.max(page, 1), total);
    setSearchParams(buildSearchParams(filters, { page: nextPage }), { replace: false });
  }

  function categoryLink(categorySlug) {
    const query = buildSearchParams(filters, { page: DEFAULT_PAGE });
    const search = query.toString();
    return `/category/${categorySlug}${search ? `?${search}` : ""}`;
  }

  return (
    <div className="relative isolate mx-auto grid w-full max-w-7xl grid-cols-1 gap-8 overflow-hidden px-4 pb-20 pt-28 md:px-8 lg:grid-cols-[300px_1fr]">
      <div className="pointer-events-none absolute inset-0 -z-10 catalog-paper-texture" />
      <div className="pointer-events-none absolute -right-36 top-20 -z-10 h-[520px] w-[520px] rounded-full bg-blue-500/[0.08] blur-[100px]" />
      <div className="pointer-events-none absolute -left-40 top-[34rem] -z-10 h-[420px] w-[420px] rounded-full bg-amber-300/[0.08] blur-[90px]" />

      <div className="lg:col-span-2">
        <CatalogHero
          title={title}
          loading={loading}
          total={pageMeta.totalElements}
          activeCategory={activeCategory}
          filters={filters}
          hasActiveFilters={hasActiveFilters}
          onClear={clearFilters}
          t={t}
        />
      </div>

      <aside className="hidden lg:block lg:sticky lg:top-28 lg:self-start">
        <CatalogSidebar
          categories={categories}
          slug={slug}
          categoryLink={categoryLink}
          form={form}
          setForm={setForm}
          onApply={applyFilters}
          onClear={clearFilters}
          hasActiveFilters={hasActiveFilters}
          t={t}
        />
      </aside>

      <main className="min-w-0">
        <CatalogToolbar
          filters={filters}
          pageMeta={pageMeta}
          loading={loading}
          onOpenFilters={() => setMobileFiltersOpen(true)}
          onSortChange={(changes) => {
            setSearchParams(buildSearchParams(filters, { ...changes, page: DEFAULT_PAGE }), { replace: false });
          }}
          t={t}
        />

        {message && <Notice className="mb-6">{message}</Notice>}

        {loading ? (
          <CatalogBookGridSkeleton count={filters.size} />
        ) : books.length ? (
          <CatalogBookGrid books={books} />
        ) : (
          <CatalogEmptyState hasActiveFilters={hasActiveFilters} onClear={clearFilters} t={t} />
        )}

        <Pagination meta={pageMeta} loading={loading} onPage={goToPage} t={t} />
      </main>

      {/* Mobile filter drawer */}
      <Drawer
        open={mobileFiltersOpen}
        title={t("catalog.filters")}
        onClose={() => setMobileFiltersOpen(false)}
      >
        <CatalogSidebar
          categories={categories}
          slug={slug}
          categoryLink={categoryLink}
          form={form}
          setForm={setForm}
          onApply={applyFilters}
          onClear={clearFilters}
          hasActiveFilters={hasActiveFilters}
          t={t}
          compact
        />
      </Drawer>
    </div>
  );
}

function CatalogHero({ title, loading, total, activeCategory, filters, hasActiveFilters, onClear, t }) {
  const chips = getActiveFilterChips(filters, t);
  const categoryLabel = activeCategory?.label || activeCategory?.categoryName || t("catalog.booksFallback");

  return (
    <motion.section
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="relative overflow-hidden rounded-[28px] border border-white/80 bg-white/[0.78] p-6 shadow-[0_28px_90px_rgba(15,23,42,0.1)] backdrop-blur-xl md:p-8"
    >
      <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-blue-500/[0.1] blur-3xl" />
      <div className="pointer-events-none absolute -bottom-24 left-12 h-60 w-60 rounded-full bg-amber-300/[0.12] blur-3xl" />
      <div className="pointer-events-none absolute inset-x-8 top-0 h-px bg-gradient-to-r from-transparent via-blue-300/70 to-transparent" />

      <div className="relative grid gap-8 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-end">
        <div>
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50/80 px-4 py-2 text-[0.68rem] font-black uppercase tracking-[0.18em] text-blue-700">
            <Sparkles size={14} className="text-blue-600" />
            {t("catalog.heroEyebrow")}
          </div>
          <h1
            className="max-w-4xl text-4xl font-bold leading-tight text-slate-950 md:text-6xl"
            style={{ fontFamily: "var(--f-serif)", letterSpacing: "-0.01em" }}
          >
            {title}
          </h1>
          <p className="mt-4 max-w-2xl text-base leading-relaxed text-slate-500 md:text-lg">
            {t("catalog.heroCopy")}
          </p>

          <ActiveFilterChips chips={chips} hasActiveFilters={hasActiveFilters} onClear={onClear} t={t} />
        </div>

        <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-1">
          <CatalogStat label={t("catalog.catalog")} value={categoryLabel} />
          <CatalogStat
            label={t("catalog.resultsSummary")}
            value={loading ? t("catalog.loadingBooks") : t("catalog.booksFound", { count: total })}
          />
          <CatalogStat label={t("catalog.sort")} value={getSortLabel(filters.sort, t)} />
        </div>
      </div>
    </motion.section>
  );
}

function CatalogStat({ label, value }) {
  return (
    <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-3 shadow-sm backdrop-blur">
      <div className="text-[0.62rem] font-black uppercase tracking-[0.16em] text-slate-400">{label}</div>
      <div className="mt-1 truncate text-sm font-bold text-slate-900">{value}</div>
    </div>
  );
}

function ActiveFilterChips({ chips, hasActiveFilters, onClear, t }) {
  if (!chips.length) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, delay: 0.2 }}
      className="mt-6 flex flex-wrap items-center gap-2"
    >
      <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-950 px-3 py-1.5 text-[0.68rem] font-black uppercase tracking-[0.14em] text-white">
        <Filter size={12} />
        {t("catalog.activeFilters", { count: chips.length })}
      </span>
      {chips.map((chip) => (
        <span key={chip.key} className="rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-700">
          {chip.label}
        </span>
      ))}
      {hasActiveFilters && (
        <button
          type="button"
          onClick={onClear}
          className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-500 transition-colors hover:border-blue-200 hover:text-blue-700"
        >
          <X size={12} />
          {t("catalog.clearAll")}
        </button>
      )}
    </motion.div>
  );
}

function CatalogToolbar({ filters, pageMeta, loading, onOpenFilters, onSortChange, t }) {
  return (
    <div className="mb-6 flex flex-col gap-4 rounded-[22px] border border-white/80 bg-white/[0.78] p-4 shadow-[0_16px_54px_rgba(15,23,42,0.07)] backdrop-blur-xl md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-2xl bg-blue-600 text-white shadow-[0_12px_28px_rgba(37,99,235,0.2)]">
          <BookOpen size={18} />
        </div>
        <div className="min-w-0">
          <div className="text-sm font-bold text-slate-950">
            {loading ? t("catalog.loadingBooks") : t("catalog.booksFound", { count: pageMeta.totalElements })}
          </div>
          <div className="mt-0.5 text-xs font-semibold text-slate-400">
            {t("catalog.pageIndicator", { page: pageMeta.currentPage || filters.page, total: pageMeta.totalPages || 1 })}
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="secondary" className="lg:hidden" onClick={onOpenFilters}>
          <SlidersHorizontal className="h-4 w-4" />
          {t("catalog.filters")}
        </Button>
        <SortSelect value={filters.sort} size={filters.size} onChange={onSortChange} t={t} />
      </div>
    </div>
  );
}

function CatalogBookGrid({ books }) {
  return (
    <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-4">
      {books.map((book, index) => (
        <motion.div
          key={book.id}
          initial={{ opacity: 0, y: 24, filter: "blur(6px)" }}
          whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          viewport={{ once: true, margin: "0px 0px -50px 0px" }}
          transition={{ duration: 0.48, delay: (index % 4) * 0.06, ease: [0.22, 1, 0.36, 1] }}
        >
          <BookCard book={book} />
        </motion.div>
      ))}
    </div>
  );
}

function CatalogEmptyState({ hasActiveFilters, onClear, t }) {
  return (
    <div className="rounded-[28px] border border-dashed border-blue-200 bg-white/[0.78] px-6 py-16 text-center shadow-[0_18px_60px_rgba(15,23,42,0.06)] backdrop-blur">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
        <Search size={24} />
      </div>
      <h2 className="mt-5 font-serif text-3xl font-bold text-slate-950">{t("catalog.noBooks")}</h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">{t("catalog.emptyCopy")}</p>
      {hasActiveFilters && (
        <Button type="button" className="mt-6" onClick={onClear}>
          {t("catalog.clearFilters")}
        </Button>
      )}
    </div>
  );
}

function CatalogSidebar({ categories, slug, categoryLink, form, setForm, onApply, onClear, hasActiveFilters, t, compact = false }) {
  const activeCount = getActiveFilterChips(form, t).length;

  return (
    <div className="grid gap-5">
      <SidebarPanel
        eyebrow={t("catalog.catalog")}
        title={t("common.categories")}
        subtitle={t("catalog.resultsSummary")}
      >
        <div className="grid gap-1.5">
          {categories.filter(Boolean).map((category) => (
            <Link
              key={category.id || category.slug}
              to={categoryLink(category.slug)}
              className={[
                "group relative overflow-hidden rounded-2xl px-3.5 py-3 text-sm font-bold transition-all",
                slug === category.slug
                  ? "bg-slate-950 text-white shadow-[0_14px_34px_rgba(15,23,42,0.18)]"
                  : "text-slate-600 hover:bg-blue-50 hover:text-blue-700",
              ].join(" ")}
            >
              <span className="relative z-10 flex items-center justify-between gap-3">
                <span className="truncate">{category.label}</span>
                <span
                  className={[
                    "h-2 w-2 rounded-full transition-colors",
                    slug === category.slug ? "bg-blue-300" : "bg-slate-200 group-hover:bg-blue-300",
                  ].join(" ")}
                />
              </span>
            </Link>
          ))}
        </div>
      </SidebarPanel>

      <SidebarPanel
        eyebrow={activeCount ? t("catalog.activeFilters", { count: activeCount }) : t("catalog.filters")}
        title={t("catalog.filters")}
        subtitle={t("catalog.heroCopy")}
      >
        <form className="grid gap-4" onSubmit={onApply}>
          <FilterInput label={t("catalog.keyword")} value={form.keyword} onChange={(value) => setForm({ ...form, keyword: value })} placeholder={t("catalog.keywordPlaceholder")} />
          <FilterInput label={t("catalog.author")} value={form.author} onChange={(value) => setForm({ ...form, author: value })} placeholder={t("catalog.authorPlaceholder")} />
          <FilterInput label={t("catalog.publisher")} value={form.publisher} onChange={(value) => setForm({ ...form, publisher: value })} placeholder={t("catalog.publisherPlaceholder")} />
          <FilterInput label={t("catalog.isbn")} value={form.isbn} onChange={(value) => setForm({ ...form, isbn: value })} placeholder={t("catalog.isbnPlaceholder")} />

          <div className="grid grid-cols-2 gap-3">
            <FilterInput label={t("catalog.minPrice")} type="number" min="0" value={form.minPrice} onChange={(value) => setForm({ ...form, minPrice: value })} />
            <FilterInput label={t("catalog.maxPrice")} type="number" min="0" value={form.maxPrice} onChange={(value) => setForm({ ...form, maxPrice: value })} />
          </div>

          <label className="grid gap-2">
            <span className="text-xs font-black uppercase tracking-wider text-slate-400">{t("catalog.availability")}</span>
            <Select
              className="border-white/80 bg-white/85 shadow-sm focus:border-blue-400 focus:ring-blue-100"
              value={form.available}
              onChange={(event) => setForm({ ...form, available: event.target.value })}
            >
              <option value="">{t("catalog.anyAvailability")}</option>
              <option value="true">{t("catalog.availableOnly")}</option>
              <option value="false">{t("catalog.outOfStockOnly")}</option>
            </Select>
          </label>

          <div className={["grid gap-3 pt-1", compact ? "sticky bottom-0 bg-white/95 py-3 backdrop-blur" : ""].join(" ")}>
            <Button type="submit" className="shadow-[0_14px_30px_rgba(37,99,235,0.18)]">
              <Filter className="h-4 w-4" />
              {t("catalog.applyFilters")}
            </Button>
            <Button type="button" variant="secondary" className="bg-white/80" disabled={!hasActiveFilters} onClick={onClear}>
              {t("catalog.clearFilters")}
            </Button>
          </div>
        </form>
      </SidebarPanel>
    </div>
  );
}

function SortSelect({ value, size, onChange, t }) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200/70 bg-white/75 p-1.5 shadow-sm backdrop-blur">
      <div className="hidden items-center gap-1.5 px-2 text-[0.68rem] font-black uppercase tracking-[0.14em] text-slate-400 sm:flex">
        <SlidersHorizontal size={13} />
        {t("catalog.sort")}
      </div>
      <Select
        value={value}
        onChange={(event) => onChange({ sort: event.target.value })}
        aria-label={t("catalog.sort")}
        className="min-w-36 rounded-xl border-transparent bg-slate-50 px-3 py-2 text-sm font-bold shadow-none focus:border-blue-300"
      >
        <option value="newest">{t("catalog.sortNewest")}</option>
        <option value="price_asc">{t("catalog.sortPriceAsc")}</option>
        <option value="price_desc">{t("catalog.sortPriceDesc")}</option>
        <option value="best_selling">{t("catalog.sortBestSelling")}</option>
        <option value="name_asc">{t("catalog.sortNameAsc")}</option>
      </Select>
      <Select
        value={size}
        onChange={(event) => onChange({ size: Number(event.target.value) })}
        aria-label={t("catalog.pageSize")}
        className="min-w-32 rounded-xl border-transparent bg-slate-50 px-3 py-2 text-sm font-bold shadow-none focus:border-blue-300"
      >
        {PAGE_SIZES.map((candidate) => (
          <option key={candidate} value={candidate}>{t("catalog.perPage", { count: candidate })}</option>
        ))}
      </Select>
    </div>
  );
}

// Lightweight sidebar panel (different style from shared Panel — smaller, header tracking)
function SidebarPanel({ eyebrow, title, subtitle, children }) {
  return (
    <section className="relative overflow-hidden rounded-[24px] border border-white/80 bg-white/[0.78] p-4 shadow-[0_18px_54px_rgba(15,23,42,0.08)] backdrop-blur-xl">
      <div className="pointer-events-none absolute inset-x-6 top-0 h-px bg-gradient-to-r from-transparent via-blue-300/70 to-transparent" />
      {eyebrow && (
        <div className="mb-2 text-[0.62rem] font-black uppercase tracking-[0.18em] text-blue-600">{eyebrow}</div>
      )}
      <h2 className="text-base font-black text-slate-950">{title}</h2>
      {subtitle && <p className="mb-4 mt-1 line-clamp-2 text-xs font-semibold leading-5 text-slate-400">{subtitle}</p>}
      {!subtitle && <div className="mb-4" />}
      {children}
    </section>
  );
}

function FilterInput({ label, value, onChange, type = "text", ...props }) {
  return (
    <label className="grid gap-2">
      <span className="text-xs font-black uppercase tracking-wider text-slate-400">{label}</span>
      <Input
        className="border-white/80 bg-white/85 shadow-sm focus:border-blue-400 focus:ring-blue-100"
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        {...props}
      />
    </label>
  );
}

function CatalogBookGridSkeleton({ count }) {
  const safeCount = Math.min(Number(count) || DEFAULT_SIZE, 24);
  return (
    <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-4">
      {Array.from({ length: safeCount }).map((_, index) => (
        <div
          key={index}
          className="overflow-hidden rounded-[22px] border border-white/80 bg-white/[0.78] p-3 shadow-[0_16px_46px_rgba(15,23,42,0.06)] backdrop-blur"
          style={{ animationDelay: `${index * 45}ms` }}
        >
          <div className="catalog-skeleton aspect-[2/3] rounded-2xl" />
          <div className="catalog-skeleton mt-5 h-3 w-20 rounded-full" />
          <div className="catalog-skeleton mt-3 h-5 w-full rounded-full" />
          <div className="catalog-skeleton mt-2 h-4 w-2/3 rounded-full" />
          <div className="catalog-skeleton mt-5 h-6 w-28 rounded-full" />
        </div>
      ))}
    </div>
  );
}

function getActiveFilterChips(filters, t) {
  const chips = [];
  if (filters.keyword) chips.push({ key: "keyword", label: `${t("catalog.keyword")}: ${filters.keyword}` });
  if (filters.author) chips.push({ key: "author", label: `${t("catalog.author")}: ${filters.author}` });
  if (filters.publisher) chips.push({ key: "publisher", label: `${t("catalog.publisher")}: ${filters.publisher}` });
  if (filters.isbn) chips.push({ key: "isbn", label: `${t("catalog.isbn")}: ${filters.isbn}` });
  if (filters.minPrice) chips.push({ key: "minPrice", label: `${t("catalog.minPrice")}: ${filters.minPrice}` });
  if (filters.maxPrice) chips.push({ key: "maxPrice", label: `${t("catalog.maxPrice")}: ${filters.maxPrice}` });
  if (filters.available) chips.push({ key: "available", label: getAvailabilityLabel(filters.available, t) });
  if (filters.sort !== DEFAULT_SORT) chips.push({ key: "sort", label: getSortLabel(filters.sort, t) });
  if (filters.size !== DEFAULT_SIZE) chips.push({ key: "size", label: t("catalog.perPage", { count: filters.size }) });
  return chips;
}

function getAvailabilityLabel(value, t) {
  if (value === "true") return t("catalog.availableOnly");
  if (value === "false") return t("catalog.outOfStockOnly");
  return t("catalog.anyAvailability");
}

function getSortLabel(value, t) {
  const labels = {
    newest: t("catalog.sortNewest"),
    price_asc: t("catalog.sortPriceAsc"),
    price_desc: t("catalog.sortPriceDesc"),
    best_selling: t("catalog.sortBestSelling"),
    name_asc: t("catalog.sortNameAsc"),
  };
  return labels[value] || labels.newest;
}

function readFilters(searchParams) {
  return {
    keyword: searchParams.get("keyword") || searchParams.get("search") || "",
    author: searchParams.get("author") || "",
    publisher: searchParams.get("publisher") || "",
    isbn: searchParams.get("isbn") || "",
    minPrice: searchParams.get("minPrice") || "",
    maxPrice: searchParams.get("maxPrice") || "",
    available: parseAvailable(searchParams.get("available")),
    sort: searchParams.get("sort") || DEFAULT_SORT,
    page: positiveInt(searchParams.get("page"), DEFAULT_PAGE),
    size: PAGE_SIZES.includes(Number(searchParams.get("size"))) ? Number(searchParams.get("size")) : DEFAULT_SIZE,
  };
}

function emptyFilters() {
  return {
    keyword: "",
    author: "",
    publisher: "",
    isbn: "",
    minPrice: "",
    maxPrice: "",
    available: "",
    sort: DEFAULT_SORT,
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
  };
}

function emptyPageMeta(filters) {
  return readPageMeta([], { page: filters.page, size: filters.size, totalPages: 0 });
}

function buildSearchParams(filters, overrides = {}) {
  const next = { ...filters, ...overrides };
  const params = new URLSearchParams();
  appendParam(params, "keyword", next.keyword);
  appendParam(params, "author", next.author);
  appendParam(params, "publisher", next.publisher);
  appendParam(params, "isbn", next.isbn);
  appendParam(params, "minPrice", next.minPrice);
  appendParam(params, "maxPrice", next.maxPrice);
  appendParam(params, "available", next.available);
  if (next.sort && next.sort !== DEFAULT_SORT) params.set("sort", next.sort);
  if (Number(next.page) > DEFAULT_PAGE) params.set("page", String(next.page));
  if (Number(next.size) !== DEFAULT_SIZE) params.set("size", String(next.size));
  return params;
}

function appendParam(params, key, value) {
  const normalized = String(value ?? "").trim();
  if (normalized) params.set(key, normalized);
}

function parseAvailable(value) {
  if (value === "true" || value === "false") return value;
  return "";
}

function positiveInt(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function hasFilters(filters) {
  return Boolean(
    filters.keyword ||
      filters.author ||
      filters.publisher ||
      filters.isbn ||
      filters.minPrice ||
      filters.maxPrice ||
      filters.available ||
      filters.sort !== DEFAULT_SORT ||
      filters.page !== DEFAULT_PAGE ||
      filters.size !== DEFAULT_SIZE
  );
}
