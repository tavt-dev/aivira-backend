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
  skills: "Skills & Wellness"
};

export default function CategoryPage() {
  const { slug = "all" } = useParams();
  const [searchParams] = useSearchParams();
  const search = searchParams.get("search") || "";
  const [sort, setSort] = useState("all");
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([{ id: "all", slug: "all", label: "All Books" }]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getCategories()
      .then((rows) => {
        const list = pageRows(rows).map(normalizeCategory).filter(Boolean);
        if (list.length) setCategories([{ id: "all", slug: "all", label: "All Books" }, ...list]);
      })
      .catch((error) => setMessage(error.message || "Could not load backend categories."));
  }, []);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setMessage("");
    getProducts({ page: 1, size: 50, keyword: search, categorySlug: slug !== "all" ? slug : "" })
      .then((page) => {
        const rows = pageRows(page);
        if (!alive) return;
        setBooks(rows.map((row) => normalizeBook(row)));
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
      const searchOk = !q || book.title.toLowerCase().includes(q) || book.author.toLowerCase().includes(q);
      return categoryOk && searchOk;
    });
    if (sort === "popular") list = [...list].sort((a, b) => b.sold - a.sold);
    if (sort === "price") list = [...list].sort((a, b) => a.price - b.price);
    if (sort === "new") list = [...list].reverse();
    return list;
  }, [books, slug, search, sort]);

  return (
    <div className="catalog-page">
      <aside className="sidebar">
        <div className="sb-title">Categories</div>
        {categories.filter(Boolean).map((category) => (
          <Link key={category.id || category.slug} className={slug === category.slug ? "active" : ""} to={`/category/${category.slug}`}>
            {category.label}
          </Link>
        ))}
      </aside>
      <section className="main-content">
        <div className="top-bar">
          <h1>{search ? `Search: "${search}"` : categoryTitles[slug] || "Books"}</h1>
          <div className="tb-sort">
            {["all", "popular", "new", "price"].map((item) => (
              <button key={item} className={sort === item ? "sort-btn active" : "sort-btn"} onClick={() => setSort(item)}>
                {item === "all" ? "All" : item === "popular" ? "Popular" : item === "new" ? "Latest" : "Price"}
              </button>
            ))}
          </div>
        </div>
        {message && <div className="notice page-notice">{message}</div>}
        {loading ? <div className="empty">Loading catalog...</div> : filtered.length ? (
          <div className="book-grid">{filtered.map((book) => <BookCard key={book.id} book={book} />)}</div>
        ) : (
          <div className="empty"><h3>No books found</h3></div>
        )}
      </section>
    </div>
  );
}
