import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { getCart } from "../api/cartApi.js";
import { createCheckout, previewCheckout } from "../api/checkoutApi.js";
import { createAddress, getAddresses } from "../api/userApi.js";
import {
  Input,
  MetaRow,
  Notice,
  PageHeader,
  Select,
  Skeleton,
  Textarea,
} from "../components/ui/index.jsx";
import {
  clearCheckoutCartItemIds,
  getCheckoutCartItemIds,
} from "../utils/checkoutSelection.js";
import { cartTotal, formatVND } from "../utils/formatters.js";
import { normalizeCartItem } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function CheckoutPage({ onAuth }) {
  const { t } = useTranslation();
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
    couponCode: "",
    notes: "",
  });
  const [selectedAddress, setSelectedAddress] = useState("");
  const [message, setMessage] = useState("");
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [checkoutResult, setCheckoutResult] = useState(null);
  const [paymentUrl, setPaymentUrl] = useState("");
  const [qrCodeUrl, setQrCodeUrl] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;

    getAddresses()
      .then((rows) => setAddresses(rows || []))
      .catch((error) => setMessage(error.message || t("checkout.addressesFailed")));

    getCart()
      .then((cart) => {
        setItems((cart?.items || []).map(normalizeCartItem));
        setCartSource("api");
      })
      .catch((error) => {
        setCartSource("error");
        setMessage(error.message || t("checkout.cartFailed"));
      });
  }, [t]);

  const loggedIn = Boolean(getAccessToken());
  const cartItemIds = useMemo(() => getCheckoutCartItemIds(items), [items]);
  const selectedItems = useMemo(() => {
    const idSet = new Set(cartItemIds);
    return items.filter((item) => idSet.has(Number(item.cartItemId)));
  }, [items, cartItemIds]);
  const previewPaymentMethod = form.paymentMethod;
  const previewCouponCode = form.couponCode;
  const previewNotes = form.notes;
  const canPreview = loggedIn && cartSource === "api" && selectedAddress && cartItemIds.length > 0;
  const canSubmit = loggedIn && cartSource === "api" && cartItemIds.length > 0 && !submitting && !previewLoading;

  useEffect(() => {
    if (!canPreview) {
      setPreview(null);
      setPreviewError("");
      setPreviewLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      setPreviewLoading(true);
      setPreviewError("");
      previewCheckout(buildCheckoutBody(selectedAddress, cartItemIds, {
        paymentMethod: previewPaymentMethod,
        couponCode: previewCouponCode,
        notes: previewNotes,
      }), {
        signal: controller.signal,
      })
        .then((response) => {
          setPreview(response);
          setPreviewError("");
        })
        .catch((error) => {
          if (error?.name === "AbortError") return;
          setPreview(null);
          setPreviewError(error.message || t("checkout.previewFailed"));
        })
        .finally(() => {
          if (!controller.signal.aborted) setPreviewLoading(false);
        });
    }, 400);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [canPreview, selectedAddress, cartItemIds, previewPaymentMethod, previewCouponCode, previewNotes, t]);

  async function ensureAddress() {
    if (selectedAddress) return selectedAddress;

    if (!form.recipientName || !form.phoneNumber || !form.addressLine) {
      throw new Error(t("checkout.chooseAddress"));
    }

    const created = await createAddress({
      recipientName: form.recipientName,
      phoneNumber: form.phoneNumber,
      addressLine: form.addressLine,
      ward: form.ward,
      district: form.district,
      city: form.city,
      defaultAddress: addresses.length === 0,
    });
    setAddresses([created, ...addresses]);
    setSelectedAddress(String(created.id));
    return created.id;
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    setPaymentUrl("");
    setQrCodeUrl("");
    setCheckoutResult(null);

    try {
      if (!loggedIn) {
        setMessage(t("checkout.loginRequired"));
        return;
      }
      if (cartSource !== "api") {
        setMessage(t("checkout.unavailable"));
        return;
      }
      if (items.length === 0) {
        setMessage(t("checkout.cartEmpty"));
        return;
      }
      if (cartItemIds.length === 0) {
        setMessage(t("checkout.noCartIds"));
        return;
      }

      setSubmitting(true);
      const addressId = await ensureAddress();
      const response = await createCheckout(buildCheckoutBody(addressId, cartItemIds, form));
      const url = response?.paymentUrl || response?.deeplink;
      const qr = !url ? response?.qrCodeUrl : "";
      const code = response?.paymentGroupCode || t("common.paymentPending");

      setCheckoutResult(response);
      setPaymentUrl(url || "");
      setQrCodeUrl(qr || "");
      clearCheckoutCartItemIds();
      window.dispatchEvent(new Event("aivira-cart"));

      if (url) {
        setMessage(t("checkout.redirectingPayment", { code }));
        window.setTimeout(() => window.location.assign(url), 900);
      } else if (qr) {
        setMessage(t("checkout.qrReady", { code }));
      } else {
        setMessage(t("checkout.created", { code }));
      }
    } catch (error) {
      setMessage(error.message || t("checkout.failed"));
    } finally {
      setSubmitting(false);
    }
  }

  const summaryItems = preview?.items || selectedItems.map((item) => ({
    cartItemId: item.cartItemId,
    productName: item.title,
    sku: item.sku,
    quantity: item.quantity,
    unitPrice: item.price,
    lineSubtotal: item.price * item.quantity,
    promotionDiscountAmount: 0,
    finalLineAmount: item.price * item.quantity,
  }));
  const temporaryTotal = cartTotal(selectedItems);
  const hasCheckoutItems = summaryItems.length > 0;

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("checkout.title")} eyebrow={t("checkout.eyebrow")} />

      {!loggedIn && (
        <Notice className="mb-6">
          {t("checkout.loginRequired")}{" "}
          <button className="font-bold text-blue-700 underline" type="button" onClick={onAuth}>
            {t("common.login")}
          </button>
        </Notice>
      )}
      {loggedIn && cartSource === "error" && <Notice className="mb-6">{t("checkout.disabled")}</Notice>}

      <form className="grid gap-8 lg:grid-cols-[1fr_420px]" onSubmit={submit}>
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
          <h2 className="font-serif text-3xl font-bold text-slate-950">{t("checkout.shipping")}</h2>

          {addresses.length > 0 && (
            <Select
              value={selectedAddress}
              onChange={(event) => setSelectedAddress(event.target.value)}
              className="mt-6"
            >
              <option value="">{t("checkout.createNewAddress")}</option>
              {addresses.map((address) => (
                <option key={address.id} value={address.id}>
                  {address.recipientName} - {address.addressLine}
                </option>
              ))}
            </Select>
          )}

          {!selectedAddress && (
            <>
              <div className="mt-6 grid gap-4 md:grid-cols-2">
                <Input
                  placeholder={t("checkout.recipientName")}
                  value={form.recipientName}
                  onChange={(event) => setForm({ ...form, recipientName: event.target.value })}
                />
                <Input
                  placeholder={t("checkout.phoneNumber")}
                  value={form.phoneNumber}
                  onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })}
                />
              </div>
              <Input
                className="mt-4"
                placeholder={t("checkout.addressLine")}
                value={form.addressLine}
                onChange={(event) => setForm({ ...form, addressLine: event.target.value })}
              />
              <div className="mt-4 grid gap-4 md:grid-cols-3">
                <Input
                  placeholder={t("checkout.ward")}
                  value={form.ward}
                  onChange={(event) => setForm({ ...form, ward: event.target.value })}
                />
                <Input
                  placeholder={t("checkout.district")}
                  value={form.district}
                  onChange={(event) => setForm({ ...form, district: event.target.value })}
                />
                <Input
                  placeholder={t("checkout.city")}
                  value={form.city}
                  onChange={(event) => setForm({ ...form, city: event.target.value })}
                />
              </div>
            </>
          )}

          <h2 className="mt-10 font-serif text-3xl font-bold text-slate-950">{t("checkout.payment")}</h2>
          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {["COD", "VNPAY", "MOMO"].map((method) => (
              <label
                className={[
                  "cursor-pointer rounded-2xl border px-4 py-3 text-center text-sm font-bold transition-colors",
                  form.paymentMethod === method
                    ? "border-blue-500 bg-blue-50 text-blue-700"
                    : "border-slate-200 bg-slate-50 text-slate-700 hover:bg-white",
                ].join(" ")}
                key={method}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value={method}
                  checked={form.paymentMethod === method}
                  onChange={(event) => setForm({ ...form, paymentMethod: event.target.value })}
                  className="sr-only"
                />
                {t(`checkout.methodLabel.${method}`, { defaultValue: method })}
              </label>
            ))}
          </div>

          <h2 className="mt-10 font-serif text-3xl font-bold text-slate-950">{t("checkout.coupon")}</h2>
          <div className="mt-4 flex gap-3">
            <Input
              placeholder={t("checkout.couponPlaceholder")}
              value={form.couponCode}
              onChange={(event) => setForm({ ...form, couponCode: event.target.value })}
            />
            <button
              type="button"
              className="shrink-0 rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              onClick={() => setForm({ ...form, couponCode: "" })}
            >
              {t("checkout.clearCoupon")}
            </button>
          </div>
          {previewError && <Notice className="mt-4">{previewError}</Notice>}

          <Textarea
            placeholder={t("checkout.notes")}
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
            className="mt-8 resize-y"
          />
        </section>

        <aside className="h-fit rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="font-serif text-3xl font-bold text-slate-950">{t("checkout.summary")}</h2>
          {!canPreview && selectedAddress === "" && cartItemIds.length > 0 && (
            <p className="mt-3 text-sm font-semibold text-slate-500">{t("checkout.previewNeedsAddress")}</p>
          )}
          {previewLoading && <Skeleton rows={3} />}

          <div className="mt-6 grid gap-4">
            {hasCheckoutItems ? (
              summaryItems.map((item) => (
                <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4 text-sm" key={item.cartItemId}>
                  <div className="flex items-start justify-between gap-4">
                    <span className="min-w-0 font-semibold text-slate-700">
                      {item.productName} x {item.quantity}
                    </span>
                    <strong className="shrink-0 text-slate-950">
                      {formatVND(item.finalLineAmount ?? item.lineSubtotal)}
                    </strong>
                  </div>
                  {item.sku && <p className="mt-1 text-xs text-slate-500">{item.sku}</p>}
                  {Number(item.promotionDiscountAmount || 0) > 0 && (
                    <p className="mt-2 text-xs font-bold text-emerald-600">
                      {item.promotionName || t("checkout.appliedPromotion")}: -{formatVND(item.promotionDiscountAmount)}
                    </p>
                  )}
                </div>
              ))
            ) : (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-5 text-center">
                <p className="font-bold text-slate-800">{t("checkout.emptyTitle")}</p>
                <p className="mt-2 text-sm text-slate-500">{t("checkout.emptyDescription")}</p>
                <Link
                  className="mt-4 inline-flex justify-center rounded-full border border-slate-200 bg-white px-5 py-2.5 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-100"
                  to="/cart"
                >
                  {t("checkout.backToCart")}
                </Link>
              </div>
            )}
          </div>

          {hasCheckoutItems && (
            <div className="mt-6 grid gap-3 border-t border-slate-100 pt-5">
              <MetaRow label={t("checkout.subtotal")} value={formatVND(preview?.subtotal ?? temporaryTotal)} />
              <MetaRow label={t("checkout.promotionDiscount")} value={formatVND(-(preview?.promotionDiscountAmount || 0))} />
              <MetaRow label={t("checkout.couponDiscount")} value={formatVND(-(preview?.couponDiscountAmount || 0))} />
              <MetaRow label={t("checkout.shippingFee")} value={formatVND(preview?.shippingFee || 0)} />
              {!preview && <p className="text-xs font-semibold text-amber-600">{t("checkout.temporaryTotal")}</p>}
            </div>
          )}

          {hasCheckoutItems && preview?.appliedPromotions?.length > 0 && (
            <div className="mt-5 rounded-2xl border border-emerald-100 bg-emerald-50 p-4">
              <p className="text-sm font-bold text-emerald-700">{t("checkout.appliedPromotions")}</p>
              <div className="mt-2 grid gap-1 text-xs font-semibold text-emerald-700">
                {preview.appliedPromotions.map((promotion) => (
                  <span key={`${promotion.promotionId}-${promotion.promotionName}`}>
                    {promotion.promotionName}: -{formatVND(promotion.discountAmount)}
                  </span>
                ))}
              </div>
            </div>
          )}

          {hasCheckoutItems && preview?.couponCode && (
            <div className="mt-5 rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm font-bold text-blue-700">
              {t("checkout.couponApplied", { code: preview.couponCode })}
            </div>
          )}

          {hasCheckoutItems && (
            <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5">
              <span className="font-semibold text-slate-500">{t("checkout.finalTotal")}</span>
              <strong className="text-2xl text-slate-950">
                {formatVND(preview?.totalAmount ?? temporaryTotal)}
              </strong>
            </div>
          )}

          <button
            className="mt-6 w-full rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            type="submit"
            disabled={!canSubmit}
          >
            {submitting ? t("common.loading") : t("checkout.placeOrder")}
          </button>
          {message && <Notice className="mt-4">{message}</Notice>}
          {checkoutResult?.paymentMethod === "COD" && (
            <Link
              className="mt-3 inline-flex w-full justify-center rounded-full border border-slate-200 px-6 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              to="/orders"
            >
              {t("checkout.viewOrders")}
            </Link>
          )}
          {paymentUrl && (
            <a
              className="mt-3 inline-flex w-full justify-center rounded-full border border-slate-200 px-6 py-3 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
              href={paymentUrl}
            >
              {t("checkout.continuePayment")}
            </a>
          )}
          {qrCodeUrl && (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-center">
              <p className="text-sm font-bold text-slate-700">{t("checkout.scanQr")}</p>
              <img src={qrCodeUrl} alt={t("checkout.scanQr")} className="mx-auto mt-3 max-h-56 rounded-xl" />
            </div>
          )}
        </aside>
      </form>
    </div>
  );
}

function buildCheckoutBody(addressId, cartItemIds, form) {
  const couponCode = String(form.couponCode || "").trim();
  const notes = String(form.notes || "").trim();

  return {
    addressId: Number(addressId),
    cartItemIds,
    paymentMethod: form.paymentMethod,
    couponCode: couponCode || undefined,
    notes: notes || undefined,
  };
}
