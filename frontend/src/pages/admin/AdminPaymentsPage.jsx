import { useState } from "react";
import { useTranslation } from "react-i18next";

import { reconcilePaymentGroup } from "../../api/adminApi.js";
import { getPaymentGroup } from "../../api/paymentApi.js";
import { formatVND } from "../../utils/formatters.js";

export default function AdminPaymentsPage() {
  const { t } = useTranslation();
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("");
  const [group, setGroup] = useState(null);

  async function lookup() {
    setMessage("");
    try {
      const result = await getPaymentGroup(code);
      setGroup(result);
      setMessage(t("admin.paymentLoaded"));
    } catch (error) {
      setMessage(error.message || t("admin.paymentUnavailable"));
    }
  }

  async function reconcile() {
    setMessage("");
    try {
      const result = await reconcilePaymentGroup(code);
      setMessage(
        t("admin.reconciled", {
          before: result.localStatusBefore || t("common.unknown"),
          after: result.localStatusAfter || t("common.unknown"),
        })
      );
    } catch (error) {
      setMessage(error.message || t("admin.errors.reconcile"));
    }
  }

  return (
    <div className="grid gap-8">
      <PageHeader title={t("admin.paymentsTitle")} eyebrow={t("admin.paymentsEyebrow")} />
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
        <div className="grid gap-4 md:grid-cols-[1fr_auto_auto]">
          <input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder={t("admin.paymentGroupCode")}
            className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          />
          <Button secondary type="button" onClick={lookup}>{t("admin.lookup")}</Button>
          <Button type="button" onClick={reconcile}>{t("admin.reconcile")}</Button>
        </div>
        {group && (
          <Notice>
            {t("admin.groupStatus", {
              status: group.status || group.paymentStatus || t("common.unknown"),
              total: formatVND(group.totalAmount || 0),
            })}
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
