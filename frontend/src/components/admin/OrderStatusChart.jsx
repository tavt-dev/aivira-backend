import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

const STATUS_COLORS = {
  PENDING_CONFIRMATION: "#f59e0b",
  PENDING_PAYMENT: "#f97316",
  PAID: "#10b981",
  CONFIRMED: "#4f46e5",
  PACKING: "#8b5cf6",
  SHIPPING: "#0ea5e9",
  COMPLETED: "#059669",
  CANCELLED: "#ef4444",
  PAYMENT_FAILED: "#e11d48",
  EXPIRED: "#64748b",
  REFUNDED: "#a855f7",
};

export default function OrderStatusChart({ rows = [], t }) {
  const data = rows.map((row, index) => ({
    status: row.status,
    name: t(`orders.statusLabels.${row.status}`, { defaultValue: row.status }),
    value: Number(row.count || 0),
    color: STATUS_COLORS[row.status] || fallbackColor(index),
  }));
  const total = data.reduce((sum, item) => sum + item.value, 0);

  return (
    <div role="img" aria-label={t("admin.orderStatusCounts", "Order status distribution")}>
      <div className="relative h-[250px] w-full">
        <ResponsiveContainer height="100%" minHeight={1} minWidth={1} width="100%">
          <PieChart>
            <Pie
              animationDuration={700}
              cornerRadius={5}
              data={data}
              dataKey="value"
              innerRadius="62%"
              nameKey="name"
              outerRadius="86%"
              paddingAngle={3}
              stroke="none"
            >
              {data.map((item) => <Cell fill={item.color} key={item.status}/>) }
            </Pie>
            <Tooltip content={<StatusTooltip/>}/>
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-center">
          <strong className="text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-50">{total.toLocaleString()}</strong>
          <span className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">{t("admin.metricOrderCount", "Orders")}</span>
        </div>
      </div>
      <div className="grid gap-2 border-t border-slate-100 pt-4 dark:border-slate-800 sm:grid-cols-2">
        {data.map((item) => (
          <div className="flex min-w-0 items-center gap-2 text-xs" key={item.status}>
            <span className="h-2.5 w-2.5 flex-shrink-0 rounded-full" style={{ backgroundColor:item.color }}/>
            <span className="truncate font-medium text-slate-500 dark:text-slate-400">{item.name}</span>
            <strong className="ml-auto text-slate-800 dark:text-slate-200">{item.value.toLocaleString()}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function StatusTooltip({ active, payload = [] }) {
  if (!active || !payload.length) return null;
  const item = payload[0]?.payload;
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm shadow-xl dark:border-slate-700 dark:bg-slate-800">
      <div className="flex items-center gap-2">
        <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor:item.color }}/>
        <span className="text-slate-500 dark:text-slate-400">{item.name}</span>
        <strong className="text-slate-900 dark:text-slate-100">{item.value.toLocaleString()}</strong>
      </div>
    </div>
  );
}

function fallbackColor(index) {
  return ["#6366f1", "#14b8a6", "#f59e0b", "#ec4899", "#06b6d4"][index % 5];
}
