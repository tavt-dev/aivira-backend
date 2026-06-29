import { useId } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { formatVND } from "../../utils/formatters.js";

export default function RevenueChart({ points = [], t }) {
  const gradientId = `revenue-${useId().replace(/:/g, "")}`;
  const data = points.map((point) => ({
    date: point.date,
    revenue: Number(point.revenue || 0),
    orders: Number(point.orderCount || 0),
  }));

  return (
    <div className="h-[320px] w-full" role="img" aria-label={t("admin.salesTrend", "Revenue trend")}>
      <ResponsiveContainer height="100%" minHeight={1} minWidth={1} width="100%">
        <AreaChart data={data} margin={{ top: 12, right: 4, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.32}/>
              <stop offset="95%" stopColor="#4f46e5" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} stroke="var(--admin-border)" strokeDasharray="4 4"/>
          <XAxis
            axisLine={false}
            dataKey="date"
            minTickGap={28}
            tick={{ fill: "var(--admin-text-muted)", fontSize: 11 }}
            tickFormatter={shortDate}
            tickLine={false}
          />
          <YAxis
            axisLine={false}
            tick={{ fill: "var(--admin-text-muted)", fontSize: 11 }}
            tickFormatter={compactMoney}
            tickLine={false}
            width={58}
          />
          <YAxis
            allowDecimals={false}
            axisLine={false}
            dataKey="orders"
            orientation="right"
            tick={{ fill: "var(--admin-text-muted)", fontSize: 11 }}
            tickLine={false}
            width={28}
            yAxisId="orders"
          />
          <Tooltip content={<RevenueTooltip t={t}/>}/>
          <Area
            animationDuration={700}
            dataKey="revenue"
            fill={`url(#${gradientId})`}
            fillOpacity={1}
            name={t("admin.metricRevenue", "Revenue")}
            stroke="#4f46e5"
            strokeWidth={3}
            type="monotone"
          />
          <Line
            animationDuration={700}
            dataKey="orders"
            dot={false}
            name={t("admin.metricOrderCount", "Orders")}
            stroke="#a855f7"
            strokeDasharray="5 5"
            strokeWidth={2}
            type="monotone"
            yAxisId="orders"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

function RevenueTooltip({ active, label, payload = [], t }) {
  if (!active || !payload.length) return null;
  const row = payload[0]?.payload || {};
  return (
    <div className="min-w-44 rounded-xl border border-slate-200 bg-white p-3 text-sm shadow-xl dark:border-slate-700 dark:bg-slate-800">
      <p className="mb-2 font-semibold text-slate-900 dark:text-slate-100">{formatDateLabel(label)}</p>
      <p className="flex items-center justify-between gap-4 text-slate-500 dark:text-slate-400">
        <span>{t("admin.metricRevenue", "Revenue")}</span>
        <strong className="text-slate-900 dark:text-slate-100">{formatVND(row.revenue)}</strong>
      </p>
      <p className="mt-1 flex items-center justify-between gap-4 text-slate-500 dark:text-slate-400">
        <span>{t("admin.metricOrderCount", "Orders")}</span>
        <strong className="text-violet-600 dark:text-violet-400">{row.orders || 0}</strong>
      </p>
    </div>
  );
}

function shortDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value || "") : date.toLocaleDateString(undefined, { day: "2-digit", month: "2-digit" });
}

function formatDateLabel(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value || "") : date.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
}

function compactMoney(value) {
  const amount = Number(value || 0);
  if (Math.abs(amount) >= 1_000_000_000) return `${(amount / 1_000_000_000).toFixed(1)}B`;
  if (Math.abs(amount) >= 1_000_000) return `${(amount / 1_000_000).toFixed(1)}M`;
  if (Math.abs(amount) >= 1_000) return `${Math.round(amount / 1_000)}K`;
  return String(amount);
}
