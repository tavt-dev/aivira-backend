import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import { cancelOrder, getOrder, getOrders } from "../api/orderApi.js";
import { retryPayment } from "../api/paymentApi.js";
import { createOrderItemReview } from "../api/reviewApi.js";
import ReviewForm from "../components/reviews/ReviewForm.jsx";
import {
  Badge,
  Button,
  Drawer,
  EmptyState,
  InfoCard,
  MetaRow,
  Modal,
  Notice,
  PageHeader,
  Pagination,
  Select,
  Skeleton,
} from "../components/ui/index.jsx";
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
      {message && <Notice className="mb-6">{message}</Notice>}
      {!loggedIn && (
        <Notice className="mb-6">
          {t("orders.loginRequired")}{" "}
          <button type="button" className="font-bold text-blue-700 underline" onClick={onAuth}>
            {t("common.login")}
          </button>
        </Notice>
      )}

      {loggedIn && (
        <>
          {/* Filters */}
          <div className="mb-6 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="grid gap-4 md:grid-cols-[1fr_180px]">
              <Select
                label={t("orders.filterStatus")}
                value={filters.status}
                disabled={loading}
                onChange={(event) => updateFilters({ status: event.target.value, page: 1 })}
              >
                <option value="">{t("common.all")}</option>
                {ORDER_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {statusLabel(status, t)}
                  </option>
                ))}
              </Select>
              <Select
                label={t("catalog.pageSize")}
                value={filters.size}
                disabled={loading}
                onChange={(event) => updateFilters({ size: Number(event.target.value), page: 1 })}
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {t("catalog.perPage", { count: size })}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {loading ? (
            <Skeleton rows={3} />
          ) : orders.length === 0 ? (
            <EmptyState title={t("orders.empty")} />
          ) : (
            <div className="grid gap-4">
              {orders.map((order) => (
                <OrderCard
                  key={order.id}
                  order={order}
                  language={i18n.language}
                  onCancel={() => { setCancelTarget(order); setCancelReason(""); }}
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

      {/* Order Detail Drawer */}
      <Drawer
        open={Boolean(selected) || detailLoading}
        title={selected ? (selected.orderCode || selected.id) : t("common.loading")}
        onClose={() => { setSelected(null); setPaymentAction(null); }}
      >
        {detailLoading && <Skeleton rows={4} />}
        {selected && (
          <OrderDetailContent
            order={selected}
            language={i18n.language}
            onCancel={() => { setCancelTarget(selected); setCancelReason(""); }}
            onRetry={() => retry(selected)}
            onReview={(item) => setReviewTarget({ orderId: selected.id, item })}
            paymentAction={paymentAction}
            reviewedItems={reviewedItems}
            t={t}
          />
        )}
      </Drawer>

      {/* Cancel Order Modal */}
      <Modal
        open={Boolean(cancelTarget)}
        title={t("orders.cancelOrder")}
        onClose={() => setCancelTarget(null)}
      >
        <form onSubmit={confirmCancel} className="grid gap-4">
          <p className="text-sm font-semibold text-slate-500">{cancelTarget?.orderCode || cancelTarget?.id}</p>
          <textarea
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            placeholder={t("orders.cancelReasonPlaceholder")}
            className="min-h-28 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setCancelTarget(null)}>
              {t("common.close")}
            </Button>
            <Button type="submit" variant="danger">
              {t("orders.confirmCancel")}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Write Review Modal */}
      <Modal
        open={Boolean(reviewTarget)}
        title={reviewTarget ? t("orders.reviewTitle", { book: reviewTarget.item.productName || reviewTarget.item.title }) : ""}
        onClose={() => setReviewTarget(null)}
      >
        {reviewTarget && (
          <ReviewForm
            busy={reviewBusy}
            onCancel={() => setReviewTarget(null)}
            onSubmit={submitReview}
          />
        )}
      </Modal>
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
            <Badge variant="info">{statusLabel(order.orderStatus, t)}</Badge>
            <Badge variant="neutral">{order.paymentStatus || "-"}</Badge>
          </div>
          <div className="mt-4 grid gap-2 text-sm text-slate-600 md:grid-cols-2 lg:grid-cols-4">
            <MetaRow label={t("orders.paymentMethod")} value={order.paymentMethod || "-"} />
            <MetaRow label={t("orders.items")} value={order.itemCount || 0} />
            <MetaRow label={t("orders.createdAt")} value={formatDateTime(order.createdAt, language)} />
            <MetaRow label={t("common.total")} value={formatVND(order.totalAmount)} />
          </div>
        </div>
        <div className="flex flex-wrap gap-2 lg:justify-end">
          <Button size="sm" variant="secondary" onClick={onDetail}>{t("orders.detail")}</Button>
          {canRetry(order) && <Button size="sm" variant="secondary" onClick={onRetry}>{t("orders.retryPayment")}</Button>}
          {canCancel(order) && (
            <Button size="sm" variant="danger" onClick={onCancel}>{t("common.cancel")}</Button>
          )}
        </div>
      </div>
    </div>
  );
}

function OrderDetailContent({ order, language, onCancel, onRetry, onReview, paymentAction, reviewedItems, t }) {
  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap gap-2">
        <Badge variant="info">{statusLabel(order.orderStatus, t)}</Badge>
        <Badge variant="neutral">{order.paymentStatus || "-"}</Badge>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <InfoCard title={t("orders.payment")}>
          <MetaRow label={t("orders.paymentMethod")} value={order.paymentMethod || "-"} />
          <MetaRow label={t("orders.paymentStatus")} value={order.paymentStatus || "-"} />
          <MetaRow label={t("orders.paymentGroup")} value={order.paymentGroupCode || "-"} />
          <MetaRow label={t("orders.paidAt")} value={formatDateTime(order.paidAt, language)} />
        </InfoCard>
        <InfoCard title={t("orders.shipping")}>
          <MetaRow label={t("checkout.recipientName")} value={order.shippingRecipientName || "-"} />
          <MetaRow label={t("checkout.phoneNumber")} value={order.shippingPhoneNumber || "-"} />
          <MetaRow label={t("checkout.addressLine")} value={shippingAddress(order) || "-"} />
        </InfoCard>
      </div>

      <div className="grid gap-3">
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
                    <Button size="sm" variant="secondary" disabled={reviewed} onClick={() => onReview(item)}>
                      {reviewed ? t("orders.reviewSubmittedShort") : t("orders.writeReview")}
                    </Button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid gap-4 md:grid-cols-[1fr_320px]">
        <div className="grid gap-3">
          {order.refund && (
            <InfoCard title={t("orders.refund")}>
              <MetaRow label={t("orders.refundCode")} value={order.refund.refundCode || "-"} />
              <MetaRow label={t("common.amount")} value={formatVND(order.refund.amount)} />
              <MetaRow label={t("orders.refundStatus")} value={order.refund.status || "-"} />
              <MetaRow label={t("orders.refundedAt")} value={formatDateTime(order.refund.refundedAt, language)} />
            </InfoCard>
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

        <InfoCard title={t("checkout.summary")}>
          <MetaRow label={t("checkout.subtotal")} value={formatVND(order.subtotal)} />
          <MetaRow label={t("orders.discount")} value={formatVND(-Number(order.discountAmount || 0))} />
          <MetaRow label={t("checkout.shippingFee")} value={formatVND(order.shippingFee)} />
          <MetaRow label={t("checkout.finalTotal")} value={formatVND(order.totalAmount)} strong />
          <MetaRow label={t("orders.createdAt")} value={formatDateTime(order.createdAt, language)} />
          <MetaRow label={t("orders.updatedAt")} value={formatDateTime(order.updatedAt, language)} />
        </InfoCard>
      </div>

      <div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 pt-4">
        {canRetry(order) && (
          <Button size="sm" variant="secondary" onClick={onRetry}>{t("orders.retryPayment")}</Button>
        )}
        {canCancel(order) && (
          <Button size="sm" variant="danger" onClick={onCancel}>{t("common.cancel")}</Button>
        )}
      </div>
    </div>
  );
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

function shippingAddress(order) {
  return [order.shippingAddressLine, order.shippingWard, order.shippingDistrict, order.shippingCity]
    .filter(Boolean)
    .join(", ");
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
