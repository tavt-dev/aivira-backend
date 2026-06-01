import { useEffect, useState } from "react";
import { getCart } from "../api/cartApi.js";
import { checkout, createAddress, getAddresses } from "../api/orderApi.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function CheckoutPage({ onAuth }) {
  const [items, setItems] = useState([]);
  const [cartSource, setCartSource] = useState("api");
  const [addresses, setAddresses] = useState([]);
  const [form, setForm] = useState({
    recipientName: "",
    phoneNumber: "",
    addressLine: "",
    ward: "",
    district: "",
    city: "",
    paymentMethod: "COD",
    notes: ""
  });
  const [selectedAddress, setSelectedAddress] = useState("");
  const [message, setMessage] = useState("");
  const [paymentUrl, setPaymentUrl] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;
    getAddresses().then((rows) => {
      setAddresses(rows || []);
    }).catch((error) => setMessage(error.message || "Could not load addresses."));
    getCart().then((cart) => {
      const mapped = (cart?.items || []).map(normalizeCartItem);
      setItems(mapped);
      setCartSource("api");
    }).catch((error) => {
      setCartSource("error");
      setMessage(error.message || "Could not load backend cart.");
    });
  }, []);

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    setPaymentUrl("");
    let addressId = selectedAddress;

    if (!addressId && form.recipientName && form.phoneNumber && form.addressLine) {
      try {
        const created = await createAddress({ ...form, defaultAddress: addresses.length === 0 });
        addressId = created.id;
        const next = [created, ...addresses];
        setAddresses(next);
      } catch (error) {
        setMessage(error.message || "Create address failed.");
        return;
      }
    }

    if (!addressId) {
      setMessage("Please choose or create a shipping address.");
      return;
    }

    try {
      if (getAccessToken() && cartSource === "api") {
        if (items.length === 0) {
          setMessage("Backend cart is empty.");
          return;
        }
        const cartItemIds = items.map((item) => Number(item.cartItemId)).filter(Boolean);
        if (cartItemIds.length === 0) {
          setMessage("No backend cart item IDs available. Please refresh cart from backend.");
          return;
        }
        const response = await checkout({
          addressId: Number(addressId),
          cartItemIds,
          paymentMethod: form.paymentMethod,
          notes: form.notes
        });
        const url = response?.paymentUrl || response?.deeplink || response?.qrCodeUrl;
        if (url) setPaymentUrl(url);
        setMessage(`Checkout created: ${response?.paymentGroupCode || response?.paymentGroup?.paymentGroupCode || "payment group pending"}`);
        if (form.paymentMethod === "COD") window.dispatchEvent(new Event("aivira-cart"));
      } else {
        setMessage(getAccessToken() ? "Backend cart unavailable. Checkout requires backend cart." : "Login required for backend checkout.");
      }
    } catch (error) {
      setMessage(error.message || "Checkout failed. Backend pending or unavailable.");
    }
  }

  return (
    <div className="page-shell">
      <PageHeader title="Checkout" eyebrow="Address, payment, and order summary" />
      {!getAccessToken() && <div className="notice">Login required for checkout. <button onClick={onAuth}>Login</button></div>}
      {getAccessToken() && cartSource === "error" && <div className="notice">Backend cart unavailable. Checkout is disabled until the API responds.</div>}
      <form className="checkout-layout" onSubmit={submit}>
        <div className="form-panel">
          <h3>Shipping Address</h3>
          {addresses.length > 0 && (
            <select value={selectedAddress} onChange={(e) => setSelectedAddress(e.target.value)}>
              <option value="">Create new address</option>
              {addresses.map((address) => <option key={address.id} value={address.id}>{address.recipientName} - {address.addressLine}</option>)}
            </select>
          )}
          <div className="form-grid">
            <input placeholder="Recipient name" value={form.recipientName} onChange={(e) => setForm({ ...form, recipientName: e.target.value })} />
            <input placeholder="Phone number" value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
          </div>
          <input placeholder="Address line" value={form.addressLine} onChange={(e) => setForm({ ...form, addressLine: e.target.value })} />
          <div className="form-grid">
            <input placeholder="Ward" value={form.ward} onChange={(e) => setForm({ ...form, ward: e.target.value })} />
            <input placeholder="District" value={form.district} onChange={(e) => setForm({ ...form, district: e.target.value })} />
            <input placeholder="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          </div>
          <h3>Payment</h3>
          <select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
            <option value="COD">COD</option>
            <option value="VNPAY">VNPay</option>
            <option value="MOMO">MoMo</option>
          </select>
          <textarea placeholder="Notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
        </div>
        <div className="summary-box">
          <h3>Order Summary</h3>
          {items.map((item) => <div key={item.cartItemId}><span>{item.title} x {item.quantity}</span><strong>{formatVND(item.price * item.quantity)}</strong></div>)}
          <div><span>Total</span><strong>{formatVND(cartTotal(items))}</strong></div>
          <button className="btn-buy" type="submit">Place order</button>
          {message && <div className="notice">{message}</div>}
          {paymentUrl && <a className="btn-fill" href={paymentUrl}>Continue to payment</a>}
        </div>
      </form>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
