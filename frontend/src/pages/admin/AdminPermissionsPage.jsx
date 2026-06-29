import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import {
  Shield, Search, Plus, Trash2, X, RefreshCw, Key, User, Save,
} from "lucide-react";

import {
  getPermissions, getRolePermissions, getRoles, getUserPermissions,
  grantUserPermission, revokeUserPermission, updateRolePermissions,
} from "../../api/adminApi.js";
import { Drawer, Modal, useConfirm } from "../../components/ui/index.jsx";
import { formatDateTime } from "../../utils/formatters.js";
import { pageRows } from "../../utils/mappers.js";

/* ── Shared UI Primitives ──────────────────────── */
function PInput({ label, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</label>}
      <input className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-shadow" {...props}/>
    </div>
  );
}
function PSelect({ label, children, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</label>}
      <select className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow" {...props}>
        {children}
      </select>
    </div>
  );
}
function PrimaryBtn({ children, loading, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }}
      className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-200 dark:shadow-none transition-colors disabled:opacity-50" {...props}>
      {loading && <RefreshCw size={13} className="animate-spin"/>}
      {children}
    </motion.button>
  );
}
function SecBtn({ children, danger, icon: Icon, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }}
      className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition-colors disabled:opacity-50 ${
        danger
          ? "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400"
          : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
      }`} {...props}>
      {Icon && <Icon size={13}/>}
      {children}
    </motion.button>
  );
}
function Toast({ message, onClose }) {
  if (!message) return null;
  const isError = /err|fail|lỗi|không|invalid|required/i.test(message);
  return (
    <motion.div initial={{ opacity:0, y:-8 }} animate={{ opacity:1, y:0 }} exit={{ opacity:0, y:-8 }}
      className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm font-semibold mb-4 ${
        isError ? "border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400"
                : "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-400"
      }`}>
      <span>{message}</span>
      <button type="button" onClick={onClose}><X size={14}/></button>
    </motion.div>
  );
}
function Card({ title, icon: Icon, children }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden h-full flex flex-col">
      <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4 flex-shrink-0">
        {Icon && <Icon size={16} className="text-indigo-500"/>}
        <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h3>
      </div>
      <div className="p-5 flex-1 flex flex-col gap-4">{children}</div>
    </div>
  );
}

/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminPermissionsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const [searchParams] = useSearchParams();

  // ── State (100% preserved) ──────────────────
  const [permissions, setPermissions]           = useState([]);
  const [roles, setRoles]                       = useState([]);
  const [selectedRole, setSelectedRole]         = useState("");
  const [rolePermissions, setRolePermissions]   = useState([]);
  const [permissionSearch, setPermissionSearch] = useState("");
  const [userId, setUserId]                     = useState(searchParams.get("userId") || "");
  const [userPermissions, setUserPermissions]   = useState(null);
  const [grantForm, setGrantForm]               = useState({ permissionCode:"", reason:"", expiresAt:"" });
  const [roleDrawerOpen, setRoleDrawerOpen]     = useState(false);
  const [userDrawerOpen, setUserDrawerOpen]     = useState(false);
  const [grantModalOpen, setGrantModalOpen]     = useState(false);
  const [loading, setLoading]                   = useState(true);
  const [message, setMessage]                   = useState("");

  const filteredPermissions = useMemo(() => {
    const keyword = permissionSearch.trim().toLowerCase();
    if (!keyword) return permissions;
    return permissions.filter(p => {
      const code = permissionCode(p).toLowerCase();
      const group = String(p.group || "").toLowerCase();
      return code.includes(keyword) || group.includes(keyword);
    });
  }, [permissionSearch, permissions]);

  useEffect(() => {
    setLoading(true);
    Promise.all([getPermissions(), getRoles()])
      .then(([permissionRows, roleRows]) => {
        const nextPermissions = pageRows(permissionRows);
        const nextRoles = pageRows(roleRows).filter(r => ["USER","ADMIN"].includes(roleCode(r)));
        setPermissions(nextPermissions); setRoles(nextRoles);
        const first = nextRoles[0]?.code || nextRoles[0]?.roleCode;
        if (first) setSelectedRole(first);
      })
      .catch(error => setMessage(error.message || t("admin.errors.permissions")))
      .finally(() => setLoading(false));
  }, [t]);

  useEffect(() => {
    if (!selectedRole) return;
    getRolePermissions(selectedRole)
      .then(role => setRolePermissions(pageRows(role?.permissions || role)))
      .catch(() => setRolePermissions([]));
  }, [selectedRole]);

  async function saveRole(event) {
    event.preventDefault(); setMessage("");
    try {
      await updateRolePermissions(selectedRole, rolePermissions.map(permissionCode));
      setMessage(t("admin.roleSaved"));
    } catch (error) { setMessage(error.message || t("admin.errors.roleUpdate")); }
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
      setUserDrawerOpen(true);
    }
    catch (error) { setUserPermissions(null); setMessage(error.message || t("admin.errors.userPermissions")); }
  }, [t]);

  useEffect(() => {
    const queryUserId = searchParams.get("userId");
    if (queryUserId) { setUserId(queryUserId); loadUserPermissionsById(queryUserId); }
  }, [loadUserPermissionsById, searchParams]);

  async function grant(event) {
    event.preventDefault(); setMessage("");
    try {
      await grantUserPermission(userId, {
        permissionCode: grantForm.permissionCode,
        reason: grantForm.reason || null,
        expiresAt: grantForm.expiresAt ? new Date(grantForm.expiresAt).toISOString() : null,
      });
      setMessage(t("admin.permissionGranted"));
      setGrantForm({ permissionCode:"", reason:"", expiresAt:"" });
      setGrantModalOpen(false);
      await loadUserPermissionsById(userId);
    } catch (error) { setMessage(error.message || t("admin.errors.grant")); }
  }

  async function revoke(permission) {
    const code = directPermissionCode(permission);
    if (!code) return;
    const confirmed = await confirm({
      title: t("admin.revokePermission"), message: t("admin.confirmRevokePermission", { code }),
      confirmLabel: t("admin.revokePermission"), cancelLabel: t("common.cancel"), danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await revokeUserPermission(userId, code);
      setMessage(t("admin.permissionRevoked"));
      await loadUserPermissionsById(userId);
    } catch (error) { setMessage(error.message || t("admin.errors.revoke")); }
  }

  function toggleRolePermission(permission) {
    const code = permissionCode(permission);
    const exists = rolePermissions.some(item => permissionCode(item) === code);
    setRolePermissions(exists ? rolePermissions.filter(item => permissionCode(item) !== code) : [...rolePermissions, permission]);
  }

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="max-w-full space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.permissionsTitle","Quản lý quyền hạn")}</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.permissionsEyebrow","Phân quyền theo vai trò và người dùng")}</p>
      </div>

      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {loading && (
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-400">
          <RefreshCw size={14} className="animate-spin"/> {t("common.loading","Đang tải...")}
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        {/* Role permissions */}
        <Card title={t("admin.rolePermissions","Quyền theo vai trò")} icon={Shield}>
          <div className="grid gap-4">
            <PSelect label={t("admin.role","Vai trò")} value={selectedRole} onChange={e => setSelectedRole(e.target.value)}>
              {roles.map(role => <option key={roleCode(role)} value={roleCode(role)}>{roleCode(role)}</option>)}
            </PSelect>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold text-slate-600 dark:border-slate-700 dark:bg-slate-800/40 dark:text-slate-300">
              {t("admin.permissionCount","{{count}} quyền đang được gán", { count: rolePermissions.length })}
            </div>
            <PrimaryBtn type="button" onClick={() => setRoleDrawerOpen(true)}>{t("admin.editRolePermissions","Sửa quyền vai trò")}</PrimaryBtn>
          </div>
        </Card>

        {/* User permissions */}
        <Card title={t("admin.userPermissions","Quyền người dùng")} icon={User}>
          {/* Lookup form */}
          <form className="grid gap-3 sm:grid-cols-[1fr_auto]" onSubmit={loadUserPermissions}>
            <PInput value={userId} onChange={e => setUserId(e.target.value)} placeholder={t("admin.userId","Nhập User ID...")} required/>
            <PrimaryBtn type="submit">{t("admin.loadUser","Tra cứu")}</PrimaryBtn>
          </form>
          <div className="flex flex-wrap gap-2">
            <SecBtn disabled={!userPermissions} type="button" onClick={() => setUserDrawerOpen(true)}>{t("common.detail","Chi tiết")}</SecBtn>
            <SecBtn icon={Plus} disabled={!userId} type="button" onClick={() => setGrantModalOpen(true)}>{t("admin.grantPermission","Cấp quyền")}</SecBtn>
          </div>
        </Card>
      </div>
      <RolePermissionDrawer
        filteredPermissions={filteredPermissions}
        onClose={() => setRoleDrawerOpen(false)}
        onPermissionSearch={setPermissionSearch}
        onSubmit={saveRole}
        onToggle={toggleRolePermission}
        open={roleDrawerOpen}
        permissionSearch={permissionSearch}
        role={selectedRole}
        rolePermissions={rolePermissions}
        t={t}
      />
      <UserPermissionDrawer
        language={i18n.language}
        onClose={() => setUserDrawerOpen(false)}
        onGrant={() => setGrantModalOpen(true)}
        onRevoke={revoke}
        open={userDrawerOpen && Boolean(userPermissions)}
        t={t}
        userPermissions={userPermissions}
      />
      <GrantPermissionModal
        form={grantForm}
        onChange={setGrantForm}
        onClose={() => setGrantModalOpen(false)}
        onSubmit={grant}
        open={grantModalOpen}
        permissions={permissions}
        t={t}
        userId={userId}
      />
    </motion.div>
  );
}

/* ── Sub-components (all logic preserved) ──────── */
function RolePermissionDrawer({ filteredPermissions, onClose, onPermissionSearch, onSubmit, onToggle, open, permissionSearch, role, rolePermissions, t }) {
  return (
    <Drawer
      description={t("admin.rolePermissionHelp","Chọn các quyền hiệu lực cho vai trò này")}
      onClose={onClose}
      open={open}
      size="lg"
      title={t("admin.rolePermissionsFor","Quyền vai trò: {{role}}", { role })}
    >
      <form className="grid gap-4" onSubmit={onSubmit}>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400"/>
          <input
            value={permissionSearch}
            onChange={e => onPermissionSearch(e.target.value)}
            placeholder={t("admin.permissionSearch","Tìm kiếm quyền...")}
            className="w-full rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-4 text-sm text-slate-900 placeholder-slate-400 outline-none transition-shadow focus:ring-2 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
          />
        </div>
        <div className="grid max-h-[calc(100vh-250px)] gap-1.5 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/40">
          {filteredPermissions.map(permission => {
            const code = permissionCode(permission);
            const checked = rolePermissions.some(item => permissionCode(item) === code);
            return (
              <label key={code} className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors ${
                checked
                  ? "border-indigo-200/60 bg-indigo-50 dark:border-indigo-500/20 dark:bg-indigo-500/10"
                  : "border-transparent bg-white hover:border-slate-200 dark:bg-slate-900 dark:hover:border-slate-700"
              }`}>
                <input type="checkbox" checked={checked} onChange={() => onToggle(permission)} aria-label={code} className="mt-0.5 h-4 w-4 flex-shrink-0 rounded text-indigo-600"/>
                <span>
                  <span className="block text-xs font-bold text-slate-900 dark:text-slate-100">{code}</span>
                  <span className="mt-0.5 block text-xs text-slate-400">{permission.group || ""}{permission.description ? ` · ${permission.description}` : ""}</span>
                </span>
              </label>
            );
          })}
          {!filteredPermissions.length && (
            <div className="py-8 text-center text-sm text-slate-400">{t("admin.noPermissions","Không tìm thấy quyền hạn")}</div>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <PrimaryBtn type="submit" icon={Save}>{t("common.save","Lưu quyền hạn")}</PrimaryBtn>
          <SecBtn type="button" onClick={onClose}>{t("common.close","Đóng")}</SecBtn>
        </div>
      </form>
    </Drawer>
  );
}

function UserPermissionDrawer({ language, onClose, onGrant, onRevoke, open, t, userPermissions }) {
  if (!userPermissions) return null;
  return (
    <Drawer
      description={t("admin.userPermissionHelp","Xem quyền hiệu lực, quyền từ vai trò và quyền trực tiếp")}
      onClose={onClose}
      open={open}
      size="lg"
      title={t("admin.userPermissions","Quyền người dùng")}
    >
      <div className="grid gap-5">
        <section className="grid gap-2">
          <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{t("admin.effectivePermissions","Quyền hiệu lực")}</h4>
          <PermissionChips empty={t("common.none","—")} permissions={userPermissions.effectivePermissions || []}/>
        </section>
        <section className="grid gap-2">
          <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{t("admin.roleGrantedPermissions","Quyền từ vai trò")}</h4>
          <PermissionChips empty={t("common.none","—")} permissions={userPermissions.rolePermissions || []} variant="sky"/>
        </section>
        <section className="grid gap-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{t("admin.directPermissions","Quyền trực tiếp")}</h4>
            <SecBtn icon={Plus} type="button" onClick={onGrant}>{t("admin.grantPermission","Cấp quyền")}</SecBtn>
          </div>
          <div className="grid gap-2">
            {(userPermissions.directPermissions || []).map(permission => (
              <DirectPermissionRow
                key={permission.id || directPermissionCode(permission)}
                language={language}
                onRevoke={() => onRevoke(permission)}
                permission={permission}
                t={t}
              />
            ))}
            {!userPermissions.directPermissions?.length && (
              <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-400 dark:bg-slate-800/40">{t("admin.noDirectPermissions","Chưa có quyền trực tiếp")}</div>
            )}
          </div>
        </section>
      </div>
    </Drawer>
  );
}

function GrantPermissionModal({ form, onChange, onClose, onSubmit, open, permissions, t, userId }) {
  return (
    <Modal
      description={userId ? t("admin.grantForUser","User ID: {{userId}}", { userId }) : undefined}
      onClose={onClose}
      open={open}
      size="lg"
      title={t("admin.grantDirect","Cấp quyền trực tiếp")}
    >
      <form className="grid gap-4" onSubmit={onSubmit}>
        <PSelect label={t("admin.permission","Quyền hạn")} required value={form.permissionCode} onChange={e => onChange({ ...form, permissionCode: e.target.value })}>
          <option value="">{t("admin.permission","Chọn quyền hạn")}</option>
          {permissions.map(p => <option key={permissionCode(p)} value={permissionCode(p)}>{permissionCode(p)}</option>)}
        </PSelect>
        <PInput label={t("admin.reason","Lý do")} maxLength={500} value={form.reason} onChange={e => onChange({ ...form, reason: e.target.value })} placeholder={t("admin.reason","Lý do cấp quyền...")}/>
        <PInput label={t("admin.expiresAt","Ngày hết hạn")} type="datetime-local" value={form.expiresAt} onChange={e => onChange({ ...form, expiresAt: e.target.value })}/>
        <div className="flex flex-wrap gap-2">
          <PrimaryBtn type="submit">{t("admin.grantPermission","Cấp quyền")}</PrimaryBtn>
          <SecBtn type="button" onClick={onClose}>{t("common.cancel","Hủy")}</SecBtn>
        </div>
      </form>
    </Modal>
  );
}

function PermissionChips({ empty, permissions, variant = "indigo" }) {
  if (!permissions.length) return <span className="text-xs text-slate-400">{empty}</span>;
  const styles = variant === "sky"
    ? "border-sky-200/60 bg-sky-50 text-sky-700 dark:border-sky-500/20 dark:bg-sky-500/10 dark:text-sky-400"
    : "border-indigo-200/60 bg-indigo-50 text-indigo-700 dark:border-indigo-500/20 dark:bg-indigo-500/10 dark:text-indigo-400";
  return (
    <div className="flex flex-wrap gap-1.5">
      {permissions.map(p => (
        <span key={permissionCode(p)} className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${styles}`}>
          {permissionCode(p)}
        </span>
      ))}
    </div>
  );
}

function DirectPermissionRow({ language, onRevoke, permission, t }) {
  const code = directPermissionCode(permission);
  const isActive = permission.currentlyActive !== false && permission.active !== false;
  return (
    <div className="grid gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-4 sm:grid-cols-[1fr_auto] sm:items-center">
      <div>
        <div className="flex items-center gap-2">
          <Key size={12} className="text-indigo-500 flex-shrink-0"/>
          <span className="text-sm font-bold text-slate-900 dark:text-slate-100">{code}</span>
          <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
            isActive ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400"
                     : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
          }`}>
            {isActive ? t("common.active","Đang hiệu lực") : t("admin.inactive","Không hiệu lực")}
          </span>
        </div>
        {permission.reason && <p className="text-xs text-slate-400 mt-1">{permission.reason}</p>}
        <p className="text-xs text-slate-400 mt-0.5">
          {t("admin.grantedBy","Cấp bởi")}: {permission.grantedByUserId || "—"} ·{" "}
          {t("admin.grantedAt","Lúc")}: {formatDateTime(permission.grantedAt, language)} ·{" "}
          {t("admin.expiresAt","Hết hạn")}: {formatDateTime(permission.expiresAt, language) || "—"}
        </p>
      </div>
      <SecBtn danger icon={Trash2} onClick={onRevoke}>{t("admin.revokePermission","Thu hồi")}</SecBtn>
    </div>
  );
}

/* ── Pure utils (100% preserved) ──────────────── */
function permissionCode(permission) {
  return String(permission?.code || permission?.permissionCode || permission || "");
}
function directPermissionCode(permission) {
  return String(permission?.permission?.code || permission?.permissionCode || permission?.code || "");
}
function roleCode(role) {
  return String(role?.code || role?.roleCode || role || "");
}
