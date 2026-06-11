import i18n from "../i18n.js";

export function currentLocale(language = i18n.language) {
  return String(language || "vi").startsWith("en") ? "en-US" : "vi-VN";
}

export function formatVND(value, language) {
  return new Intl.NumberFormat(currentLocale(language), {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
}

export function formatSold(value, language) {
  const numeric = Number(value || 0);
  if (numeric > 1000) return `${new Intl.NumberFormat(currentLocale(language), { maximumFractionDigits: 1 }).format(numeric / 1000)}k`;
  return new Intl.NumberFormat(currentLocale(language)).format(numeric);
}

export function discount(book) {
  if (!book?.priceOld) return 0;
  return Math.max(0, Math.round((1 - Number(book.price || 0) / Number(book.priceOld || 1)) * 100));
}

export function cartTotal(items) {
  return items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 1), 0);
}

export function formatDateTime(value, language) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  return new Intl.DateTimeFormat(currentLocale(language), {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
