import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { motion, AnimatePresence } from "motion/react";
import {
  LayoutGrid, Plus, Edit, Trash2, X, RefreshCw, ChevronRight,
} from "lucide-react";

import {
  createAdminCategory, deleteAdminCategory, updateAdminCategory,
} from "../../api/adminApi.js";
import { Modal, useConfirm } from "../../components/ui/index.jsx";
import { getCategories, getCategoryTree } from "../../api/catalogApi.js";
import { normalizeCategory, pageRows } from "../../utils/mappers.js";

/* ── Constants ─────────────────────────────────── */
const emptyForm = {
  categoryName:"", slug:"", description:"", imageUrl:"", imagePublicId:"",
  displayOrder:0, parentId:"", active:true, visible:true,
};

/* ── Shared UI ─────────────────────────────────── */
function PInput({ label, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</label>}
      <input className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-shadow" {...props}/>
    </div>
  );
}
function PTextarea({ label, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</label>}
      <textarea rows={3} className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 resize-none transition-shadow" {...props}/>
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
function Card({ title, icon: Icon, children, action }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
        <div className="flex items-center gap-2">
          {Icon && <Icon size={16} className="text-indigo-500"/>}
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h3>
        </div>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminCategoriesPage() {
  const { t } = useTranslation();
  const confirm = useConfirm();

  // ── State (100% preserved) ──────────────────
  const [categories, setCategories] = useState([]);
  const [tree, setTree]             = useState([]);
  const [message, setMessage]       = useState("");
  const [loading, setLoading]       = useState(true);
  const [form, setForm]             = useState(emptyForm);
  const [editingId, setEditingId]   = useState(null);
  const [formOpen, setFormOpen]     = useState(false);

  const flatCategories = useMemo(() => categories.map(normalizeCategory).filter(Boolean), [categories]);
  const treeRows       = useMemo(() => flattenTree(tree), [tree]);

  const refreshCategories = useCallback(async () => {
    setLoading(true);
    try {
      const [listRows, treePayload] = await Promise.all([getCategories(), getCategoryTree()]);
      setCategories(pageRows(listRows));
      setTree(pageRows(treePayload));
    } catch (error) {
      setCategories([]); setTree([]);
      setMessage(error.message || t("admin.errors.categories"));
    } finally { setLoading(false); }
  }, [t]);

  useEffect(() => { refreshCategories(); }, [refreshCategories]);

  async function submit(event) {
    event.preventDefault(); setMessage("");
    const validation = validateCategoryForm(form, t);
    if (validation) { setMessage(validation); return; }
    try {
      const payload = { ...form, parentId: form.parentId ? Number(form.parentId) : null, displayOrder: Number(form.displayOrder || 0) };
      if (editingId) await updateAdminCategory(editingId, payload);
      else await createAdminCategory(payload);
      setMessage(editingId ? t("admin.categoryUpdated") : t("admin.categorySaved"));
      closeForm();
      await refreshCategories();
    } catch (error) { setMessage(error.message || t("admin.errors.categorySave")); }
  }

  async function remove(category) {
    const confirmed = await confirm({
      title: t("common.delete"), message: t("admin.confirmDeleteCategory", { name: category.label }),
      confirmLabel: t("common.delete"), cancelLabel: t("common.cancel"), danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteAdminCategory(category.id);
      setMessage(t("admin.categoryDeleted"));
      if (editingId === category.id) closeForm();
      await refreshCategories();
    } catch (error) { setMessage(error.message || t("admin.errors.categoryDelete")); }
  }

  function startCreate() {
    setEditingId(null);
    setForm(emptyForm);
    setMessage("");
    setFormOpen(true);
  }

  function edit(category) {
    setEditingId(category.id);
    setForm({
      categoryName: category.label || "", slug: category.slug || "", description: category.description || "",
      imageUrl: category.imageUrl || "", imagePublicId: category.imagePublicId || "",
      displayOrder: category.displayOrder || 0, parentId: category.parentId || "",
      active: category.active !== false, visible: category.visible !== false,
    });
    setMessage("");
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditingId(null);
    setForm(emptyForm);
  }

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="max-w-full space-y-6">

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.categoriesTitle","Quản lý danh mục")}</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.categoriesEyebrow","Quản lý cây danh mục sách")}</p>
        </div>
        <PrimaryBtn icon={Plus} onClick={startCreate}>{t("admin.newCategory","Thêm danh mục")}</PrimaryBtn>
      </div>

      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {/* Category tree */}
      <Card title={t("admin.categoryTree","Cây danh mục")} icon={LayoutGrid}>
        <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700">
          {loading
            ? [...Array(4)].map((_,i) => (
                <div key={i} className="animate-pulse border-b border-slate-100 dark:border-slate-800 p-4 last:border-0">
                  <div className="h-4 bg-slate-100 dark:bg-slate-800 rounded w-1/3"/>
                </div>
              ))
            : treeRows.map(({ category, level }, index) => (
                <motion.div key={category.id}
                  initial={{ opacity:0, x:-8 }} animate={{ opacity:1, x:0 }} transition={{ delay: index * 0.04 }}
                  className="grid gap-3 border-b border-slate-100 dark:border-slate-800 p-4 last:border-0 hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors md:grid-cols-[1fr_180px_100px_120px_auto] md:items-center">
                  <div style={{ paddingLeft: `${level * 20}px` }} className="flex items-center gap-2">
                    {level > 0 && <ChevronRight size={14} className="text-slate-300 dark:text-slate-600 flex-shrink-0"/>}
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-slate-100">{category.label}</p>
                      <p className="text-xs text-slate-400 mt-0.5">{category.description || "—"}</p>
                    </div>
                  </div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 font-mono">{category.slug}</span>
                  <span className="text-xs text-slate-600 dark:text-slate-300 font-semibold">{t("admin.orderValue","#{{value}}", { value: category.displayOrder ?? 0 })}</span>
                  <div className="flex flex-wrap gap-1">
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      category.active !== false ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400" : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
                    }`}>{category.active === false ? t("common.hidden","Ẩn") : t("common.active","Hiện")}</span>
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      category.visible !== false ? "bg-sky-50 text-sky-700 dark:bg-sky-500/10 dark:text-sky-400" : "bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400"
                    }`}>{category.visible === false ? t("common.hidden","Ẩn") : t("common.visible","Hiển thị")}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <SecBtn icon={Edit} onClick={() => edit(category)}>{t("common.edit","Sửa")}</SecBtn>
                    <SecBtn icon={Trash2} danger onClick={() => remove(category)}>{t("common.delete","Xóa")}</SecBtn>
                  </div>
                </motion.div>
              ))
          }
          {!loading && !treeRows.length && (
            <div className="flex flex-col items-center justify-center py-12 text-slate-400">
              <LayoutGrid size={28} className="mb-2 opacity-40"/>
              <p className="text-sm">{t("admin.noCategories","Chưa có danh mục nào")}</p>
            </div>
          )}
        </div>
      </Card>

      <CategoryFormModal
        editingId={editingId}
        flatCategories={flatCategories}
        form={form}
        onClose={closeForm}
        open={formOpen}
        setForm={setForm}
        submit={submit}
        t={t}
      />
    </motion.div>
  );
}

function CategoryFormModal({ editingId, flatCategories, form, onClose, open, setForm, submit, t }) {
  return (
    <Modal
      description={t("admin.categoryFormHelp","Điền thông tin danh mục bên dưới")}
      onClose={onClose}
      open={open}
      size="lg"
      title={editingId ? t("admin.editCategory","Sửa danh mục") : t("admin.createCategory","Thêm danh mục mới")}
    >
      <form className="grid gap-5" onSubmit={submit}>
        <div className="grid gap-4 sm:grid-cols-2">
          <PInput label={t("admin.categoryName","Tên danh mục")} required value={form.categoryName} onChange={e => setForm({ ...form, categoryName: e.target.value })} placeholder="Sách văn học"/>
          <PInput label={t("admin.slug","Slug")} value={form.slug} onChange={e => setForm({ ...form, slug: e.target.value })} placeholder="sach-van-hoc"/>
        </div>
        <PTextarea label={t("admin.description","Mô tả")} required value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Mô tả danh mục..."/>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <PInput label={t("admin.imageUrl","URL ảnh")} value={form.imageUrl} onChange={e => setForm({ ...form, imageUrl: e.target.value })}/>
          <PInput label={t("admin.imagePublicId","Public ID ảnh")} value={form.imagePublicId} onChange={e => setForm({ ...form, imagePublicId: e.target.value })}/>
          <PInput label={t("admin.displayOrder","Thứ tự hiển thị")} value={form.displayOrder} onChange={e => setForm({ ...form, displayOrder: e.target.value })} type="number" min="0"/>
          <PSelect label={t("admin.rootCategory","Danh mục cha")} value={form.parentId} onChange={e => setForm({ ...form, parentId: e.target.value })}>
            <option value="">{t("admin.rootCategory","Danh mục gốc")}</option>
            {flatCategories.filter(c => c.id !== editingId).map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
          </PSelect>
        </div>
        <div className="flex flex-wrap gap-4">
          <label className="flex items-center gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
            <input type="checkbox" checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })} className="w-4 h-4 text-indigo-600 rounded"/>
            <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{t("common.active","Hiển thị")}</span>
          </label>
          <label className="flex items-center gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
            <input type="checkbox" checked={form.visible} onChange={e => setForm({ ...form, visible: e.target.checked })} className="w-4 h-4 text-indigo-600 rounded"/>
            <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{t("common.visible","Hiển thị trên website")}</span>
          </label>
        </div>
        <div className="flex flex-wrap gap-3">
          <PrimaryBtn type="submit">{editingId ? t("admin.updateCategory","Cập nhật") : t("admin.saveCategory","Lưu danh mục")}</PrimaryBtn>
          <SecBtn type="button" onClick={onClose}>{t("common.cancel","Hủy")}</SecBtn>
        </div>
      </form>
    </Modal>
  );
}

/* ── Pure utils (100% preserved) ──────────────── */
function flattenTree(rows, level = 0) {
  return pageRows(rows).flatMap(row => {
    const category = normalizeCategory(row);
    if (!category) return [];
    return [{ category, level }, ...flattenTree(row.children || [], level + 1)];
  });
}
function validateCategoryForm(form, t) {
  if (!String(form.categoryName || "").trim()) return t("admin.validationCategoryName");
  if (!String(form.description || "").trim()) return t("admin.validationCategoryDescription");
  if (form.displayOrder !== "" && Number(form.displayOrder) < 0) return t("admin.validationNonNegative");
  return "";
}
