import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import AdminPaymentsPage from "./AdminPaymentsPage.jsx";
import { renderWithProviders } from "../../test/render.jsx";

describe("AdminPaymentsPage", () => {
  it("looks up a payment group and displays reconcile result", async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/admin/payments" element={<AdminPaymentsPage />} />
      </Routes>,
      { route: "/admin/payments?code=PG-20260611-001" }
    );

    expect(await screen.findByText(/PG-20260611-001/i)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Chi tiết|detail/i }));
    expect(screen.getAllByText(/ORD-20260611-001/i).length).toBeGreaterThan(0);
    await user.click(screen.getByRole("button", { name: /close/i }));
    await user.click(screen.getByRole("button", { name: /Đối soát|reconcile/i }));
    expect(await screen.findByText(/Payment status reconciled/i)).toBeInTheDocument();
  });
});
