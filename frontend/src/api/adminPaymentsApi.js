import { request } from "./client.js";

export function reconcilePaymentGroup(code) {
  return request(`/admin/payments/groups/${encodeURIComponent(code)}/reconcile`, { method: "POST" });
}
