import { query, request } from "./client.js";

export function getAdminProducts(params = {}) {
  return request(`/admin/products${query(params)}`);
}

export function getAdminProduct(productId) {
  return request(`/admin/products/${encodeURIComponent(productId)}`);
}

export function createAdminProduct(body) {
  return request("/admin/products", { method: "POST", body });
}

export function updateAdminProduct(productId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}`, { method: "PUT", body });
}

export function deleteAdminProduct(productId) {
  return request(`/admin/products/${encodeURIComponent(productId)}`, { method: "DELETE" });
}

export function uploadProductMedia(productId, file, fields = {}) {
  const body = new FormData();
  body.append("media", file);
  Object.entries(fields).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") body.append(key, value);
  });
  return request(`/admin/products/${encodeURIComponent(productId)}/media`, { method: "POST", body });
}

export function updateProductMedia(productId, mediaId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}/media/${encodeURIComponent(mediaId)}`, {
    method: "PUT",
    body
  });
}

export function deleteProductMedia(productId, mediaId) {
  return request(`/admin/products/${encodeURIComponent(productId)}/media/${encodeURIComponent(mediaId)}`, {
    method: "DELETE"
  });
}

export function createProductVariation(productId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations`, { method: "POST", body });
}

export function updateProductVariation(productId, variationId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}`, {
    method: "PUT",
    body
  });
}

export function deleteProductVariation(productId, variationId) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}`, {
    method: "DELETE"
  });
}

export function updateProductStock(productId, variationId, stockQuantity) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}/stock`, {
    method: "PUT",
    body: { stockQuantity: Number(stockQuantity) }
  });
}
