import { render } from "@testing-library/react";
import { I18nextProvider } from "react-i18next";
import { MemoryRouter } from "react-router-dom";
import i18n from "../i18n.js";
import { ConfirmDialogProvider, ToastProvider } from "../components/ui/index.jsx";

export function renderWithProviders(ui, { route = "/", router = true } = {}) {
  window.history.pushState({}, "Test", route);
  const tree = (
    <I18nextProvider i18n={i18n}>
      <ToastProvider>
        <ConfirmDialogProvider>
          {router ? <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter> : ui}
        </ConfirmDialogProvider>
      </ToastProvider>
    </I18nextProvider>
  );

  return render(tree);
}

export function seedAuth(
  user = { id: "user-1", username: "reader", roles: [{ code: "USER" }] },
  token = "access-token"
) {
  localStorage.setItem("aivira_access_token", token);
  localStorage.setItem("aivira_refresh_token", "refresh-token");
  localStorage.setItem("aivira_user", JSON.stringify(user));
}
