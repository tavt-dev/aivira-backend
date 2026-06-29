import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import { getAdminReviews, moderateReview, replyReview } from "../../api/adminReviewsApi.js";
import RatingStars from "../../components/reviews/RatingStars.jsx";
import {
  Button,
  InfoCard,
  Input,
  MetaRow as Meta,
  Modal,
  Notice,
  PageHeader,
  Pagination,
  Panel,
  Select,
  Textarea,
  useConfirm,
  useToast,
} from "../../components/ui/index.jsx";
import { formatDateTime } from "../../utils/formatters.js";
import { normalizeReview, pageMeta as readPageMeta, pageRows } from "../../utils/mappers.js";

const PAGE_SIZES = [10, 20, 50];
const RATINGS = [5, 4, 3, 2, 1];

const emptyFilters = {
  approved: "",
  visible: "",
  rating: "",
  keyword: "",
  productId: "",
  userId: "",
  page: 1,
  size: 20,
};

export default function AdminReviewsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const appliedFilters = useMemo(() => filtersFromSearch(searchParams), [searchParams]);
  const [filters, setFilters] = useState(appliedFilters);
  const [reviews, setReviews] = useState([]);
  const [pageMeta, setPageMeta] = useState(createEmptyMeta(appliedFilters));
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState("");
  const [replyDraft, setReplyDraft] = useState("");
  const [moderationDraft, setModerationDraft] = useState({ approved: false, visible: true });
  const [moderationOpen, setModerationOpen] = useState(false);
  const [replyOpen, setReplyOpen] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);

  const refreshReviews = useCallback(async (nextFilters = appliedFilters) => {
    setLoading(true);
    setMessage("");
    try {
      const page = await getAdminReviews(toQuery(nextFilters));
      const rows = pageRows(page).map(normalizeReview);
      setReviews(rows);
      setPageMeta(readPageMeta(page, { page: nextFilters.page, size: nextFilters.size }));
      if (selected) {
        const updatedSelected = rows.find((review) => review.id === selected.id);
        if (updatedSelected) syncSelected(updatedSelected);
      }
    } catch (error) {
      setReviews([]);
      setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.reviewLoadFailed"));
    } finally {
      setLoading(false);
    }
  }, [appliedFilters, selected, t]);

  useEffect(() => {
    setFilters(appliedFilters);
    refreshReviews(appliedFilters);
  }, [appliedFilters, refreshReviews]);

  function applyFilters(event) {
    event.preventDefault();
    const validation = validateFilters(filters, t);
    if (validation) {
      setMessage(validation);
      return;
    }
    setSearchParams(toSearchParams({ ...filters, page: 1, size: Number(filters.size || 20) }));
  }

  function clearFilters() {
    setSearchParams(toSearchParams(emptyFilters));
  }

  function changePage(page) {
    setSearchParams(toSearchParams({ ...appliedFilters, page: Math.max(1, page) }));
  }

  function changePageSize(size) {
    setSearchParams(toSearchParams({ ...appliedFilters, page: 1, size: Number(size || 20) }));
  }

  function openDetail(review) {
    syncSelected(review);
  }

  function syncSelected(review) {
    setSelected(review);
    setReplyDraft(review.adminReply || "");
    setModerationDraft({
      approved: Boolean(review.approved),
      visible: review.visible !== false,
    });
  }

  function applyUpdatedReview(updated) {
    setReviews((current) => current.map((review) => (review.id === updated.id ? updated : review)));
    setSelected((current) => (current?.id === updated.id ? updated : current));
    setReplyDraft(updated.adminReply || "");
    setModerationDraft({
      approved: Boolean(updated.approved),
      visible: updated.visible !== false,
    });
  }

  async function runModeration(review, approved, visible, successKey) {
    if (!review || review.deletedAt) return;
    setBusy(`moderate-${review.id}`);
    setMessage("");
    try {
      const updated = normalizeReview(await moderateReview(review.id, { approved, visible }));
      applyUpdatedReview(updated);
      setMessage(t(successKey));
      toast({ message: t(successKey), variant: "success" });
      await refreshReviews(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.reviewModerationFailed"));
    } finally {
      setBusy("");
    }
  }

  async function saveModeration(event) {
    event.preventDefault();
    if (!selected) return;
    await runModeration(selected, Boolean(moderationDraft.approved), Boolean(moderationDraft.visible), "admin.reviewModerated");
    setModerationOpen(false);
  }

  async function saveReply(event) {
    event.preventDefault();
    if (!selected || selected.deletedAt) return;
    setBusy(`reply-${selected.id}`);
    setMessage("");
    try {
      const updated = normalizeReview(await replyReview(selected.id, { adminReply: replyDraft.trim() }));
      applyUpdatedReview(updated);
      setMessage(replyDraft.trim() ? t("admin.reviewReplySaved") : t("admin.reviewReplyCleared"));
      toast({ message: replyDraft.trim() ? t("admin.reviewReplySaved") : t("admin.reviewReplyCleared"), variant: "success" });
      await refreshReviews(appliedFilters);
      setReplyOpen(false);
    } catch (error) {
      setMessage(error.message || t("admin.reviewReplyFailed"));
    } finally {
      setBusy("");
    }
  }

  async function clearReply() {
    if (!selected || selected.deletedAt) return;
    if (selected.adminReply) {
      const confirmed = await confirm({
        title: t("admin.clearReply"),
        message: t("admin.confirmClearReviewReply"),
        confirmLabel: t("admin.clearReply"),
        cancelLabel: t("common.cancel"),
        danger: true,
      });
      if (!confirmed) return;
    }
    setReplyDraft("");
    setBusy(`reply-${selected.id}`);
    setMessage("");
    try {
      const updated = normalizeReview(await replyReview(selected.id, { adminReply: "" }));
      applyUpdatedReview(updated);
      setMessage(t("admin.reviewReplyCleared"));
      toast({ message: t("admin.reviewReplyCleared"), variant: "success" });
      await refreshReviews(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.reviewReplyFailed"));
    } finally {
      setBusy("");
    }
  }

  return (
    <div className="mx-auto grid w-full max-w-7xl gap-6">
      <PageHeader title={t("admin.reviewsTitle")} eyebrow={t("admin.reviewsEyebrow")} />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.reviewFilters")}>
        <form className="grid gap-3 xl:grid-cols-[150px_150px_120px_1fr_130px_170px_100px_auto_auto]" onSubmit={applyFilters}>
          <Select value={filters.approved} onChange={(event) => setFilters({ ...filters, approved: event.target.value })}>
            <option value="">{t("admin.allApprovalStates")}</option>
            <option value="true">{t("admin.approved")}</option>
            <option value="false">{t("admin.unapproved")}</option>
          </Select>
          <Select value={filters.visible} onChange={(event) => setFilters({ ...filters, visible: event.target.value })}>
            <option value="">{t("admin.allVisibilityStates")}</option>
            <option value="true">{t("common.visible")}</option>
            <option value="false">{t("common.hidden")}</option>
          </Select>
          <Select value={filters.rating} onChange={(event) => setFilters({ ...filters, rating: event.target.value })}>
            <option value="">{t("admin.allRatings")}</option>
            {RATINGS.map((rating) => <option key={rating} value={rating}>{t("admin.ratingValue", { rating })}</option>)}
          </Select>
          <Input value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} placeholder={t("admin.reviewKeyword")} />
          <Input inputMode="numeric" value={filters.productId} onChange={(event) => setFilters({ ...filters, productId: event.target.value })} placeholder={t("admin.productId")} />
          <Input value={filters.userId} onChange={(event) => setFilters({ ...filters, userId: event.target.value })} placeholder={t("admin.userId")} />
          <Select value={filters.size} onChange={(event) => changePageSize(event.target.value)} aria-label={t("catalog.pageSize")}>
            {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
          </Select>
          <Button type="submit">{t("admin.applyFilters")}</Button>
          <Button variant="secondary" type="button" onClick={clearFilters}>{t("admin.clearFilters")}</Button>
        </form>
      </Panel>

      <Panel title={t("admin.reviewsList")}>
        <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
          <table className="min-w-[1180px] w-full border-collapse text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500 dark:bg-slate-800/50 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3">{t("admin.review")}</th>
                <th className="px-4 py-3">{t("admin.book")}</th>
                <th className="px-4 py-3">{t("admin.user")}</th>
                <th className="px-4 py-3">{t("admin.order")}</th>
                <th className="px-4 py-3">{t("common.status")}</th>
                <th className="px-4 py-3">{t("admin.reply")}</th>
                <th className="px-4 py-3">{t("orders.createdAt")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {reviews.map((review) => (
                <tr className="border-t border-slate-100 align-middle transition-colors hover:bg-slate-50/60 dark:border-slate-800 dark:hover:bg-slate-800/30" key={review.id}>
                  <td className="px-4 py-3">
                    <div className="flex items-start gap-3">
                      <ReviewThumb review={review} onPreview={setImagePreview} />
                      <div className="min-w-0">
                        <RatingStars size="sm" value={review.rating} />
                        <p className="mt-1 max-w-xs truncate font-semibold text-slate-700 dark:text-slate-300">{review.comment || "-"}</p>
                        <p className="text-xs text-slate-400">#{review.id}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-bold text-slate-950 dark:text-slate-100">{review.productName || "-"}</p>
                    <p className="text-xs text-slate-500">ID {review.productId || "-"} / {review.sku || "-"}</p>
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-semibold text-slate-700 dark:text-slate-300">{review.username || "-"}</p>
                    <p className="text-xs text-slate-500">{review.userId || "-"}</p>
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                    <p>{t("admin.order")} #{review.orderId || "-"}</p>
                    <p className="text-xs">{t("admin.orderItemId")} #{review.orderItemId || "-"}</p>
                  </td>
                  <td className="px-4 py-3"><ReviewBadges review={review} t={t} /></td>
                  <td className="px-4 py-3">{review.adminReply ? <Badge active>{t("admin.replied")}</Badge> : <span className="text-slate-400">-</span>}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(review.createdAt, i18n.language)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" variant="secondary" onClick={() => openDetail(review)}>{t("common.detail")}</Button>
                      <ReviewQuickActions busy={busy} onModerate={runModeration} review={review} t={t} />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
          {!loading && !reviews.length && <div className="p-5 text-sm text-slate-500">{t("admin.noReviews")}</div>}
        </div>
        <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t} />
      </Panel>

      {selected && (
        <ReviewDetailDrawer
          busy={busy}
          language={i18n.language}
          onClearReply={clearReply}
          onClose={() => setSelected(null)}
          onImagePreview={setImagePreview}
          onOpenModeration={() => setModerationOpen(true)}
          onOpenReply={() => setReplyOpen(true)}
          onQuickModerate={runModeration}
          review={selected}
          t={t}
        />
      )}
      {selected && (
        <>
          <ReviewModerationModal
            busy={busy}
            draft={moderationDraft}
            onChange={setModerationDraft}
            onClose={() => setModerationOpen(false)}
            onSubmit={saveModeration}
            open={moderationOpen}
            review={selected}
            t={t}
          />
          <ReviewReplyModal
            busy={busy}
            draft={replyDraft}
            language={i18n.language}
            onChange={setReplyDraft}
            onClearReply={clearReply}
            onClose={() => setReplyOpen(false)}
            onSubmit={saveReply}
            open={replyOpen}
            review={selected}
            t={t}
          />
        </>
      )}
      {imagePreview && <ImagePreview image={imagePreview} onClose={() => setImagePreview(null)} t={t} />}
    </div>
  );
}

function ReviewDetailDrawer({
  busy,
  language,
  onClearReply,
  onClose,
  onImagePreview,
  onOpenModeration,
  onOpenReply,
  onQuickModerate,
  review,
  t,
}) {
  const deleted = Boolean(review.deletedAt);

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm">
      <aside className="h-full w-full max-w-5xl overflow-y-auto bg-white shadow-2xl dark:bg-slate-900">
        <div className="p-5 md:p-8">
        <div className="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 md:flex-row md:items-start md:justify-between">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">{t("admin.reviewDetail")}</span>
            <h2 className="mt-2 text-2xl font-bold text-slate-950 dark:text-slate-50">{review.productName || t("admin.review")}</h2>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <RatingStars size="sm" value={review.rating} />
              <ReviewBadges review={review} t={t} />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <ReviewQuickActions busy={busy} onModerate={onQuickModerate} review={review} t={t} />
            <Button variant="secondary" type="button" onClick={onClose}>{t("common.close")}</Button>
          </div>
        </div>

        {deleted && <Notice>{t("admin.deletedReviewNotice")}</Notice>}

        <div className="grid gap-5 xl:grid-cols-3">
          <InfoCard title={t("admin.reviewContent")}>
            <Meta label="ID" value={review.id || "-"} />
            <Meta label={t("admin.rating")} value={review.rating || "-"} />
            <div className="rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-700 dark:bg-slate-800 dark:text-slate-300">{review.comment || "-"}</div>
          </InfoCard>
          <InfoCard title={t("admin.book")}>
            <Meta label={t("admin.bookTitle")} value={review.productName || "-"} />
            <Meta label={t("admin.productId")} value={review.productId || "-"} />
            <Meta label={t("admin.productVariationId")} value={review.productVariationId || "-"} />
            <Meta label={t("admin.sku")} value={review.sku || "-"} />
          </InfoCard>
          <InfoCard title={t("admin.user")}>
            <Meta label={t("auth.username")} value={review.username || "-"} />
            <Meta label={t("admin.userId")} value={review.userId || "-"} />
            <Meta label={t("admin.order")} value={review.orderId || "-"} />
            <Meta label={t("admin.orderItemId")} value={review.orderItemId || "-"} />
          </InfoCard>
        </div>

        <div className="mt-5 grid gap-5 xl:grid-cols-2">
          <InfoCard title={t("admin.reviewImages")}>
            <ImageGrid images={review.images} onPreview={onImagePreview} t={t} />
          </InfoCard>
          <InfoCard title={t("admin.moderationMetadata")}>
            <Meta label={t("admin.approved")} value={yesNo(review.approved, t)} />
            <Meta label={t("common.visible")} value={yesNo(review.visible, t)} />
            <Meta label={t("admin.deleted")} value={review.deletedAt ? formatDateTime(review.deletedAt, language) : t("common.no")} />
            <Meta label={t("admin.moderatedBy")} value={review.moderatedBy || "-"} />
            <Meta label={t("admin.moderatedAt")} value={formatDateTime(review.moderatedAt, language)} />
            <Meta label={t("orders.createdAt")} value={formatDateTime(review.createdAt, language)} />
            <Meta label={t("orders.updatedAt")} value={formatDateTime(review.updatedAt, language)} />
          </InfoCard>
        </div>

        <div className="mt-5 grid gap-5 xl:grid-cols-2">
          <InfoCard title={t("admin.moderationActions")}>
            <div className="grid gap-3">
              <Meta label={t("admin.approved")} value={yesNo(review.approved, t)} />
              <Meta label={t("common.visible")} value={yesNo(review.visible, t)} />
              <Button disabled={deleted || Boolean(busy)} type="button" onClick={onOpenModeration}>{t("admin.saveModeration")}</Button>
            </div>
          </InfoCard>
          <InfoCard title={t("admin.adminReply")}>
            <div className="grid gap-4">
              <div className="rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-700 dark:bg-slate-800 dark:text-slate-300">{review.adminReply || "-"}</div>
              <div className="flex flex-wrap gap-2">
                <Button disabled={deleted || Boolean(busy)} type="button" onClick={onOpenReply}>{t("admin.saveReply")}</Button>
                <Button variant="secondary" disabled={deleted || Boolean(busy)} type="button" onClick={onClearReply}>{t("admin.clearReply")}</Button>
              </div>
              <Meta label={t("admin.repliedBy")} value={review.repliedBy || "-"} />
              <Meta label={t("admin.repliedAt")} value={formatDateTime(review.repliedAt, language)} />
            </div>
          </InfoCard>
        </div>
        </div>
      </aside>
    </div>
  );
}

function ReviewModerationModal({ busy, draft, onChange, onClose, onSubmit, open, review, t }) {
  const deleted = Boolean(review.deletedAt);
  return (
    <Modal onClose={onClose} open={open} title={t("admin.moderationActions")}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <label className="flex items-center gap-3 rounded-xl border border-slate-200 p-4 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800">
          <input
            checked={Boolean(draft.approved)}
            disabled={deleted}
            type="checkbox"
            onChange={(event) => onChange({ ...draft, approved: event.target.checked })}
          />
          {t("admin.approved")}
        </label>
        <label className="flex items-center gap-3 rounded-xl border border-slate-200 p-4 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800">
          <input
            checked={Boolean(draft.visible)}
            disabled={deleted}
            type="checkbox"
            onChange={(event) => onChange({ ...draft, visible: event.target.checked })}
          />
          {t("common.visible")}
        </label>
        <div className="flex flex-wrap gap-2">
          <Button disabled={deleted || Boolean(busy)} type="submit">{t("admin.saveModeration")}</Button>
          <Button variant="secondary" type="button" onClick={onClose}>{t("common.cancel")}</Button>
        </div>
      </form>
    </Modal>
  );
}

function ReviewReplyModal({ busy, draft, language, onChange, onClearReply, onClose, onSubmit, open, review, t }) {
  const deleted = Boolean(review.deletedAt);
  return (
    <Modal onClose={onClose} open={open} size="lg" title={t("admin.adminReply")}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <Textarea
          disabled={deleted}
          maxLength={2000}
          value={draft}
          onChange={(event) => onChange(event.target.value)}
          placeholder={t("admin.reviewReplyPlaceholder")}
        />
        <div className="flex flex-wrap justify-between gap-3 text-xs font-semibold text-slate-500">
          <span>{t("admin.adminReplyNote")}</span>
          <span>{draft.length}/2000</span>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button disabled={deleted || Boolean(busy)} type="submit">{t("admin.saveReply")}</Button>
          <Button variant="secondary" disabled={deleted || Boolean(busy)} type="button" onClick={onClearReply}>{t("admin.clearReply")}</Button>
          <Button variant="secondary" type="button" onClick={onClose}>{t("common.cancel")}</Button>
        </div>
        <Meta label={t("admin.repliedBy")} value={review.repliedBy || "-"} />
        <Meta label={t("admin.repliedAt")} value={formatDateTime(review.repliedAt, language)} />
      </form>
    </Modal>
  );
}

function ReviewQuickActions({ busy, onModerate, review, t }) {
  if (review.deletedAt) return null;
  const disabled = Boolean(busy);
  return (
    <>
      {(!review.approved || review.visible === false) && (
        <Button size="sm" variant="secondary" disabled={disabled} onClick={() => onModerate(review, true, true, "admin.reviewApproved")}>
          {t("admin.approveShow")}
        </Button>
      )}
      {review.approved && (
        <Button size="sm" variant="secondary" disabled={disabled} onClick={() => onModerate(review, false, false, "admin.reviewUnapproved")}>
          {t("admin.unapprove")}
        </Button>
      )}
      {review.visible === false ? (
        <Button size="sm" variant="secondary" disabled={disabled} onClick={() => onModerate(review, Boolean(review.approved), true, "admin.reviewShown")}>
          {t("admin.showReview")}
        </Button>
      ) : (
        <Button size="sm" variant="secondary" disabled={disabled} onClick={() => onModerate(review, Boolean(review.approved), false, "admin.reviewHidden")}>
          {t("admin.hideReview")}
        </Button>
      )}
    </>
  );
}

function ReviewBadges({ review, t }) {
  return (
    <div className="flex flex-wrap gap-1">
      <Badge active={review.approved}>{review.approved ? t("admin.approved") : t("admin.unapproved")}</Badge>
      <Badge active={review.visible !== false}>{review.visible !== false ? t("common.visible") : t("common.hidden")}</Badge>
      {review.deletedAt && <Badge>{t("admin.deleted")}</Badge>}
    </div>
  );
}

function ReviewThumb({ onPreview, review }) {
  const image = firstImage(review.images);
  if (!image) {
    return <span className="grid h-14 w-14 place-items-center rounded-xl bg-slate-100 text-xs font-bold text-slate-400 dark:bg-slate-800">{review.images?.length || 0}</span>;
  }
  return (
    <button className="h-14 w-14 overflow-hidden rounded-xl ring-1 ring-slate-200 transition hover:ring-2 hover:ring-indigo-500 dark:ring-slate-700" type="button" onClick={() => onPreview(image)}>
      <img className="h-full w-full object-cover" src={image.imageUrl} alt={image.imagePublicId || "review"} />
    </button>
  );
}

function ImageGrid({ images = [], onPreview, t }) {
  if (!images.length) return <p className="text-sm text-slate-500">{t("admin.noReviewImages")}</p>;
  return (
    <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
      {images.map((image, index) => (
        <button className="aspect-square overflow-hidden rounded-xl ring-1 ring-slate-200 transition hover:ring-2 hover:ring-indigo-500 dark:ring-slate-700" key={image.id || image.imagePublicId || index} type="button" onClick={() => onPreview(image)}>
          <img className="h-full w-full object-cover" src={image.imageUrl} alt={image.imagePublicId || t("admin.reviewImageAlt", { index: index + 1 })} />
        </button>
      ))}
    </div>
  );
}

function ImagePreview({ image, onClose, t }) {
  return (
    <div className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/80 p-4 backdrop-blur-sm" role="presentation" onClick={onClose}>
      <section className="max-h-full w-full max-w-4xl" role="presentation" onClick={(event) => event.stopPropagation()}>
        <div className="mb-3 flex justify-end">
          <button className="rounded-full bg-white px-4 py-2 text-sm font-bold text-slate-700" type="button" onClick={onClose}>{t("common.close")}</button>
        </div>
        <img className="mx-auto max-h-[80vh] rounded-2xl object-contain shadow-2xl" src={image.imageUrl} alt={image.imagePublicId || t("admin.reviewImage")} />
      </section>
    </div>
  );
}

function filtersFromSearch(searchParams) {
  return {
    approved: readEnum(searchParams.get("approved"), ["true", "false"]),
    visible: readEnum(searchParams.get("visible"), ["true", "false"]),
    rating: readEnum(searchParams.get("rating"), ["1", "2", "3", "4", "5"]),
    keyword: searchParams.get("keyword") || "",
    productId: searchParams.get("productId") || "",
    userId: searchParams.get("userId") || "",
    page: positiveNumber(searchParams.get("page"), 1),
    size: PAGE_SIZES.includes(Number(searchParams.get("size"))) ? Number(searchParams.get("size")) : 20,
  };
}

function toSearchParams(filters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    if (key === "page" && Number(value) === 1) return;
    if (key === "size" && Number(value) === 20) return;
    params.set(key, String(value));
  });
  return params;
}

function toQuery(filters) {
  return {
    approved: parseBoolean(filters.approved),
    visible: parseBoolean(filters.visible),
    rating: optionalNumber(filters.rating),
    keyword: filters.keyword || undefined,
    productId: optionalNumber(filters.productId),
    userId: filters.userId || undefined,
    page: Number(filters.page || 1),
    size: Number(filters.size || 20),
  };
}

function validateFilters(filters, t) {
  if (filters.productId && !isPositiveInteger(filters.productId)) return t("admin.invalidProductId");
  if (filters.rating && !["1", "2", "3", "4", "5"].includes(String(filters.rating))) return t("admin.invalidRating");
  if (!isPositiveInteger(filters.page || 1)) return t("admin.invalidPage");
  if (!PAGE_SIZES.includes(Number(filters.size || 20))) return t("admin.invalidPageSize");
  return "";
}

function readEnum(value, allowed) {
  return allowed.includes(String(value)) ? String(value) : "";
}

function parseBoolean(value) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

function optionalNumber(value) {
  if (value === undefined || value === null || value === "") return undefined;
  return Number(value);
}

function positiveNumber(value, fallback) {
  return isPositiveInteger(value) ? Number(value) : fallback;
}

function isPositiveInteger(value) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) return false;
  return Number(text) > 0;
}

function firstImage(images = []) {
  return images.find((image) => image?.imageUrl);
}

function yesNo(value, t) {
  return value ? t("common.yes") : t("common.no");
}

function createEmptyMeta(filters) {
  return readPageMeta([], { page: filters.page || 1, size: filters.size || 20, totalPages: 0 });
}

function Badge({ active = false, children }) {
  return <span className={["inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold", active ? "border-emerald-200/60 bg-emerald-50 text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400" : "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400"].join(" ")}>{children}</span>;
}
