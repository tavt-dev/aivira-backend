import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import { getSessions, logoutAll, revokeSession } from "../api/authApi.js";
import {
  createAddress,
  deleteAddress,
  getAddresses,
  setDefaultAddress,
  updateAddress,
} from "../api/orderApi.js";
import { changePassword, getProfile, updateAvatar, updateProfile } from "../api/userApi.js";
import { getAccessToken, getCurrentUser, saveCurrentUser } from "../utils/storage.js";

const emptyAddress = {
  recipientName: "",
  phoneNumber: "",
  addressLine: "",
  ward: "",
  district: "",
  city: "",
  defaultAddress: false,
};

export default function AccountPage({ onAuth }) {
  const { t } = useTranslation();
  const [profile, setProfile] = useState(getCurrentUser());
  const [profileForm, setProfileForm] = useState({ firstName: "", lastName: "", gender: "" });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [addresses, setAddresses] = useState([]);
  const [addressForm, setAddressForm] = useState(emptyAddress);
  const [editingAddressId, setEditingAddressId] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!getAccessToken()) return;
    refreshProfile();
    refreshAddresses();
    getSessions().then(setSessions).catch(() => {});
  }, []);

  async function refreshProfile() {
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
    }
  }

  async function refreshAddresses() {
    try {
      const rows = await getAddresses();
      setAddresses(rows || []);
    } catch (error) {
      setMessage(error.message || t("account.addressesLoadFailed"));
    }
  }

  async function saveProfile(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginUpdateProfile"));
      return;
    }
    try {
      const updated = await updateProfile({ ...profileForm, gender: profileForm.gender || null });
      setProfile(updated);
      saveCurrentUser(updated);
      setMessage(t("account.profileUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.profileUpdateFailed"));
    }
  }

  async function uploadAvatar(event) {
    const file = event.target.files?.[0];
    if (!file || !getAccessToken()) return;
    setMessage("");
    try {
      const updated = await updateAvatar(file);
      setProfile(updated);
      saveCurrentUser(updated);
      setMessage(t("account.avatarUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.avatarFailed"));
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    setMessage("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMessage(t("account.confirmMismatch"));
      return;
    }
    try {
      await changePassword(passwordForm);
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setMessage(t("account.passwordChanged"));
    } catch (error) {
      setMessage(error.message || t("account.passwordFailed"));
    }
  }

  async function saveAddress(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginSaveAddress"));
      return;
    }
    try {
      const saved = editingAddressId
        ? await updateAddress(editingAddressId, addressForm)
        : await createAddress(addressForm);
      const next = editingAddressId
        ? addresses.map((item) => (item.id === editingAddressId ? saved : item))
        : [saved, ...addresses];
      setAddresses(next);
      setEditingAddressId(null);
      setAddressForm(emptyAddress);
      setMessage(t("account.addressSaved"));
    } catch (error) {
      setMessage(error.message || t("account.addressFailed"));
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

  async function makeDefault(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginUpdateAddress"));
      return;
    }
    try {
      const updated = await setDefaultAddress(address.id);
      setAddresses((current) =>
        current.map((item) => ({
          ...item,
          defaultAddress: item.id === updated.id || item.id === address.id,
        }))
      );
      setMessage(t("account.defaultUpdated"));
    } catch (error) {
      setMessage(error.message || t("account.defaultFailed"));
    }
  }

  async function removeAddress(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage(t("account.loginDeleteAddress"));
      return;
    }
    try {
      await deleteAddress(address.id);
      setAddresses((current) => current.filter((item) => item.id !== address.id));
      setMessage(t("account.addressDeleted"));
    } catch (error) {
      setMessage(error.message || t("account.deleteFailed"));
    }
  }

  async function revoke(sessionId) {
    setMessage("");
    try {
      await revokeSession(sessionId);
      setSessions((current) =>
        current.filter((item) => item.sessionId !== sessionId && item.id !== sessionId)
      );
      setMessage(t("account.sessionRevoked"));
    } catch (error) {
      setMessage(error.message || t("account.revokeFailed"));
    }
  }

  async function logoutEverywhere() {
    setMessage("");
    try {
      await logoutAll();
      setSessions([]);
      setMessage(t("account.loggedOutAll"));
    } catch (error) {
      setMessage(error.message || t("account.logoutAllFailed"));
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <PageHeader title={t("account.title")} eyebrow={t("account.eyebrow")} />
      {message && <Notice>{message}</Notice>}

      {!profile ? (
        <EmptyState
          title={t("account.loginRequired")}
          action={
            <button
              type="button"
              className="rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
              onClick={onAuth}
            >
              {t("common.login")}
            </button>
          }
        />
      ) : (
        <div className="grid gap-8 lg:grid-cols-2">
          <Panel title={t("account.profile")}>
            <div className="flex items-center gap-4">
              {profile.avatarUrl ? (
                <img
                  className="h-20 w-20 rounded-2xl object-cover"
                  src={profile.avatarUrl}
                  alt={profile.username || "Avatar"}
                />
              ) : (
                <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-slate-950 font-serif text-3xl font-bold text-white">
                  A
                </div>
              )}
              <div className="min-w-0">
                <p className="truncate font-bold text-slate-950">
                  {profile.username || profile.email}
                </p>
                <p className="truncate text-sm text-slate-500">{profile.email || t("account.aiviraAccount")}</p>
              </div>
            </div>

            <form className="mt-6 grid gap-4" onSubmit={saveProfile}>
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  value={profileForm.firstName}
                  onChange={(event) =>
                    setProfileForm({ ...profileForm, firstName: event.target.value })
                  }
                  placeholder={t("account.firstName")}
                />
                <Input
                  value={profileForm.lastName}
                  onChange={(event) =>
                    setProfileForm({ ...profileForm, lastName: event.target.value })
                  }
                  placeholder={t("account.lastName")}
                />
              </div>
              <select
                value={profileForm.gender}
                onChange={(event) => setProfileForm({ ...profileForm, gender: event.target.value })}
                className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
              >
                <option value="">{t("account.gender")}</option>
                <option value="MALE">{t("account.male")}</option>
                <option value="FEMALE">{t("account.female")}</option>
                <option value="OTHER">{t("account.other")}</option>
              </select>
              <Button type="submit">{t("account.saveProfile")}</Button>
            </form>

            {getAccessToken() && (
              <div className="mt-8 grid gap-6 border-t border-slate-100 pt-6">
                <label className="grid gap-2 text-sm font-bold text-slate-600">
                  {t("account.avatar")}
                  <input
                    type="file"
                    accept="image/*"
                    onChange={uploadAvatar}
                    className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm"
                  />
                </label>

                <form id="password" className="scroll-mt-28 grid gap-4" onSubmit={savePassword}>
                  <h3 className="font-serif text-2xl font-bold text-slate-950">{t("account.password")}</h3>
                  <Input
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={(event) =>
                      setPasswordForm({ ...passwordForm, currentPassword: event.target.value })
                    }
                    placeholder={t("account.currentPassword")}
                    required
                  />
                  <Input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(event) =>
                      setPasswordForm({ ...passwordForm, newPassword: event.target.value })
                    }
                    placeholder={t("account.newPassword")}
                    required
                  />
                  <Input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(event) =>
                      setPasswordForm({ ...passwordForm, confirmPassword: event.target.value })
                    }
                    placeholder={t("account.confirmPassword")}
                    required
                  />
                  <Button secondary type="submit">
                    {t("account.changePassword")}
                  </Button>
                </form>
              </div>
            )}
          </Panel>

          <Panel title={t("account.addresses")}>
            <div className="grid gap-3">
              {addresses.length ? (
                addresses.map((address) => (
                  <div className="rounded-2xl bg-slate-50 p-4" key={address.id}>
                    <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                      <div>
                        <p className="font-bold text-slate-950">
                          {address.recipientName} - {address.phoneNumber}
                        </p>
                        <p className="mt-1 text-sm text-slate-500">
                          {address.addressLine}, {address.ward || ""} {address.district || ""}{" "}
                          {address.city || ""}
                        </p>
                        <span className="mt-2 inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
                          {address.defaultAddress ? t("common.default") : t("common.saved")}
                        </span>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <SmallButton onClick={() => editAddress(address)}>{t("common.edit")}</SmallButton>
                        <SmallButton onClick={() => makeDefault(address)}>{t("common.default")}</SmallButton>
                        <SmallButton danger onClick={() => removeAddress(address)}>
                          {t("common.delete")}
                        </SmallButton>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-slate-500">{t("account.noAddresses")}</p>
              )}
            </div>

            <form className="mt-8 grid gap-4 border-t border-slate-100 pt-6" onSubmit={saveAddress}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">
                {editingAddressId ? t("account.editAddress") : t("account.addAddress")}
              </h3>
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  value={addressForm.recipientName}
                  onChange={(event) =>
                    setAddressForm({ ...addressForm, recipientName: event.target.value })
                  }
                  placeholder={t("checkout.recipientName")}
                  required
                />
                <Input
                  value={addressForm.phoneNumber}
                  onChange={(event) =>
                    setAddressForm({ ...addressForm, phoneNumber: event.target.value })
                  }
                  placeholder={t("checkout.phoneNumber")}
                  required
                />
              </div>
              <Input
                value={addressForm.addressLine}
                onChange={(event) =>
                  setAddressForm({ ...addressForm, addressLine: event.target.value })
                }
                placeholder={t("checkout.addressLine")}
                required
              />
              <div className="grid gap-4 md:grid-cols-3">
                <Input
                  value={addressForm.ward}
                  onChange={(event) => setAddressForm({ ...addressForm, ward: event.target.value })}
                  placeholder={t("checkout.ward")}
                />
                <Input
                  value={addressForm.district}
                  onChange={(event) =>
                    setAddressForm({ ...addressForm, district: event.target.value })
                  }
                  placeholder={t("checkout.district")}
                />
                <Input
                  value={addressForm.city}
                  onChange={(event) => setAddressForm({ ...addressForm, city: event.target.value })}
                  placeholder={t("checkout.city")}
                />
              </div>
              <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                <input
                  type="checkbox"
                  checked={addressForm.defaultAddress}
                  onChange={(event) =>
                    setAddressForm({ ...addressForm, defaultAddress: event.target.checked })
                  }
                />
                {t("account.defaultAddress")}
              </label>
              <Button type="submit">{editingAddressId ? t("account.updateAddress") : t("account.addAddress")}</Button>
            </form>

            {getAccessToken() && (
              <div className="mt-8 grid gap-4 border-t border-slate-100 pt-6">
                <h3 className="font-serif text-2xl font-bold text-slate-950">{t("account.sessions")}</h3>
                {sessions.length ? (
                  sessions.map((session) => (
                    <div
                      className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 p-4"
                      key={session.sessionId || session.id}
                    >
                      <span className="text-sm text-slate-600">
                        <strong className="block text-slate-950">
                          {session.deviceInfo || t("account.device")}
                        </strong>
                        {session.ipAddress || ""}
                      </span>
                      <div className="flex items-center gap-2">
                        <small className="rounded-full bg-slate-200 px-2 py-1 text-xs font-bold text-slate-600">
                          {session.current ? t("common.current") : t("common.active")}
                        </small>
                        <SmallButton onClick={() => revoke(session.sessionId || session.id)}>
                          {t("account.revoke")}
                        </SmallButton>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-slate-500">{t("account.noSessions")}</p>
                )}
                <Button secondary type="button" onClick={logoutEverywhere}>
                  {t("account.logoutAll")}
                </Button>
              </div>
            )}
          </Panel>
        </div>
      )}
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="mb-8 border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
        {eyebrow}
      </span>
      <h1 className="mt-3 font-serif text-4xl font-bold text-slate-950 md:text-5xl">{title}</h1>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
      <h2 className="mb-6 font-serif text-3xl font-bold text-slate-950">{title}</h2>
      {children}
    </section>
  );
}

function Input({ className = "", ...props }) {
  return (
    <input
      {...props}
      className={[
        "w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100",
        className,
      ].join(" ")}
    />
  );
}

function Button({ secondary = false, className = "", ...props }) {
  return (
    <button
      {...props}
      className={[
        "rounded-full px-6 py-3 text-sm font-bold transition-colors",
        secondary
          ? "border border-slate-200 text-slate-700 hover:bg-slate-50"
          : "bg-slate-950 text-white hover:bg-blue-600",
        className,
      ].join(" ")}
    />
  );
}

function SmallButton({ danger = false, className = "", ...props }) {
  return (
    <button
      type="button"
      {...props}
      className={[
        "rounded-full border px-3 py-1.5 text-xs font-bold transition-colors",
        danger
          ? "border-red-100 text-red-600 hover:bg-red-50"
          : "border-slate-200 text-slate-600 hover:bg-white",
        className,
      ].join(" ")}
    />
  );
}

function EmptyState({ title, action }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-300 bg-white px-8 py-16 text-center">
      <h2 className="font-serif text-3xl font-bold text-slate-950">{title}</h2>
      {action && <div className="mt-6 flex justify-center">{action}</div>}
    </div>
  );
}

function Notice({ children }) {
  return (
    <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">
      {children}
    </div>
  );
}
