import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AdminDiscountsPage from "./AdminDiscountsPage.jsx";
import { renderWithProviders } from "../../test/render.jsx";

describe("AdminDiscountsPage", () => {
  it("renders coupon and promotion tabs from backend data", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminDiscountsPage />);

    expect(await screen.findByText(/AIVIRA10/i)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Khuyến mãi|promotion/i }));
    expect(await screen.findByText(/Architecture Week/i)).toBeInTheDocument();
  });
});
