import { request } from "./client.js";

export function previewCheckout(body, options = {}) {
  return request("/checkout/preview", { method: "POST", body, ...options });
}

export function createCheckout(body) {
  return request("/checkout", { method: "POST", body });
}
