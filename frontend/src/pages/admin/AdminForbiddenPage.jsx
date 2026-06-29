import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowLeft, ShieldX } from "lucide-react";

export default function AdminForbiddenPage() {
  const { t } = useTranslation();
  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden bg-slate-50 px-4 py-12 dark:bg-slate-950">
      <div className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-indigo-500/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-violet-500/10 blur-3xl" />
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-8 shadow-xl dark:border-slate-800 dark:bg-slate-900">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400">
          <ShieldX size={24} />
        </div>
        <span className="mt-5 inline-flex rounded-full border border-rose-200/60 bg-rose-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-rose-600 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400">403</span>
        <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-950 dark:text-slate-50">{t("admin.forbiddenTitle")}</h1>
        <p className="mt-3 leading-7 text-slate-500 dark:text-slate-400">{t("admin.forbiddenCopy")}</p>
        <div className="mt-7 flex flex-wrap gap-3">
          <Link className="rounded-lg bg-indigo-600 px-5 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-indigo-700" to="/?auth=login&next=/admin/dashboard">
            {t("admin.loginAsAdmin")}
          </Link>
          <Link className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800" to="/">
            <ArrowLeft size={15} />
            {t("common.backToBookstore")}
          </Link>
        </div>
      </div>
    </div>
  );
}
