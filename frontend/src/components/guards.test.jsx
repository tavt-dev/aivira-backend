import { screen, waitFor } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import RequireAdmin from "./RequireAdmin.jsx";
import RequireAuth from "./RequireAuth.jsx";
import { renderWithProviders, seedAuth } from "../test/render.jsx";
import { adminUser, customerUser } from "../test/mockData.js";

describe("route guards", () => {
  it("redirects guest customer routes to login with next path", () => {
    renderWithProviders(
      <Routes>
        <Route path="/" element={<div>Home</div>} />
        <Route
          path="/cart"
          element={
            <RequireAuth>
              <div>Cart</div>
            </RequireAuth>
          }
        />
      </Routes>,
      { route: "/cart?x=1" }
    );

    expect(screen.getByText("Home")).toBeInTheDocument();
  });

  it("allows authenticated customer routes", () => {
    seedAuth(customerUser);
    renderWithProviders(
      <Routes>
        <Route
          path="/cart"
          element={
            <RequireAuth>
              <div>Cart</div>
            </RequireAuth>
          }
        />
      </Routes>,
      { route: "/cart" }
    );

    expect(screen.getByText("Cart")).toBeInTheDocument();
  });

  it("allows admin profile through admin guard", async () => {
    seedAuth(adminUser, "admin-token");
    renderWithProviders(
      <Routes>
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <div>Admin area</div>
            </RequireAdmin>
          }
        />
        <Route path="/admin/forbidden" element={<div>Forbidden</div>} />
        <Route path="/" element={<div>Home</div>} />
      </Routes>,
      { route: "/admin" }
    );

    await waitFor(() => expect(screen.getByText("Admin area")).toBeInTheDocument());
  });

  it("redirects normal users away from admin routes", async () => {
    seedAuth(customerUser, "access-token");
    renderWithProviders(
      <Routes>
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <div>Admin area</div>
            </RequireAdmin>
          }
        />
        <Route path="/admin/forbidden" element={<div>Forbidden</div>} />
        <Route path="/" element={<div>Home</div>} />
      </Routes>,
      { route: "/admin" }
    );

    await waitFor(() => expect(screen.getByText("Forbidden")).toBeInTheDocument());
  });
});
