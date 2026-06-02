import { Link } from "react-router-dom";

export default function AdminForbiddenPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-950 px-4 py-12">
      <div className="w-full max-w-lg rounded-3xl border border-white/10 bg-white p-8 shadow-2xl">
        <span className="inline-flex rounded-full bg-red-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-red-600">403</span>
        <h1 className="mt-4 font-serif text-4xl font-bold text-slate-950">Admin access denied</h1>
        <p className="mt-3 text-slate-500">This dashboard is only available to accounts with the ADMIN role.</p>
        <div className="mt-7 flex flex-wrap gap-3">
          <Link className="rounded-full bg-blue-600 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-500" to="/?auth=login&next=/admin/products">
            Login as admin
          </Link>
          <Link className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50" to="/">
            Back to bookstore
          </Link>
        </div>
      </div>
    </div>
  );
}
