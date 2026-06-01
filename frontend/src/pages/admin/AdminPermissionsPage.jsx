import { useEffect, useState } from "react";
import { getPermissions, getRolePermissions, getRoles, getUserPermissions, grantUserPermission, revokeUserPermission, updateRolePermissions } from "../../api/adminApi.js";
import { pageRows } from "../../utils/mappers.js";

export default function AdminPermissionsPage() {
  const [permissions, setPermissions] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedRole, setSelectedRole] = useState("");
  const [rolePermissions, setRolePermissions] = useState([]);
  const [userId, setUserId] = useState("");
  const [userPermissions, setUserPermissions] = useState(null);
  const [grantForm, setGrantForm] = useState({ permissionCode: "", reason: "", expiresAt: "" });
  const [message, setMessage] = useState("");

  useEffect(() => {
    getPermissions().then((rows) => setPermissions(pageRows(rows))).catch((error) => setMessage(error.message || "Permissions unavailable."));
    getRoles().then((rows) => {
      const list = pageRows(rows);
      setRoles(list);
      const first = list[0]?.code || list[0]?.roleCode;
      if (first) setSelectedRole(first);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!selectedRole) return;
    getRolePermissions(selectedRole).then((role) => {
      setRolePermissions(pageRows(role?.permissions || role));
    }).catch(() => setRolePermissions([]));
  }, [selectedRole]);

  async function saveRole(event) {
    event.preventDefault();
    setMessage("");
    try {
      await updateRolePermissions(selectedRole, rolePermissions.map((permission) => permission.code || permission.permissionCode || permission));
      setMessage("Role permissions updated.");
    } catch (error) {
      setMessage(error.message || "Update role permissions failed.");
    }
  }

  async function loadUserPermissions(event) {
    event.preventDefault();
    setMessage("");
    try {
      setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || "User permissions unavailable.");
    }
  }

  async function grant(event) {
    event.preventDefault();
    setMessage("");
    try {
      await grantUserPermission(userId, {
        permissionCode: grantForm.permissionCode,
        reason: grantForm.reason || null,
        expiresAt: grantForm.expiresAt ? new Date(grantForm.expiresAt).toISOString() : null
      });
      setMessage("Permission granted.");
      if (userId) setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || "Grant permission failed.");
    }
  }

  async function revoke(permissionCode) {
    setMessage("");
    try {
      await revokeUserPermission(userId, permissionCode);
      setMessage("Permission revoked.");
      setUserPermissions(await getUserPermissions(userId));
    } catch (error) {
      setMessage(error.message || "Revoke permission failed.");
    }
  }

  function toggleRolePermission(permission) {
    const code = permission.code || permission.permissionCode;
    const exists = rolePermissions.some((item) => (item.code || item.permissionCode || item) === code);
    setRolePermissions(exists ? rolePermissions.filter((item) => (item.code || item.permissionCode || item) !== code) : [...rolePermissions, permission]);
  }

  return (
    <>
      <PageHeader title="Admin Permissions" eyebrow="Backend RBAC endpoints" />
      {message && <div className="notice page-notice">{message}</div>}
      <div className="dashboard-grid">
        <form className="panel form-panel" onSubmit={saveRole}>
          <h3>Role permissions</h3>
          <select value={selectedRole} onChange={(e) => setSelectedRole(e.target.value)}>
            {roles.map((role) => <option key={role.code || role.roleCode} value={role.code || role.roleCode}>{role.code || role.roleCode}</option>)}
          </select>
          <div className="permission-list">
            {permissions.map((permission) => {
              const code = permission.code || permission.permissionCode;
              const checked = rolePermissions.some((item) => (item.code || item.permissionCode || item) === code);
              return <label key={code} className="check-line"><input type="checkbox" checked={checked} onChange={() => toggleRolePermission(permission)} /> {code}</label>;
            })}
          </div>
          <button className="btn-fill" type="submit">Save role</button>
        </form>

        <div className="panel form-panel">
          <form className="compact-form form-panel" onSubmit={loadUserPermissions}>
            <h3>User permissions</h3>
            <input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="User ID" required />
            <button className="btn-fill" type="submit">Load user</button>
          </form>
          {userPermissions && (
            <div className="form-panel compact-form">
              <h3>Effective permissions</h3>
              {(userPermissions.effectivePermissions || []).map((permission) => <span className="tag" key={permission.code}>{permission.code}</span>)}
              <h3>Direct permissions</h3>
              {(userPermissions.directPermissions || []).map((permission) => (
                <div className="mini-row" key={permission.permissionCode || permission.code}>
                  <span>{permission.permissionCode || permission.code}</span>
                  <small>{permission.reason || "Direct"}</small>
                  <button type="button" onClick={() => revoke(permission.permissionCode || permission.code)}>Revoke</button>
                </div>
              ))}
            </div>
          )}
          <form className="compact-form form-panel" onSubmit={grant}>
            <h3>Grant direct permission</h3>
            <select value={grantForm.permissionCode} onChange={(e) => setGrantForm({ ...grantForm, permissionCode: e.target.value })} required>
              <option value="">Permission</option>
              {permissions.map((permission) => <option key={permission.code} value={permission.code}>{permission.code}</option>)}
            </select>
            <input value={grantForm.reason} onChange={(e) => setGrantForm({ ...grantForm, reason: e.target.value })} placeholder="Reason" />
            <input type="datetime-local" value={grantForm.expiresAt} onChange={(e) => setGrantForm({ ...grantForm, expiresAt: e.target.value })} />
            <button className="btn-line dark" type="submit">Grant permission</button>
          </form>
        </div>
      </div>
    </>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
