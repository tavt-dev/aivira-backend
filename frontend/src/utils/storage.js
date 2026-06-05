const TOKEN_KEY = "aivira_access_token";
const REFRESH_KEY = "aivira_refresh_token";
const USER_KEY = "aivira_user";
const PENDING_VERIFY_KEY = "aivira_pending_verify";
const PENDING_VERIFY_TTL_MS = 15 * 60 * 1000;

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function hasAccessToken() {
  return Boolean(getAccessToken());
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

export function getCurrentUser() {
  if (!getAccessToken()) return null;
  return readJson(USER_KEY, null);
}

export function getAuthSnapshot() {
  return {
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    user: getCurrentUser()
  };
}

export function saveAccessToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
    dispatchAuth();
  }
}

export function saveRefreshToken(refreshToken) {
  if (refreshToken) {
    localStorage.setItem(REFRESH_KEY, refreshToken);
    dispatchAuth();
  }
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

export function notifyAuthExpired() {
  window.dispatchEvent(new Event("aivira-auth-expired"));
}

export function savePendingVerify(context) {
  sessionStorage.setItem(PENDING_VERIFY_KEY, JSON.stringify({
    email: context?.email || "",
    username: context?.username || "",
    source: context?.source || "auth",
    createdAt: Date.now()
  }));
}

export function getPendingVerify() {
  const context = readSessionJson(PENDING_VERIFY_KEY, null);
  if (!context?.createdAt || Date.now() - context.createdAt > PENDING_VERIFY_TTL_MS) {
    clearPendingVerify();
    return null;
  }
  return context;
}

export function clearPendingVerify() {
  sessionStorage.removeItem(PENDING_VERIFY_KEY);
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

function readSessionJson(key, fallback) {
  const raw = sessionStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}
