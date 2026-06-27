import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import {
  Users, Search, Shield, UserCircle, Mail, Lock, Unlock,
  X, RefreshCw, Filter, ExternalLink,
} from "lucide-react";

import {
  getAdminUser, getAdminUsers, lockUser, unlockUser, updateUserRoles,
} from "../../api/adminUsersApi.js";
import { InfoCard, MetaRow as Meta, Modal, Pagination, useConfirm } from "../../components/ui/index.jsx";
import { formatDateTime } from "../../utils/formatters.js";
import { pageMeta as readPageMeta, pageRows } from "../../utils/mappers.js";
import { getCurrentUser } from "../../utils/storage.js";

/* ── Constants ─────────────────────────────────── */
const ROLES = ["USER", "ADMIN"];
const PAGE_SIZES = [10, 20, 50];
const emptyFilters = { keyword:"", role:"", active:"", locked:"", emailVerified:"", page:1, size:20 };

/* ── Shared UI Primitives ──────────────────────── */
function PInput({ ...props }) {
  return <input className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-shadow" {...props}/>;
}
function PSelect({ children, ...props }) {
  return (
    <select className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow" {...props}>
      {children}
    </select>
  );
}
function PrimaryBtn({ children, loading, disabled, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }} disabled={loading||disabled}
      className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-200 dark:shadow-none transition-colors focus:ring-2 focus:ring-offset-2 focus:ring-indigo-600 disabled:opacity-50" {...props}>
      {loading && <RefreshCw size={13} className="animate-spin"/>}
      {children}
    </motion.button>
  );
}
function SecBtn({ children, danger, disabled, icon: Icon, ...props }) {
  return (
    <motion.button whileHover={{ scale:1.02 }} whileTap={{ scale:0.97 }} disabled={disabled}
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

/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminUsersPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();

  // ── State (100% preserved) ──────────────────
  const [filters, setFilters]             = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [users, setUsers]                 = useState([]);
  const [pageMeta, setPageMeta]           = useState(createEmptyMeta(emptyFilters));
  const [loading, setLoading]             = useState(false);
  const [message, setMessage]             = useState("");
  const [selected, setSelected]           = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [roleDraft, setRoleDraft]         = useState([]);
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [busy, setBusy]                   = useState("");
  const currentUser = getCurrentUser();

  const refreshUsers = useCallback(async (nextFilters = appliedFilters) => {
    setLoading(true); setMessage("");
    try {
      const page = await getAdminUsers(toQuery(nextFilters));
      setUsers(pageRows(page));
      setPageMeta(readPageMeta(page, { page: nextFilters.page, size: nextFilters.size }));
    } catch (error) {
      setUsers([]); setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.userLoadFailed"));
    } finally { setLoading(false); }
  }, [appliedFilters, t]);

  useEffect(() => { refreshUsers(appliedFilters); }, [appliedFilters, refreshUsers]);

  function applyFilters(event) {
    event.preventDefault();
    const next = { ...filters, page:1, size: Number(filters.size || 20) };
    setFilters(next); setAppliedFilters(next);
  }
  function clearFilters() { setFilters(emptyFilters); setAppliedFilters(emptyFilters); }
  function changePage(page) {
    const p = Math.max(1, page);
    setFilters(c => ({ ...c, page: p })); setAppliedFilters(c => ({ ...c, page: p }));
  }
  function changePageSize(size) {
    const next = { ...filters, page:1, size: Number(size || 20) };
    setFilters(next); setAppliedFilters(next);
  }

  async function openDetail(user) {
    setDetailLoading(true); setMessage("");
    try {
      const detail = await getAdminUser(user.id);
      setSelected(detail); setRoleDraft(roleCodes(detail));
    } catch (error) { setMessage(error.message || t("admin.userDetailFailed")); }
    finally { setDetailLoading(false); }
  }

  async function runLockAction(user, action) {
    const isLock = action === "lock";
    const confirmed = await confirm({
      title: isLock ? t("admin.lockUser") : t("admin.unlockUser"),
      message: isLock ? t("admin.confirmLockUser", { user: user.username || user.email || user.id })
                      : t("admin.confirmUnlockUser", { user: user.username || user.email || user.id }),
      confirmLabel: isLock ? t("admin.lockUser") : t("admin.unlockUser"),
      cancelLabel: t("common.cancel"), danger: isLock,
    });
    if (!confirmed) return;
    setBusy(`${action}-${user.id}`); setMessage("");
    try {
      const updated = isLock ? await lockUser(user.id) : await unlockUser(user.id);
      applyUpdatedUser(updated);
      setMessage(isLock ? t("admin.userLocked") : t("admin.userUnlocked"));
      await refreshUsers(appliedFilters);
    } catch (error) { setMessage(error.message || (isLock ? t("admin.userLockFailed") : t("admin.userUnlockFailed"))); }
    finally { setBusy(""); }
  }

  async function saveRoles(event) {
    event.preventDefault(); if (!selected) return;
    if (roleDraft.length === 0) { setMessage(t("admin.roleRequired")); return; }
    setBusy(`roles-${selected.id}`); setMessage("");
    try {
      const updated = await updateUserRoles(selected.id, roleDraft);
      applyUpdatedUser(updated); setRoleDraft(roleCodes(updated));
      setRoleModalOpen(false);
      setMessage(t("admin.userRolesUpdated"));
      await refreshUsers(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.userRolesFailed")); }
    finally { setBusy(""); }
  }

  function applyUpdatedUser(updated) {
    setUsers(current => current.map(user => user.id === updated.id ? updated : user));
    setSelected(current => current?.id === updated.id ? updated : current);
  }
  function toggleRole(role) {
    setRoleDraft(current => current.includes(role) ? current.filter(r => r !== role) : [...current, role]);
  }

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="max-w-full space-y-6">

      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.usersTitle","Quản lý người dùng")}</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.usersEyebrow","Quản lý tài khoản và phân quyền")}</p>
        </div>
      </div>

      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {/* Filter bar */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <Filter size={16} className="text-indigo-500"/>
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.userFilters","Bộ lọc")}</h3>
        </div>
        <div className="p-5">
          <form className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[1fr_130px_110px_110px_140px_90px_auto_auto]" onSubmit={applyFilters}>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400"/>
              <PInput className="pl-9" value={filters.keyword} onChange={e => setFilters({ ...filters, keyword: e.target.value })} placeholder={t("admin.userKeyword","Tìm người dùng...")}/>
            </div>
            <PSelect value={filters.role} onChange={e => setFilters({ ...filters, role: e.target.value })}>
              <option value="">{t("admin.allRoles","Tất cả vai trò")}</option>
              {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
            </PSelect>
            <BooleanSelectNew label={t("common.active","Hoạt động")} value={filters.active} onChange={v => setFilters({ ...filters, active: v })} t={t}/>
            <BooleanSelectNew label={t("admin.locked","Bị khóa")} value={filters.locked} onChange={v => setFilters({ ...filters, locked: v })} t={t}/>
            <BooleanSelectNew label={t("admin.emailVerified","Xác thực email")} value={filters.emailVerified} onChange={v => setFilters({ ...filters, emailVerified: v })} t={t}/>
            <PSelect value={filters.size} onChange={e => changePageSize(e.target.value)}>
              {PAGE_SIZES.map(s => <option key={s} value={s}>{s}</option>)}
            </PSelect>
            <PrimaryBtn type="submit">{t("admin.applyFilters","Lọc")}</PrimaryBtn>
            <SecBtn type="button" onClick={clearFilters}>{t("admin.clearFilters","Xóa lọc")}</SecBtn>
          </form>
        </div>
      </div>

      {/* Users table */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <div className="flex items-center gap-2">
            <Users size={16} className="text-indigo-500"/>
            <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.usersList","Danh sách người dùng")}</h3>
          </div>
          <span className="text-xs font-semibold text-slate-400">{pageMeta.totalElements ?? 0} {t("admin.users","người dùng")}</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap min-w-[1100px]">
            <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-medium text-xs uppercase tracking-wider">
              <tr>
                <th className="px-5 py-4">{t("admin.user","Người dùng")}</th>
                <th className="px-5 py-4">{t("auth.email","Email")}</th>
                <th className="px-5 py-4">{t("admin.roles","Vai trò")}</th>
                <th className="px-5 py-4">{t("admin.provider","Provider")}</th>
                <th className="px-5 py-4">{t("admin.flags","Trạng thái")}</th>
                <th className="px-5 py-4">{t("admin.failedAttempts","Thất bại")}</th>
                <th className="px-5 py-4">{t("admin.lockoutUntil","Khóa đến")}</th>
                <th className="px-5 py-4">{t("orders.createdAt","Tạo lúc")}</th>
                <th className="px-5 py-4 text-right">{t("admin.actions","Hành động")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {loading && !users.length
                ? [...Array(5)].map((_,i) => (
                    <tr key={i} className="animate-pulse">
                      {[...Array(9)].map((_,j) => <td key={j} className="px-5 py-4"><div className="h-4 bg-slate-100 dark:bg-slate-800 rounded w-3/4"/></td>)}
                    </tr>
                  ))
                : users.map((user, index) => (
                    <motion.tr key={user.id}
                      initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }}
                      transition={{ delay: index * 0.04, duration: 0.3 }}
                      className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <Avatar user={user}/>
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-900 dark:text-slate-100">{user.username || user.id}</p>
                            <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">{fullName(user) || "—"}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                          <Mail size={12} className="text-slate-400 flex-shrink-0"/>
                          <span className="text-xs truncate max-w-[160px]">{user.email || "—"}</span>
                        </div>
                      </td>
                      <td className="px-5 py-4"><RoleBadges roles={roleCodes(user)}/></td>
                      <td className="px-5 py-4 text-xs text-slate-500 dark:text-slate-400">{user.provider || "—"}</td>
                      <td className="px-5 py-4"><UserFlags user={user} t={t}/></td>
                      <td className="px-5 py-4 text-slate-600 dark:text-slate-300">{user.failedLoginAttempts ?? 0}</td>
                      <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(user.lockoutUntil, i18n.language)}</td>
                      <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(user.createdAt, i18n.language)}</td>
                      <td className="px-5 py-4 text-right">
                        <SecBtn onClick={() => openDetail(user)}>{t("common.detail","Chi tiết")}</SecBtn>
                      </td>
                    </motion.tr>
                  ))
              }
            </tbody>
          </table>
          {!loading && !users.length && (
            <div className="flex flex-col items-center justify-center py-16 text-slate-400">
              <Users size={32} className="mb-3 opacity-40"/>
              <p className="text-sm font-semibold">{t("admin.noUsers","Không có người dùng nào")}</p>
            </div>
          )}
        </div>
        <div className="border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-3">
          <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t}/>
        </div>
      </div>

      {/* Detail loading */}
      {detailLoading && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 backdrop-blur-sm">
          <div className="rounded-2xl bg-white dark:bg-slate-900 p-8 text-sm font-bold text-slate-700 dark:text-slate-300 shadow-2xl flex items-center gap-3">
            <RefreshCw size={16} className="animate-spin text-indigo-500"/>
            {t("common.loading","Đang tải...")}
          </div>
        </div>
      )}

      {/* User detail drawer */}
      {selected && (
        <UserDetailDrawer busy={busy} currentUser={currentUser} language={i18n.language}
          onClose={() => setSelected(null)}
          onLock={() => runLockAction(selected, "lock")}
          onOpenRoles={() => setRoleModalOpen(true)}
          onUnlock={() => runLockAction(selected, "unlock")}
          selected={selected} t={t}/>
      )}
      {selected && (
        <UserRolesModal
          busy={busy}
          currentUser={currentUser}
          onClose={() => setRoleModalOpen(false)}
          onRolesSubmit={saveRoles}
          onToggleRole={toggleRole}
          open={roleModalOpen}
          roleDraft={roleDraft}
          selected={selected}
          t={t}
        />
      )}
    </motion.div>
  );
}

/* ── Sub-components (all logic preserved) ──────── */
function UserDetailDrawer({ busy, currentUser, language, onClose, onLock, onOpenRoles, onUnlock, selected, t }) {
  const self = String(currentUser?.id || "") === String(selected.id || "");
  const disabled = Boolean(selected.isDeleted);
  const canMutate = !self && !disabled;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm">
      <motion.aside initial={{ x:"100%" }} animate={{ x:0 }} exit={{ x:"100%" }} transition={{ type:"spring", stiffness:300, damping:30 }}
        className="h-full w-full max-w-5xl overflow-y-auto bg-white dark:bg-slate-900 shadow-2xl">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-6 py-4">
          <div className="flex items-center gap-3">
            <Avatar large user={selected}/>
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">{t("admin.userDetail","Chi tiết người dùng")}</p>
              <h2 className="text-xl font-bold text-slate-900 dark:text-slate-50">{selected.username || selected.email || selected.id}</h2>
              <RoleBadges roles={roleCodes(selected)}/>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {selected.isLocked
              ? <PrimaryBtn disabled={!canMutate || Boolean(busy)} onClick={onUnlock}><Unlock size={13}/> {t("admin.unlockUser","Mở khóa")}</PrimaryBtn>
              : <PrimaryBtn disabled={!canMutate || Boolean(busy)} onClick={onLock}><Lock size={13}/> {t("admin.lockUser","Khóa tài khoản")}</PrimaryBtn>
            }
            <button onClick={onClose} className="rounded-lg border border-slate-200 dark:border-slate-700 p-2 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              <X size={18}/>
            </button>
          </div>
        </div>
        <div className="p-6 space-y-5">
          {self && <div className="rounded-xl border border-amber-200/60 bg-amber-50 dark:border-amber-500/20 dark:bg-amber-500/10 px-4 py-3 text-sm font-semibold text-amber-700 dark:text-amber-400">{t("admin.selfActionDisabled","Bạn không thể tự quản lý tài khoản của mình")}</div>}
          {disabled && <div className="rounded-xl border border-rose-200/60 bg-rose-50 dark:border-rose-500/20 dark:bg-rose-500/10 px-4 py-3 text-sm font-semibold text-rose-700 dark:text-rose-400">{t("admin.deletedUserDisabled","Tài khoản này đã bị xóa")}</div>}

          <div className="grid gap-5 xl:grid-cols-3">
            <InfoCard title={t("admin.identity","Danh tính")}>
              <Meta label="ID" value={selected.id || "—"}/>
              <Meta label={t("auth.username","Tên đăng nhập")} value={selected.username || "—"}/>
              <Meta label={t("auth.email","Email")} value={selected.email || "—"}/>
              <Meta label={t("account.firstName","Họ")} value={selected.firstName || "—"}/>
              <Meta label={t("account.lastName","Tên")} value={selected.lastName || "—"}/>
              <Meta label={t("account.phoneNumber","Điện thoại")} value={selected.phoneNumber || "—"}/>
            </InfoCard>
            <InfoCard title={t("admin.accountFlags","Trạng thái tài khoản")}>
              <Meta label={t("admin.provider","Provider")} value={selected.provider || "—"}/>
              <Meta label={t("admin.gender","Giới tính")} value={selected.gender || "—"}/>
              <Meta label={t("common.active","Hoạt động")} value={yesNo(selected.isActive, t)}/>
              <Meta label={t("admin.emailVerified","Xác thực email")} value={yesNo(selected.emailVerified, t)}/>
              <Meta label={t("admin.locked","Bị khóa")} value={yesNo(selected.isLocked, t)}/>
              <Meta label={t("admin.deleted","Đã xóa")} value={yesNo(selected.isDeleted, t)}/>
            </InfoCard>
            <InfoCard title={t("admin.securityState","Bảo mật")}>
              <Meta label={t("admin.failedAttempts","Đăng nhập thất bại")} value={selected.failedLoginAttempts ?? 0}/>
              <Meta label={t("admin.lockoutUntil","Khóa đến")} value={formatDateTime(selected.lockoutUntil, language)}/>
              <Meta label={t("admin.tokenVersion","Token version")} value={selected.tokenVersion ?? "—"}/>
              <Meta label={t("orders.createdAt","Tạo lúc")} value={formatDateTime(selected.createdAt, language)}/>
              <Meta label={t("orders.updatedAt","Cập nhật")} value={formatDateTime(selected.updatedAt, language)}/>
            </InfoCard>
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <InfoCard title={t("admin.roleEditor","Phân quyền")}>
              <div className="grid gap-4">
                <RoleBadges roles={roleCodes(selected)}/>
                <p className="text-sm text-slate-500 dark:text-slate-400">{t("admin.roleReplaceNote","Vai trò mới sẽ thay thế hoàn toàn vai trò hiện tại")}</p>
                <PrimaryBtn disabled={!canMutate || Boolean(busy)} type="button" onClick={onOpenRoles}>{t("admin.updateRoles","Cập nhật vai trò")}</PrimaryBtn>
              </div>
            </InfoCard>
            <InfoCard title={t("admin.userPermissionShortcut","Quản lý quyền riêng lẻ")}>
              <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">{t("admin.userPermissionShortcutCopy","Cấp hoặc thu hồi quyền trực tiếp cho người dùng này")}</p>
              <Link className="inline-flex items-center gap-2 rounded-xl bg-slate-900 dark:bg-indigo-600 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-indigo-600 dark:hover:bg-indigo-700"
                to={`/admin/permissions?userId=${encodeURIComponent(selected.id)}`}>
                <ExternalLink size={14}/>
                {t("admin.openPermissions","Mở trang phân quyền")}
              </Link>
            </InfoCard>
          </div>
        </div>
      </motion.aside>
    </div>
  );
}

/* ── Helper sub-components (all preserved) ─────── */
function UserRolesModal({ busy, currentUser, onClose, onRolesSubmit, onToggleRole, open, roleDraft, selected, t }) {
  const self = String(currentUser?.id || "") === String(selected.id || "");
  const disabled = Boolean(selected.isDeleted);
  const canMutate = !self && !disabled;
  return (
    <Modal
      description={t("admin.roleReplaceNote","Vai trò mới sẽ thay thế hoàn toàn vai trò hiện tại")}
      onClose={onClose}
      open={open}
      title={t("admin.roleEditor","Phân quyền")}
    >
      <form className="grid gap-4" onSubmit={onRolesSubmit}>
        <div className="flex flex-wrap gap-3">
          {ROLES.map(role => (
            <label key={role} className={`flex cursor-pointer items-center gap-2.5 rounded-xl border px-4 py-3 transition-colors ${
              roleDraft.includes(role)
                ? "border-indigo-300 bg-indigo-50 dark:border-indigo-500/40 dark:bg-indigo-500/10"
                : "border-slate-200 hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
            }`}>
              <input checked={roleDraft.includes(role)} disabled={!canMutate} type="checkbox" onChange={() => onToggleRole(role)} className="h-4 w-4 rounded text-indigo-600"/>
              <div className="flex items-center gap-1.5">
                {role === "ADMIN" ? <Shield size={14} className="text-indigo-500"/> : <UserCircle size={14} className="text-slate-400"/>}
                <span className="text-sm font-bold text-slate-700 dark:text-slate-300">{role}</span>
              </div>
            </label>
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          <PrimaryBtn disabled={!canMutate || Boolean(busy)} type="submit">{t("admin.updateRoles","Cập nhật vai trò")}</PrimaryBtn>
          <SecBtn type="button" onClick={onClose}>{t("common.cancel","Hủy")}</SecBtn>
        </div>
      </form>
    </Modal>
  );
}

function RoleBadges({ roles }) {
  return (
    <div className="flex flex-wrap gap-1">
      {(roles || []).map(role => (
        <span key={role} className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${
          role === "ADMIN"
            ? "border-indigo-200/60 bg-indigo-50 text-indigo-700 dark:border-indigo-500/20 dark:bg-indigo-500/10 dark:text-indigo-400"
            : "border-slate-200/60 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400"
        }`}>
          {role === "ADMIN" ? <span className="flex items-center gap-1"><Shield size={9}/> {role}</span> : role}
        </span>
      ))}
      {!roles?.length && <span className="text-xs font-semibold text-slate-400">—</span>}
    </div>
  );
}

function UserFlags({ user, t }) {
  return (
    <div className="flex flex-wrap gap-1">
      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${user.isActive !== false ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400" : "bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-400"}`}>
        {user.isActive === false ? t("admin.inactive","Không HĐ") : t("common.active","Hoạt động")}
      </span>
      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${!user.isLocked ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400" : "bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-400"}`}>
        {user.isLocked ? t("admin.locked","Bị khóa") : t("admin.unlocked","Mở khóa")}
      </span>
      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${user.emailVerified ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400" : "bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400"}`}>
        {user.emailVerified ? t("admin.verified","Đã xác thực") : t("admin.unverified","Chưa xác thực")}
      </span>
      {user.isDeleted && <span className="px-2 py-1 text-xs font-semibold rounded-full bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-400">{t("admin.deleted","Đã xóa")}</span>}
    </div>
  );
}

function Avatar({ large = false, user }) {
  const size = large ? "h-14 w-14 text-base" : "h-10 w-10 text-sm";
  if (user.avatarUrl) return <img className={`${size} rounded-full object-cover ring-2 ring-white dark:ring-slate-900 shadow-sm`} src={user.avatarUrl} alt={user.username}/>;
  const initial = String(user.username || user.email || user.id || "U").charAt(0).toUpperCase();
  return (
    <span className={`${size} inline-flex items-center justify-center rounded-full bg-gradient-to-br from-indigo-500 to-violet-500 font-bold text-white flex-shrink-0`}>
      {initial}
    </span>
  );
}

function BooleanSelectNew({ label, onChange, t, value }) {
  return (
    <select aria-label={label} value={value} onChange={e => onChange(e.target.value)}
      className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow">
      <option value="">{label}</option>
      <option value="true">{t("common.yes","Có")}</option>
      <option value="false">{t("common.no","Không")}</option>
    </select>
  );
}

/* ── Pure utils (100% preserved) ──────────────── */
function toQuery(filters) {
  return {
    keyword: filters.keyword || undefined, role: filters.role || undefined,
    active: parseBoolean(filters.active), locked: parseBoolean(filters.locked),
    emailVerified: parseBoolean(filters.emailVerified),
    page: Number(filters.page || 1), size: Number(filters.size || 20),
  };
}
function parseBoolean(value) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}
function roleCodes(user) {
  return (user?.roles || [])
    .map(role => role?.code || role?.roleCode || role?.name || role)
    .filter(role => ROLES.includes(String(role).toUpperCase()))
    .map(role => String(role).toUpperCase());
}
function fullName(user) { return [user?.firstName, user?.lastName].filter(Boolean).join(" "); }
function yesNo(value, t) { return value ? t("common.yes","Có") : t("common.no","Không"); }
function createEmptyMeta(filters) {
  return readPageMeta([], { page: filters.page || 1, size: filters.size || 20, totalPages: 0 });
}
