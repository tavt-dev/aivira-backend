const TOKEN_KEY = "aivira_access_token";
const REFRESH_KEY = "aivira_refresh_token";
const USER_KEY = "aivira_user";

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

export function getCurrentUser() {
  if (!getAccessToken()) return null;
  return readJson(USER_KEY, null);
}

export function saveAuth(auth, fallbackUser) {
  const accessToken = auth?.accessToken || auth?.token || auth?.jwt || auth?.access_token;
  const refreshToken = auth?.refreshToken || auth?.refresh_token;
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(auth?.user || fallbackUser || { username: "Aivira Reader" }));
  dispatchAuth();
}

export function saveCurrentUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  dispatchAuth();
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USER_KEY);
  dispatchAuth();
}

function dispatchAuth() {
  window.dispatchEvent(new Event("aivira-auth"));
}

function readJson(key, fallback) {
  const raw = localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}
