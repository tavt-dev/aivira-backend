import { query, request } from "./client.js";

export function getAdminProducts(params = {}) {
  return request(`/admin/products${query(params)}`);
}

export function createAdminProduct(body) {
  return request("/admin/products", { method: "POST", body });
}

export function updateAdminProduct(id, body) {
  return request(`/admin/products/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function deleteAdminProduct(id) {
  return request(`/admin/products/${encodeURIComponent(id)}`, { method: "DELETE" });
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
  return request(`/admin/products/${encodeURIComponent(productId)}/media/${encodeURIComponent(mediaId)}`, { method: "PUT", body });
}

export function deleteProductMedia(productId, mediaId) {
  return request(`/admin/products/${encodeURIComponent(productId)}/media/${encodeURIComponent(mediaId)}`, { method: "DELETE" });
}

export function createProductVariation(productId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations`, { method: "POST", body });
}

export function updateProductVariation(productId, variationId, body) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}`, { method: "PUT", body });
}

export function deleteProductVariation(productId, variationId) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}`, { method: "DELETE" });
}

export function updateProductStock(productId, variationId, stockQuantity) {
  return request(`/admin/products/${encodeURIComponent(productId)}/variations/${encodeURIComponent(variationId)}/stock`, {
    method: "PUT",
    body: { stockQuantity: Number(stockQuantity) }
  });
}

export function createAdminCategory(body) {
  return request("/admin/categories", { method: "POST", body });
}

export function updateAdminCategory(id, body) {
  return request(`/admin/categories/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function deleteAdminCategory(id) {
  return request(`/admin/categories/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function reconcilePaymentGroup(code) {
  return request(`/admin/payments/groups/${encodeURIComponent(code)}/reconcile`, { method: "POST" });
}

export function getPermissions() {
  return request("/admin/permissions");
}

export function getRoles() {
  return request("/admin/roles");
}

export function getRolePermissions(roleCode) {
  return request(`/admin/roles/${encodeURIComponent(roleCode)}/permissions`);
}

export function updateRolePermissions(roleCode, permissions) {
  return request(`/admin/roles/${encodeURIComponent(roleCode)}/permissions`, { method: "PUT", body: { permissions } });
}

export function getUserPermissions(userId) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions`);
}

export function grantUserPermission(userId, body) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions`, { method: "POST", body });
}

export function revokeUserPermission(userId, permissionCode) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions/${encodeURIComponent(permissionCode)}`, { method: "DELETE" });
}
