import { request } from "./client.js";

export function getProfile() {
  return request("/users/me");
}

export function updateProfile(body) {
  return request("/users/me", { method: "PUT", body });
}

export function updateAvatar(file) {
  const body = new FormData();
  body.append("avatar", file);
  return request("/users/me/avatar", { method: "PUT", body });
}

export function changePassword(body) {
  return request("/users/me/password", { method: "PUT", body });
}

export function deactivateAccount() {
  return request("/users/me/deactivate", { method: "POST" });
}

export function getAddresses() {
  return request("/users/me/addresses");
}

export function createAddress(body) {
  return request("/users/me/addresses", { method: "POST", body });
}

export function updateAddress(id, body) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}`, { method: "PUT", body });
}

export function deleteAddress(id) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function setDefaultAddress(id) {
  return request(`/users/me/addresses/${encodeURIComponent(id)}/default`, { method: "PUT" });
}
