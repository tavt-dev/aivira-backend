export function getRoleCode(role) {
  if (!role) return "";
  const raw = typeof role === "string"
    ? role
    : role.code || role.roleCode || role.name || role.authority || role.value || "";
  return normalizeRoleCode(raw);
}

export function hasRole(user, roleCode) {
  const expected = normalizeRoleCode(roleCode);
  return (user?.roles || []).some((role) => getRoleCode(role) === expected);
}

export function hasAdminRole(user) {
  return hasRole(user, "ADMIN");
}

export function hasAdminTokenRole(accessToken) {
  const claims = parseJwtClaims(accessToken);
  const roleClaims = [
    claims?.scope,
    claims?.scp,
    claims?.roles,
    claims?.authorities,
    claims?.permissions
  ];

  return roleClaims
    .flatMap((value) => normalizeClaimList(value))
    .some((role) => normalizeRoleCode(role) === "ADMIN");
}

export function hasAdminAccess(user, accessToken) {
  return hasAdminRole(user) || hasAdminTokenRole(accessToken);
}

function normalizeRoleCode(value) {
  const code = String(value || "").trim().toUpperCase();
  return code.startsWith("ROLE_") ? code.slice(5) : code;
}

function normalizeClaimList(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  if (typeof value === "string") return value.split(/\s+/).filter(Boolean);
  return [];
}

function parseJwtClaims(token) {
  if (!token || typeof token !== "string") return null;
  const [, payload] = token.split(".");
  if (!payload) return null;

  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
    return JSON.parse(window.atob(padded));
  } catch {
    return null;
  }
}
