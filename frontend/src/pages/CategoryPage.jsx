import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { SlidersHorizontal, X } from "lucide-react";

import { getCategories, getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, normalizeCategory, pageRows } from "../utils/mappers.js";

const DEFAULT_PAGE = 1;
const DEFAULT_SIZE = 12;
const DEFAULT_SORT = "newest";
const PAGE_SIZES = [12, 24, 48];

export default function CategoryPage() {
  const { t } = useTranslation();
  const { slug = "all" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const searchKey = searchParams.toString();
  const filters = useMemo(() => readFilters(searchParams), [searchKey]);
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
  }, [searchParams]);

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
        setPageMeta({
          currentPage: Number(page?.currentPage || filters.page),
          totalPages: Number(page?.totalPages || 0),
          pageSize: Number(page?.pageSize || filters.size),
          totalElements: Number(page?.totalElements || 0),
          hasNext: Boolean(page?.hasNext),
          hasPrevious: Boolean(page?.hasPrevious),
        });
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
    <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-8 px-4 pb-20 pt-28 md:px-8 lg:grid-cols-[280px_1fr]">
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
        <div className="mb-8 flex flex-col gap-4 border-b border-slate-200 pb-6 md:flex-row md:items-end md:justify-between">
          <div>
            <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
              {t("catalog.catalog")}
            </span>
            <h1 className="mt-3 font-serif text-4xl font-bold text-slate-950 md:text-5xl">
              {title}
            </h1>
            <p className="mt-2 text-sm text-slate-500">
              {loading
                ? t("catalog.loadingBooks")
                : t("catalog.booksFound", { count: pageMeta.totalElements })}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setMobileFiltersOpen(true)}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm lg:hidden"
            >
              <SlidersHorizontal className="h-4 w-4" />
              {t("catalog.filters")}
            </button>
            <SortSelect
              value={filters.sort}
              size={filters.size}
              onChange={(changes) => {
                setSearchParams(buildSearchParams(filters, { ...changes, page: DEFAULT_PAGE }), { replace: false });
              }}
              t={t}
            />
          </div>
        </div>

        {message && (
          <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
            {message}
          </div>
        )}

        {loading ? (
          <BookGridSkeleton count={filters.size} />
        ) : books.length ? (
          <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-4">
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        ) : (
          <EmptyState title={t("catalog.noBooks")} action={hasActiveFilters ? (
            <button type="button" onClick={clearFilters} className="rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white hover:bg-blue-600">
              {t("catalog.clearFilters")}
            </button>
          ) : null} />
        )}

        <Pagination meta={pageMeta} loading={loading} onPage={goToPage} t={t} />
      </main>

      {mobileFiltersOpen && (
        <div className="fixed inset-0 z-[6000] bg-slate-950/60 backdrop-blur-sm lg:hidden" role="presentation">
          <div className="ml-auto flex h-full w-[min(420px,92vw)] flex-col overflow-y-auto bg-white p-5 shadow-2xl">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="font-serif text-2xl font-bold text-slate-950">{t("catalog.filters")}</h2>
              <button
                type="button"
                onClick={() => setMobileFiltersOpen(false)}
                className="grid h-10 w-10 place-items-center rounded-full bg-slate-100 text-slate-700"
                aria-label={t("common.close")}
              >
                <X className="h-5 w-5" />
              </button>
            </div>
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
          </div>
        </div>
      )}
    </div>
  );
}

function CatalogSidebar({ categories, slug, categoryLink, form, setForm, onApply, onClear, hasActiveFilters, t, compact = false }) {
  return (
    <div className="grid gap-5">
      <Panel title={t("common.categories")}>
        <div className="grid gap-1">
          {categories.filter(Boolean).map((category) => (
            <Link
              key={category.id || category.slug}
              to={categoryLink(category.slug)}
              className={[
                "rounded-xl px-3 py-2 text-sm font-semibold transition-colors",
                slug === category.slug
                  ? "bg-blue-600 text-white"
                  : "text-slate-600 hover:bg-slate-100 hover:text-slate-950",
              ].join(" ")}
            >
              {category.label}
            </Link>
          ))}
        </div>
      </Panel>

      <Panel title={t("catalog.filters")}>
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
            <select
              value={form.available}
              onChange={(event) => setForm({ ...form, available: event.target.value })}
              className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-semibold text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
            >
              <option value="">{t("catalog.anyAvailability")}</option>
              <option value="true">{t("catalog.availableOnly")}</option>
              <option value="false">{t("catalog.outOfStockOnly")}</option>
            </select>
          </label>

          <div className={["grid gap-3", compact ? "sticky bottom-0 bg-white py-3" : ""].join(" ")}>
            <button type="submit" className="rounded-full bg-slate-950 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600">
              {t("catalog.applyFilters")}
            </button>
            <button
              type="button"
              onClick={onClear}
              disabled={!hasActiveFilters}
              className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {t("catalog.clearFilters")}
            </button>
          </div>
        </form>
      </Panel>
    </div>
  );
}

function SortSelect({ value, size, onChange, t }) {
  return (
    <div className="flex flex-wrap gap-2">
      <select
        value={value}
        onChange={(event) => onChange({ sort: event.target.value })}
        className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm outline-none focus:border-blue-500"
        aria-label={t("catalog.sort")}
      >
        <option value="newest">{t("catalog.sortNewest")}</option>
        <option value="price_asc">{t("catalog.sortPriceAsc")}</option>
        <option value="price_desc">{t("catalog.sortPriceDesc")}</option>
        <option value="best_selling">{t("catalog.sortBestSelling")}</option>
        <option value="name_asc">{t("catalog.sortNameAsc")}</option>
      </select>
      <select
        value={size}
        onChange={(event) => onChange({ size: Number(event.target.value) })}
        className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm outline-none focus:border-blue-500"
        aria-label={t("catalog.pageSize")}
      >
        {PAGE_SIZES.map((candidate) => (
          <option key={candidate} value={candidate}>{t("catalog.perPage", { count: candidate })}</option>
        ))}
      </select>
    </div>
  );
}

function Pagination({ meta, loading, onPage, t }) {
  if (!meta.totalPages || meta.totalPages <= 1) return null;

  return (
    <div className="mt-10 flex flex-wrap items-center justify-center gap-2">
      <PageButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(1)}>{t("catalog.firstPage")}</PageButton>
      <PageButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(meta.currentPage - 1)}>{t("catalog.previousPage")}</PageButton>
      <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold text-slate-700">
        {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })}
      </span>
      <PageButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.currentPage + 1)}>{t("catalog.nextPage")}</PageButton>
      <PageButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.totalPages)}>{t("catalog.lastPage")}</PageButton>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-slate-400">{title}</h2>
      {children}
    </section>
  );
}

function FilterInput({ label, value, onChange, type = "text", ...props }) {
  return (
    <label className="grid gap-2">
      <span className="text-xs font-black uppercase tracking-wider text-slate-400">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-semibold text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
        {...props}
      />
    </label>
  );
}

function PageButton({ disabled, onClick, children }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
    >
      {children}
    </button>
  );
}

function BookGridSkeleton({ count }) {
  const safeCount = Math.min(Number(count) || DEFAULT_SIZE, 24);
  return (
    <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-4">
      {Array.from({ length: safeCount }).map((_, index) => (
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

function EmptyState({ title, action }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h3 className="font-serif text-2xl font-bold text-slate-950">{title}</h3>
      {action && <div className="mt-6 flex justify-center">{action}</div>}
    </div>
  );
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
  return {
    currentPage: filters.page,
    totalPages: 0,
    pageSize: filters.size,
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  };
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
