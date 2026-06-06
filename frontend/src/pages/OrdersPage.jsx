import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import { cancelOrder, getOrder, getOrders } from "../api/orderApi.js";
import { retryPayment } from "../api/paymentApi.js";
import { createOrderItemReview } from "../api/reviewApi.js";
import ReviewForm from "../components/reviews/ReviewForm.jsx";
import { formatDateTime, formatVND } from "../utils/formatters.js";
import { normalizeOrder, normalizePaymentGroup, pageRows } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

const ORDER_STATUSES = [
  "PENDING_CONFIRMATION",
  "PENDING_PAYMENT",
  "PAID",
  "CONFIRMED",
  "PACKING",
  "SHIPPING",
  "COMPLETED",
  "CANCELLED",
  "PAYMENT_FAILED",
  "EXPIRED",
  "REFUNDED",
];
const PAGE_SIZE_OPTIONS = [10, 20, 50];
const CANCELABLE_STATUSES = new Set(["PENDING_CONFIRMATION", "PENDING_PAYMENT"]);
const RETRY_PAYMENT_STATUSES = new Set(["FAILED", "CANCELLED", "EXPIRED"]);
const ONLINE_METHODS = new Set(["VNPAY", "MOMO"]);

export default function OrdersPage({ onAuth }) {
  const { t, i18n } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [orders, setOrders] = useState([]);
  const [pageMeta, setPageMeta] = useState(emptyMeta());
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [reviewTarget, setReviewTarget] = useState(null);
  const [reviewBusy, setReviewBusy] = useState(false);
  const [reviewedItems, setReviewedItems] = useState([]);
  const [paymentAction, setPaymentAction] = useState(null);
  const [message, setMessage] = useState("");

  const filters = useMemo(() => readFilters(searchParams), [searchParams]);
  const loggedIn = Boolean(getAccessToken());

  useEffect(() => {
    if (!loggedIn) return;

    setLoading(true);
    setMessage("");
    getOrders({
      status: filters.status || undefined,
      page: filters.page,
      size: filters.size,
    })
      .then((page) => {
        setOrders(pageRows(page).map(normalizeOrder));
        setPageMeta({
          currentPage: Number(page?.currentPage || filters.page),
          totalPages: Number(page?.totalPages || 0),
          pageSize: Number(page?.pageSize || filters.size),
          totalElements: Number(page?.totalElements || 0),
          hasNext: Boolean(page?.hasNext),
          hasPrevious: Boolean(page?.hasPrevious),
        });
      })
      .catch((error) => setMessage(error.message || t("orders.loadFailed")))
      .finally(() => setLoading(false));
  }, [filters.page, filters.size, filters.status, loggedIn, t]);

  function updateFilters(overrides) {
    const next = { ...filters, ...overrides };
    const params = new URLSearchParams();
    if (next.status) params.set("status", next.status);
    if (Number(next.page) > 1) params.set("page", String(next.page));
    if (Number(next.size) !== 20) params.set("size", String(next.size));
    setSearchParams(params, { replace: false });
  }

  async function viewDetail(order) {
    setMessage("");
    setDetailLoading(true);
    try {
      setSelected(normalizeOrder(await getOrder(order.id)));
    } catch (error) {
      setMessage(error.message || t("orders.detailFailed"));
    } finally {
      setDetailLoading(false);
    }
  }

  async function confirmCancel(event) {
    event.preventDefault();
    if (!cancelTarget) return;
    setMessage("");
    try {
      const updated = normalizeOrder(await cancelOrder(cancelTarget.id, cancelReason || t("orders.cancelReason")));
      setOrders((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      if (selected?.id === updated.id) setSelected(updated);
      setCancelTarget(null);
      setCancelReason("");
      setMessage(t("orders.cancelled"));
    } catch (error) {
      setMessage(error.message || t("orders.cancelFailed"));
    }
  }

  async function retry(order) {
    setMessage("");
    setPaymentAction(null);
    try {
      const response = normalizePaymentGroup(await retryPayment(order.paymentGroupCode));
      const url = response.paymentUrl || response.deeplink;
      if (url) {
        setPaymentAction({ url, qrCodeUrl: "", message: t("orders.retryRedirecting") });
        window.setTimeout(() => window.location.assign(url), 900);
      } else if (response.qrCodeUrl) {
        setPaymentAction({ url: "", qrCodeUrl: response.qrCodeUrl, message: t("orders.retryQrReady") });
      } else {
        setPaymentAction({ url: "", qrCodeUrl: "", message: t("orders.retryPending") });
      }
    } catch (error) {
      setMessage(error.message || t("orders.retryFailed"));
    }
  }

  async function submitReview(body) {
    if (!reviewTarget) return;
    setReviewBusy(true);
    setMessage("");
    try {
      await createOrderItemReview(reviewTarget.orderId, reviewTarget.item.id, body);
      setReviewedItems((current) => [...new Set([...current, reviewTarget.item.id])]);
      setReviewTarget(null);
      setMessage(t("orders.reviewSubmitted"));
    } catch (error) {
      setMessage(error.message || t("orders.reviewFailed"));
    } finally {
      setReviewBusy(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("orders.title")} eyebrow={t("orders.eyebrow")} />
      {message && <Notice>{message}</Notice>}
      {!loggedIn && (
        <Notice>
          {t("orders.loginRequired")}{" "}
          <button type="button" className="font-bold text-blue-700 underline" onClick={onAuth}>
            {t("common.login")}
          </button>
        </Notice>
      )}

      {loggedIn && (
        <>
          <OrderFilters filters={filters} loading={loading} onChange={updateFilters} t={t} />
          {loading ? (
            <OrderSkeleton />
          ) : orders.length === 0 ? (
            <EmptyState title={t("orders.empty")} />
          ) : (
            <div className="grid gap-4">
              {orders.map((order) => (
                <OrderCard
                  key={order.id}
                  order={order}
                  language={i18n.language}
                  onCancel={() => {
                    setCancelTarget(order);
                    setCancelReason("");
                  }}
                  onDetail={() => viewDetail(order)}
                  onRetry={() => retry(order)}
                  t={t}
                />
              ))}
            </div>
          )}
          <Pagination meta={pageMeta} loading={loading} onPage={(page) => updateFilters({ page })} t={t} />
        </>
      )}

      {detailLoading && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm">
          <div className="rounded-3xl bg-white p-8 text-sm font-bold text-slate-700 shadow-2xl">
            {t("common.loading")}
          </div>
        </div>
      )}

      {selected && (
        <OrderDetailModal
          order={selected}
          language={i18n.language}
          onClose={() => {
            setSelected(null);
            setPaymentAction(null);
          }}
          onCancel={() => {
            setCancelTarget(selected);
            setCancelReason("");
          }}
          onRetry={() => retry(selected)}
          onReview={(item) => setReviewTarget({ orderId: selected.id, item })}
          paymentAction={paymentAction}
          reviewedItems={reviewedItems}
          t={t}
        />
      )}

      {cancelTarget && (
        <CancelOrderModal
          order={cancelTarget}
          reason={cancelReason}
          onReason={setCancelReason}
          onCancel={() => setCancelTarget(null)}
          onSubmit={confirmCancel}
          t={t}
        />
      )}

      {reviewTarget && (
        <div className="fixed inset-0 z-[60] grid place-items-center bg-slate-950/70 px-4 backdrop-blur-sm">
          <div className="w-full max-w-xl rounded-3xl bg-white p-6 shadow-2xl">
            <ReviewForm
              title={t("orders.reviewTitle", { book: reviewTarget.item.productName || reviewTarget.item.title })}
              busy={reviewBusy}
              onCancel={() => setReviewTarget(null)}
              onSubmit={submitReview}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function OrderFilters({ filters, loading, onChange, t }) {
  return (
    <div className="mb-6 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-4 md:grid-cols-[1fr_180px]">
        <label className="grid gap-2 text-sm font-bold text-slate-600">
          {t("orders.filterStatus")}
          <select
            value={filters.status}
            disabled={loading}
            onChange={(event) => onChange({ status: event.target.value, page: 1 })}
            className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          >
            <option value="">{t("common.all")}</option>
            {ORDER_STATUSES.map((status) => (
              <option key={status} value={status}>
                {statusLabel(status, t)}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-2 text-sm font-bold text-slate-600">
          {t("catalog.pageSize")}
          <select
            value={filters.size}
            disabled={loading}
            onChange={(event) => onChange({ size: Number(event.target.value), page: 1 })}
            className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {t("catalog.perPage", { count: size })}
              </option>
            ))}
          </select>
        </label>
      </div>
    </div>
  );
}

function OrderCard({ order, language, onCancel, onDetail, onRetry, t }) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-4 lg:grid-cols-[1fr_auto] lg:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="font-serif text-2xl font-bold text-slate-950">{order.orderCode || order.id}</h2>
            <StatusBadge status={order.orderStatus} t={t} />
            <PaymentBadge status={order.paymentStatus} />
          </div>
          <div className="mt-4 grid gap-2 text-sm text-slate-600 md:grid-cols-2 lg:grid-cols-4">
            <Meta label={t("orders.paymentMethod")} value={order.paymentMethod || "-"} />
            <Meta label={t("orders.items")} value={order.itemCount || 0} />
            <Meta label={t("orders.createdAt")} value={formatDateTime(order.createdAt, language)} />
            <Meta label={t("common.total")} value={formatVND(order.totalAmount)} />
          </div>
        </div>
        <div className="flex flex-wrap gap-2 lg:justify-end">
          <SmallButton onClick={onDetail}>{t("orders.detail")}</SmallButton>
          {canRetry(order) && <SmallButton onClick={onRetry}>{t("orders.retryPayment")}</SmallButton>}
          {canCancel(order) && (
            <SmallButton danger onClick={onCancel}>
              {t("common.cancel")}
            </SmallButton>
          )}
        </div>
      </div>
    </div>
  );
}

function OrderDetailModal({ order, language, onClose, onCancel, onRetry, onReview, paymentAction, reviewedItems, t }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm">
      <div className="relative max-h-[88vh] w-full max-w-4xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl">
        <button
          className="absolute right-4 top-4 rounded-full bg-slate-100 px-3 py-1 text-sm font-bold text-slate-600"
          type="button"
          onClick={onClose}
        >
          x
        </button>
        <div className="pr-10">
          <h2 className="font-serif text-3xl font-bold text-slate-950">{order.orderCode || order.id}</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            <StatusBadge status={order.orderStatus} t={t} />
            <PaymentBadge status={order.paymentStatus} />
          </div>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <InfoPanel title={t("orders.payment")}>
            <Meta label={t("orders.paymentMethod")} value={order.paymentMethod || "-"} />
            <Meta label={t("orders.paymentStatus")} value={order.paymentStatus || "-"} />
            <Meta label={t("orders.paymentGroup")} value={order.paymentGroupCode || "-"} />
            <Meta label={t("orders.paidAt")} value={formatDateTime(order.paidAt, language)} />
          </InfoPanel>
          <InfoPanel title={t("orders.shipping")}>
            <Meta label={t("checkout.recipientName")} value={order.shippingRecipientName || "-"} />
            <Meta label={t("checkout.phoneNumber")} value={order.shippingPhoneNumber || "-"} />
            <Meta label={t("checkout.addressLine")} value={shippingAddress(order) || "-"} />
          </InfoPanel>
        </div>

        <div className="mt-6 grid gap-3">
          {(order.items || []).map((item) => {
            const reviewed = reviewedItems.includes(item.id);
            return (
              <div className="rounded-2xl bg-slate-50 p-4" key={item.id || item.productId || item.productName}>
                <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                  <div className="min-w-0">
                    <span className="font-semibold text-slate-800">
                      {item.productName || item.title} x {item.quantity}
                    </span>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold text-slate-500">
                      {item.sku && <span className="rounded-full bg-white px-2 py-1">{item.sku}</span>}
                      {item.variationSize && <span className="rounded-full bg-white px-2 py-1">{item.variationSize}</span>}
                      {item.variationColor && <span className="rounded-full bg-white px-2 py-1">{item.variationColor}</span>}
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-3 md:justify-end">
                    <strong className="text-slate-950">{formatVND(item.lineTotal)}</strong>
                    {Number(item.discountAmount || 0) > 0 && (
                      <span className="text-xs font-bold text-emerald-600">
                        -{formatVND(item.discountAmount)}
                      </span>
                    )}
                    {order.orderStatus === "COMPLETED" && (
                      <SmallButton disabled={reviewed} onClick={() => onReview(item)}>
                        {reviewed ? t("orders.reviewSubmittedShort") : t("orders.writeReview")}
                      </SmallButton>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-[1fr_320px]">
          <div className="grid gap-3">
            {order.refund && (
              <InfoPanel title={t("orders.refund")}>
                <Meta label={t("orders.refundCode")} value={order.refund.refundCode || "-"} />
                <Meta label={t("common.amount")} value={formatVND(order.refund.amount)} />
                <Meta label={t("orders.refundStatus")} value={order.refund.status || "-"} />
                <Meta label={t("orders.refundedAt")} value={formatDateTime(order.refund.refundedAt, language)} />
              </InfoPanel>
            )}
            {paymentAction && (
              <div className="rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm font-bold text-blue-700">
                <p>{paymentAction.message}</p>
                {paymentAction.url && (
                  <a className="mt-3 inline-flex rounded-full bg-blue-600 px-4 py-2 text-white" href={paymentAction.url}>
                    {t("checkout.continuePayment")}
                  </a>
                )}
                {paymentAction.qrCodeUrl && (
                  <img className="mx-auto mt-3 max-h-56 rounded-xl" src={paymentAction.qrCodeUrl} alt={t("checkout.scanQr")} />
                )}
              </div>
            )}
          </div>

          <InfoPanel title={t("checkout.summary")}>
            <Money label={t("checkout.subtotal")} value={order.subtotal} />
            <Money label={t("orders.discount")} value={-Number(order.discountAmount || 0)} discount />
            <Money label={t("checkout.shippingFee")} value={order.shippingFee} />
            <Money label={t("checkout.finalTotal")} value={order.totalAmount} strong />
            <Meta label={t("orders.createdAt")} value={formatDateTime(order.createdAt, language)} />
            <Meta label={t("orders.updatedAt")} value={formatDateTime(order.updatedAt, language)} />
          </InfoPanel>
        </div>

        <div className="mt-6 flex flex-wrap justify-end gap-2">
          {canRetry(order) && <SmallButton onClick={onRetry}>{t("orders.retryPayment")}</SmallButton>}
          {canCancel(order) && (
            <SmallButton danger onClick={onCancel}>
              {t("common.cancel")}
            </SmallButton>
          )}
          <SmallButton onClick={onClose}>{t("common.close")}</SmallButton>
        </div>
      </div>
    </div>
  );
}

function CancelOrderModal({ order, reason, onReason, onCancel, onSubmit, t }) {
  return (
    <div className="fixed inset-0 z-[60] grid place-items-center bg-slate-950/70 px-4 backdrop-blur-sm">
      <form className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl" onSubmit={onSubmit}>
        <h2 className="font-serif text-3xl font-bold text-slate-950">{t("orders.cancelOrder")}</h2>
        <p className="mt-2 text-sm font-semibold text-slate-500">{order.orderCode || order.id}</p>
        <textarea
          value={reason}
          onChange={(event) => onReason(event.target.value)}
          placeholder={t("orders.cancelReasonPlaceholder")}
          className="mt-5 min-h-28 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
        />
        <div className="mt-5 flex justify-end gap-2">
          <SmallButton onClick={onCancel}>{t("common.close")}</SmallButton>
          <button className="rounded-full bg-red-600 px-5 py-2 text-sm font-bold text-white hover:bg-red-700" type="submit">
            {t("orders.confirmCancel")}
          </button>
        </div>
      </form>
    </div>
  );
}

function Pagination({ meta, loading, onPage, t }) {
  if (!meta.totalPages || meta.totalPages <= 1) return null;

  return (
    <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
      <PageButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(meta.currentPage - 1)}>
        {t("catalog.previousPage")}
      </PageButton>
      <span className="rounded-full bg-white px-4 py-2 text-sm font-bold text-slate-600 shadow-sm">
        {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })}
      </span>
      <PageButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.currentPage + 1)}>
        {t("catalog.nextPage")}
      </PageButton>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="mb-8 border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
        {eyebrow}
      </span>
      <h1 className="mt-3 font-serif text-4xl font-bold text-slate-950 md:text-5xl">{title}</h1>
    </div>
  );
}

function InfoPanel({ title, children }) {
  return (
    <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
      <h3 className="mb-3 font-serif text-xl font-bold text-slate-950">{title}</h3>
      <div className="grid gap-2 text-sm">{children}</div>
    </div>
  );
}

function Meta({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="font-semibold text-slate-500">{label}</span>
      <strong className="truncate text-right text-slate-950">{value ?? "-"}</strong>
    </div>
  );
}

function Money({ label, value, discount = false, strong = false }) {
  const numeric = Number(value || 0);
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="font-semibold text-slate-500">{label}</span>
      <strong className={[discount && numeric < 0 ? "text-emerald-600" : "text-slate-950", strong ? "text-lg" : ""].join(" ")}>
        {formatVND(numeric)}
      </strong>
    </div>
  );
}

function StatusBadge({ status, t }) {
  return (
    <strong className="rounded-full bg-blue-50 px-3 py-1 text-xs uppercase tracking-wider text-blue-700">
      {statusLabel(status, t)}
    </strong>
  );
}

function PaymentBadge({ status }) {
  return (
    <strong className="rounded-full bg-slate-100 px-3 py-1 text-xs uppercase tracking-wider text-slate-700">
      {status || "-"}
    </strong>
  );
}

function SmallButton({ danger = false, className = "", ...props }) {
  return (
    <button
      type="button"
      {...props}
      className={[
        "rounded-full border px-4 py-2 text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-700 hover:bg-slate-50",
        className,
      ].join(" ")}
    />
  );
}

function PageButton(props) {
  return (
    <button
      type="button"
      {...props}
      className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
    />
  );
}

function EmptyState({ title }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h2 className="font-serif text-3xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

function Notice({ children }) {
  return (
    <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
      {children}
    </div>
  );
}

function OrderSkeleton() {
  return (
    <div className="grid gap-4" aria-hidden="true">
      <div className="h-32 animate-pulse rounded-3xl bg-slate-100" />
      <div className="h-32 animate-pulse rounded-3xl bg-slate-100" />
      <div className="h-32 animate-pulse rounded-3xl bg-slate-100" />
    </div>
  );
}

function shippingAddress(order) {
  return [order.shippingAddressLine, order.shippingWard, order.shippingDistrict, order.shippingCity]
    .filter(Boolean)
    .join(", ");
}

function canCancel(order) {
  return CANCELABLE_STATUSES.has(order?.orderStatus);
}

function canRetry(order) {
  return ONLINE_METHODS.has(order?.paymentMethod) && RETRY_PAYMENT_STATUSES.has(order?.paymentStatus) && Boolean(order?.paymentGroupCode);
}

function statusLabel(status, t) {
  return t(`orders.statusLabels.${status}`, { defaultValue: status || "-" });
}

function readFilters(searchParams) {
  return {
    status: searchParams.get("status") || "",
    page: positiveInt(searchParams.get("page"), 1),
    size: positiveInt(searchParams.get("size"), 20),
  };
}

function positiveInt(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function emptyMeta() {
  return {
    currentPage: 1,
    totalPages: 0,
    pageSize: 20,
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  };
}
