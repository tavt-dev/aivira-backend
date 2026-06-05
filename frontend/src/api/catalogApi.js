import { query, request } from "./client.js";

export function getProducts(params = {}, options = {}) {
  return request(`/products${query(params)}`, { ...options, skipAuth: options.skipAuth ?? true });
}

export function getProduct(slug, options = {}) {
  return request(`/products/${encodeURIComponent(slug)}`, { ...options, skipAuth: options.skipAuth ?? true });
}

export function getCategories(options = {}) {
  return request("/categories", { ...options, skipAuth: options.skipAuth ?? true });
}

export function getCategoryTree(options = {}) {
  return request("/categories/tree", { ...options, skipAuth: options.skipAuth ?? true });
}
