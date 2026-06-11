import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import { getPaymentGroup, retryPayment } from "../api/paymentApi.js";
import { Button, MetaRow, Notice, PageHeader } from "../components/ui/index.jsx";
import { formatVND } from "../utils/formatters.js";
import { normalizePaymentGroup } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function PaymentResultPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const code =
    params.get("paymentGroupCode") || params.get("vnp_TxnRef") || params.get("orderId") || "";
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!code || !getAccessToken()) {
      setMessage(code ? t("payment.loginLookup") : "");
      return;
    }

    getPaymentGroup(code)
      .then((data) => setResult(normalizePaymentGroup(data)))
      .catch(() =>
        setMessage(t("payment.lookupUnavailable"))
      );
  }, [code, t]);

  async function retry() {
    setMessage("");
    try {
      const response = await retryPayment(code);
      const normalized = normalizePaymentGroup(response);
      setResult(normalized);
      const url = normalized.paymentUrl || normalized.deeplink || normalized.qrCodeUrl;
      if (url) window.location.href = url;
      else setMessage(t("payment.retryPending"));
    } catch (error) {
      setMessage(error.message || t("payment.retryFailed"));
    }
  }

  const paymentUrl = result?.paymentUrl || result?.deeplink || result?.qrCodeUrl;

  return (
    <div className="mx-auto w-full max-w-4xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("payment.title")} eyebrow={t("payment.eyebrow")} />
      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <div className="grid gap-4 text-slate-600">
          <MetaRow label={t("common.reference")} value={code || t("payment.noCode")} />
          <MetaRow label={t("common.status")} value={result?.status || t("payment.pending")} />
          <MetaRow label={t("common.method")} value={result?.method || "-"} />
          <MetaRow label={t("common.amount")} value={formatVND(result?.totalAmount || 0)} />
        </div>
        <div className="mt-8 flex flex-wrap gap-3">
          {paymentUrl && (
            <a
              className="rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              href={paymentUrl}
            >
              {t("payment.continue")}
            </a>
          )}
          {code && getAccessToken() && (
            <Button variant="secondary" type="button" onClick={retry}>
              {t("payment.retry")}
            </Button>
          )}
        </div>
        {message && <Notice className="mt-6">{message}</Notice>}
      </div>
    </div>
  );
}

