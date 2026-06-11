import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { formatVND } from "../utils/formatters.js";

export default function BookCard({ book }) {
  const { t } = useTranslation();
  const hasOldPrice = Number(book.priceOld || 0) > Number(book.price || 0);
  const rating = Number(book.rating || 0);
  const stockQuantity = Number(book.stockQuantity || 0);

  return (
    <Link
      to={`/product/${book.slug}`}
      className="group relative flex h-full min-w-0 flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white p-4 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-blue-100 hover:shadow-xl"
    >
      {book.badge && (
        <div className="absolute right-4 top-4 z-20 rounded-full bg-blue-600/90 px-3 py-1 text-xs font-bold uppercase tracking-wider text-white shadow-lg backdrop-blur">
          {book.badge}
        </div>
      )}

      <div className="relative mb-5 aspect-[2/3] w-full overflow-hidden rounded-xl bg-slate-100">
        <img
          src={book.image || book.cover}
          alt={book.title}
          className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/30 via-transparent to-white/10 opacity-70" />
        <div className="absolute left-0 top-0 h-full w-2 bg-slate-950/70" />
        <div className="absolute inset-x-4 bottom-4 translate-y-3 rounded-full bg-white/95 px-4 py-2 text-center text-sm font-bold text-slate-900 opacity-0 shadow-lg transition duration-300 group-hover:translate-y-0 group-hover:opacity-100">
          {t("common.viewDetails")}
        </div>
      </div>

      <div className="flex flex-grow flex-col text-left">
        {book.catLabel && (
          <span className="mb-2 text-xs font-bold uppercase tracking-wider text-blue-600">
            {book.catLabel}
          </span>
        )}

        <h3 className="mb-1 line-clamp-2 font-serif text-lg font-bold leading-snug text-slate-900">
          {book.title}
        </h3>

        <p className="mb-4 line-clamp-1 text-sm text-slate-500">
          {book.author}
        </p>

        <span className={[
          "mb-3 inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-bold",
          stockQuantity > 0 ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-600"
        ].join(" ")}>
          {stockQuantity > 0 ? t("home.inStock", { count: stockQuantity }) : t("home.outOfStock")}
        </span>

        <div className="mt-auto flex items-end justify-between gap-3">
          <div className="flex min-w-0 flex-col items-start gap-1">
            <span className="text-lg font-bold text-slate-900">
              {formatVND(book.price)}
            </span>

            {hasOldPrice && (
              <span className="text-xs text-slate-400 line-through">
                {formatVND(book.priceOld)}
              </span>
            )}
          </div>

          {rating > 0 && (
            <div className="flex flex-shrink-0 items-center gap-1 text-amber-500">
              <span className="text-sm font-bold">{rating.toFixed(1)}</span>
              <svg
                className="h-4 w-4"
                fill="currentColor"
                viewBox="0 0 20 20"
                aria-hidden="true"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}
