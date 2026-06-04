import { request } from "./client.js";

export function getCart() {
  return request("/cart");
}

export function addCartItem(body) {
  return request("/cart/items", { method: "POST", body });
}

export function updateCartItem(id, body) {
  return request(`/cart/items/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function removeCartItem(id) {
  return request(`/cart/items/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function clearCart() {
  return request("/cart/items", { method: "DELETE" });
}
