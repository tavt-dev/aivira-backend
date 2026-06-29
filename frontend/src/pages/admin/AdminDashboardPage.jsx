import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import {
  AlertTriangle, ArrowUpRight, BarChart3,
  Calendar, CheckCircle2, Clock, CreditCard,
  RefreshCw, ShoppingBag, TrendingUp, Users,
  AlertCircle, Star,
} from "lucide-react";

import {
  getDashboardLowStock, getDashboardOrders,
  getDashboardSales, getDashboardSummary, getDashboardTopBooks,
} from "../../api/adminDashboardApi.js";
import OrderStatusChart from "../../components/admin/OrderStatusChart.jsx";
import RevenueChart from "../../components/admin/RevenueChart.jsx";
import { formatVND } from "../../utils/formatters.js";

/* ── Constants ─────────────────────────────── */
const DEFAULT_TOP_LIMIT        = 10;
const DEFAULT_LOW_STOCK_LIMIT  = 10;
const DEFAULT_LOW_STOCK_THRESHOLD = 5;
const MAX_LIMIT = 50;

/* ── Dark admin token palette ───────────────── */
const tk = {
  pageBg:   "transparent",
  surface:  "var(--admin-surface)",
  surface2: "var(--admin-surface-soft)",
  surface3: "var(--admin-surface-muted)",
  heroLine: "linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
  border:   "var(--admin-border)",
  borderMid:"var(--admin-border-strong)",
  text1:    "var(--admin-text)",
  text2:    "var(--admin-text-muted)",
  text3:    "var(--admin-text-faint)",
  accent:   "#4f6ef7",
  emerald:  "#10d98a",
  gold:     "#f0a500",
  red:      "#ef4444",
  violet:   "#a78bfa",
  sky:      "#38bdf8",
};

/* ── Metric config ──────────────────────────── */
const METRIC_CFG = {
  revenue:              { icon:TrendingUp,    color:"#4f6ef7", bg:"rgba(79,110,247,0.14)",  border:"rgba(79,110,247,0.3)"  },
  orders:               { icon:ShoppingBag,   color:"#a78bfa", bg:"rgba(167,139,250,0.14)", border:"rgba(167,139,250,0.3)" },
  success:              { icon:CheckCircle2,  color:"#10d98a", bg:"rgba(16,217,138,0.12)",  border:"rgba(16,217,138,0.3)"  },
  failed:               { icon:AlertCircle,   color:"#ef4444", bg:"rgba(239,68,68,0.12)",   border:"rgba(239,68,68,0.3)"   },
  users:                { icon:Users,         color:"#38bdf8", bg:"rgba(56,189,248,0.12)",  border:"rgba(56,189,248,0.3)"  },
  pendingOrders:        { icon:Clock,         color:"#f0a500", bg:"rgba(240,165,0,0.14)",   border:"rgba(240,165,0,0.3)"   },
  pendingPayments:      { icon:CreditCard,    color:"#f0a500", bg:"rgba(240,165,0,0.14)",   border:"rgba(240,165,0,0.3)"   },
  lowStock:             { icon:AlertTriangle, color:"#ef4444", bg:"rgba(239,68,68,0.10)",   border:"rgba(239,68,68,0.25)"  },
};

const ORDER_STATUS_COLOR = {
  PENDING_CONFIRMATION: "#f0a500",
  PENDING_PAYMENT: "#f97316",
  PAID: "#10d98a",
  CONFIRMED: "#4f6ef7",
  PACKING: "#a78bfa",
  SHIPPING: "#38bdf8",
  COMPLETED: "#059669",
  CANCELLED: "#ef4444",
  PAYMENT_FAILED: "#e11d48",
  EXPIRED: "#64748b",
  REFUNDED: "#a855f7",
};

/* ── Order status palette ───────────────────── */
/* ══════════════════════════════════════════════
   DASHBOARD PAGE
══════════════════════════════════════════════ */
export default function AdminDashboardPage() {
  const { t } = useTranslation();
  const defaults = useMemo(() => defaultDateRange(), []);
  const [filters, setFilters] = useState({
    fromDate: defaults.fromDate,
    toDate:   defaults.toDate,
    topLimit: DEFAULT_TOP_LIMIT,
    lowStockLimit: DEFAULT_LOW_STOCK_LIMIT,
    lowStockThreshold: DEFAULT_LOW_STOCK_THRESHOLD,
  });
  const [data, setData]     = useState({ summary:null, sales:null, orders:null, topBooks:null, lowStock:null });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [spinning, setSpinning] = useState(false);

  useEffect(() => {
    let ignore = false;
    const rp = toRangeParams(filters);
    const topLimit = clamp(filters.topLimit,1,MAX_LIMIT);
    const lowStockLimit = clamp(filters.lowStockLimit,1,MAX_LIMIT);
    const lowStockThreshold = Math.max(0, Number(filters.lowStockThreshold||0));

    setLoading(true);
    Promise.allSettled([
      getDashboardSummary(rp),
      getDashboardSales(rp),
      getDashboardOrders(rp),
      getDashboardTopBooks({ ...rp, limit: topLimit }),
      getDashboardLowStock({ threshold: lowStockThreshold, limit: lowStockLimit }),
    ]).then(([summary,sales,orders,topBooks,lowStock]) => {
      if (ignore) return;
      setData({ summary:resultValue(summary), sales:resultValue(sales), orders:resultValue(orders), topBooks:resultValue(topBooks), lowStock:resultValue(lowStock) });
      setErrors({ summary:resultError(summary), sales:resultError(sales), orders:resultError(orders), topBooks:resultError(topBooks), lowStock:resultError(lowStock) });
    }).finally(() => { if (!ignore) setLoading(false); });
    return () => { ignore = true; };
  }, [filters, refreshKey]);

  function handleRefresh() {
    setSpinning(true);
    setTimeout(() => setSpinning(false), 800);
    setRefreshKey(k => k+1);
  }

  return (
    <div className="mx-auto grid w-full max-w-7xl gap-6">

      {/* ── Hero + Filter bar ── */}
      <motion.div initial={{opacity:0,y:-16}} animate={{opacity:1,y:0}}
        transition={{duration:0.5,ease:[0.22,1,0.36,1]}}
        className="relative overflow-hidden rounded-2xl shadow-sm"
        style={{ background:tk.surface, border:`1px solid ${tk.border}` }}>
        <div className="absolute left-0 right-0 top-0 h-[1.5px]" style={{ background:tk.heroLine }}/>
        <div className="pointer-events-none absolute -right-12 -top-12 h-48 w-48 rounded-full"
          style={{ background:"radial-gradient(circle,rgba(79,110,247,0.15) 0%,transparent 70%)" }}/>

        <div className="relative flex flex-wrap items-center gap-4 px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 6px 20px rgba(79,110,247,0.45)" }}>
              <BarChart3 size={18} color="#fff"/>
            </div>
            <div>
              <p className="text-[0.55rem] font-black uppercase tracking-[0.2em]" style={{ color:tk.text3 }}>
                {t("admin.dashboardEyebrow","Tổng quan")}
              </p>
              <h1 className="text-xl font-bold tracking-tight" style={{ color:tk.text1 }}>
                {t("admin.dashboardTitle","Dashboard")}
              </h1>
            </div>
          </div>

          {/* Date range */}
          <div className="flex flex-wrap items-center gap-3 ml-auto">
            <FilterLabel label={t("admin.fromDate","Từ ngày")}>
              <DateInput value={filters.fromDate} onChange={v => setFilters(f => ({...f,fromDate:v}))}/>
            </FilterLabel>
            <FilterLabel label={t("admin.toDate","Đến ngày")}>
              <DateInput value={filters.toDate} onChange={v => setFilters(f => ({...f,toDate:v}))}/>
            </FilterLabel>
            <FilterLabel label={t("admin.topLimit","Top sách")}>
              <NumInput value={filters.topLimit} min={1} max={MAX_LIMIT}
                onChange={v => setFilters(f => ({...f,topLimit:clamp(v,1,MAX_LIMIT)}))}/>
            </FilterLabel>
            <FilterLabel label={t("admin.lowStockThreshold","Ngưỡng tồn kho")}>
              <NumInput value={filters.lowStockThreshold} min={0} max={MAX_LIMIT}
                onChange={v => setFilters(f => ({...f,lowStockThreshold:Math.max(0,v)}))}/>
            </FilterLabel>
            <FilterLabel label={t("admin.lowStockLimit","Limit tồn kho")}>
              <NumInput value={filters.lowStockLimit} min={1} max={MAX_LIMIT}
                onChange={v => setFilters(f => ({...f,lowStockLimit:clamp(v,1,MAX_LIMIT)}))}/>
            </FilterLabel>
            <motion.button whileHover={{scale:1.04,y:-1}} whileTap={{scale:0.96}}
              type="button" onClick={handleRefresh} disabled={loading}
              className="flex items-center gap-2 self-end rounded-lg px-4 py-2 text-sm font-semibold text-white shadow-sm transition disabled:cursor-not-allowed disabled:opacity-50"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 6px 20px rgba(79,110,247,0.4)" }}>
              <motion.div animate={spinning?{rotate:360}:{rotate:0}} transition={spinning?{duration:0.8,repeat:Infinity,ease:"linear"}:{}}>
                <RefreshCw size={14}/>
              </motion.div>
              {loading ? t("admin.dashboardLoading","Đang tải…") : t("admin.refresh","Làm mới")}
            </motion.button>
          </div>
        </div>
      </motion.div>

      {/* ── Summary KPI cards ── */}
      <SummarySection summary={data.summary} error={errors.summary} loading={loading} t={t}/>

      {/* ── Sales + Order status ── */}
      <div className="grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
        <SalesSection sales={data.sales} error={errors.sales} loading={loading} t={t}/>
        <OrderStatusSection orders={data.orders} error={errors.orders} loading={loading} t={t}/>
      </div>

      {/* ── Top books + Low stock ── */}
      <div className="grid gap-6 xl:grid-cols-2">
        <TopBooksSection topBooks={data.topBooks} error={errors.topBooks} loading={loading} t={t}/>
        <LowStockSection lowStock={data.lowStock} error={errors.lowStock} loading={loading} t={t}/>
      </div>
    </div>
  );
}

/* ── Summary Section ────────────────────────── */
function SummarySection({ summary, error, loading, t }) {
  const metrics = [
    ["revenue",         t("admin.metricRevenue","Doanh thu"),           formatVND(summary?.revenue||0)],
    ["orders",          t("admin.metricOrderCount","Đơn hàng"),         number(summary?.orderCount)],
    ["success",         t("admin.metricSuccessfulPayments","TT thành công"), number(summary?.successfulPaymentCount)],
    ["failed",          t("admin.metricFailedPayments","TT thất bại"),  number(summary?.failedPaymentCount)],
    ["users",           t("admin.metricNewUsers","Người dùng mới"),     number(summary?.newUserCount)],
    ["pendingOrders",   t("admin.metricPendingOrders","Chờ xác nhận"), number(summary?.pendingOrderCount)],
    ["pendingPayments", t("admin.metricPendingPayments","Chờ TT"),      number(summary?.pendingPaymentCount)],
    ["lowStock",        t("admin.metricLowStock","Sắp hết hàng"),       number(summary?.lowStockCount)],
  ];

  return (
    <section className="grid gap-4">
      {error && <ErrorBanner>{error}</ErrorBanner>}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metrics.map(([key, label, value], i) => {
          const cfg = METRIC_CFG[key]||METRIC_CFG.revenue;
          const { icon:Icon, color, bg, border } = cfg;
          return (
            <motion.div key={key}
              initial={{opacity:0,y:20,scale:0.96}} animate={{opacity:1,y:0,scale:1}}
              transition={{duration:0.45,delay:i*0.05,ease:[0.22,1,0.36,1]}}
              whileHover={{y:-3,boxShadow:`0 16px 40px ${bg.replace("0.14","0.28").replace("0.12","0.25").replace("0.10","0.2")}`}}
              className="relative overflow-hidden rounded-2xl p-5 shadow-sm transition-all"
              style={{ background:tk.surface, border:`1px solid ${border}` }}>
              {/* Top glow line */}
              <div className="absolute left-0 right-0 top-0 h-[1px]"
                style={{ background:`linear-gradient(90deg,transparent,${color} 50%,transparent)` }}/>
              {/* BG glow orb */}
              <div className="pointer-events-none absolute -right-4 -top-4 h-20 w-20 rounded-full"
                style={{ background:`radial-gradient(circle,${bg} 0%,transparent 70%)` }}/>
              <div className="relative">
                <div className="mb-4 flex items-center justify-between">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl"
                    style={{ background:bg, border:`1px solid ${border}` }}>
                    <Icon size={16} style={{ color }}/>
                  </div>
                  {key==="revenue" && (
                    <span className="flex items-center gap-1 rounded-full px-2 py-1 text-[0.6rem] font-black"
                      style={{ background:"rgba(16,217,138,0.12)", color:"#10d98a" }}>
                      <TrendingUp size={9}/> LIVE
                    </span>
                  )}
                </div>
                <p className="text-[0.65rem] font-black uppercase tracking-[0.14em]" style={{ color:tk.text3 }}>{label}</p>
                <motion.strong
                  key={value}
                  initial={{scale:1.08}} animate={{scale:1}}
                  transition={{type:"spring",stiffness:400,damping:20}}
                  className="mt-1.5 block text-2xl font-bold tracking-tight"
                  style={{ color:tk.text1 }}>
                  {loading&&!summary?"—":value}
                </motion.strong>
              </div>
            </motion.div>
          );
        })}
      </div>
    </section>
  );
}

/* ── Sales Section ──────────────────────────── */
function SalesSection({ sales, error, loading, t }) {
  const points = sales?.points||[];
  const maxRevenue = Math.max(...points.map(p => Number(p.revenue||0)), 1);

  return (
    <DashPanel title={t("admin.salesTrend","Xu hướng doanh thu")} icon={TrendingUp}>
      {error && <ErrorBanner>{error}</ErrorBanner>}
      {loading && points.length===0 ? <DashSkeleton rows={5}/>
        : points.length===0 ? <EmptyBlock>{t("admin.noSalesPoints","Không có dữ liệu")}</EmptyBlock>
        : (
          <div className="grid gap-4">
            <RevenueChart points={points} t={t}/>
            {points.map((p, i) => {
              const pct = Math.max(4, Math.round((Number(p.revenue||0)/maxRevenue)*100));
              return (
                <motion.div key={p.date}
                  initial={{opacity:0,x:-16}} animate={{opacity:1,x:0}}
                  transition={{duration:0.35,delay:i*0.04}}
                  className="overflow-hidden rounded-2xl p-4"
                  style={{ background:tk.surface2, border:`1px solid ${tk.border}` }}>
                  <div className="mb-2.5 flex items-center justify-between gap-4">
                    <span className="flex items-center gap-2 text-sm font-bold" style={{ color:tk.text1 }}>
                      <Calendar size={12} style={{ color:tk.text3 }}/>{p.date}
                    </span>
                    <span className="text-xs font-semibold" style={{ color:tk.text3 }}>
                      {t("admin.ordersValue","{{count}} đơn",{ count:number(p.orderCount) })}
                    </span>
                  </div>
                  {/* Bar */}
                  <div className="h-1.5 overflow-hidden rounded-full" style={{ background:"var(--admin-track)" }}>
                    <motion.div
                      initial={{width:0}} animate={{width:`${pct}%`}}
                      transition={{duration:0.6,delay:i*0.04,ease:[0.22,1,0.36,1]}}
                      className="h-full rounded-full"
                      style={{ background:"linear-gradient(90deg,#2a3ecc,#4f6ef7,#a78bfa)" }}/>
                  </div>
                  <strong className="mt-2 block text-sm font-black" style={{ color:tk.accent }}>
                    {formatVND(p.revenue)}
                  </strong>
                </motion.div>
              );
            })}
          </div>
        )
      }
    </DashPanel>
  );
}

/* ── Order Status Section ───────────────────── */
function OrderStatusSection({ orders, error, loading, t }) {
  const rows = orders?.statusCounts||[];
  const maxCount = Math.max(...rows.map(r => Number(r.count||0)), 1);

  return (
    <DashPanel title={t("admin.orderStatusCounts","Trạng thái đơn")} icon={ShoppingBag}>
      {error && <ErrorBanner>{error}</ErrorBanner>}
      {loading && rows.length===0 ? <DashSkeleton rows={4}/>
        : rows.length===0 ? <EmptyBlock>{t("admin.noOrderStatusCounts","Không có dữ liệu")}</EmptyBlock>
        : (
          <div className="grid gap-4">
            <OrderStatusChart rows={rows} t={t}/>
            {rows.map((row, i) => {
              const pct = Math.max(4, Math.round((Number(row.count||0)/maxCount)*100));
              const color = ORDER_STATUS_COLOR[row.status]||tk.text3;
              return (
                <motion.div key={row.status}
                  initial={{opacity:0,x:16}} animate={{opacity:1,x:0}}
                  transition={{duration:0.35,delay:i*0.04}}
                  className="overflow-hidden rounded-2xl p-4"
                  style={{ background:tk.surface2, border:`1px solid ${tk.border}` }}>
                  <div className="mb-2.5 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full" style={{ background:color }}/>
                      <span className="text-xs font-bold" style={{ color:tk.text2 }}>
                        {t(`orders.statusLabels.${row.status}`,{defaultValue:row.status})}
                      </span>
                    </div>
                    <strong className="text-sm font-black" style={{ color:tk.text1 }}>{number(row.count)}</strong>
                  </div>
                  <div className="h-1.5 overflow-hidden rounded-full" style={{ background:"var(--admin-track)" }}>
                    <motion.div
                      initial={{width:0}} animate={{width:`${pct}%`}}
                      transition={{duration:0.6,delay:i*0.04,ease:[0.22,1,0.36,1]}}
                      className="h-full rounded-full"
                      style={{ background:color }}/>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )
      }
    </DashPanel>
  );
}

/* ── Top Books Section ──────────────────────── */
function TopBooksSection({ topBooks, error, loading, t }) {
  const books = topBooks?.books||[];
  return (
    <DashPanel title={t("admin.topBooks","Sách bán chạy")} icon={Star} linkTo="/admin/products">
      {error && <ErrorBanner>{error}</ErrorBanner>}
      {loading && books.length===0 ? <DashSkeleton rows={5}/>
        : books.length===0 ? <EmptyBlock>{t("admin.noTopBooks","Không có dữ liệu")}</EmptyBlock>
        : (
          <div className="grid gap-2.5">
            {books.map((b,i) => (
              <BookRow key={`${b.productId}-${b.sku}`}
                rank={i+1} image={b.thumbnailUrl} title={b.productName} sku={b.sku}
                meta={t("admin.quantitySold","{{count}} đã bán",{count:number(b.quantitySold)})}
                value={formatVND(b.revenue)} to="/admin/products"/>
            ))}
          </div>
        )
      }
    </DashPanel>
  );
}

/* ── Low Stock Section ──────────────────────── */
function LowStockSection({ lowStock, error, loading, t }) {
  const books = lowStock?.books||[];
  return (
    <DashPanel title={t("admin.lowStockBooks","Sắp hết hàng")} icon={AlertTriangle} danger>
      {error && <ErrorBanner>{error}</ErrorBanner>}
      {loading && books.length===0 ? <DashSkeleton rows={5}/>
        : books.length===0 ? <EmptyBlock>{t("admin.noLowStockBooks","Không có sách sắp hết hàng")}</EmptyBlock>
        : (
          <div className="grid gap-2.5">
            {books.map((b,i) => (
              <BookRow key={`${b.productId}-${b.sku}`}
                rank={i+1} image={b.thumbnailUrl} title={b.productName} sku={b.sku}
                meta={t("admin.stockLeft","Còn {{count}} cuốn",{count:number(b.stockQuantity)})}
                value={b.slug?t("admin.publicBookLink","Xem sách"):""}
                to={b.slug?`/product/${b.slug}`:"/admin/products"}
                warn/>
            ))}
          </div>
        )
      }
    </DashPanel>
  );
}

/* ── BookRow ────────────────────────────────── */
function BookRow({ rank, image, title, sku, meta, value, to, warn }) {
  return (
    <motion.div whileHover={{x:3}} transition={{duration:0.2}}>
      <Link to={to}
        className="grid items-center gap-3 overflow-hidden rounded-2xl p-3 transition-all hover:opacity-90"
        style={{
          background:tk.surface2, border:`1px solid ${tk.border}`,
          gridTemplateColumns:"24px 52px 1fr auto",
        }}>
        {/* Rank */}
        <span className="text-center text-xs font-black" style={{ color:tk.text3 }}>#{rank}</span>
        {/* Cover */}
        <div className="overflow-hidden rounded-xl" style={{ aspectRatio:"2/3", boxShadow:"0 4px 12px rgba(0,0,0,0.4)" }}>
          <img src={image||"https://placehold.co/120x180?text=📚"} alt={title}
            className="h-full w-full object-cover"/>
        </div>
        {/* Info */}
        <div className="min-w-0">
          <p className="truncate text-sm font-bold" style={{ color:tk.text1 }}>{title||"—"}</p>
          <p className="mt-0.5 truncate text-[0.65rem] font-semibold" style={{ color:tk.text3 }}>{sku||"—"}</p>
          <p className="mt-1 text-[0.65rem] font-black uppercase tracking-wider"
            style={{ color: warn?tk.red:tk.accent }}>{meta}</p>
        </div>
        {/* Value */}
        <div className="flex items-center gap-1 text-right">
          <strong className="text-sm font-black" style={{ color:tk.text1 }}>{value}</strong>
          <ArrowUpRight size={12} style={{ color:tk.text3 }}/>
        </div>
      </Link>
    </motion.div>
  );
}

/* ── DashPanel ──────────────────────────────── */
function DashPanel({ title, icon:Icon, children, danger, linkTo }) {
  return (
    <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}}
      transition={{duration:0.45,ease:[0.22,1,0.36,1]}}
      className="overflow-hidden rounded-2xl shadow-sm"
      style={{
        background:tk.surface,
        border:`1px solid ${danger?"rgba(239,68,68,0.25)":tk.border}`,
      }}>
      <div className="h-[1.5px]"
        style={{ background: danger?"linear-gradient(90deg,transparent,#ef4444 40%,transparent)":tk.heroLine }}/>
      <div className="flex items-center gap-3 border-b px-5 py-4" style={{ borderColor:tk.border, background:tk.surface2 }}>
        <div className="flex h-8 w-8 items-center justify-center rounded-lg"
          style={{ background:danger?"rgba(239,68,68,0.12)":"rgba(79,110,247,0.12)", color:danger?tk.red:tk.accent }}>
          <Icon size={14}/>
        </div>
        <h2 className="text-sm font-bold" style={{ color:tk.text1 }}>{title}</h2>
        {linkTo && (
          <Link to={linkTo} className="ml-auto flex items-center gap-1 text-xs font-bold hover:opacity-70 transition-opacity"
            style={{ color:tk.accent }}>
            Xem thêm <ArrowUpRight size={11}/>
          </Link>
        )}
      </div>
      <div className="p-5">{children}</div>
    </motion.div>
  );
}

/* ── Filter helpers ─────────────────────────── */
function FilterLabel({ label, children }) {
  return (
    <label className="grid gap-1">
      <span className="text-[0.6rem] font-black uppercase tracking-[0.14em]" style={{ color:tk.text3 }}>{label}</span>
      {children}
    </label>
  );
}

function DateInput({ value, onChange }) {
  return (
    <input type="date" value={value} onChange={e => onChange(e.target.value)}
      className="rounded-lg px-3 py-2 text-sm font-semibold outline-none transition-all focus:ring-2 focus:ring-indigo-500/30"
      style={{
        background:tk.surface, border:`1px solid ${tk.borderMid}`,
        color:tk.text1,
      }}/>
  );
}

function NumInput({ value, min, max, onChange }) {
  return (
    <input type="number" min={min} max={max} value={value}
      onChange={e => onChange(Number(e.target.value))}
      className="w-20 rounded-lg px-3 py-2 text-sm font-semibold outline-none transition-all focus:ring-2 focus:ring-indigo-500/30"
      style={{
        background:tk.surface, border:`1px solid ${tk.borderMid}`,
        color:tk.text1,
      }}/>
  );
}

/* ── Skeleton ───────────────────────────────── */
function DashSkeleton({ rows }) {
  const sh = { "--sa":"var(--admin-surface-soft)","--sb":"rgba(79,110,247,0.10)","--sc":"rgba(167,139,250,0.08)" };
  return (
    <div className="grid gap-2.5">
      {[...Array(rows)].map((_,i) => (
        <div key={i} className="catalog-dynamic-shimmer rounded-2xl"
          style={{...sh,height:68,animationDelay:`${i*60}ms`}}/>
      ))}
    </div>
  );
}

/* ── Misc ───────────────────────────────────── */
function EmptyBlock({ children }) {
  return (
    <div className="rounded-2xl px-5 py-10 text-center text-sm font-bold"
      style={{ background:tk.surface2, border:`1px dashed ${tk.border}`, color:tk.text3 }}>
      {children}
    </div>
  );
}

function ErrorBanner({ children }) {
  return (
    <div className="mb-3 flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-bold"
      style={{ background:"rgba(239,68,68,0.10)", border:"1px solid rgba(239,68,68,0.28)", color:"#ef4444" }}>
      <AlertCircle size={14}/> {children}
    </div>
  );
}

/* ── Date / number utils ────────────────────── */
function defaultDateRange() {
  const now = new Date(); const from = new Date(now); from.setDate(now.getDate()-30);
  return { fromDate: toDateInputValue(from), toDate: toDateInputValue(now) };
}
function toRangeParams(f) { return { fromDate: startOfDayIso(f.fromDate), toDate: endOfDayIso(f.toDate) }; }
function toDateInputValue(d) { return d.toISOString().slice(0,10); }
function startOfDayIso(v) { return new Date(`${v}T00:00:00`).toISOString(); }
function endOfDayIso(v) { return new Date(`${v}T23:59:59.999`).toISOString(); }
function resultValue(r) { return r.status==="fulfilled"?r.value:null; }
function resultError(r) { return r.status==="rejected"?r.reason?.message||"Unavailable":""; }
function clamp(v,min,max) { const n=Number(v); return !Number.isFinite(n)?min:Math.min(max,Math.max(min,Math.floor(n))); }
function number(v) { return new Intl.NumberFormat().format(Number(v||0)); }
