import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ReviewForm from "./ReviewForm.jsx";
import { renderWithProviders } from "../../test/render.jsx";

describe("ReviewForm", () => {
  it("requires a comment before submit", async () => {
    const onSubmit = vi.fn();
    renderWithProviders(<ReviewForm title="Review Clean Architecture" onSubmit={onSubmit} />);

    await userEvent.click(screen.getByRole("button", { name: /Submit review|Gửi đánh giá/ }));

    expect(
      screen.getByText(/Write a short review before submitting.|Vui lòng nhập nhận xét trước khi gửi./)
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("submits the backend review payload shape", async () => {
    const onSubmit = vi.fn();
    renderWithProviders(<ReviewForm title="Review Clean Architecture" onSubmit={onSubmit} />);

    await userEvent.type(
      screen.getByPlaceholderText(/Share your thoughts about this book|Chia sẻ cảm nhận/),
      "Good book quality."
    );
    await userEvent.click(screen.getByRole("button", { name: /Submit review|Gửi đánh giá/ }));

    expect(onSubmit).toHaveBeenCalledWith({
      rating: 5,
      comment: "Good book quality.",
      images: []
    });
  });
});
