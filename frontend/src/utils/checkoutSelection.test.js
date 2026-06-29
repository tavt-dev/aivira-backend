import { describe, expect, it } from "vitest";
import {
  clearCheckoutCartItemIds,
  getCheckoutCartItemIds,
  getStoredCheckoutCartItemIds,
  removeCheckoutCartItemIds,
  saveCheckoutCartItemIds
} from "./checkoutSelection.js";

describe("checkout selection helpers", () => {
  const items = [
    { cartItemId: 1, available: true, stockQuantity: 3 },
    { cartItemId: 2, available: false, stockQuantity: 3 },
    { cartItemId: 3, available: true, stockQuantity: 0 },
    { cartItemId: 4, available: true, stockQuantity: 1 }
  ];

  it("returns no checkout ids until the user selects cart items", () => {
    expect(getCheckoutCartItemIds(items)).toEqual([]);
  });

  it("persists selected ids and filters invalid/unavailable ids", () => {
    saveCheckoutCartItemIds([4, 4, "bad", 999]);
    expect(getStoredCheckoutCartItemIds()).toEqual([4, 999]);
    expect(getCheckoutCartItemIds(items)).toEqual([4]);
  });

  it("removes selected ids and clears storage", () => {
    saveCheckoutCartItemIds([1, 4]);
    expect(removeCheckoutCartItemIds([1])).toEqual([4]);
    clearCheckoutCartItemIds();
    expect(getStoredCheckoutCartItemIds()).toEqual([]);
  });
});
