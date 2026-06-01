export default function AdminOrdersPendingPage() {
  return (
    <>
      <PageHeader title="Admin Orders" eyebrow="Backend pending" />
      <div className="panel">
        <h3>Backend pending: /admin/orders/** is not implemented yet.</h3>
        <p>The frontend route is ready and can be wired when backend admin order lifecycle lands.</p>
      </div>
    </>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
