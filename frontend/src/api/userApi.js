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
