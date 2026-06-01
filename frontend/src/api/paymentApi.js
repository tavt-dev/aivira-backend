import { request } from "./client.js";

export function getPaymentGroup(code) {
  return request(`/payments/groups/${encodeURIComponent(code)}`);
}

export function getPayment(paymentId) {
  return request(`/payments/${encodeURIComponent(paymentId)}`);
}

export function retryPayment(code) {
  return request(`/payments/groups/${encodeURIComponent(code)}/retry`, { method: "POST" });
}
