import { useEffect, useState } from "react";
import { getSessions, logoutAll, revokeSession } from "../api/authApi.js";
import { createAddress, deleteAddress, getAddresses, setDefaultAddress, updateAddress } from "../api/orderApi.js";
import { changePassword, getProfile, updateAvatar, updateProfile } from "../api/userApi.js";
import { getAccessToken, getCurrentUser, saveCurrentUser } from "../utils/storage.js";

const emptyAddress = { recipientName: "", phoneNumber: "", addressLine: "", ward: "", district: "", city: "", defaultAddress: false };

export default function AccountPage({ onAuth }) {
  const [profile, setProfile] = useState(getCurrentUser());
  const [profileForm, setProfileForm] = useState({ firstName: "", lastName: "", gender: "" });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
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
      setProfileForm({ firstName: data?.firstName || "", lastName: data?.lastName || "", gender: data?.gender || "" });
    } catch (error) {
      setMessage(error.message || "Could not load backend profile.");
    }
  }

  async function refreshAddresses() {
    try {
      const rows = await getAddresses();
      setAddresses(rows || []);
    } catch (error) {
      setMessage(error.message || "Could not load backend addresses.");
    }
  }

  async function saveProfile(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage("Login required to update profile.");
      return;
    }
    try {
      const updated = await updateProfile({ ...profileForm, gender: profileForm.gender || null });
      setProfile(updated);
      saveCurrentUser(updated);
      setMessage("Profile updated.");
    } catch (error) {
      setMessage(error.message || "Profile update failed.");
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
      setMessage("Avatar updated.");
    } catch (error) {
      setMessage(error.message || "Avatar update failed.");
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    setMessage("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMessage("Confirm password does not match.");
      return;
    }
    try {
      await changePassword(passwordForm);
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setMessage("Password changed. Other sessions may be revoked by backend.");
    } catch (error) {
      setMessage(error.message || "Password change failed.");
    }
  }

  async function saveAddress(event) {
    event.preventDefault();
    setMessage("");
    if (!getAccessToken()) {
      setMessage("Login required to save addresses.");
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
      setMessage("Address saved.");
    } catch (error) {
      setMessage(error.message || "Address save failed.");
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
      defaultAddress: Boolean(address.defaultAddress)
    });
  }

  async function makeDefault(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage("Login required to update addresses.");
      return;
    }
    try {
      const updated = await setDefaultAddress(address.id);
      const next = addresses.map((item) => ({ ...item, defaultAddress: item.id === updated.id || item.id === address.id }));
      setAddresses(next);
      setMessage("Default address updated.");
    } catch (error) {
      setMessage(error.message || "Set default address failed.");
    }
  }

  async function removeAddress(address) {
    setMessage("");
    if (!getAccessToken()) {
      setMessage("Login required to delete addresses.");
      return;
    }
    try {
      await deleteAddress(address.id);
      const next = addresses.filter((item) => item.id !== address.id);
      setAddresses(next);
      setMessage("Address deleted.");
    } catch (error) {
      setMessage(error.message || "Delete address failed.");
    }
  }

  async function revoke(sessionId) {
    setMessage("");
    try {
      await revokeSession(sessionId);
      setSessions((current) => current.filter((item) => item.sessionId !== sessionId && item.id !== sessionId));
      setMessage("Session revoked.");
    } catch (error) {
      setMessage(error.message || "Revoke session failed.");
    }
  }

  async function logoutEverywhere() {
    setMessage("");
    try {
      await logoutAll();
      setSessions([]);
      setMessage("All sessions logged out.");
    } catch (error) {
      setMessage(error.message || "Logout all failed.");
    }
  }

  return (
    <div className="page-shell">
      <PageHeader title="Account" eyebrow="Profile, sessions, and address book" />
      {message && <div className="notice page-notice">{message}</div>}
      {!profile ? (
        <EmptyState title="Please login to manage your account" action={<button className="btn-fill" onClick={onAuth}>Login</button>} />
      ) : (
        <div className="dashboard-grid">
          <div className="panel form-panel">
            <h3>Profile</h3>
            {profile.avatarUrl && <img className="avatar-preview" src={profile.avatarUrl} alt={profile.username || "Avatar"} />}
            <p>{profile.username || profile.email}</p>
            <p>{profile.email || "Aivira account"}</p>
            <form className="compact-form form-panel" onSubmit={saveProfile}>
              <div className="form-grid">
                <input value={profileForm.firstName} onChange={(e) => setProfileForm({ ...profileForm, firstName: e.target.value })} placeholder="First name" />
                <input value={profileForm.lastName} onChange={(e) => setProfileForm({ ...profileForm, lastName: e.target.value })} placeholder="Last name" />
              </div>
              <select value={profileForm.gender} onChange={(e) => setProfileForm({ ...profileForm, gender: e.target.value })}>
                <option value="">Gender</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
              <button className="btn-fill" type="submit">Save profile</button>
            </form>
            {getAccessToken() && (
              <>
                <label className="file-line">Avatar <input type="file" accept="image/*" onChange={uploadAvatar} /></label>
                <form className="compact-form form-panel" onSubmit={savePassword}>
                  <h3>Password</h3>
                  <input type="password" value={passwordForm.currentPassword} onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })} placeholder="Current password" required />
                  <input type="password" value={passwordForm.newPassword} onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })} placeholder="New password" required />
                  <input type="password" value={passwordForm.confirmPassword} onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })} placeholder="Confirm password" required />
                  <button className="btn-line dark" type="submit">Change password</button>
                </form>
              </>
            )}
          </div>

          <div className="panel form-panel">
            <h3>Addresses</h3>
            {addresses.length ? addresses.map((address) => (
              <div className="mini-row" key={address.id}>
                <span>{address.recipientName} - {address.phoneNumber}<br />{address.addressLine}, {address.ward || ""} {address.district || ""} {address.city || ""}</span>
                <small>{address.defaultAddress ? "Default" : "Saved"}</small>
                <button type="button" onClick={() => editAddress(address)}>Edit</button>
                <button type="button" onClick={() => makeDefault(address)}>Default</button>
                <button type="button" onClick={() => removeAddress(address)}>Delete</button>
              </div>
            )) : <p>No saved addresses.</p>}
            <form className="form-panel compact-form" onSubmit={saveAddress}>
              <h3>{editingAddressId ? "Edit address" : "Add address"}</h3>
              <div className="form-grid">
                <input value={addressForm.recipientName} onChange={(e) => setAddressForm({ ...addressForm, recipientName: e.target.value })} placeholder="Recipient name" required />
                <input value={addressForm.phoneNumber} onChange={(e) => setAddressForm({ ...addressForm, phoneNumber: e.target.value })} placeholder="Phone number" required />
              </div>
              <input value={addressForm.addressLine} onChange={(e) => setAddressForm({ ...addressForm, addressLine: e.target.value })} placeholder="Address line" required />
              <div className="form-grid">
                <input value={addressForm.ward} onChange={(e) => setAddressForm({ ...addressForm, ward: e.target.value })} placeholder="Ward" />
                <input value={addressForm.district} onChange={(e) => setAddressForm({ ...addressForm, district: e.target.value })} placeholder="District" />
                <input value={addressForm.city} onChange={(e) => setAddressForm({ ...addressForm, city: e.target.value })} placeholder="City" />
              </div>
              <label className="check-line"><input type="checkbox" checked={addressForm.defaultAddress} onChange={(e) => setAddressForm({ ...addressForm, defaultAddress: e.target.checked })} /> Default address</label>
              <button className="btn-fill" type="submit">{editingAddressId ? "Update address" : "Add address"}</button>
            </form>

            {getAccessToken() && (
              <div className="form-panel compact-form">
                <h3>Sessions</h3>
                {sessions.length ? sessions.map((session) => (
                  <div className="mini-row" key={session.sessionId || session.id}>
                    <span>{session.deviceInfo || "Device"}<br />{session.ipAddress || ""}</span>
                    <small>{session.current ? "Current" : "Active"}</small>
                    <button type="button" onClick={() => revoke(session.sessionId || session.id)}>Revoke</button>
                  </div>
                )) : <p>No session data loaded.</p>}
                <button className="btn-line dark" type="button" onClick={logoutEverywhere}>Logout all sessions</button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}

function EmptyState({ title, action }) {
  return <div className="empty"><h3>{title}</h3>{action}</div>;
}
