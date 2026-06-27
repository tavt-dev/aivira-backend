import { createContext, forwardRef, useCallback, useContext, useEffect, useId, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, X } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

function cx(...parts) {
  return parts.filter(Boolean).join(" ");
}

/* ══════════════════════════════════════════════
   BUTTON
══════════════════════════════════════════════ */
const buttonVariants = {
  primary:   "bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm shadow-indigo-200 dark:shadow-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-600 dark:focus:ring-offset-slate-950",
  secondary: "border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800",
  danger:    "border border-rose-200 dark:border-rose-500/30 bg-rose-50 dark:bg-rose-500/10 text-rose-700 dark:text-rose-400 hover:bg-rose-100 dark:hover:bg-rose-500/20",
  ghost:     "text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800",
};

const buttonSizes = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
};

export const Button = forwardRef(function Button(
  { children, className = "", loading = false, size = "md", variant = "primary", ...props },
  ref
) {
  return (
    <button
      {...props}
      ref={ref}
      className={cx(
        "inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        buttonVariants[variant] || buttonVariants.primary,
        buttonSizes[size] || buttonSizes.md,
        className
      )}
    >
      {loading && <span className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden="true"/>}
      <span>{children}</span>
    </button>
  );
});

export const IconButton = forwardRef(function IconButton(
  { "aria-label": ariaLabel, children, className = "", variant = "ghost", ...props },
  ref
) {
  if (!ariaLabel) throw new Error("IconButton requires an aria-label.");
  return (
    <button
      {...props}
      ref={ref}
      aria-label={ariaLabel}
      className={cx(
        "inline-flex h-9 w-9 items-center justify-center rounded-lg transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        buttonVariants[variant] || buttonVariants.ghost,
        className
      )}
    >
      {children}
    </button>
  );
});

/* ══════════════════════════════════════════════
   FORM CONTROLS
══════════════════════════════════════════════ */
function Field({ children, error, hint, id, label, required }) {
  if (!label && !error && !hint) return children;
  return (
    <label className="grid gap-1.5" htmlFor={id}>
      {label && (
        <span className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
          {label}
          {required && <span className="text-rose-500"> *</span>}
        </span>
      )}
      {children}
      {hint && !error && <span className="text-xs font-semibold text-slate-400">{hint}</span>}
      {error && <span className="text-xs font-semibold text-rose-600" id={`${id}-error`}>{error}</span>}
    </label>
  );
}

const controlClass =
  "w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none transition-shadow focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:bg-slate-50 dark:disabled:bg-slate-900 disabled:cursor-not-allowed disabled:opacity-60";

export const Input = forwardRef(function Input(
  { className = "", error, hint, id, label, required, ...props },
  ref
) {
  const fallbackId = useId();
  const fieldId = id || fallbackId;
  return (
    <Field error={error} hint={hint} id={fieldId} label={label} required={required}>
      <input
        {...props}
        ref={ref}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        aria-invalid={error ? "true" : undefined}
        className={cx(controlClass, className)}
        id={fieldId}
        required={required}
      />
    </Field>
  );
});

export const DateTimeInput = forwardRef(function DateTimeInput(props, ref) {
  return <Input {...props} ref={ref} type="datetime-local"/>;
});

export const Textarea = forwardRef(function Textarea(
  { className = "", error, hint, id, label, required, ...props },
  ref
) {
  const fallbackId = useId();
  const fieldId = id || fallbackId;
  return (
    <Field error={error} hint={hint} id={fieldId} label={label} required={required}>
      <textarea
        {...props}
        ref={ref}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        aria-invalid={error ? "true" : undefined}
        className={cx("min-h-[6rem] resize-none", controlClass, className)}
        id={fieldId}
        required={required}
      />
    </Field>
  );
});

export const Select = forwardRef(function Select(
  { children, className = "", error, hint, id, label, required, ...props },
  ref
) {
  const fallbackId = useId();
  const fieldId = id || fallbackId;
  return (
    <Field error={error} hint={hint} id={fieldId} label={label} required={required}>
      <select
        {...props}
        ref={ref}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        aria-invalid={error ? "true" : undefined}
        className={cx(controlClass, className)}
        id={fieldId}
        required={required}
      >
        {children}
      </select>
    </Field>
  );
});

export function Checkbox({ children, className = "", ...props }) {
  return (
    <label className={cx("inline-flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-300", className)}>
      <input {...props} className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500" type="checkbox"/>
      {children && <span>{children}</span>}
    </label>
  );
}

export function Toggle({ checked, children, onChange, ...props }) {
  return (
    <button
      {...props}
      aria-pressed={checked}
      className={cx(
        "inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition-colors",
        checked
          ? "border-indigo-200 dark:border-indigo-500/30 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-400"
          : "border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800"
      )}
      type="button"
      onClick={() => onChange?.(!checked)}
    >
      <span className={cx("h-3 w-3 rounded-full transition-colors", checked ? "bg-indigo-600" : "bg-slate-300 dark:bg-slate-600")}/>
      {children}
    </button>
  );
}

/* ══════════════════════════════════════════════
   BADGES
══════════════════════════════════════════════ */
const badgeVariants = {
  neutral: "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200/60 dark:border-slate-700",
  success: "bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-200/60 dark:border-emerald-500/20",
  warning: "bg-amber-50 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-200/60 dark:border-amber-500/20",
  danger:  "bg-rose-50 dark:bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-200/60 dark:border-rose-500/20",
  info:    "bg-sky-50 dark:bg-sky-500/10 text-sky-700 dark:text-sky-400 border-sky-200/60 dark:border-sky-500/20",
};

export function Badge({ children, className = "", variant = "neutral" }) {
  return (
    <span className={cx(
      "inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold",
      badgeVariants[variant] || badgeVariants.neutral,
      className
    )}>
      {children}
    </span>
  );
}

export function StatusPill({ status, type = "generic" }) {
  const text = status || "—";
  const value = String(status || "").toUpperCase();
  let variant = "neutral";
  if (["ACTIVE","SUCCESS","COMPLETED","APPROVED","VISIBLE","PAID"].includes(value)) variant = "success";
  if (["PENDING","PENDING_PAYMENT","PENDING_CONFIRMATION","CONFIRMED","PACKING","SHIPPING"].includes(value)) variant = "warning";
  if (["FAILED","PAYMENT_FAILED","CANCELLED","EXPIRED","REJECTED","HIDDEN","LOCKED"].includes(value)) variant = "danger";
  if (type === "payment" && value === "REFUNDED") variant = "info";
  return <Badge variant={variant}>{text}</Badge>;
}

/* ══════════════════════════════════════════════
   LAYOUT COMPONENTS
══════════════════════════════════════════════ */
export function Panel({ children, className = "", title }) {
  return (
    <section className={cx(
      "bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden",
      className
    )}>
      {title && (
        <div className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h3>
        </div>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}

export function PageHeader({ eyebrow, title }) {
  return (
    <div className="pb-2">
      {eyebrow && (
        <span className="inline-flex rounded-full border border-indigo-200/60 dark:border-indigo-500/20 bg-indigo-50 dark:bg-indigo-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
          {eyebrow}
        </span>
      )}
      <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">{title}</h2>
    </div>
  );
}

export function InfoCard({ children, className = "", title }) {
  return (
    <section className={cx(
      "rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden",
      className
    )}>
      {title && (
        <div className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-3">
          <h4 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h4>
        </div>
      )}
      <div className="grid gap-2 p-5">{children}</div>
    </section>
  );
}

export function MetaRow({ label, strong = false, value }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 text-sm">
      <span className="text-slate-500 dark:text-slate-400">{label}</span>
      <span className={cx(
        "max-w-[65%] text-right",
        strong ? "font-bold text-slate-900 dark:text-slate-50" : "font-semibold text-slate-700 dark:text-slate-300"
      )}>
        {value}
      </span>
    </div>
  );
}

export function Table({ children, empty, loading, minWidth = "900px" }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-sm" style={{ minWidth }}>
        {children}
      </table>
      {loading && <div className="p-5 text-sm font-semibold text-slate-400 dark:text-slate-500">{loading}</div>}
      {!loading && empty && <div className="flex flex-col items-center justify-center py-12 text-slate-400 dark:text-slate-500 text-sm">{empty}</div>}
    </div>
  );
}

/* ══════════════════════════════════════════════
   PAGINATION
══════════════════════════════════════════════ */
export function Pagination({ loading, meta, onPage, t }) {
  const totalPages = Number.isInteger(Number(meta?.totalPages)) ? Math.max(Number(meta.totalPages), 0) : 0;
  const currentPage = clampPage(meta?.currentPage, totalPages);
  const pages = paginationItems(currentPage, totalPages);
  if (totalPages <= 1) return null;
  return (
    <nav className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 pt-4 text-sm dark:border-slate-800 sm:flex-row" aria-label={t("catalog.pagination", "Pagination")}>
      <span className="font-semibold text-slate-500 dark:text-slate-400">
        {t("catalog.pageIndicator", { page: currentPage, total: totalPages })} — {Number(meta.totalElements) || 0}
      </span>
      <div className="flex max-w-full items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
        <PageButton aria-label={t("catalog.firstPage")} className="hidden sm:inline-flex" disabled={loading || !meta.hasPrevious} onClick={() => onPage(1)}>
          <ChevronsLeft size={15}/>
        </PageButton>
        <PageButton aria-label={t("catalog.previousPage")} disabled={loading || !meta.hasPrevious} onClick={() => onPage(currentPage - 1)}>
          <ChevronLeft size={15}/>
        </PageButton>
        {pages.map((page) => page.type === "ellipsis" ? (
          <span className="grid h-8 min-w-7 place-items-center text-slate-400" key={page.key}>…</span>
        ) : (
          <PageButton
            active={page.value === currentPage}
            aria-current={page.value === currentPage ? "page" : undefined}
            aria-label={t("catalog.pageNumber", { page: page.value, defaultValue: `Page ${page.value}` })}
            disabled={loading}
            key={page.value}
            onClick={() => onPage(page.value)}
          >
            {page.value}
          </PageButton>
        ))}
        <PageButton aria-label={t("catalog.nextPage")} disabled={loading || !meta.hasNext} onClick={() => onPage(currentPage + 1)}>
          <ChevronRight size={15}/>
        </PageButton>
        <PageButton aria-label={t("catalog.lastPage")} className="hidden sm:inline-flex" disabled={loading || !meta.hasNext} onClick={() => onPage(totalPages)}>
          <ChevronsRight size={15}/>
        </PageButton>
      </div>
    </nav>
  );
}

function PageButton({ active = false, children, className = "", ...props }) {
  return (
    <button
      className={cx(
        "inline-flex h-8 min-w-8 flex-shrink-0 items-center justify-center rounded-lg border px-2 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:cursor-not-allowed disabled:opacity-40",
        active
          ? "border-indigo-600 bg-indigo-600 text-white shadow-sm"
          : "border-slate-200 bg-white text-slate-600 hover:border-indigo-300 hover:bg-indigo-50 hover:text-indigo-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:border-indigo-500/40 dark:hover:bg-indigo-500/10 dark:hover:text-indigo-300",
        className
      )}
      type="button"
      {...props}
    >
      {children}
    </button>
  );
}

function paginationItems(currentPage, totalPages) {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => ({ type:"page", value:index + 1 }));
  }

  const values = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1]);
  if (currentPage <= 3) [2, 3, 4].forEach((page) => values.add(page));
  if (currentPage >= totalPages - 2) [totalPages - 3, totalPages - 2, totalPages - 1].forEach((page) => values.add(page));
  const pages = [...values].filter((page) => page >= 1 && page <= totalPages).sort((a, b) => a - b);
  const items = [];
  pages.forEach((page, index) => {
    if (index > 0 && page - pages[index - 1] > 1) items.push({ type:"ellipsis", key:`ellipsis-${page}` });
    items.push({ type:"page", value:page });
  });
  return items;
}

function clampPage(value, totalPages) {
  const parsed = Number(value);
  const page = Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
  return Math.min(Math.max(page, 1), Math.max(totalPages, 1));
}

/* ══════════════════════════════════════════════
   TABS
══════════════════════════════════════════════ */
export function Tabs({ items, onChange, value }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {items.map((item) => (
        <Button
          key={item.value}
          type="button"
          variant={item.value === value ? "primary" : "secondary"}
          size="sm"
          onClick={() => onChange(item.value)}
        >
          {item.label}
        </Button>
      ))}
    </div>
  );
}

/* ══════════════════════════════════════════════
   SKELETON / EMPTY / ERROR
══════════════════════════════════════════════ */
export function Skeleton({ rows = 3 }) {
  return (
    <div className="grid gap-3" aria-hidden="true">
      {Array.from({ length: rows }).map((_, index) => (
        <div className="h-14 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" key={index}/>
      ))}
    </div>
  );
}

export function EmptyState({ action, children, title }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/30 p-10 text-center">
      {title && <h3 className="text-base font-bold text-slate-700 dark:text-slate-300">{title}</h3>}
      {children && <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function ErrorState({ children, title }) {
  return (
    <div className="rounded-2xl border border-rose-200 dark:border-rose-500/30 bg-rose-50 dark:bg-rose-500/10 p-5 text-sm font-semibold text-rose-700 dark:text-rose-400">
      {title && <p className="mb-1 font-bold">{title}</p>}
      {children}
    </div>
  );
}

/* ══════════════════════════════════════════════
   NOTICE
══════════════════════════════════════════════ */
export function Notice({ children, className = "", variant = "warning" }) {
  const styles = {
    warning: "border-amber-200/60 dark:border-amber-500/20 bg-amber-50 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400",
    error:   "border-rose-200/60 dark:border-rose-500/20 bg-rose-50 dark:bg-rose-500/10 text-rose-700 dark:text-rose-400",
    success: "border-emerald-200/60 dark:border-emerald-500/20 bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
    info:    "border-sky-200/60 dark:border-sky-500/20 bg-sky-50 dark:bg-sky-500/10 text-sky-700 dark:text-sky-400",
  };
  return (
    <div className={cx("rounded-xl border px-4 py-3 text-sm font-semibold", styles[variant] || styles.warning, className)}>
      {children}
    </div>
  );
}

/* ══════════════════════════════════════════════
   OVERLAYS (Modal + Drawer)
══════════════════════════════════════════════ */
function useOverlay({ onClose, open }) {
  const dialogRef = useRef(null);
  const returnFocusRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    returnFocusRef.current = document.activeElement;
    const dialog = dialogRef.current;
    const focusable = dialog?.querySelector("button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])");
    focusable?.focus?.();

    const onKeyDown = (event) => {
      if (event.key === "Escape") onClose?.();
      if (event.key !== "Tab" || !dialog) return;
      const items = Array.from(dialog.querySelectorAll("button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])"))
        .filter((item) => !item.disabled && item.offsetParent !== null);
      if (!items.length) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault(); first.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      returnFocusRef.current?.focus?.();
    };
  }, [onClose, open]);

  return dialogRef;
}

const modalSizes = {
  sm: "max-w-md",
  md: "max-w-xl",
  lg: "max-w-3xl",
  xl: "max-w-5xl",
  full: "max-w-[min(96vw,1200px)]",
};

export function Modal({ children, closeOnBackdrop = true, description, footer, onClose, open = true, size = "md", title }) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useOverlay({ onClose, open });
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(event) => { if (closeOnBackdrop && event.target === event.currentTarget) onClose?.(); }}
    >
      <motion.section
        ref={dialogRef}
        initial={{ opacity: 0, scale: 0.95, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 8 }}
        transition={{ duration: 0.18 }}
        aria-describedby={description ? descriptionId : undefined}
        aria-labelledby={titleId}
        aria-modal="true"
        className={cx("flex max-h-[90vh] w-full flex-col overflow-hidden rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl", modalSizes[size] || modalSizes.md)}
        role="dialog"
      >
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 p-6 dark:border-slate-800">
          <div>
            {title && <h3 className="text-lg font-bold text-slate-900 dark:text-slate-50" id={titleId}>{title}</h3>}
            {description && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400" id={descriptionId}>{description}</p>}
          </div>
          <IconButton aria-label="Close" type="button" onClick={onClose}>
            <X className="h-4 w-4"/>
          </IconButton>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-6">{children}</div>
        {footer && <div className="border-t border-slate-100 p-4 dark:border-slate-800">{footer}</div>}
      </motion.section>
    </div>
  );
}

const drawerSizes = {
  md: "max-w-3xl",
  lg: "max-w-5xl",
  xl: "max-w-6xl",
  full: "max-w-[min(96vw,1280px)]",
};

export function Drawer({ children, description, onClose, open = true, size = "lg", title }) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useOverlay({ onClose, open });
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm">
      <motion.aside
        ref={dialogRef}
        initial={{ x: "100%" }}
        animate={{ x: 0 }}
        exit={{ x: "100%" }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        aria-describedby={description ? descriptionId : undefined}
        aria-labelledby={titleId}
        aria-modal="true"
        className={cx("h-full w-full overflow-y-auto bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl", drawerSizes[size] || drawerSizes.lg)}
        role="dialog"
      >
        <div className="sticky top-0 z-10 flex items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-6 py-4">
          <div>
            {title && <h2 className="text-xl font-bold text-slate-900 dark:text-slate-50" id={titleId}>{title}</h2>}
            {description && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400" id={descriptionId}>{description}</p>}
          </div>
          <IconButton aria-label="Close" type="button" onClick={onClose}>
            <X className="h-4 w-4"/>
          </IconButton>
        </div>
        <div className="p-6">{children}</div>
      </motion.aside>
    </div>
  );
}

/* ══════════════════════════════════════════════
   CONFIRM DIALOG
══════════════════════════════════════════════ */
const ConfirmContext = createContext(null);

export function ConfirmDialogProvider({ children }) {
  const [state, setState] = useState(null);
  const confirm = useCallback(
    (options) => new Promise((resolve) => { setState({ ...options, resolve }); }),
    []
  );

  function close(result) {
    state?.resolve(result);
    setState(null);
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && (
        <Modal closeOnBackdrop={false} onClose={() => close(false)} title={state.title}>
          <p className="text-sm leading-6 text-slate-600 dark:text-slate-400">{state.message}</p>
          <div className="mt-6 flex flex-wrap justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => close(false)}>{state.cancelLabel}</Button>
            <Button type="button" variant={state.danger ? "danger" : "primary"} onClick={() => close(true)}>
              {state.confirmLabel}
            </Button>
          </div>
        </Modal>
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const confirm = useContext(ConfirmContext);
  if (!confirm) throw new Error("useConfirm must be used inside ConfirmDialogProvider.");
  return confirm;
}

/* ══════════════════════════════════════════════
   TOAST
══════════════════════════════════════════════ */
const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const showToast = useCallback((toast) => {
    const id = Date.now() + Math.random();
    setToasts((current) => [...current, { id, variant: "info", ...toast }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== id));
    }, toast.duration || 3500);
  }, []);

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      <div className="fixed bottom-5 right-5 z-[90] grid w-[min(360px,calc(100vw-40px))] gap-2">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, x: 40 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 40 }}
              className={cx(
                "rounded-xl border px-4 py-3 text-sm font-semibold shadow-lg",
                toast.variant === "success"
                  ? "border-emerald-200/60 dark:border-emerald-500/20 bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
                  : toast.variant === "error"
                  ? "border-rose-200/60 dark:border-rose-500/20 bg-rose-50 dark:bg-rose-500/10 text-rose-700 dark:text-rose-400"
                  : "border-sky-200/60 dark:border-sky-500/20 bg-sky-50 dark:bg-sky-500/10 text-sky-700 dark:text-sky-400"
              )}
            >
              <div className="flex items-start justify-between gap-3">
                <span>{toast.message}</span>
                <button
                  className="font-bold opacity-60 hover:opacity-100 flex-shrink-0"
                  type="button"
                  onClick={() => setToasts((current) => current.filter((item) => item.id !== toast.id))}
                >
                  <X size={14}/>
                </button>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const showToast = useContext(ToastContext);
  if (!showToast) throw new Error("useToast must be used inside ToastProvider.");
  return showToast;
}
