import { useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

import { getCategories, getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, normalizeCategory, pageRows } from "../utils/mappers.js";

const categoryTitles = {
  all: "All Books",
  business: "Business & Finance",
  "self-help": "Self-help & Growth",
  literature: "Literature & Fiction",
  skills: "Skills & Wellness",
};

const sortOptions = [
  ["all", "All"],
  ["popular", "Popular"],
  ["new", "Latest"],
  ["price", "Price"],
];

export default function CategoryPage() {
  const { slug = "all" } = useParams();
  const [searchParams] = useSearchParams();
  const search = searchParams.get("search") || "";
  const [sort, setSort] = useState("all");
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([
    { id: "all", slug: "all", label: "All Books" },
  ]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getCategories()
      .then((rows) => {
        const list = pageRows(rows).map(normalizeCategory).filter(Boolean);
        if (list.length) {
          setCategories([{ id: "all", slug: "all", label: "All Books" }, ...list]);
        }
      })
      .catch((error) => setMessage(error.message || "Could not load backend categories."));
  }, []);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setMessage("");

    getProducts({
      page: 1,
      size: 50,
      keyword: search,
      categorySlug: slug !== "all" ? slug : "",
    })
      .then((page) => {
        if (!alive) return;
        setBooks(pageRows(page).map((row) => normalizeBook(row)));
      })
      .catch((error) => {
        if (alive) {
          setBooks([]);
          setMessage(error.message || "Could not load backend products.");
        }
      })
      .finally(() => alive && setLoading(false));

    return () => {
      alive = false;
    };
  }, [slug, search]);

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    let list = books.filter((book) => {
      const categoryOk = slug === "all" || book.cat === slug;
      const searchOk =
        !q ||
        book.title.toLowerCase().includes(q) ||
        book.author.toLowerCase().includes(q);
      return categoryOk && searchOk;
    });

    if (sort === "popular") list = [...list].sort((a, b) => b.sold - a.sold);
    if (sort === "price") list = [...list].sort((a, b) => a.price - b.price);
    if (sort === "new") list = [...list].reverse();
    return list;
  }, [books, slug, search, sort]);

  const title = search ? `Search: "${search}"` : categoryTitles[slug] || "Books";

  return (
    <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-8 px-4 pb-20 pt-28 md:px-8 lg:grid-cols-[240px_1fr]">
      <aside className="lg:sticky lg:top-28 lg:self-start">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-slate-400">
            Categories
          </h2>
          <div className="grid gap-1">
            {categories.filter(Boolean).map((category) => (
              <Link
                key={category.id || category.slug}
                to={`/category/${category.slug}`}
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
        </div>
      </aside>

      <main className="min-w-0">
        <div className="mb-8 flex flex-col gap-4 border-b border-slate-200 pb-6 md:flex-row md:items-end md:justify-between">
          <div>
            <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
              Catalog
            </span>
            <h1 className="mt-3 font-serif text-4xl font-bold text-slate-950 md:text-5xl">
              {title}
            </h1>
            <p className="mt-2 text-sm text-slate-500">
              {loading ? "Loading books..." : `${filtered.length} books found`}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {sortOptions.map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setSort(value)}
                className={[
                  "rounded-full px-4 py-2 text-sm font-bold transition-colors",
                  sort === value
                    ? "bg-slate-950 text-white"
                    : "border border-slate-200 bg-white text-slate-600 hover:bg-slate-50",
                ].join(" ")}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {message && (
          <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
            {message}
          </div>
        )}

        {loading ? (
          <EmptyState title="Loading catalog..." />
        ) : filtered.length ? (
          <div className="grid grid-cols-2 gap-5 md:grid-cols-3 xl:grid-cols-4">
            {filtered.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        ) : (
          <EmptyState title="No books found" />
        )}
      </main>
    </div>
  );
}

function EmptyState({ title }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h3 className="font-serif text-2xl font-bold text-slate-950">{title}</h3>
    </div>
  );
}
