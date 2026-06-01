import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { getPaymentGroup, retryPayment } from "../api/paymentApi.js";
import { formatVND } from "../utils/formatters.js";
import { normalizePaymentGroup } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function PaymentResultPage() {
  const [params] = useSearchParams();
  const code = params.get("paymentGroupCode") || params.get("vnp_TxnRef") || params.get("orderId") || "";
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!code || !getAccessToken()) {
      setMessage(code ? "Login with a backend session to lookup payment group status." : "");
      return;
    }
    getPaymentGroup(code).then((data) => setResult(normalizePaymentGroup(data))).catch(() => setMessage("Payment group lookup unavailable. Status is pending or backend is not reachable."));
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

  return (
    <div className="page-shell">
      <PageHeader title="Payment Result" eyebrow="Provider callback and payment status" />
      <div className="panel">
        <p>Reference: {code || "No payment code in URL"}</p>
        <p>Status: {result?.status || "Pending / unavailable"}</p>
        <p>Method: {result?.method || "-"}</p>
        <p>Amount: {formatVND(result?.totalAmount || 0)}</p>
        {(result?.paymentUrl || result?.deeplink || result?.qrCodeUrl) && <a className="btn-fill" href={result.paymentUrl || result.deeplink || result.qrCodeUrl}>Continue payment</a>}
        {code && getAccessToken() && <button className="btn-fill" onClick={retry}>Retry payment</button>}
        {message && <div className="notice">{message}</div>}
      </div>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
