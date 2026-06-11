import { screen, waitFor } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import CategoryPage from "./CategoryPage.jsx";
import { renderWithProviders } from "../test/render.jsx";
import { apiResponse, book, pageResponse } from "../test/mockData.js";
import { server } from "../test/server.js";

const API = "http://localhost/api/v1";

describe("CategoryPage", () => {
  it("maps URL filters to backend product query params", async () => {
    let requestedUrl;
    server.use(
      http.get(`${API}/products`, ({ request }) => {
        requestedUrl = new URL(request.url);
        return HttpResponse.json(apiResponse(pageResponse([book])));
      })
    );

    renderWithProviders(
      <Routes>
        <Route path="/category/:slug" element={<CategoryPage />} />
      </Routes>,
      {
        route:
          "/category/programming?search=architecture&author=Robert&publisher=Prentice&isbn=978&minPrice=1000&maxPrice=900000&available=false&sort=name_asc&page=2&size=24"
      }
    );

    await screen.findByText("Clean Architecture");
    await waitFor(() => expect(requestedUrl).toBeTruthy());

    expect(requestedUrl.searchParams.get("keyword")).toBe("architecture");
    expect(requestedUrl.searchParams.get("categorySlug")).toBe("programming");
    expect(requestedUrl.searchParams.get("author")).toBe("Robert");
    expect(requestedUrl.searchParams.get("publisher")).toBe("Prentice");
    expect(requestedUrl.searchParams.get("isbn")).toBe("978");
    expect(requestedUrl.searchParams.get("available")).toBe("false");
    expect(requestedUrl.searchParams.get("sort")).toBe("name_asc");
    expect(requestedUrl.searchParams.get("page")).toBe("1");
    expect(requestedUrl.searchParams.get("size")).toBe("24");
  });
});
