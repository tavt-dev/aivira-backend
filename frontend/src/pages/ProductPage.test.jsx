import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import ProductPage from "./ProductPage.jsx";
import { renderWithProviders } from "../test/render.jsx";

describe("ProductPage", () => {
  it("renders book metadata and public reviews from backend data", async () => {
    renderWithProviders(
      <Routes>
        <Route path="/product/:slug" element={<ProductPage onAuth={() => {}} />} />
      </Routes>,
      { route: "/product/clean-architecture" }
    );

    expect(await screen.findByRole("heading", { name: /Clean Architecture/i })).toBeInTheDocument();
    expect(screen.getAllByText(/Robert C\. Martin/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/9780134494166/i)).toBeInTheDocument();
    expect(screen.getByText(/Prentice Hall/i)).toBeInTheDocument();
    expect(await screen.findByText(/Clear writing and excellent book quality/i)).toBeInTheDocument();
    expect(screen.getByText(/Thanks for reading with Aivira/i)).toBeInTheDocument();
  });
});
