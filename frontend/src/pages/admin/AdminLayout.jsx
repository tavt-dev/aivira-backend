import { NavLink, Outlet } from "react-router-dom";

export default function AdminLayout() {
  return (
    <div className="admin-page">
      <aside className="admin-side">
        <h2>AIVIRA ADMIN</h2>
        <NavLink to="/admin/products">Products</NavLink>
        <NavLink to="/admin/categories">Categories</NavLink>
        <NavLink to="/admin/payments">Payments</NavLink>
        <NavLink to="/admin/permissions">Permissions</NavLink>
        <NavLink to="/admin/orders-pending">Orders</NavLink>
      </aside>
      <section className="admin-main">
        <Outlet />
      </section>
    </div>
  );
}
