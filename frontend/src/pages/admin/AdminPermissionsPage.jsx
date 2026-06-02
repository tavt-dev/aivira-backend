import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  getPermissions,
  getRolePermissions,
  getRoles,
  getUserPermissions,
  grantUserPermission,
  revokeUserPermission,
  updateRolePermissions,
} from "../../api/adminApi.js";
import { pageRows } from "../../utils/mappers.js";

export default function AdminPermissionsPage() {
  const { t } = useTranslation();
  const [permissions, setPermissions] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedRole, setSelectedRole] = useState("");
  const [rolePermissions, setRolePermissions] = useState([]);
  const [userId, setUserId] = useState("");
  const [userPermissions, setUserPermissions] = useState(null);
  const [grantForm, setGrantForm] = useState({ permissionCode: "", reason: "", expiresAt: "" });
  const [message, setMessage] = useState("");

  useEffect(() => {
    getPermissions()
      .then((rows) => setPermissions(pageRows(rows)))
      .catch((error) => setMessage(error.message || t("admin.errors.permissions")));
    getRoles()
      .then((rows) => {
        const list = pageRows(rows);
        setRoles(list);
        const first = list[0]?.code || list[0]?.roleCode;
        if (first) setSelectedRole(first);
      })
      .catch(() => {});
  }, []);

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
      await updateRolePermissions(
        selectedRole,
        rolePermissions.map((permission) => permission.code || permission.permissionCode || permission)
      );
      setMessage(t("admin.roleSaved"));
    } catch (error) {
      setMessage(error.message || t("admin.errors.roleUpdate"));
    }
  }

  async function loadUserPermissions(event) {
    event.preventDefault();
    setMessage("");
    try {
      setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || t("admin.errors.userPermissions"));
    }
  }

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
      if (userId) setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || t("admin.errors.grant"));
    }
  }

  async function revoke(permissionCode) {
    setMessage("");
    try {
      await revokeUserPermission(userId, permissionCode);
      setMessage(t("admin.permissionRevoked"));
      setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || t("admin.errors.revoke"));
    }
  }

  function toggleRolePermission(permission) {
    const code = permission.code || permission.permissionCode;
    const exists = rolePermissions.some((item) => (item.code || item.permissionCode || item) === code);
    setRolePermissions(
      exists
        ? rolePermissions.filter((item) => (item.code || item.permissionCode || item) !== code)
        : [...rolePermissions, permission]
    );
  }

  return (
    <div className="grid gap-8">
      <PageHeader title={t("admin.permissionsTitle")} eyebrow={t("admin.permissionsEyebrow")} />
      {message && <Notice>{message}</Notice>}
      <div className="grid gap-8 xl:grid-cols-2">
        <Panel title={t("admin.rolePermissions")}>
          <form className="grid gap-5" onSubmit={saveRole}>
            <Select value={selectedRole} onChange={(event) => setSelectedRole(event.target.value)}>
              {roles.map((role) => (
                <option key={role.code || role.roleCode} value={role.code || role.roleCode}>
                  {role.code || role.roleCode}
                </option>
              ))}
            </Select>
            <div className="grid max-h-[520px] gap-2 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
              {permissions.map((permission) => {
                const code = permission.code || permission.permissionCode;
                const checked = rolePermissions.some(
                  (item) => (item.code || item.permissionCode || item) === code
                );
                return (
                  <label key={code} className="flex items-center gap-3 rounded-xl bg-white p-3 text-sm font-semibold text-slate-700">
                    <input type="checkbox" checked={checked} onChange={() => toggleRolePermission(permission)} />
                    {code}
                  </label>
                );
              })}
            </div>
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
              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.effectivePermissions")}</h3>
              <div className="flex flex-wrap gap-2">
                {(userPermissions.effectivePermissions || []).map((permission) => (
                  <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-700" key={permission.code}>
                    {permission.code}
                  </span>
                ))}
              </div>

              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.directPermissions")}</h3>
              <div className="grid gap-3">
                {(userPermissions.directPermissions || []).map((permission) => (
                  <div className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 p-4" key={permission.permissionCode || permission.code}>
                    <span className="font-semibold text-slate-800">{permission.permissionCode || permission.code}</span>
                    <small className="text-slate-500">{permission.reason || t("admin.direct")}</small>
                    <SmallButton onClick={() => revoke(permission.permissionCode || permission.code)}>{t("account.revoke")}</SmallButton>
                  </div>
                ))}
              </div>
            </div>
          )}

          <form className="mt-6 grid gap-4 border-t border-slate-100 pt-6" onSubmit={grant}>
            <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.grantDirect")}</h3>
            <Select value={grantForm.permissionCode} onChange={(event) => setGrantForm({ ...grantForm, permissionCode: event.target.value })} required>
              <option value="">{t("admin.permission")}</option>
              {permissions.map((permission) => (
                <option key={permission.code} value={permission.code}>{permission.code}</option>
              ))}
            </Select>
            <Input value={grantForm.reason} onChange={(event) => setGrantForm({ ...grantForm, reason: event.target.value })} placeholder={t("admin.reason")} />
            <Input type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} />
            <Button secondary type="submit">{t("admin.grantPermission")}</Button>
          </form>
        </Panel>
      </div>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="border-b border-slate-200 pb-6"><span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span><h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2></div>;
}
function Panel({ title, children }) {
  return <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"><h3 className="mb-5 font-serif text-3xl font-bold text-slate-950">{title}</h3>{children}</section>;
}
function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}
function Select(props) {
  return <select {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}
function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}
function SmallButton(props) {
  return <button type="button" {...props} className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-bold text-slate-600 transition-colors hover:bg-white" />;
}
function Notice({ children }) {
  return <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
