import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import CheckoutPage from "./CheckoutPage.jsx";
import i18n from "../i18n.js";
import { saveCheckoutCartItemIds } from "../utils/checkoutSelection.js";
import { renderWithProviders, seedAuth } from "../test/render.jsx";
import { customerUser } from "../test/mockData.js";

describe("CheckoutPage", () => {
  it("renders backend preview totals and coupon errors", async () => {
    seedAuth(customerUser);
    saveCheckoutCartItemIds([701]);
    const user = userEvent.setup();

    renderWithProviders(
      <Routes>
        <Route path="/checkout" element={<CheckoutPage onAuth={() => {}} />} />
      </Routes>,
      { route: "/checkout" }
    );

    await user.selectOptions(await screen.findByRole("combobox"), "801");
    expect((await screen.findAllByText(/Architecture Week/i)).length).toBeGreaterThan(0);
    await waitFor(() => expect(screen.getAllByText(/850\.000/).length).toBeGreaterThan(0));

    await user.type(screen.getByPlaceholderText(/coupon/i), "BAD");
    await waitFor(() => expect(screen.getByText(/Coupon is invalid/i)).toBeInTheDocument(), { timeout: 2000 });
  });

  it("shows an empty state and blocks ordering when no cart items were selected", async () => {
    seedAuth(customerUser);

    renderWithProviders(
      <Routes>
        <Route path="/checkout" element={<CheckoutPage onAuth={() => {}} />} />
      </Routes>,
      { route: "/checkout" }
    );

    expect(await screen.findByText(i18n.t("checkout.emptyTitle"))).toBeInTheDocument();
    expect(screen.getByRole("link", { name: i18n.t("checkout.backToCart") })).toHaveAttribute("href", "/cart");
    expect(screen.getByRole("button", { name: i18n.t("checkout.placeOrder") })).toBeDisabled();
    expect(screen.queryByText(i18n.t("checkout.finalTotal"))).not.toBeInTheDocument();
  });
});
