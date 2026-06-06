import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";

import { getAdminPaymentGroup, reconcilePaymentGroup } from "../../api/adminPaymentsApi.js";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeOrder, normalizePaymentGroup } from "../../utils/mappers.js";

const TERMINAL_STATUSES = new Set(["SUCCESS", "CANCELLED", "EXPIRED", "REFUNDED"]);

export default function AdminPaymentsPage() {
  const { t, i18n } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [code, setCode] = useState(searchParams.get("code") || "");
  const [message, setMessage] = useState("");
  const [group, setGroup] = useState(null);
  const [reconcileResult, setReconcileResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [reconciling, setReconciling] = useState(false);

  useEffect(() => {
    const nextCode = searchParams.get("code") || "";
    setCode(nextCode);
    if (nextCode) lookup(nextCode, { silent: true });
  }, [searchParams]);

  async function lookup(rawCode = code, options = {}) {
    const normalizedCode = rawCode.trim();
    if (!normalizedCode) {
      setMessage(t("admin.paymentCodeRequired"));
      return;
    }
    setLoading(true);
    if (!options.silent) setMessage("");
    try {
      const result = normalizePaymentGroup(await getAdminPaymentGroup(normalizedCode));
      setGroup(result);
      setReconcileResult(null);
      setCode(normalizedCode);
      if (searchParams.get("code") !== normalizedCode) {
        setSearchParams({ code: normalizedCode });
      }
      if (!options.silent) setMessage(t("admin.paymentLoaded"));
    } catch (error) {
      setGroup(null);
      setReconcileResult(null);
      setMessage(error.message || t("admin.paymentUnavailable"));
    } finally {
      setLoading(false);
    }
  }

  async function reconcile() {
    const normalizedCode = code.trim();
    if (!normalizedCode) {
      setMessage(t("admin.paymentCodeRequired"));
      return;
    }
    if (group?.status && TERMINAL_STATUSES.has(group.status) && !window.confirm(t("admin.confirmTerminalReconcile", { status: group.status }))) {
      return;
    }
    setReconciling(true);
    setMessage("");
    try {
      const result = await reconcilePaymentGroup(normalizedCode);
      setReconcileResult(result);
      setMessage(
        t("admin.reconciled", {
          before: result.localStatusBefore || t("common.unknown"),
          after: result.localStatusAfter || t("common.unknown"),
        })
      );
      await lookup(normalizedCode, { silent: true });
      setReconcileResult(result);
    } catch (error) {
      setMessage(error.message || t("admin.errors.reconcile"));
    } finally {
      setReconciling(false);
    }
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.paymentsTitle")} eyebrow={t("admin.paymentsEyebrow")} />

      <Panel title={t("admin.paymentLookup")}>
        <div className="grid gap-3 md:grid-cols-[1fr_auto_auto_auto]">
          <Input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder={t("admin.paymentGroupCode")}
            onKeyDown={(event) => {
              if (event.key === "Enter") lookup();
            }}
          />
          <Button disabled={loading || reconciling || !code.trim()} secondary type="button" onClick={() => lookup()}>
            {loading ? t("common.loading") : t("admin.lookup")}
          </Button>
          <Button disabled={loading || reconciling || !code.trim()} type="button" onClick={reconcile}>
            {reconciling ? t("common.working") : t("admin.reconcile")}
          </Button>
          <Button disabled={loading || reconciling || !group} secondary type="button" onClick={() => lookup(code)}>
            {t("admin.refresh")}
          </Button>
        </div>
        {message && <Notice>{message}</Notice>}
      </Panel>

      {group ? (
        <>
          <PaymentGroupSummary group={group} language={i18n.language} t={t} />
          <PaymentsTable group={group} language={i18n.language} t={t} />
          <RelatedOrders group={group} language={i18n.language} t={t} />
          {reconcileResult && <ReconcileResult language={i18n.language} result={reconcileResult} t={t} />}
        </>
      ) : (
        <Panel title={t("admin.paymentGroupDetail")}>
          <p className="text-sm text-slate-500">{loading ? t("common.loading") : t("admin.paymentEmptyState")}</p>
        </Panel>
      )}
    </div>
  );
}

function PaymentGroupSummary({ group, language, t }) {
  return (
    <Panel title={t("admin.paymentGroupDetail")}>
      <div className="grid gap-4 xl:grid-cols-3">
        <InfoCard title={t("admin.groupStatusTitle")}>
          <Meta label={t("admin.paymentGroupCode")} value={group.paymentCode || group.paymentGroupCode || "-"} />
          <Meta label={t("common.method")} value={group.method || "-"} />
          <Meta label={t("common.status")} value={<PaymentBadge status={group.status} />} />
          <Meta label={t("common.amount")} value={formatVND(group.amount, language)} />
        </InfoCard>
        <InfoCard title={t("admin.providerData")}>
          <Meta label={t("admin.providerTxnRef")} value={group.providerTxnRef || "-"} />
          <Meta label={t("admin.providerTransactionId")} value={group.providerTransactionId || "-"} />
          <Meta label={t("orders.paidAt")} value={formatDateTime(group.paidAt, language)} />
          <Meta label={t("admin.expiresAt")} value={formatDateTime(group.expiresAt, language)} />
        </InfoCard>
        <InfoCard title={t("admin.paymentProviderLinks")}>
          <Meta label={t("admin.hasPaymentUrl")} value={yesNo(Boolean(group.paymentUrl), t)} />
          <Meta label={t("admin.hasDeeplink")} value={yesNo(Boolean(group.deeplink), t)} />
          <Meta label={t("admin.hasQrCode")} value={yesNo(Boolean(group.qrCodeUrl), t)} />
          <p className="text-xs font-semibold text-slate-500">{t("admin.providerLinkNote")}</p>
        </InfoCard>
      </div>
    </Panel>
  );
}

function PaymentsTable({ group, language, t }) {
  const payments = group.payments || [];
  return (
    <Panel title={t("admin.paymentRows")}>
      <div className="overflow-x-auto rounded-xl border border-slate-200">
        <table className="min-w-[900px] w-full border-collapse text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">{t("admin.orderCode")}</th>
              <th className="px-4 py-3">{t("common.method")}</th>
              <th className="px-4 py-3">{t("common.status")}</th>
              <th className="px-4 py-3">{t("common.amount")}</th>
              <th className="px-4 py-3">{t("admin.transactionId")}</th>
              <th className="px-4 py-3">{t("orders.paidAt")}</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((payment) => (
              <tr className="border-t border-slate-100" key={payment.id}>
                <td className="px-4 py-3 font-bold text-slate-950">{payment.id}</td>
                <td className="px-4 py-3">
                  <Link className="font-bold text-blue-600 hover:underline" to={`/admin/orders?keyword=${encodeURIComponent(payment.orderCode || payment.orderId || "")}`}>
                    {payment.orderCode || `#${payment.orderId || "-"}`}
                  </Link>
                  <p className="text-xs text-slate-500">#{payment.orderId || "-"}</p>
                </td>
                <td className="px-4 py-3">{payment.method || "-"}</td>
                <td className="px-4 py-3"><PaymentBadge status={payment.status} /></td>
                <td className="px-4 py-3 font-semibold">{formatVND(payment.amount, language)}</td>
                <td className="px-4 py-3 text-slate-600">{payment.transactionId || "-"}</td>
                <td className="px-4 py-3 text-slate-500">{formatDateTime(payment.paidAt, language)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!payments.length && <div className="p-5 text-sm text-slate-500">{t("admin.noPaymentRows")}</div>}
      </div>
    </Panel>
  );
}

function RelatedOrders({ group, language, t }) {
  const orders = (group.orders || []).map(normalizeOrder);
  return (
    <Panel title={t("admin.relatedOrders")}>
      <div className="overflow-x-auto rounded-xl border border-slate-200">
        <table className="min-w-[900px] w-full border-collapse text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">{t("admin.orderCode")}</th>
              <th className="px-4 py-3">{t("orders.status")}</th>
              <th className="px-4 py-3">{t("orders.paymentStatus")}</th>
              <th className="px-4 py-3">{t("common.total")}</th>
              <th className="px-4 py-3">{t("orders.createdAt")}</th>
              <th className="px-4 py-3">{t("admin.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr className="border-t border-slate-100" key={order.id || order.orderCode}>
                <td className="px-4 py-3 font-bold text-slate-950">{order.orderCode || "-"}</td>
                <td className="px-4 py-3">{order.orderStatus || "-"}</td>
                <td className="px-4 py-3"><PaymentBadge status={order.paymentStatus} /></td>
                <td className="px-4 py-3 font-semibold">{formatVND(order.totalAmount, language)}</td>
                <td className="px-4 py-3 text-slate-500">{formatDateTime(order.createdAt, language)}</td>
                <td className="px-4 py-3">
                  <Link className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-50" to={`/admin/orders?keyword=${encodeURIComponent(order.orderCode || "")}`}>
                    {t("admin.openOrder")}
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!orders.length && <div className="p-5 text-sm text-slate-500">{t("admin.noRelatedOrders")}</div>}
      </div>
    </Panel>
  );
}

function ReconcileResult({ language, result, t }) {
  return (
    <Panel title={t("admin.reconcileResult")}>
      <div className="grid gap-4 xl:grid-cols-3">
        <InfoCard title={t("admin.localState")}>
          <Meta label={t("admin.paymentGroupCode")} value={result.paymentGroupCode || "-"} />
          <Meta label={t("common.method")} value={result.method || "-"} />
          <Meta label={t("admin.before")} value={result.localStatusBefore || "-"} />
          <Meta label={t("admin.after")} value={result.localStatusAfter || "-"} />
        </InfoCard>
        <InfoCard title={t("admin.providerState")}>
          <Meta label={t("admin.providerTxnRef")} value={result.providerTxnRef || "-"} />
          <Meta label={t("admin.providerStatus")} value={result.providerStatus || "-"} />
          <Meta label={t("admin.changed")} value={result.changed ? t("admin.changedYes") : t("admin.changedNo")} />
          <Meta label={t("admin.checkedAt")} value={formatDateTime(result.checkedAt, language)} />
        </InfoCard>
        <InfoCard title={t("admin.reconcileMessage")}>
          <p className="text-sm font-semibold leading-6 text-slate-700">{result.message || "-"}</p>
        </InfoCard>
      </div>
    </Panel>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span>
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <h3 className="mb-5 text-xl font-bold text-slate-950">{title}</h3>
      {children}
    </section>
  );
}

function InfoCard({ children, title }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h4 className="mb-4 text-lg font-bold text-slate-950">{title}</h4>
      <div className="grid gap-2">{children}</div>
    </section>
  );
}

function Meta({ label, value }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="max-w-[70%] text-right font-semibold text-slate-700">{value}</span>
    </div>
  );
}

function PaymentBadge({ status }) {
  return <span className="inline-flex rounded-full bg-blue-50 px-2 py-1 text-xs font-bold text-blue-700">{status || "-"}</span>;
}

function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-50" />;
}

function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}

function Notice({ children }) {
  return <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}

function yesNo(value, t) {
  return value ? t("common.yes") : t("common.no");
}
