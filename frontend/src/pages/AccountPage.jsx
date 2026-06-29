import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import {
  AlertCircle, BadgeCheck, Camera, CheckCircle2,
  ChevronRight, Globe, Home, Key, Laptop, Lock,
  LogOut, Mail, MapPin, Monitor, Phone, Plus,
  ShieldOff, Star, Trash2, User, UserCircle,
  X, Eye, EyeOff, Edit3, Save, RotateCcw,
} from "lucide-react";

import { getSessions, logoutAll, revokeSession } from "../api/authApi.js";
import {
  changePassword, createAddress, deactivateAccount,
  deleteAddress, getAddresses, getProfile,
  setDefaultAddress, updateAddress, updateAvatar, updateProfile,
} from "../api/userApi.js";
import { formatDateTime } from "../utils/formatters.js";
import { normalizeAddress } from "../utils/mappers.js";
import { clearAuth, getAccessToken, getCurrentUser, saveCurrentUser } from "../utils/storage.js";
import { getTheme } from "../utils/theme.js";

/* ── Constants ─────────────────────────────── */
const emptyAddress  = { recipientName:"", phoneNumber:"", addressLine:"", ward:"", district:"", city:"", defaultAddress:false };
const emptyPassword = { currentPassword:"", newPassword:"", confirmPassword:"" };

const TABS = [
  { id:"profile",   icon:UserCircle, label:"account.profile" },
  { id:"address",   icon:MapPin,     label:"account.addresses" },
  { id:"security",  icon:Key,        label:"account.password" },
  { id:"sessions",  icon:Monitor,    label:"account.sessions" },
  { id:"danger",    icon:ShieldOff,  label:"account.deactivate" },
];

/* ── Token system ───────────────────────────── */
function tokens(isDark) {
  if (isDark) return {
    pageBg:    "radial-gradient(ellipse at 65% 0%,rgba(30,24,80,0.9) 0%,#07091a 55%)",
    surface1:  "rgba(10,15,42,0.95)",
    surface2:  "rgba(16,22,58,0.88)",
    surface3:  "rgba(22,28,70,0.75)",
    heroBg:    "linear-gradient(135deg,rgba(12,17,48,0.98) 0%,rgba(8,12,35,0.99) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#4f6ef7 40%,#a78bfa 70%,transparent)",
    border:    "rgba(255,255,255,0.075)",
    borderMid: "rgba(255,255,255,0.14)",
    text1:     "#e8eeff",
    text2:     "#8892b0",
    text3:     "#4a5578",
    accent:    "#4f6ef7",
    gold:      "#f0a500",
    emerald:   "#10d98a",
    red:       "#ef4444",
    skA:       "rgba(255,255,255,0.03)",
    skB:       "rgba(79,110,247,0.10)",
    skC:       "rgba(167,139,250,0.08)",
    inputBg:   "rgba(255,255,255,0.05)",
    orb1:      "rgba(79,110,247,0.25)",
    orb2:      "rgba(240,165,0,0.16)",
    orb3:      "rgba(167,139,250,0.15)",
  };
  return {
    pageBg:    "radial-gradient(ellipse at 65% 0%,rgba(210,220,255,0.35) 0%,#f0ede8 55%)",
    surface1:  "rgba(255,252,246,0.97)",
    surface2:  "rgba(250,247,241,0.92)",
    surface3:  "rgba(244,241,234,0.88)",
    heroBg:    "linear-gradient(135deg,rgba(15,23,42,0.97) 0%,rgba(22,30,58,0.98) 100%)",
    heroLine:  "linear-gradient(90deg,transparent,#6d8fff 40%,#c4b5fd 70%,transparent)",
    border:    "rgba(15,23,42,0.10)",
    borderMid: "rgba(15,23,42,0.18)",
    text1:     "#0f172a",
    text2:     "#334155",
    text3:     "#94a3b8",
    accent:    "#1d4ed8",
    gold:      "#b45309",
    emerald:   "#047857",
    red:       "#b91c1c",
    skA:       "rgba(15,23,42,0.05)",
    skB:       "rgba(37,99,235,0.07)",
    skC:       "rgba(139,92,246,0.05)",
    inputBg:   "rgba(15,23,42,0.06)",
    orb1:      "rgba(79,110,247,0.10)",
    orb2:      "rgba(240,165,0,0.08)",
    orb3:      "rgba(167,139,250,0.08)",
  };
}

function useTheme() {
  const [isDark, setIsDark] = useState(() => getTheme() === "dark");
  useEffect(() => {
    const sync = () => setIsDark(getTheme() === "dark");
    window.addEventListener("aivira-theme", sync);
    return () => window.removeEventListener("aivira-theme", sync);
  }, []);
  return isDark;
}

/* ══════════════════════════════════════════════
   ACCOUNT PAGE
══════════════════════════════════════════════ */
export default function AccountPage({ onAuth }) {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isDark = useTheme();
  const tk = tokens(isDark);

  const [tab, setTab]                             = useState("profile");
  const [profile, setProfile]                     = useState(getCurrentUser());
  const [profileForm, setProfileForm]             = useState({ firstName:"", lastName:"", gender:"" });
  const [passwordForm, setPasswordForm]           = useState(emptyPassword);
  const [addresses, setAddresses]                 = useState([]);
  const [addressForm, setAddressForm]             = useState(emptyAddress);
  const [editingAddressId, setEditingAddressId]   = useState(null);
  const [sessions, setSessions]                   = useState([]);
  const [message, setMessage]                     = useState("");
  const [msgOk, setMsgOk]                         = useState(false);
  const [busy, setBusy]                           = useState("");
  const [deactivateConfirm, setDeactivateConfirm] = useState("");
  const [showPw, setShowPw]                       = useState({});
  const fileRef = useRef();

  /* ── Data loaders ── */
  const refreshProfile = useCallback(async () => {
    setBusy(c => c||"profile");
    try {
      const data = await getProfile();
      setProfile(data); saveCurrentUser(data);
      setProfileForm({ firstName:data?.firstName||"", lastName:data?.lastName||"", gender:data?.gender||"" });
    } catch(e) { toast(false, e.message||t("account.profileLoadFailed")); }
    finally { setBusy(c => c==="profile"?"":c); }
  }, [t]);

  const refreshAddresses = useCallback(async () => {
    try { setAddresses((await getAddresses()||[]).map(normalizeAddress).filter(Boolean)); }
    catch(e) { toast(false, e.message||t("account.addressesLoadFailed")); }
  }, [t]);

  const refreshSessions = useCallback(async () => {
    try { setSessions(await getSessions()||[]); }
    catch { setSessions([]); }
  }, []);

  useEffect(() => {
    if (!getAccessToken()) return;
    refreshProfile(); refreshAddresses(); refreshSessions();
  }, [refreshAddresses, refreshProfile, refreshSessions]);

  function toast(ok, msg) { setMsgOk(ok); setMessage(msg); }

  /* ── Handlers ── */
  async function saveProfile(e) {
    e.preventDefault(); setMessage("");
    if (!getAccessToken()) { toast(false,t("account.loginUpdateProfile")); return; }
    setBusy("profileSave");
    try {
      const u = await updateProfile({ firstName:profileForm.firstName||null, lastName:profileForm.lastName||null, gender:profileForm.gender||null });
      setProfile(u); saveCurrentUser(u); toast(true,t("account.profileUpdated"));
    } catch(err) { toast(false,err.message||t("account.profileUpdateFailed")); }
    finally { setBusy(""); }
  }

  async function uploadAvatar(e) {
    const file = e.target.files?.[0];
    if (!file||!getAccessToken()) return;
    setMessage(""); setBusy("avatar");
    try {
      const u = await updateAvatar(file); setProfile(u); saveCurrentUser(u); toast(true,t("account.avatarUpdated"));
    } catch(err) { toast(false,err.message||t("account.avatarFailed")); }
    finally { e.target.value=""; setBusy(""); }
  }

  async function savePassword(e) {
    e.preventDefault(); setMessage("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) { toast(false,t("account.confirmMismatch")); return; }
    setBusy("password");
    try {
      await changePassword({ currentPassword:passwordForm.currentPassword, newPassword:passwordForm.newPassword });
      setPasswordForm(emptyPassword); toast(true,t("account.passwordChanged")); refreshSessions();
    } catch(err) { toast(false,err.message||t("account.passwordFailed")); }
    finally { setBusy(""); }
  }

  async function saveAddress(e) {
    e.preventDefault(); setMessage("");
    if (!getAccessToken()) { toast(false,t("account.loginSaveAddress")); return; }
    if (!addressForm.recipientName||!addressForm.phoneNumber||!addressForm.addressLine) { toast(false,t("account.addressRequired")); return; }
    setBusy("address");
    try {
      const saved = normalizeAddress(editingAddressId ? await updateAddress(editingAddressId,addressForm) : await createAddress(addressForm));
      setAddresses(cur => editingAddressId ? cur.map(a => a.id===editingAddressId?saved:a) : [saved,...cur]);
      resetAddressForm(); toast(true,t("account.addressSaved"));
    } catch(err) { toast(false,err.message||t("account.addressFailed")); }
    finally { setBusy(""); }
  }

  function editAddress(a) { setEditingAddressId(a.id); setAddressForm({ recipientName:a.recipientName||"", phoneNumber:a.phoneNumber||"", addressLine:a.addressLine||"", ward:a.ward||"", district:a.district||"", city:a.city||"", defaultAddress:Boolean(a.defaultAddress) }); }
  function resetAddressForm() { setEditingAddressId(null); setAddressForm(emptyAddress); }

  async function makeDefault(a) {
    setMessage(""); if (!getAccessToken()) { toast(false,t("account.loginUpdateAddress")); return; }
    setBusy(`default-${a.id}`);
    try {
      const u = normalizeAddress(await setDefaultAddress(a.id));
      setAddresses(cur => cur.map(x => ({ ...x, defaultAddress: x.id===u.id||x.id===a.id })));
      toast(true,t("account.defaultUpdated"));
    } catch(err) { toast(false,err.message||t("account.defaultFailed")); }
    finally { setBusy(""); }
  }

  async function removeAddress(a) {
    setMessage(""); if (!getAccessToken()) { toast(false,t("account.loginDeleteAddress")); return; }
    setBusy(`delete-${a.id}`);
    try {
      await deleteAddress(a.id);
      setAddresses(cur => cur.filter(x => x.id!==a.id));
      if (editingAddressId===a.id) resetAddressForm();
      toast(true,t("account.addressDeleted"));
    } catch(err) { toast(false,err.message||t("account.deleteFailed")); }
    finally { setBusy(""); }
  }

  async function revoke(sessionId) {
    setMessage("");
    const target = sessions.find(s => (s.sessionId||s.id)===sessionId);
    setBusy(`session-${sessionId}`);
    try {
      await revokeSession(sessionId);
      if (target?.current) { clearAuthAndPrompt(); return; }
      setSessions(cur => cur.filter(s => s.sessionId!==sessionId&&s.id!==sessionId));
      toast(true,t("account.sessionRevoked"));
    } catch(err) { toast(false,err.message||t("account.revokeFailed")); }
    finally { setBusy(""); }
  }

  async function logoutEverywhere() {
    setMessage(""); setBusy("logoutAll");
    try { await logoutAll(); clearAuthAndPrompt(); toast(true,t("account.loggedOutAll")); }
    catch(err) { toast(false,err.message||t("account.logoutAllFailed")); }
    finally { setBusy(""); }
  }

  async function deactivate() {
    setMessage("");
    if (deactivateConfirm!=="DEACTIVATE") { toast(false,t("account.deactivateMismatch")); return; }
    setBusy("deactivate");
    try { await deactivateAccount(); clearAuthAndPrompt(); toast(true,t("account.deactivated")); }
    catch(err) { toast(false,err.message||t("account.deactivateFailed")); }
    finally { setBusy(""); }
  }

  function clearAuthAndPrompt() { clearAuth(); setProfile(null); setSessions([]); onAuth?.(); navigate("/?auth=login&next=/account",{replace:true}); }

  const loggedIn = Boolean(getAccessToken());

  /* ── Avatar initial ── */
  const initials = [profile?.firstName, profile?.lastName].filter(Boolean).map(s=>s[0]).join("") || (profile?.username||"A")[0].toUpperCase();

  return (
    <div className="relative w-full overflow-hidden" style={{ background:tk.pageBg, minHeight:"100vh" }}>
      {/* Dot grid */}
      <div className="pointer-events-none absolute inset-0" style={{
        backgroundImage:`radial-gradient(circle,${isDark?"rgba(79,110,247,0.09)":"rgba(37,99,235,0.05)"} 1px,transparent 1px)`,
        backgroundSize:"48px 48px",
        maskImage:"radial-gradient(ellipse at 50% 0%,black 0%,transparent 65%)",
      }}/>
      {/* Orbs */}
      <div className="pointer-events-none absolute -right-40 -top-40 h-[500px] w-[500px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb1} 0%,transparent 70%)`, filter:"blur(80px)" }}/>
      <div className="pointer-events-none absolute -left-32 top-[40%] h-[400px] w-[400px] rounded-full"
        style={{ background:`radial-gradient(circle,${tk.orb2} 0%,transparent 70%)`, filter:"blur(90px)" }}/>

      <div className="relative z-10 mx-auto max-w-7xl px-4 pb-28 pt-20 md:px-8">

        {/* ── Hero bar with avatar ── */}
        <AccountHeroBar profile={profile} initials={initials} tk={tk} isDark={isDark} t={t}
          onAvatarClick={() => fileRef.current?.click()}
          busy={busy==="avatar"}/>
        <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={uploadAvatar}/>

        {/* Toast */}
        <AnimatePresence>
          {message && (
            <motion.div key="toast"
              initial={{opacity:0,y:-12,scale:0.96}} animate={{opacity:1,y:0,scale:1}} exit={{opacity:0,y:-12}}
              transition={{duration:0.3,ease:[0.22,1,0.36,1]}}
              className="mb-6 flex items-center gap-3 rounded-2xl px-5 py-4"
              style={{
                background:msgOk?"rgba(16,217,138,0.12)":"rgba(239,68,68,0.10)",
                border:`1px solid ${msgOk?"rgba(16,217,138,0.4)":"rgba(239,68,68,0.4)"}`,
                backdropFilter:"blur(20px)",
              }}>
              {msgOk?<CheckCircle2 size={15} color={tk.emerald}/>:<AlertCircle size={15} color={tk.red}/>}
              <span className="text-sm font-bold" style={{ color:msgOk?tk.emerald:tk.red }}>{message}</span>
              <button type="button" onClick={() => setMessage("")} className="ml-auto opacity-60 hover:opacity-100">
                <X size={13} color={msgOk?tk.emerald:tk.red}/>
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Not logged in */}
        {(!loggedIn||!profile) && (
          <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.5}}
            className="flex flex-col items-center justify-center gap-6 rounded-[32px] px-8 py-24 text-center"
            style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)" }}>
            <div className="flex h-20 w-20 items-center justify-center rounded-[22px]"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 16px 40px rgba(79,110,247,0.45)" }}>
              <User size={32} color="#fff"/>
            </div>
            <div>
              <h2 className="text-2xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                {t("account.loginRequired")}
              </h2>
            </div>
            <motion.button whileHover={{scale:1.04,y:-2}} whileTap={{scale:0.97}}
              type="button" onClick={onAuth}
              className="rounded-full px-8 py-4 text-sm font-black uppercase tracking-wider text-white"
              style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 28px rgba(79,110,247,0.45)" }}>
              {t("common.login")}
            </motion.button>
          </motion.div>
        )}

        {/* Main logged-in content */}
        {loggedIn && profile && (
          <div className="grid gap-6 lg:grid-cols-[240px_1fr]">

            {/* ── Tab sidebar ── */}
            <motion.nav initial={{opacity:0,x:-24}} animate={{opacity:1,x:0}}
              transition={{duration:0.5,delay:0.05,ease:[0.22,1,0.36,1]}}
              className="h-fit overflow-hidden rounded-[22px]"
              style={{ background:tk.surface1, border:`1px solid ${tk.border}`, backdropFilter:"blur(24px)" }}>
              <div className="absolute left-0 right-0 top-0 h-[1.5px]" style={{ background:tk.heroLine }}/>
              <div className="p-2">
                {TABS.map((item, i) => {
                  const active = tab === item.id;
                  const danger  = item.id === "danger";
                  return (
                    <motion.button key={item.id}
                      initial={{opacity:0,x:-16}} animate={{opacity:1,x:0}}
                      transition={{duration:0.35,delay:i*0.06}}
                      whileHover={{ x:3 }} whileTap={{ scale:0.97 }}
                      type="button" onClick={() => setTab(item.id)}
                      className="relative flex w-full items-center gap-3 rounded-xl px-4 py-3.5 text-sm font-bold transition-all"
                      style={{
                        background: active
                          ? danger?"rgba(239,68,68,0.12)":"rgba(79,110,247,0.14)"
                          : "transparent",
                        color: active
                          ? danger?tk.red:tk.accent
                          : danger?"rgba(239,68,68,0.7)":tk.text2,
                        border: active
                          ? `1px solid ${danger?"rgba(239,68,68,0.3)":"rgba(79,110,247,0.3)"}`
                          : "1px solid transparent",
                      }}>
                      <item.icon size={16}/>
                      {t(item.label)}
                      {active && (
                        <motion.div layoutId="tab-indicator" className="ml-auto"
                          initial={false} transition={{type:"spring",stiffness:500,damping:35}}>
                          <ChevronRight size={14}/>
                        </motion.div>
                      )}
                    </motion.button>
                  );
                })}
              </div>
            </motion.nav>

            {/* ── Tab content ── */}
            <AnimatePresence mode="wait">
              <motion.div key={tab}
                initial={{opacity:0,x:20}} animate={{opacity:1,x:0}} exit={{opacity:0,x:-20}}
                transition={{duration:0.32,ease:[0.22,1,0.36,1]}}>

                {/* PROFILE TAB */}
                {tab==="profile" && (
                  <div className="grid gap-6">
                    {/* Profile meta */}
                    <Section title={t("account.profile")} icon={UserCircle} tk={tk} isDark={isDark}>
                      <div className="grid gap-4 md:grid-cols-[80px_1fr]">
                        {/* Avatar */}
                        <motion.div whileHover={{scale:1.05}} className="relative cursor-pointer"
                          onClick={() => fileRef.current?.click()}>
                          <div className="overflow-hidden rounded-2xl"
                            style={{ width:80, height:80, boxShadow:`0 8px 24px ${isDark?"rgba(0,0,0,0.5)":"rgba(15,23,42,0.18)"}` }}>
                            {profile.avatarUrl
                              ? <img className="h-full w-full object-cover" src={profile.avatarUrl} alt={profile.username||"Avatar"}/>
                              : <div className="flex h-full w-full items-center justify-center text-3xl font-black text-white"
                                  style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7,#7c3aed)" }}>
                                  {initials}
                                </div>
                            }
                          </div>
                          <div className="absolute -bottom-1.5 -right-1.5 flex h-7 w-7 items-center justify-center rounded-full"
                            style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 2px 8px rgba(79,110,247,0.5)" }}>
                            {busy==="avatar"
                              ? <motion.div animate={{rotate:360}} transition={{duration:0.8,repeat:Infinity,ease:"linear"}}><RotateCcw size={11} color="#fff"/></motion.div>
                              : <Camera size={11} color="#fff"/>
                            }
                          </div>
                        </motion.div>
                        {/* User info */}
                        <div className="flex flex-col justify-center gap-1.5">
                          <h3 className="text-xl font-black" style={{ color:tk.text1, fontFamily:"var(--f-serif)" }}>
                            {[profile.firstName,profile.lastName].filter(Boolean).join(" ")||profile.username||"—"}
                          </h3>
                          <p className="flex items-center gap-2 text-sm" style={{ color:tk.text2 }}>
                            <Mail size={12} style={{ color:tk.accent }}/> {profile.email||"—"}
                          </p>
                          <div className="mt-1 flex flex-wrap gap-2">
                            <StatusBadge
                              active={true}
                              label={profile.provider||"LOCAL"}
                              color={tk.accent} bg={isDark?"rgba(79,110,247,0.14)":"rgba(29,78,216,0.10)"} border={isDark?"rgba(79,110,247,0.35)":"rgba(29,78,216,0.25)"}/>
                            <StatusBadge
                              active={profile.emailVerified}
                              label={profile.emailVerified?t("account.emailVerified"):t("account.emailUnverified")}
                              color={profile.emailVerified?tk.emerald:tk.gold}
                              bg={profile.emailVerified?isDark?"rgba(16,217,138,0.12)":"rgba(4,120,87,0.08)":isDark?"rgba(240,165,0,0.12)":"rgba(180,83,9,0.08)"}
                              border={profile.emailVerified?isDark?"rgba(16,217,138,0.3)":"rgba(4,120,87,0.22)":isDark?"rgba(240,165,0,0.3)":"rgba(180,83,9,0.22)"}/>
                          </div>
                        </div>
                      </div>
                      {/* Meta rows */}
                      <div className="mt-4 grid gap-3 rounded-2xl p-4" style={{ background:tk.surface2 }}>
                        <MetaLine icon={User} label={t("account.userId")} value={profile.id} tk={tk}/>
                        <MetaLine icon={Phone} label={t("account.phone")} value={profile.phoneNumber||"—"} tk={tk}/>
                        <MetaLine icon={Globe} label={t("account.createdAt")} value={formatDateTime(profile.createdAt,i18n.language)} tk={tk}/>
                      </div>
                    </Section>

                    {/* Edit profile form */}
                    <Section title={t("account.saveProfile")} icon={Edit3} tk={tk} isDark={isDark}>
                      <form className="grid gap-4" onSubmit={saveProfile}>
                        <div className="grid gap-4 md:grid-cols-2">
                          <PInput value={profileForm.firstName} placeholder={t("account.firstName")} tk={tk} isDark={isDark}
                            onChange={e => setProfileForm({...profileForm,firstName:e.target.value})}/>
                          <PInput value={profileForm.lastName} placeholder={t("account.lastName")} tk={tk} isDark={isDark}
                            onChange={e => setProfileForm({...profileForm,lastName:e.target.value})}/>
                        </div>
                        <PSelect value={profileForm.gender} tk={tk} isDark={isDark}
                          onChange={e => setProfileForm({...profileForm,gender:e.target.value})}>
                          <option value="">{t("account.gender")}</option>
                          <option value="MALE">{t("account.male")}</option>
                          <option value="FEMALE">{t("account.female")}</option>
                          <option value="OTHER">{t("account.other")}</option>
                        </PSelect>
                        <PrimaryBtn type="submit" loading={busy==="profileSave"} icon={Save} tk={tk}>
                          {busy==="profileSave"?t("common.working"):t("account.saveProfile")}
                        </PrimaryBtn>
                      </form>
                    </Section>
                  </div>
                )}

                {/* ADDRESS TAB */}
                {tab==="address" && (
                  <div className="grid gap-6">
                    {/* Address list */}
                    <Section title={t("account.addresses")} icon={MapPin} tk={tk} isDark={isDark}>
                      {addresses.length===0
                        ? <p className="py-4 text-center text-sm" style={{ color:tk.text3 }}>{t("account.noAddresses")}</p>
                        : (
                          <div className="grid gap-3">
                            <AnimatePresence>
                              {addresses.map((a,i) => (
                                <motion.div key={a.id}
                                  initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} exit={{opacity:0,x:40}}
                                  transition={{duration:0.35,delay:i*0.04}}
                                  className="overflow-hidden rounded-2xl"
                                  style={{
                                    background:tk.surface2, border:`1px solid ${a.defaultAddress?isDark?"rgba(16,217,138,0.3)":"rgba(4,120,87,0.25)":tk.border}`,
                                    boxShadow:a.defaultAddress?`0 4px 20px ${isDark?"rgba(16,217,138,0.12)":"rgba(4,120,87,0.08)"}`:undefined,
                                  }}>
                                  {a.defaultAddress && <div className="h-[1.5px]" style={{ background:"linear-gradient(90deg,transparent,#10d98a 40%,transparent)" }}/>}
                                  <div className="flex flex-col gap-3 p-4 md:flex-row md:items-start md:justify-between">
                                    <div className="flex items-start gap-3">
                                      <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl mt-0.5"
                                        style={{ background:a.defaultAddress?isDark?"rgba(16,217,138,0.12)":"rgba(4,120,87,0.08)":isDark?"rgba(79,110,247,0.10)":"rgba(29,78,216,0.06)" }}>
                                        <Home size={14} style={{ color:a.defaultAddress?tk.emerald:tk.accent }}/>
                                      </div>
                                      <div>
                                        <p className="font-bold" style={{ color:tk.text1 }}>{a.recipientName} · {a.phoneNumber}</p>
                                        <p className="mt-1 text-sm" style={{ color:tk.text2 }}>{fullAddress(a)}</p>
                                        {a.defaultAddress && (
                                          <span className="mt-2 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[0.6rem] font-black uppercase tracking-wider"
                                            style={{ background:isDark?"rgba(16,217,138,0.12)":"rgba(4,120,87,0.08)", border:`1px solid ${isDark?"rgba(16,217,138,0.3)":"rgba(4,120,87,0.22)"}`, color:tk.emerald }}>
                                            <Star size={9}/> {t("common.default")}
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                    <div className="flex flex-wrap gap-2 md:flex-shrink-0">
                                      <SecondaryBtn size="sm" icon={Edit3} onClick={() => { editAddress(a); setTab("address"); }} tk={tk} isDark={isDark}>
                                        {t("common.edit")}
                                      </SecondaryBtn>
                                      {!a.defaultAddress && (
                                        <SecondaryBtn size="sm" icon={Star} loading={busy===`default-${a.id}`} onClick={() => makeDefault(a)} tk={tk} isDark={isDark}>
                                          {t("common.default")}
                                        </SecondaryBtn>
                                      )}
                                      <DangerBtn size="sm" loading={busy===`delete-${a.id}`} onClick={() => removeAddress(a)} tk={tk}>
                                        <Trash2 size={12}/>
                                      </DangerBtn>
                                    </div>
                                  </div>
                                </motion.div>
                              ))}
                            </AnimatePresence>
                          </div>
                        )
                      }
                    </Section>

                    {/* Address form */}
                    <Section
                      title={editingAddressId?t("account.editAddress"):t("account.addAddress")}
                      icon={editingAddressId?Edit3:Plus} tk={tk} isDark={isDark}
                      action={editingAddressId && (
                        <SecondaryBtn size="sm" icon={X} onClick={resetAddressForm} tk={tk} isDark={isDark}>
                          {t("account.cancelEdit")}
                        </SecondaryBtn>
                      )}>
                      <form className="grid gap-4" onSubmit={saveAddress}>
                        <div className="grid gap-4 md:grid-cols-2">
                          <PInput value={addressForm.recipientName} placeholder={t("checkout.recipientName")} tk={tk} isDark={isDark} required
                            onChange={e => setAddressForm({...addressForm,recipientName:e.target.value})}/>
                          <PInput value={addressForm.phoneNumber} placeholder={t("checkout.phoneNumber")} tk={tk} isDark={isDark} required
                            onChange={e => setAddressForm({...addressForm,phoneNumber:e.target.value})}/>
                        </div>
                        <PInput value={addressForm.addressLine} placeholder={t("checkout.addressLine")} tk={tk} isDark={isDark} required
                          onChange={e => setAddressForm({...addressForm,addressLine:e.target.value})}/>
                        <div className="grid gap-4 md:grid-cols-3">
                          <PInput value={addressForm.ward} placeholder={t("checkout.ward")} tk={tk} isDark={isDark}
                            onChange={e => setAddressForm({...addressForm,ward:e.target.value})}/>
                          <PInput value={addressForm.district} placeholder={t("checkout.district")} tk={tk} isDark={isDark}
                            onChange={e => setAddressForm({...addressForm,district:e.target.value})}/>
                          <PInput value={addressForm.city} placeholder={t("checkout.city")} tk={tk} isDark={isDark}
                            onChange={e => setAddressForm({...addressForm,city:e.target.value})}/>
                        </div>
                        {/* Default checkbox */}
                        <label className="flex cursor-pointer items-center gap-3">
                          <motion.button type="button" whileHover={{scale:1.1}} whileTap={{scale:0.9}}
                            onClick={() => setAddressForm(f => ({...f,defaultAddress:!f.defaultAddress}))}
                            className="flex h-5 w-5 items-center justify-center rounded-md transition-all"
                            style={{
                              background:addressForm.defaultAddress?"linear-gradient(135deg,#2a3ecc,#4f6ef7)":isDark?"rgba(255,255,255,0.06)":"rgba(15,23,42,0.06)",
                              border:`1.5px solid ${addressForm.defaultAddress?"transparent":tk.border}`,
                              boxShadow:addressForm.defaultAddress?"0 2px 12px rgba(79,110,247,0.45)":"none",
                            }}>
                            <AnimatePresence>
                              {addressForm.defaultAddress && (
                                <motion.div initial={{scale:0}} animate={{scale:1}} exit={{scale:0}} transition={{duration:0.15}}>
                                  <CheckCircle2 size={13} color="#fff" strokeWidth={3}/>
                                </motion.div>
                              )}
                            </AnimatePresence>
                          </motion.button>
                          <span className="text-sm font-semibold" style={{ color:tk.text2 }}>{t("account.defaultAddress")}</span>
                        </label>
                        <PrimaryBtn type="submit" loading={busy==="address"} icon={editingAddressId?Save:Plus} tk={tk}>
                          {busy==="address"?t("common.working"):editingAddressId?t("account.updateAddress"):t("account.addAddress")}
                        </PrimaryBtn>
                      </form>
                    </Section>
                  </div>
                )}

                {/* SECURITY TAB */}
                {tab==="security" && (
                  <Section title={t("account.password")} icon={Key} tk={tk} isDark={isDark}>
                    <form className="grid gap-4" onSubmit={savePassword}>
                      {[
                        ["currentPassword",t("account.currentPassword")],
                        ["newPassword",t("account.newPassword")],
                        ["confirmPassword",t("account.confirmPassword")],
                      ].map(([field,label]) => (
                        <div key={field} className="relative">
                          <PInput type={showPw[field]?"text":"password"} value={passwordForm[field]}
                            placeholder={label} tk={tk} isDark={isDark} required
                            onChange={e => setPasswordForm(f => ({...f,[field]:e.target.value}))}/>
                          <button type="button"
                            onClick={() => setShowPw(p => ({...p,[field]:!p[field]}))}
                            className="absolute right-4 top-1/2 -translate-y-1/2 opacity-50 hover:opacity-100 transition-opacity"
                            style={{ color:tk.text2 }}>
                            {showPw[field]?<EyeOff size={15}/>:<Eye size={15}/>}
                          </button>
                        </div>
                      ))}
                      <PrimaryBtn type="submit" loading={busy==="password"} icon={Lock} tk={tk}>
                        {busy==="password"?t("common.working"):t("account.changePassword")}
                      </PrimaryBtn>
                    </form>

                    {/* Avatar upload */}
                    <div className="mt-6 border-t pt-6" style={{ borderColor:tk.border }}>
                      <p className="mb-3 text-xs font-black uppercase tracking-wider" style={{ color:tk.text3 }}>{t("account.avatar")}</p>
                      <motion.label whileHover={{scale:1.01}} whileTap={{scale:0.99}}
                        className="flex cursor-pointer items-center gap-3 overflow-hidden rounded-2xl px-5 py-4"
                        style={{ background:tk.surface2, border:`1px dashed ${tk.border}` }}>
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl"
                          style={{ background:"rgba(79,110,247,0.14)", color:"#4f6ef7" }}>
                          <Camera size={16}/>
                        </div>
                        <span className="text-sm font-semibold" style={{ color:tk.text2 }}>
                          {busy==="avatar"?t("common.working"):t("account.uploadNewAvatar","Tải ảnh đại diện mới")}
                        </span>
                        <input type="file" accept="image/*" className="hidden" disabled={busy==="avatar"} onChange={uploadAvatar}/>
                      </motion.label>
                    </div>
                  </Section>
                )}

                {/* SESSIONS TAB */}
                {tab==="sessions" && (
                  <Section title={t("account.sessions")} icon={Monitor} tk={tk} isDark={isDark}
                    action={
                      <DangerBtn loading={busy==="logoutAll"} onClick={logoutEverywhere} tk={tk}>
                        <LogOut size={13}/> {t("account.logoutAll")}
                      </DangerBtn>
                    }>
                    {sessions.length===0
                      ? <p className="py-4 text-center text-sm" style={{ color:tk.text3 }}>{t("account.noSessions")}</p>
                      : (
                        <div className="grid gap-3">
                          <AnimatePresence>
                            {sessions.map((s,i) => {
                              const sid = s.sessionId||s.id;
                              return (
                                <motion.div key={sid}
                                  initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} exit={{opacity:0,x:40}}
                                  transition={{duration:0.35,delay:i*0.04}}
                                  className="flex items-center justify-between gap-4 overflow-hidden rounded-2xl p-4"
                                  style={{
                                    background:s.current?(isDark?"rgba(79,110,247,0.12)":"rgba(29,78,216,0.07)"):tk.surface2,
                                    border:`1px solid ${s.current?isDark?"rgba(79,110,247,0.3)":"rgba(29,78,216,0.22)":tk.border}`,
                                  }}>
                                  {s.current && <div className="absolute left-0 top-0 h-full w-[2px] rounded-l-2xl" style={{ background:"#4f6ef7" }}/>}
                                  <div className="flex items-center gap-3">
                                    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl"
                                      style={{ background:s.current?isDark?"rgba(79,110,247,0.15)":"rgba(29,78,216,0.10)":isDark?"rgba(255,255,255,0.05)":"rgba(15,23,42,0.05)" }}>
                                      <Laptop size={14} style={{ color:s.current?tk.accent:tk.text3 }}/>
                                    </div>
                                    <div>
                                      <p className="text-sm font-bold" style={{ color:tk.text1 }}>
                                        {s.deviceInfo||t("account.device")}
                                        {s.current && <span className="ml-2 text-[0.6rem] font-black uppercase tracking-wider" style={{ color:tk.accent }}>{t("common.current","Thiết bị này")}</span>}
                                      </p>
                                      <p className="text-xs" style={{ color:tk.text3 }}>{s.ipAddress||""}</p>
                                    </div>
                                  </div>
                                  <DangerBtn size="sm" loading={busy===`session-${sid}`} onClick={() => revoke(sid)} tk={tk}>
                                    {t("account.revoke")}
                                  </DangerBtn>
                                </motion.div>
                              );
                            })}
                          </AnimatePresence>
                        </div>
                      )
                    }
                  </Section>
                )}

                {/* DANGER ZONE TAB */}
                {tab==="danger" && (
                  <Section title={t("account.dangerZone")} icon={ShieldOff} tk={tk} isDark={isDark} danger>
                    <p className="text-sm font-semibold" style={{ color:tk.red }}>{t("account.deactivateHelp")}</p>
                    <div className="mt-4 overflow-hidden rounded-2xl" style={{ border:`1px solid rgba(239,68,68,0.3)` }}>
                      <div className="flex items-center gap-2 border-b px-4 py-2.5" style={{ borderColor:"rgba(239,68,68,0.2)", background:"rgba(239,68,68,0.06)" }}>
                        <AlertCircle size={13} style={{ color:tk.red }}/>
                        <span className="text-[0.65rem] font-black uppercase tracking-wider" style={{ color:tk.red }}>
                          {t("account.confirmTyping","Gõ DEACTIVATE để xác nhận")}
                        </span>
                      </div>
                      <PInput value={deactivateConfirm} placeholder="DEACTIVATE" tk={tk} isDark={isDark}
                        onChange={e => setDeactivateConfirm(e.target.value)}
                        className="rounded-none rounded-b-2xl"
                        style={{ borderRadius:"0 0 16px 16px" }}/>
                    </div>
                    <motion.button
                      whileHover={deactivateConfirm==="DEACTIVATE"?{scale:1.02,y:-1}:{}}
                      whileTap={deactivateConfirm==="DEACTIVATE"?{scale:0.97}:{}}
                      type="button" onClick={deactivate}
                      disabled={busy==="deactivate"||deactivateConfirm!=="DEACTIVATE"}
                      className="mt-4 flex w-full items-center justify-center gap-2 rounded-full py-4 text-sm font-black uppercase tracking-wider text-white transition-all disabled:cursor-not-allowed disabled:opacity-40"
                      style={{
                        background:"linear-gradient(135deg,#7f1d1d,#b91c1c,#ef4444)",
                        boxShadow:deactivateConfirm==="DEACTIVATE"?"0 8px 24px rgba(239,68,68,0.45)":"none",
                      }}>
                      <ShieldOff size={15}/>
                      {busy==="deactivate"?t("common.working"):t("account.deactivate")}
                    </motion.button>
                  </Section>
                )}
              </motion.div>
            </AnimatePresence>
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Account Hero Bar ───────────────────────── */
function AccountHeroBar({ profile, initials, tk, t, onAvatarClick, busy }) {
  return (
    <motion.div initial={{opacity:0,y:-20}} animate={{opacity:1,y:0}}
      transition={{duration:0.55,ease:[0.22,1,0.36,1]}}
      className="relative mb-8 overflow-hidden rounded-[22px] px-8 py-7"
      style={{ background:tk.heroBg, border:"1px solid rgba(255,255,255,0.07)", boxShadow:"0 24px 60px rgba(0,0,0,0.4)" }}>
      <div className="absolute left-0 right-0 top-0 h-[1.5px]" style={{ background:tk.heroLine }}/>
      <div className="pointer-events-none absolute -right-8 -top-8 h-36 w-36 rounded-full"
        style={{ background:"radial-gradient(circle,rgba(79,110,247,0.18) 0%,transparent 70%)" }}/>
      <div className="relative flex items-center gap-5">
        {/* Mini avatar */}
        <motion.div whileHover={{scale:1.06}} className="relative cursor-pointer flex-shrink-0" onClick={onAvatarClick}>
          <div className="overflow-hidden rounded-2xl" style={{ width:52, height:52, boxShadow:"0 6px 20px rgba(0,0,0,0.4)" }}>
            {profile?.avatarUrl
              ? <img className="h-full w-full object-cover" src={profile.avatarUrl} alt="avatar"/>
              : <div className="flex h-full w-full items-center justify-center font-black text-xl text-white"
                  style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7,#7c3aed)" }}>{initials}</div>
            }
          </div>
          {busy && (
            <div className="absolute inset-0 flex items-center justify-center rounded-2xl" style={{ background:"rgba(0,0,0,0.5)" }}>
              <motion.div animate={{rotate:360}} transition={{duration:0.8,repeat:Infinity,ease:"linear"}}>
                <RotateCcw size={14} color="#fff"/>
              </motion.div>
            </div>
          )}
        </motion.div>
        <div>
          <p className="text-[0.6rem] font-black uppercase tracking-[0.2em]" style={{ color:"#4a5578" }}>
            {t("account.eyebrow","Tài khoản")}
          </p>
          <h1 className="text-2xl font-black md:text-3xl" style={{ color:"#e8eeff", fontFamily:"var(--f-serif)" }}>
            {[profile?.firstName,profile?.lastName].filter(Boolean).join(" ")||profile?.username||t("account.title","Tài khoản của tôi")}
          </h1>
          {profile?.email && (
            <p className="mt-0.5 flex items-center gap-1.5 text-xs" style={{ color:"#4a5578" }}>
              <Mail size={10}/>{profile.email}
            </p>
          )}
        </div>
        {profile?.emailVerified && (
          <motion.div initial={{scale:0}} animate={{scale:1}}
            transition={{type:"spring",stiffness:400,damping:18,delay:0.4}}
            className="ml-auto flex h-10 w-10 items-center justify-center rounded-full"
            style={{ background:"rgba(16,217,138,0.15)", border:"1px solid rgba(16,217,138,0.35)" }}>
            <BadgeCheck size={18} color="#10d98a"/>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}

/* ── Section wrapper ────────────────────────── */
function Section({ title, icon:Icon, children, tk, danger, action }) {
  return (
    <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{duration:0.4,ease:[0.22,1,0.36,1]}}
      className="overflow-hidden rounded-[22px]"
      style={{
        background:tk.surface1,
        border:`1px solid ${danger?"rgba(239,68,68,0.25)":tk.border}`,
        backdropFilter:"blur(24px)",
        boxShadow:danger?"0 8px 32px rgba(239,68,68,0.10)":undefined,
      }}>
      {danger && <div className="h-[1.5px]" style={{ background:"linear-gradient(90deg,transparent,#ef4444 40%,transparent)" }}/>}
      {!danger && <div className="h-[1.5px]" style={{ background:tk.heroLine }}/>}
      <div className="flex items-center gap-3 border-b px-6 py-5" style={{ borderColor:tk.border }}>
        <div className="flex h-8 w-8 items-center justify-center rounded-lg"
          style={{ background:danger?"rgba(239,68,68,0.12)":"rgba(79,110,247,0.12)", color:danger?"#ef4444":"#4f6ef7" }}>
          <Icon size={15}/>
        </div>
        <h2 className="text-base font-black" style={{ color:danger?"#ef4444":tk.text1 }}>{title}</h2>
        {action && <div className="ml-auto">{action}</div>}
      </div>
      <div className="p-6">{children}</div>
    </motion.div>
  );
}

/* ── Premium Input ──────────────────────────── */
function PInput({ tk, isDark, ...props }) {
  const [focus, setFocus] = useState(false);
  return (
    <input
      {...props}
      onFocus={() => setFocus(true)}
      onBlur={() => setFocus(false)}
      className={`w-full rounded-2xl px-4 py-3.5 text-sm font-semibold outline-none transition-all ${props.className||""}`}
      style={{
        background:tk.inputBg,
        border:`1px solid ${focus?isDark?"rgba(79,110,247,0.6)":"rgba(29,78,216,0.5)":tk.border}`,
        color:tk.text1,
        boxShadow:focus?`0 0 0 3px ${isDark?"rgba(79,110,247,0.15)":"rgba(29,78,216,0.10)"}`:undefined,
        "::placeholder":{ color:tk.text3 },
        ...props.style,
      }}/>
  );
}

/* ── Premium Select ─────────────────────────── */
function PSelect({ tk, children, ...props }) {
  return (
    <select {...props}
      className="w-full rounded-2xl px-4 py-3.5 text-sm font-semibold outline-none transition-all"
      style={{
        background:tk.inputBg, border:`1px solid ${tk.border}`,
        color:tk.text1, appearance:"none",
      }}>
      {children}
    </select>
  );
}

/* ── Primary Button ─────────────────────────── */
function PrimaryBtn({ children, icon:Icon, loading, ...props }) {
  return (
    <motion.button
      whileHover={{ scale:1.02, y:-1 }} whileTap={{ scale:0.97 }}
      {...props}
      className="flex items-center justify-center gap-2 rounded-full py-4 text-sm font-black uppercase tracking-wider text-white"
      style={{ background:"linear-gradient(135deg,#2a3ecc,#4f6ef7)", boxShadow:"0 8px 28px rgba(79,110,247,0.4)", ...(props.style||{}) }}>
      {loading
        ? <motion.div animate={{rotate:360}} transition={{duration:0.8,repeat:Infinity,ease:"linear"}}><RotateCcw size={14}/></motion.div>
        : Icon && <Icon size={14}/>
      }
      {children}
    </motion.button>
  );
}

/* ── Secondary Button ───────────────────────── */
function SecondaryBtn({ children, icon:Icon, loading, onClick, size:sz, tk }) {
  return (
    <motion.button type="button" whileHover={{scale:1.03}} whileTap={{scale:0.97}}
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-full font-bold ${sz==="sm"?"px-4 py-2 text-xs":"px-5 py-3 text-sm"}`}
      style={{ background:tk.surface2, border:`1px solid ${tk.border}`, color:tk.text1 }}>
      {loading
        ? <motion.div animate={{rotate:360}} transition={{duration:0.8,repeat:Infinity,ease:"linear"}}><RotateCcw size={11}/></motion.div>
        : Icon && <Icon size={12}/>
      }
      {children}
    </motion.button>
  );
}

/* ── Danger Button ──────────────────────────── */
function DangerBtn({ children, loading, onClick, size:sz, tk }) {
  return (
    <motion.button type="button" whileHover={{scale:1.03}} whileTap={{scale:0.97}}
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-full font-bold ${sz==="sm"?"px-4 py-2 text-xs":"px-5 py-2.5 text-sm"}`}
      style={{ background:"rgba(239,68,68,0.10)", border:"1px solid rgba(239,68,68,0.28)", color:tk.red }}>
      {loading
        ? <motion.div animate={{rotate:360}} transition={{duration:0.8,repeat:Infinity,ease:"linear"}}><RotateCcw size={11}/></motion.div>
        : null
      }
      {children}
    </motion.button>
  );
}

/* ── Meta line ──────────────────────────────── */
function MetaLine({ icon:Icon, label, value, tk }) {
  return (
    <div className="flex items-center gap-3">
      <Icon size={12} style={{ color:tk.accent, flexShrink:0 }}/>
      <dt className="text-[0.65rem] font-black uppercase tracking-[0.14em] w-24 flex-shrink-0" style={{ color:tk.text3 }}>{label}</dt>
      <dd className="truncate text-sm font-semibold" style={{ color:tk.text1 }}>{value}</dd>
    </div>
  );
}

/* ── Status badge ───────────────────────────── */
function StatusBadge({ label, color, bg, border }) {
  return (
    <span className="inline-flex items-center rounded-full px-3 py-1 text-[0.62rem] font-black uppercase tracking-wider"
      style={{ background:bg, border:`1px solid ${border}`, color }}>
      {label}
    </span>
  );
}

/* ── Utils ──────────────────────────────────── */
function fullAddress(a) {
  return [a.addressLine,a.ward,a.district,a.city].filter(Boolean).join(", ");
}
