import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { request, query } from "./client.js";
import { server } from "../test/server.js";
import { apiResponse, customerUser } from "../test/mockData.js";

const API = "http://localhost/api/v1";

describe("api client", () => {
  it("builds query strings without empty values while preserving false and zero", () => {
    expect(
      query({
        keyword: "clean",
        empty: "",
        missing: null,
        omitted: undefined,
        available: false,
        page: 0,
        tags: ["book", "", null, "sale"]
      })
    ).toBe("?keyword=clean&available=false&page=0&tags=book&tags=sale");
  });

  it("returns ApiResponse.data on success", async () => {
    await expect(request("/products")).resolves.toMatchObject({
      currentPage: 1,
      totalElements: 2
    });
  });

  it("normalizes backend error envelope", async () => {
    server.use(
      http.get(`${API}/products`, () =>
        HttpResponse.json(
          {
            success: false,
            errorCode: "PRODUCT_NOT_FOUND",
            message: "Book not found",
            data: { slug: "missing" }
          },
          { status: 404 }
        )
      )
    );

    await expect(request("/products")).rejects.toMatchObject({
      message: "Book not found",
      status: 404,
      errorCode: "PRODUCT_NOT_FOUND",
      data: { slug: "missing" }
    });
  });

  it("refreshes once on protected 401 and retries with the new token", async () => {
    localStorage.setItem("aivira_access_token", "expired-token");
    localStorage.setItem("aivira_refresh_token", "refresh-token");
    let refreshCalls = 0;

    server.use(
      http.get(`${API}/users/me`, ({ request }) => {
        const auth = request.headers.get("authorization") || "";
        if (auth.includes("new-access-token")) {
          return HttpResponse.json(apiResponse(customerUser));
        }
        return HttpResponse.json({ success: false, message: "Expired" }, { status: 401 });
      }),
      http.post(`${API}/auth/refresh-token`, () => {
        refreshCalls += 1;
        return HttpResponse.json(
          apiResponse({ token: "new-access-token", refreshToken: "refresh-token", user: customerUser })
        );
      })
    );

    await expect(request("/users/me")).resolves.toMatchObject({ username: "reader" });
    expect(refreshCalls).toBe(1);
    expect(localStorage.getItem("aivira_access_token")).toBe("new-access-token");
  });

  it("clears auth and emits auth-expired when refresh fails", async () => {
    localStorage.setItem("aivira_access_token", "expired-token");
    localStorage.setItem("aivira_refresh_token", "bad-refresh-token");
    const expiredListener = vi.fn();
    window.addEventListener("aivira-auth-expired", expiredListener);

    server.use(
      http.get(`${API}/users/me`, () => HttpResponse.json({ success: false, message: "Expired" }, { status: 401 })),
      http.post(`${API}/auth/refresh-token`, () =>
        HttpResponse.json(
          {
            success: false,
            errorCode: "AUTH_REFRESH_INVALID",
            message: "Refresh token is invalid"
          },
          { status: 401 }
        )
      )
    );

    await expect(request("/users/me")).rejects.toMatchObject({
      message: "Refresh token is invalid",
      status: 401,
      errorCode: "AUTH_REFRESH_INVALID"
    });
    expect(localStorage.getItem("aivira_access_token")).toBeNull();
    expect(expiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener("aivira-auth-expired", expiredListener);
  });
});
