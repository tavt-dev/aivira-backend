import { request } from "./client.js";

export function getStorefrontHome(options = {}) {
  return request("/storefront/home", { ...options, skipAuth: options.skipAuth ?? true });
}
