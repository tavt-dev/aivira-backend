import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

import {
  createCoupon,
  deleteCoupon,
  getCoupons,
  updateCoupon,
} from "../../api/adminCouponsApi.js";
import {
  createPromotion,
  deletePromotion,
  getPromotions,
  updatePromotion,
} from "../../api/adminPromotionsApi.js";
import {
  Button,
  Input,
  Notice,
  PageHeader,
  Pagination,
  Panel,
  Select,
  Textarea,
  useConfirm,
} from "../../components/ui/index.jsx";
import { getAdminProducts } from "../../api/adminProductsApi.js";
import { getCategories } from "../../api/catalogApi.js";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import { normalizeBook, normalizeCategory, pageRows } from "../../utils/mappers.js";

const DISCOUNT_TYPES = ["PERCENT", "FIXED"];
const PROMOTION_SCOPES = ["PRODUCT", "CATEGORY"];
const PAGE_SIZES = [10, 20, 50];

const emptyCouponForm = {
  code: "",
  type: "PERCENT",
  value: "",
  maxDiscountAmount: "",
  minOrderAmount: "",
  usageLimit: "",
  usageLimitPerUser: "",
  startAt: "",
  endAt: "",
  active: true,
};

const emptyPromotionForm = {
  promotionName: "",
  description: "",
  promotionType: "PERCENT",
  value: "",
  maxDiscountAmount: "",
  promotionScope: "CATEGORY",
  targetId: "",
  startAt: "",
  endAt: "",
  active: true,
};

export default function AdminDiscountsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const [tab, setTab] = useState("coupons");
  const [coupons, setCoupons] = useState([]);
  const [promotions, setPromotions] = useState([]);
  const [couponMeta, setCouponMeta] = useState(createEmptyMeta());
  const [promotionMeta, setPromotionMeta] = useState(createEmptyMeta());
  const [couponPage, setCouponPage] = useState({ page: 1, size: 20 });
  const [promotionPage, setPromotionPage] = useState({ page: 1, size: 20 });
  const [couponForm, setCouponForm] = useState(emptyCouponForm);
  const [promotionForm, setPromotionForm] = useState(emptyPromotionForm);
  const [editingCouponId, setEditingCouponId] = useState(null);
  const [editingPromotionId, setEditingPromotionId] = useState(null);
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [productKeyword, setProductKeyword] = useState("");
  const [loading, setLoading] = useState("");
  const [message, setMessage] = useState("");

  const categoryOptions = useMemo(() => categories.map(normalizeCategory).filter(Boolean), [categories]);
  const productOptions = useMemo(() => products.map(normalizeBook).filter(Boolean), [products]);

  useEffect(() => {
    refreshCoupons(couponPage);
  }, [couponPage]);

  useEffect(() => {
    refreshPromotions(promotionPage);
  }, [promotionPage]);

  useEffect(() => {
    getCategories()
      .then((rows) => setCategories(pageRows(rows)))
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    if (promotionForm.promotionScope !== "PRODUCT") return undefined;
    const timer = window.setTimeout(() => {
      getAdminProducts({ keyword: productKeyword || undefined, page: 1, size: 20 })
        .then((page) => setProducts(pageRows(page)))
        .catch(() => setProducts([]));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [promotionForm.promotionScope, productKeyword]);

  async function refreshCoupons(next = couponPage) {
    setLoading("coupons");
    setMessage("");
    try {
      const page = await getCoupons(next);
      const rows = pageRows(page);
      setCoupons(rows);
      setCouponMeta(pageMeta(page, next, rows));
    } catch (error) {
      setCoupons([]);
      setCouponMeta(createEmptyMeta(next));
      setMessage(error.message || t("admin.couponLoadFailed"));
    } finally {
      setLoading("");
    }
  }

  async function refreshPromotions(next = promotionPage) {
    setLoading("promotions");
    setMessage("");
    try {
      const page = await getPromotions(next);
      const rows = pageRows(page);
      setPromotions(rows);
      setPromotionMeta(pageMeta(page, next, rows));
    } catch (error) {
      setPromotions([]);
      setPromotionMeta(createEmptyMeta(next));
      setMessage(error.message || t("admin.promotionLoadFailed"));
    } finally {
      setLoading("");
    }
  }

  async function submitCoupon(event) {
    event.preventDefault();
    setMessage("");
    const validation = validateCoupon(couponForm, t);
    if (validation) {
      setMessage(validation);
      return;
    }
    try {
      const payload = couponPayload(couponForm);
      if (editingCouponId) {
        await updateCoupon(editingCouponId, payload);
        setMessage(t("admin.couponUpdated"));
      } else {
        await createCoupon(payload);
        setMessage(t("admin.couponCreated"));
      }
      resetCouponForm();
      await refreshCoupons(couponPage);
    } catch (error) {
      setMessage(error.message || t("admin.couponSaveFailed"));
    }
  }

  async function submitPromotion(event) {
    event.preventDefault();
    setMessage("");
    const validation = validatePromotion(promotionForm, t);
    if (validation) {
      setMessage(validation);
      return;
    }
    try {
      const payload = promotionPayload(promotionForm);
      if (editingPromotionId) {
        await updatePromotion(editingPromotionId, payload);
        setMessage(t("admin.promotionUpdated"));
      } else {
        await createPromotion(payload);
        setMessage(t("admin.promotionCreated"));
      }
      resetPromotionForm();
      await refreshPromotions(promotionPage);
    } catch (error) {
      setMessage(error.message || t("admin.promotionSaveFailed"));
    }
  }

  async function deactivateCoupon(coupon) {
    const confirmed = await confirm({
      title: t("admin.deactivate"),
      message: t("admin.confirmDeactivateCoupon", { code: coupon.code }),
      confirmLabel: t("admin.deactivate"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteCoupon(coupon.id);
      setMessage(t("admin.couponDeactivated"));
      await refreshCoupons(couponPage);
    } catch (error) {
      setMessage(error.message || t("admin.couponDeactivateFailed"));
    }
  }

  async function deactivatePromotion(promotion) {
    const confirmed = await confirm({
      title: t("admin.deactivate"),
      message: t("admin.confirmDeactivatePromotion", { name: promotion.promotionName }),
      confirmLabel: t("admin.deactivate"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deletePromotion(promotion.id);
      setMessage(t("admin.promotionDeactivated"));
      await refreshPromotions(promotionPage);
    } catch (error) {
      setMessage(error.message || t("admin.promotionDeactivateFailed"));
    }
  }

  async function copyCoupon(code) {
    try {
      await navigator.clipboard.writeText(code);
      setMessage(t("admin.couponCopied", { code }));
    } catch {
      setMessage(code);
    }
  }

  function editCoupon(coupon) {
    setEditingCouponId(coupon.id);
    setCouponForm({
      code: coupon.code || "",
      type: coupon.type || "PERCENT",
      value: coupon.value ?? "",
      maxDiscountAmount: coupon.maxDiscountAmount ?? "",
      minOrderAmount: coupon.minOrderAmount ?? "",
      usageLimit: coupon.usageLimit ?? "",
      usageLimitPerUser: coupon.usageLimitPerUser ?? "",
      startAt: toInputDateTime(coupon.startAt),
      endAt: toInputDateTime(coupon.endAt),
      active: coupon.active !== false,
    });
    setTab("coupons");
  }

  function editPromotion(promotion) {
    setEditingPromotionId(promotion.id);
    setPromotionForm({
      promotionName: promotion.promotionName || "",
      description: promotion.description || "",
      promotionType: promotion.promotionType || "PERCENT",
      value: promotion.value ?? "",
      maxDiscountAmount: promotion.maxDiscountAmount ?? "",
      promotionScope: promotion.promotionScope || "CATEGORY",
      targetId: promotion.targetId ?? "",
      startAt: toInputDateTime(promotion.startAt),
      endAt: toInputDateTime(promotion.endAt),
      active: promotion.active !== false,
    });
    setTab("promotions");
  }

  function resetCouponForm() {
    setEditingCouponId(null);
    setCouponForm(emptyCouponForm);
  }

  function resetPromotionForm() {
    setEditingPromotionId(null);
    setPromotionForm(emptyPromotionForm);
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.discountsTitle")} eyebrow={t("admin.discountsEyebrow")} />
      {message && <Notice>{message}</Notice>}

      <div className="flex flex-wrap gap-2">
        <TabButton active={tab === "coupons"} onClick={() => setTab("coupons")}>{t("admin.coupons")}</TabButton>
        <TabButton active={tab === "promotions"} onClick={() => setTab("promotions")}>{t("admin.promotions")}</TabButton>
        <Link className="ml-auto rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-50" to="/checkout">
          {t("admin.testInCheckout")}
        </Link>
      </div>

      {tab === "coupons" ? (
        <div className="grid gap-6 xl:grid-cols-[1fr_430px]">
          <CouponList
            coupons={coupons}
            language={i18n.language}
            loading={loading === "coupons"}
            meta={couponMeta}
            onCopy={copyCoupon}
            onDeactivate={deactivateCoupon}
            onEdit={editCoupon}
            onPage={(page) => setCouponPage((current) => ({ ...current, page }))}
            onSize={(size) => setCouponPage({ page: 1, size: Number(size || 20) })}
            t={t}
          />
          <CouponForm
            editing={Boolean(editingCouponId)}
            form={couponForm}
            onCancel={resetCouponForm}
            onChange={setCouponForm}
            onSubmit={submitCoupon}
            t={t}
          />
        </div>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[1fr_460px]">
          <PromotionList
            language={i18n.language}
            loading={loading === "promotions"}
            meta={promotionMeta}
            onDeactivate={deactivatePromotion}
            onEdit={editPromotion}
            onPage={(page) => setPromotionPage((current) => ({ ...current, page }))}
            onSize={(size) => setPromotionPage({ page: 1, size: Number(size || 20) })}
            promotions={promotions}
            resolveTarget={(promotion) => resolveTarget(promotion, categoryOptions, productOptions)}
            t={t}
          />
          <PromotionForm
            categories={categoryOptions}
            editing={Boolean(editingPromotionId)}
            form={promotionForm}
            onCancel={resetPromotionForm}
            onChange={setPromotionForm}
            onProductKeyword={setProductKeyword}
            onSubmit={submitPromotion}
            productKeyword={productKeyword}
            products={productOptions}
            t={t}
          />
        </div>
      )}
    </div>
  );
}

function CouponList({ coupons, language, loading, meta, onCopy, onDeactivate, onEdit, onPage, onSize, t }) {
  return (
    <Panel title={t("admin.couponList")}>
      <PageSize value={meta.pageSize} onChange={onSize} />
      <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        <table className="min-w-[980px] w-full border-collapse text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">{t("admin.code")}</th>
              <th className="px-4 py-3">{t("admin.type")}</th>
              <th className="px-4 py-3">{t("admin.value")}</th>
              <th className="px-4 py-3">{t("admin.usage")}</th>
              <th className="px-4 py-3">{t("admin.dateRange")}</th>
              <th className="px-4 py-3">{t("common.status")}</th>
              <th className="px-4 py-3">{t("admin.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {coupons.map((coupon) => (
              <tr className="border-t border-slate-100 align-top" key={coupon.id}>
                <td className="px-4 py-3">
                  <p className="font-bold text-slate-950">{coupon.code}</p>
                  <p className="text-xs text-slate-500">#{coupon.id}</p>
                </td>
                <td className="px-4 py-3">{coupon.type}</td>
                <td className="px-4 py-3">
                  <p>{formatDiscount(coupon.type, coupon.value, language)}</p>
                  <p className="text-xs text-slate-500">{t("admin.max")}: {formatOptionalMoney(coupon.maxDiscountAmount, language)}</p>
                  <p className="text-xs text-slate-500">{t("admin.minOrder")}: {formatOptionalMoney(coupon.minOrderAmount, language)}</p>
                </td>
                <td className="px-4 py-3">
                  <p>{coupon.usedCount || 0} / {coupon.usageLimit ?? "-"}</p>
                  <p className="text-xs text-slate-500">{t("admin.perUser")}: {coupon.usageLimitPerUser ?? "-"}</p>
                </td>
                <td className="px-4 py-3 text-slate-600">
                  <p>{formatLocalDate(coupon.startAt)}</p>
                  <p>{formatLocalDate(coupon.endAt)}</p>
                  <p className="text-xs text-slate-400">{formatDateTime(coupon.updatedAt, language)}</p>
                </td>
                <td className="px-4 py-3"><Status active={coupon.active} t={t} /></td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="secondary" onClick={() => onCopy(coupon.code)}>{t("admin.copyCode")}</Button>
                    <Button size="sm" variant="secondary" onClick={() => onEdit(coupon)}>{t("common.edit")}</Button>
                    <Button size="sm" variant="danger" onClick={() => onDeactivate(coupon)}>{t("admin.deactivate")}</Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
        {!loading && !coupons.length && <div className="p-5 text-sm text-slate-500">{t("admin.noCoupons")}</div>}
      </div>
      <Pagination meta={meta} loading={loading} onPage={onPage} t={t} />
    </Panel>
  );
}

function CouponForm({ editing, form, onCancel, onChange, onSubmit, t }) {
  return (
    <Panel title={editing ? t("admin.editCoupon") : t("admin.createCoupon")}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <Input required maxLength={50} value={form.code} onChange={(event) => onChange({ ...form, code: event.target.value.toUpperCase() })} placeholder={t("admin.code")} />
        <div className="grid gap-3 md:grid-cols-2">
          <TypeSelect value={form.type} onChange={(value) => onChange({ ...form, type: value })} />
          <Input required min="0" type="number" value={form.value} onChange={(event) => onChange({ ...form, value: event.target.value })} placeholder={t("admin.value")} />
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          <Input min="0" type="number" value={form.maxDiscountAmount} onChange={(event) => onChange({ ...form, maxDiscountAmount: event.target.value })} placeholder={t("admin.maxDiscountAmount")} />
          <Input min="0" type="number" value={form.minOrderAmount} onChange={(event) => onChange({ ...form, minOrderAmount: event.target.value })} placeholder={t("admin.minOrderAmount")} />
          <Input min="0" type="number" value={form.usageLimit} onChange={(event) => onChange({ ...form, usageLimit: event.target.value })} placeholder={t("admin.usageLimit")} />
          <Input min="0" type="number" value={form.usageLimitPerUser} onChange={(event) => onChange({ ...form, usageLimitPerUser: event.target.value })} placeholder={t("admin.usageLimitPerUser")} />
        </div>
        <DateRangeInputs form={form} onChange={onChange} t={t} />
        <ActiveCheckbox checked={form.active} onChange={(active) => onChange({ ...form, active })} t={t} />
        <div className="flex flex-wrap gap-2">
          <Button type="submit">{editing ? t("admin.updateCoupon") : t("admin.createCoupon")}</Button>
          {editing && <Button variant="secondary" type="button" onClick={onCancel}>{t("common.cancel")}</Button>}
        </div>
      </form>
    </Panel>
  );
}

function PromotionList({ language, loading, meta, onDeactivate, onEdit, onPage, onSize, promotions, resolveTarget, t }) {
  return (
    <Panel title={t("admin.promotionList")}>
      <PageSize value={meta.pageSize} onChange={onSize} />
      <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        <table className="min-w-[1040px] w-full border-collapse text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">{t("admin.promotionName")}</th>
              <th className="px-4 py-3">{t("admin.type")}</th>
              <th className="px-4 py-3">{t("admin.value")}</th>
              <th className="px-4 py-3">{t("admin.target")}</th>
              <th className="px-4 py-3">{t("admin.dateRange")}</th>
              <th className="px-4 py-3">{t("common.status")}</th>
              <th className="px-4 py-3">{t("admin.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {promotions.map((promotion) => (
              <tr className="border-t border-slate-100 align-top" key={promotion.id}>
                <td className="px-4 py-3">
                  <p className="font-bold text-slate-950">{promotion.promotionName}</p>
                  <p className="text-xs text-slate-500">{promotion.description}</p>
                </td>
                <td className="px-4 py-3">{promotion.promotionType}</td>
                <td className="px-4 py-3">
                  <p>{formatDiscount(promotion.promotionType, promotion.value, language)}</p>
                  <p className="text-xs text-slate-500">{t("admin.max")}: {formatOptionalMoney(promotion.maxDiscountAmount, language)}</p>
                </td>
                <td className="px-4 py-3">
                  <p className="font-semibold">{promotion.promotionScope}</p>
                  <p className="text-xs text-slate-500">#{promotion.targetId} {resolveTarget(promotion)}</p>
                </td>
                <td className="px-4 py-3 text-slate-600">
                  <p>{formatLocalDate(promotion.startAt)}</p>
                  <p>{formatLocalDate(promotion.endAt)}</p>
                  <p className="text-xs text-slate-400">{formatDateTime(promotion.updatedAt, language)}</p>
                </td>
                <td className="px-4 py-3"><Status active={promotion.active} t={t} /></td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="secondary" onClick={() => onEdit(promotion)}>{t("common.edit")}</Button>
                    <Button size="sm" variant="danger" onClick={() => onDeactivate(promotion)}>{t("admin.deactivate")}</Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
        {!loading && !promotions.length && <div className="p-5 text-sm text-slate-500">{t("admin.noPromotions")}</div>}
      </div>
      <Pagination meta={meta} loading={loading} onPage={onPage} t={t} />
    </Panel>
  );
}

function PromotionForm({ categories, editing, form, onCancel, onChange, onProductKeyword, onSubmit, productKeyword, products, t }) {
  return (
    <Panel title={editing ? t("admin.editPromotion") : t("admin.createPromotion")}>
      <form className="grid gap-4" onSubmit={onSubmit}>
        <Input required maxLength={150} value={form.promotionName} onChange={(event) => onChange({ ...form, promotionName: event.target.value })} placeholder={t("admin.promotionName")} />
        <Textarea required value={form.description} onChange={(event) => onChange({ ...form, description: event.target.value })} placeholder={t("admin.description")} />
        <div className="grid gap-3 md:grid-cols-2">
          <TypeSelect value={form.promotionType} onChange={(value) => onChange({ ...form, promotionType: value })} />
          <Input required min="0" type="number" value={form.value} onChange={(event) => onChange({ ...form, value: event.target.value })} placeholder={t("admin.value")} />
          <Input min="0" type="number" value={form.maxDiscountAmount} onChange={(event) => onChange({ ...form, maxDiscountAmount: event.target.value })} placeholder={t("admin.maxDiscountAmount")} />
          <Select value={form.promotionScope} onChange={(event) => onChange({ ...form, promotionScope: event.target.value, targetId: "" })}>
            {PROMOTION_SCOPES.map((scope) => <option key={scope} value={scope}>{scope}</option>)}
          </Select>
        </div>
        {form.promotionScope === "CATEGORY" ? (
          <Select required value={form.targetId} onChange={(event) => onChange({ ...form, targetId: event.target.value })}>
            <option value="">{t("admin.chooseCategory")}</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
          </Select>
        ) : (
          <div className="grid gap-3">
            <Input value={productKeyword} onChange={(event) => onProductKeyword(event.target.value)} placeholder={t("admin.searchProducts")} />
            <Select required value={form.targetId} onChange={(event) => onChange({ ...form, targetId: event.target.value })}>
              <option value="">{t("admin.chooseProduct")}</option>
              {products.map((product) => <option key={product.id} value={product.id}>{product.title} / {product.sku}</option>)}
            </Select>
          </div>
        )}
        <DateRangeInputs form={form} onChange={onChange} t={t} />
        <ActiveCheckbox checked={form.active} onChange={(active) => onChange({ ...form, active })} t={t} />
        <div className="flex flex-wrap gap-2">
          <Button type="submit">{editing ? t("admin.updatePromotion") : t("admin.createPromotion")}</Button>
          {editing && <Button variant="secondary" type="button" onClick={onCancel}>{t("common.cancel")}</Button>}
        </div>
      </form>
    </Panel>
  );
}

function DateRangeInputs({ form, onChange, t }) {
  return (
    <div className="grid gap-3 md:grid-cols-2">
      <Input required type="datetime-local" value={form.startAt} onChange={(event) => onChange({ ...form, startAt: event.target.value })} aria-label={t("admin.startAt")} />
      <Input required type="datetime-local" value={form.endAt} onChange={(event) => onChange({ ...form, endAt: event.target.value })} aria-label={t("admin.endAt")} />
    </div>
  );
}

function TypeSelect({ onChange, value }) {
  return (
    <Select value={value} onChange={(event) => onChange(event.target.value)}>
      {DISCOUNT_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
    </Select>
  );
}

function ActiveCheckbox({ checked, onChange, t }) {
  return (
    <label className="flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold text-slate-700">
      <input checked={checked} type="checkbox" onChange={(event) => onChange(event.target.checked)} />
      {t("common.active")}
    </label>
  );
}

function couponPayload(form) {
  return {
    code: form.code.trim().toUpperCase(),
    type: form.type,
    value: Number(form.value),
    maxDiscountAmount: optionalNumber(form.maxDiscountAmount),
    minOrderAmount: optionalNumber(form.minOrderAmount),
    usageLimit: optionalNumber(form.usageLimit),
    usageLimitPerUser: optionalNumber(form.usageLimitPerUser),
    startAt: localDateTime(form.startAt),
    endAt: localDateTime(form.endAt),
    active: form.active,
  };
}

function promotionPayload(form) {
  return {
    promotionName: form.promotionName.trim(),
    description: form.description.trim(),
    promotionType: form.promotionType,
    value: Number(form.value),
    maxDiscountAmount: optionalNumber(form.maxDiscountAmount),
    promotionScope: form.promotionScope,
    targetId: Number(form.targetId),
    startAt: localDateTime(form.startAt),
    endAt: localDateTime(form.endAt),
    active: form.active,
  };
}

function validateCoupon(form, t) {
  if (!form.code.trim() || !form.type || !form.value || !form.startAt || !form.endAt) return t("admin.discountRequired");
  if (form.code.trim().length > 50) return t("admin.couponCodeLength");
  return validateDiscountFields(form.type, form.value, form.startAt, form.endAt, t, [
    form.maxDiscountAmount,
    form.minOrderAmount,
    form.usageLimit,
    form.usageLimitPerUser,
  ]);
}

function validatePromotion(form, t) {
  if (!form.promotionName.trim() || !form.description.trim() || !form.value || !form.targetId || !form.startAt || !form.endAt) return t("admin.discountRequired");
  if (form.promotionName.trim().length > 150) return t("admin.promotionNameLength");
  return validateDiscountFields(form.promotionType, form.value, form.startAt, form.endAt, t, [form.maxDiscountAmount]);
}

function validateDiscountFields(type, value, startAt, endAt, t, numbers = []) {
  if (Number(value) <= 0) return t("admin.discountValueInvalid");
  if (type === "PERCENT" && Number(value) > 100) return t("admin.percentInvalid");
  if (numbers.some((item) => item !== "" && item !== null && Number(item) < 0)) return t("admin.validationNonNegative");
  if (new Date(startAt).getTime() > new Date(endAt).getTime()) return t("admin.invalidDateRange");
  return "";
}

function optionalNumber(value) {
  if (value === "" || value === null || value === undefined) return null;
  return Number(value);
}

function localDateTime(value) {
  return value ? `${value.length === 16 ? `${value}:00` : value}` : null;
}

function toInputDateTime(value) {
  if (!value) return "";
  return String(value).slice(0, 16);
}

function formatDiscount(type, value, language) {
  if (type === "PERCENT") return `${Number(value || 0)}%`;
  return formatVND(value, language);
}

function formatOptionalMoney(value, language) {
  return value === null || value === undefined ? "-" : formatVND(value, language);
}

function formatLocalDate(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}

function resolveTarget(promotion, categories, products) {
  if (promotion.promotionScope === "CATEGORY") {
    return categories.find((category) => String(category.id) === String(promotion.targetId))?.label || "";
  }
  return products.find((product) => String(product.id) === String(promotion.targetId))?.title || "";
}

function pageMeta(page, fallback, rows) {
  return {
    currentPage: Number(page?.currentPage || fallback.page || 1),
    totalPages: Number(page?.totalPages || 0),
    pageSize: Number(page?.pageSize || fallback.size || 20),
    totalElements: Number(page?.totalElements || rows.length),
    hasNext: Boolean(page?.hasNext),
    hasPrevious: Boolean(page?.hasPrevious),
  };
}

function createEmptyMeta(fallback = { page: 1, size: 20 }) {
  return {
    currentPage: Number(fallback.page || 1),
    totalPages: 0,
    pageSize: Number(fallback.size || 20),
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  };
}

function PageSize({ onChange, value }) {
  return (
    <Select className="max-w-28" value={value || 20} onChange={(event) => onChange(event.target.value)}>
      {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
    </Select>
  );
}

function Status({ active, t }) {
  return <span className={["rounded-full px-2 py-1 text-xs font-bold", active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"].join(" ")}>{active ? t("common.active") : t("admin.inactive")}</span>;
}

function TabButton({ active, children, onClick }) {
  return <button className={["rounded-full px-4 py-2 text-sm font-bold transition-colors", active ? "bg-slate-950 text-white" : "border border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} type="button" onClick={onClick}>{children}</button>;
}


