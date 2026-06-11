import { useState } from "react";
import { useTranslation } from "react-i18next";
import RatingStars from "./RatingStars.jsx";

export default function ReviewForm({ title, onSubmit, onCancel, busy = false }) {
  const { t } = useTranslation();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");

  function submit(event) {
    event.preventDefault();
    const nextRating = Number(rating);
    const nextComment = comment.trim();
    if (nextRating < 1 || nextRating > 5) {
      setError(t("product.reviewRatingInvalid"));
      return;
    }
    if (!nextComment) {
      setError(t("product.reviewCommentRequired"));
      return;
    }
    setError("");
    onSubmit?.({ rating: nextRating, comment: nextComment, images: [] });
  }

  return (
    <form className="grid gap-5" onSubmit={submit}>
      {title && <h3 className="font-serif text-3xl font-bold text-slate-950">{title}</h3>}
      <label className="grid gap-2">
        <span className="text-sm font-black uppercase tracking-wider text-slate-400">{t("product.reviewRating")}</span>
        <RatingStars value={rating} onChange={setRating} />
      </label>
      <label className="grid gap-2">
        <span className="text-sm font-black uppercase tracking-wider text-slate-400">{t("product.reviewComment")}</span>
        <textarea
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          maxLength={2000}
          rows={5}
          className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          placeholder={t("product.reviewCommentPlaceholder")}
        />
      </label>
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-bold text-red-700">{error}</p>}
      <p className="text-sm font-semibold text-slate-500">{t("product.reviewModerationNotice")}</p>
      <div className="flex flex-wrap justify-end gap-3">
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-slate-700 hover:bg-slate-50"
          >
            {t("common.cancel")}
          </button>
        )}
        <button
          type="submit"
          disabled={busy}
          className="rounded-full bg-slate-950 px-5 py-3 text-sm font-bold text-white hover:bg-blue-600 disabled:cursor-wait disabled:opacity-60"
        >
          {busy ? t("common.working") : t("product.submitReview")}
        </button>
      </div>
    </form>
  );
}
