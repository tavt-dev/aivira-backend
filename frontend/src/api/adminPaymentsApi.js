import { request } from "./client.js";

export function getAdminPaymentGroup(code) {
  return request(`/admin/payments/groups/${encodeURIComponent(code)}`);
}

export function reconcilePaymentGroup(code) {
  return request(`/admin/payments/groups/${encodeURIComponent(code)}/reconcile`, { method: "POST" });
}
