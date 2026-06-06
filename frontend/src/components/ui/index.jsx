import { createContext, forwardRef, useCallback, useContext, useEffect, useId, useRef, useState } from "react";
import { X } from "lucide-react";

function cx(...parts) {
  return parts.filter(Boolean).join(" ");
}

const buttonVariants = {
  primary: "bg-slate-950 text-white hover:bg-blue-600",
  secondary: "border border-slate-200 text-slate-700 hover:bg-slate-50",
  danger: "border border-red-100 text-red-600 hover:bg-red-50",
  ghost: "text-slate-600 hover:bg-slate-100",
};

const buttonSizes = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-5 py-3 text-sm",
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
        "inline-flex items-center justify-center gap-2 rounded-full font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        buttonVariants[variant] || buttonVariants.primary,
        buttonSizes[size] || buttonSizes.md,
        className
      )}
    >
      {loading && <span className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden="true" />}
      <span>{children}</span>
    </button>
  );
});

export const IconButton = forwardRef(function IconButton({ "aria-label": ariaLabel, children, className = "", variant = "ghost", ...props }, ref) {
  if (!ariaLabel) {
    throw new Error("IconButton requires an aria-label.");
  }
  return (
    <button
      {...props}
      ref={ref}
      aria-label={ariaLabel}
      className={cx(
        "inline-flex h-10 w-10 items-center justify-center rounded-full transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        buttonVariants[variant] || buttonVariants.ghost,
        className
      )}
    >
      {children}
    </button>
  );
});

function Field({ children, error, hint, id, label, required }) {
  if (!label && !error && !hint) return children;
  return (
    <label className="grid gap-1.5" htmlFor={id}>
      {label && (
        <span className="text-sm font-bold text-slate-700">
          {label}
          {required && <span className="text-red-500"> *</span>}
        </span>
      )}
      {children}
      {hint && !error && <span className="text-xs font-semibold text-slate-500">{hint}</span>}
      {error && <span className="text-xs font-semibold text-red-600" id={`${id}-error`}>{error}</span>}
    </label>
  );
}

const controlClass =
  "w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60";

export const Input = forwardRef(function Input({ className = "", error, hint, id, label, required, ...props }, ref) {
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
  return <Input {...props} ref={ref} type="datetime-local" />;
});

export const Textarea = forwardRef(function Textarea({ className = "", error, hint, id, label, required, ...props }, ref) {
  const fallbackId = useId();
  const fieldId = id || fallbackId;
  return (
    <Field error={error} hint={hint} id={fieldId} label={label} required={required}>
      <textarea
        {...props}
        ref={ref}
        aria-describedby={error ? `${fieldId}-error` : undefined}
        aria-invalid={error ? "true" : undefined}
        className={cx("min-h-28", controlClass, className)}
        id={fieldId}
        required={required}
      />
    </Field>
  );
});

export const Select = forwardRef(function Select({ children, className = "", error, hint, id, label, required, ...props }, ref) {
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
    <label className={cx("inline-flex items-center gap-2 text-sm font-semibold text-slate-700", className)}>
      <input {...props} className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" type="checkbox" />
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
        "inline-flex items-center gap-2 rounded-full border px-3 py-2 text-sm font-bold transition-colors",
        checked ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 text-slate-600 hover:bg-slate-50"
      )}
      type="button"
      onClick={() => onChange?.(!checked)}
    >
      <span className={cx("h-3 w-3 rounded-full", checked ? "bg-blue-600" : "bg-slate-300")} />
      {children}
    </button>
  );
}

const badgeVariants = {
  neutral: "bg-slate-100 text-slate-700",
  success: "bg-emerald-50 text-emerald-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-red-50 text-red-700",
  info: "bg-blue-50 text-blue-700",
};

export function Badge({ children, className = "", variant = "neutral" }) {
  return <span className={cx("inline-flex rounded-full px-2 py-1 text-xs font-bold", badgeVariants[variant] || badgeVariants.neutral, className)}>{children}</span>;
}

export function StatusPill({ status, type = "generic" }) {
  const text = status || "-";
  const value = String(status || "").toUpperCase();
  let variant = "neutral";
  if (["ACTIVE", "SUCCESS", "COMPLETED", "APPROVED", "VISIBLE", "PAID"].includes(value)) variant = "success";
  if (["PENDING", "PENDING_PAYMENT", "PENDING_CONFIRMATION", "CONFIRMED", "PACKING", "SHIPPING"].includes(value)) variant = "warning";
  if (["FAILED", "PAYMENT_FAILED", "CANCELLED", "EXPIRED", "REJECTED", "HIDDEN", "LOCKED"].includes(value)) variant = "danger";
  if (type === "payment" && value === "REFUNDED") variant = "info";
  return <Badge variant={variant}>{text}</Badge>;
}

export function Panel({ children, className = "", title }) {
  return (
    <section className={cx("rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:p-6", className)}>
      {title && <h3 className="mb-5 text-xl font-bold text-slate-950">{title}</h3>}
      {children}
    </section>
  );
}

export function PageHeader({ eyebrow, title }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      {eyebrow && <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span>}
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

export function InfoCard({ children, className = "", title }) {
  return (
    <section className={cx("rounded-xl border border-slate-200 bg-white p-5", className)}>
      {title && <h4 className="mb-4 text-lg font-bold text-slate-950">{title}</h4>}
      <div className="grid gap-2">{children}</div>
    </section>
  );
}

export function MetaRow({ label, strong = false, value }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className={cx("max-w-[70%] text-right", strong ? "font-bold text-slate-950" : "font-semibold text-slate-700")}>{value}</span>
    </div>
  );
}

export function Table({ children, empty, loading, minWidth = "900px" }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200">
      <table className="w-full border-collapse text-left text-sm" style={{ minWidth }}>
        {children}
      </table>
      {loading && <div className="p-5 text-sm font-semibold text-slate-500">{loading}</div>}
      {!loading && empty && <div className="p-5 text-sm text-slate-500">{empty}</div>}
    </div>
  );
}

export function Pagination({ loading, meta, onPage, t }) {
  if (!meta?.totalPages || meta.totalPages <= 1) return null;
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
      <span className="font-semibold text-slate-500">
        {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })} - {meta.totalElements}
      </span>
      <div className="flex flex-wrap gap-2">
        <Button disabled={loading || !meta.hasPrevious} size="sm" variant="secondary" onClick={() => onPage(1)}>{t("catalog.firstPage")}</Button>
        <Button disabled={loading || !meta.hasPrevious} size="sm" variant="secondary" onClick={() => onPage(meta.currentPage - 1)}>{t("catalog.previousPage")}</Button>
        <Button disabled={loading || !meta.hasNext} size="sm" variant="secondary" onClick={() => onPage(meta.currentPage + 1)}>{t("catalog.nextPage")}</Button>
        <Button disabled={loading || !meta.hasNext} size="sm" variant="secondary" onClick={() => onPage(meta.totalPages)}>{t("catalog.lastPage")}</Button>
      </div>
    </div>
  );
}

export function Tabs({ items, onChange, value }) {
  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <Button key={item.value} type="button" variant={item.value === value ? "primary" : "secondary"} onClick={() => onChange(item.value)}>
          {item.label}
        </Button>
      ))}
    </div>
  );
}

export function Skeleton({ rows = 3 }) {
  return (
    <div className="grid gap-3" aria-hidden="true">
      {Array.from({ length: rows }).map((_, index) => (
        <div className="h-16 animate-pulse rounded-xl bg-slate-100" key={index} />
      ))}
    </div>
  );
}

export function EmptyState({ action, children, title }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center">
      {title && <h3 className="text-lg font-bold text-slate-950">{title}</h3>}
      {children && <p className="mt-2 text-sm text-slate-500">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function ErrorState({ children, title }) {
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm font-semibold text-red-700">
      {title && <p className="mb-1 font-bold">{title}</p>}
      {children}
    </div>
  );
}

export function Notice({ children, className = "", variant = "warning" }) {
  const styles = {
    warning: "border-amber-200 bg-amber-50 text-amber-700",
    error: "border-red-200 bg-red-50 text-red-700",
    success: "border-emerald-200 bg-emerald-50 text-emerald-700",
    info: "border-blue-200 bg-blue-50 text-blue-700",
  };
  return <div className={cx("rounded-xl border px-5 py-4 text-sm font-semibold", styles[variant] || styles.warning, className)}>{children}</div>;
}

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
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
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

export function Modal({ children, closeOnBackdrop = true, onClose, open = true, title }) {
  const titleId = useId();
  const dialogRef = useOverlay({ onClose, open });
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm" onMouseDown={(event) => {
      if (closeOnBackdrop && event.target === event.currentTarget) onClose?.();
    }}>
      <section ref={dialogRef} aria-labelledby={titleId} aria-modal="true" className="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl" role="dialog">
        <div className="mb-4 flex items-start justify-between gap-4">
          {title && <h3 className="text-xl font-bold text-slate-950" id={titleId}>{title}</h3>}
          <IconButton aria-label="Close" type="button" onClick={onClose}>
            <X className="h-4 w-4" />
          </IconButton>
        </div>
        {children}
      </section>
    </div>
  );
}

export function Drawer({ children, onClose, open = true, title }) {
  const titleId = useId();
  const dialogRef = useOverlay({ onClose, open });
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm">
      <aside ref={dialogRef} aria-labelledby={titleId} aria-modal="true" className="h-full w-full max-w-5xl overflow-y-auto bg-white p-5 shadow-2xl md:p-8" role="dialog">
        <div className="mb-5 flex items-start justify-between gap-4">
          {title && <h2 className="text-2xl font-bold text-slate-950" id={titleId}>{title}</h2>}
          <IconButton aria-label="Close" type="button" onClick={onClose}>
            <X className="h-4 w-4" />
          </IconButton>
        </div>
        {children}
      </aside>
    </div>
  );
}

const ConfirmContext = createContext(null);

export function ConfirmDialogProvider({ children }) {
  const [state, setState] = useState(null);
  const confirm = useCallback((options) => new Promise((resolve) => {
    setState({ ...options, resolve });
  }), []);

  function close(result) {
    state?.resolve(result);
    setState(null);
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && (
        <Modal closeOnBackdrop={false} onClose={() => close(false)} title={state.title}>
          <p className="text-sm leading-6 text-slate-600">{state.message}</p>
          <div className="mt-6 flex flex-wrap justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => close(false)}>{state.cancelLabel}</Button>
            <Button type="button" variant={state.danger ? "danger" : "primary"} onClick={() => close(true)}>{state.confirmLabel}</Button>
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
        {toasts.map((toast) => (
          <Notice key={toast.id} variant={toast.variant}>
            <div className="flex items-start justify-between gap-3">
              <span>{toast.message}</span>
              <button className="font-bold" type="button" onClick={() => setToasts((current) => current.filter((item) => item.id !== toast.id))}>×</button>
            </div>
          </Notice>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const showToast = useContext(ToastContext);
  if (!showToast) throw new Error("useToast must be used inside ToastProvider.");
  return showToast;
}
