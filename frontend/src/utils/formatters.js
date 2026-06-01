export function formatVND(value) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value || 0));
}

export function formatSold(value) {
  return Number(value || 0) > 1000 ? `${(Number(value || 0) / 1000).toFixed(1)}k` : String(value || 0);
}

export function discount(book) {
  if (!book?.priceOld) return 0;
  return Math.max(0, Math.round((1 - Number(book.price || 0) / Number(book.priceOld || 1)) * 100));
}

export function cartTotal(items) {
  return items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 1), 0);
}
