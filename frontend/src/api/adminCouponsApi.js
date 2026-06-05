import { query, request } from "./client.js";

export function getCoupons(params = {}) {
  return request(`/admin/coupons${query(params)}`);
}

export function getCoupon(couponId) {
  return request(`/admin/coupons/${encodeURIComponent(couponId)}`);
}

export function createCoupon(body) {
  return request("/admin/coupons", { method: "POST", body });
}

export function updateCoupon(couponId, body) {
  return request(`/admin/coupons/${encodeURIComponent(couponId)}`, { method: "PUT", body });
}

export function deleteCoupon(couponId) {
  return request(`/admin/coupons/${encodeURIComponent(couponId)}`, { method: "DELETE" });
}
