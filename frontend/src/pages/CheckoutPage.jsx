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
    notes: "",
  });
  const [selectedAddress, setSelectedAddress] = useState("");
  const [message, setMessage] = useState("");
  const [paymentUrl, setPaymentUrl] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;

    getAddresses()
      .then((rows) => setAddresses(rows || []))
      .catch((error) => setMessage(error.message || "Could not load addresses."));

    getCart()
      .then((cart) => {
        setItems((cart?.items || []).map(normalizeCartItem));
        setCartSource("api");
      })
      .catch((error) => {
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
        const created = await createAddress({
          ...form,
          defaultAddress: addresses.length === 0,
        });
        addressId = created.id;
        setAddresses([created, ...addresses]);
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
          notes: form.notes,
        });
        const url = response?.paymentUrl || response?.deeplink || response?.qrCodeUrl;
        if (url) setPaymentUrl(url);
        setMessage(
          `Checkout created: ${
            response?.paymentGroupCode ||
            response?.paymentGroup?.paymentGroupCode ||
            "payment group pending"
          }`
        );
        if (form.paymentMethod === "COD") window.dispatchEvent(new Event("aivira-cart"));
      } else {
        setMessage(
          getAccessToken()
            ? "Backend cart unavailable. Checkout requires backend cart."
            : "Login required for backend checkout."
        );
      }
    } catch (error) {
      setMessage(error.message || "Checkout failed. Backend pending or unavailable.");
    }
  }

  const loggedIn = Boolean(getAccessToken());

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title="Checkout" eyebrow="Address, payment, and order summary" />

      {!loggedIn && (
        <Notice>
          Login required for checkout.{" "}
          <button className="font-bold text-blue-700 underline" type="button" onClick={onAuth}>
            Login
          </button>
        </Notice>
      )}
      {loggedIn && cartSource === "error" && (
        <Notice>Backend cart unavailable. Checkout is disabled until the API responds.</Notice>
      )}

      <form className="grid gap-8 lg:grid-cols-[1fr_380px]" onSubmit={submit}>
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
          <h2 className="font-serif text-3xl font-bold text-slate-950">Shipping Address</h2>

          {addresses.length > 0 && (
            <select
              value={selectedAddress}
              onChange={(event) => setSelectedAddress(event.target.value)}
              className="mt-6 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
            >
              <option value="">Create new address</option>
              {addresses.map((address) => (
                <option key={address.id} value={address.id}>
                  {address.recipientName} - {address.addressLine}
                </option>
              ))}
            </select>
          )}

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <Input
              placeholder="Recipient name"
              value={form.recipientName}
              onChange={(event) => setForm({ ...form, recipientName: event.target.value })}
            />
            <Input
              placeholder="Phone number"
              value={form.phoneNumber}
              onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })}
            />
          </div>
          <Input
            className="mt-4"
            placeholder="Address line"
            value={form.addressLine}
            onChange={(event) => setForm({ ...form, addressLine: event.target.value })}
          />
          <div className="mt-4 grid gap-4 md:grid-cols-3">
            <Input
              placeholder="Ward"
              value={form.ward}
              onChange={(event) => setForm({ ...form, ward: event.target.value })}
            />
            <Input
              placeholder="District"
              value={form.district}
              onChange={(event) => setForm({ ...form, district: event.target.value })}
            />
            <Input
              placeholder="City"
              value={form.city}
              onChange={(event) => setForm({ ...form, city: event.target.value })}
            />
          </div>

          <h2 className="mt-10 font-serif text-3xl font-bold text-slate-950">Payment</h2>
          <select
            value={form.paymentMethod}
            onChange={(event) => setForm({ ...form, paymentMethod: event.target.value })}
            className="mt-6 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          >
            <option value="COD">COD</option>
            <option value="VNPAY">VNPay</option>
            <option value="MOMO">MoMo</option>
          </select>
          <textarea
            placeholder="Notes"
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
            className="mt-4 min-h-32 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          />
        </section>

        <aside className="h-fit rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="font-serif text-3xl font-bold text-slate-950">Order Summary</h2>
          <div className="mt-6 grid gap-4">
            {items.length ? (
              items.map((item) => (
                <div className="flex items-start justify-between gap-4 text-sm" key={item.cartItemId}>
                  <span className="min-w-0 text-slate-600">
                    {item.title} x {item.quantity}
                  </span>
                  <strong className="shrink-0 text-slate-950">
                    {formatVND(item.price * item.quantity)}
                  </strong>
                </div>
              ))
            ) : (
              <p className="text-sm text-slate-500">No cart items loaded.</p>
            )}
          </div>
          <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5">
            <span className="font-semibold text-slate-500">Total</span>
            <strong className="text-2xl text-slate-950">{formatVND(cartTotal(items))}</strong>
          </div>
          <button
            className="mt-6 w-full rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            type="submit"
            disabled={!loggedIn || cartSource === "error"}
          >
            Place order
          </button>
          {message && <Notice className="mt-4">{message}</Notice>}
          {paymentUrl && (
            <a
              className="mt-3 inline-flex w-full justify-center rounded-full border border-slate-200 px-6 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              href={paymentUrl}
            >
              Continue to payment
            </a>
          )}
        </aside>
      </form>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="mb-8 border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
        {eyebrow}
      </span>
      <h1 className="mt-3 font-serif text-4xl font-bold text-slate-950 md:text-5xl">{title}</h1>
    </div>
  );
}

function Input({ className = "", ...props }) {
  return (
    <input
      {...props}
      className={[
        "w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100",
        className,
      ].join(" ")}
    />
  );
}

function Notice({ children, className = "" }) {
  return (
    <div
      className={[
        "mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700",
        className,
      ].join(" ")}
    >
      {children}
    </div>
  );
}
