import { describe, expect, it } from "vitest";
import { pageMeta, pageRows } from "./mappers.js";

describe("pagination mappers", () => {
  it("extracts rows from supported backend page shapes", () => {
    expect(pageRows({ content: [1] })).toEqual([1]);
    expect(pageRows({ data: [2] })).toEqual([2]);
    expect(pageRows({ items: [3] })).toEqual([3]);
    expect(pageRows([4])).toEqual([4]);
  });

  it("normalizes explicit PageResponse metadata", () => {
    expect(
      pageMeta({
        content: ["book"],
        currentPage: 2,
        totalPages: 4,
        pageSize: 10,
        totalElements: 31,
        hasNext: true,
        hasPrevious: true
      })
    ).toEqual({
      currentPage: 2,
      totalPages: 4,
      pageSize: 10,
      totalElements: 31,
      hasNext: true,
      hasPrevious: true
    });
  });

  it("falls back safely for empty or raw array payloads", () => {
    expect(pageMeta(null)).toEqual({
      currentPage: 1,
      totalPages: 1,
      pageSize: 20,
      totalElements: 0,
      hasNext: false,
      hasPrevious: false
    });
    expect(pageMeta(["a", "b"], { page: 1, size: 12 })).toEqual({
      currentPage: 1,
      totalPages: 1,
      pageSize: 12,
      totalElements: 2,
      hasNext: false,
      hasPrevious: false
    });
  });
});
