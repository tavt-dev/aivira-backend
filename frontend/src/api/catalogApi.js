import { query, request } from "./client.js";

export function getProducts(params = {}) {
  return request(`/products${query(params)}`);
}

export function getProduct(slug) {
  return request(`/products/${encodeURIComponent(slug)}`);
}

export function getCategories() {
  return request("/categories");
}
