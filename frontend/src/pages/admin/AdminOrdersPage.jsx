import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import {
  ShoppingBag, Search, Filter, Eye, Check, Package, Truck,
  CheckCircle, XCircle, RefreshCw, X, RotateCcw,
  AlertTriangle,
} from "lucide-react";

import {
  cancelAdminOrder, confirmOrder, getAdminOrder, getAdminOrders,
  markCompleted, markPacking, markRefunded, markShipping,
} from "../../api/adminOrdersApi.js";
import { Drawer, InfoCard, MetaRow as MetaRow, Modal, Pagination } from "../../components/ui/index.jsx";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeOrder, pageMeta as readPageMeta, pageRows } from "../../utils/mappers.js";

/* ── Constants ─────────────────────────────────── */
const ORDER_STATUSES = [
  "PENDING_CONFIRMATION","PENDING_PAYMENT","PAID","CONFIRMED",
  "PACKING","SHIPPING","COMPLETED","CANCELLED","PAYMENT_FAILED","EXPIRED","REFUNDED",
];
const PAGE_SIZES = [10, 20, 50];
const REFUNDABLE_STATUSES = new Set(["PAID","CONFIRMED","PACKING"]);
const TERMINAL_STATUSES   = new Set(["COMPLETED","CANCELLED","PAYMENT_FAILED","EXPIRED","REFUNDED"]);

const ORDER_STATUS_STYLES = {
  PENDING_CONFIRMATION: "bg-amber-50 text-amber-700 border-amber-200/60 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20",
  PENDING_PAYMENT:      "bg-amber-50 text-amber-700 border-amber-200/60 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20",
  PAID:                 "bg-sky-50 text-sky-700 border-sky-200/60 dark:bg-sky-500/10 dark:text-sky-400 dark:border-sky-500/20",
  CONFIRMED:            "bg-indigo-50 text-indigo-700 border-indigo-200/60 dark:bg-indigo-500/10 dark:text-indigo-400 dark:border-indigo-500/20",
  PACKING:              "bg-violet-50 text-violet-700 border-violet-200/60 dark:bg-violet-500/10 dark:text-violet-400 dark:border-violet-500/20",
  SHIPPING:             "bg-blue-50 text-blue-700 border-blue-200/60 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20",
  COMPLETED:            "bg-emerald-50 text-emerald-700 border-emerald-200/60 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20",
  CANCELLED:            "bg-rose-50 text-rose-700 border-rose-200/60 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20",
  PAYMENT_FAILED:       "bg-rose-50 text-rose-700 border-rose-200/60 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20",
  EXPIRED:              "bg-slate-100 text-slate-600 border-slate-200/60 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700",
  REFUNDED:             "bg-orange-50 text-orange-700 border-orange-200/60 dark:bg-orange-500/10 dark:text-orange-400 dark:border-orange-500/20",
};

const PAYMENT_STATUS_STYLES = {
  SUCCESS:  "bg-emerald-50 text-emerald-700 border-emerald-200/60 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20",
  PENDING:  "bg-amber-50 text-amber-700 border-amber-200/60 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20",
  FAILED:   "bg-rose-50 text-rose-700 border-rose-200/60 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20",
  REFUNDED: "bg-orange-50 text-orange-700 border-orange-200/60 dark:bg-orange-500/10 dark:text-orange-400 dark:border-orange-500/20",
};

const emptyFilters = { status:"", keyword:"", fromDate:"", toDate:"", page:1, size:20 };

/* ── Shared UI Primitives ──────────────────────── */
function PInput({ ...props }) {
  return (
    <input className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none transition-shadow focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" {...props}/>
  );
}
function PSelect({ children, ...props }) {
  return (
    <select className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow" {...props}>
      {children}
    </select>
  );
}
function PTextarea({ ...props }) {
  return (
    <textarea rows={3} className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 resize-none" {...props}/>
  );
}
function PrimaryBtn({ children, loading, disabled, ...props }) {
  return (
    <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }} disabled={loading || disabled}
      className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-200 dark:shadow-none transition-colors focus:ring-2 focus:ring-offset-2 focus:ring-indigo-600 disabled:opacity-50" {...props}>
      {loading && <RefreshCw size={13} className="animate-spin"/>}
      {children}
    </motion.button>
  );
}
function SecBtn({ children, danger, disabled, icon: Icon, ...props }) {
  return (
    <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }} disabled={disabled}
      className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition-colors disabled:opacity-50 ${
        danger
          ? "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400 dark:hover:bg-rose-500/20"
          : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
      }`} {...props}>
      {Icon && <Icon size={13}/>}
      {children}
    </motion.button>
  );
}
function Toast({ message, onClose }) {
  if (!message) return null;
  const isError = /err|fail|lỗi|không|invalid|required/i.test(message);
  return (
    <motion.div initial={{ opacity:0, y:-8 }} animate={{ opacity:1, y:0 }} exit={{ opacity:0, y:-8 }}
      className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm font-semibold mb-4 ${
        isError ? "border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400"
                : "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-400"
      }`}>
      <span>{message}</span>
      <button type="button" onClick={onClose}><X size={14}/></button>
    </motion.div>
  );
}
function StatusBadge({ status, t }) {
  const cls = ORDER_STATUS_STYLES[status] || "bg-slate-100 text-slate-600 border-slate-200/60";
  return <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${cls}`}>{statusLabel(status, t)}</span>;
}
function PaymentBadge({ status }) {
  const cls = PAYMENT_STATUS_STYLES[status] || "bg-slate-100 text-slate-600 border-slate-200/60";
  return <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${cls}`}>{status || "—"}</span>;
}

/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminOrdersPage() {
  const { t, i18n } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialFilters = useMemo(() => filtersFromSearch(searchParams), [searchParams]);
  const [filters, setFilters]           = useState(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState(initialFilters);
  const [orders, setOrders]             = useState([]);
  const [pageMeta, setPageMeta]         = useState(createEmptyMeta(emptyFilters));
  const [loading, setLoading]           = useState(false);
  const [message, setMessage]           = useState("");
  const [selected, setSelected]         = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionBusy, setActionBusy]     = useState("");
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState("");
  const [refundTarget, setRefundTarget] = useState(null);
  const [refundForm, setRefundForm]     = useState({ amount:"", reason:"", note:"" });

  const refreshOrders = useCallback(async (nextFilters = appliedFilters) => {
    setLoading(true); setMessage("");
    try {
      const page = await getAdminOrders(toQuery(nextFilters));
      setOrders(pageRows(page).map(normalizeOrder));
      setPageMeta(readPageMeta(page, { page: nextFilters.page, size: nextFilters.size }));
    } catch (error) {
      setOrders([]); setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.orderLoadFailed"));
    } finally { setLoading(false); }
  }, [appliedFilters, t]);

  useEffect(() => { refreshOrders(appliedFilters); }, [appliedFilters, refreshOrders]);
  useEffect(() => {
    const next = filtersFromSearch(searchParams);
    setFilters(next); setAppliedFilters(next);
  }, [searchParams]);

  function applyFilters(event) {
    event.preventDefault();
    const validation = validateDateRange(filters, t);
    if (validation) { setMessage(validation); return; }
    const next = { ...filters, page:1, size: Number(filters.size || 20) };
    setFilters(next); setAppliedFilters(next);
  }
  function clearFilters() { setFilters(emptyFilters); setAppliedFilters(emptyFilters); }
  function changePage(page) {
    const p = Math.max(1, page);
    setFilters(c => ({ ...c, page: p }));
    setAppliedFilters(c => ({ ...c, page: p }));
  }
  function changePageSize(size) {
    const next = { ...filters, page:1, size: Number(size || 20) };
    setFilters(next); setAppliedFilters(next);
  }

  async function openDetail(order) {
    setDetailLoading(true); setMessage("");
    try { setSelected(normalizeOrder(await getAdminOrder(order.id))); }
    catch (error) { setMessage(error.message || t("admin.orderDetailFailed")); }
    finally { setDetailLoading(false); }
  }

  async function runTransition(order, action, successKey) {
    setActionBusy(`${action.name}-${order.id}`); setMessage("");
    try {
      const updated = normalizeOrder(await action(order.id));
      applyUpdatedOrder(updated); setMessage(t(successKey));
      await refreshOrders(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.orderActionFailed")); }
    finally { setActionBusy(""); }
  }

  async function submitCancel(event) {
    event.preventDefault(); if (!cancelTarget) return;
    setActionBusy(`cancel-${cancelTarget.id}`); setMessage("");
    try {
      const updated = normalizeOrder(await cancelAdminOrder(cancelTarget.id, { reason: cancelReason }));
      applyUpdatedOrder(updated); setCancelTarget(null); setCancelReason("");
      setMessage(t("admin.adminOrderCancelled"));
      await refreshOrders(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.orderCancelFailed")); }
    finally { setActionBusy(""); }
  }

  async function submitRefund(event) {
    event.preventDefault(); if (!refundTarget) return;
    if (!refundForm.amount || !refundForm.reason.trim() || !refundForm.note.trim()) {
      setMessage(t("admin.refundValidation")); return;
    }
    setActionBusy(`refund-${refundTarget.id}`); setMessage("");
    try {
      const updated = normalizeOrder(await markRefunded(refundTarget.id, {
        amount: Number(refundForm.amount), reason: refundForm.reason, note: refundForm.note,
      }));
      applyUpdatedOrder(updated); setRefundTarget(null);
      setRefundForm({ amount:"", reason:"", note:"" });
      setMessage(t("admin.orderRefunded"));
      await refreshOrders(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.orderRefundFailed")); }
    finally { setActionBusy(""); }
  }

  function applyUpdatedOrder(updated) {
    setOrders(current => current.map(item => item.id === updated.id ? { ...item, ...updated } : item));
    setSelected(current => current?.id === updated.id ? updated : current);
  }

  function openRefund(order) {
    setRefundTarget(order);
    setRefundForm({ amount: Number(order.totalAmount || 0), reason:"", note:"" });
  }

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="max-w-full space-y-6">

      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.ordersTitle","Quản lý đơn hàng")}</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.ordersEyebrow","Theo dõi và xử lý tất cả đơn hàng")}</p>
        </div>
      </div>

      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {/* Filter bar */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <Filter size={16} className="text-indigo-500"/>
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.orderFilters","Bộ lọc")}</h3>
        </div>
        <div className="p-5">
          <form className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[160px_1fr_180px_180px_90px_auto_auto]" onSubmit={applyFilters}>
            <PSelect value={filters.status} onChange={e => setFilters({ ...filters, status: e.target.value })}>
              <option value="">{t("admin.allOrderStatuses","Tất cả trạng thái")}</option>
              {ORDER_STATUSES.map(s => <option key={s} value={s}>{statusLabel(s,t)}</option>)}
            </PSelect>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400"/>
              <PInput className="pl-9" value={filters.keyword} onChange={e => setFilters({ ...filters, keyword: e.target.value })} placeholder={t("admin.orderKeyword","Tìm đơn hàng...")}/>
            </div>
            <PInput type="datetime-local" value={filters.fromDate} onChange={e => setFilters({ ...filters, fromDate: e.target.value })} aria-label={t("admin.fromDate","Từ ngày")}/>
            <PInput type="datetime-local" value={filters.toDate} onChange={e => setFilters({ ...filters, toDate: e.target.value })} aria-label={t("admin.toDate","Đến ngày")}/>
            <PSelect value={filters.size} onChange={e => changePageSize(e.target.value)} aria-label="Page size">
              {PAGE_SIZES.map(s => <option key={s} value={s}>{s}</option>)}
            </PSelect>
            <PrimaryBtn type="submit">{t("admin.applyFilters","Lọc")}</PrimaryBtn>
            <SecBtn type="button" onClick={clearFilters}>{t("admin.clearFilters","Xóa lọc")}</SecBtn>
          </form>
        </div>
      </div>

      {/* Orders table */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <div className="flex items-center gap-2">
            <ShoppingBag size={16} className="text-indigo-500"/>
            <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.ordersList","Danh sách đơn hàng")}</h3>
          </div>
          <span className="text-xs font-semibold text-slate-400">{pageMeta.totalElements ?? 0} {t("admin.orders","đơn hàng")}</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap min-w-[1100px]">
            <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-medium text-xs uppercase tracking-wider">
              <tr>
                <th className="px-5 py-4">{t("admin.orderCode","Mã đơn")}</th>
                <th className="px-5 py-4">{t("orders.status","Trạng thái")}</th>
                <th className="px-5 py-4">{t("orders.payment","Thanh toán")}</th>
                <th className="px-5 py-4">{t("orders.paymentGroup","Nhóm TT")}</th>
                <th className="px-5 py-4">{t("common.total","Tổng tiền")}</th>
                <th className="px-5 py-4">{t("orders.items","Sản phẩm")}</th>
                <th className="px-5 py-4">{t("orders.paidAt","Thanh toán lúc")}</th>
                <th className="px-5 py-4">{t("orders.createdAt","Tạo lúc")}</th>
                <th className="px-5 py-4 text-right">{t("admin.actions","Hành động")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {loading && !orders.length
                ? [...Array(5)].map((_,i) => (
                    <tr key={i} className="animate-pulse">
                      {[...Array(9)].map((_,j) => (
                        <td key={j} className="px-5 py-4"><div className="h-4 bg-slate-100 dark:bg-slate-800 rounded w-3/4"/></td>
                      ))}
                    </tr>
                  ))
                : orders.map((order, index) => (
                    <motion.tr key={order.id}
                      initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }}
                      transition={{ delay: index * 0.04, duration: 0.3 }}
                      className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                      <td className="px-5 py-4">
                        <p className="font-semibold text-slate-900 dark:text-slate-100 font-mono">{order.orderCode}</p>
                        <p className="text-xs text-slate-400 dark:text-slate-500">#{order.id}</p>
                      </td>
                      <td className="px-5 py-4"><StatusBadge status={order.orderStatus} t={t}/></td>
                      <td className="px-5 py-4">
                        <p className="font-semibold text-slate-700 dark:text-slate-300 text-xs mb-1">{order.paymentMethod || "—"}</p>
                        <PaymentBadge status={order.paymentStatus}/>
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-500 dark:text-slate-400 font-mono">{order.paymentGroupCode || "—"}</td>
                      <td className="px-5 py-4 font-bold text-slate-900 dark:text-slate-100">{formatVND(order.totalAmount, i18n.language)}</td>
                      <td className="px-5 py-4 text-slate-600 dark:text-slate-300">{order.itemCount || 0}</td>
                      <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(order.paidAt, i18n.language)}</td>
                      <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(order.createdAt, i18n.language)}</td>
                      <td className="px-5 py-4">
                        <div className="flex items-center justify-end gap-1.5 flex-wrap">
                          <SecBtn onClick={() => openDetail(order)} icon={Eye}>{t("common.detail","Chi tiết")}</SecBtn>
                          <OrderActionBtns busy={actionBusy} order={order}
                            onCancel={() => setCancelTarget(order)}
                            onCompleted={() => runTransition(order, markCompleted, "admin.orderCompleted")}
                            onConfirm={() => runTransition(order, confirmOrder, "admin.orderConfirmed")}
                            onPacking={() => runTransition(order, markPacking, "admin.orderPacking")}
                            onRefund={() => openRefund(order)}
                            onShipping={() => runTransition(order, markShipping, "admin.orderShipping")}
                            t={t}/>
                        </div>
                      </td>
                    </motion.tr>
                  ))
              }
            </tbody>
          </table>
          {!loading && !orders.length && (
            <div className="flex flex-col items-center justify-center py-16 text-slate-400">
              <ShoppingBag size={32} className="mb-3 opacity-40"/>
              <p className="text-sm font-semibold">{t("admin.noAdminOrders","Không có đơn hàng nào")}</p>
            </div>
          )}
        </div>
        <div className="border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-3">
          <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t}/>
        </div>
      </div>

      {/* Detail loading overlay */}
      {detailLoading && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 backdrop-blur-sm">
          <div className="rounded-2xl bg-white dark:bg-slate-900 p-8 text-sm font-bold text-slate-700 dark:text-slate-300 shadow-2xl flex items-center gap-3">
            <RefreshCw size={16} className="animate-spin text-indigo-500"/>
            {t("common.loading","Đang tải...")}
          </div>
        </div>
      )}

      {/* Detail Drawer */}
      {selected && (
        <OrderDetailDrawer
          actionBusy={actionBusy} language={i18n.language}
          onCancel={() => setCancelTarget(selected)} onClose={() => setSelected(null)}
          onCompleted={() => runTransition(selected, markCompleted, "admin.orderCompleted")}
          onConfirm={() => runTransition(selected, confirmOrder, "admin.orderConfirmed")}
          onPacking={() => runTransition(selected, markPacking, "admin.orderPacking")}
          onRefund={() => openRefund(selected)}
          onShipping={() => runTransition(selected, markShipping, "admin.orderShipping")}
          order={selected} t={t}/>
      )}

      {/* Cancel modal */}
      {cancelTarget && (
        <CancelModal busy={Boolean(actionBusy)} onClose={() => { setCancelTarget(null); setCancelReason(""); }}
          onSubmit={submitCancel} order={cancelTarget} reason={cancelReason} setReason={setCancelReason} t={t}/>
      )}

      {/* Refund modal */}
      {refundTarget && (
        <RefundModal busy={Boolean(actionBusy)} form={refundForm} language={i18n.language}
          onClose={() => { setRefundTarget(null); setRefundForm({ amount:"", reason:"", note:"" }); }}
          onSubmit={submitRefund} order={refundTarget} setForm={setRefundForm} t={t}/>
      )}
    </motion.div>
  );
}

/* ── Sub-components (all logic preserved) ──────── */
function OrderDetailDrawer({ actionBusy, language, onCancel, onClose, onCompleted, onConfirm, onPacking, onRefund, onShipping, order, t }) {
  return (
    <Drawer title={order.orderCode} onClose={onClose}>
      <div className="mb-6 flex flex-col gap-4 border-b border-slate-200 dark:border-slate-800 pb-5 md:flex-row md:items-start md:justify-between">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">{t("admin.orderDetail","Chi tiết đơn hàng")}</span>
          <h2 className="mt-2 text-2xl font-bold text-slate-900 dark:text-slate-50">{order.orderCode}</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            <StatusBadge status={order.orderStatus} t={t}/>
            <PaymentBadge status={order.paymentStatus}/>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <OrderActionBtns busy={actionBusy} order={order}
            onCancel={onCancel} onCompleted={onCompleted} onConfirm={onConfirm}
            onPacking={onPacking} onRefund={onRefund} onShipping={onShipping} t={t}/>
          <SecBtn type="button" onClick={onClose}>{t("common.close","Đóng")}</SecBtn>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-3">
        <InfoCard title={t("orders.payment","Thanh toán")}>
          <MetaRow label={t("orders.paymentMethod","Phương thức")} value={order.paymentMethod || "—"}/>
          <MetaRow label={t("orders.paymentStatus","Trạng thái TT")} value={order.paymentStatus || "—"}/>
          <MetaRow label={t("orders.paymentGroup","Nhóm TT")} value={order.paymentGroupCode || "—"}/>
          <MetaRow label={t("orders.paidAt","Thanh toán lúc")} value={formatDateTime(order.paidAt, language)}/>
        </InfoCard>
        <InfoCard title={t("orders.shipping","Giao hàng")}>
          <MetaRow label={t("account.recipientName","Người nhận")} value={order.shippingRecipientName || "—"}/>
          <MetaRow label={t("account.phoneNumber","Số điện thoại")} value={order.shippingPhoneNumber || "—"}/>
          <MetaRow label={t("account.addressLine","Địa chỉ")} value={formatAddress(order)}/>
        </InfoCard>
        <InfoCard title={t("orders.status","Trạng thái")}>
          <MetaRow label={t("orders.createdAt","Tạo lúc")} value={formatDateTime(order.createdAt, language)}/>
          <MetaRow label={t("orders.updatedAt","Cập nhật")} value={formatDateTime(order.updatedAt, language)}/>
          <MetaRow label={t("admin.cancelReason","Lý do hủy")} value={order.cancelReason || "—"}/>
          <MetaRow label={t("checkout.notes","Ghi chú")} value={order.notes || "—"}/>
        </InfoCard>
      </div>

      <InfoCard title={t("orders.items","Sản phẩm")} className="mt-5">
        <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700">
          {(order.items || []).map(item => (
            <div key={item.id} className="grid gap-3 border-b border-slate-100 dark:border-slate-800 p-4 last:border-0 md:grid-cols-[1fr_90px_120px_120px] md:items-center">
              <div className="flex items-center gap-3">
                {item.thumbnailUrl && <img className="h-14 w-10 rounded-lg object-cover" src={item.thumbnailUrl} alt={item.productName}/>}
                <div>
                  <p className="font-semibold text-slate-900 dark:text-slate-100">{item.productName}</p>
                  <p className="text-xs text-slate-400 mt-0.5">{item.sku || "—"} / {item.variationColor || "—"} / {item.variationSize || "—"}</p>
                </div>
              </div>
              <span className="text-sm text-slate-600 dark:text-slate-300">x{item.quantity}</span>
              <span className="text-sm text-slate-600 dark:text-slate-300">{formatVND(item.finalPrice, language)}</span>
              <strong className="text-sm font-bold text-slate-900 dark:text-slate-100">{formatVND(item.lineTotal, language)}</strong>
            </div>
          ))}
        </div>
      </InfoCard>

      <div className="mt-5 grid gap-5 xl:grid-cols-2">
        <InfoCard title={t("common.total","Tổng cộng")}>
          <MetaRow label={t("checkout.subtotal","Tạm tính")} value={formatVND(order.subtotal, language)}/>
          <MetaRow label={t("orders.discount","Giảm giá")} value={formatVND(order.discountAmount, language)}/>
          <MetaRow label={t("checkout.shippingFee","Phí vận chuyển")} value={formatVND(order.shippingFee, language)}/>
          <MetaRow label={t("common.total","Tổng")} value={formatVND(order.totalAmount, language)} strong/>
        </InfoCard>
        {order.refund ? (
          <InfoCard title={t("orders.refund","Hoàn tiền")}>
            <MetaRow label={t("orders.refundCode","Mã hoàn")} value={order.refund.refundCode || "—"}/>
            <MetaRow label={t("common.amount","Số tiền")} value={formatVND(order.refund.amount, language)}/>
            <MetaRow label={t("orders.refundStatus","Trạng thái")} value={order.refund.status || "—"}/>
            <MetaRow label={t("admin.refundedBy","Người thực hiện")} value={order.refund.refundedBy || "—"}/>
            <MetaRow label={t("orders.refundedAt","Hoàn lúc")} value={formatDateTime(order.refund.refundedAt, language)}/>
            <MetaRow label={t("admin.refundReason","Lý do")} value={order.refund.reason || "—"}/>
            <MetaRow label={t("admin.refundNote","Ghi chú")} value={order.refund.note || "—"}/>
          </InfoCard>
        ) : (
          <InfoCard title={t("orders.refund","Hoàn tiền")}>
            <p className="text-sm text-slate-400">{t("admin.noRefund","Chưa có hoàn tiền")}</p>
          </InfoCard>
        )}
      </div>
    </Drawer>
  );
}

function OrderActionBtns({ busy, order, onCancel, onCompleted, onConfirm, onPacking, onRefund, onShipping, t }) {
  const actions = nextActions(order);
  if (!actions.length || TERMINAL_STATUSES.has(order.orderStatus)) return null;
  return (
    <>
      {actions.includes("confirm")  && <SecBtn disabled={Boolean(busy)} onClick={onConfirm} icon={Check}>{t("admin.confirmOrder","Xác nhận")}</SecBtn>}
      {actions.includes("packing")  && <SecBtn disabled={Boolean(busy)} onClick={onPacking} icon={Package}>{t("admin.markPacking","Đóng gói")}</SecBtn>}
      {actions.includes("shipping") && <SecBtn disabled={Boolean(busy)} onClick={onShipping} icon={Truck}>{t("admin.markShipping","Vận chuyển")}</SecBtn>}
      {actions.includes("completed")&& <SecBtn disabled={Boolean(busy)} onClick={onCompleted} icon={CheckCircle}>{t("admin.markCompleted","Hoàn thành")}</SecBtn>}
      {actions.includes("cancel")   && <SecBtn danger disabled={Boolean(busy)} onClick={onCancel} icon={XCircle}>{t("admin.cancelAdminOrder","Hủy đơn")}</SecBtn>}
      {actions.includes("refund")   && <SecBtn danger disabled={Boolean(busy)} onClick={onRefund} icon={RotateCcw}>{t("admin.markRefunded","Hoàn tiền")}</SecBtn>}
    </>
  );
}

function CancelModal({ busy, onClose, onSubmit, order, reason, setReason, t }) {
  return (
    <Modal title={t("admin.cancelOrderTitle","Hủy đơn hàng: {{code}}", { code: order.orderCode })} onClose={onClose}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <div className="flex items-start gap-3 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200/60 dark:border-amber-500/20 p-4 text-sm font-semibold text-amber-700 dark:text-amber-400">
          <AlertTriangle size={16} className="flex-shrink-0 mt-0.5"/>
          {t("admin.cancelStockNote","Tồn kho sẽ được hoàn lại sau khi hủy")}
        </div>
        <PTextarea maxLength={500} value={reason} onChange={e => setReason(e.target.value)} placeholder={t("orders.cancelReasonPlaceholder","Lý do hủy đơn...")}/>
        <div className="flex flex-wrap justify-end gap-2">
          <SecBtn type="button" onClick={onClose}>{t("common.cancel","Hủy")}</SecBtn>
          <PrimaryBtn disabled={busy} type="submit">{t("orders.confirmCancel","Xác nhận hủy")}</PrimaryBtn>
        </div>
      </form>
    </Modal>
  );
}

function RefundModal({ busy, form, language, onClose, onSubmit, order, setForm, t }) {
  return (
    <Modal title={t("admin.refundOrderTitle","Hoàn tiền: {{code}}", { code: order.orderCode })} onClose={onClose}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <div className="rounded-xl bg-sky-50 dark:bg-sky-500/10 border border-sky-200/60 dark:border-sky-500/20 p-4 text-sm font-semibold text-sky-700 dark:text-sky-400">
          {t("admin.manualRefundNote","Đây là hoàn tiền thủ công. Vui lòng xác nhận kỹ trước khi thực hiện.")}
        </div>
        <MetaRow label={t("common.total","Tổng tiền đơn")} value={formatVND(order.totalAmount, language)} strong/>
        <PInput required min="0" step="1000" type="number" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} placeholder={t("common.amount","Số tiền hoàn")}/>
        <PInput required maxLength={255} value={form.reason} onChange={e => setForm({ ...form, reason: e.target.value })} placeholder={t("admin.refundReason","Lý do hoàn tiền")}/>
        <PTextarea required maxLength={1000} value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} placeholder={t("admin.refundNote","Ghi chú nội bộ")}/>
        <div className="flex flex-wrap justify-end gap-2">
          <SecBtn type="button" onClick={onClose}>{t("common.cancel","Hủy")}</SecBtn>
          <PrimaryBtn disabled={busy} type="submit">{t("admin.markRefunded","Xác nhận hoàn tiền")}</PrimaryBtn>
        </div>
      </form>
    </Modal>
  );
}

/* ── Pure utils (100% preserved) ──────────────── */
function nextActions(order) {
  const actions = [];
  if (order.orderStatus === "PENDING_CONFIRMATION") actions.push("confirm","cancel");
  if (order.orderStatus === "PAID") actions.push("confirm");
  if (order.orderStatus === "CONFIRMED") actions.push("packing","cancel");
  if (order.orderStatus === "PACKING") actions.push("shipping","cancel");
  if (order.orderStatus === "SHIPPING") actions.push("completed");
  if (canRefund(order)) actions.push("refund");
  return actions;
}
function canRefund(order) {
  return REFUNDABLE_STATUSES.has(order.orderStatus) && order.paymentStatus === "SUCCESS" && !order.refund;
}
function toQuery(filters) {
  return {
    status: filters.status || undefined, keyword: filters.keyword || undefined,
    fromDate: toInstant(filters.fromDate), toDate: toInstant(filters.toDate),
    page: Number(filters.page || 1), size: Number(filters.size || 20),
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
  const to   = new Date(filters.toDate).getTime();
  if (!Number.isNaN(from) && !Number.isNaN(to) && from > to) return t("admin.invalidDateRange");
  return "";
}
function formatAddress(order) {
  return [order.shippingAddressLine, order.shippingWard, order.shippingDistrict, order.shippingCity].filter(Boolean).join(", ") || "—";
}
function statusLabel(status, t) {
  return t(`orders.statusLabels.${status}`, { defaultValue: status || "—" });
}
function filtersFromSearch(searchParams) {
  return {
    status:   ORDER_STATUSES.includes(searchParams.get("status")) ? searchParams.get("status") : "",
    keyword:  searchParams.get("keyword") || "",
    fromDate: searchParams.get("fromDate") || "",
    toDate:   searchParams.get("toDate") || "",
    page:     positiveNumber(searchParams.get("page"), 1),
    size:     PAGE_SIZES.includes(Number(searchParams.get("size"))) ? Number(searchParams.get("size")) : 20,
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
