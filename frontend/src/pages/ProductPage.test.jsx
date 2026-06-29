import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import ProductPage from "./ProductPage.jsx";
import i18n from "../i18n.js";
import { customerUser } from "../test/mockData.js";
import { renderWithProviders, seedAuth } from "../test/render.jsx";
import { getStoredCheckoutCartItemIds } from "../utils/checkoutSelection.js";

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

  it("adds the selected variation and opens checkout with its cart item", async () => {
    seedAuth(customerUser);
    const user = userEvent.setup();

    renderWithProviders(
      <Routes>
        <Route path="/product/:slug" element={<ProductPage onAuth={() => {}} />} />
        <Route path="/checkout" element={<div>Checkout target</div>} />
      </Routes>,
      { route: "/product/clean-architecture" }
    );

    await screen.findByRole("heading", { name: /Clean Architecture/i });
    await user.click(screen.getByRole("button", { name: i18n.t("product.checkout") }));

    expect(await screen.findByText("Checkout target")).toBeInTheDocument();
    await waitFor(() => expect(getStoredCheckoutCartItemIds()).toEqual([701]));
  });

  it("opens authentication instead of checkout when the reader is logged out", async () => {
    const onAuth = vi.fn();
    const user = userEvent.setup();

    renderWithProviders(
      <Routes>
        <Route path="/product/:slug" element={<ProductPage onAuth={onAuth} />} />
        <Route path="/checkout" element={<div>Checkout target</div>} />
      </Routes>,
      { route: "/product/clean-architecture" }
    );

    await screen.findByRole("heading", { name: /Clean Architecture/i });
    await user.click(screen.getByRole("button", { name: i18n.t("product.checkout") }));

    expect(onAuth).toHaveBeenCalledOnce();
    expect(screen.queryByText("Checkout target")).not.toBeInTheDocument();
    expect(getStoredCheckoutCartItemIds()).toEqual([]);
  });
});
