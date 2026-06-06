import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import {
  getAdminUser,
  getAdminUsers,
  lockUser,
  unlockUser,
  updateUserRoles,
} from "../../api/adminUsersApi.js";
import { useConfirm } from "../../components/ui/index.jsx";
import { formatDateTime } from "../../utils/formatters.js";
import { pageRows } from "../../utils/mappers.js";
import { getCurrentUser } from "../../utils/storage.js";

const ROLES = ["USER", "ADMIN"];
const PAGE_SIZES = [10, 20, 50];

const emptyFilters = {
  keyword: "",
  role: "",
  active: "",
  locked: "",
  emailVerified: "",
  page: 1,
  size: 20,
};

export default function AdminUsersPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const [filters, setFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [users, setUsers] = useState([]);
  const [pageMeta, setPageMeta] = useState(createEmptyMeta(emptyFilters));
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [roleDraft, setRoleDraft] = useState([]);
  const [busy, setBusy] = useState("");
  const currentUser = getCurrentUser();

  useEffect(() => {
    refreshUsers(appliedFilters);
  }, [appliedFilters]);

  async function refreshUsers(nextFilters = appliedFilters) {
    setLoading(true);
    setMessage("");
    try {
      const page = await getAdminUsers(toQuery(nextFilters));
      const rows = pageRows(page);
      setUsers(rows);
      setPageMeta({
        currentPage: Number(page?.currentPage || nextFilters.page),
        totalPages: Number(page?.totalPages || 0),
        pageSize: Number(page?.pageSize || nextFilters.size),
        totalElements: Number(page?.totalElements || rows.length),
        hasNext: Boolean(page?.hasNext),
        hasPrevious: Boolean(page?.hasPrevious),
      });
    } catch (error) {
      setUsers([]);
      setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.userLoadFailed"));
    } finally {
      setLoading(false);
    }
  }

  function applyFilters(event) {
    event.preventDefault();
    const next = { ...filters, page: 1, size: Number(filters.size || 20) };
    setFilters(next);
    setAppliedFilters(next);
  }

  function clearFilters() {
    setFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
  }

  function changePage(page) {
    const nextPage = Math.max(1, page);
    setFilters((current) => ({ ...current, page: nextPage }));
    setAppliedFilters((current) => ({ ...current, page: nextPage }));
  }

  function changePageSize(size) {
    const next = { ...filters, page: 1, size: Number(size || 20) };
    setFilters(next);
    setAppliedFilters(next);
  }

  async function openDetail(user) {
    setDetailLoading(true);
    setMessage("");
    try {
      const detail = await getAdminUser(user.id);
      setSelected(detail);
      setRoleDraft(roleCodes(detail));
    } catch (error) {
      setMessage(error.message || t("admin.userDetailFailed"));
    } finally {
      setDetailLoading(false);
    }
  }

  async function runLockAction(user, action) {
    const isLock = action === "lock";
    const confirmed = await confirm({
      title: isLock ? t("admin.lockUser") : t("admin.unlockUser"),
      message: isLock
        ? t("admin.confirmLockUser", { user: user.username || user.email || user.id })
        : t("admin.confirmUnlockUser", { user: user.username || user.email || user.id }),
      confirmLabel: isLock ? t("admin.lockUser") : t("admin.unlockUser"),
      cancelLabel: t("common.cancel"),
      danger: isLock,
    });
    if (!confirmed) return;

    setBusy(`${action}-${user.id}`);
    setMessage("");
    try {
      const updated = isLock ? await lockUser(user.id) : await unlockUser(user.id);
      applyUpdatedUser(updated);
      setMessage(isLock ? t("admin.userLocked") : t("admin.userUnlocked"));
      await refreshUsers(appliedFilters);
    } catch (error) {
      setMessage(error.message || (isLock ? t("admin.userLockFailed") : t("admin.userUnlockFailed")));
    } finally {
      setBusy("");
    }
  }

  async function saveRoles(event) {
    event.preventDefault();
    if (!selected) return;
    if (roleDraft.length === 0) {
      setMessage(t("admin.roleRequired"));
      return;
    }

    setBusy(`roles-${selected.id}`);
    setMessage("");
    try {
      const updated = await updateUserRoles(selected.id, roleDraft);
      applyUpdatedUser(updated);
      setRoleDraft(roleCodes(updated));
      setMessage(t("admin.userRolesUpdated"));
      await refreshUsers(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.userRolesFailed"));
    } finally {
      setBusy("");
    }
  }

  function applyUpdatedUser(updated) {
    setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)));
    setSelected((current) => (current?.id === updated.id ? updated : current));
  }

  function toggleRole(role) {
    setRoleDraft((current) => (
      current.includes(role) ? current.filter((item) => item !== role) : [...current, role]
    ));
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.usersTitle")} eyebrow={t("admin.usersEyebrow")} />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.userFilters")}>
        <form className="grid gap-3 xl:grid-cols-[1fr_150px_130px_130px_150px_100px_auto_auto]" onSubmit={applyFilters}>
          <Input value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} placeholder={t("admin.userKeyword")} />
          <Select value={filters.role} onChange={(event) => setFilters({ ...filters, role: event.target.value })}>
            <option value="">{t("admin.allRoles")}</option>
            {ROLES.map((role) => <option key={role} value={role}>{role}</option>)}
          </Select>
          <BooleanSelect label={t("common.active")} value={filters.active} onChange={(value) => setFilters({ ...filters, active: value })} t={t} />
          <BooleanSelect label={t("admin.locked")} value={filters.locked} onChange={(value) => setFilters({ ...filters, locked: value })} t={t} />
          <BooleanSelect label={t("admin.emailVerified")} value={filters.emailVerified} onChange={(value) => setFilters({ ...filters, emailVerified: value })} t={t} />
          <Select value={filters.size} onChange={(event) => changePageSize(event.target.value)} aria-label={t("catalog.pageSize")}>
            {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
          </Select>
          <Button type="submit">{t("admin.applyFilters")}</Button>
          <Button secondary type="button" onClick={clearFilters}>{t("admin.clearFilters")}</Button>
        </form>
      </Panel>

      <Panel title={t("admin.usersList")}>
        <div className="overflow-x-auto rounded-xl border border-slate-200">
          <table className="min-w-[1080px] w-full border-collapse text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">{t("admin.user")}</th>
                <th className="px-4 py-3">{t("auth.email")}</th>
                <th className="px-4 py-3">{t("admin.roles")}</th>
                <th className="px-4 py-3">{t("admin.provider")}</th>
                <th className="px-4 py-3">{t("admin.flags")}</th>
                <th className="px-4 py-3">{t("admin.failedAttempts")}</th>
                <th className="px-4 py-3">{t("admin.lockoutUntil")}</th>
                <th className="px-4 py-3">{t("orders.createdAt")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr className="border-t border-slate-100 align-middle" key={user.id}>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <Avatar user={user} />
                      <div>
                        <p className="font-bold text-slate-950">{user.username || user.id}</p>
                        <p className="text-xs text-slate-500">{fullName(user) || "-"}</p>
                        <p className="text-xs text-slate-400">{user.id}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{user.email || "-"}</td>
                  <td className="px-4 py-3"><RoleBadges roles={roleCodes(user)} /></td>
                  <td className="px-4 py-3 text-slate-600">{user.provider || "-"}</td>
                  <td className="px-4 py-3"><UserFlags user={user} t={t} /></td>
                  <td className="px-4 py-3">{user.failedLoginAttempts ?? 0}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(user.lockoutUntil, i18n.language)}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(user.createdAt, i18n.language)}</td>
                  <td className="px-4 py-3">
                    <SmallButton onClick={() => openDetail(user)}>{t("common.detail")}</SmallButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
          {!loading && !users.length && <div className="p-5 text-sm text-slate-500">{t("admin.noUsers")}</div>}
        </div>
        <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t} />
      </Panel>

      {detailLoading && <BlockingLoader text={t("common.loading")} />}
      {selected && (
        <UserDetailDrawer
          busy={busy}
          currentUser={currentUser}
          language={i18n.language}
          onClose={() => setSelected(null)}
          onLock={() => runLockAction(selected, "lock")}
          onRolesSubmit={saveRoles}
          onToggleRole={toggleRole}
          onUnlock={() => runLockAction(selected, "unlock")}
          roleDraft={roleDraft}
          selected={selected}
          t={t}
        />
      )}
    </div>
  );
}

function UserDetailDrawer({ busy, currentUser, language, onClose, onLock, onRolesSubmit, onToggleRole, onUnlock, roleDraft, selected, t }) {
  const self = String(currentUser?.id || "") === String(selected.id || "");
  const disabled = Boolean(selected.isDeleted);
  const canMutate = !self && !disabled;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm">
      <aside className="h-full w-full max-w-5xl overflow-y-auto bg-white p-5 shadow-2xl md:p-8">
        <div className="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 md:flex-row md:items-start md:justify-between">
          <div className="flex items-center gap-4">
            <Avatar large user={selected} />
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600">{t("admin.userDetail")}</span>
              <h2 className="mt-2 text-3xl font-bold text-slate-950">{selected.username || selected.email || selected.id}</h2>
              <p className="text-sm text-slate-500">{selected.email || "-"}</p>
              <div className="mt-3"><RoleBadges roles={roleCodes(selected)} /></div>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {selected.isLocked ? (
              <Button disabled={!canMutate || Boolean(busy)} type="button" onClick={onUnlock}>{t("admin.unlockUser")}</Button>
            ) : (
              <Button disabled={!canMutate || Boolean(busy)} type="button" onClick={onLock}>{t("admin.lockUser")}</Button>
            )}
            <Button secondary type="button" onClick={onClose}>{t("common.close")}</Button>
          </div>
        </div>

        {self && <Notice>{t("admin.selfActionDisabled")}</Notice>}
        {disabled && <Notice>{t("admin.deletedUserDisabled")}</Notice>}

        <div className="mt-5 grid gap-5 xl:grid-cols-3">
          <InfoCard title={t("admin.identity")}>
            <Meta label="ID" value={selected.id || "-"} />
            <Meta label={t("auth.username")} value={selected.username || "-"} />
            <Meta label={t("auth.email")} value={selected.email || "-"} />
            <Meta label={t("account.firstName")} value={selected.firstName || "-"} />
            <Meta label={t("account.lastName")} value={selected.lastName || "-"} />
            <Meta label={t("account.phoneNumber")} value={selected.phoneNumber || "-"} />
          </InfoCard>
          <InfoCard title={t("admin.accountFlags")}>
            <Meta label={t("admin.provider")} value={selected.provider || "-"} />
            <Meta label={t("admin.gender")} value={selected.gender || "-"} />
            <Meta label={t("common.active")} value={yesNo(selected.isActive, t)} />
            <Meta label={t("admin.emailVerified")} value={yesNo(selected.emailVerified, t)} />
            <Meta label={t("admin.locked")} value={yesNo(selected.isLocked, t)} />
            <Meta label={t("admin.deleted")} value={yesNo(selected.isDeleted, t)} />
          </InfoCard>
          <InfoCard title={t("admin.securityState")}>
            <Meta label={t("admin.failedAttempts")} value={selected.failedLoginAttempts ?? 0} />
            <Meta label={t("admin.lockoutUntil")} value={formatDateTime(selected.lockoutUntil, language)} />
            <Meta label={t("admin.tokenVersion")} value={selected.tokenVersion ?? "-"} />
            <Meta label={t("orders.createdAt")} value={formatDateTime(selected.createdAt, language)} />
            <Meta label={t("orders.updatedAt")} value={formatDateTime(selected.updatedAt, language)} />
          </InfoCard>
        </div>

        <div className="mt-5 grid gap-5 xl:grid-cols-[1fr_1fr]">
          <InfoCard title={t("admin.roleEditor")}>
            <form className="grid gap-4" onSubmit={onRolesSubmit}>
              <div className="flex flex-wrap gap-3">
                {ROLES.map((role) => (
                  <label key={role} className="flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-bold text-slate-700">
                    <input checked={roleDraft.includes(role)} disabled={!canMutate} type="checkbox" onChange={() => onToggleRole(role)} />
                    {role}
                  </label>
                ))}
              </div>
              <p className="text-sm text-slate-500">{t("admin.roleReplaceNote")}</p>
              <Button disabled={!canMutate || Boolean(busy)} type="submit">{t("admin.updateRoles")}</Button>
            </form>
          </InfoCard>
          <InfoCard title={t("admin.userPermissionShortcut")}>
            <p className="text-sm text-slate-500">{t("admin.userPermissionShortcutCopy")}</p>
            <Link className="inline-flex w-fit rounded-full bg-slate-950 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600" to={`/admin/permissions?userId=${encodeURIComponent(selected.id)}`}>
              {t("admin.openPermissions")}
            </Link>
          </InfoCard>
        </div>
      </aside>
    </div>
  );
}

function toQuery(filters) {
  return {
    keyword: filters.keyword || undefined,
    role: filters.role || undefined,
    active: parseBoolean(filters.active),
    locked: parseBoolean(filters.locked),
    emailVerified: parseBoolean(filters.emailVerified),
    page: Number(filters.page || 1),
    size: Number(filters.size || 20),
  };
}

function parseBoolean(value) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

function roleCodes(user) {
  return (user?.roles || [])
    .map((role) => role?.code || role?.roleCode || role?.name || role)
    .filter((role) => ROLES.includes(String(role).toUpperCase()))
    .map((role) => String(role).toUpperCase());
}

function fullName(user) {
  return [user?.firstName, user?.lastName].filter(Boolean).join(" ");
}

function yesNo(value, t) {
  return value ? t("common.yes") : t("common.no");
}

function createEmptyMeta(filters) {
  return {
    currentPage: Number(filters.page || 1),
    totalPages: 0,
    pageSize: Number(filters.size || 20),
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  };
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span>
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <h3 className="mb-5 text-xl font-bold text-slate-950">{title}</h3>
      {children}
    </section>
  );
}

function InfoCard({ children, title }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h3 className="mb-4 text-lg font-bold text-slate-950">{title}</h3>
      <div className="grid gap-2">{children}</div>
    </section>
  );
}

function Meta({ label, value }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="max-w-[70%] text-right font-semibold text-slate-700">{value}</span>
    </div>
  );
}

function RoleBadges({ roles }) {
  return (
    <div className="flex flex-wrap gap-1">
      {(roles || []).map((role) => (
        <span className="rounded-full bg-blue-50 px-2 py-1 text-xs font-bold text-blue-700" key={role}>{role}</span>
      ))}
      {!roles?.length && <span className="text-xs font-semibold text-slate-400">-</span>}
    </div>
  );
}

function UserFlags({ t, user }) {
  return (
    <div className="flex flex-wrap gap-1">
      <Flag active={user.isActive !== false}>{user.isActive === false ? t("admin.inactive") : t("common.active")}</Flag>
      <Flag active={!user.isLocked}>{user.isLocked ? t("admin.locked") : t("admin.unlocked")}</Flag>
      <Flag active={user.emailVerified}>{user.emailVerified ? t("admin.verified") : t("admin.unverified")}</Flag>
      {user.isDeleted && <Flag active={false}>{t("admin.deleted")}</Flag>}
    </div>
  );
}

function Flag({ active, children }) {
  return <span className={["rounded-full px-2 py-1 text-xs font-bold", active ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"].join(" ")}>{children}</span>;
}

function Avatar({ large = false, user }) {
  const size = large ? "h-16 w-16" : "h-11 w-11";
  if (user.avatarUrl) return <img className={`${size} rounded-full object-cover ring-1 ring-slate-200`} src={user.avatarUrl} alt={user.username || user.email || user.id} />;
  return <span className={`${size} grid place-items-center rounded-full bg-slate-900 text-sm font-bold text-white`}>{String(user.username || user.email || user.id || "U").charAt(0).toUpperCase()}</span>;
}

function BooleanSelect({ label, onChange, t, value }) {
  return (
    <Select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">{label}</option>
      <option value="true">{t("common.yes")}</option>
      <option value="false">{t("common.no")}</option>
    </Select>
  );
}

function Pagination({ loading, meta, onPage, t }) {
  if (!meta.totalPages || meta.totalPages <= 1) return null;
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
      <span className="font-semibold text-slate-500">
        {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })} - {meta.totalElements}
      </span>
      <div className="flex flex-wrap gap-2">
        <SmallButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(1)}>{t("catalog.firstPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(meta.currentPage - 1)}>{t("catalog.previousPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.currentPage + 1)}>{t("catalog.nextPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.totalPages)}>{t("catalog.lastPage")}</SmallButton>
      </div>
    </div>
  );
}

function BlockingLoader({ text }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-4 backdrop-blur-sm">
      <div className="rounded-2xl bg-white p-8 text-sm font-bold text-slate-700 shadow-2xl">{text}</div>
    </div>
  );
}

function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-50" />;
}

function Select(props) {
  return <select {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}

function SmallButton({ danger = false, ...props }) {
  return <button type="button" {...props} className={["rounded-full border px-3 py-1.5 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50", danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} />;
}

function Notice({ children }) {
  return <div className="rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
