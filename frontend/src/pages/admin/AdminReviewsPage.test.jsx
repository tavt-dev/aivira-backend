import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import AdminReviewsPage from "./AdminReviewsPage.jsx";
import { renderWithProviders } from "../../test/render.jsx";
import { apiResponse, review } from "../../test/mockData.js";
import { server } from "../../test/server.js";

const API = "http://localhost/api/v1";

describe("AdminReviewsPage", () => {
  it("moderates reviews with approved and visible payload", async () => {
    const user = userEvent.setup();
    let capturedBody;
    server.use(
      http.put(`${API}/admin/reviews/:id/moderate`, async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(apiResponse({ ...review, ...capturedBody }));
      })
    );
    renderWithProviders(
      <Routes>
        <Route path="/admin/reviews" element={<AdminReviewsPage />} />
      </Routes>,
      { route: "/admin/reviews" }
    );

    expect(await screen.findByText(/Clear writing and excellent book quality/i)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Duyệt|approve/i }));
    expect(capturedBody).toEqual({ approved: true, visible: true });
  });
});
