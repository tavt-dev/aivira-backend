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

export function getAddresses() {
  return request("/users/me/addresses");
}

export function createAddress(body) {
  return request("/users/me/addresses", { method: "POST", body });
}

export function updateAddress(id, body) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function deleteAddress(id) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function setDefaultAddress(id) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}/default`, { method: "PUT" });
}

export function checkout(body) {
  return request("/checkout", { method: "POST", body });
}
