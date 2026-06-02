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
      setMessage(
        `Reconciled: ${result.localStatusBefore || "unknown"} -> ${
          result.localStatusAfter || "unknown"
        }`
      );
    } catch (error) {
      setMessage(error.message || "Backend pending: reconcile unavailable.");
    }
  }

  return (
    <div className="grid gap-8">
      <PageHeader title="Admin Payments" eyebrow="Payment group reconciliation" />
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
        <div className="grid gap-4 md:grid-cols-[1fr_auto_auto]">
          <input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="Payment group code"
            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          />
          <Button secondary type="button" onClick={lookup}>Lookup</Button>
          <Button type="button" onClick={reconcile}>Reconcile</Button>
        </div>
        {group && (
          <Notice>
            Status: {group.status || group.paymentStatus || "unknown"} - Total:{" "}
            {formatVND(group.totalAmount || 0)}
          </Notice>
        )}
        {message && <Notice>{message}</Notice>}
      </section>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="border-b border-slate-200 pb-6"><span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span><h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2></div>;
}
function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}
function Notice({ children }) {
  return <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
