import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";

import {
  getPermissions,
  getRolePermissions,
  getRoles,
  getUserPermissions,
  grantUserPermission,
  revokeUserPermission,
  updateRolePermissions,
} from "../../api/adminApi.js";
import {
  Button,
  Input,
  Notice,
  PageHeader,
  Panel,
  Select,
  useConfirm,
} from "../../components/ui/index.jsx";
import { formatDateTime } from "../../utils/formatters.js";
import { pageRows } from "../../utils/mappers.js";

export default function AdminPermissionsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const [searchParams] = useSearchParams();
  const [permissions, setPermissions] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedRole, setSelectedRole] = useState("");
  const [rolePermissions, setRolePermissions] = useState([]);
  const [permissionSearch, setPermissionSearch] = useState("");
  const [userId, setUserId] = useState(searchParams.get("userId") || "");
  const [userPermissions, setUserPermissions] = useState(null);
  const [grantForm, setGrantForm] = useState({ permissionCode: "", reason: "", expiresAt: "" });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  const filteredPermissions = useMemo(() => {
    const keyword = permissionSearch.trim().toLowerCase();
    if (!keyword) return permissions;
    return permissions.filter((permission) => {
      const code = permissionCode(permission).toLowerCase();
      const group = String(permission.group || "").toLowerCase();
      return code.includes(keyword) || group.includes(keyword);
    });
  }, [permissionSearch, permissions]);

  useEffect(() => {
    setLoading(true);
    Promise.all([getPermissions(), getRoles()])
      .then(([permissionRows, roleRows]) => {
        const nextPermissions = pageRows(permissionRows);
        const nextRoles = pageRows(roleRows).filter((role) => ["USER", "ADMIN"].includes(roleCode(role)));
        setPermissions(nextPermissions);
        setRoles(nextRoles);
        const first = nextRoles[0]?.code || nextRoles[0]?.roleCode;
        if (first) setSelectedRole(first);
      })
      .catch((error) => setMessage(error.message || t("admin.errors.permissions")))
      .finally(() => setLoading(false));
  }, [t]);

  useEffect(() => {
    if (!selectedRole) return;
    getRolePermissions(selectedRole)
      .then((role) => setRolePermissions(pageRows(role?.permissions || role)))
      .catch(() => setRolePermissions([]));
  }, [selectedRole]);

  async function saveRole(event) {
    event.preventDefault();
    setMessage("");
    try {
      await updateRolePermissions(selectedRole, rolePermissions.map(permissionCode));
      setMessage(t("admin.roleSaved"));
    } catch (error) {
      setMessage(error.message || t("admin.errors.roleUpdate"));
    }
  }

  async function loadUserPermissions(event) {
    event.preventDefault();
    await loadUserPermissionsById(userId);
  }

  const loadUserPermissionsById = useCallback(async (nextUserId) => {
    if (!nextUserId) return;
    setMessage("");
    try {
      setUserPermissions(await getUserPermissions(nextUserId));
    } catch (error) {
      setUserPermissions(null);
      setMessage(error.message || t("admin.errors.userPermissions"));
    }
  }, [t]);

  useEffect(() => {
    const queryUserId = searchParams.get("userId");
    if (queryUserId) {
      setUserId(queryUserId);
      loadUserPermissionsById(queryUserId);
    }
  }, [loadUserPermissionsById, searchParams]);

  async function grant(event) {
    event.preventDefault();
    setMessage("");
    try {
      await grantUserPermission(userId, {
        permissionCode: grantForm.permissionCode,
        reason: grantForm.reason || null,
        expiresAt: grantForm.expiresAt ? new Date(grantForm.expiresAt).toISOString() : null,
      });
      setMessage(t("admin.permissionGranted"));
      setGrantForm({ permissionCode: "", reason: "", expiresAt: "" });
      await loadUserPermissionsById(userId);
    } catch (error) {
      setMessage(error.message || t("admin.errors.grant"));
    }
  }

  async function revoke(permission) {
    const code = directPermissionCode(permission);
    if (!code) return;
    const confirmed = await confirm({
      title: t("admin.revokePermission"),
      message: t("admin.confirmRevokePermission", { code }),
      confirmLabel: t("admin.revokePermission"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await revokeUserPermission(userId, code);
      setMessage(t("admin.permissionRevoked"));
      await loadUserPermissionsById(userId);
    } catch (error) {
      setMessage(error.message || t("admin.errors.revoke"));
    }
  }

  function toggleRolePermission(permission) {
    const code = permissionCode(permission);
    const exists = rolePermissions.some((item) => permissionCode(item) === code);
    setRolePermissions(
      exists
        ? rolePermissions.filter((item) => permissionCode(item) !== code)
        : [...rolePermissions, permission]
    );
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.permissionsTitle")} eyebrow={t("admin.permissionsEyebrow")} />
      {message && <Notice>{message}</Notice>}
      {loading && <Notice>{t("common.loading")}</Notice>}

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <Panel title={t("admin.rolePermissions")}>
          <form className="grid gap-5" onSubmit={saveRole}>
            <div className="grid gap-3 md:grid-cols-[180px_1fr]">
              <Select value={selectedRole} onChange={(event) => setSelectedRole(event.target.value)}>
                {roles.map((role) => (
                  <option key={roleCode(role)} value={roleCode(role)}>
                    {roleCode(role)}
                  </option>
                ))}
              </Select>
              <Input value={permissionSearch} onChange={(event) => setPermissionSearch(event.target.value)} placeholder={t("admin.permissionSearch")} />
            </div>
            <PermissionChecklist
              checkedPermissions={rolePermissions}
              permissions={filteredPermissions}
              onToggle={toggleRolePermission}
            />
            <Button type="submit">{t("common.save")}</Button>
          </form>
        </Panel>

        <Panel title={t("admin.userPermissions")}>
          <form className="grid gap-4" onSubmit={loadUserPermissions}>
            <Input value={userId} onChange={(event) => setUserId(event.target.value)} placeholder={t("admin.userId")} required />
            <Button type="submit">{t("admin.loadUser")}</Button>
          </form>

          {userPermissions && (
            <div className="mt-6 grid gap-5 border-t border-slate-100 pt-6">
              <PermissionSection
                permissions={userPermissions.effectivePermissions || []}
                title={t("admin.effectivePermissions")}
              />
              <PermissionSection
                permissions={userPermissions.rolePermissions || []}
                title={t("admin.roleGrantedPermissions")}
              />

              <section className="grid gap-3">
                <h3 className="text-xl font-bold text-slate-950">{t("admin.directPermissions")}</h3>
                {(userPermissions.directPermissions || []).map((permission) => (
                  <DirectPermissionRow
                    key={permission.id || directPermissionCode(permission)}
                    language={i18n.language}
                    onRevoke={() => revoke(permission)}
                    permission={permission}
                    t={t}
                  />
                ))}
                {!userPermissions.directPermissions?.length && (
                  <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">{t("admin.noDirectPermissions")}</p>
                )}
              </section>
            </div>
          )}

          <form className="mt-6 grid gap-4 border-t border-slate-100 pt-6" onSubmit={grant}>
            <h3 className="text-xl font-bold text-slate-950">{t("admin.grantDirect")}</h3>
            <Select value={grantForm.permissionCode} onChange={(event) => setGrantForm({ ...grantForm, permissionCode: event.target.value })} required>
              <option value="">{t("admin.permission")}</option>
              {permissions.map((permission) => (
                <option key={permissionCode(permission)} value={permissionCode(permission)}>{permissionCode(permission)}</option>
              ))}
            </Select>
            <Input maxLength={500} value={grantForm.reason} onChange={(event) => setGrantForm({ ...grantForm, reason: event.target.value })} placeholder={t("admin.reason")} />
            <Input type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} />
            <Button variant="secondary" type="submit">{t("admin.grantPermission")}</Button>
          </form>
        </Panel>
      </div>
    </div>
  );
}

function PermissionChecklist({ checkedPermissions, onToggle, permissions }) {
  return (
    <div className="grid max-h-[560px] gap-2 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-4">
      {permissions.map((permission) => {
        const code = permissionCode(permission);
        const checked = checkedPermissions.some((item) => permissionCode(item) === code);
        return (
          <label key={code} className="flex items-start gap-3 rounded-xl bg-white p-3 text-sm font-semibold text-slate-700">
            <input type="checkbox" checked={checked} onChange={() => onToggle(permission)} aria-label={code} />
            <span>
              <span className="block font-bold text-slate-900">{code}</span>
              <span className="block text-xs text-slate-500">{permission.group || ""} {permission.description ? `- ${permission.description}` : ""}</span>
            </span>
          </label>
        );
      })}
    </div>
  );
}

function PermissionSection({ permissions, title }) {
  return (
    <section className="grid gap-2">
      <h3 className="text-xl font-bold text-slate-950">{title}</h3>
      <div className="flex flex-wrap gap-2">
        {(permissions || []).map((permission) => (
          <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-700" key={permissionCode(permission)}>
            {permissionCode(permission)}
          </span>
        ))}
        {!permissions?.length && <span className="text-sm text-slate-500">-</span>}
      </div>
    </section>
  );
}

function DirectPermissionRow({ language, onRevoke, permission, t }) {
  const code = directPermissionCode(permission);
  return (
    <div className="grid gap-3 rounded-xl bg-slate-50 p-4 text-sm md:grid-cols-[1fr_auto] md:items-center">
      <div>
        <p className="font-bold text-slate-900">{code}</p>
        <p className="text-slate-500">{permission.reason || t("admin.direct")}</p>
        <p className="text-xs text-slate-500">
          {t("admin.grantedBy")}: {permission.grantedByUserId || "-"} / {t("admin.grantedAt")}: {formatDateTime(permission.grantedAt, language)} / {t("admin.expiresAt")}: {formatDateTime(permission.expiresAt, language)}
        </p>
        <p className="text-xs font-semibold text-slate-600">
          {permission.currentlyActive === false || permission.active === false ? t("admin.inactive") : t("common.active")}
        </p>
      </div>
      <Button size="sm" variant="secondary" onClick={onRevoke}>{t("admin.revokePermission")}</Button>
    </div>
  );
}

function permissionCode(permission) {
  return String(permission?.code || permission?.permissionCode || permission || "");
}

function directPermissionCode(permission) {
  return String(permission?.permission?.code || permission?.permissionCode || permission?.code || "");
}

function roleCode(role) {
  return String(role?.code || role?.roleCode || role || "");
}
