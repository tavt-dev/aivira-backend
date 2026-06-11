import { query, request } from "./client.js";

export function getAdminReviews(params = {}) {
  return request(`/admin/reviews${query(params)}`);
}

export function moderateReview(reviewId, body) {
  return request(`/admin/reviews/${encodeURIComponent(reviewId)}/moderate`, { method: "PUT", body });
}

export function replyReview(reviewId, body) {
  return request(`/admin/reviews/${encodeURIComponent(reviewId)}/reply`, { method: "PUT", body });
}
