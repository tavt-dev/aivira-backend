import { query, request } from "./client.js";

export function getAdminUsers(params = {}) {
  return request(`/admin/users${query(params)}`);
}

export function getAdminUser(userId) {
  return request(`/admin/users/${encodeURIComponent(userId)}`);
}

export function lockUser(userId) {
  return request(`/admin/users/${encodeURIComponent(userId)}/lock`, { method: "PUT" });
}

export function unlockUser(userId) {
  return request(`/admin/users/${encodeURIComponent(userId)}/unlock`, { method: "PUT" });
}

export function updateUserRoles(userId, roles) {
  return request(`/admin/users/${encodeURIComponent(userId)}/roles`, { method: "PUT", body: { roles } });
}
