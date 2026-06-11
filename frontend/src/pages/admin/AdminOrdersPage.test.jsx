import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import AdminOrdersPage from "./AdminOrdersPage.jsx";
import { renderWithProviders } from "../../test/render.jsx";

describe("AdminOrdersPage", () => {
  it("shows lifecycle actions for pending confirmation orders", async () => {
    renderWithProviders(
      <Routes>
        <Route path="/admin/orders" element={<AdminOrdersPage />} />
      </Routes>,
      { route: "/admin/orders?keyword=ORD-20260611-001" }
    );

    expect(await screen.findByText(/ORD-20260611-001/i)).toBeInTheDocument();
    expect((await screen.findAllByText(/Chờ xác nhận|PENDING_CONFIRMATION/i)).length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: /Xác nhận|confirm/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hủy|cancel/i })).toBeInTheDocument();
  });
});
