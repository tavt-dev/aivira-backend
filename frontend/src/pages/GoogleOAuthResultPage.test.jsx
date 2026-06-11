import { screen, waitFor } from "@testing-library/react";
import { Route, Routes, useLocation } from "react-router-dom";
import { describe, expect, it } from "vitest";
import GoogleOAuthResultPage from "./GoogleOAuthResultPage.jsx";
import { renderWithProviders } from "../test/render.jsx";
import { getAuthSnapshot, getCurrentUser } from "../utils/storage.js";

describe("GoogleOAuthResultPage", () => {
  it("exchanges a login ticket, stores auth, and redirects to next path", async () => {
    renderGoogleRoutes("/auth/google/success?ticket=valid-ticket&next=/checkout");

    await waitFor(() => expect(screen.getByTestId("location")).toHaveTextContent("/checkout"));
    expect(getAuthSnapshot().accessToken).toBe("google-access-token");
    expect(getAuthSnapshot().refreshToken).toBe("google-refresh-token");
    expect(getCurrentUser()?.username).toBe("reader");
  });

  it("shows an error when the success redirect has no ticket", async () => {
    renderGoogleRoutes("/auth/google/success?next=/checkout");

    expect(await screen.findByText(/Google login ticket is missing|Thiếu hoặc hết hạn/)).toBeInTheDocument();
  });

  it("shows a readable failure callback error", async () => {
    renderGoogleRoutes("/auth/google/failure?error=GOOGLE_OAUTH_CODE_INVALID");

    expect(await screen.findByText("GOOGLE_OAUTH_CODE_INVALID")).toBeInTheDocument();
  });

  it("redirects non-admin Google users away from admin next paths", async () => {
    renderGoogleRoutes("/auth/google/success?ticket=valid-ticket&next=/admin/dashboard");

    await waitFor(() => expect(screen.getByTestId("location")).toHaveTextContent("/admin/forbidden"));
  });
});

function renderGoogleRoutes(route) {
  return renderWithProviders(
    <Routes>
      <Route path="/auth/google/success" element={<GoogleOAuthResultPage />} />
      <Route path="/auth/google/failure" element={<GoogleOAuthResultPage failure />} />
      <Route path="/checkout" element={<LocationMarker />} />
      <Route path="/admin/forbidden" element={<LocationMarker />} />
    </Routes>,
    { route }
  );
}

function LocationMarker() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}
