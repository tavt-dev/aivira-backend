import { request } from "./client.js";

export function createAdminCategory(body) {
  return request("/admin/categories", { method: "POST", body });
}

export function updateAdminCategory(id, body) {
  return request(`/admin/categories/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function deleteAdminCategory(id) {
  return request(`/admin/categories/${encodeURIComponent(id)}`, { method: "DELETE" });
}
