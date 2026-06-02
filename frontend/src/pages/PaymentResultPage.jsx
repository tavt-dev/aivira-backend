import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

import { getPaymentGroup, retryPayment } from "../api/paymentApi.js";
import { formatVND } from "../utils/formatters.js";
import { normalizePaymentGroup } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function PaymentResultPage() {
  const [params] = useSearchParams();
  const code =
    params.get("paymentGroupCode") || params.get("vnp_TxnRef") || params.get("orderId") || "";
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!code || !getAccessToken()) {
      setMessage(code ? "Login with a backend session to lookup payment group status." : "");
      return;
    }

    getPaymentGroup(code)
      .then((data) => setResult(normalizePaymentGroup(data)))
      .catch(() =>
        setMessage("Payment group lookup unavailable. Status is pending or backend is not reachable.")
      );
  }, [code]);

  async function retry() {
    setMessage("");
    try {
      const response = await retryPayment(code);
      const normalized = normalizePaymentGroup(response);
      setResult(normalized);
      const url = normalized.paymentUrl || normalized.deeplink || normalized.qrCodeUrl;
      if (url) window.location.href = url;
      else setMessage("Retry requested. Payment URL is pending from backend.");
    } catch (error) {
      setMessage(error.message || "Payment retry failed.");
    }
  }

  const paymentUrl = result?.paymentUrl || result?.deeplink || result?.qrCodeUrl;

  return (
    <div className="mx-auto w-full max-w-4xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title="Payment Result" eyebrow="Provider callback and payment status" />
      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <div className="grid gap-4 text-slate-600">
          <Info label="Reference" value={code || "No payment code in URL"} />
          <Info label="Status" value={result?.status || "Pending / unavailable"} />
          <Info label="Method" value={result?.method || "-"} />
          <Info label="Amount" value={formatVND(result?.totalAmount || 0)} />
        </div>
        <div className="mt-8 flex flex-wrap gap-3">
          {paymentUrl && (
            <a
              className="rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              href={paymentUrl}
            >
              Continue payment
            </a>
          )}
          {code && getAccessToken() && (
            <button
              className="rounded-full border border-slate-200 px-6 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              type="button"
              onClick={retry}
            >
              Retry payment
            </button>
          )}
        </div>
        {message && (
          <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
            {message}
          </div>
        )}
      </div>
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

function Info({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 p-4">
      <span className="font-semibold text-slate-500">{label}</span>
      <strong className="text-right text-slate-950">{value}</strong>
    </div>
  );
}
