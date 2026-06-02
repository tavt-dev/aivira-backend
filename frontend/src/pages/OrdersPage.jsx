import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import { cancelOrder, getOrder, getOrders } from "../api/orderApi.js";
import { formatVND } from "../utils/formatters.js";
import { normalizeOrder, pageRows } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function OrdersPage({ onAuth }) {
  const { t } = useTranslation();
  const [orders, setOrders] = useState([]);
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;

    getOrders()
      .then((page) => setOrders(pageRows(page).map(normalizeOrder)))
      .catch((error) => setMessage(error.message || t("orders.loadFailed")));
  }, [t]);

  async function cancel(order) {
    try {
      const updated = await cancelOrder(order.id, t("orders.cancelReason"));
      setOrders((current) =>
        current.map((item) => (item.id === order.id ? normalizeOrder(updated) : item))
      );
      setMessage(t("orders.cancelled"));
    } catch (error) {
      setMessage(error.message || t("orders.cancelFailed"));
    }
  }

  async function viewDetail(order) {
    setMessage("");
    try {
      setSelected(await getOrder(order.id));
    } catch (error) {
      setMessage(error.message || t("orders.detailFailed"));
    }
  }

  const loggedIn = Boolean(getAccessToken());

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("orders.title")} eyebrow={t("orders.eyebrow")} />
      {message && <Notice>{message}</Notice>}
      {!loggedIn && (
        <Notice>
          {t("orders.loginRequired")}{" "}
          <button type="button" className="font-bold text-blue-700 underline" onClick={onAuth}>
            {t("common.login")}
          </button>
        </Notice>
      )}

      {orders.length === 0 ? (
        <EmptyState title={t("orders.empty")} />
      ) : (
        <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          {orders.map((order) => (
            <div
              className="grid gap-3 border-b border-slate-100 p-5 last:border-b-0 md:grid-cols-[1fr_160px_160px_auto_auto] md:items-center"
              key={order.id}
            >
              <span className="font-bold text-slate-950">{order.orderCode || order.id}</span>
              <strong className="rounded-full bg-blue-50 px-3 py-1 text-center text-xs uppercase tracking-wider text-blue-700">
                {order.orderStatus}
              </strong>
              <span className="font-semibold text-slate-700">{formatVND(order.totalAmount || 0)}</span>
              <button
                type="button"
                onClick={() => viewDetail(order)}
                className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 hover:bg-slate-50"
              >
                {t("orders.detail")}
              </button>
              <button
                type="button"
                onClick={() => cancel(order)}
                className="rounded-full border border-red-100 px-4 py-2 text-sm font-bold text-red-600 hover:bg-red-50"
              >
                {t("common.cancel")}
              </button>
            </div>
          ))}
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm">
          <div className="relative max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl">
            <button
              className="absolute right-4 top-4 rounded-full bg-slate-100 px-3 py-1 text-sm font-bold text-slate-600"
              type="button"
              onClick={() => setSelected(null)}
            >
              x
            </button>
            <h2 className="font-serif text-3xl font-bold text-slate-950">
              {selected.orderCode || selected.id}
            </h2>
            <div className="mt-5 grid gap-2 text-sm text-slate-600">
              <p>{t("orders.status")}: {selected.orderStatus}</p>
              <p>
                {t("orders.payment")}: {selected.paymentMethod || "-"} / {selected.paymentStatus || "-"}
              </p>
              <p>
                {t("orders.shipping")}: {selected.shippingRecipientName || selected.recipientName || "-"} -{" "}
                {selected.shippingAddressLine || ""}
              </p>
            </div>
            <div className="mt-6 grid gap-3">
              {(selected.items || []).map((item) => (
                <div
                  className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 p-4"
                  key={item.id || item.productId || item.productName}
                >
                  <span className="font-semibold text-slate-800">
                    {item.productName || item.title} x {item.quantity}
                  </span>
                  <small className="font-bold text-slate-950">
                    {formatVND(item.totalPrice || item.price || item.finalPrice || 0)}
                  </small>
                </div>
              ))}
            </div>
          </div>
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

function EmptyState({ title }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h2 className="font-serif text-3xl font-bold text-slate-950">{title}</h2>
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
