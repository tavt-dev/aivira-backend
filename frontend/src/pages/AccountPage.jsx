import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { getSessions, logoutAll, revokeSession } from "../api/authApi.js";
import {
  Badge,
  Button,
  Checkbox,
  EmptyState,
  Input,
  MetaRow,
  Notice,
  PageHeader,
  Panel,
  Select,
  useConfirm,
} from "../components/ui/index.jsx";
import {
  changePassword,
  createAddress,
  deactivateAccount,
  deleteAddress,
  getAddresses,
  getProfile,
  setDefaultAddress,
  updateAddress,
  updateAvatar,
  updateProfile,
} from "../api/userApi.js";
import { formatDateTime } from "../utils/formatters.js";
import { normalizeAddress } from "../utils/mappers.js";
import { clearAuth, getAccessToken, getCurrentUser, saveCurrentUser } from "../utils/storage.js";

const emptyAddress = {
  recipientName: "",
  phoneNumber: "",
  addressLine: "",
  ward: "",
  district: "",
  city: "",
  defaultAddress: false,
};

const emptyPassword = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
};

export default function AccountPage({ onAuth }) {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(getCurrentUser());
  const [profileForm, setProfileForm] = useState({ firstName: "", lastName: "", gender: "" });
  const [passwordForm, setPasswordForm] = useState(emptyPassword);
  const [addresses, setAddresses] = useState([]);
  const [addressForm, setAddressForm] = useState(emptyAddress);
  const [editingAddressId, setEditingAddressId] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState("");
  const [deactivateConfirmation, setDeactivateConfirmation] = useState("");

  const refreshProfile = useCallback(async () => {
    setBusy((current) => current || "profile");
    try {
      const data = await getProfile();
      setProfile(data);
      saveCurrentUser(data);
      setProfileForm({
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        gender: data?.gender || "",
      });
    } catch (error) {
      setMessage(error.message || t("account.profileLoadFailed"));
    } finally {
      setBusy((current) => (current === "profile" ? "" : current));
    }
  }, [t]);

  const refreshAddresses = useCallback(async () => {
    try {
      const rows = await getAddresses();
      setAddresses((rows || []).map(normalizeAddress).filter(Boolean));
    } catch (error) {
      setMessage(error.message || t("account.addressesLoadFailed"));
    }
  }, [t]);

  const refreshSessions = useCallback(async () => {
    try {
      const rows = await getSessions();
      setSessions(rows || []);
    } catch {
      setSessions([]);
    }
  }, []);

  useEffect(() => {
    if (!getAccessToken()) return;
    refreshProfile();
    refreshAddresses();
    refreshSessions();
  }, [refreshAddresses, refreshProfile, refreshSessions]);

  async function saveProfile(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginUpdateProfile"));
      return;
    }
    setBusy("profileSave");
    try {
      const updated = await updateProfile({
        firstName: profileForm.firstName || null,
        lastName: profileForm.lastName || null,
        gender: profileForm.gender || null,
      });
      setProfile(updated);
      saveCurrentUser(updated);
      setMessage(t("account.profileUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.profileUpdateFailed"));
    } finally {
      setBusy("");
    }
  }

  async function uploadAvatar(event) {
    const file = event.target.files?.[0];
    if (!file || !getAccessToken()) return;
    setMessage("");
    setBusy("avatar");
    try {
      const updated = await updateAvatar(file);
      setProfile(updated);
      saveCurrentUser(updated);
      setMessage(t("account.avatarUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.avatarFailed"));
    } finally {
      event.target.value = "";
      setBusy("");
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    setMessage("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMessage(t("account.confirmMismatch"));
      return;
    }
    setBusy("password");
    try {
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm(emptyPassword);
      setMessage(t("account.passwordChanged"));
      refreshSessions();
    } catch (error) {
      setMessage(error.message || t("account.passwordFailed"));
    } finally {
      setBusy("");
    }
  }

  async function saveAddress(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginSaveAddress"));
      return;
    }
    if (!addressForm.recipientName || !addressForm.phoneNumber || !addressForm.addressLine) {
      setMessage(t("account.addressRequired"));
      return;
    }

    setBusy("address");
    try {
      const saved = normalizeAddress(
        editingAddressId ? await updateAddress(editingAddressId, addressForm) : await createAddress(addressForm)
      );
      setAddresses((current) =>
        editingAddressId
          ? current.map((item) => (item.id === editingAddressId ? saved : item))
          : [saved, ...current]
      );
      resetAddressForm();
      setMessage(t("account.addressSaved"));
    } catch (error) {
      setMessage(error.message || t("account.addressFailed"));
    } finally {
      setBusy("");
    }
  }

  function editAddress(address) {
    setEditingAddressId(address.id);
    setAddressForm({
      recipientName: address.recipientName || "",
      phoneNumber: address.phoneNumber || "",
      addressLine: address.addressLine || "",
      ward: address.ward || "",
      district: address.district || "",
      city: address.city || "",
      defaultAddress: Boolean(address.defaultAddress),
    });
  }

  function resetAddressForm() {
    setEditingAddressId(null);
    setAddressForm(emptyAddress);
  }

  async function makeDefault(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginUpdateAddress"));
      return;
    }
    setBusy(`default-${address.id}`);
    try {
      const updated = normalizeAddress(await setDefaultAddress(address.id));
      setAddresses((current) =>
        current.map((item) => ({
          ...item,
          defaultAddress: item.id === updated.id || item.id === address.id,
        }))
      );
      setMessage(t("account.defaultUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.defaultFailed"));
    } finally {
      setBusy("");
    }
  }

  async function removeAddress(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginDeleteAddress"));
      return;
    }
    const confirmed = await confirm({
      title: t("common.delete"),
      message: t("account.confirmDeleteAddress"),
      confirmLabel: t("common.delete"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;

    setBusy(`delete-${address.id}`);
    try {
      await deleteAddress(address.id);
      setAddresses((current) => current.filter((item) => item.id !== address.id));
      if (editingAddressId === address.id) resetAddressForm();
      setMessage(t("account.addressDeleted"));
    } catch (error) {
      setMessage(error.message || t("account.deleteFailed"));
    } finally {
      setBusy("");
    }
  }

  async function revoke(sessionId) {
    setMessage("");
    const target = sessions.find((item) => (item.sessionId || item.id) === sessionId);
    setBusy(`session-${sessionId}`);
    try {
      await revokeSession(sessionId);
      if (target?.current) {
        clearAuthAndPromptLogin();
        return;
      }
      setSessions((current) =>
        current.filter((item) => item.sessionId !== sessionId && item.id !== sessionId)
      );
      setMessage(t("account.sessionRevoked"));
    } catch (error) {
      setMessage(error.message || t("account.revokeFailed"));
    } finally {
      setBusy("");
    }
  }

  async function logoutEverywhere() {
    setMessage("");
    setBusy("logoutAll");
    try {
      await logoutAll();
      clearAuthAndPromptLogin();
      setMessage(t("account.loggedOutAll"));
    } catch (error) {
      setMessage(error.message || t("account.logoutAllFailed"));
    } finally {
      setBusy("");
    }
  }

  async function deactivate() {
    setMessage("");
    if (deactivateConfirmation !== "DEACTIVATE") {
      setMessage(t("account.deactivateMismatch"));
      return;
    }
    setBusy("deactivate");
    try {
      await deactivateAccount();
      clearAuthAndPromptLogin();
      setMessage(t("account.deactivated"));
    } catch (error) {
      setMessage(error.message || t("account.deactivateFailed"));
    } finally {
      setBusy("");
    }
  }

  function clearAuthAndPromptLogin() {
    clearAuth();
    setProfile(null);
    setSessions([]);
    onAuth?.();
    navigate("/?auth=login&next=/account", { replace: true });
  }

  const loggedIn = Boolean(getAccessToken());

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("account.title")} eyebrow={t("account.eyebrow")} />
      {message && <Notice className="mb-6">{message}</Notice>}

      {!loggedIn || !profile ? (
        <EmptyState
          title={t("account.loginRequired")}
          action={
            <Button type="button" onClick={onAuth}>
              {t("common.login")}
            </Button>
          }
        />
      ) : (
        <div className="grid gap-8 lg:grid-cols-2">
          {/* Profile Panel */}
          <Panel title={t("account.profile")}>
            <div className="flex items-center gap-4">
              {profile.avatarUrl ? (
                <img className="h-20 w-20 rounded-2xl object-cover" src={profile.avatarUrl} alt={profile.username || "Avatar"} />
              ) : (
                <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-slate-950 font-serif text-3xl font-bold text-white">
                  A
                </div>
              )}
              <div className="min-w-0">
                <p className="truncate font-bold text-slate-950">{profile.username || profile.email}</p>
                <p className="truncate text-sm text-slate-500">{profile.email || t("account.aiviraAccount")}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Badge>{profile.provider || "LOCAL"}</Badge>
                  <Badge variant={profile.emailVerified ? "success" : "warning"}>
                    {profile.emailVerified ? t("account.emailVerified") : t("account.emailUnverified")}
                  </Badge>
                </div>
              </div>
            </div>

            <div className="mt-6 grid gap-2 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
              <MetaRow label={t("account.userId")} value={profile.id} />
              <MetaRow label={t("account.phone")} value={profile.phoneNumber || "-"} />
              <MetaRow label={t("account.createdAt")} value={formatDateTime(profile.createdAt, i18n.language)} />
            </div>

            <form className="mt-6 grid gap-4" onSubmit={saveProfile}>
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  value={profileForm.firstName}
                  onChange={(event) => setProfileForm({ ...profileForm, firstName: event.target.value })}
                  placeholder={t("account.firstName")}
                />
                <Input
                  value={profileForm.lastName}
                  onChange={(event) => setProfileForm({ ...profileForm, lastName: event.target.value })}
                  placeholder={t("account.lastName")}
                />
              </div>
              <Select
                value={profileForm.gender}
                onChange={(event) => setProfileForm({ ...profileForm, gender: event.target.value })}
              >
                <option value="">{t("account.gender")}</option>
                <option value="MALE">{t("account.male")}</option>
                <option value="FEMALE">{t("account.female")}</option>
                <option value="OTHER">{t("account.other")}</option>
              </Select>
              <Button disabled={busy === "profileSave"} type="submit">
                {busy === "profileSave" ? t("common.working") : t("account.saveProfile")}
              </Button>
            </form>

            <div className="mt-8 grid gap-6 border-t border-slate-100 pt-6">
              <label className="grid gap-2 text-sm font-bold text-slate-600">
                {t("account.avatar")}
                <input
                  type="file"
                  accept="image/*"
                  disabled={busy === "avatar"}
                  onChange={uploadAvatar}
                  className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm"
                />
              </label>

              <form id="password" className="scroll-mt-28 grid gap-4" onSubmit={savePassword}>
                <h3 className="font-serif text-2xl font-bold text-slate-950">{t("account.password")}</h3>
                <Input
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(event) => setPasswordForm({ ...passwordForm, currentPassword: event.target.value })}
                  placeholder={t("account.currentPassword")}
                  required
                />
                <Input
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(event) => setPasswordForm({ ...passwordForm, newPassword: event.target.value })}
                  placeholder={t("account.newPassword")}
                  required
                />
                <Input
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(event) => setPasswordForm({ ...passwordForm, confirmPassword: event.target.value })}
                  placeholder={t("account.confirmPassword")}
                  required
                />
                <Button variant="secondary" disabled={busy === "password"} type="submit">
                  {busy === "password" ? t("common.working") : t("account.changePassword")}
                </Button>
              </form>
            </div>
          </Panel>

          {/* Addresses & Sessions Panel */}
          <Panel title={t("account.addresses")}>
            <div className="grid gap-3">
              {addresses.length ? (
                addresses.map((address) => (
                  <div className="rounded-2xl bg-slate-50 p-4" key={address.id}>
                    <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                      <div>
                        <p className="font-bold text-slate-950">{address.recipientName} - {address.phoneNumber}</p>
                        <p className="mt-1 text-sm text-slate-500">{fullAddress(address)}</p>
                        <span className="mt-2 inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
                          {address.defaultAddress ? t("common.default") : t("common.saved")}
                        </span>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Button size="sm" variant="secondary" onClick={() => editAddress(address)}>{t("common.edit")}</Button>
                        <Button size="sm" variant="secondary" disabled={busy === `default-${address.id}`} onClick={() => makeDefault(address)}>
                          {t("common.default")}
                        </Button>
                        <Button size="sm" variant="danger" disabled={busy === `delete-${address.id}`} onClick={() => removeAddress(address)}>
                          {t("common.delete")}
                        </Button>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-slate-500">{t("account.noAddresses")}</p>
              )}
            </div>

            <form className="mt-8 grid gap-4 border-t border-slate-100 pt-6" onSubmit={saveAddress}>
              <div className="flex items-center justify-between gap-4">
                <h3 className="font-serif text-2xl font-bold text-slate-950">
                  {editingAddressId ? t("account.editAddress") : t("account.addAddress")}
                </h3>
                {editingAddressId && (
                  <Button size="sm" variant="secondary" type="button" onClick={resetAddressForm}>{t("account.cancelEdit")}</Button>
                )}
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  value={addressForm.recipientName}
                  onChange={(event) => setAddressForm({ ...addressForm, recipientName: event.target.value })}
                  placeholder={t("checkout.recipientName")}
                  required
                />
                <Input
                  value={addressForm.phoneNumber}
                  onChange={(event) => setAddressForm({ ...addressForm, phoneNumber: event.target.value })}
                  placeholder={t("checkout.phoneNumber")}
                  required
                />
              </div>
              <Input
                value={addressForm.addressLine}
                onChange={(event) => setAddressForm({ ...addressForm, addressLine: event.target.value })}
                placeholder={t("checkout.addressLine")}
                required
              />
              <div className="grid gap-4 md:grid-cols-3">
                <Input value={addressForm.ward} onChange={(event) => setAddressForm({ ...addressForm, ward: event.target.value })} placeholder={t("checkout.ward")} />
                <Input value={addressForm.district} onChange={(event) => setAddressForm({ ...addressForm, district: event.target.value })} placeholder={t("checkout.district")} />
                <Input value={addressForm.city} onChange={(event) => setAddressForm({ ...addressForm, city: event.target.value })} placeholder={t("checkout.city")} />
              </div>
              <Checkbox
                checked={addressForm.defaultAddress}
                onChange={(event) => setAddressForm({ ...addressForm, defaultAddress: event.target.checked })}
              >
                {t("account.defaultAddress")}
              </Checkbox>
              <Button disabled={busy === "address"} type="submit">
                {busy === "address"
                  ? t("common.working")
                  : editingAddressId
                    ? t("account.updateAddress")
                    : t("account.addAddress")}
              </Button>
            </form>

            <div className="mt-8 grid gap-4 border-t border-slate-100 pt-6">
              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("account.sessions")}</h3>
              {sessions.length ? (
                sessions.map((session) => {
                  const sessionId = session.sessionId || session.id;
                  return (
                    <div className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 p-4" key={sessionId}>
                      <span className="text-sm text-slate-600">
                        <strong className="block text-slate-950">{session.deviceInfo || t("account.device")}</strong>
                        {session.ipAddress || ""}
                      </span>
                      <div className="flex items-center gap-2">
                        <Badge variant={session.current ? "info" : "neutral"}>
                          {session.current ? t("common.current") : t("common.active")}
                        </Badge>
                        <Button size="sm" variant="secondary" disabled={busy === `session-${sessionId}`} onClick={() => revoke(sessionId)}>
                          {t("account.revoke")}
                        </Button>
                      </div>
                    </div>
                  );
                })
              ) : (
                <p className="text-sm text-slate-500">{t("account.noSessions")}</p>
              )}
              <Button variant="secondary" disabled={busy === "logoutAll"} type="button" onClick={logoutEverywhere}>
                {busy === "logoutAll" ? t("common.working") : t("account.logoutAll")}
              </Button>
            </div>

            <div className="mt-8 rounded-2xl border border-red-100 bg-red-50 p-5">
              <h3 className="font-serif text-2xl font-bold text-red-700">{t("account.dangerZone")}</h3>
              <p className="mt-2 text-sm font-semibold text-red-600">{t("account.deactivateHelp")}</p>
              <Input
                className="mt-4 border-red-100 bg-white"
                value={deactivateConfirmation}
                onChange={(event) => setDeactivateConfirmation(event.target.value)}
                placeholder="DEACTIVATE"
              />
              <Button
                variant="danger"
                className="mt-4"
                disabled={busy === "deactivate" || deactivateConfirmation !== "DEACTIVATE"}
                type="button"
                onClick={deactivate}
              >
                {busy === "deactivate" ? t("common.working") : t("account.deactivate")}
              </Button>
            </div>
          </Panel>
        </div>
      )}
    </div>
  );
}

function fullAddress(address) {
  return [address.addressLine, address.ward, address.district, address.city].filter(Boolean).join(", ");
}
