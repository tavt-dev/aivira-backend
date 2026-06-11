import { query, request } from "./client.js";

export function getProductReviews(slug, params = {}, options = {}) {
  return request(`/products/${encodeURIComponent(slug)}/reviews${query(params)}`, {
    ...options,
    skipAuth: options.skipAuth ?? true
  });
}

export function createOrderItemReview(orderId, orderItemId, body) {
  return request(`/orders/${encodeURIComponent(orderId)}/items/${encodeURIComponent(orderItemId)}/review`, {
    method: "POST",
    body
  });
}

export function updateReview(reviewId, body) {
  return request(`/reviews/${encodeURIComponent(reviewId)}`, { method: "PUT", body });
}

export function deleteReview(reviewId) {
  return request(`/reviews/${encodeURIComponent(reviewId)}`, { method: "DELETE" });
}
