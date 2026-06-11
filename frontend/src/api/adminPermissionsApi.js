import { request } from "./client.js";

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
  return request(`/admin/roles/${encodeURIComponent(roleCode)}/permissions`, {
    method: "PUT",
    body: { permissions }
  });
}

export function getUserPermissions(userId) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions`);
}

export function grantUserPermission(userId, body) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions`, { method: "POST", body });
}

export function revokeUserPermission(userId, permissionCode) {
  return request(`/admin/users/${encodeURIComponent(userId)}/permissions/${encodeURIComponent(permissionCode)}`, {
    method: "DELETE"
  });
}
