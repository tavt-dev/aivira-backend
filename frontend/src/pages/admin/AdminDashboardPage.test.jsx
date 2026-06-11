import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AdminDashboardPage from "./AdminDashboardPage.jsx";
import { renderWithProviders } from "../../test/render.jsx";

describe("AdminDashboardPage", () => {
  it("renders dashboard summary and operational book sections", async () => {
    renderWithProviders(<AdminDashboardPage />);

    expect(await screen.findByText(/Clean Architecture/i)).toBeInTheDocument();
    expect(screen.getByText(/Domain-Driven Design/i)).toBeInTheDocument();
    expect(screen.getAllByText(/810\.000/).length).toBeGreaterThan(0);
  });
});
