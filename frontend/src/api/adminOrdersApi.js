import { query, request } from "./client.js";

export function getAdminOrders(params = {}) {
  return request(`/admin/orders${query(params)}`);
}

export function getAdminOrder(orderId) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}`);
}

export function confirmOrder(orderId) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/confirm`, { method: "PUT" });
}

export function markPacking(orderId) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/packing`, { method: "PUT" });
}

export function markShipping(orderId) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/shipping`, { method: "PUT" });
}

export function markCompleted(orderId) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/completed`, { method: "PUT" });
}

export function cancelAdminOrder(orderId, body) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/cancel`, { method: "PUT", body });
}

export function markRefunded(orderId, body) {
  return request(`/admin/orders/${encodeURIComponent(orderId)}/mark-refunded`, { method: "PUT", body });
}
