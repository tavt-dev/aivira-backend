import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { clearCart, getCart, removeCartItem, updateCartItem } from "../api/cartApi.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function CartPage({ onAuth }) {
  const [items, setItems] = useState([]);
  const [source, setSource] = useState("api");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;
    getCart()
      .then((cart) => {
        const mapped = (cart?.items || []).map(normalizeCartItem);
        setItems(mapped);
        setSource("api");
      })
      .catch((error) => setMessage(error.message || "Could not load backend cart."));
  }, []);

  function updateQuantity(item, quantity) {
    const next = items.map((candidate) => candidate.cartItemId === item.cartItemId ? { ...candidate, quantity } : candidate);
    setItems(next);
    updateCartItem(item.cartItemId, { quantity }).then((cart) => {
      const mapped = (cart?.items || []).map(normalizeCartItem);
      setItems(mapped);
      window.dispatchEvent(new Event("aivira-cart"));
    }).catch((error) => setMessage(error.message || "Update cart failed."));
  }

  function removeItem(item) {
    const next = items.filter((candidate) => candidate.cartItemId !== item.cartItemId);
    setItems(next);
    removeCartItem(item.cartItemId)
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || "Remove item failed."));
  }

  function clearAll() {
    setItems([]);
    clearCart()
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || "Clear cart failed."));
  }

  return (
    <div className="page-shell">
      <PageHeader title="Cart" eyebrow="Backend cart" />
      {message && <div className="notice page-notice">{message}</div>}
      {!getAccessToken() ? (
        <EmptyState title="Please login to view your backend cart" action={<button className="btn-fill" onClick={onAuth}>Login</button>} />
      ) : items.length === 0 ? (
        <EmptyState title="Your cart is empty" action={<Link className="btn-fill" to="/category/all">Browse books</Link>} />
      ) : (
        <div className="cart-layout">
          <div className="cart-list">
            {items.map((item) => (
              <div className="cart-item" key={item.cartItemId}>
                <img src={item.image} alt={item.title} />
                <div>
                  <Link to={`/product/${item.slug}`}>{item.title}</Link>
                  <span>{formatVND(item.price)}</span>
                </div>
                <input type="number" min="1" value={item.quantity} onChange={(e) => updateQuantity(item, Math.max(1, Number(e.target.value)))} />
                <button onClick={() => removeItem(item)}>Remove</button>
              </div>
            ))}
          </div>
          <div className="summary-box">
            <h3>Summary</h3>
            <div><span>Total</span><strong>{formatVND(cartTotal(items))}</strong></div>
            <Link className="btn-buy" to="/checkout">Checkout</Link>
            <button className="btn-line dark" onClick={clearAll}>Clear cart</button>
          </div>
        </div>
      )}
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}

function EmptyState({ title, action }) {
  return <div className="empty"><h3>{title}</h3>{action}</div>;
}
