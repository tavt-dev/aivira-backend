import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it, vi } from "vitest";
import OrdersPage from "./OrdersPage.jsx";
import i18n from "../i18n.js";
import { apiResponse, customerUser, order, pageResponse, paymentGroup } from "../test/mockData.js";
import { renderWithProviders, seedAuth } from "../test/render.jsx";
import { server } from "../test/server.js";

const API = "http://localhost/api/v1";

describe("OrdersPage payment actions", () => {
  it("continues a pending online payment with the existing payment group", async () => {
    seedAuth(customerUser);
    const user = userEvent.setup();
    const pendingOrder = {
      ...order,
      orderStatus: "PENDING_PAYMENT",
      paymentMethod: "VNPAY",
      paymentStatus: "PENDING",
    };
    const getPaymentGroup = vi.fn();

    server.use(
      http.get(`${API}/orders`, () => HttpResponse.json(apiResponse(pageResponse([pendingOrder])))),
      http.get(`${API}/orders/:id`, () => HttpResponse.json(apiResponse(pendingOrder))),
      http.get(`${API}/payments/groups/:code`, ({ params }) => {
        getPaymentGroup(params.code);
        return HttpResponse.json(apiResponse({
          ...paymentGroup,
          paymentUrl: null,
          qrCodeUrl: "https://pay.example.test/qr.png",
        }));
      })
    );

    renderWithProviders(<OrdersPage onAuth={() => {}} />, { route: "/orders" });

    await screen.findByText(pendingOrder.orderCode);
    await user.click(screen.getByRole("button", { name: i18n.t("orders.detail") }));
    const continueButtons = await screen.findAllByRole("button", { name: i18n.t("orders.continuePayment") });
    await user.click(continueButtons.at(-1));

    await waitFor(() => expect(getPaymentGroup).toHaveBeenCalledWith(pendingOrder.paymentGroupCode));
    expect(await screen.findByRole("img", { name: i18n.t("checkout.scanQr") })).toHaveAttribute(
      "src",
      "https://pay.example.test/qr.png"
    );
  });

  it("retries failed online payments through the retry endpoint", async () => {
    seedAuth(customerUser);
    const user = userEvent.setup();
    const failedOrder = {
      ...order,
      orderStatus: "PAYMENT_FAILED",
      paymentMethod: "MOMO",
      paymentStatus: "FAILED",
    };
    const retryPayment = vi.fn();

    server.use(
      http.get(`${API}/orders`, () => HttpResponse.json(apiResponse(pageResponse([failedOrder])))),
      http.post(`${API}/payments/groups/:code/retry`, ({ params }) => {
        retryPayment(params.code);
        return HttpResponse.json(apiResponse({ ...paymentGroup, paymentUrl: null, qrCodeUrl: null }));
      })
    );

    renderWithProviders(<OrdersPage onAuth={() => {}} />, { route: "/orders" });

    await screen.findByText(failedOrder.orderCode);
    await user.click(screen.getByRole("button", { name: i18n.t("orders.retryPayment") }));

    await waitFor(() => expect(retryPayment).toHaveBeenCalledWith(failedOrder.paymentGroupCode));
  });

  it("does not offer online payment actions for COD orders", async () => {
    seedAuth(customerUser);
    renderWithProviders(<OrdersPage onAuth={() => {}} />, { route: "/orders" });

    await screen.findByText(order.orderCode);
    expect(screen.queryByRole("button", { name: i18n.t("orders.continuePayment") })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: i18n.t("orders.retryPayment") })).not.toBeInTheDocument();
  });
});
