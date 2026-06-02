export default function AdminOrdersPendingPage() {
  return (
    <div className="grid gap-8">
      <PageHeader title="Admin Orders" eyebrow="Backend pending" />
      <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="font-serif text-3xl font-bold text-slate-950">
          Backend pending: /admin/orders/** is not implemented yet.
        </h3>
        <p className="mt-3 max-w-2xl text-slate-500">
          The frontend route is ready and can be wired when backend admin order lifecycle lands.
        </p>
      </section>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
        {eyebrow}
      </span>
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}
