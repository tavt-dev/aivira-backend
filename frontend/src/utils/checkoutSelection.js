const CHECKOUT_CART_ITEM_IDS_KEY = "aivira_checkout_cart_item_ids";

function normalizeId(value) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null;
}

export function isCartItemCheckoutAvailable(item) {
  if (!item?.cartItemId) return false;
  if (item.available === false) return false;
  if (item.stockQuantity != null && Number(item.stockQuantity) <= 0) return false;
  return true;
}

export function getStoredCheckoutCartItemIds() {
  if (typeof window === "undefined") return [];

  try {
    const parsed = JSON.parse(window.sessionStorage.getItem(CHECKOUT_CART_ITEM_IDS_KEY) || "[]");
    return Array.isArray(parsed) ? parsed.map(normalizeId).filter(Boolean) : [];
  } catch {
    return [];
  }
}

export function saveCheckoutCartItemIds(ids) {
  if (typeof window === "undefined") return;

  const uniqueIds = [...new Set((ids || []).map(normalizeId).filter(Boolean))];
  window.sessionStorage.setItem(CHECKOUT_CART_ITEM_IDS_KEY, JSON.stringify(uniqueIds));
}

export function clearCheckoutCartItemIds() {
  if (typeof window === "undefined") return;

  window.sessionStorage.removeItem(CHECKOUT_CART_ITEM_IDS_KEY);
}

export function removeCheckoutCartItemIds(ids) {
  const removeSet = new Set((ids || []).map(normalizeId).filter(Boolean));
  const nextIds = getStoredCheckoutCartItemIds().filter((id) => !removeSet.has(id));
  saveCheckoutCartItemIds(nextIds);
  return nextIds;
}

export function getCheckoutCartItemIds(items = []) {
  const availableIds = items
    .filter(isCartItemCheckoutAvailable)
    .map((item) => normalizeId(item.cartItemId))
    .filter(Boolean);
  const availableSet = new Set(availableIds);
  return getStoredCheckoutCartItemIds().filter((id) => availableSet.has(id));
}
