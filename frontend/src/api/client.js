import { clearAuth, getAccessToken, getRefreshToken, notifyAuthExpired, saveAuth } from "../utils/storage.js";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";
let refreshPromise = null;

export async function request(path, options = {}) {
  const response = await send(path, options);
  const payload = await parsePayload(response);

  if (response.status === 401 && shouldRefresh(path, options)) {
    try {
      await refreshAccessToken();
      const retryResponse = await send(path, options);
      const retryPayload = await parsePayload(retryResponse);
      return handleResponse(retryResponse, retryPayload);
    } catch (refreshError) {
      clearAuth();
      notifyAuthExpired();
      throw refreshError;
    }
  }

  return handleResponse(response, payload);
}

async function send(path, options = {}) {
  const { body, headers: optionHeaders, skipAuth = false, ...fetchOptions } = options;
  const headers = new Headers(optionHeaders || {});
  const hasBody = body !== undefined && body !== null;
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

  if (hasBody && !isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (!skipAuth) {
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  return fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    ...fetchOptions,
    headers,
    body: isFormData || typeof body === "string" ? body : hasBody ? JSON.stringify(body) : undefined
  });
}

async function parsePayload(response) {
  const text = await response.text();
  if (text) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return null;
}

function handleResponse(response, payload) {
  if (!response.ok) {
    throw toApiError(response, payload);
  }

  return payload?.data ?? payload;
}

function toApiError(response, payload) {
  const message = payload?.message || payload?.error || payload?.error_description || `Request failed (${response.status})`;
  const error = new Error(message);
  error.status = response.status;
  error.errorCode = payload?.errorCode || payload?.code;
  error.payload = payload;
  error.data = payload?.data;
  return error;
}

function shouldRefresh(path, options = {}) {
  if (options.skipAuth || options.skipRefresh) return false;
  return !["/auth/token", "/auth/refresh-token", "/auth/logout"].some((authPath) => path.startsWith(authPath));
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function doRefresh() {
  const refreshToken = getRefreshToken();
  const body = refreshToken ? JSON.stringify({ refreshToken }) : "{}";

  const response = await fetch(`${API_BASE_URL}/auth/refresh-token`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json"
    },
    body
  });
  const payload = await parsePayload(response);

  if (!response.ok) {
    throw toApiError(response, payload);
  }

  const auth = payload?.data ?? payload;
  saveAuth(auth);
  return auth;
}

export function query(params = {}) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && item !== "") {
          searchParams.append(key, String(item));
        }
      });
      return;
    }

    searchParams.set(key, String(value));
  });

  const search = searchParams.toString();
  return search ? `?${search}` : "";
}
