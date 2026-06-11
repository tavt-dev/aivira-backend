import { query, request } from "./client.js";

export function getOrders(params = {}) {
  return request(`/orders${query(params)}`);
}

export function getOrder(id) {
  return request(`/orders/${encodeURIComponent(id)}`);
}

export function cancelOrder(id, reason) {
  return request(`/orders/${encodeURIComponent(id)}/cancel`, { method: "POST", body: { reason } });
}

export { createCheckout as checkout } from "./checkoutApi.js";
export { createAddress, deleteAddress, getAddresses, setDefaultAddress, updateAddress } from "./userApi.js";
