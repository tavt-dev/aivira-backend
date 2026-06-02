import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { clearCart, getCart, removeCartItem, updateCartItem } from "../api/cartApi.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function CartPage({ onAuth }) {
  const { t } = useTranslation();
  const [items, setItems] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;

    getCart()
      .then((cart) => {
        setItems((cart?.items || []).map(normalizeCartItem));
      })
      .catch((error) => setMessage(error.message || t("cart.loadFailed")));
  }, [t]);

  function updateQuantity(item, quantity) {
    const next = items.map((candidate) =>
      candidate.cartItemId === item.cartItemId ? { ...candidate, quantity } : candidate
    );
    setItems(next);

    updateCartItem(item.cartItemId, { quantity })
      .then((cart) => {
        setItems((cart?.items || []).map(normalizeCartItem));
        window.dispatchEvent(new Event("aivira-cart"));
      })
      .catch((error) => setMessage(error.message || t("cart.updateFailed")));
  }

  function removeItem(item) {
    setItems((current) => current.filter((candidate) => candidate.cartItemId !== item.cartItemId));
    removeCartItem(item.cartItemId)
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || t("cart.removeFailed")));
  }

  function clearAll() {
    setItems([]);
    clearCart()
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || t("cart.clearFailed")));
  }

  const loggedIn = Boolean(getAccessToken());

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("cart.title")} eyebrow={t("cart.eyebrow")} />

      {message && <Notice>{message}</Notice>}

      {!loggedIn ? (
        <EmptyState
          title={t("cart.loginRequired")}
          action={
            <button
              type="button"
              className="inline-flex rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              onClick={onAuth}
            >
              {t("common.login")}
            </button>
          }
        />
      ) : items.length === 0 ? (
        <EmptyState
          title={t("cart.empty")}
          action={
            <Link
              className="inline-flex rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              to="/category/all"
            >
              {t("cart.browse")}
            </Link>
          }
        />
      ) : (
        <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
          <div className="grid gap-4">
            {items.map((item) => (
              <div
                className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[86px_1fr_120px_auto] md:items-center"
                key={item.cartItemId}
              >
                <img
                  src={item.image}
                  alt={item.title}
                  className="aspect-[2/3] w-24 rounded-xl object-cover md:w-full"
                />
                <div className="min-w-0">
                  <Link
                    to={`/product/${item.slug}`}
                    className="font-serif text-xl font-bold text-slate-950 transition-colors hover:text-blue-600"
                  >
                    {item.title}
                  </Link>
                  <p className="mt-1 text-sm text-slate-500">{formatVND(item.price)}</p>
                </div>
                <input
                  type="number"
                  min="1"
                  value={item.quantity}
                  onChange={(event) =>
                    updateQuantity(item, Math.max(1, Number(event.target.value)))
                  }
                  className="w-28 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                />
                <button
                  type="button"
                  onClick={() => removeItem(item)}
                  className="rounded-full border border-red-100 px-4 py-2 text-sm font-bold text-red-600 transition-colors hover:bg-red-50"
                >
                  {t("common.remove")}
                </button>
              </div>
            ))}
          </div>

          <aside className="h-fit rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-serif text-3xl font-bold text-slate-950">{t("cart.summary")}</h2>
            <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5">
              <span className="font-semibold text-slate-500">{t("common.total")}</span>
              <strong className="text-2xl text-slate-950">{formatVND(cartTotal(items))}</strong>
            </div>
            <Link
              className="mt-6 inline-flex w-full justify-center rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              to="/checkout"
            >
              {t("cart.checkout")}
            </Link>
            <button
              className="mt-3 w-full rounded-full border border-slate-200 px-5 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              type="button"
              onClick={clearAll}
            >
              {t("cart.clear")}
            </button>
          </aside>
        </div>
      )}
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

function EmptyState({ title, action }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h2 className="font-serif text-3xl font-bold text-slate-950">{title}</h2>
      {action && <div className="mt-6 flex justify-center">{action}</div>}
    </div>
  );
}

function Notice({ children }) {
  return (
    <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
      {children}
    </div>
  );
}
