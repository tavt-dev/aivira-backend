export default function RatingStars({ value = 0, onChange, size = "md" }) {
  const rating = Number(value || 0);
  const interactive = typeof onChange === "function";
  const starClass = size === "sm" ? "text-lg" : "text-2xl";

  return (
    <div className="flex items-center gap-0.5" role={interactive ? "radiogroup" : undefined}>
      {[1, 2, 3, 4, 5].map((star) => {
        const active = rating >= star;
        if (!interactive) {
          return (
            <span key={star} className={[starClass, active ? "text-amber-400" : "text-slate-200"].join(" ")}>
              ★
            </span>
          );
        }

        return (
          <button
            key={star}
            type="button"
            onClick={() => onChange(star)}
            className={[starClass, "leading-none transition-colors", active ? "text-amber-400" : "text-slate-300 hover:text-amber-300"].join(" ")}
            aria-label={`${star} stars`}
          >
            ★
          </button>
        );
      })}
    </div>
  );
}
