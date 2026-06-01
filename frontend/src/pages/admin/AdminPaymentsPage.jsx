import { useState } from "react";
import { reconcilePaymentGroup } from "../../api/adminApi.js";
import { getPaymentGroup } from "../../api/paymentApi.js";
import { formatVND } from "../../utils/formatters.js";

export default function AdminPaymentsPage() {
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("");
  const [group, setGroup] = useState(null);

  async function lookup() {
    setMessage("");
    try {
      const result = await getPaymentGroup(code);
      setGroup(result);
      setMessage("Payment group loaded.");
    } catch (error) {
      setMessage(error.message || "Payment group unavailable.");
    }
  }

  async function reconcile() {
    setMessage("");
    try {
      const result = await reconcilePaymentGroup(code);
      setMessage(`Reconciled: ${result.localStatusBefore || "unknown"} -> ${result.localStatusAfter || "unknown"}`);
    } catch (error) {
      setMessage(error.message || "Backend pending: reconcile unavailable.");
    }
  }

  return (
    <>
      <PageHeader title="Admin Payments" eyebrow="Payment group reconciliation" />
      <div className="panel form-panel">
        <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Payment group code" />
        <div className="button-row">
          <button className="btn-line dark" type="button" onClick={lookup}>Lookup</button>
          <button className="btn-fill" type="button" onClick={reconcile}>Reconcile</button>
        </div>
        {group && <div className="notice">Status: {group.status || group.paymentStatus || "unknown"} - Total: {formatVND(group.totalAmount || 0)}</div>}
        {message && <div className="notice">{message}</div>}
      </div>
    </>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
