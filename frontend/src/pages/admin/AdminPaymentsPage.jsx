import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import {
  CreditCard, Search, RefreshCw, X,
  RotateCcw, FileText, ExternalLink,
} from "lucide-react";

import { getAdminPaymentGroup, reconcilePaymentGroup } from "../../api/adminPaymentsApi.js";
import {
  Drawer, InfoCard, MetaRow as Meta, StatusPill, Table, useConfirm, useToast,
} from "../../components/ui/index.jsx";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeOrder, normalizePaymentGroup } from "../../utils/mappers.js";

/* ── Constants ─────────────────────────────────── */
const TERMINAL_STATUSES = new Set(["SUCCESS","CANCELLED","EXPIRED","REFUNDED"]);

/* ── Shared UI Primitives ──────────────────────── */
function PInput({ ...props }) {
  return <input className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-shadow" {...props}/>;
}
function PrimaryBtn({ children, loading, disabled, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }} disabled={loading||disabled}
      className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-200 dark:shadow-none transition-colors disabled:opacity-50" {...props}>
      {loading && <RefreshCw size={13} className="animate-spin"/>}
      {children}
    </motion.button>
  );
}
function SecBtn({ children, icon: Icon, disabled, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }} disabled={disabled}
      className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 px-4 py-2 text-sm font-semibold transition-colors disabled:opacity-50" {...props}>
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
function Card({ title, icon: Icon, children, action }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
        <div className="flex items-center gap-2">
          {Icon && <Icon size={16} className="text-indigo-500"/>}
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h3>
        </div>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}
/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminPaymentsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  // ── State (100% preserved) ──────────────────
  const [code, setCode]                     = useState(searchParams.get("code") || "");
  const [message, setMessage]               = useState("");
  const [group, setGroup]                   = useState(null);
  const [groupDrawerOpen, setGroupDrawerOpen] = useState(false);
  const [reconcileResult, setReconcileResult] = useState(null);
  const [loading, setLoading]               = useState(false);
  const [reconciling, setReconciling]       = useState(false);

  const lookup = useCallback(async (rawCode = code, options = {}) => {
    const normalizedCode = rawCode.trim();
    if (!normalizedCode) { setMessage(t("admin.paymentCodeRequired")); return; }
    setLoading(true);
    if (!options.silent) setMessage("");
    try {
      const result = normalizePaymentGroup(await getAdminPaymentGroup(normalizedCode));
      setGroup(result); setReconcileResult(null); setCode(normalizedCode);
      if (!options.silent) setGroupDrawerOpen(true);
      if (searchParams.get("code") !== normalizedCode) setSearchParams({ code: normalizedCode });
      if (!options.silent) setMessage(t("admin.paymentLoaded"));
    } catch (error) {
      setGroup(null); setReconcileResult(null); setGroupDrawerOpen(false);
      setMessage(error.message || t("admin.paymentUnavailable"));
    } finally { setLoading(false); }
  }, [code, searchParams, setSearchParams, t]);

  useEffect(() => {
    const nextCode = searchParams.get("code") || "";
    setCode(nextCode);
    if (nextCode) lookup(nextCode, { silent: true });
  }, [lookup, searchParams]);

  async function reconcile() {
    const normalizedCode = code.trim();
    if (!normalizedCode) { setMessage(t("admin.paymentCodeRequired")); return; }
    if (group?.status && TERMINAL_STATUSES.has(group.status)) {
      const confirmed = await confirm({
        title: t("admin.reconcile"),
        message: t("admin.confirmTerminalReconcile", { status: group.status }),
        confirmLabel: t("admin.reconcile"), cancelLabel: t("common.cancel"), danger: false,
      });
      if (!confirmed) return;
    }
    setReconciling(true); setMessage("");
    try {
      const result = await reconcilePaymentGroup(normalizedCode);
      setReconcileResult(result);
      const msg = t("admin.reconciled", {
        before: result.localStatusBefore || t("common.unknown"),
        after:  result.localStatusAfter  || t("common.unknown"),
      });
      setMessage(msg);
      toast({ message: msg, variant: "success" });
      await lookup(normalizedCode, { silent: true });
      setReconcileResult(result);
      setGroupDrawerOpen(true);
    } catch (error) { setMessage(error.message || t("admin.errors.reconcile")); }
    finally { setReconciling(false); }
  }

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="max-w-full space-y-6">

      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.paymentsTitle","Quản lý thanh toán")}</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.paymentsEyebrow","Tra cứu và đối soát giao dịch thanh toán")}</p>
      </div>

      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {/* Lookup panel */}
      <Card title={t("admin.paymentLookup","Tra cứu thanh toán")} icon={Search}>
        <div className="grid gap-3 sm:grid-cols-[1fr_auto_auto_auto]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400"/>
            <PInput
              className="pl-9"
              value={code}
              onChange={e => setCode(e.target.value)}
              placeholder={t("admin.paymentGroupCode","Nhập mã nhóm thanh toán...")}
              onKeyDown={e => { if (e.key === "Enter") lookup(); }}
            />
          </div>
          <SecBtn disabled={loading || reconciling || !code.trim()} onClick={() => lookup()}>
            {loading ? <><RefreshCw size={13} className="animate-spin"/> {t("common.loading","Đang tải...")}</> : t("admin.lookup","Tra cứu")}
          </SecBtn>
          <PrimaryBtn disabled={loading || reconciling || !code.trim()} loading={reconciling} onClick={reconcile}>
            {reconciling ? t("common.working","Đang xử lý...") : <><RotateCcw size={13}/> {t("admin.reconcile","Đối soát")}</>}
          </PrimaryBtn>
          <SecBtn disabled={loading || reconciling || !group} onClick={() => lookup(code)}>
            <RefreshCw size={13}/> {t("admin.refresh","Làm mới")}
          </SecBtn>
        </div>
      </Card>

      {!group ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <CreditCard size={36} className="mb-3 opacity-40"/>
          <p className="text-sm font-semibold">
            {loading ? t("common.loading","Đang tải...") : t("admin.paymentEmptyState","Nhập mã thanh toán để tra cứu")}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-slate-400">{t("admin.paymentLoaded","Đã tải thanh toán")}</p>
              <h2 className="mt-1 text-lg font-bold text-slate-900 dark:text-slate-50">
                {group.paymentCode || group.paymentGroupCode || t("admin.paymentReady","Sẵn sàng xem chi tiết")}
              </h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t("admin.paymentReady","Sẵn sàng xem chi tiết")}</p>
            </div>
            <SecBtn type="button" onClick={() => setGroupDrawerOpen(true)}>{t("common.detail","Chi tiết")}</SecBtn>
          </div>
        </div>
      )}
      <PaymentGroupDrawer
        group={group}
        language={i18n.language}
        onClose={() => setGroupDrawerOpen(false)}
        open={groupDrawerOpen && Boolean(group)}
        reconcileResult={reconcileResult}
        t={t}
      />
    </motion.div>
  );
}

/* ── Sub-components (all logic preserved) ──────── */
function PaymentGroupDrawer({ group, language, onClose, open, reconcileResult, t }) {
  if (!group) return null;
  return (
    <Drawer
      description={t("admin.paymentLookup","Tra cứu thanh toán")}
      onClose={onClose}
      open={open}
      size="xl"
      title={t("admin.paymentGroup","Nhóm thanh toán")}
    >
      <div className="grid gap-5">
        <PaymentGroupSummary group={group} language={language} t={t}/>
        <PaymentsTable group={group} language={language} t={t}/>
        <RelatedOrders group={group} language={language} t={t}/>
        {reconcileResult && <ReconcileResult language={language} result={reconcileResult} t={t}/>}
      </div>
    </Drawer>
  );
}

function PaymentGroupSummary({ group, language, t }) {
  return (
    <div className="grid gap-5 xl:grid-cols-3">
      <InfoCard title={t("admin.groupStatusTitle","Thông tin nhóm")}>
        <Meta label={t("admin.paymentGroupCode","Mã nhóm")} value={group.paymentCode || group.paymentGroupCode || "—"}/>
        <Meta label={t("common.method","Phương thức")} value={group.method || "—"}/>
        <Meta label={t("common.status","Trạng thái")} value={<StatusPill status={group.status} type="payment"/>}/>
        <Meta label={t("common.amount","Số tiền")} value={formatVND(group.amount, language)}/>
      </InfoCard>
      <InfoCard title={t("admin.providerData","Dữ liệu provider")}>
        <Meta label={t("admin.providerTxnRef","Mã GD provider")} value={group.providerTxnRef || "—"}/>
        <Meta label={t("admin.providerTransactionId","Transaction ID")} value={group.providerTransactionId || "—"}/>
        <Meta label={t("orders.paidAt","Thanh toán lúc")} value={formatDateTime(group.paidAt, language)}/>
        <Meta label={t("admin.expiresAt","Hết hạn lúc")} value={formatDateTime(group.expiresAt, language)}/>
      </InfoCard>
      <InfoCard title={t("admin.paymentProviderLinks","Link provider")}>
        <Meta label={t("admin.hasPaymentUrl","Có payment URL")} value={yesNo(Boolean(group.paymentUrl), t)}/>
        <Meta label={t("admin.hasDeeplink","Có deeplink")} value={yesNo(Boolean(group.deeplink), t)}/>
        <Meta label={t("admin.hasQrCode","Có QR code")} value={yesNo(Boolean(group.qrCodeUrl), t)}/>
        <p className="text-xs text-slate-400 mt-2">{t("admin.providerLinkNote","Link đã hết hạn nếu giao dịch hoàn thành")}</p>
      </InfoCard>
    </div>
  );
}

function PaymentsTable({ group, language, t }) {
  const payments = group.payments || [];
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
        <FileText size={16} className="text-indigo-500"/>
        <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.paymentRows","Chi tiết thanh toán")}</h3>
      </div>
      <Table empty={!payments.length ? t("admin.noPaymentRows","Không có bản ghi thanh toán") : ""} minWidth="900px">
        <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-medium text-xs uppercase tracking-wider">
          <tr>
            <th className="px-5 py-4">ID</th>
            <th className="px-5 py-4">{t("admin.orderCode","Mã đơn")}</th>
            <th className="px-5 py-4">{t("common.method","Phương thức")}</th>
            <th className="px-5 py-4">{t("common.status","Trạng thái")}</th>
            <th className="px-5 py-4">{t("common.amount","Số tiền")}</th>
            <th className="px-5 py-4">{t("admin.transactionId","Transaction ID")}</th>
            <th className="px-5 py-4">{t("orders.paidAt","Thanh toán lúc")}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
          {payments.map((payment, index) => (
            <motion.tr key={payment.id}
              initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }}
              transition={{ delay: index * 0.04, duration: 0.3 }}
              className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
              <td className="px-5 py-4 font-mono text-xs font-semibold text-slate-700 dark:text-slate-300">{payment.id}</td>
              <td className="px-5 py-4">
                <Link className="font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
                  to={`/admin/orders?keyword=${encodeURIComponent(payment.orderCode || payment.orderId || "")}`}>
                  {payment.orderCode || `#${payment.orderId || "—"}`}
                </Link>
                <p className="text-xs text-slate-400 mt-0.5">#{payment.orderId || "—"}</p>
              </td>
              <td className="px-5 py-4 text-sm text-slate-600 dark:text-slate-300">{payment.method || "—"}</td>
              <td className="px-5 py-4"><StatusPill status={payment.status} type="payment"/></td>
              <td className="px-5 py-4 font-semibold text-slate-900 dark:text-slate-100">{formatVND(payment.amount, language)}</td>
              <td className="px-5 py-4 text-xs font-mono text-slate-500 dark:text-slate-400">{payment.transactionId || "—"}</td>
              <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(payment.paidAt, language)}</td>
            </motion.tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}

function RelatedOrders({ group, language, t }) {
  const orders = (group.orders || []).map(normalizeOrder);
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
        <ExternalLink size={16} className="text-indigo-500"/>
        <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.relatedOrders","Đơn hàng liên quan")}</h3>
      </div>
      <Table empty={!orders.length ? t("admin.noRelatedOrders","Không có đơn hàng liên quan") : ""} minWidth="900px">
        <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-medium text-xs uppercase tracking-wider">
          <tr>
            <th className="px-5 py-4">{t("admin.orderCode","Mã đơn")}</th>
            <th className="px-5 py-4">{t("orders.status","Trạng thái đơn")}</th>
            <th className="px-5 py-4">{t("orders.paymentStatus","Trạng thái TT")}</th>
            <th className="px-5 py-4">{t("common.total","Tổng tiền")}</th>
            <th className="px-5 py-4">{t("orders.createdAt","Tạo lúc")}</th>
            <th className="px-5 py-4">{t("admin.actions","Hành động")}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
          {orders.map((order, index) => (
            <motion.tr key={order.id || order.orderCode}
              initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }}
              transition={{ delay: index * 0.04, duration: 0.3 }}
              className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
              <td className="px-5 py-4 font-mono font-semibold text-slate-900 dark:text-slate-100">{order.orderCode || "—"}</td>
              <td className="px-5 py-4 text-sm text-slate-600 dark:text-slate-300">{order.orderStatus || "—"}</td>
              <td className="px-5 py-4"><StatusPill status={order.paymentStatus} type="payment"/></td>
              <td className="px-5 py-4 font-semibold text-slate-900 dark:text-slate-100">{formatVND(order.totalAmount, language)}</td>
              <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(order.createdAt, language)}</td>
              <td className="px-5 py-4">
                <Link className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-1.5 text-xs font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  to={`/admin/orders?keyword=${encodeURIComponent(order.orderCode || "")}`}>
                  <ExternalLink size={11}/> {t("admin.openOrder","Xem đơn")}
                </Link>
              </td>
            </motion.tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}

function ReconcileResult({ language, result, t }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-emerald-200 dark:border-emerald-500/30 shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 border-b border-emerald-100 dark:border-emerald-500/20 bg-emerald-50/50 dark:bg-emerald-500/5 px-5 py-4">
        <RotateCcw size={16} className="text-emerald-600 dark:text-emerald-400"/>
        <h3 className="text-sm font-bold text-emerald-800 dark:text-emerald-300">{t("admin.reconcileResult","Kết quả đối soát")}</h3>
      </div>
      <div className="p-5">
        <div className="grid gap-5 xl:grid-cols-3">
          <InfoCard title={t("admin.localState","Trạng thái hệ thống")}>
            <Meta label={t("admin.paymentGroupCode","Mã nhóm")} value={result.paymentGroupCode || "—"}/>
            <Meta label={t("common.method","Phương thức")} value={result.method || "—"}/>
            <Meta label={t("admin.before","Trước")} value={result.localStatusBefore || "—"}/>
            <Meta label={t("admin.after","Sau")} value={result.localStatusAfter || "—"}/>
          </InfoCard>
          <InfoCard title={t("admin.providerState","Trạng thái provider")}>
            <Meta label={t("admin.providerTxnRef","Mã GD")} value={result.providerTxnRef || "—"}/>
            <Meta label={t("admin.providerStatus","Trạng thái provider")} value={result.providerStatus || "—"}/>
            <Meta label={t("admin.changed","Có thay đổi")} value={result.changed ? t("admin.changedYes","Có thay đổi") : t("admin.changedNo","Không thay đổi")}/>
            <Meta label={t("admin.checkedAt","Kiểm tra lúc")} value={formatDateTime(result.checkedAt, language)}/>
          </InfoCard>
          <InfoCard title={t("admin.reconcileMessage","Thông điệp")}>
            <p className="text-sm font-semibold leading-6 text-slate-700 dark:text-slate-300">{result.message || "—"}</p>
          </InfoCard>
        </div>
      </div>
    </div>
  );
}

/* ── Pure utils (100% preserved) ──────────────── */
function yesNo(value, t) { return value ? t("common.yes","Có") : t("common.no","Không"); }
