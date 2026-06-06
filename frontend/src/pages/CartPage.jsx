import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { clearCart, getCart, removeCartItem, updateCartItem } from "../api/cartApi.js";
import {
  getCheckoutCartItemIds,
  getStoredCheckoutCartItemIds,
  isCartItemCheckoutAvailable,
  removeCheckoutCartItemIds,
  saveCheckoutCartItemIds,
} from "../utils/checkoutSelection.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function CartPage({ onAuth }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;

    getCart()
      .then((cart) => {
        const normalizedItems = (cart?.items || []).map(normalizeCartItem);
        const storedIds = getStoredCheckoutCartItemIds();
        const nextSelectedIds = storedIds.length
          ? getCheckoutCartItemIds(normalizedItems)
          : normalizedItems
              .filter(isCartItemCheckoutAvailable)
              .map((item) => Number(item.cartItemId))
              .filter(Boolean);

        setItems(normalizedItems);
        setSelectedIds(nextSelectedIds);
        saveCheckoutCartItemIds(nextSelectedIds);
      })
      .catch((error) => setMessage(error.message || t("cart.loadFailed")));
  }, [t]);

  function syncSelection(nextIds) {
    setSelectedIds(nextIds);
    saveCheckoutCartItemIds(nextIds);
  }

  function toggleItem(item) {
    if (!isCartItemCheckoutAvailable(item)) return;

    const itemId = Number(item.cartItemId);
    const nextIds = selectedIds.includes(itemId)
      ? selectedIds.filter((id) => id !== itemId)
      : [...selectedIds, itemId];
    syncSelection(nextIds);
  }

  function toggleAll() {
    const availableIds = items
      .filter(isCartItemCheckoutAvailable)
      .map((item) => Number(item.cartItemId))
      .filter(Boolean);
    const allSelected = availableIds.length > 0 && availableIds.every((id) => selectedIds.includes(id));
    syncSelection(allSelected ? [] : availableIds);
  }

  function updateQuantity(item, quantity) {
    if (!isCartItemCheckoutAvailable(item)) return;

    const maxQuantity = item.stockQuantity == null ? Number.MAX_SAFE_INTEGER : Math.max(1, Number(item.stockQuantity));
    const nextQuantity = Math.min(maxQuantity, Math.max(1, Number(quantity) || 1));
    const next = items.map((candidate) =>
      candidate.cartItemId === item.cartItemId ? { ...candidate, quantity: nextQuantity } : candidate
    );
    setItems(next);

    updateCartItem(item.cartItemId, { quantity: nextQuantity })
      .then((cart) => {
        const normalizedItems = (cart?.items || []).map(normalizeCartItem);
        setItems(normalizedItems);
        syncSelection(getCheckoutCartItemIds(normalizedItems));
        window.dispatchEvent(new Event("aivira-cart"));
      })
      .catch((error) => setMessage(error.message || t("cart.updateFailed")));
  }

  function removeItem(item) {
    const itemId = Number(item.cartItemId);
    setItems((current) => current.filter((candidate) => candidate.cartItemId !== item.cartItemId));
    syncSelection(removeCheckoutCartItemIds([itemId]));

    removeCartItem(item.cartItemId)
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || t("cart.removeFailed")));
  }

  function clearAll() {
    setItems([]);
    syncSelection([]);
    clearCart()
      .then(() => window.dispatchEvent(new Event("aivira-cart")))
      .catch((error) => setMessage(error.message || t("cart.clearFailed")));
  }

  function goToCheckout() {
    if (selectedIds.length === 0) {
      setMessage(t("cart.selectAtLeastOne"));
      return;
    }
    saveCheckoutCartItemIds(selectedIds);
    navigate("/checkout");
  }

  const loggedIn = Boolean(getAccessToken());
  const availableItems = items.filter(isCartItemCheckoutAvailable);
  const allSelected = availableItems.length > 0 && availableItems.every((item) => selectedIds.includes(Number(item.cartItemId)));
  const selectedItems = useMemo(
    () => items.filter((item) => selectedIds.includes(Number(item.cartItemId))),
    [items, selectedIds]
  );

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
            <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-700 shadow-sm">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={toggleAll}
                className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              {t("cart.selectAll")}
              <span className="ml-auto text-xs font-semibold text-slate-500">
                {t("cart.selectedCount", { count: selectedIds.length })}
              </span>
            </label>

            {items.map((item) => {
              const itemId = Number(item.cartItemId);
              const available = isCartItemCheckoutAvailable(item);
              const maxQuantity = item.stockQuantity == null ? null : Math.max(0, Number(item.stockQuantity));

              return (
                <div
                  className={[
                    "grid gap-4 rounded-2xl border bg-white p-4 shadow-sm md:grid-cols-[28px_86px_1fr_150px_auto] md:items-center",
                    available ? "border-slate-200" : "border-red-100 bg-red-50/30",
                  ].join(" ")}
                  key={item.cartItemId}
                >
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(itemId)}
                    disabled={!available}
                    onChange={() => toggleItem(item)}
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-40"
                    aria-label={t("cart.selectItem")}
                  />
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
                    <p className="mt-1 text-sm text-slate-500">{item.author}</p>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold text-slate-500">
                      {item.size && <span className="rounded-full bg-slate-100 px-2 py-1">{item.size}</span>}
                      {item.color && <span className="rounded-full bg-slate-100 px-2 py-1">{item.color}</span>}
                      {item.sku && <span className="rounded-full bg-slate-100 px-2 py-1">{item.sku}</span>}
                    </div>
                    <p className={["mt-2 text-xs font-bold", available ? "text-emerald-600" : "text-red-600"].join(" ")}>
                      {available
                        ? maxQuantity == null
                          ? t("cart.available")
                          : t("cart.stockLeft", { count: maxQuantity })
                        : t("cart.unavailable")}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-500">{formatVND(item.price)}</p>
                    <div className="mt-3 inline-flex items-center rounded-full border border-slate-200 bg-slate-50">
                      <button
                        type="button"
                        className="px-3 py-2 text-sm font-bold text-slate-700 disabled:opacity-40"
                        disabled={!available || item.quantity <= 1}
                        onClick={() => updateQuantity(item, item.quantity - 1)}
                        aria-label={t("cart.decrease")}
                      >
                        -
                      </button>
                      <span className="min-w-10 px-2 text-center text-sm font-bold text-slate-950">{item.quantity}</span>
                      <button
                        type="button"
                        className="px-3 py-2 text-sm font-bold text-slate-700 disabled:opacity-40"
                        disabled={!available || (maxQuantity != null && item.quantity >= maxQuantity)}
                        onClick={() => updateQuantity(item, item.quantity + 1)}
                        aria-label={t("cart.increase")}
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <div className="flex items-center justify-between gap-3 md:block md:text-right">
                    <strong className="text-lg text-slate-950">{formatVND(item.price * item.quantity)}</strong>
                    <button
                      type="button"
                      onClick={() => removeItem(item)}
                      className="mt-0 rounded-full border border-red-100 px-4 py-2 text-sm font-bold text-red-600 transition-colors hover:bg-red-50 md:mt-3"
                    >
                      {t("common.remove")}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          <aside className="h-fit rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-serif text-3xl font-bold text-slate-950">{t("cart.summary")}</h2>
            <div className="mt-6 grid gap-3 text-sm">
              <SummaryRow label={t("cart.selectedItems")} value={selectedIds.length} />
              <SummaryRow label={t("cart.availableItems")} value={availableItems.length} />
            </div>
            <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5">
              <span className="font-semibold text-slate-500">{t("common.total")}</span>
              <strong className="text-2xl text-slate-950">{formatVND(cartTotal(selectedItems))}</strong>
            </div>
            <button
              className="mt-6 inline-flex w-full justify-center rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
              type="button"
              disabled={selectedIds.length === 0}
              onClick={goToCheckout}
            >
              {t("cart.checkout")}
            </button>
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

function SummaryRow({ label, value }) {
  return (
    <div className="flex items-center justify-between">
      <span className="font-semibold text-slate-500">{label}</span>
      <strong className="text-slate-950">{value}</strong>
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
