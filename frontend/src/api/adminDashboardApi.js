import { query, request } from "./client.js";

export function getDashboardSummary(params = {}) {
  return request(`/admin/dashboard/summary${query(params)}`);
}

export function getDashboardSales(params = {}) {
  return request(`/admin/dashboard/sales${query(params)}`);
}

export function getDashboardOrders(params = {}) {
  return request(`/admin/dashboard/orders${query(params)}`);
}

export function getDashboardTopBooks(params = {}) {
  return request(`/admin/dashboard/top-books${query(params)}`);
}

export function getDashboardLowStock(params = {}) {
  return request(`/admin/dashboard/low-stock${query(params)}`);
}
