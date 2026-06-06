import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import {
  getDashboardLowStock,
  getDashboardOrders,
  getDashboardSales,
  getDashboardSummary,
  getDashboardTopBooks,
} from "../../api/adminDashboardApi.js";
import { formatVND } from "../../utils/formatters.js";

const DEFAULT_TOP_LIMIT = 10;
const DEFAULT_LOW_STOCK_LIMIT = 10;
const DEFAULT_LOW_STOCK_THRESHOLD = 5;
const MAX_LIMIT = 50;
const ADMIN_INPUT =
  "w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100";

export default function AdminDashboardPage() {
  const { t } = useTranslation();
  const defaults = useMemo(() => defaultDateRange(), []);
  const [filters, setFilters] = useState({
    fromDate: defaults.fromDate,
    toDate: defaults.toDate,
    topLimit: DEFAULT_TOP_LIMIT,
    lowStockLimit: DEFAULT_LOW_STOCK_LIMIT,
    lowStockThreshold: DEFAULT_LOW_STOCK_THRESHOLD,
  });
  const [data, setData] = useState({
    summary: null,
    sales: null,
    orders: null,
    topBooks: null,
    lowStock: null,
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    const rangeParams = toRangeParams(filters);
    const topLimit = clamp(filters.topLimit, 1, MAX_LIMIT);
    const lowStockLimit = clamp(filters.lowStockLimit, 1, MAX_LIMIT);
    const lowStockThreshold = Math.max(0, Number(filters.lowStockThreshold || 0));

    setLoading(true);
    Promise.allSettled([
      getDashboardSummary(rangeParams),
      getDashboardSales(rangeParams),
      getDashboardOrders(rangeParams),
      getDashboardTopBooks({ ...rangeParams, limit: topLimit }),
      getDashboardLowStock({ threshold: lowStockThreshold, limit: lowStockLimit }),
    ])
      .then((results) => {
        if (ignore) return;
        const [summary, sales, orders, topBooks, lowStock] = results;
        setData({
          summary: resultValue(summary),
          sales: resultValue(sales),
          orders: resultValue(orders),
          topBooks: resultValue(topBooks),
          lowStock: resultValue(lowStock),
        });
        setErrors({
          summary: resultError(summary),
          sales: resultError(sales),
          orders: resultError(orders),
          topBooks: resultError(topBooks),
          lowStock: resultError(lowStock),
        });
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [filters.fromDate, filters.toDate, filters.topLimit, filters.lowStockLimit, filters.lowStockThreshold, refreshKey]);

  function updateFilter(field, value) {
    setFilters((current) => ({ ...current, [field]: value }));
  }

  return (
    <div className="grid gap-8">
      <PageHeader title={t("admin.dashboardTitle")} eyebrow={t("admin.dashboardEyebrow")} />

      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_150px_150px_170px_auto]">
          <Field label={t("admin.fromDate")}>
            <input
              type="date"
              value={filters.fromDate}
              onChange={(event) => updateFilter("fromDate", event.target.value)}
              className={ADMIN_INPUT}
            />
          </Field>
          <Field label={t("admin.toDate")}>
            <input
              type="date"
              value={filters.toDate}
              onChange={(event) => updateFilter("toDate", event.target.value)}
              className={ADMIN_INPUT}
            />
          </Field>
          <NumberField
            label={t("admin.topLimit")}
            value={filters.topLimit}
            onChange={(value) => updateFilter("topLimit", clamp(value, 1, MAX_LIMIT))}
          />
          <NumberField
            label={t("admin.lowStockThreshold")}
            value={filters.lowStockThreshold}
            min={0}
            onChange={(value) => updateFilter("lowStockThreshold", Math.max(0, value))}
          />
          <NumberField
            label={t("admin.lowStockLimit")}
            value={filters.lowStockLimit}
            onChange={(value) => updateFilter("lowStockLimit", clamp(value, 1, MAX_LIMIT))}
          />
          <button
            type="button"
            onClick={() => setRefreshKey((current) => current + 1)}
            className="self-end rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600 disabled:cursor-wait disabled:opacity-60"
            disabled={loading}
          >
            {loading ? t("admin.dashboardLoading") : t("admin.refresh")}
          </button>
        </div>
      </section>

      <SummarySection summary={data.summary} error={errors.summary} loading={loading} t={t} />

      <div className="grid gap-8 xl:grid-cols-[1.3fr_0.7fr]">
        <SalesSection sales={data.sales} error={errors.sales} loading={loading} t={t} />
        <OrderStatusSection orders={data.orders} error={errors.orders} loading={loading} t={t} />
      </div>

      <div className="grid gap-8 xl:grid-cols-2">
        <TopBooksSection topBooks={data.topBooks} error={errors.topBooks} loading={loading} t={t} />
        <LowStockSection lowStock={data.lowStock} error={errors.lowStock} loading={loading} t={t} />
      </div>
    </div>
  );
}

function SummarySection({ summary, error, loading, t }) {
  const metrics = [
    ["revenue", t("admin.metricRevenue"), formatVND(summary?.revenue || 0)],
    ["orders", t("admin.metricOrderCount"), number(summary?.orderCount)],
    ["success", t("admin.metricSuccessfulPayments"), number(summary?.successfulPaymentCount)],
    ["failed", t("admin.metricFailedPayments"), number(summary?.failedPaymentCount)],
    ["users", t("admin.metricNewUsers"), number(summary?.newUserCount)],
    ["pendingOrders", t("admin.metricPendingOrders"), number(summary?.pendingOrderCount)],
    ["pendingPayments", t("admin.metricPendingPayments"), number(summary?.pendingPaymentCount)],
    ["lowStock", t("admin.metricLowStock"), number(summary?.lowStockCount)],
  ];

  return (
    <section className="grid gap-4">
      <SectionHeader title={t("admin.summaryMetrics")} />
      {error && <InlineError message={error} />}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metrics.map(([key, label, value]) => (
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" key={key}>
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">{label}</span>
            <strong className="mt-3 block text-2xl text-slate-950">{loading && !summary ? "..." : value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function SalesSection({ sales, error, loading, t }) {
  const points = sales?.points || [];
  const maxRevenue = Math.max(...points.map((point) => Number(point.revenue || 0)), 1);

  return (
    <Panel title={t("admin.salesTrend")} error={error}>
      {loading && points.length === 0 ? (
        <SkeletonRows />
      ) : points.length === 0 ? (
        <EmptyState>{t("admin.noSalesPoints")}</EmptyState>
      ) : (
        <div className="grid gap-3">
          {points.map((point) => {
            const width = Math.max(4, Math.round((Number(point.revenue || 0) / maxRevenue) * 100));
            return (
              <div className="grid gap-2 rounded-2xl bg-slate-50 p-4" key={point.date}>
                <div className="flex items-center justify-between gap-4 text-sm">
                  <span className="font-bold text-slate-700">{point.date}</span>
                  <span className="font-semibold text-slate-500">{t("admin.ordersValue", { count: number(point.orderCount) })}</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-200">
                  <div className="h-full rounded-full bg-blue-600" style={{ width: `${width}%` }} />
                </div>
                <strong className="text-slate-950">{formatVND(point.revenue)}</strong>
              </div>
            );
          })}
        </div>
      )}
    </Panel>
  );
}

function OrderStatusSection({ orders, error, loading, t }) {
  const rows = orders?.statusCounts || [];
  const maxCount = Math.max(...rows.map((row) => Number(row.count || 0)), 1);

  return (
    <Panel title={t("admin.orderStatusCounts")} error={error}>
      {loading && rows.length === 0 ? (
        <SkeletonRows />
      ) : rows.length === 0 ? (
        <EmptyState>{t("admin.noOrderStatusCounts")}</EmptyState>
      ) : (
        <div className="grid gap-3">
          {rows.map((row) => {
            const width = Math.max(4, Math.round((Number(row.count || 0) / maxCount) * 100));
            return (
              <div className="rounded-2xl bg-slate-50 p-4" key={row.status}>
                <div className="flex items-center justify-between gap-3 text-sm">
                  <span className="font-bold text-slate-700">{t(`orders.statusLabels.${row.status}`, { defaultValue: row.status })}</span>
                  <strong className="text-slate-950">{number(row.count)}</strong>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200">
                  <div className="h-full rounded-full bg-emerald-500" style={{ width: `${width}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </Panel>
  );
}

function TopBooksSection({ topBooks, error, loading, t }) {
  const books = topBooks?.books || [];

  return (
    <Panel title={t("admin.topBooks")} error={error}>
      {loading && books.length === 0 ? (
        <SkeletonRows />
      ) : books.length === 0 ? (
        <EmptyState>{t("admin.noTopBooks")}</EmptyState>
      ) : (
        <div className="grid gap-3">
          {books.map((book) => (
            <BookRow
              key={`${book.productId}-${book.sku}`}
              image={book.thumbnailUrl}
              title={book.productName}
              sku={book.sku}
              meta={t("admin.quantitySold", { count: number(book.quantitySold) })}
              value={formatVND(book.revenue)}
              to="/admin/products"
            />
          ))}
        </div>
      )}
    </Panel>
  );
}

function LowStockSection({ lowStock, error, loading, t }) {
  const books = lowStock?.books || [];

  return (
    <Panel title={t("admin.lowStockBooks")} error={error}>
      {loading && books.length === 0 ? (
        <SkeletonRows />
      ) : books.length === 0 ? (
        <EmptyState>{t("admin.noLowStockBooks")}</EmptyState>
      ) : (
        <div className="grid gap-3">
          {books.map((book) => (
            <BookRow
              key={`${book.productId}-${book.sku}`}
              image={book.thumbnailUrl}
              title={book.productName}
              sku={book.sku}
              meta={t("admin.stockLeft", { count: number(book.stockQuantity) })}
              value={book.slug ? t("admin.publicBookLink") : ""}
              to={book.slug ? `/product/${book.slug}` : "/admin/products"}
            />
          ))}
        </div>
      )}
    </Panel>
  );
}

function BookRow({ image, title, sku, meta, value, to }) {
  return (
    <Link className="grid grid-cols-[54px_1fr_auto] items-center gap-3 rounded-2xl bg-slate-50 p-3 transition-colors hover:bg-slate-100" to={to}>
      <img src={image || "https://placehold.co/120x180?text=Aivira"} alt={title} className="aspect-[2/3] w-full rounded-xl object-cover" />
      <div className="min-w-0">
        <p className="truncate font-bold text-slate-950">{title || "-"}</p>
        <p className="mt-1 truncate text-xs font-semibold text-slate-500">{sku || "-"}</p>
        <p className="mt-1 text-xs font-bold text-blue-600">{meta}</p>
      </div>
      <strong className="text-right text-sm text-slate-950">{value}</strong>
    </Link>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
        {eyebrow}
      </span>
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

function SectionHeader({ title }) {
  return <h3 className="font-serif text-3xl font-bold text-slate-950">{title}</h3>;
}

function Panel({ title, error, children }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <SectionHeader title={title} />
      {error && <InlineError message={error} />}
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Field({ label, children }) {
  return (
    <label className="grid gap-2 text-sm font-bold text-slate-600">
      {label}
      {children}
    </label>
  );
}

function NumberField({ label, value, onChange, min = 1 }) {
  return (
    <Field label={label}>
      <input
        type="number"
        min={min}
        max={MAX_LIMIT}
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
        className={ADMIN_INPUT}
      />
    </Field>
  );
}

function InlineError({ message }) {
  return (
    <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700">
      {message}
    </div>
  );
}

function EmptyState({ children }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-5 py-8 text-center text-sm font-bold text-slate-500">
      {children}
    </div>
  );
}

function SkeletonRows() {
  return (
    <div className="grid gap-3" aria-hidden="true">
      <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
      <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
      <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
    </div>
  );
}

function defaultDateRange() {
  const now = new Date();
  const from = new Date(now);
  from.setDate(now.getDate() - 30);
  return {
    fromDate: toDateInputValue(from),
    toDate: toDateInputValue(now),
  };
}

function toRangeParams(filters) {
  return {
    fromDate: startOfDayIso(filters.fromDate),
    toDate: endOfDayIso(filters.toDate),
  };
}

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

function startOfDayIso(value) {
  return new Date(`${value}T00:00:00`).toISOString();
}

function endOfDayIso(value) {
  return new Date(`${value}T23:59:59.999`).toISOString();
}

function resultValue(result) {
  return result.status === "fulfilled" ? result.value : null;
}

function resultError(result) {
  return result.status === "rejected" ? result.reason?.message || "Unavailable" : "";
}

function clamp(value, min, max) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return min;
  return Math.min(max, Math.max(min, Math.floor(numeric)));
}

function number(value) {
  return new Intl.NumberFormat().format(Number(value || 0));
}
