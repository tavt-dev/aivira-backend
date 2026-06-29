import { request } from "./client.js";

export function login(body) {
  return request("/auth/token", { method: "POST", body });
}

export function register(body) {
  return request("/auth/register", { method: "POST", body });
}

export function verifyUser(body) {
  return request("/auth/verify-user", { method: "POST", body });
}

export function resendVerification(body) {
  return request("/auth/resend-verification", { method: "POST", body });
}

export function logout(refreshToken) {
  return request("/auth/logout", { method: "POST", body: refreshToken ? { refreshToken } : {} });
}

export function refreshToken(refreshToken) {
  return request("/auth/refresh-token", { method: "POST", body: refreshToken ? { refreshToken } : {} });
}

export function logoutAll() {
  return request("/auth/logout-all", { method: "POST" });
}

export function getSessions() {
  return request("/auth/sessions");
}

export function revokeSession(sessionId) {
  return request(`/auth/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" });
}

export function forgotPassword(body) {
  return request("/auth/forgot-password", { method: "POST", body });
}

export function resetPassword(body) {
  return request("/auth/reset-password", { method: "POST", body });
}

export function exchangeGoogleTicket(ticket) {
  return request("/auth/google/exchange-ticket", {
    method: "POST",
    body: { ticket },
    skipAuth: true,
    skipRefresh: true
  });
}
