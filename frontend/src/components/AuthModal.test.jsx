import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AuthModal, { buildGoogleAuthorizeUrl } from "./AuthModal.jsx";
import { renderWithProviders } from "../test/render.jsx";
import { getAuthSnapshot, getCurrentUser } from "../utils/storage.js";

describe("AuthModal", () => {
  it("stores auth, loads profile, and closes after login success", async () => {
    const onClose = vi.fn();
    renderWithProviders(<AuthModal open onClose={onClose} initialMode="login" />);

    await userEvent.type(screen.getByPlaceholderText(/you@example.com/), "reader");
    await userEvent.type(screen.getByPlaceholderText(/Enter your password|Nhập mật khẩu/), "pw");
    await userEvent.click(screen.getAllByRole("button", { name: /Login|Đăng nhập/ }).at(-1));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(getAuthSnapshot().accessToken).toBe("access-token");
    expect(getAuthSnapshot().refreshToken).toBe("refresh-token");
    expect(getCurrentUser()?.username).toBe("reader");
  });

  it("renders backend login error messages", async () => {
    renderWithProviders(<AuthModal open onClose={vi.fn()} initialMode="login" />);

    await userEvent.type(screen.getByPlaceholderText(/you@example.com/), "bad");
    await userEvent.type(screen.getByPlaceholderText(/Enter your password|Nhập mật khẩu/), "wrong");
    await userEvent.click(screen.getAllByRole("button", { name: /Login|Đăng nhập/ }).at(-1));

    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
  });

  it("renders Google login and builds authorize URL with next path", () => {
    renderWithProviders(<AuthModal open onClose={vi.fn()} initialMode="login" nextPath="/checkout" />);

    expect(screen.getByRole("button", { name: /Continue with Google|Tiếp tục với Google/ })).toBeInTheDocument();
    const url = new URL(buildGoogleAuthorizeUrl("/checkout"));
    expect(url.pathname).toBe("/api/v1/auth/google/authorize");
    expect(url.searchParams.get("next")).toBe("/checkout");
  });

  it("keeps Google login available in register mode", () => {
    renderWithProviders(<AuthModal open onClose={vi.fn()} initialMode="register" />);

    expect(screen.getByRole("button", { name: /Continue with Google|Tiếp tục với Google/ })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Username|Tên đăng nhập/)).toBeInTheDocument();
  });
});
