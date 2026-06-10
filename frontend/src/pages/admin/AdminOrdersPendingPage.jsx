import { useTranslation } from "react-i18next";

import { PageHeader } from "../../components/ui/index.jsx";

export default function AdminOrdersPendingPage() {
  const { t } = useTranslation();
  return (
    <div className="grid gap-8">
      <PageHeader title={t("admin.ordersTitle")} eyebrow={t("admin.backendPending")} />
      <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="font-serif text-3xl font-bold text-slate-950">
          {t("admin.ordersPendingTitle")}
        </h3>
        <p className="mt-3 max-w-2xl text-slate-500">
          {t("admin.ordersPendingCopy")}
        </p>
      </section>
    </div>
  );
}
