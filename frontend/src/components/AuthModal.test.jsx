import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AuthModal from "./AuthModal.jsx";
import { renderWithProviders } from "../test/render.jsx";
import { getAuthSnapshot, getCurrentUser } from "../utils/storage.js";

describe("AuthModal", () => {
  it("stores auth, loads profile, and closes after login success", async () => {
    const onClose = vi.fn();
    renderWithProviders(<AuthModal open onClose={onClose} initialMode="login" />);

    await userEvent.type(screen.getByPlaceholderText(/Username|Tên đăng nhập/), "reader");
    await userEvent.type(screen.getByPlaceholderText(/Password|Mật khẩu/), "pw");
    await userEvent.click(screen.getAllByRole("button", { name: /Login|Đăng nhập/ }).at(-1));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(getAuthSnapshot().accessToken).toBe("access-token");
    expect(getAuthSnapshot().refreshToken).toBe("refresh-token");
    expect(getCurrentUser()?.username).toBe("reader");
  });

  it("renders backend login error messages", async () => {
    renderWithProviders(<AuthModal open onClose={vi.fn()} initialMode="login" />);

    await userEvent.type(screen.getByPlaceholderText(/Username|Tên đăng nhập/), "bad");
    await userEvent.type(screen.getByPlaceholderText(/Password|Mật khẩu/), "wrong");
    await userEvent.click(screen.getAllByRole("button", { name: /Login|Đăng nhập/ }).at(-1));

    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
  });
});
