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
      className="group relative flex h-full min-w-0 flex-col overflow-hidden rounded-2xl bg-white p-4 shadow-[0_4px_20px_rgba(0,0,0,0.03)] ring-1 ring-slate-100 transition-all duration-400 hover:-translate-y-1.5 hover:shadow-[0_20px_40px_rgba(37,99,235,0.08)] hover:ring-blue-100"
    >
      {book.badge && (
        <div className="absolute right-3 top-3 z-20 rounded-full bg-gradient-to-r from-blue-600 to-blue-500 px-3 py-1 font-display text-xs tracking-widest text-white shadow-lg backdrop-blur">
          {book.badge}
        </div>
      )}

      <div className="relative mb-5 aspect-[2/3] w-full overflow-hidden rounded-xl bg-slate-100 shadow-inner">
        <img
          src={book.image || book.cover}
          alt={book.title}
          className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        {/* Book spine shadow */}
        <div className="absolute left-0 top-0 h-full w-2 bg-gradient-to-r from-black/20 to-transparent mix-blend-multiply" />
        
        {/* Hover overlay */}
        <div className="absolute inset-0 bg-slate-900/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
        
        {/* Hover button */}
        <div className="absolute inset-x-4 bottom-4 translate-y-4 text-center opacity-0 transition-all duration-400 group-hover:translate-y-0 group-hover:opacity-100">
          <div className="inline-block w-full rounded-full border border-white/20 bg-white/90 px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-slate-900 shadow-xl backdrop-blur-md transition-colors hover:bg-white">
            {t("common.viewDetails")}
          </div>
        </div>
      </div>

      <div className="flex flex-grow flex-col text-left">
        {book.catLabel && (
          <span className="mb-2 text-[10px] font-bold uppercase tracking-widest text-blue-500">
            {book.catLabel}
          </span>
        )}

        <h3 className="mb-1 line-clamp-2 font-serif text-lg font-bold leading-snug text-slate-900 transition-colors group-hover:text-blue-600">
          {book.title}
        </h3>

        <p className="mb-4 line-clamp-1 text-sm text-slate-500">
          {book.author}
        </p>

        {/* Stock Status Indicator */}
        <div className="mb-4 flex items-center gap-1.5">
          <span className="relative flex h-2 w-2">
            {stockQuantity > 0 && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>}
            <span className={`relative inline-flex h-2 w-2 rounded-full ${stockQuantity > 0 ? "bg-emerald-500" : "bg-red-500"}`}></span>
          </span>
          <span className="text-xs font-semibold text-slate-600">
            {stockQuantity > 0 ? t("home.inStock", { count: stockQuantity }) : t("home.outOfStock")}
          </span>
        </div>

        <div className="mt-auto flex items-end justify-between gap-3 pt-2">
          <div className="flex min-w-0 flex-col items-start gap-0.5">
            <span className="font-display text-2xl tracking-wide text-slate-900">
              {formatVND(book.price)}
            </span>

            {hasOldPrice && (
              <span className="text-xs font-medium text-slate-400 line-through">
                {formatVND(book.priceOld)}
              </span>
            )}
          </div>

          {rating > 0 && (
            <div className="mb-1 flex flex-shrink-0 items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-amber-600">
              <span className="text-xs font-bold">{rating.toFixed(1)}</span>
              <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}
