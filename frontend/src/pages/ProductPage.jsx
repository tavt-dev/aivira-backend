import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";

import { addCartItem } from "../api/cartApi.js";
import { getProduct } from "../api/catalogApi.js";
import { getProductReviews } from "../api/reviewApi.js";
import RatingStars from "../components/reviews/RatingStars.jsx";
import { discount, formatSold, formatVND } from "../utils/formatters.js";
import { normalizeBook, normalizeReview, pageMeta as readPageMeta, pageRows } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

const REVIEW_SIZE = 5;

export default function ProductPage({ onAuth }) {
  const { t } = useTranslation();
  const { slug } = useParams();
  const [book, setBook] = useState(null);
  const [selectedImage, setSelectedImage] = useState("");
  const [selectedVariationId, setSelectedVariationId] = useState("");
  const [message, setMessage] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    getProduct(slug, { signal: controller.signal })
      .then((data) => {
        const normalized = normalizeBook(data);
        const defaultVariation = pickDefaultVariation(normalized.variations);
        setBook(normalized);
        setSelectedVariationId(defaultVariation?.id || "");
        setSelectedImage(buildGallery(normalized)[0]?.url || normalized.image);
        setQuantity(1);
        setMessage("");
      })
      .catch((error) => {
        if (error.name === "AbortError") return;
        setBook(null);
        setMessage(error.message || t("product.notFound"));
      });

    return () => controller.abort();
  }, [slug, t]);

  const selectedVariation = useMemo(
    () => book?.variations?.find((variation) => String(variation.id) === String(selectedVariationId)),
    [book?.variations, selectedVariationId]
  );
  const stockQuantity = Number(selectedVariation?.stockQuantity ?? book?.stockQuantity ?? 0);
  const canAdd = Boolean(selectedVariation?.id) && stockQuantity > 0 && !busy;
  const gallery = useMemo(() => buildGallery(book), [book]);
  const hasDiscount = discount(book) > 0;

  useEffect(() => {
    setQuantity((current) => Math.min(Math.max(1, current), Math.max(1, stockQuantity || 1)));
  }, [stockQuantity]);

  async function addToCart() {
    if (!getAccessToken()) {
      onAuth?.();
      return;
    }

    if (!selectedVariation?.id) {
      setMessage(t("product.noVariation"));
      return;
    }
    if (stockQuantity <= 0) {
      setMessage(t("product.outOfStock"));
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await addCartItem({ productVariationId: selectedVariation.id, quantity });
      setMessage(t("product.added"));
      window.dispatchEvent(new Event("aivira-cart"));
    } catch (error) {
      setMessage(error.message || t("product.addFailed"));
    } finally {
      setBusy(false);
    }
  }

  if (!book) {
    return (
      <div className="mx-auto w-full max-w-5xl px-4 pb-20 pt-28 md:px-8">
        <div className="rounded-3xl border border-slate-200 bg-white px-8 py-16 text-center shadow-sm">
          <h1 className="font-serif text-3xl font-bold text-slate-950">
            {message || t("product.loading")}
          </h1>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <div className="mb-8 flex flex-wrap items-center gap-2 text-sm font-semibold text-slate-500">
        <Link className="hover:text-blue-600" to="/">
          {t("common.home")}
        </Link>
        <span>/</span>
        <Link className="hover:text-blue-600" to={`/category/${book.cat || "all"}`}>
          {book.catLabel || t("common.books")}
        </Link>
        <span>/</span>
        <span className="text-slate-900">{book.title}</span>
      </div>

      {message && (
        <div className="mb-6 rounded-2xl border border-blue-100 bg-blue-50 px-5 py-4 text-sm font-semibold text-blue-700">
          {message}
        </div>
      )}

      <div className="grid gap-10 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-8 lg:grid-cols-[minmax(280px,440px)_1fr]">
        <Gallery gallery={gallery} selectedImage={selectedImage} onSelect={setSelectedImage} title={book.title} t={t} />

        <section className="flex min-w-0 flex-col justify-center">
          <span className="mb-4 inline-flex w-fit rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
            {book.catLabel || t("common.books")}
          </span>
          <h1 className="font-serif text-4xl font-bold leading-tight text-slate-950 md:text-6xl">
            {book.title}
          </h1>
          <p className="mt-3 text-lg font-medium text-slate-500">{t("product.byAuthor", { author: book.author })}</p>

          <div className="mt-6 flex flex-wrap gap-3 text-sm font-bold text-slate-600">
            <span className="rounded-full bg-slate-100 px-4 py-2">
              {t("product.rating", { rating: Number(book.rating || 0).toFixed(1) })}
            </span>
            <span className="rounded-full bg-slate-100 px-4 py-2">
              {t("product.sold", { sold: formatSold(book.sold) })}
            </span>
            <span className={[
              "rounded-full px-4 py-2",
              stockQuantity > 0 ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-600"
            ].join(" ")}>
              {stockQuantity > 0 ? t("product.inStock", { count: stockQuantity }) : t("product.outOfStock")}
            </span>
          </div>

          <div className="mt-8 flex flex-wrap items-end gap-4">
            {hasDiscount && (
              <span className="text-lg font-semibold text-slate-400 line-through">
                {formatVND(book.priceOld)}
              </span>
            )}
            <span className="font-serif text-5xl font-bold text-slate-950">
              {formatVND(book.price)}
            </span>
            {hasDiscount && (
              <span className="rounded-full bg-orange-50 px-3 py-1 text-sm font-bold text-orange-600">
                {t("product.off", { discount: discount(book) })}
              </span>
            )}
          </div>

          <p className="mt-8 max-w-2xl text-base leading-7 text-slate-600">
            {book.desc || t("product.noDescription")}
          </p>

          <MetadataGrid book={book} t={t} />

          <VariationSelector
            variations={book.variations}
            selectedVariationId={selectedVariationId}
            onSelect={setSelectedVariationId}
            t={t}
          />

          <div className="mt-8 flex flex-wrap items-center gap-4">
            <QuantityStepper value={quantity} max={Math.max(1, stockQuantity || 1)} onChange={setQuantity} t={t} />
          </div>

          <div className="mt-8 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={addToCart}
              disabled={!canAdd}
              className="rounded-full bg-slate-950 px-7 py-4 text-sm font-bold text-white transition-colors hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {busy ? t("common.working") : t("product.addToCart")}
            </button>
            <Link
              to="/checkout"
              className="rounded-full border border-slate-200 px-7 py-4 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
            >
              {t("product.checkout")}
            </Link>
          </div>
        </section>
      </div>

      <ReviewList slug={slug} />
    </div>
  );
}

function Gallery({ gallery, selectedImage, onSelect, title, t }) {
  return (
    <div className="grid gap-4">
      <div className="relative overflow-hidden rounded-2xl bg-slate-100">
        <div className="aspect-[3/4]">
          <img
            src={selectedImage || gallery[0]?.url}
            alt={title}
            className="h-full w-full object-cover"
          />
        </div>
        <div className="absolute left-0 top-0 h-full w-3 bg-slate-950/80" />
      </div>
      {gallery.length > 1 && (
        <div className="grid grid-cols-5 gap-2" aria-label={t("product.gallery")}>
          {gallery.map((item) => (
            <button
              key={item.url}
              type="button"
              onClick={() => onSelect(item.url)}
              className={[
                "aspect-[2/3] overflow-hidden rounded-xl border bg-slate-100",
                selectedImage === item.url ? "border-blue-600 ring-2 ring-blue-100" : "border-slate-200"
              ].join(" ")}
            >
              <img src={item.url} alt={item.alt || title} className="h-full w-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function MetadataGrid({ book, t }) {
  const rows = [
    [t("product.metaAuthor"), book.author],
    [t("product.metaIsbn"), book.isbn],
    [t("product.metaPublisher"), book.publisher],
    [t("product.metaPublicationYear"), book.publicationYear],
    [t("product.metaLanguage"), book.bookLanguage],
    [t("product.metaPageCount"), book.pageCount],
    [t("product.metaFormat"), book.bookFormat],
    [t("product.metaDimensions"), book.dimensions],
    [t("product.metaCategory"), book.catLabel],
  ].filter(([, value]) => value !== undefined && value !== null && value !== "");

  if (!rows.length) return null;

  return (
    <div className="mt-8 rounded-2xl border border-slate-200 bg-slate-50 p-5">
      <h2 className="font-serif text-2xl font-bold text-slate-950">{t("product.metadata")}</h2>
      <dl className="mt-4 grid gap-3 md:grid-cols-2">
        {rows.map(([label, value]) => (
          <div key={label} className="rounded-xl bg-white px-4 py-3">
            <dt className="text-xs font-black uppercase tracking-wider text-slate-400">{label}</dt>
            <dd className="mt-1 font-semibold text-slate-800">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function VariationSelector({ variations, selectedVariationId, onSelect, t }) {
  const activeVariations = (variations || []).filter((variation) => variation.active !== false);
  if (!activeVariations.length) return null;

  return (
    <div className="mt-8">
      <h2 className="text-sm font-black uppercase tracking-wider text-slate-400">{t("product.variations")}</h2>
      <div className="mt-3 grid gap-3 md:grid-cols-2">
        {activeVariations.map((variation) => (
          <button
            key={variation.id}
            type="button"
            onClick={() => onSelect(variation.id)}
            className={[
              "rounded-2xl border p-4 text-left transition-colors",
              String(selectedVariationId) === String(variation.id)
                ? "border-blue-600 bg-blue-50"
                : "border-slate-200 bg-white hover:bg-slate-50"
            ].join(" ")}
          >
            <strong className="block text-slate-950">
              {[variation.size, variation.color].filter(Boolean).join(" / ") || t("product.defaultVariation")}
            </strong>
            <span className="mt-1 block text-xs font-semibold text-slate-500">{variation.sku}</span>
            <span className="mt-2 block text-sm font-bold text-slate-700">
              {t("product.variationStock", { count: variation.stockQuantity || 0 })}
            </span>
            {Number(variation.additionalPrice || 0) > 0 && (
              <span className="mt-1 block text-sm font-bold text-blue-700">
                +{formatVND(variation.additionalPrice)}
              </span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}

function QuantityStepper({ value, max, onChange, t }) {
  return (
    <div className="flex items-center gap-3 text-sm font-bold text-slate-600">
      <span>{t("product.quantity")}</span>
      <div className="flex items-center overflow-hidden rounded-full border border-slate-200 bg-slate-50">
        <button type="button" onClick={() => onChange(Math.max(1, value - 1))} className="h-11 w-11 text-lg text-slate-700">-</button>
        <input
          type="number"
          min="1"
          max={max}
          value={value}
          onChange={(event) => onChange(Math.min(max, Math.max(1, Number(event.target.value) || 1)))}
          className="h-11 w-16 border-x border-slate-200 bg-white text-center text-slate-950 outline-none"
        />
        <button type="button" onClick={() => onChange(Math.min(max, value + 1))} className="h-11 w-11 text-lg text-slate-700">+</button>
      </div>
    </div>
  );
}

function ReviewList({ slug }) {
  const { t } = useTranslation();
  const [reviews, setReviews] = useState([]);
  const [meta, setMeta] = useState({ currentPage: 1, totalPages: 0, hasNext: false, hasPrevious: false });
  const [rating, setRating] = useState("");
  const [sort, setSort] = useState("newest");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setMessage("");

    getProductReviews(slug, { rating, sort, page, size: REVIEW_SIZE }, { signal: controller.signal })
      .then((payload) => {
        setReviews(pageRows(payload).map(normalizeReview));
        setMeta(readPageMeta(payload, { page, size: 5 }));
      })
      .catch((error) => {
        if (error.name === "AbortError") return;
        setReviews([]);
        setMessage(error.message || t("product.reviewLoadFailed"));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [page, rating, slug, sort, t]);

  function changeRating(nextRating) {
    setRating(nextRating);
    setPage(1);
  }

  function changeSort(nextSort) {
    setSort(nextSort);
    setPage(1);
  }

  return (
    <section className="mt-10 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="font-serif text-3xl font-bold text-slate-950">{t("product.reviews")}</h2>
          <p className="mt-2 text-sm text-slate-500">{t("product.publicReviewsCopy")}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <select value={rating} onChange={(event) => changeRating(event.target.value)} className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">
            <option value="">{t("product.allRatings")}</option>
            {[5, 4, 3, 2, 1].map((value) => (
              <option key={value} value={value}>{t("product.ratingFilter", { rating: value })}</option>
            ))}
          </select>
          <select value={sort} onChange={(event) => changeSort(event.target.value)} className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700">
            <option value="newest">{t("product.reviewNewest")}</option>
            <option value="oldest">{t("product.reviewOldest")}</option>
            <option value="rating_desc">{t("product.reviewRatingDesc")}</option>
            <option value="rating_asc">{t("product.reviewRatingAsc")}</option>
          </select>
        </div>
      </div>

      {message && <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{message}</div>}

      {loading ? (
        <div className="mt-6 grid gap-3">
          {Array.from({ length: 3 }).map((_, index) => <div key={index} className="h-28 animate-pulse rounded-2xl bg-slate-100" />)}
        </div>
      ) : reviews.length ? (
        <div className="mt-6 grid gap-4">
          {reviews.map((review) => <ReviewCard key={review.id} review={review} />)}
        </div>
      ) : (
        <div className="mt-6 rounded-2xl border border-dashed border-slate-300 px-6 py-12 text-center">
          <h3 className="font-serif text-2xl font-bold text-slate-950">{t("product.noReviews")}</h3>
          <p className="mt-2 text-sm text-slate-500">{t("product.noReviewsCopy")}</p>
        </div>
      )}

      {meta.totalPages > 1 && (
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button disabled={!meta.hasPrevious || loading} onClick={() => setPage((current) => Math.max(1, current - 1))} className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-50">
            {t("catalog.previousPage")}
          </button>
          <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold text-slate-700">
            {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })}
          </span>
          <button disabled={!meta.hasNext || loading} onClick={() => setPage((current) => current + 1)} className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 disabled:opacity-50">
            {t("catalog.nextPage")}
          </button>
        </div>
      )}
    </section>
  );
}

function ReviewCard({ review }) {
  const { t, i18n } = useTranslation();
  return (
    <article className="rounded-2xl border border-slate-100 bg-slate-50 p-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <strong className="text-slate-950">{review.username}</strong>
          <div className="mt-1"><RatingStars value={review.rating} size="sm" /></div>
        </div>
        {review.createdAt && (
          <time className="text-sm font-semibold text-slate-500">
            {new Intl.DateTimeFormat(String(i18n.language || "vi").startsWith("en") ? "en-US" : "vi-VN").format(new Date(review.createdAt))}
          </time>
        )}
      </div>
      {review.comment && <p className="mt-4 leading-7 text-slate-700">{review.comment}</p>}
      {review.images?.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {review.images.map((image) => (
            <img key={image.id || image.imageUrl} src={image.imageUrl} alt={review.comment || review.username} className="h-20 w-20 rounded-xl object-cover" />
          ))}
        </div>
      )}
      {review.adminReply && (
        <div className="mt-4 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          <strong className="block">{t("product.adminReply")}</strong>
          <span>{review.adminReply}</span>
        </div>
      )}
    </article>
  );
}

function buildGallery(book) {
  if (!book) return [];
  const rows = [
    book.thumbnailUrl ? { url: book.thumbnailUrl, alt: book.title } : null,
    ...(book.media || [])
      .filter((item) => item.active !== false && (!item.mediaType || item.mediaType === "IMAGE"))
      .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
      .map((item) => ({ url: item.mediaUrl, alt: item.altText || book.title })),
    ...(book.variations || [])
      .filter((item) => item.imageUrl)
      .map((item) => ({ url: item.imageUrl, alt: item.size || book.title })),
    book.image ? { url: book.image, alt: book.title } : null,
  ].filter((item) => item?.url);

  const seen = new Set();
  return rows.filter((item) => {
    if (seen.has(item.url)) return false;
    seen.add(item.url);
    return true;
  });
}

function pickDefaultVariation(variations = []) {
  return variations.find((variation) => variation.active !== false && Number(variation.stockQuantity || 0) > 0)
    || variations.find((variation) => variation.active !== false)
    || variations[0];
}
