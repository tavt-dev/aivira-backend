import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import {
  cancelAdminOrder,
  confirmOrder,
  getAdminOrder,
  getAdminOrders,
  markCompleted,
  markPacking,
  markRefunded,
  markShipping,
} from "../../api/adminOrdersApi.js";
import {
  Button,
  Drawer,
  InfoCard,
  Input,
  MetaRow as MetaRow,
  Modal,
  Notice,
  PageHeader,
  Pagination,
  Panel,
  Select,
  Textarea,
} from "../../components/ui/index.jsx";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeOrder, pageMeta as readPageMeta, pageRows } from "../../utils/mappers.js";

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
const PAGE_SIZES = [10, 20, 50];
const REFUNDABLE_STATUSES = new Set(["PAID", "CONFIRMED", "PACKING"]);
const TERMINAL_STATUSES = new Set(["COMPLETED", "CANCELLED", "PAYMENT_FAILED", "EXPIRED", "REFUNDED"]);

const emptyFilters = {
  status: "",
  keyword: "",
  fromDate: "",
  toDate: "",
  page: 1,
  size: 20,
};

export default function AdminOrdersPage() {
  const { t, i18n } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialFilters = useMemo(() => filtersFromSearch(searchParams), [searchParams]);
  const [filters, setFilters] = useState(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState(initialFilters);
  const [orders, setOrders] = useState([]);
  const [pageMeta, setPageMeta] = useState(createEmptyMeta(emptyFilters));
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionBusy, setActionBusy] = useState("");
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [refundTarget, setRefundTarget] = useState(null);
  const [refundForm, setRefundForm] = useState({ amount: "", reason: "", note: "" });

  const refreshOrders = useCallback(async (nextFilters = appliedFilters) => {
    setLoading(true);
    setMessage("");
    try {
      const page = await getAdminOrders(toQuery(nextFilters));
      const rows = pageRows(page).map(normalizeOrder);
      setOrders(rows);
      setPageMeta(readPageMeta(page, { page: nextFilters.page, size: nextFilters.size }));
    } catch (error) {
      setOrders([]);
      setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.orderLoadFailed"));
    } finally {
      setLoading(false);
    }
  }, [appliedFilters, t]);

  useEffect(() => {
    refreshOrders(appliedFilters);
  }, [appliedFilters, refreshOrders]);

  useEffect(() => {
    const next = filtersFromSearch(searchParams);
    setFilters(next);
    setAppliedFilters(next);
  }, [searchParams]);

  function applyFilters(event) {
    event.preventDefault();
    const validation = validateDateRange(filters, t);
    if (validation) {
      setMessage(validation);
      return;
    }
    const next = { ...filters, page: 1, size: Number(filters.size || 20) };
    setFilters(next);
    setAppliedFilters(next);
  }

  function clearFilters() {
    setFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
  }

  function changePage(page) {
    const nextPage = Math.max(1, page);
    setFilters((current) => ({ ...current, page: nextPage }));
    setAppliedFilters((current) => ({ ...current, page: nextPage }));
  }

  function changePageSize(size) {
    const next = { ...filters, page: 1, size: Number(size || 20) };
    setFilters(next);
    setAppliedFilters(next);
  }

  async function openDetail(order) {
    setDetailLoading(true);
    setMessage("");
    try {
      setSelected(normalizeOrder(await getAdminOrder(order.id)));
    } catch (error) {
      setMessage(error.message || t("admin.orderDetailFailed"));
    } finally {
      setDetailLoading(false);
    }
  }

  async function runTransition(order, action, successKey) {
    setActionBusy(`${action.name}-${order.id}`);
    setMessage("");
    try {
      const updated = normalizeOrder(await action(order.id));
      applyUpdatedOrder(updated);
      setMessage(t(successKey));
      await refreshOrders(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.orderActionFailed"));
    } finally {
      setActionBusy("");
    }
  }

  async function submitCancel(event) {
    event.preventDefault();
    if (!cancelTarget) return;
    setActionBusy(`cancel-${cancelTarget.id}`);
    setMessage("");
    try {
      const updated = normalizeOrder(await cancelAdminOrder(cancelTarget.id, { reason: cancelReason }));
      applyUpdatedOrder(updated);
      setCancelTarget(null);
      setCancelReason("");
      setMessage(t("admin.adminOrderCancelled"));
      await refreshOrders(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.orderCancelFailed"));
    } finally {
      setActionBusy("");
    }
  }

  async function submitRefund(event) {
    event.preventDefault();
    if (!refundTarget) return;
    if (!refundForm.amount || !refundForm.reason.trim() || !refundForm.note.trim()) {
      setMessage(t("admin.refundValidation"));
      return;
    }
    setActionBusy(`refund-${refundTarget.id}`);
    setMessage("");
    try {
      const updated = normalizeOrder(await markRefunded(refundTarget.id, {
        amount: Number(refundForm.amount),
        reason: refundForm.reason,
        note: refundForm.note,
      }));
      applyUpdatedOrder(updated);
      setRefundTarget(null);
      setRefundForm({ amount: "", reason: "", note: "" });
      setMessage(t("admin.orderRefunded"));
      await refreshOrders(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.orderRefundFailed"));
    } finally {
      setActionBusy("");
    }
  }

  function applyUpdatedOrder(updated) {
    setOrders((current) => current.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
    setSelected((current) => (current?.id === updated.id ? updated : current));
  }

  function openRefund(order) {
    setRefundTarget(order);
    setRefundForm({
      amount: Number(order.totalAmount || 0),
      reason: "",
      note: "",
    });
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.ordersTitle")} eyebrow={t("admin.ordersEyebrow")} />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.orderFilters")}>
        <form className="grid gap-3 xl:grid-cols-[180px_1fr_190px_190px_100px_auto_auto]" onSubmit={applyFilters}>
          <Select value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="">{t("admin.allOrderStatuses")}</option>
            {ORDER_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status, t)}</option>)}
          </Select>
          <Input value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} placeholder={t("admin.orderKeyword")} />
          <Input value={filters.fromDate} onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} type="datetime-local" aria-label={t("admin.fromDate")} />
          <Input value={filters.toDate} onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} type="datetime-local" aria-label={t("admin.toDate")} />
          <Select value={filters.size} onChange={(event) => changePageSize(event.target.value)} aria-label={t("catalog.pageSize")}>
            {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
          </Select>
          <Button type="submit">{t("admin.applyFilters")}</Button>
          <Button variant="secondary" type="button" onClick={clearFilters}>{t("admin.clearFilters")}</Button>
        </form>
      </Panel>

      <Panel title={t("admin.ordersList")}>
        <div className="overflow-x-auto rounded-xl border border-slate-200">
          <table className="min-w-[1080px] w-full border-collapse text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">{t("admin.orderCode")}</th>
                <th className="px-4 py-3">{t("orders.status")}</th>
                <th className="px-4 py-3">{t("orders.payment")}</th>
                <th className="px-4 py-3">{t("orders.paymentGroup")}</th>
                <th className="px-4 py-3">{t("common.total")}</th>
                <th className="px-4 py-3">{t("orders.items")}</th>
                <th className="px-4 py-3">{t("orders.paidAt")}</th>
                <th className="px-4 py-3">{t("orders.createdAt")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr className="border-t border-slate-100 align-middle" key={order.id}>
                  <td className="px-4 py-3">
                    <p className="font-bold text-slate-950">{order.orderCode}</p>
                    <p className="text-xs text-slate-500">#{order.id}</p>
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={order.orderStatus} t={t} /></td>
                  <td className="px-4 py-3">
                    <p className="font-semibold">{order.paymentMethod || "-"}</p>
                    <PaymentBadge status={order.paymentStatus} />
                  </td>
                  <td className="px-4 py-3 text-slate-600">{order.paymentGroupCode || "-"}</td>
                  <td className="px-4 py-3 font-bold">{formatVND(order.totalAmount, i18n.language)}</td>
                  <td className="px-4 py-3">{order.itemCount || 0}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(order.paidAt, i18n.language)}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(order.createdAt, i18n.language)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" variant="secondary" onClick={() => openDetail(order)}>{t("common.detail")}</Button>
                      <OrderActions
                        busy={actionBusy}
                        order={order}
                        onCancel={() => setCancelTarget(order)}
                        onCompleted={() => runTransition(order, markCompleted, "admin.orderCompleted")}
                        onConfirm={() => runTransition(order, confirmOrder, "admin.orderConfirmed")}
                        onPacking={() => runTransition(order, markPacking, "admin.orderPacking")}
                        onRefund={() => openRefund(order)}
                        onShipping={() => runTransition(order, markShipping, "admin.orderShipping")}
                        t={t}
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
          {!loading && !orders.length && <div className="p-5 text-sm text-slate-500">{t("admin.noAdminOrders")}</div>}
        </div>
          <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t} />
      </Panel>

      {detailLoading && <BlockingLoader text={t("common.loading")} />}
      {selected && (
        <OrderDetailDrawer
          actionBusy={actionBusy}
          language={i18n.language}
          onCancel={() => setCancelTarget(selected)}
          onClose={() => setSelected(null)}
          onCompleted={() => runTransition(selected, markCompleted, "admin.orderCompleted")}
          onConfirm={() => runTransition(selected, confirmOrder, "admin.orderConfirmed")}
          onPacking={() => runTransition(selected, markPacking, "admin.orderPacking")}
          onRefund={() => openRefund(selected)}
          onShipping={() => runTransition(selected, markShipping, "admin.orderShipping")}
          order={selected}
          t={t}
        />
      )}
      {cancelTarget && (
        <CancelModal
          busy={Boolean(actionBusy)}
          onClose={() => {
            setCancelTarget(null);
            setCancelReason("");
          }}
          onSubmit={submitCancel}
          order={cancelTarget}
          reason={cancelReason}
          setReason={setCancelReason}
          t={t}
        />
      )}
      {refundTarget && (
        <RefundModal
          busy={Boolean(actionBusy)}
          form={refundForm}
          language={i18n.language}
          onClose={() => {
            setRefundTarget(null);
            setRefundForm({ amount: "", reason: "", note: "" });
          }}
          onSubmit={submitRefund}
          order={refundTarget}
          setForm={setRefundForm}
          t={t}
        />
      )}
    </div>
  );
}

function OrderDetailDrawer({ actionBusy, language, onCancel, onClose, onCompleted, onConfirm, onPacking, onRefund, onShipping, order, t }) {
  return (
    <Drawer title={order.orderCode} onClose={onClose}>
      <div className="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 md:flex-row md:items-start md:justify-between">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-blue-600">{t("admin.orderDetail")}</span>
          <h2 className="mt-2 text-3xl font-bold text-slate-950">{order.orderCode}</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            <StatusBadge status={order.orderStatus} t={t} />
            <PaymentBadge status={order.paymentStatus} />
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <OrderActions
            busy={actionBusy}
            order={order}
            onCancel={onCancel}
            onCompleted={onCompleted}
            onConfirm={onConfirm}
            onPacking={onPacking}
            onRefund={onRefund}
            onShipping={onShipping}
            t={t}
          />
          <Button variant="secondary" type="button" onClick={onClose}>{t("common.close")}</Button>
        </div>
      </div>

        <div className="grid gap-5 xl:grid-cols-3">
        <InfoCard title={t("orders.payment")}>
            <MetaRow label={t("orders.paymentMethod")} value={order.paymentMethod || "-"} />
            <MetaRow label={t("orders.paymentStatus")} value={order.paymentStatus || "-"} />
            <MetaRow label={t("orders.paymentGroup")} value={order.paymentGroupCode || "-"} />
            <MetaRow label={t("orders.paidAt")} value={formatDateTime(order.paidAt, language)} />
          </InfoCard>
          <InfoCard title={t("orders.shipping")}>
            <MetaRow label={t("account.recipientName")} value={order.shippingRecipientName || "-"} />
            <MetaRow label={t("account.phoneNumber")} value={order.shippingPhoneNumber || "-"} />
            <MetaRow label={t("account.addressLine")} value={formatAddress(order)} />
          </InfoCard>
          <InfoCard title={t("orders.status")}>
            <MetaRow label={t("orders.createdAt")} value={formatDateTime(order.createdAt, language)} />
            <MetaRow label={t("orders.updatedAt")} value={formatDateTime(order.updatedAt, language)} />
            <MetaRow label={t("admin.cancelReason")} value={order.cancelReason || "-"} />
            <MetaRow label={t("checkout.notes")} value={order.notes || "-"} />
          </InfoCard>
        </div>

        <InfoCard title={t("orders.items")} className="mt-5">
          <div className="overflow-hidden rounded-xl border border-slate-200">
            {(order.items || []).map((item) => (
              <div className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_90px_120px_120px] md:items-center" key={item.id}>
                <div className="flex items-center gap-3">
                  {item.thumbnailUrl && <img className="h-16 w-12 rounded-md object-cover ring-1 ring-slate-200" src={item.thumbnailUrl} alt={item.productName} />}
                  <div>
                    <p className="font-bold text-slate-950">{item.productName}</p>
                    <p className="text-xs text-slate-500">{item.sku || "-"} / {item.variationColor || "-"} / {item.variationSize || "-"}</p>
                  </div>
                </div>
                <span>{t("common.quantity")}: {item.quantity}</span>
                <span>{formatVND(item.finalPrice, language)}</span>
                <strong>{formatVND(item.lineTotal, language)}</strong>
              </div>
            ))}
          </div>
        </InfoCard>

        <div className="mt-5 grid gap-5 xl:grid-cols-2">
          <InfoCard title={t("common.total")}>
            <MetaRow label={t("checkout.subtotal")} value={formatVND(order.subtotal, language)} />
            <MetaRow label={t("orders.discount")} value={formatVND(order.discountAmount, language)} />
            <MetaRow label={t("checkout.shippingFee")} value={formatVND(order.shippingFee, language)} />
            <MetaRow label={t("common.total")} value={formatVND(order.totalAmount, language)} strong />
          </InfoCard>
          {order.refund ? (
            <InfoCard title={t("orders.refund")}>
              <MetaRow label={t("orders.refundCode")} value={order.refund.refundCode || "-"} />
              <MetaRow label={t("common.amount")} value={formatVND(order.refund.amount, language)} />
              <MetaRow label={t("orders.refundStatus")} value={order.refund.status || "-"} />
              <MetaRow label={t("admin.refundedBy")} value={order.refund.refundedBy || "-"} />
              <MetaRow label={t("orders.refundedAt")} value={formatDateTime(order.refund.refundedAt, language)} />
              <MetaRow label={t("admin.refundReason")} value={order.refund.reason || "-"} />
              <MetaRow label={t("admin.refundNote")} value={order.refund.note || "-"} />
            </InfoCard>
          ) : (
            <InfoCard title={t("orders.refund")}>
              <p className="text-sm text-slate-500">{t("admin.noRefund")}</p>
            </InfoCard>
          )}
        </div>
      </Drawer>
  );
}

function OrderActions({ busy, order, onCancel, onCompleted, onConfirm, onPacking, onRefund, onShipping, t }) {
  const actions = nextActions(order);
  if (!actions.length || TERMINAL_STATUSES.has(order.orderStatus)) return null;

  return (
    <>
      {actions.includes("confirm") && <Button size="sm" variant="secondary" disabled={Boolean(busy)} onClick={onConfirm}>{t("admin.confirmOrder")}</Button>}
      {actions.includes("packing") && <Button size="sm" variant="secondary" disabled={Boolean(busy)} onClick={onPacking}>{t("admin.markPacking")}</Button>}
      {actions.includes("shipping") && <Button size="sm" variant="secondary" disabled={Boolean(busy)} onClick={onShipping}>{t("admin.markShipping")}</Button>}
      {actions.includes("completed") && <Button size="sm" variant="secondary" disabled={Boolean(busy)} onClick={onCompleted}>{t("admin.markCompleted")}</Button>}
      {actions.includes("cancel") && <Button size="sm" variant="danger" disabled={Boolean(busy)} onClick={onCancel}>{t("admin.cancelAdminOrder")}</Button>}
      {actions.includes("refund") && <Button size="sm" variant="danger" disabled={Boolean(busy)} onClick={onRefund}>{t("admin.markRefunded")}</Button>}
    </>
  );
}

function nextActions(order) {
  const actions = [];
  if (order.orderStatus === "PENDING_CONFIRMATION") actions.push("confirm", "cancel");
  if (order.orderStatus === "PAID") actions.push("confirm");
  if (order.orderStatus === "CONFIRMED") actions.push("packing", "cancel");
  if (order.orderStatus === "PACKING") actions.push("shipping", "cancel");
  if (order.orderStatus === "SHIPPING") actions.push("completed");
  if (canRefund(order)) actions.push("refund");
  return actions;
}

function canRefund(order) {
  return REFUNDABLE_STATUSES.has(order.orderStatus) && order.paymentStatus === "SUCCESS" && !order.refund;
}

function CancelModal({ busy, onClose, onSubmit, order, reason, setReason, t }) {
  return (
    <Modal title={t("admin.cancelOrderTitle", { code: order.orderCode })} onClose={onClose}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <p className="rounded-xl bg-amber-50 p-4 text-sm font-semibold text-amber-700">{t("admin.cancelStockNote")}</p>
        <Textarea maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} placeholder={t("orders.cancelReasonPlaceholder")} />
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t("common.cancel")}</Button>
          <Button disabled={busy} type="submit">{t("orders.confirmCancel")}</Button>
        </div>
      </form>
    </Modal>
  );
}

function RefundModal({ busy, form, language, onClose, onSubmit, order, setForm, t }) {
  return (
    <Modal title={t("admin.refundOrderTitle", { code: order.orderCode })} onClose={onClose}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <p className="rounded-xl bg-blue-50 p-4 text-sm font-semibold text-blue-700">{t("admin.manualRefundNote")}</p>
        <MetaRow label={t("common.total")} value={formatVND(order.totalAmount, language)} strong />
        <Input required min="0" step="1000" type="number" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder={t("common.amount")} />
        <Input required maxLength={255} value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} placeholder={t("admin.refundReason")} />
        <Textarea required maxLength={1000} value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder={t("admin.refundNote")} />
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t("common.cancel")}</Button>
          <Button disabled={busy} type="submit">{t("admin.markRefunded")}</Button>
        </div>
      </form>
    </Modal>
  );
}

function toQuery(filters) {
  return {
    status: filters.status || undefined,
    keyword: filters.keyword || undefined,
    fromDate: toInstant(filters.fromDate),
    toDate: toInstant(filters.toDate),
    page: Number(filters.page || 1),
    size: Number(filters.size || 20),
  };
}

function toInstant(value) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

function validateDateRange(filters, t) {
  if (!filters.fromDate || !filters.toDate) return "";
  const from = new Date(filters.fromDate).getTime();
  const to = new Date(filters.toDate).getTime();
  if (!Number.isNaN(from) && !Number.isNaN(to) && from > to) return t("admin.invalidDateRange");
  return "";
}

function formatAddress(order) {
  return [order.shippingAddressLine, order.shippingWard, order.shippingDistrict, order.shippingCity].filter(Boolean).join(", ") || "-";
}

function statusLabel(status, t) {
  return t(`orders.statusLabels.${status}`, { defaultValue: status || "-" });
}

function filtersFromSearch(searchParams) {
  return {
    status: ORDER_STATUSES.includes(searchParams.get("status")) ? searchParams.get("status") : "",
    keyword: searchParams.get("keyword") || "",
    fromDate: searchParams.get("fromDate") || "",
    toDate: searchParams.get("toDate") || "",
    page: positiveNumber(searchParams.get("page"), 1),
    size: PAGE_SIZES.includes(Number(searchParams.get("size"))) ? Number(searchParams.get("size")) : 20,
  };
}

function positiveNumber(value, fallback) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) return fallback;
  return Number(text) > 0 ? Number(text) : fallback;
}

function createEmptyMeta(filters) {
  return readPageMeta([], { page: filters.page || 1, size: filters.size || 20, totalPages: 0 });
}

function StatusBadge({ status, t }) {
  return <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700">{statusLabel(status, t)}</span>;
}

function PaymentBadge({ status }) {
  return <span className="mt-1 inline-flex rounded-full bg-blue-50 px-2 py-1 text-xs font-bold text-blue-700">{status || "-"}</span>;
}

function BlockingLoader({ text }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm">
      <div className="rounded-2xl bg-white p-8 text-sm font-bold text-slate-700 shadow-2xl">{text}</div>
    </div>
  );
}
