import { useEffect, useState } from "react";
import { cancelOrder, getOrder, getOrders } from "../api/orderApi.js";
import { formatVND } from "../utils/formatters.js";
import { normalizeOrder, pageRows } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function OrdersPage({ onAuth }) {
  const [orders, setOrders] = useState([]);
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;
    getOrders().then((page) => {
      setOrders(pageRows(page).map(normalizeOrder));
    }).catch((error) => setMessage(error.message || "Could not load backend orders."));
  }, []);

  async function cancel(order) {
    try {
      const updated = await cancelOrder(order.id, "Cancelled from frontend");
      setOrders((current) => current.map((item) => item.id === order.id ? normalizeOrder(updated) : item));
      setMessage("Order cancelled.");
    } catch (error) {
      setMessage(error.message || "Cancel order failed.");
    }
  }

  async function viewDetail(order) {
    setMessage("");
    try {
      setSelected(await getOrder(order.id));
    } catch (error) {
      setMessage(error.message || "Order detail unavailable.");
    }
  }

  return (
    <div className="page-shell">
      <PageHeader title="Orders" eyebrow="Backend orders" />
      {message && <div className="notice page-notice">{message}</div>}
      {!getAccessToken() && <div className="notice">Login to view backend orders. <button onClick={onAuth}>Login</button></div>}
      {orders.length === 0 ? <EmptyState title="No orders yet" /> : (
        <div className="table-card">
          {orders.map((order) => (
            <div className="table-row" key={order.id}>
              <span>{order.orderCode || order.id}</span>
              <strong>{order.orderStatus}</strong>
              <span>{formatVND(order.totalAmount || 0)}</span>
              <button onClick={() => viewDetail(order)}>Detail</button>
              <button onClick={() => cancel(order)}>Cancel</button>
            </div>
          ))}
        </div>
      )}
      {selected && (
        <div className="panel detail-panel">
          <button className="modal-x light-x" onClick={() => setSelected(null)}>x</button>
          <h3>{selected.orderCode || selected.id}</h3>
          <p>Status: {selected.orderStatus}</p>
          <p>Payment: {selected.paymentMethod || "-"} / {selected.paymentStatus || "-"}</p>
          <p>Shipping: {selected.shippingRecipientName || selected.recipientName || "-"} - {selected.shippingAddressLine || ""}</p>
          {(selected.items || []).map((item) => (
            <div className="mini-row" key={item.id || item.productId || item.productName}>
              <span>{item.productName || item.title} x {item.quantity}</span>
              <small>{formatVND(item.totalPrice || item.price || item.finalPrice || 0)}</small>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}

function EmptyState({ title }) {
  return <div className="empty"><h3>{title}</h3></div>;
}
