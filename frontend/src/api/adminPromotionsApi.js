import { query, request } from "./client.js";

export function getPromotions(params = {}) {
  return request(`/admin/promotions${query(params)}`);
}

export function getPromotion(promotionId) {
  return request(`/admin/promotions/${encodeURIComponent(promotionId)}`);
}

export function createPromotion(body) {
  return request("/admin/promotions", { method: "POST", body });
}

export function updatePromotion(promotionId, body) {
  return request(`/admin/promotions/${encodeURIComponent(promotionId)}`, { method: "PUT", body });
}

export function deletePromotion(promotionId) {
  return request(`/admin/promotions/${encodeURIComponent(promotionId)}`, { method: "DELETE" });
}
