import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";

import { getAdminPaymentGroup, reconcilePaymentGroup } from "../../api/adminPaymentsApi.js";
import {
  Button,
  InfoCard,
  Input,
  MetaRow as Meta,
  Notice,
  PageHeader,
  Panel,
  StatusPill,
  Table,
  useConfirm,
  useToast,
} from "../../components/ui/index.jsx";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeOrder, normalizePaymentGroup } from "../../utils/mappers.js";

const TERMINAL_STATUSES = new Set(["SUCCESS", "CANCELLED", "EXPIRED", "REFUNDED"]);

export default function AdminPaymentsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [code, setCode] = useState(searchParams.get("code") || "");
  const [message, setMessage] = useState("");
  const [group, setGroup] = useState(null);
  const [reconcileResult, setReconcileResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [reconciling, setReconciling] = useState(false);

  const lookup = useCallback(async (rawCode = code, options = {}) => {
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
  }, [code, searchParams, setSearchParams, t]);

  useEffect(() => {
    const nextCode = searchParams.get("code") || "";
    setCode(nextCode);
    if (nextCode) lookup(nextCode, { silent: true });
  }, [lookup, searchParams]);

  async function reconcile() {
    const normalizedCode = code.trim();
    if (!normalizedCode) {
      setMessage(t("admin.paymentCodeRequired"));
      return;
    }
    if (group?.status && TERMINAL_STATUSES.has(group.status)) {
      const confirmed = await confirm({
        title: t("admin.reconcile"),
        message: t("admin.confirmTerminalReconcile", { status: group.status }),
        confirmLabel: t("admin.reconcile"),
        cancelLabel: t("common.cancel"),
        danger: false,
      });
      if (!confirmed) return;
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
      toast({ message: t("admin.reconciled", {
        before: result.localStatusBefore || t("common.unknown"),
        after: result.localStatusAfter || t("common.unknown"),
      }), variant: "success" });
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
          <Button disabled={loading || reconciling || !code.trim()} loading={loading} type="button" variant="secondary" onClick={() => lookup()}>
            {loading ? t("common.loading") : t("admin.lookup")}
          </Button>
          <Button disabled={loading || reconciling || !code.trim()} loading={reconciling} type="button" onClick={reconcile}>
            {reconciling ? t("common.working") : t("admin.reconcile")}
          </Button>
          <Button disabled={loading || reconciling || !group} type="button" variant="secondary" onClick={() => lookup(code)}>
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
          <Meta label={t("common.status")} value={<StatusPill status={group.status} type="payment" />} />
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
      <Table empty={!payments.length ? t("admin.noPaymentRows") : ""} minWidth="900px">
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
                <td className="px-4 py-3"><StatusPill status={payment.status} type="payment" /></td>
                <td className="px-4 py-3 font-semibold">{formatVND(payment.amount, language)}</td>
                <td className="px-4 py-3 text-slate-600">{payment.transactionId || "-"}</td>
                <td className="px-4 py-3 text-slate-500">{formatDateTime(payment.paidAt, language)}</td>
              </tr>
            ))}
          </tbody>
      </Table>
    </Panel>
  );
}

function RelatedOrders({ group, language, t }) {
  const orders = (group.orders || []).map(normalizeOrder);
  return (
    <Panel title={t("admin.relatedOrders")}>
      <Table empty={!orders.length ? t("admin.noRelatedOrders") : ""} minWidth="900px">
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
                <td className="px-4 py-3"><StatusPill status={order.paymentStatus} type="payment" /></td>
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
      </Table>
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

function yesNo(value, t) {
  return value ? t("common.yes") : t("common.no");
}
